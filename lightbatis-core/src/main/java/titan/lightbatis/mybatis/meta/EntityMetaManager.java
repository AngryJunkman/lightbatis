/**
 * 
 */
package titan.lightbatis.mybatis.meta;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OrderBy;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.ObjectTypeHandler;
import org.apache.ibatis.type.SimpleTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import titan.lightbatis.configuration.MapperConfig;
import titan.lightbatis.exception.LightbatisException;
import titan.lightbatis.table.ColumnSchema;
import titan.lightbatis.table.ITableSchemaManager;
import titan.lightbatis.table.TableSchema;
import titan.lightbatis.utils.FieldUtils;

/**
 * 实体类相关的辅助类
 * 
 * @author lifei
 *
 */
public class EntityMetaManager {

	private static final Logger log = LoggerFactory.getLogger(EntityMetaManager.class);

	/**
	 * 实体类 => 表对象
	 */
	private static final Map<Class<?>, EntityMeta> entityTableMap = new HashMap<Class<?>, EntityMeta>();
	private static final Map<String, EntityMeta> entityMetas = new HashMap<>();
	/**
	 * 获取表对象
	 * @TODO 需要修改，存在一个类中如果有多个返回的实体类是一样的，以 实体类为Key 会出现，后面的类覆盖前面的类。
	 * @param entityClass
	 * @return
	 */
	public static EntityMeta getEntityMeta(Class<?> entityClass) {
		EntityMeta entityTable = entityTableMap.get(entityClass);
		if (entityTable == null) {
			throw new LightbatisException("无法获取实体类" + entityClass.getCanonicalName() + "对应的表名!");
		}
		return entityTable;
	}
	public static EntityMeta findEntityMeta(Class<?> entityClass) {
		EntityMeta entityTable = entityTableMap.get(entityClass);
		return entityTable;
	}

	public static EntityMeta getEntityMeta(String mapperStatementId) {
//		Optional<EntityMeta> opt = entityTableMap.values().stream()
//				.filter(meta -> meta.getMappedStatementId().equals(mapperStatementId)).findFirst();
//		if (opt.isPresent()) {
//			return opt.get();
//		}
		if (entityMetas.containsKey(mapperStatementId)) {
			return entityMetas.get(mapperStatementId);
		}
		return null;
	}

	/**
	 * 获取默认的orderby语句
	 *
	 * @param entityClass
	 * @return
	 */
	public static String getOrderByClause(Class<?> entityClass) {
		EntityMeta table = getEntityMeta(entityClass);
		if (table.getOrderByClause() != null) {
			return table.getOrderByClause();
		}
		StringBuilder orderBy = new StringBuilder();
		for (ColumnMeta column : table.getClassColumns()) {
			if (column.getOrderBy() != null) {
				if (orderBy.length() != 0) {
					orderBy.append(",");
				}
				orderBy.append(column.getColumn()).append(" ").append(column.getOrderBy());
			}
		}
		table.setOrderByClause(orderBy.toString());
		return table.getOrderByClause();
	}

	/**
	 * 获取全部列
	 *
	 * @param entityClass
	 * @return
	 */
	public static Set<ColumnMeta> getColumns(Class<?> entityClass) {
		return getEntityMeta(entityClass).getClassColumns();
	}

	public static Set<ColumnMeta> getColumns(String tableName, Class<?> entityClass) {
		Set<ColumnMeta> colSet = getEntityMeta(entityClass).getClassColumns();
		HashSet<ColumnMeta> tableSet = new HashSet<>();
		colSet.forEach((meta) -> {
			if (meta.getTableName().equals(tableName)) {
				tableSet.add(meta);
			}
		});
		return tableSet;
	}

	/**
	 * 获取主键信息
	 *
	 * @param entityClass
	 * @return
	 */
	public static Set<ColumnMeta> getPKColumns(Class<?> entityClass) {
		return getEntityMeta(entityClass).getEntityClassPKColumns();
	}

	/**
	 * 获取查询的Select
	 *
	 * @param entityClass
	 * @return
	 */
	public static String getSelectColumns(Class<?> entityClass) {
		EntityMeta entityTable = getEntityMeta(entityClass);
		if (entityTable.getBaseSelect() != null) {
			return entityTable.getBaseSelect();
		}
		Set<ColumnMeta> columnList = getColumns(entityClass);
		StringBuilder selectBuilder = new StringBuilder();
		boolean skipAlias = Map.class.isAssignableFrom(entityClass);
		for (ColumnMeta entityColumn : columnList) {
			selectBuilder.append(entityColumn.getColumn());
			if (!skipAlias && !entityColumn.getColumn().equalsIgnoreCase(entityColumn.getProperty())) {
				// 不等的时候分几种情况，例如`DESC`
				if (entityColumn.getColumn().substring(1, entityColumn.getColumn().length() - 1)
						.equalsIgnoreCase(entityColumn.getProperty())) {
					selectBuilder.append(",");
				} else {
					selectBuilder.append(" AS ").append(entityColumn.getProperty()).append(",");
				}
			} else {
				selectBuilder.append(",");
			}
		}
		entityTable.setBaseSelect(selectBuilder.substring(0, selectBuilder.length() - 1));
		return entityTable.getBaseSelect();
	}

	/**
	 * 初始化实体属性
	 *
	 * @param entityClass
	 * @param config
	 */
	public static synchronized EntityMeta initEntityNameMap(Class<?> entityClass, MapperConfig config, String msId) {
		if (entityMetas.containsKey(msId) ) {
			return entityMetas.get(msId);
		}
//		if (entityTableMap.get(entityClass) != null) {
//			return entityTableMap.get(entityClass);
//		}

		// 创建并缓存EntityTable
		EntityMeta entityTable = processEntity(entityClass,config);
		entityTable.setMappedStatementId(msId);
		entityMetas.put(msId, entityTable);
		entityTableMap.put(entityClass, entityTable);
		return entityTable;
	}

	private static EntityMeta processEntity(Class<?> entityClass, MapperConfig config) {
		// 创建并缓存EntityTable
		EntityMeta entityTable = null;
		if (entityClass.isAnnotationPresent(Table.class)) {
			Table table = entityClass.getAnnotation(Table.class);
			if (!table.name().equals("")) {
				entityTable = new EntityMeta(entityClass);
				entityTable.setTable(table);
			}
		}
		
		if (entityTable == null) {
			entityTable = new EntityMeta(entityClass);
			// 可以通过stye控制
			//entityTable.setName(StringUtil.convertByStyle(entityClass.getSimpleName(), style));
			entityTable.setName(entityClass.getSimpleName());
		}
		if (entityClass.isAnnotationPresent(SecondaryTable.class)) {
			SecondaryTable st = entityClass.getAnnotation(SecondaryTable.class);
			entityTable.addSecondaryTable(st);
		}
		if (entityClass.isAnnotationPresent(SecondaryTables.class)) {
			SecondaryTables tables = entityClass.getAnnotation(SecondaryTables.class);
			SecondaryTable tbls[] = tables.value();
			for (SecondaryTable secondaryTable : tbls) {
				entityTable.addSecondaryTable(secondaryTable);
			}
		}
		TableSchema tableSchema = ITableSchemaManager.getInstance().getTable(entityTable.getName());
		
		// 处理所有列
		List<FieldMeta> fields = null;
		if (config.isEnableMethodAnnotation()) {
			fields = FieldUtils.getAll(entityClass);
		} else {
			fields = FieldUtils.getFields(entityClass);
		}
		Map<String, Field> fieldMap = new HashMap<>();
		Field[] myfields = entityClass.getDeclaredFields();
		for (Field f : myfields) {
			String name = f.getName();
			fieldMap.put(name, f);
		}
		for (FieldMeta field : fields) {
			// 如果启用了简单类型，就做简单类型校验，如果不是简单类型，直接跳过
			if (config.isUseSimpleType() && !SimpleTypeRegistry.isSimpleType(field.getJavaType())) {
				continue;
			}
			ColumnMeta colMeta = processField(entityTable, tableSchema , field);
			if (List.class.isAssignableFrom(colMeta.getJavaType())) {
				Field colField = fieldMap.get(colMeta.getProperty());
				Type fieldType = colField.getGenericType();
				Class<?> fieldClz = getReturnType(fieldType);
				//分析元素的类型
				EntityMeta innerEntity = processEntity(fieldClz, config);
				colMeta.setCollectionBaseType(innerEntity);
				innerEntity.setMappedStatementId("");
				entityTableMap.put(fieldClz, innerEntity);
			}
		}
		// 当pk.size=0的时候使用所有列作为主键
		if (entityTable.getEntityClassPKColumns().size() == 0) {
			entityTable.setEntityClassPKColumns(entityTable.getClassColumns());
		}
		entityTable.initPropertyMap();
		return entityTable;
	}

	/**
	 * 处理一列
	 *
	 * @param entityTable
	 * @param style
	 * @param field
	 */
	private static ColumnMeta processField(EntityMeta entityTable, TableSchema tableSchema, FieldMeta field) {
		// 排除字段
		if (field.isAnnotationPresent(Transient.class)) {
			return null;
		}
		// Id
		ColumnMeta entityColumn = new ColumnMeta(entityTable);
		if (field.isAnnotationPresent(Id.class)) {
			entityColumn.setId(true);
		}
		// Column
		String columnName = null;
		if (field.isAnnotationPresent(Column.class)) {
			Column column = field.getAnnotation(Column.class);
			columnName = column.name();
			entityColumn.setUpdatable(column.updatable());
			entityColumn.setInsertable(column.insertable());
			String tableName = column.table();
			if (tableName != null) {
				entityColumn.setTableName(tableName);
			} else {
				entityColumn.setTableName(entityTable.getName());
			}
		}
		if (StringUtils.isEmpty(entityColumn.getTableName())) {
			entityColumn.setTableName(entityTable.getName());
		}
		// ColumnType
//		if (field.isAnnotationPresent(ColumnType.class)) {
//			ColumnType columnType = field.getAnnotation(ColumnType.class);
//			// column可以起到别名的作用
//			if (StringUtils.isEmpty(columnName) && StringUtils.isNotEmpty(columnType.column())) {
//				columnName = columnType.column();
//			}
//			if (columnType.jdbcType() != JdbcType.UNDEFINED) {
//				entityColumn.setJdbcType(columnType.jdbcType());
//			}
//			if (columnType.typeHandler() != UnknownTypeHandler.class) {
//				entityColumn.setTypeHandler(columnType.typeHandler());
//			}
//		}
		ColumnSchema colSchema = tableSchema.getColumn(columnName);
		if (colSchema != null) {
			entityColumn.setJdbcType(JdbcType.forCode(colSchema.getType()));
		}
		// 表名
		if (StringUtils.isEmpty(columnName)) {
			columnName =field.getName();// StringUtils.convertByStyle(field.getName(), style);
		}
		entityColumn.setProperty(field.getName());
		entityColumn.setColumn(columnName);
		entityColumn.setJavaType(field.getJavaType());
		// OrderBy
		if (field.isAnnotationPresent(OrderBy.class)) {
			OrderBy orderBy = field.getAnnotation(OrderBy.class);
			if (orderBy.value().equals("")) {
				entityColumn.setOrderBy("ASC");
			} else {
				entityColumn.setOrderBy(orderBy.value());
			}
		}
		// 如果类型是 List, 这里需要查找到元素的基类的类型
		if (List.class.isAssignableFrom(entityColumn.getJavaType()) && entityColumn.getTypeHandler() == null) {
			entityColumn.setTypeHandler(ObjectTypeHandler.class);
		}
		// 主键策略 - Oracle序列，MySql自动增长，UUID
		if (field.isAnnotationPresent(SequenceGenerator.class)) {
			SequenceGenerator sequenceGenerator = field.getAnnotation(SequenceGenerator.class);
			if (sequenceGenerator.sequenceName().equals("")) {
				throw new LightbatisException(entityTable.getEntityClass() + "字段" + field.getName()
						+ "的注解@SequenceGenerator未指定sequenceName!");
			}
			entityColumn.setSequenceName(sequenceGenerator.sequenceName());
		} else if (field.isAnnotationPresent(GeneratedValue.class)) {
			GeneratedValue generatedValue = field.getAnnotation(GeneratedValue.class);
			if (generatedValue.generator().equals("UUID")) {
				entityColumn.setUuid(true);
			} else if (generatedValue.generator().equals("JDBC")) {
				entityColumn.setIdentity(true);
				entityColumn.setGenerator("JDBC");
				entityTable.setKeyProperties(entityColumn.getProperty());
				entityTable.setKeyColumns(entityColumn.getColumn());
			} else if (generatedValue.generator().equals(GeneratedValueType.SNOWFLAKE)) {
				entityColumn.setIdentity(true);
				entityColumn.setGenerator(GeneratedValueType.SNOWFLAKE);
				entityTable.setKeyProperties(entityColumn.getProperty());
				entityTable.setKeyColumns(entityColumn.getColumn());
			}
			else {
				// 允许通过generator来设置获取id的sql,例如mysql=CALL IDENTITY(),hsqldb=SELECT
				// SCOPE_IDENTITY()
				// 允许通过拦截器参数设置公共的generator
				if (generatedValue.strategy() == GenerationType.IDENTITY) {
					// mysql的自动增长
					entityColumn.setIdentity(true);
//					if (!generatedValue.generator().equals("")) {
//						String generator = null;
//						IdentityDialect identityDialect = IdentityDialect
//								.getDatabaseDialect(generatedValue.generator());
//						if (identityDialect != null) {
//							generator = identityDialect.getIdentityRetrievalStatement();
//						} else {
//							generator = generatedValue.generator();
//						}
//						entityColumn.setGenerator(generator);
//					}
				} else {
					throw new LightbatisException(field.getName() + " - 该字段@GeneratedValue配置只允许以下几种形式:"
							+ "\n1.全部数据库通用的@GeneratedValue(generator=\"UUID\")"
							+ "\n2.useGeneratedKeys的@GeneratedValue(generator=\\\"" + GeneratedValueType.SNOWFLAKE + "\\\")  "
							+ "\n3.雪花算法 useGeneratedKeys的@GeneratedValue(generator=\\\"Snowflake\\\")  "
							+ "\n4.类似mysql数据库的@GeneratedValue(strategy=GenerationType.IDENTITY[,generator=\"Mysql\"])"
							);
				}
			}
		}
		entityTable.addColumn(entityColumn);
		if (entityColumn.isId()) {
			entityTable.getEntityClassPKColumns().add(entityColumn);
		}
		return entityColumn;
	}

	private static Class<?> getReturnType(Type type) {
		Class<?> returnType = null;
		Type resolvedReturnType = type;
		if (resolvedReturnType instanceof Class) {
			returnType = (Class<?>) resolvedReturnType;
			if (returnType.isArray()) {
				returnType = returnType.getComponentType();
			}

		} else if (resolvedReturnType instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) resolvedReturnType;
			Class<?> rawType = (Class<?>) parameterizedType.getRawType();
			if (Collection.class.isAssignableFrom(rawType) || Cursor.class.isAssignableFrom(rawType)) {
				Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
				if (actualTypeArguments != null && actualTypeArguments.length == 1) {
					Type returnTypeParameter = actualTypeArguments[0];
					if (returnTypeParameter instanceof Class<?>) {
						returnType = (Class<?>) returnTypeParameter;
					} else if (returnTypeParameter instanceof ParameterizedType) {
						// (gcode issue #443) actual type can be a also a parameterized type
						returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
					} else if (returnTypeParameter instanceof GenericArrayType) {
						Class<?> componentType = (Class<?>) ((GenericArrayType) returnTypeParameter)
								.getGenericComponentType();
						// (gcode issue #525) support List<byte[]>
						returnType = Array.newInstance(componentType, 0).getClass();
					}
				}
			}
		}

		return returnType;
	}
}
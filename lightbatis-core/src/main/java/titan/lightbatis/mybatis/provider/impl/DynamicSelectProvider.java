package titan.lightbatis.mybatis.provider.impl;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.session.Configuration;

import titan.lightbatis.exception.LightbatisException;
import titan.lightbatis.mapper.QueryMapperManger;
import titan.lightbatis.mybatis.MapperBuilder;
import titan.lightbatis.mybatis.MybatisSQLBuilder;
import titan.lightbatis.mybatis.meta.EntityMetaManager;
import titan.lightbatis.mybatis.provider.MapperProvider;

public class DynamicSelectProvider extends MapperProvider{
	
	private ParamNameResolver parameterResolver = null;
	private Method method = null;
	
	public DynamicSelectProvider(Configuration config, Method method, Class<?> mapperClass, MapperBuilder mapperHelper) {
		super(mapperClass, mapperHelper);
		parameterResolver= new ParamNameResolver(config, method);
		this.method = method;
	}

	public String buildSQL(SqlCommandType sqlCommandType, String msId) {
		if (sqlCommandType == SqlCommandType.SELECT) {
			String sql;
			try {
				sql = buildSelectSQL(msId);
				return sql;
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		return null;
	}
	public String buildQuery(SqlCommandType sqlCommandType, String mappedStatementId) {
		QueryMapperManger.addMapper(mappedStatementId, mapperClass);
		
		Class<?> entityClass = getEntityClass(mappedStatementId, method);
		// 修改返回值类型为实体类型
		StringBuilder sql = new StringBuilder();
		sql.append(MybatisSQLBuilder.selectAllColumns(entityClass));
		sql.append(MybatisSQLBuilder.fromTable(entityClass, tableName(entityClass)));
		//String whereSql = buildWhereSql();
		//sql.append(whereSql);
		// stmt.getBoundSql(parameterObject)
		return sql.toString();
	}
	private static String buildWhereSql() { 
	      return "<where>\n" +
	                "  <foreach collection=\"array\" item=\"predicate\" separator=\"and\">\n" +
	                "		${predicate}" +
	                "  </foreach>\n" +
	                "</where>";
	}
	protected String buildSelectSQL(String msId) throws Exception {
		Class<?> entityClass = getEntityClass(msId, method);
		String tableName = tableName(entityClass);
		StringBuilder sql = new StringBuilder();
		sql.append(MybatisSQLBuilder.selectAllColumns(tableName,entityClass));
		sql.append(MybatisSQLBuilder.fromTable(entityClass, tableName(entityClass)));
		String[] names = parameterResolver.getNames();
		if (names.length > 0) {
			// sql.append("<trim prefix=\"WHERE\" prefixOverrides=\"AND |OR \">");
			String whereSQL = MybatisSQLBuilder.whereColumns(entityClass, names);
			sql.append(whereSQL);
			// sql.append("</trim>");
		}
		String defaultOrderBy = MybatisSQLBuilder.orderByDefault(entityClass);
		if (StringUtils.isNotEmpty(defaultOrderBy)) {
			sql.append(" ").append(defaultOrderBy);
		}
		return sql.toString();
	}

	/**
	 * 将返回值的类型，注册到 MappedStatement 中去
	 * 
	 * @param mappedStatement
	 */
	public void registeResultMap(MappedStatement mappedStatement) {
		String msId = mappedStatement.getId();
		Class<?> entityClass = getEntityClass(msId, method);
		// 修改返回值类型为实体类型
		setResultType(mappedStatement, entityClass);
	}

	public Class<?> getEntityClass(String msId, Method method) {
		// String msId = ms.getId();
		if (entityClassMap.containsKey(msId)) {
			return entityClassMap.get(msId);
		} else {
			// Class<?> returnType = method.getReturnType();
			// EntityHelper.initEntityNameMap(returnType, mapperHelper.getConfig());
			// entityClassMap.put(msId, returnType);
			// return returnType;
			Class<?> entityClass = null;
			Class<?> mClass = method.getReturnType(); // getMapperClass(msId);
			if (List.class.isAssignableFrom(mClass)) {
				Type methodType = method.getGenericReturnType();
				if (methodType instanceof ParameterizedType) {
					ParameterizedType pt = (ParameterizedType) methodType;
					Type[] actualTypes = pt.getActualTypeArguments();
					if (actualTypes.length > 0) {
						Type t = actualTypes[0];
						if (t instanceof Class) {
							entityClass = (Class<?>) t;
						}
						
					}
				}
				if (entityClass == null) {
					Type[] types = mapperClass.getGenericInterfaces();
					for (Type type : types) {
						if (type instanceof ParameterizedType) {
							ParameterizedType t = (ParameterizedType) type;
							Class<?> returnType = (Class<?>) t.getActualTypeArguments()[0];
							entityClass = returnType;
						}
					}
				}
			} else {
				entityClass = mClass;
			}

			if (entityClass != null) {
				// 获取该类型后，第一次对该类型进行初始化
				EntityMetaManager.initEntityNameMap(entityClass, mapperBuilder.getConfig(),msId);
				entityClassMap.put(msId, entityClass);

				return entityClass;
			}
		}
		throw new LightbatisException("无法获取Mapper<T>泛型类型:" + msId);
	}

}
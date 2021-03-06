/**
 * 
 */
package titan.lightbatis.web.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import titan.lightbatis.table.TableSchema;

/**
 * @author lifei
 *
 */
@Data
@ApiModel(value="表的结构信息及对应的实体", description="表的结构信息及对应的实体")
public class TableEntitySchema extends TableSchema {
	/**
	 * 
	 */
	private static final long serialVersionUID = -530355886813687293L;
	@ApiModelProperty("Mapper 类名")
	private String mapperClzName = null;
	@ApiModelProperty("Controller 类名")
	private String controllerClzName = null;
	@ApiModelProperty("Service 类名")
	private String serviceClzName = null;
	@ApiModelProperty("Entity 类包名")
	private String entityPackageName = null;
	@ApiModelProperty("Mapper 类包名")
	private String mapperPackageName = null;
	@ApiModelProperty("Controller 类包名")
	private String controllerPackageName = null;
	@ApiModelProperty("Service 类包名")
	private String servicePackageName = null;

	@ApiModelProperty("默认逻辑删除字段")
	private String logicDelteColumn = "status";

	@ApiModelProperty("默认乐观锁字段")
	private String revisionColumn = "revision";
	@ApiModelProperty("默认创建时间字段")
	private String createdTimeColumn = "created_time";
	@ApiModelProperty("默认更新时间字段")
	private String updatedTimeColumn = "updated_time";
}

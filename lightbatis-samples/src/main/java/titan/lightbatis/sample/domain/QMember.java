package titan.lightbatis.sample.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;


import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;



/**
 * QMember is a Querydsl query type for Member
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMember extends com.querydsl.sql.RelationalPathBase<Member> {

    private static final long serialVersionUID = -944205493;

    public static final QMember member = new QMember("member");

    public final NumberPath<Long> createdBy = createNumber("createdBy", Long.class);

    public final DateTimePath<java.sql.Timestamp> createdTime = createDateTime("createdTime", java.sql.Timestamp.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Short> kindId = createNumber("kindId", Short.class);

    public final StringPath memberName = createString("memberName");

    public final NumberPath<Short> revision = createNumber("revision", Short.class);

    public final NumberPath<Long> staffId = createNumber("staffId", Long.class);

    public final StringPath staffName = createString("staffName");

    public final StringPath status = createString("status");

    public final NumberPath<Long> updatedBy = createNumber("updatedBy", Long.class);

    public final DateTimePath<java.sql.Timestamp> updatedTime = createDateTime("updatedTime", java.sql.Timestamp.class);

    public QMember(String variable) {
        super(Member.class, forVariable(variable), "", "member");
        addMetadata();
    }

    public QMember(String variable, String schema, String table) {
        super(Member.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMember(String variable, String schema) {
        super(Member.class, forVariable(variable), schema, "member");
        addMetadata();
    }

    public QMember(Path<? extends Member> path) {
        super(path.getType(), path.getMetadata(), "", "member");
        addMetadata();
    }

    public QMember(PathMetadata metadata) {
        super(Member.class, metadata, "", "member");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(createdBy, ColumnMetadata.named("created_by").withIndex(0).ofType(Types.NULL).withSize(19));
        addMetadata(createdTime, ColumnMetadata.named("created_time").withIndex(0).ofType(Types.NULL).withSize(29));
        addMetadata(id, ColumnMetadata.named("id").withIndex(0).ofType(Types.NULL).withSize(19).notNull());
        addMetadata(kindId, ColumnMetadata.named("kind_id").withIndex(0).ofType(Types.NULL).withSize(5));
        addMetadata(memberName, ColumnMetadata.named("member_name").withIndex(0).ofType(Types.NULL).withSize(128));
        addMetadata(revision, ColumnMetadata.named("revision").withIndex(0).ofType(Types.NULL).withSize(5));
        addMetadata(staffId, ColumnMetadata.named("staff_id").withIndex(0).ofType(Types.NULL).withSize(19));
        addMetadata(staffName, ColumnMetadata.named("staff_name").withIndex(0).ofType(Types.NULL).withSize(32));
        addMetadata(status, ColumnMetadata.named("status").withIndex(0).ofType(Types.NULL).withSize(32));
        addMetadata(updatedBy, ColumnMetadata.named("updated_by").withIndex(0).ofType(Types.NULL).withSize(19));
        addMetadata(updatedTime, ColumnMetadata.named("updated_time").withIndex(0).ofType(Types.NULL).withSize(29));
    }

}


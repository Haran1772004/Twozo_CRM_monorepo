package com.tlc.sql.olap.internal.meta.engine.ch;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Argument;
import lombok.Getter;

import java.util.Objects;

/**
 * ClickHouse Distributed engine. All configuration comes from {@code <param>} tags
 * in {@code <engine-configuration>}. The sharding key is expressed as an {@link Argument}
 * (e.g. cityHash64(ORG_ID)) parsed from a {@code <function>} tag nested inside a {@code <param>}.
 * DDL output: Distributed({cluster}, {database}, {table}, {shardingKey})
 *
 * @author sundar
 * @version 1.0
 */
@Getter
public class DistributedEngine implements CHTableEngine {

    public static final String NAME = "Distributed";

    private final String cluster;
    private final String database;
    private final String table;
    private final Argument shardingKey;

    /**
     * Creates a new Distributed engine with the given configuration parameters.
     *
     * @param cluster     the ClickHouse cluster name
     * @param database    the target database name
     * @param table       the target table name (must end with _dist, validated at parse time)
     * @param shardingKey the sharding key argument (typically a function call)
     */
    public DistributedEngine(final String cluster, final String database, final String table, final Argument shardingKey) {
        this.cluster = Objects.requireNonNull(cluster);
        this.database = Objects.requireNonNull(database);
        this.table = Objects.requireNonNull(table);
        this.shardingKey = Objects.requireNonNull(shardingKey);
    }

    /**
     * Returns the full engine name clause for DDL generation.
     *
     * @return the engine clause, e.g. Distributed(my_cluster, my_db, my_table_dist, cityHash64(ORG_ID))
     */
    @Override
    public String getName() {
        final String shardingKeyExpression = shardingKey.resolve(Column::getColumnName);

        return String.format("%s(%s, %s, %s, %s)", NAME, cluster, database, table, shardingKeyExpression);
    }
}

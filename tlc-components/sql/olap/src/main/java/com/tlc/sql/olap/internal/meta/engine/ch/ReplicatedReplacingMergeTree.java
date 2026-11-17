package com.tlc.sql.olap.internal.meta.engine.ch;

import com.tlc.sql.common.api.meta.ColumnDefinition;
import lombok.Getter;

import java.util.Objects;

/**
 * Composed engine combining ReplacingMergeTree semantics with replication support.
 * Extends {@link ReplacingMergeTree} for version/delete-flag column handling and implements
 * {@link ReplicatedEngine} for ZooKeeper-based replication params.
 * DDL output: ReplicatedReplacingMergeTree('{zookeeperPath}', '{replicaName}', ver_column[, del_flag_column])
 *
 * @author sundar
 * @version 1.0
 */
@Getter
public class ReplicatedReplacingMergeTree extends ReplacingMergeTree implements ReplicatedEngine {

    private static final String NAME = "ReplicatedReplacingMergeTree";

    private final String zookeeperPath;
    private final String replicaName;

    /**
     * Creates a replicated replacing merge tree engine with default replication params.
     *
     * @param versioningColumnDefinition the column used for version tracking
     */
    public ReplicatedReplacingMergeTree(final ColumnDefinition versioningColumnDefinition) {
        this(versioningColumnDefinition, null, ReplicatedEngineProperties.getZookeeperPath(), ReplicatedEngineProperties.getReplicaName());
    }

    /**
     * Creates a replicated replacing merge tree engine with default replication params
     * and an optional delete-flag column.
     *
     * @param versioningColumnDefinition  the column used for version tracking
     * @param deleteFlagColumnDefinition  the optional delete-flag column, may be null
     */
    public ReplicatedReplacingMergeTree(final ColumnDefinition versioningColumnDefinition,
                                        final ColumnDefinition deleteFlagColumnDefinition) {
        this(versioningColumnDefinition, deleteFlagColumnDefinition, ReplicatedEngineProperties.getZookeeperPath(), ReplicatedEngineProperties.getReplicaName());
    }

    /**
     * Creates a replicated replacing merge tree engine with explicit replication params.
     *
     * @param versioningColumnDefinition  the column used for version tracking
     * @param deleteFlagColumnDefinition  the optional delete-flag column, may be null
     * @param zookeeperPath               the ZooKeeper path for the replicated table
     * @param replicaName                 the replica identifier
     */
    public ReplicatedReplacingMergeTree(final ColumnDefinition versioningColumnDefinition,
                                        final ColumnDefinition deleteFlagColumnDefinition,
                                        final String zookeeperPath, final String replicaName) {
        super(versioningColumnDefinition, deleteFlagColumnDefinition);
        this.zookeeperPath = Objects.requireNonNull(zookeeperPath);
        this.replicaName = Objects.requireNonNull(replicaName);
    }

    /**
     * Returns the full engine name including replication params and version/delete-flag columns.
     *
     * @return the DDL engine clause, e.g. ReplicatedReplacingMergeTree('/path', '{replica}', TIMESTAMP)
     */
    @Override
    public String getName() {
        final StringBuilder nameBuilder = new StringBuilder(NAME);

        nameBuilder.append("('").append(zookeeperPath).append("', '").append(replicaName).append("'");
        nameBuilder.append(", ").append(getVersioningColumnDefinition().getColumnName());

        getDeleteFlagColumnDefinition().ifPresent(deleteFlagCol ->
                nameBuilder.append(", ").append(deleteFlagCol.getColumnName()));

        nameBuilder.append(")");
        return nameBuilder.toString();
    }
}

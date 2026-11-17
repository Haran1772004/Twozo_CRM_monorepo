package com.tlc.sql.olap.internal.meta.engine.ch;

/**
 * Contract for ClickHouse replicated engine parameters.
 * Defines the ZooKeeper path and replica name required by all
 * Replicated*MergeTree engine variants. The effective values are
 * sourced from application configuration through
 * {@link ReplicatedEngineProperties}. DDL assembly is performed by
 * {@link com.tlc.sql.olap.internal.handler.ch.CHDDLHandler}.
 *
 * @author sundar
 * @version 1.0
 */
public interface ReplicatedEngine {

    /**
     * Returns the ZooKeeper path for the replicated table.
     *
     * @return the ZooKeeper table path
     */
    String getZookeeperPath();

    /**
     * Returns the replica identifier for the replicated table.
     *
     * @return the replica name
     */
    String getReplicaName();
}

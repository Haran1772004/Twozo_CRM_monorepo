package com.tlc.sql.olap.internal.meta.engine.ch;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

/**
 * Runtime accessor for {@link ReplicatedEngine} parameters sourced from the
 * application configuration (application.yml of crm-server) under the
 * {@code tlc.config.database.olap.clickhouse.*} keys.
 */
public final class ReplicatedEngineProperties {

    static final String DATABASE_CONFIG_GROUP = "database";
    static final String ZOOKEEPER_PATH_KEY    = "olap.clickhouse.zookeeper-path";
    static final String REPLICA_NAME_KEY      = "olap.clickhouse.replica-name";

    private ReplicatedEngineProperties() {
    }

    /**
     * Returns the configured ZooKeeper path for replicated tables.
     *
     * @return the ZooKeeper path declared in application.yml
     */
    public static String getZookeeperPath() {
        return resolve(ZOOKEEPER_PATH_KEY);
    }

    /**
     * Returns the configured replica name for replicated tables.
     *
     * @return the replica name declared in application.yml
     */
    public static String getReplicaName() {
        return resolve(REPLICA_NAME_KEY);
    }

    private static String resolve(final String key) {
        final Configurations configurations = ConfHolder.getInstance().getProperties(DATABASE_CONFIG_GROUP);
        final String value = configurations.optConfig(key, null);

        if (value == null || value.isBlank()) {
            throw ErrorCode.get(ErrorCodes.CONFIG_NOT_FOUND,
                    String.format("Missing required configuration: tlc.config.database.%s", key));
        }
        return value;
    }
}

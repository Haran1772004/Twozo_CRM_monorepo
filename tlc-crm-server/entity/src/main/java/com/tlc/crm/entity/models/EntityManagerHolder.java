package com.tlc.crm.entity.models;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;

import com.tlc.sql.common.api.dml.Table;

import com.tlc.validator.TlcModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Holds the config managers of store type
 *
 * @author sundar
 * @version 1.0
 */
public final class EntityManagerHolder {

    private final Map<StoreType, ConfigManager<? extends TlcModel>> configManagerByStoreType;
    private final Map<StoreSubType, ConfigManager<? extends TlcModel>> configManagerByStoreSubtype;
    private final Map<StoreType, Table> tableByStoreType;

    private EntityManagerHolder() {
        this.configManagerByStoreType = new HashMap<>();
        this.configManagerByStoreSubtype = new HashMap<>();
        this.tableByStoreType = new HashMap<>();
    }

    private static class Instance {
        private static final EntityManagerHolder INSTANCE = new EntityManagerHolder();
    }

    public static EntityManagerHolder getInstance() {
        return EntityManagerHolder.Instance.INSTANCE;
    }

    /**
     * Gets the {@link ConfigManager} of given store type
     *
     * @param storeType represents the store type
     * @return the {@link ConfigManager} of the store type
     */
    public ConfigManager<? extends TlcModel> getManager(final StoreType storeType) {
        return configManagerByStoreType.get(storeType);
    }

    /**
     * Gets the {@link Table} of given store type.
     *
     * @param storeType Represents the {@link StoreType}
     * @return the {@link Table} of the store type
     */
    public Table getTable(final StoreType storeType) {
        return tableByStoreType.get(storeType);
    }

    /**
     * Gets the {@link ConfigManager} of given {@link StoreSubType}.
     *
     * @param storeSubType Represents the {@link StoreSubType}.
     * @return the {@link ConfigManager} of the {@link StoreSubType}.
     */
    public ConfigManager<? extends TlcModel> getManager(final StoreSubType storeSubType) {
        return configManagerByStoreSubtype.get(storeSubType);
    }

    /**
     * Gets the {@link ConfigManager} of given store type
     *
     * @return {@link Map}
     */
    public Map<StoreType, ConfigManager<? extends TlcModel>> getManagers() {
        return configManagerByStoreType;
    }

    /**
     * Loads the config manager of respective store type into the holder
     *
     * @param managers which contains the config managers of regarding store type
     */
    public void loadManagersByStoreType(final Map<StoreType, ConfigManager<? extends TlcModel>> managers) {
        if (Objects.nonNull(managers) && !managers.isEmpty()) {
            configManagerByStoreType.putAll(managers);
        }
    }

    /**
     * Loads the config manager of respective {@link StoreSubType} into the holder.
     *
     * @param managers which contains the config managers of regarding {@link StoreSubType}.
     */
    public void loadManagersByStoreSubtype(final Map<StoreSubType, ConfigManager<? extends TlcModel>> managers) {
        if (Objects.nonNull(managers) && !managers.isEmpty()) {
            configManagerByStoreSubtype.putAll(managers);
        }
    }

    /**
     * <p>
     * Loads the {@link Table} based on {@link StoreType}.
     * </p>
     *
     * @param tables which contains the {@link Table} of regarding {@link StoreType}.
     */
    public void loadTableByStoreType(final Map<StoreType, Table> tables) {
        tableByStoreType.putAll(tables);
    }

    /**
     * Checks whether the config manager exists or not
     *
     * @param storeType represents the store type
     * @return true if the respective config manager exists for the {@link StoreType}
     */
    public boolean exists(final StoreType storeType) {
        return configManagerByStoreType.containsKey(storeType);
    }

}

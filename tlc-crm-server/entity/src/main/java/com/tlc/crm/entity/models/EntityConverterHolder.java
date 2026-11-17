package com.tlc.crm.entity.models;

import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.validator.TlcModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Holds the converters of respective store type {@link TlcModel}
 *
 * @author sundar
 * @version 1.0
 */
public final class EntityConverterHolder {

    private final Map<StoreType, EntityConverter<? extends TlcModel>> entityConverterByStoreType;
    private final Map<StoreSubType, EntityConverter<? extends TlcModel>> entityConverterBySubType;

    private EntityConverterHolder() {
        this.entityConverterByStoreType = new HashMap<>();
        this.entityConverterBySubType = new HashMap<>();
    }

    private static class Instance {
        private static final EntityConverterHolder INSTANCE = new EntityConverterHolder();
    }

    public static EntityConverterHolder getInstance() {
        return EntityConverterHolder.Instance.INSTANCE;
    }

    /**
     * Loads the converters of respective store type into the holder
     *
     * @param converterMap which contains the converters of regarding store type
     */
    public void loadConvertersByStoreType(final Map<StoreType, EntityConverter<? extends TlcModel>> converterMap) {
        if (Objects.nonNull(converterMap) && !converterMap.isEmpty()) {
            entityConverterByStoreType.putAll(converterMap);
        }
    }

    /**
     * Loads the converters of respective store subtype into the holder.
     *
     * @param converterMap which contains the converters of regarding store subtype.
     */
    public void loadConvertersBySubType(final Map<StoreSubType, EntityConverter<? extends TlcModel>> converterMap) {
        if (Objects.nonNull(converterMap) && !converterMap.isEmpty()) {
            entityConverterBySubType.putAll(converterMap);
        }
    }

    /**
     * Gets the converters grouped by store types
     *
     * @return {@link Map}
     */
    public Map<StoreType, EntityConverter<? extends TlcModel>> getConverters() {
        return entityConverterByStoreType;
    }

    /**
     * Gets the {@link EntityConverter} of given store type
     *
     * @param storeType represents the store type
     * @return the {@link EntityConverter} of the store type
     */
    public EntityConverter<? extends TlcModel> getConverter(final StoreType storeType) {
        return entityConverterByStoreType.get(storeType);
    }

    /**
     * Gets the {@link EntityConverter} of given store subtype.
     *
     * @param storeSubType represents the store subtype.
     * @return the {@link EntityConverter} of the store subtype.
     */
    public EntityConverter<? extends TlcModel> getConverter(final StoreSubType storeSubType) {
        return entityConverterBySubType.get(storeSubType);
    }
}

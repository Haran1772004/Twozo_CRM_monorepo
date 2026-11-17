package com.tlc.crm.entity.cache.internal;

import com.tlc.cache.Cache;

import com.tlc.cache.CacheManager;
import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.entity.builder.FieldInfoBuilder;
import com.tlc.crm.entity.cache.field.FieldCache;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.manager.CrmFieldsManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.internal.Field;
import com.tlc.crm.entity.models.internal.FieldConfig;

import com.tlc.crm.security.iam.LoggedInUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Stores the field details into cache
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public abstract class AbstractFieldCache implements FieldCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFieldCache.class);

    private final Cache<Long, Map<Long, Map<String, Collection<FieldInfo>>>> fieldsCache;
    private final Cache<Long, Map<Long, Map<Long, FieldConfig>>> fieldConfigsCache;
    private final Cache<String, Collection<FieldInfo>> defaultFieldsCache;
    private final CrmFieldsManager fieldsManager;
    private final FieldInfoBuilder fieldInfoBuilder;
    private final ReentrantReadWriteLock lock;

    protected AbstractFieldCache(final Cache<Long, Map<Long, Map<String, Collection<FieldInfo>>>> fieldsCache,
                                 final Cache<Long, Map<Long, Map<Long, FieldConfig>>> fieldConfigsCache) {
        this.fieldsCache = fieldsCache;
        this.fieldConfigsCache = fieldConfigsCache;
        this.defaultFieldsCache = CacheManager.getInstance().createCache();
        this.fieldsManager = CrmFieldsManager.getFieldsManager();
        this.fieldInfoBuilder = FieldInfoBuilder.getInstance();
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Collection<Field> fields = fieldsManager.getDefaultFields();

        if (Objects.nonNull(fields) && !fields.isEmpty()) {
            final Map<String, Collection<FieldInfo>> groupedFields = fieldInfoBuilder.build(fields).stream()
                    .collect(Collectors.groupingBy(fieldInfo -> fieldInfo.fieldSource().getName(), Collectors.toCollection(ArrayList::new)));

            LOGGER.info("Total number of fields loaded into cache : {}", fields.size());
            defaultFieldsCache.putAll(groupedFields);
        } else {
            throw ErrorCode.get(EntityErrorCodes.FAILED_LOAD_CRM_FIELDS_INTO_CACHE);
        }
    }

    /**
     * <p>
     * Clears the default fields cache
     * </p>
     */
    void clearDefaultFields() {
        defaultFieldsCache.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id
     */
    @Override
    public void invalidateCache(final long orgId) {
        fieldsCache.remove(orgId);
        fieldConfigsCache.remove(orgId);
    }

    /**
     * <p>
     * Sets the organization fields into cache
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param fields Which contains the collection of {@link FieldInfo}
     */
    @Override
    public void addFieldsIntoCache(final Long orgId, final Collection<FieldInfo> fields) {
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final Map<String, Collection<FieldInfo>> sourceFields = fields.stream().collect(
                Collectors.groupingBy(fieldInfo -> fieldInfo.fieldSource().getName(),
                        Collectors.mapping(Function.identity(), Collectors.toCollection(HashSet::new))));

        final Optional<Map<Long, Map<String, Collection<FieldInfo>>>> optionalOrgFields = Optional.ofNullable(fieldsCache.get(orgId));

        LOGGER.info("Field Caching: Total count of the fields to be cached : {}", fields.size());

        if (optionalOrgFields.isEmpty()) {
            fieldsCache.put(orgId, Map.of(crmUser.id(), sourceFields));
        } else {
            final Map<Long, Map<String, Collection<FieldInfo>>> orgFields = new HashMap<>(optionalOrgFields.get());

            orgFields.put(crmUser.id(), sourceFields);
            fieldsCache.put(orgId, orgFields);
        }
    }

    /**
     * <p>
     * Adds the organization field configs into cache
     * </p>
     *
     * @param orgId        Represents the organization id
     * @param fieldConfigs Which contains the collection of {@link FieldConfig}
     */
    @Override
    public void addFieldConfigsIntoCache(final Long orgId, final Collection<FieldConfig> fieldConfigs) {
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final Map<Long, FieldConfig> sourceFieldConfigs = fieldConfigs.stream().collect(Collectors.toMap(FieldConfig::getFieldId, Function.identity()));
        final Optional<Map<Long, Map<Long, FieldConfig>>> optionalOrgFieldConfigs = Optional.ofNullable(fieldConfigsCache.get(orgId));

        LOGGER.info("Field Caching: Total count of the field configs to be cached : {}", fieldConfigs.size());

        if (optionalOrgFieldConfigs.isEmpty()) {
            fieldConfigsCache.put(orgId, Map.of(crmUser.id(), sourceFieldConfigs));
        } else {
            final Map<Long, Map<Long, FieldConfig>> orgFieldConfigs = new HashMap<>(optionalOrgFieldConfigs.get());

            orgFieldConfigs.put(crmUser.id(), sourceFieldConfigs);
            fieldConfigsCache.put(orgId, orgFieldConfigs);
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param sourceName Represents the source name
     * @return the {@link Collection} of {@link FieldInfo}
     */
    @Override
    public Collection<FieldInfo> getFieldsBySource(final Long orgId, final String sourceName) {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        readLock.lock();

        try {
            final Collection<FieldInfo> fields = new ArrayList<>();
            final Map<Long, Map<String, Collection<FieldInfo>>> orgFields = fieldsCache.get(orgId);

            if (Objects.nonNull(orgFields)) {
                final Map<String, Collection<FieldInfo>> userFields = orgFields.get(LoggedInUserInfo.getUser().id());

                if (Objects.nonNull(userFields)) {
                    final Collection<FieldInfo> sourceFields = userFields.get(sourceName);

                    if (Objects.nonNull(sourceFields)) {
                        fields.addAll(sourceFields);
                    }
                }
            }
            return fields;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id.
     * @return The collection of {@link FieldInfo}
     */
    @Override
    public Collection<FieldInfo> get(final Long orgId) {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        readLock.lock();

        try {
            final Collection<FieldInfo> fields = new ArrayList<>();
            final Map<Long, Map<String, Collection<FieldInfo>>> orgFields = fieldsCache.get(orgId);

            if (Objects.nonNull(orgFields)) {
                final Map<String, Collection<FieldInfo>> userFields = orgFields.get(LoggedInUserInfo.getUser().id());

                if (Objects.nonNull(userFields)) {
                    userFields.values().stream().filter(Objects::nonNull).forEach(fields::addAll);
                }
            }

            return fields;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * <p>
     * Retrieves a collection of source transient fields
     * </p>
     *
     * @param sourceNames Represents the source names
     * @return the {@link Collection} of {@link FieldInfo}
     */
    @Override
    public Map<String, Collection<FieldInfo>> getDefaultFieldsBySource(final Collection<String> sourceNames) {
        return defaultFieldsCache.getAll(Set.copyOf(sourceNames));
    }

    /**
     * <p>
     * Fetches the transient fields for the given field IDs.
     * </p>
     *
     * @param fieldIds Represents the field IDs.
     * @return The {@link FieldInfo}
     */
    @Override
    public Map<Long, FieldInfo> getDefaultFields(final Collection<Long> fieldIds) {
        return defaultFieldsCache.getAll().values().stream().flatMap(Collection::stream)
                .filter(fieldInfo -> fieldIds.contains(fieldInfo.id()))
                .collect(Collectors.toMap(FieldInfo::id, Function.identity()));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the field id
     * @return The {@link Map} containing the key as field id value as {@link FieldConfig}.
     */
    @Override
    public Map<Long, FieldConfig> getFieldConfigs(final Long orgId) {
        final Map<Long, Map<Long, FieldConfig>> orgFieldConfigs = fieldConfigsCache.get(orgId);

        if (Objects.nonNull(orgFieldConfigs)) {
            return orgFieldConfigs.get(LoggedInUserInfo.getUser().id());
        }
        return null;
    }
}

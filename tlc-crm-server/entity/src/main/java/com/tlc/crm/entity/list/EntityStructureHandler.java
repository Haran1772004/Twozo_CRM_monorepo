package com.tlc.crm.entity.list;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheConfig;
import com.tlc.cache.CacheManager;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.list.model.ConfigHolder;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.FieldConfigSource;
import com.tlc.crm.common.list.ListStructure;
import com.tlc.crm.common.list.ListStructureContainer;
import com.tlc.crm.common.list.StructureHandler;
import com.tlc.crm.common.list.model.SubFieldConfig;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.ModelTransformer;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.common.sql.resource.METFIELDTRANSFORMERS;
import com.tlc.crm.common.sql.resource.METMODELTRANSFORMERS;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.sql.resource.METCONFIGUREDSUBFIELD;
import com.tlc.crm.entity.sql.resource.METDATAPROVIDER;
import com.tlc.crm.entity.sql.resource.METFIELDS;
import com.tlc.crm.entity.sql.resource.METFIELDSQLSOURCE;
import com.tlc.crm.entity.sql.resource.METFIELDSQLTABLE;
import com.tlc.crm.entity.sql.resource.METSYSTEMFIELDSCONFIGURATION;
import com.tlc.crm.entity.sql.resource.OMETCONFIGUREDSUBFIELD;
import com.tlc.crm.entity.sql.resource.OMETCUSTOMFIELDSCONFIGURATION;
import com.tlc.crm.entity.sql.resource.METDYNAMICFIELDSCONFIGURATION;
import com.tlc.crm.entity.sql.resource.OMETFIELDS;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.fields.sql.resource.METSOURCE;
import com.tlc.i18n.I18nKey;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DmlUtil;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.sql.common.api.ds.ReadableDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Handler class responsible for managing the structure defining the fields and their configurations for various providers
 * </p>
 *
 * @author Abishek
 * @version 1.0
 */
public class EntityStructureHandler extends StructureHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityStructureHandler.class.getName());
    private final Cache<String, ListStructureContainer> sqlStructureCache;
    private final Cache<String, Map<Long, Long>> orgFieldsCountCache;
    private final FieldTypeService fieldTypeService;
    private final FieldService fieldService;

    private static class Helper {
        private static final EntityStructureHandler INSTANCE = new EntityStructureHandler();
    }

    public static EntityStructureHandler get() {
        return Helper.INSTANCE;
    }

    private EntityStructureHandler() {
        final CacheConfig<String, ListStructureContainer> config = new CacheConfig<>();
        config.setExpireAfterAccess(TimeUnit.MINUTES.toMillis(10));
        this.sqlStructureCache = CacheManager.getInstance().createCache(config);
        this.orgFieldsCountCache = CacheManager.getInstance().createCache();
        fieldTypeService = FieldTypeService.getInstance();
        this.fieldService = FieldService.getSecureInstance();
    }

    public void clearCache() {
        sqlStructureCache.clear();
    }

    /**
     * <p>
     * Retrieves the list structure based on the provided user ID, organization ID, and provider.
     * </p>
     *
     * @param userId   Represents the user id
     * @param orgId    Represents an organization id
     * @param provider Represents the {@link Provider}
     * @return {@link ListStructure}
     */
    protected ListStructure getStructure(final Long userId, final Long orgId, final String provider) {
        final Long orgFieldsSize = getOrgFieldsCount(orgId, provider);
        final Map<Long, Long> orgFieldsCount = orgFieldsCountCache.get(provider);

        if (Objects.nonNull(orgFieldsCount) && Objects.equals(orgFieldsSize, orgFieldsCount.get(userId))) {
            final ListStructureContainer listStructureContainer = sqlStructureCache.get(provider);

            if (Objects.isNull(listStructureContainer)) {
                return fetchAndPopulateListStructure(orgId, userId, provider, orgFieldsSize);
            } else {
                return listStructureContainer.getListStructure();
            }
        } else {
            return fetchAndPopulateListStructure(orgId, userId, provider, orgFieldsSize);
        }
    }

    /**
     * Fetches the configured fields.
     *
     * @param userId   User ID
     * @param orgId    Organisation ID
     * @param provider Provider
     * @return {@link List<FieldConfig>} which contains the field configuration.
     */
    public List<ConfigHolder> fetchConfiguredFields(final Long userId, final Long orgId, final String provider) {
        final List<ConfigHolder> configHolderList = new ArrayList<>();
        final ReadableDataStore secureReadableOrgDataStore = DataStoreProvider.getSecureReadableOrgDataStore(orgId);
        final Table systemFieldTable = Table.get(METFIELDS.TABLE);
        final Table customFieldTable = Table.get(OMETFIELDS.TABLE);
        final ListStructure listStructure = getStructure(userId, orgId, provider);
        final Long providerId = listStructure.getProviderId();
        final Table systemConfigTable = Table.get(METSYSTEMFIELDSCONFIGURATION.TABLE);
        final Table customConfigTable = Table.get(OMETCUSTOMFIELDSCONFIGURATION.TABLE);
        final Table systemSubFieldTable = Table.get(METCONFIGUREDSUBFIELD.TABLE);
        final Table customSubFieldTable = Table.get(OMETCONFIGUREDSUBFIELD.TABLE);
        final DataContainer systemFieldContainer = getSystemFieldContainer(userId, providerId, systemFieldTable,
                secureReadableOrgDataStore);
        final DataContainer customFieldContainer = getCustomFieldContainer(userId, providerId, customFieldTable,
                secureReadableOrgDataStore);
        final DataContainer dynamicFieldContainer = getDynamicFieldContainer(userId, providerId, customFieldTable);

        if (systemFieldContainer.isEmpty() && customFieldContainer.isEmpty() && dynamicFieldContainer.isEmpty()) {
            LOGGER.debug("List Action: Fields not configured, loading default fields for provider {}", provider);
            configHolderList.addAll(listStructure.getFields().stream().filter(field -> !field.isPersistent() && !field.isDynamicField())
                    .map(fieldConfig -> new ConfigHolder(fieldConfig, fieldConfig.getSubFieldsConfig())).toList());
        } else {
            LOGGER.debug("List Action: Fields configured, loading configured fields for provider {}", provider);
            configHolderList.addAll(systemFieldContainer.getRows(systemFieldTable).map(row -> {
                final FieldConfig fieldConfig = listStructure.getField(row.get(METFIELDS.NAME));
                final boolean hasSubField = fieldConfig.hasSubField();
                final Row systemFieldConfigRow = systemFieldContainer.getRow(systemConfigTable,
                        new WhereClause(Criteria.eq(systemConfigTable.getColumn(METSYSTEMFIELDSCONFIGURATION.FIELD_ID),
                                row.getPKValue())));

                fieldConfig.setOrder(systemFieldConfigRow.get(METSYSTEMFIELDSCONFIGURATION.ORDER));
                fieldConfig.setConfig(systemFieldConfigRow.get(METSYSTEMFIELDSCONFIGURATION.CONFIG));

                final Collection<SubFieldConfig> subFieldConfigs;

                if (hasSubField) {
                    final Stream<Row> subFieldConfigRows = systemFieldContainer.getRows(systemSubFieldTable,
                            new WhereClause(Criteria.eq(systemSubFieldTable.getColumn(METCONFIGUREDSUBFIELD.CONFIGURED_FIELD_ID),
                                    systemFieldConfigRow.getPKValue())));
                    final Set<Long> subFieldIds = subFieldConfigRows.map(subFieldConfigRow ->
                            (Long) subFieldConfigRow.get(METCONFIGUREDSUBFIELD.SUB_FIELD_ID)).collect(Collectors.toSet());
                    final Collection<SubFieldConfig> existingSubFieldsConfigs = fieldConfig.getSubFieldsConfig();

                    subFieldConfigs = Objects.isNull(existingSubFieldsConfigs) || existingSubFieldsConfigs.isEmpty() ? null :
                            existingSubFieldsConfigs.stream().filter(subFieldConfig -> subFieldIds.contains(subFieldConfig.getFieldSubType().id()))
                                    .toList();
                } else {
                    subFieldConfigs = null;
                }

                if (Objects.nonNull(subFieldConfigs) && !subFieldConfigs.isEmpty()) {
                    subFieldConfigs.forEach(subFieldConfig -> subFieldConfig.setConfigured(true));
                }

                return new ConfigHolder(fieldConfig, subFieldConfigs);
            }).toList());
        }

        if (!customFieldContainer.isEmpty()) {
            LOGGER.debug("List Action: Fields configured, loading configured fields for provider {}", provider);
            customFieldContainer.getRows(customFieldTable).forEach(row -> {
                final FieldConfig fieldConfig = listStructure.getField(row.getPKValue().toString());

                if (Objects.nonNull(fieldConfig)) {
                    final boolean hasSubField = fieldConfig.hasSubField();
                    final Row customFieldConfigRow = customFieldContainer.getRow(customConfigTable,
                            new WhereClause(Criteria.eq(customConfigTable.getColumn(OMETCUSTOMFIELDSCONFIGURATION.FIELD_ID),
                                    row.getPKValue())));

                    if (Objects.nonNull(customFieldConfigRow)) {
                        fieldConfig.setOrder(customFieldConfigRow.get(OMETCUSTOMFIELDSCONFIGURATION.ORDER));
                        fieldConfig.setConfig(customFieldConfigRow.get(OMETCUSTOMFIELDSCONFIGURATION.CONFIG));
                    }
                    final Collection<SubFieldConfig> subFieldConfigs;

                    fieldConfig.setOrder(customFieldConfigRow.get(OMETCUSTOMFIELDSCONFIGURATION.ORDER));
                    fieldConfig.setConfig(customFieldConfigRow.get(OMETCUSTOMFIELDSCONFIGURATION.CONFIG));

                    if (hasSubField) {
                        final Stream<Row> subFieldConfigRows = customFieldContainer.getRows(customSubFieldTable,
                                new WhereClause(Criteria.eq(customSubFieldTable.getColumn(OMETCONFIGUREDSUBFIELD.CONFIGURED_FIELD_ID),
                                        customFieldConfigRow.getPKValue())));
                        final Set<Long> subFieldIds = subFieldConfigRows.map(subFieldConfigRow ->
                                (Long) subFieldConfigRow.get(OMETCONFIGUREDSUBFIELD.SUB_FIELD_ID)).collect(Collectors.toSet());
                        subFieldConfigs = fieldConfig.getSubFieldsConfig().stream()
                                .filter(subFieldConfig -> subFieldIds.contains(subFieldConfig.getFieldSubType().id()))
                                .toList();
                    } else {
                        subFieldConfigs = null;
                    }

                    if (Objects.nonNull(subFieldConfigs) && !subFieldConfigs.isEmpty()) {
                        subFieldConfigs.forEach(subFieldConfig -> subFieldConfig.setConfigured(true));
                    }
                    configHolderList.add(new ConfigHolder(fieldConfig, subFieldConfigs));
                }
            });
        }

        if (!dynamicFieldContainer.isEmpty()) {
            LOGGER.debug("List Action: Fields configured, loading configured fields for provider {}", provider);
            dynamicFieldContainer.getRows(customFieldTable).forEach(row -> {
                final FieldConfig fieldConfig = listStructure.getField(row.get(OMETFIELDS.ALIAS_NAME));

                if (Objects.nonNull(fieldConfig)) {
                    final Table dynamicFieldConfigTable = Table.get(METDYNAMICFIELDSCONFIGURATION.TABLE);
                    final Row dynamicFieldConfigRow = dynamicFieldContainer.getRow(dynamicFieldConfigTable,
                            new WhereClause(Criteria.eq(dynamicFieldConfigTable.getColumn(METDYNAMICFIELDSCONFIGURATION.FIELD_ID),
                                    row.getPKValue())));

                    if (Objects.nonNull(dynamicFieldConfigRow)) {
                        fieldConfig.setOrder(dynamicFieldConfigRow.get(METDYNAMICFIELDSCONFIGURATION.ORDER));
                        fieldConfig.setConfig(dynamicFieldConfigRow.get(METDYNAMICFIELDSCONFIGURATION.CONFIG));
                    }

                    configHolderList.add(new ConfigHolder(fieldConfig, null));
                }
            });
        }

        return configHolderList;
    }

    /**
     * Gets the custom field container.
     *
     * @param userId               User ID
     * @param providerId           Provider ID
     * @param fieldTable           Represents the custom configuration table.
     * @param secureReadableOrgDataStore Represents the organisation data store
     * @return {@link DataContainer } Holds the updated information of custom field values
     */
    private DataContainer getCustomFieldContainer(final Long userId, final Long providerId, final Table fieldTable,
                                                  final ReadableDataStore secureReadableOrgDataStore) {
        final Table customFieldConfigTable = Table.get(OMETCUSTOMFIELDSCONFIGURATION.TABLE);
        final Table subFieldConfigTable = Table.get(OMETCONFIGUREDSUBFIELD.TABLE);
        final SelectQuery query = SelectQuery.get(fieldTable);
        final Column configuredFieldIdColumn = subFieldConfigTable.getColumn(OMETCONFIGUREDSUBFIELD.CONFIGURED_FIELD_ID);
        final Column providerIdColumn = customFieldConfigTable.getColumn(OMETCUSTOMFIELDSCONFIGURATION.PROVIDER_ID);
        final Column userIdColumn = customFieldConfigTable.getColumn(OMETCUSTOMFIELDSCONFIGURATION.USER_ID);

        query.addJoinClause(new JoinClause(fieldTable.getPKColumn(), customFieldConfigTable.getColumn(OMETCUSTOMFIELDSCONFIGURATION.FIELD_ID)));
        query.addJoinClause(new JoinClause(customFieldConfigTable.getPKColumn(), configuredFieldIdColumn, JoinClause.JoinType.LEFT));
        query.addSelectClause(List.of(fieldTable.getColumn(OMETFIELDS.NAME), customFieldConfigTable.getColumn(OMETCUSTOMFIELDSCONFIGURATION.FIELD_ID),
                configuredFieldIdColumn, subFieldConfigTable.getColumn(OMETCONFIGUREDSUBFIELD.SUB_FIELD_ID),
                providerIdColumn, userIdColumn, customFieldConfigTable.getColumn(OMETCUSTOMFIELDSCONFIGURATION.CONFIG),
                customFieldConfigTable.getColumn(OMETCUSTOMFIELDSCONFIGURATION.ORDER)));

        query.setWhereClause(new WhereClause(Criteria.eq(providerIdColumn, providerId)).and(Criteria.eq(userIdColumn, userId))
                .and(Criteria.eq(fieldTable.getColumn(OMETFIELDS.IS_ACTIVE), true)));

        return secureReadableOrgDataStore.get(query);
    }

    /**
     * <p>
     * Gets the dynamic fields container
     * </p>
     *
     * @param userId     Represents the user id
     * @param providerId Represents the provider id
     * @param fieldTable Represents field table
     * @return {@link DataContainer } Holds the updated information of dynamic field values
     */
    private DataContainer getDynamicFieldContainer(final Long userId, final Long providerId, final Table fieldTable) {
        final Table dynamicFieldConfigTable = Table.get(METDYNAMICFIELDSCONFIGURATION.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(fieldTable);
        final Column providerIdColumn = dynamicFieldConfigTable.getColumn(METDYNAMICFIELDSCONFIGURATION.PROVIDER_ID);
        final Column userIdColumn = dynamicFieldConfigTable.getColumn(METDYNAMICFIELDSCONFIGURATION.USER_ID);
        final Column dynamicFieldConfigFieldIdColumn = dynamicFieldConfigTable.getColumn(METDYNAMICFIELDSCONFIGURATION.FIELD_ID);

        selectQuery.addSelectClause(List.of(fieldTable.getColumn(OMETFIELDS.NAME), fieldTable.getColumn(OMETFIELDS.ALIAS_NAME), dynamicFieldConfigFieldIdColumn,
                providerIdColumn, userIdColumn, dynamicFieldConfigTable.getColumn(METDYNAMICFIELDSCONFIGURATION.CONFIG),
                dynamicFieldConfigTable.getColumn(METDYNAMICFIELDSCONFIGURATION.ORDER)));
        selectQuery.addJoinClause(new JoinClause(fieldTable.getPKColumn(), dynamicFieldConfigFieldIdColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(providerIdColumn, providerId)).and(Criteria.eq(userIdColumn, userId))
                .and(Criteria.eq(fieldTable.getColumn(OMETFIELDS.ORG_ID), null)));

        return DataStoreProvider.getReadableDataStore().get(selectQuery);
    }

    /**
     * Update the configured fields.
     *
     * @param userId   User ID
     * @param provider Provider ID
     * @param orgId    Organisation ID
     * @param fields   To be updated into the data container
     * @param fieldEnableType Represents the {@link FieldConfig.FieldEnableType} of the list being updated
     */
    public void updateConfiguredFields(final Long userId, final String provider, final Long orgId,
                                       final List<ConfigHolder> fields, final FieldConfig.FieldEnableType fieldEnableType) {
        final OrgDataStore orgDataStore = DataStoreProvider.getSecureOrgDataStore(orgId);
        final Table systemFieldTable = Table.get(METFIELDS.TABLE);
        final Table customFieldTable = Table.get(OMETFIELDS.TABLE);
        final List<ConfigHolder> transientFields = fields.stream().filter(field -> !field.fieldConfig().isPersistent() && !field.fieldConfig().isDynamicField()).toList();
        final List<ConfigHolder> persistentFields = fields.stream().filter(field -> field.fieldConfig().isPersistent() && !field.fieldConfig().isDynamicField()).toList();
        final List<ConfigHolder> dynamicFields = fields.stream().filter(field -> field.fieldConfig().isDynamicField()).toList();
        final ListStructure listStructure = getStructure(userId, orgId, provider);
        final Long providerId = listStructure.getProviderId();
        final DataContainer systemFieldContainer = getSystemFieldContainer(userId, providerId, systemFieldTable, orgDataStore);
        final DataContainer customFieldContainer = getCustomFieldContainer(userId, providerId, customFieldTable, orgDataStore);
        final DataContainer dynamicFieldContainer = getDynamicFieldContainer(userId, providerId, customFieldTable);
        final DataContainer targetContainer = DataContainer.create();
        final Table systemConfigTable = Table.get(METSYSTEMFIELDSCONFIGURATION.TABLE);
        final Table customConfigTable = Table.get(OMETCUSTOMFIELDSCONFIGURATION.TABLE);
        final Table dynamicFieldConfigTable =  Table.get(METDYNAMICFIELDSCONFIGURATION.TABLE);

        if (systemFieldContainer.isEmpty()) {
            LOGGER.debug("List Action: Fields not configured, Configuring fields for provider {}", providerId);
            addSystemFieldRows(targetContainer, systemConfigTable, userId, providerId, transientFields);
        } else {
            LOGGER.debug("List Action: Fields already configured, Reconfiguring fields for provider {}", providerId);
            updateSystemFieldRows(systemFieldContainer, targetContainer, systemConfigTable, userId, providerId, transientFields, fieldEnableType);
        }

        if (customFieldContainer.isEmpty()) {
            LOGGER.debug("List Action: Fields not configured, Configuring fields for provider {}", providerId);
            addCustomFieldRows(targetContainer, customConfigTable, userId, providerId, persistentFields);
        } else {
            LOGGER.debug("List Action: Fields already configured, Reconfiguring fields for provider {}", providerId);
            updateCustomFieldRows(customFieldContainer, targetContainer, customConfigTable, userId, providerId, persistentFields, fieldEnableType);
        }

        if (dynamicFieldContainer.isEmpty()) {
            LOGGER.debug("List action dynamic fields not configured, Configuring fields for provider {}", providerId);
            loadDynamicFieldRows(targetContainer, dynamicFieldConfigTable, userId, providerId, dynamicFields);
        } else {
            LOGGER.debug("List action dynamic fields already configured, Reconfiguring fields for provider {}", providerId);
            updateDynamicFieldRows(dynamicFieldContainer, targetContainer, dynamicFieldConfigTable, userId, providerId, dynamicFields, fieldEnableType);
        }
        final Collection<FieldConfig> configs = fields.stream().map(ConfigHolder::fieldConfig).toList();

        listStructure.setFieldConfigs(configs);
        loadStructureContainerIntoCache(orgId, userId, provider, listStructure);
        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * Updates the custom field rows into container.
     *
     * @param dataContainer     Holds the updated information of custom field values.
     * @param targetContainer   Holds the updated information of custom field values.
     * @param customConfigTable Represents the custom configuration table.
     * @param userId            User ID
     * @param providerId        Provider ID
     * @param configHolders     To be process the fields in to the data container
     * @param fieldEnableType   Represents the {@link FieldConfig.FieldEnableType} of the list being updated
     */
    private void updateCustomFieldRows(final DataContainer dataContainer, final DataContainer targetContainer,
                                       final Table customConfigTable, final Long userId, final Long providerId,
                                       final List<ConfigHolder> configHolders, final FieldConfig.FieldEnableType fieldEnableType) {
        final Column customFieldIdColumn = customConfigTable.getColumn(OMETCUSTOMFIELDSCONFIGURATION.FIELD_ID);
        final Table subFieldConfigTable = Table.get(OMETCONFIGUREDSUBFIELD.TABLE);
        final Column configuredFieldIdColumn = subFieldConfigTable.getColumn(OMETCONFIGUREDSUBFIELD.CONFIGURED_FIELD_ID);
        final Column subFieldIdColumn = subFieldConfigTable.getColumn(OMETCONFIGUREDSUBFIELD.SUB_FIELD_ID);

        dataContainer.indexRows(customFieldIdColumn);

        final Set<Long> customFieldConfigCurrentIds = dataContainer.getRows(customConfigTable)
                .map(row -> (Long) row.get(OMETCUSTOMFIELDSCONFIGURATION.FIELD_ID)).collect(Collectors.toSet());

        for (final ConfigHolder configHolder : configHolders) {
            final FieldConfig fieldConfig = configHolder.fieldConfig();
            final Long fieldId = fieldConfig.getFieldId();
            final int order = fieldConfig.getOrder();
            final Row customFieldConfigRow = dataContainer.getIndexedRow(customFieldIdColumn, fieldId);
            final Set<Long> subFieldConfigCurrentIds = Objects.isNull(customFieldConfigRow) ? null : dataContainer.getRows(subFieldConfigTable,
                            new WhereClause(Criteria.eq(configuredFieldIdColumn, customFieldConfigRow.getPKValue())))
                    .map(row -> (Long) row.get(OMETCONFIGUREDSUBFIELD.SUB_FIELD_ID)).collect(Collectors.toSet());

            if (customFieldConfigCurrentIds.remove(fieldId)) {
                final Collection<SubFieldConfig> subFieldConfigs = configHolder.subFieldConfigs();

                customFieldConfigRow.set(OMETCUSTOMFIELDSCONFIGURATION.ORDER, order);
                customFieldConfigRow.set(OMETCUSTOMFIELDSCONFIGURATION.CONFIG,
                        mergeEnableFlag(customFieldConfigRow.get(OMETCUSTOMFIELDSCONFIGURATION.CONFIG), fieldConfig, fieldEnableType));
                targetContainer.updateRow(customFieldConfigRow);

                if (Objects.nonNull(subFieldConfigs) && !subFieldConfigs.isEmpty()) {
                    int subFieldOrder = 0;

                    for (final SubFieldConfig subFieldConfig : subFieldConfigs) {
                        final Long subFieldId = subFieldConfig.getFieldSubType().id();

                        if (Objects.nonNull(subFieldConfigCurrentIds) && subFieldConfigCurrentIds.remove(subFieldId)) {
                            final Row subFieldConfigRow = dataContainer.getRow(subFieldConfigTable,
                                    new WhereClause(Criteria.eq(subFieldIdColumn, subFieldId)));

                            if (Objects.nonNull(subFieldConfigRow)) {
                                subFieldConfigRow.set(OMETCONFIGUREDSUBFIELD.ORDER, ++subFieldOrder);
                                targetContainer.updateRow(subFieldConfigRow);
                            }
                        } else {
                            final Row subFieldConfigRow = new Row(subFieldConfigTable);

                            subFieldConfigRow.set(OMETCONFIGUREDSUBFIELD.CONFIGURED_FIELD_ID, customFieldConfigRow.getPKValue());
                            subFieldConfigRow.set(OMETCONFIGUREDSUBFIELD.SUB_FIELD_ID, subFieldConfig.getFieldSubType().id());
                            subFieldConfigRow.set(OMETCONFIGUREDSUBFIELD.ORDER, order);
                            subFieldConfig.setConfigured(true);

                            targetContainer.addNewRow(subFieldConfigRow);
                        }
                    }
                }
            } else {
                loadCustomFieldConfigRow(customConfigTable, subFieldConfigTable, userId, order, providerId, targetContainer,
                        configHolder);
            }

            if (Objects.nonNull(subFieldConfigCurrentIds)) {
                for (final Long subFieldConfigCurrentId : subFieldConfigCurrentIds) {
                    final Row subFieldConfigRow = dataContainer.getRow(subFieldConfigTable,
                            new WhereClause(Criteria.eq(subFieldIdColumn, subFieldConfigCurrentId)));

                    if (Objects.nonNull(subFieldConfigRow)) {
                        targetContainer.deleteRow(subFieldConfigRow);
                    }
                }
            }
        }

        for (final Long customFieldConfigCurrentId : customFieldConfigCurrentIds) {
            final Row customFieldConfigRow = dataContainer.getIndexedRow(customFieldIdColumn, customFieldConfigCurrentId);

            if (Objects.nonNull(customFieldConfigRow)) {
                targetContainer.deleteRow(customFieldConfigRow);
            }
        }
    }

    /**
     * <p>
     * Updates the dynamic fields configuration rows
     * </p>
     *
     * @param dataContainer      Holds the updated information of dynamic field values.
     * @param targetContainer    Holds the updated information of dynamic field values.
     * @param dynamicConfigTable Represents the dynamic configuration table.
     * @param userId             Represents the user id
     * @param providerId         Represents the provider id
     * @param configHolders      The configuration holder containing the field configuration.
     * @param fieldEnableType    Represents the {@link FieldConfig.FieldEnableType} of the list being updated
     */
    private void updateDynamicFieldRows(final DataContainer dataContainer, final DataContainer targetContainer,
                                        final Table dynamicConfigTable, final Long userId, final Long providerId,
                                        final List<ConfigHolder> configHolders, final FieldConfig.FieldEnableType fieldEnableType) {
        final Column dynamicFieldIdColumn = dynamicConfigTable.getColumn(METDYNAMICFIELDSCONFIGURATION.FIELD_ID);

        dataContainer.indexRows(dynamicFieldIdColumn);

        final Set<Long> dynamicFieldConfigIds = dataContainer.getRows(dynamicConfigTable)
                .map(row -> (Long) row.get(METDYNAMICFIELDSCONFIGURATION.FIELD_ID)).collect(Collectors.toSet());

        for (final ConfigHolder configHolder : configHolders) {
            final FieldConfig fieldConfig = configHolder.fieldConfig();
            final Long fieldId = fieldConfig.getFieldId();
            final int order = fieldConfig.getOrder();
            final Row dynamicFieldConfigRow = dataContainer.getIndexedRow(dynamicFieldIdColumn, fieldId);

            if (dynamicFieldConfigIds.remove(fieldId)) {
                dynamicFieldConfigRow.set(METDYNAMICFIELDSCONFIGURATION.ORDER, order);
                dynamicFieldConfigRow.set(METDYNAMICFIELDSCONFIGURATION.CONFIG,
                        mergeEnableFlag(dynamicFieldConfigRow.get(METDYNAMICFIELDSCONFIGURATION.CONFIG), fieldConfig, fieldEnableType));
                targetContainer.updateRow(dynamicFieldConfigRow);
            } else {
                loadDynamicFieldConfigRow(dynamicConfigTable, userId, order, providerId, targetContainer,
                        configHolder);
            }
        }

        for (final Long dynamicFieldConfigCurrentId : dynamicFieldConfigIds) {
            final Row dynamicFieldConfigRow = dataContainer.getIndexedRow(dynamicFieldIdColumn, dynamicFieldConfigCurrentId);

            if (Objects.nonNull(dynamicFieldConfigRow)) {
                targetContainer.deleteRow(dynamicFieldConfigRow);
            }
        }
    }

    /**
     * Adds the custom field row into the container
     *
     * @param dataContainer     Holds the updated information of custom field values.
     * @param customConfigTable Represents the custom configuration table.
     * @param userId            User ID
     * @param providerId        Provider ID
     * @param configHolders     To be process the fields in to the data container
     */
    private void addCustomFieldRows(final DataContainer dataContainer, final Table customConfigTable, final Long userId,
                                    final Long providerId, final List<ConfigHolder> configHolders) {
        for (final ConfigHolder configHolder : configHolders) {
            final FieldConfig fieldConfig = configHolder.fieldConfig();

            if (!fieldConfig.isHidden() && !fieldConfig.isDynamicField()) {
                loadCustomFieldConfigRow(customConfigTable, Table.get(OMETCONFIGUREDSUBFIELD.TABLE), userId,
                        fieldConfig.getOrder(), providerId, dataContainer, configHolder);
            }
        }
    }

    /**
     * <p>
     * Loads dynamic field configuration rows to the data container.
     * </p>
     *
     * @param dataContainer           Holds the updated information of custom field values
     * @param dynamicFieldConfigTable Represents the dynamic field configurations table
     * @param userId                  Represents the user id
     * @param providerId              Represents the provider id
     * @param configHolders           The configuration holder containing the field configuration
     */
    private void loadDynamicFieldRows(final DataContainer dataContainer, final Table dynamicFieldConfigTable, final Long userId,
                                      final Long providerId, final List<ConfigHolder> configHolders) {
        for (final ConfigHolder configHolder : configHolders) {
            final FieldConfig fieldConfig = configHolder.fieldConfig();

            if (!fieldConfig.isHidden()) {
                loadDynamicFieldConfigRow(dynamicFieldConfigTable, userId, fieldConfig.getOrder(), providerId, dataContainer, configHolder);
            }
        }
    }

    /**
     * <p>
     * Loads a dynamic field configuration row into the data container.
     * </p>
     *
     * @param dynamicFieldConfigTable Represents the dynamic field configurations table
     * @param userId                  Represents the user id
     * @param order                   Represents the order of the field configuration
     * @param providerId              Represents the provider id
     * @param targetContainer         The data container to add the new row to
     * @param configHolder            The configuration holder containing the field configuration.
     */
    private void loadDynamicFieldConfigRow(final Table dynamicFieldConfigTable, final Long userId,
                                           final int order, final Long providerId, final DataContainer targetContainer,
                                           final ConfigHolder configHolder) {
        final Row dynamicFieldConfigRow = new Row(dynamicFieldConfigTable);
        final FieldConfig fieldConfig = configHolder.fieldConfig();

        dynamicFieldConfigRow.set(METDYNAMICFIELDSCONFIGURATION.USER_ID, userId);
        dynamicFieldConfigRow.set(METDYNAMICFIELDSCONFIGURATION.PROVIDER_ID, providerId);
        dynamicFieldConfigRow.set(METDYNAMICFIELDSCONFIGURATION.FIELD_ID, fieldConfig.getFieldId());
        dynamicFieldConfigRow.set(METDYNAMICFIELDSCONFIGURATION.ORDER, order);
        dynamicFieldConfigRow.set(METDYNAMICFIELDSCONFIGURATION.CONFIG, fieldConfig.getConfigAsString());
        targetContainer.addNewRow(dynamicFieldConfigRow);
    }

    /**
     * Loads custom field configuration row.
     *
     * @param customFieldConfigTable Represents the custom configuration table.
     * @param customSubFieldTable    Represents the custom sub-field configuration table.
     * @param userId                 Represents user id
     * @param order                  Represents the sorting order
     * @param providerId             Provider ID
     * @param configHolder           To be process the fields in to the data container
     */
    private void loadCustomFieldConfigRow(final Table customFieldConfigTable, final Table customSubFieldTable, final Long userId,
                                          final int order, final Long providerId, final DataContainer targetContainer,
                                          final ConfigHolder configHolder) {
        final Row customFieldConfigRow = new Row(customFieldConfigTable);
        final Collection<SubFieldConfig> subFieldConfigs = configHolder.subFieldConfigs();
        final FieldConfig fieldConfig = configHolder.fieldConfig();

        customFieldConfigRow.set(OMETCUSTOMFIELDSCONFIGURATION.USER_ID, userId);
        customFieldConfigRow.set(OMETCUSTOMFIELDSCONFIGURATION.PROVIDER_ID, providerId);
        customFieldConfigRow.set(OMETCUSTOMFIELDSCONFIGURATION.FIELD_ID, fieldConfig.getFieldId());
        customFieldConfigRow.set(OMETCUSTOMFIELDSCONFIGURATION.ORDER, order);
        customFieldConfigRow.set(OMETCUSTOMFIELDSCONFIGURATION.CONFIG, fieldConfig.getConfigAsString());
        targetContainer.addNewRow(customFieldConfigRow);

        if (Objects.nonNull(subFieldConfigs) && !subFieldConfigs.isEmpty()) {
            for (final SubFieldConfig subFieldConfig : subFieldConfigs) {
                if (!subFieldConfig.isConfigured()) {
                    final Row subFieldConfigRow = new Row(customSubFieldTable);

                    subFieldConfigRow.set(OMETCONFIGUREDSUBFIELD.CONFIGURED_FIELD_ID, customFieldConfigRow.getPKValue());
                    subFieldConfigRow.set(OMETCONFIGUREDSUBFIELD.SUB_FIELD_ID, subFieldConfig.getFieldSubType().id());
                    subFieldConfigRow.set(OMETCONFIGUREDSUBFIELD.ORDER, order);
                    subFieldConfig.setConfigured(true);

                    targetContainer.addNewRow(subFieldConfigRow);
                }
            }
        }
    }

    /**
     * Fetches the structure of fields
     *
     * @param provider Represent provider name
     * @param userId   Represent user id
     * @param orgId    Represent org id
     * @return {@link ListStructure}
     */
    private ListStructure fetchStructure(final String provider, final Long userId, final Long orgId) {
        LOGGER.debug("List Action: Loading Structure for provider {}", provider);
        final List<FieldConfig> fieldConfigList = new ArrayList<>();
        final Table providerTable = Table.get(METDATAPROVIDER.TABLE);
        final Table systemFieldsTable = Table.get(METFIELDS.TABLE);
        final Table transformerTable = Table.get(METFIELDTRANSFORMERS.TABLE);
        final Table entityTransformerTable = Table.get(METMODELTRANSFORMERS.TABLE);
        final Table dataSourceTable = Table.get(METFIELDSQLSOURCE.TABLE);
        final Table dataSourceSQLTable = Table.get(METFIELDSQLTABLE.TABLE);
        final Column fieldIdColumn = dataSourceTable.getColumn(METFIELDSQLSOURCE.FIELD_ID);
        final Table systemConfigTable = Table.get(METSYSTEMFIELDSCONFIGURATION.TABLE);
        final Table customConfigTable = Table.get(OMETCUSTOMFIELDSCONFIGURATION.TABLE);
        final Table systemSubFieldTable = Table.get(METCONFIGUREDSUBFIELD.TABLE);
        final Table customSubFieldTable = Table.get(OMETCONFIGUREDSUBFIELD.TABLE);
        final ReadableDataStore secureReadableOrgDataStore = DataStoreProvider.getSecureReadableOrgDataStore(orgId);
        final SelectQuery selectQuery = SelectQuery.get(systemFieldsTable);
        final List<JoinClause> innerJoins = DmlUtil.getJoins(systemFieldsTable, dataSourceTable, dataSourceSQLTable);
        final Collection<FieldInfo> orgFields = getOrgFields(orgId, provider, providerTable);
        final Column configuredFieldIdColumn = systemSubFieldTable.getColumn(METCONFIGUREDSUBFIELD.CONFIGURED_FIELD_ID);
        final Column filedIdColumn = systemConfigTable.getColumn(METSYSTEMFIELDSCONFIGURATION.FIELD_ID);
        final Column orderColumn = systemConfigTable.getColumn(METSYSTEMFIELDSCONFIGURATION.ORDER);
        final Column customOrderColumn = customConfigTable.getColumn(OMETCUSTOMFIELDSCONFIGURATION.ORDER);

        selectQuery.addJoinClause(new JoinClause(systemFieldsTable.getColumn(METFIELDS.PROVIDER_ID), providerTable.getPKColumn()));
        selectQuery.addJoinClause(innerJoins);
        selectQuery.addJoinClause(new JoinClause(systemFieldsTable.getColumn(METFIELDS.TRANSFORMER_ID), transformerTable.getPKColumn(), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(providerTable.getColumn(METDATAPROVIDER.TRANSFORMER_ID), entityTransformerTable.getPKColumn(), JoinClause.JoinType.LEFT));
        selectQuery.addSelectClause(providerTable, systemFieldsTable, dataSourceTable,
                dataSourceSQLTable, transformerTable, entityTransformerTable);
        selectQuery.addOrderByClause(new OrderByClause(systemFieldsTable.getColumn(METFIELDS.ORDER), OrderByClause.OrderType.ASCENDING));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(providerTable.getColumn(METDATAPROVIDER.NAME), provider)));

        final DataContainer dataContainer = secureReadableOrgDataStore.get(selectQuery);
        final Row providerRow = dataContainer.getRow(providerTable);

        if (Objects.isNull(providerRow)) {
            throw ErrorCode.get(EntityErrorCodes.PROVIDER_NOT_FOUND, "i18n.entity.list.provider.not.found");
        }
        final SelectQuery systemFieldConfigQuery = SelectQuery.get(systemConfigTable);

        systemFieldConfigQuery.addJoinClause(new JoinClause(systemConfigTable.getPKColumn(), configuredFieldIdColumn, JoinClause.JoinType.LEFT));
        systemFieldConfigQuery.setWhereClause(new WhereClause(Criteria.eq(systemConfigTable.getColumn(METSYSTEMFIELDSCONFIGURATION.USER_ID), userId))
                .and(Criteria.eq(systemConfigTable.getColumn(METSYSTEMFIELDSCONFIGURATION.PROVIDER_ID), providerRow.getPKValue())));

        final DataContainer systemConfigContainer = secureReadableOrgDataStore.get(systemFieldConfigQuery);
        final Long providerId = providerRow.get(METDATAPROVIDER.ID);
        final Long providerTransformerId = providerRow.get(METDATAPROVIDER.TRANSFORMER_ID);
        final ModelTransformer modelTransformer;

        if (Objects.isNull(providerTransformerId)) {
            modelTransformer = null;
        } else {
            final Row row = dataContainer.getRow(entityTransformerTable, providerTransformerId);
            modelTransformer = convertRowToTransformer(row);
        }
        final boolean hasSystemFieldConfigurations = systemConfigContainer.size(systemConfigTable) > 0;

        dataContainer.indexRows(fieldIdColumn);
        dataContainer.getRows(systemFieldsTable).forEach(fieldRow ->
        {
            final Long fieldId = fieldRow.get(METFIELDS.ID);
            final String fieldIdAsString = fieldId.toString();
            final String fieldName = fieldRow.get(METFIELDS.NAME);
            final I18nKey displayName = fieldRow.get(METFIELDS.DISPLAY_NAME);
            final Boolean primary = fieldRow.get(METFIELDS.PRIMARY);
            final Long fieldTypeId = fieldRow.get(METFIELDS.FIELD_TYPE_ID);
            final FieldType fieldType = Objects.isNull(fieldTypeId) ? null : fieldTypeService.get(fieldTypeId);
            final StoreType storeType = Objects.isNull(fieldType) ? null : fieldType.storeType();
            final boolean hasSubField = Objects.nonNull(storeType) && storeType.hasSubtypes();
            final Row configRow = hasSystemFieldConfigurations ? systemConfigContainer.getRow(systemConfigTable,
                    new WhereClause(Criteria.eq(filedIdColumn, fieldId))) : null;
            final boolean hasConfig = Objects.nonNull(configRow);
            Collection<SubFieldConfig> subFieldsConfig = null;

            if (hasSubField) {
                final Map<String, Map<StoreSubType, String>> fieldTypeTableDetails = fieldTypeService.getTableDetails(fieldType.storeType());

                if (Objects.nonNull(fieldTypeTableDetails) && !fieldTypeTableDetails.isEmpty()) {
                    subFieldsConfig = new ArrayList<>();

                    for (final Map.Entry<String, Map<StoreSubType, String>> fieldTypeTableDetailsEntry : fieldTypeTableDetails.entrySet()) {

                        for (final Map.Entry<StoreSubType, String> subTypeColumnEntry : fieldTypeTableDetailsEntry.getValue().entrySet()) {
                            final FieldSubType fieldSubType = fieldTypeService.getFieldSubTypeByStoreType(storeType, subTypeColumnEntry.getKey());
                            final Row subConfigRow = hasConfig ? systemConfigContainer.getRow(systemSubFieldTable,
                                    new WhereClause(Criteria.eq(configuredFieldIdColumn, configRow.getPKValue()))) : null;
                            final SubFieldConfig subFieldConfig = new SubFieldConfig(fieldSubType,
                                    new FieldConfigSource(fieldTypeTableDetailsEntry.getKey(), subTypeColumnEntry.getValue(), 0, 0));

                            subFieldConfig.setConfigured(Objects.nonNull(subConfigRow));
                            subFieldsConfig.add(subFieldConfig);
                        }
                    }
                }
            }
            final int sortFlag = fieldRow.get(METFIELDS.SORT);
            final Boolean searchable = fieldRow.get(METFIELDS.SEARCHABLE);
            final Boolean hidden = fieldRow.get(METFIELDS.HIDDEN);
            final Long transformerId = fieldRow.get(METFIELDS.TRANSFORMER_ID);
            final String fieldConfigInfo = fieldRow.get(METFIELDS.CONFIG_INFO);
            final boolean isConfigured = hasSystemFieldConfigurations ? hasConfig : !hidden;

            final Row dataSourceRow = dataContainer.getIndexedRow(fieldIdColumn, fieldId);
            final Long tableId = dataSourceRow.get(METFIELDSQLSOURCE.TABLE_ID);
            final String columnName = dataSourceRow.get(METFIELDSQLSOURCE.COLUMN);
            final Integer column_type = dataSourceRow.get(METFIELDSQLSOURCE.TYPE);
            final Integer flag = dataSourceRow.get(METFIELDSQLSOURCE.FLAG);
            final int order;

            if (Objects.nonNull(configRow)) {
                order = configRow.get(orderColumn);
            } else {
                order = fieldRow.get(METFIELDS.ORDER);
            }

            final Row tableRow = dataContainer.getRow(dataSourceSQLTable, tableId);
            final String tableName = tableRow.get(METFIELDSQLTABLE.TABLE_NAME);
            final FieldConfigSource fieldSource = new FieldConfigSource(tableName, columnName, column_type, flag);
            final FieldTransformer fieldTransformer;

            if (transformerId != null) {
                final Row row = dataContainer.getRow(transformerTable, transformerId);
                fieldTransformer = buildTransformer(row);
            } else {
                fieldTransformer = null;
            }

            fieldConfigList.add(new FieldConfig(fieldId, fieldIdAsString, fieldName, displayName, primary, sortFlag, order, searchable,
                    hidden, new FieldConfig.Config(fieldConfigInfo), false, fieldSource, fieldType, subFieldsConfig, hasSubField, fieldTransformer,
                    isConfigured, isSortable(fieldType) && (sortFlag == 1 || sortFlag == 2), false));
        });

        if (!orgFields.isEmpty()) {
            final DataContainer customFieldContainer = getCustomFieldContainer(userId, providerId,
                    Table.get(OMETFIELDS.TABLE), secureReadableOrgDataStore);
            final boolean hasCustomFieldConfigurations = customFieldContainer.size(customConfigTable) > 0;
            final DataContainer transformerContainer = DataStoreProvider.getReadableDataStore().get(transformerTable,
                    new WhereClause(Criteria.notEq(transformerTable.getPKColumn(), null)));

            orgFields.forEach(field -> {
                final Row configRow = hasCustomFieldConfigurations ? customFieldContainer.getRow(customConfigTable,
                        new WhereClause(Criteria.eq(customConfigTable.getColumn(OMETCUSTOMFIELDSCONFIGURATION.FIELD_ID),
                                field.id()))) : null;
                final boolean hasConfig = Objects.nonNull(configRow);
                final StoreType storeType = field.storeType();
                final Row transformerRow = transformerContainer.getRow(transformerTable, new WhereClause(Criteria
                        .eq(transformerTable.getColumn(METFIELDTRANSFORMERS.STORE_TYPE), storeType.getName())));
                final String fieldIdString = field.idAsString();
                final boolean hasSubtypes = storeType.hasSubtypes();
                final boolean isConfigured = hasCustomFieldConfigurations && hasConfig;
                Collection<SubFieldConfig> subFieldsConfig = null;

                if (hasSubtypes) {
                    final Map<String, Map<StoreSubType, String>> fieldTypeTableDetails = fieldTypeService.getTableDetails(storeType);

                    if (Objects.nonNull(fieldTypeTableDetails) && !fieldTypeTableDetails.isEmpty()) {
                        subFieldsConfig = new ArrayList<>();

                        for (final Map.Entry<String, Map<StoreSubType, String>> fieldTypeTableDetailsEntry : fieldTypeTableDetails.entrySet()) {

                            for (final Map.Entry<StoreSubType, String> subTypeColumnEntry : fieldTypeTableDetailsEntry.getValue().entrySet()) {
                                final FieldSubType fieldSubType = fieldTypeService.getFieldSubTypeByStoreType(storeType, subTypeColumnEntry.getKey());
                                final Row subConfigRow = hasConfig ? dataContainer.getRow(customSubFieldTable,
                                        new WhereClause(Criteria.eq(customSubFieldTable
                                                .getColumn(OMETCONFIGUREDSUBFIELD.CONFIGURED_FIELD_ID), configRow.getPKValue()))) : null;
                                final SubFieldConfig subFieldConfig = new SubFieldConfig(fieldSubType,
                                        new FieldConfigSource(fieldTypeTableDetailsEntry.getKey(), subTypeColumnEntry.getValue(), 0, 0));

                                subFieldConfig.setConfigured(Objects.nonNull(subConfigRow));
                                subFieldsConfig.add(subFieldConfig);
                            }
                        }
                    }
                }
                final FieldTransformer fieldTransformer = Objects.nonNull(transformerRow)
                        ? buildTransformer(transformerRow) : null;
                final FieldType fieldType = fieldTypeService.getFieldTypeByStoreType(storeType);
                final int order;

                if (Objects.isNull(configRow)) {
                    order = 0;
                } else {
                    order = configRow.get(customOrderColumn);
                }

                fieldConfigList.add(new FieldConfig(field.id(), fieldIdString, fieldIdString, field.i18nName(), false, 1, order, true,
                        false, null, true, new FieldConfigSource(field.table().getName(), field.column().getColumnName(), 0, 1),
                        fieldTypeService.getFieldTypeByStoreType(storeType), subFieldsConfig, hasSubtypes,
                        isTransformerNeeded(storeType) ? fieldTransformer : null, isConfigured, isSortable(fieldType), false));
            });
        }
        loadDynamicFieldsIntoFieldConfigs(orgId, userId, providerId, provider, providerTable, fieldConfigList);

        return new ListStructure(providerId, fieldConfigList, modelTransformer);
    }

    /**
     * Checks the transformer needed for given store type
     *
     * @param storeType Refer the {@link StoreType}
     * @return Whether transformer is needed return true otherwise false
     */
    private boolean isTransformerNeeded(final StoreType storeType) {
        return storeType.hasSubtypes() || switch (storeType) {
            case CONTACT, COMPANY, DROPDOWN, MULTI_SELECT, TIME, DATE -> true;
            default -> false;
        };
    }

    /**
     * Get org Fields.
     *
     * @param orgId         Organisation Id
     * @param providerName  Provider name
     * @param providerTable Provider Table
     * @return Collection of fieldInfo
     */
    private Collection<FieldInfo> getOrgFields(final Long orgId, final String providerName, final Table providerTable) {
        final Table sourceTable = Table.get(METSOURCE.TABLE);
        final SelectQuery sourceSelect = SelectQuery.get(providerTable);

        sourceSelect.addJoinClause(new JoinClause(providerTable.getColumn(METDATAPROVIDER.SOURCE_ID), sourceTable.getPKColumn()));
        sourceSelect.setWhereClause(new WhereClause(Criteria.eq(providerTable.getColumn(METDATAPROVIDER.NAME), providerName)));

        final Row sourceRow = DataStoreProvider.getReadableDataStore().get(sourceSelect).getRow(sourceTable);

        return Objects.isNull(sourceRow) || !(boolean) sourceRow.get(METSOURCE.IS_COMMON) ? List.of() : fieldService.getFieldsBySource(orgId,
                        (String) sourceRow.get(METSOURCE.NAME)).stream()
                .filter(fieldInfo -> fieldInfo.isPersistent() && fieldInfo.isActive()).toList();
    }

    /**
     * <p>
     * Gets the dynamic fields
     * </p>
     *
     * @param orgId         Represents the organization id
     * @param providerName  Represents the provider name
     * @param providerTable Represents the provider table
     * @return The collection of dynamic {@link FieldInfo}
     */
    private Collection<FieldInfo> getDynamicFields(final long orgId, final String providerName, final Table providerTable) {
        final Table sourceTable = Table.get(METSOURCE.TABLE);
        final SelectQuery sourceSelect = SelectQuery.get(providerTable);

        sourceSelect.addJoinClause(new JoinClause(providerTable.getColumn(METDATAPROVIDER.SOURCE_ID), sourceTable.getPKColumn()));
        sourceSelect.setWhereClause(new WhereClause(Criteria.eq(providerTable.getColumn(METDATAPROVIDER.NAME), providerName)));
        final Row sourceRow = DataStoreProvider.getReadableDataStore().get(sourceSelect).getRow(sourceTable);

        return Objects.isNull(sourceRow) || !(boolean) sourceRow.get(METSOURCE.IS_COMMON) ? List.of() : fieldService.getFieldsBySource(orgId,
                        (String) sourceRow.get(METSOURCE.NAME)).stream()
                .filter(fieldInfo -> fieldInfo.presetConfig().isDynamicField() && fieldInfo.isActive()).toList();
    }

    /**
     * Get count for org fields
     *
     * @param orgId        Organisation ID
     * @param providerName providerName
     * @return the total org field count
     */
    private Long getOrgFieldsCount(final Long orgId, final String providerName) {
        final Table providerTable = Table.get(METDATAPROVIDER.TABLE);
        final Table sourceTable = Table.get(METSOURCE.TABLE);
        final Table fieldsTable = Table.get(OMETFIELDS.TABLE);
        final CountQuery countQuery = CountQuery.get(fieldsTable);
        final Column sourcePkColumn = sourceTable.getPKColumn();

        countQuery.addJoinClause(new JoinClause(fieldsTable.getColumn(OMETFIELDS.SOURCE_ID), sourcePkColumn));
        countQuery.addJoinClause(new JoinClause(sourcePkColumn, providerTable.getColumn(METDATAPROVIDER.SOURCE_ID)));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(providerTable.getColumn(METDATAPROVIDER.NAME), providerName)).and(Criteria.eq(fieldsTable.getColumn(OMETFIELDS.IS_ACTIVE), true)));

        return DataStoreProvider.getSecureOrgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Resolves the {@link ListStructure} and loaded in to the cache.
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param userId        Represents the user id
     * @param provider      Represents the {@link Provider}
     * @param orgFieldsSize Represents the size of org fields exists count
     * @return {@link ListStructure}
     */
    private ListStructure fetchAndPopulateListStructure(final long orgId, final long userId, final String provider, final Long orgFieldsSize) {
        final ListStructure listStructure = fetchStructure(provider, userId, orgId);

        loadStructureContainerIntoCache(orgId, userId, provider, listStructure);
        orgFieldsCountCache.put(provider, Map.of(userId, orgFieldsSize));

        return listStructure;
    }

    /**
     * <p>
     * Loads the {@link ListStructureContainer} in to the cache.
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param userId        Represents the user id
     * @param provider      Represents the provider
     * @param listStructure Represents the {@link ListStructure}
     */
    protected void loadStructureContainerIntoCache(final long orgId, final long userId, final String provider, final ListStructure listStructure) {
        final ListStructureContainer listStructureContainer = new ListStructureContainer();

        listStructureContainer.setListStructure(listStructure);
        listStructureContainer.setOrgId(orgId);
        listStructureContainer.setUserId(userId);
        listStructureContainer.setProviderId(listStructure.getProviderId());

        sqlStructureCache.put(provider, listStructureContainer);
    }

    /**
     * <p>
     * Loads the dynamic fields into collection of {@link FieldConfig}
     * </p>
     *
     * @param orgId         Represents the org id
     * @param userId        Represents the user id
     * @param providerId    Represents the provider id
     * @param provider      Represents the provider name
     * @param providerTable Represents teh provider table
     * @param fieldConfigs  Which contains the collection of {@link FieldConfig}
     */
    private void loadDynamicFieldsIntoFieldConfigs(final long orgId, final long userId, final long providerId, final String provider,
                                                   final Table providerTable, final Collection<FieldConfig> fieldConfigs) {
        final Collection<FieldInfo> dynamicFields = getDynamicFields(orgId, provider, providerTable);

        if (!dynamicFields.isEmpty()) {
            final Table dynamicFieldConfigTable = Table.get(METDYNAMICFIELDSCONFIGURATION.TABLE);
            final Column dynamicOrderColumn = dynamicFieldConfigTable.getColumn(METDYNAMICFIELDSCONFIGURATION.ORDER);

            if (!dynamicFields.isEmpty()) {
                final DataContainer dynamicFieldContainer = getDynamicFieldContainer(userId, providerId, Table.get(OMETFIELDS.TABLE));
                final boolean hasDynamicFieldConfigurations = dynamicFieldContainer.size(dynamicFieldConfigTable) > 0;

                dynamicFields.forEach(field -> {
                    final Row configRow = hasDynamicFieldConfigurations ? dynamicFieldContainer.getRow(dynamicFieldConfigTable,
                            new WhereClause(Criteria.eq(dynamicFieldConfigTable.getColumn(OMETCUSTOMFIELDSCONFIGURATION.FIELD_ID),
                                    field.id()))) : null;
                    final boolean hasConfig = Objects.nonNull(configRow);
                    final StoreType storeType = field.storeType();
                    final String fieldIdString = field.idAsString();
                    final boolean hasSubtypes = false;
                    final boolean isConfigured = hasDynamicFieldConfigurations && hasConfig;
                    final int order;

                    if (Objects.isNull(configRow)) {
                        order = 0;
                    } else {
                        order = configRow.get(dynamicOrderColumn);
                    }

                    fieldConfigs.add(new FieldConfig(field.id(), fieldIdString, field.aliasName(), field.i18nName(), false, 1, order, true,
                            false, null, false, new FieldConfigSource(field.table().getName(), field.fieldStore().column(), 0, 1),
                            fieldTypeService.getFieldTypeByStoreType(storeType), Collections.emptyList(), hasSubtypes,
                            null, isConfigured, false, true));
                });
            }
        }
    }
}


package com.tlc.crm.common.list;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheConfig;
import com.tlc.cache.CacheManager;
import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.list.model.ConfigHolder;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.FieldConfigSource;
import com.tlc.crm.common.list.model.SubFieldConfig;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.ModelTransformer;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.common.sql.resource.METCONFIGUREDSUBFIELD;
import com.tlc.crm.common.sql.resource.METDATAPROVIDER;
import com.tlc.crm.common.sql.resource.METFIELDS;
import com.tlc.crm.common.sql.resource.METFIELDSQLSOURCE;
import com.tlc.crm.common.sql.resource.METFIELDSQLTABLE;
import com.tlc.crm.common.sql.resource.METFIELDTRANSFORMERS;
import com.tlc.crm.common.sql.resource.METMODELTRANSFORMERS;
import com.tlc.crm.common.sql.resource.METSYSTEMFIELDSCONFIGURATION;
import com.tlc.i18n.I18nKey;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
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
public class DefaultStructureHandler extends StructureHandler {
    private final Cache<String, ListStructure> sqlStructureCache;
    private final FieldTypeService fieldTypeService;
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStructureHandler.class.getName());

    private static class Helper {
        private static final DefaultStructureHandler INSTANCE = new DefaultStructureHandler();
    }

    public static DefaultStructureHandler get() {
        return Helper.INSTANCE;
    }

    private DefaultStructureHandler() {
        final CacheConfig<String, ListStructure> config = new CacheConfig<>();
        config.setExpireAfterAccess(TimeUnit.MINUTES.toMillis(10));
        this.sqlStructureCache = CacheManager.getInstance().createCache(config);
        this.fieldTypeService = FieldTypeService.getInstance();
    }

    public void clearCache() {
        sqlStructureCache.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @param userId   Represents the user id
     * @param orgId    Represents an organization id
     * @param provider Represents the provider name
     * @return The {@link ListStructure}
     */
    public ListStructure getStructure(final Long userId, final Long orgId, final String provider) {
        return sqlStructureCache.computeIfAbsent(provider, k -> fetchStructure(k, orgId, userId));
    }

    /**
     * Fetches the configured fields.
     *
     * @param userId   User ID
     * @param orgId    Organisation ID
     * @param provider Provider
     * @return {@link List< FieldConfig >} which contains the field configuration.
     */
    public List<ConfigHolder> fetchConfiguredFields(final Long userId, final Long orgId, final String provider) {
        final List<ConfigHolder> configHolderList = new ArrayList<>();
        final ReadableDataStore readableDataStore = DataStoreProvider.getSecureReadableOrgDataStore(orgId);
        final Table systemFieldTable = Table.get(METFIELDS.TABLE);
        final ListStructure listStructure = getStructure(userId, orgId, provider);
        final Long providerId = listStructure.getProviderId();
        final Table systemConfigTable = Table.get(METSYSTEMFIELDSCONFIGURATION.TABLE);
        final Table systemSubFieldTable = Table.get(METCONFIGUREDSUBFIELD.TABLE);
        final DataContainer systemFieldContainer = getSystemFieldContainer(userId, providerId, systemFieldTable,
                readableDataStore);

        if (systemFieldContainer.isEmpty()) {
            LOGGER.debug("List Action: Fields not configured, loading default fields for provider {}", provider);
            configHolderList.addAll(listStructure.getFields().stream().filter(field -> !field.isHidden() && !field.isPersistent())
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

        return configHolderList;
    }

    /**
     * Update the configured fields.
     *
     * @param userId   User ID
     * @param provider Provider ID
     * @param orgId    Organisation ID
     * @param fields   To be updated into the data container
     */
    public void updateConfiguredFields(final Long userId, final String provider, final Long orgId,
                                       final List<ConfigHolder> fields) {
        final OrgDataStore orgDataStore = DataStoreProvider.getSecureOrgDataStore(orgId);
        final Table systemFieldTable = Table.get(METFIELDS.TABLE);
        final List<ConfigHolder> transientFields = fields.stream().filter(field -> !field.fieldConfig().isPersistent()).toList();
        final ListStructure listStructure = getStructure(userId, orgId, provider);
        final Long providerId = listStructure.getProviderId();
        final DataContainer systemFieldContainer = getSystemFieldContainer(userId, providerId, systemFieldTable, orgDataStore);
        final DataContainer targetContainer = DataContainer.create();
        final Table systemConfigTable = Table.get(METSYSTEMFIELDSCONFIGURATION.TABLE);

        if (systemFieldContainer.isEmpty()) {
            LOGGER.debug("List Action: Fields not configured, Configuring fields for provider {}", providerId);
            addSystemFieldRows(targetContainer, systemConfigTable, userId, providerId, transientFields);
        } else {
            LOGGER.debug("List Action: Fields already configured, Reconfiguring fields for provider {}", providerId);
            updateSystemFieldRows(systemFieldContainer, targetContainer, systemConfigTable, userId, providerId, transientFields);
        }

        final Collection<FieldConfig> configs = fields.stream().map(ConfigHolder::fieldConfig).toList();

        listStructure.setFieldConfigs(configs);
        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * <p>
     * Fetches the structure defining the fields and their configurations from the specified provider
     * </p>
     *
     * @param providerName Represents the provider name
     * @param orgId        Represents the organization ID
     * @param userId       Represents the user ID
     * @return The {@link ListStructure} details
     */
    private ListStructure fetchStructure(final String providerName, final Long orgId, final Long userId) {
        LOGGER.debug("Loading Structure for provider {}", providerName);

        final List<FieldConfig> fieldConfigList = new ArrayList<>();
        final Table providerTable = Table.get(METDATAPROVIDER.TABLE);
        final Table systemFieldsTable = Table.get(METFIELDS.TABLE);
        final Table transformerTable = Table.get(METFIELDTRANSFORMERS.TABLE);
        final Table entityTransformerTable = Table.get(METMODELTRANSFORMERS.TABLE);
        final Table dataSourceTable = Table.get(METFIELDSQLSOURCE.TABLE);
        final Table dataSourceSQLTable = Table.get(METFIELDSQLTABLE.TABLE);
        final Column fieldIdColumn = dataSourceTable.getColumn(METFIELDSQLSOURCE.FIELD_ID);
        final ReadableDataStore readableDataStore = DataStoreProvider.getSecureReadableOrgDataStore(orgId);
        final SelectQuery selectQuery = SelectQuery.get(providerTable);
        final List<JoinClause> innerJoins = DmlUtil.getJoins(providerTable, systemFieldsTable, dataSourceTable, dataSourceSQLTable);

        selectQuery.addJoinClause(innerJoins);
        selectQuery.addJoinClause(new JoinClause(systemFieldsTable.getColumn(METFIELDS.TRANSFORMER_ID), transformerTable.getPKColumn(), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(providerTable.getColumn(METDATAPROVIDER.TRANSFORMER_ID), entityTransformerTable.getPKColumn(), JoinClause.JoinType.LEFT));
        selectQuery.addSelectClause(providerTable, systemFieldsTable, dataSourceTable,
                dataSourceSQLTable, transformerTable, entityTransformerTable);
        selectQuery.addOrderByClause(new OrderByClause(systemFieldsTable.getColumn(METFIELDS.ORDER), OrderByClause.OrderType.ASCENDING));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(providerTable.getColumn(METDATAPROVIDER.NAME), providerName)));

        final DataContainer dataContainer = readableDataStore.get(selectQuery);
        final Row providerRow = dataContainer.getRow(providerTable);

        if (Objects.isNull(providerRow)) {
            throw ErrorCode.get(CommonErrorCodes.LIST_PROVIDER_NOT_FOUND, "i18n.entity.list.provider.not.found");
        }
        final Long providerId = providerRow.get(METDATAPROVIDER.ID);
        final Long providerTransformerId = providerRow.get(METDATAPROVIDER.TRANSFORMER_ID);
        final ModelTransformer modelTransformer;

        if (Objects.isNull(providerTransformerId)) {
            modelTransformer = null;
        } else {
            final Row row = dataContainer.getRow(entityTransformerTable, providerTransformerId);
            modelTransformer = convertRowToTransformer(row);
        }
        dataContainer.indexRows(fieldIdColumn);
        dataContainer.getRows(systemFieldsTable).forEach(fieldRow ->
        {
            final Long fieldId = fieldRow.get(METFIELDS.ID);
            final String fieldIdAsString = fieldId.toString();
            final String fieldName = fieldRow.get(METFIELDS.NAME);
            final I18nKey displayName = fieldRow.get(METFIELDS.DISPLAY_NAME);
            final Boolean primary = fieldRow.get(METFIELDS.PRIMARY);

            final int sortFlag = fieldRow.get(METFIELDS.SORT);
            final Boolean searchable = fieldRow.get(METFIELDS.SEARCHABLE);
            final Boolean hidden = fieldRow.get(METFIELDS.HIDDEN);
            final Long transformerId = fieldRow.get(METFIELDS.TRANSFORMER_ID);
            final String fieldConfigInfo = fieldRow.get(METFIELDS.CONFIG_INFO);
            final Long fieldTypeId = fieldRow.get(METFIELDS.FIELD_TYPE_ID);
            final FieldType fieldType = Objects.isNull(fieldTypeId) ? null : fieldTypeService.get(fieldTypeId);
            final StoreType storeType = Objects.isNull(fieldType) ? null : fieldType.storeType();
            final boolean hasSubField = Objects.nonNull(storeType) && storeType.hasSubtypes();

            final Row dataSourceRow = dataContainer.getIndexedRow(fieldIdColumn, fieldId);
            final Long tableId = dataSourceRow.get(METFIELDSQLSOURCE.TABLE_ID);
            final String columnName = dataSourceRow.get(METFIELDSQLSOURCE.COLUMN);
            final Integer column_type = dataSourceRow.get(METFIELDSQLSOURCE.TYPE);
            final Integer flag = dataSourceRow.get(METFIELDSQLSOURCE.FLAG);
            final int order = fieldRow.get(METFIELDS.ORDER);
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
            Collection<SubFieldConfig> subFieldsConfig = null;

            if (hasSubField) {
                final Map<String, Map<StoreSubType, String>> fieldTypeTableDetails = fieldTypeService.getTableDetails(fieldType.storeType());

                if (Objects.nonNull(fieldTypeTableDetails) && !fieldTypeTableDetails.isEmpty()) {
                    subFieldsConfig = new ArrayList<>();

                    for (final Map.Entry<String, Map<StoreSubType, String>> fieldTypeTableDetailsEntry : fieldTypeTableDetails.entrySet()) {

                        for (final Map.Entry<StoreSubType, String> subTypeColumnEntry : fieldTypeTableDetailsEntry.getValue().entrySet()) {
                            final FieldSubType fieldSubType = fieldTypeService.getFieldSubTypeByStoreType(storeType, subTypeColumnEntry.getKey());

                            final SubFieldConfig subFieldConfig = new SubFieldConfig(fieldSubType,
                                    new FieldConfigSource(fieldTypeTableDetailsEntry.getKey(), subTypeColumnEntry.getValue(), 0, 0));

                            subFieldsConfig.add(subFieldConfig);
                        }
                    }
                }
            }

            fieldConfigList.add(new FieldConfig(fieldId, fieldIdAsString, fieldName, displayName, primary, sortFlag, order, searchable,
                    hidden, new FieldConfig.Config(fieldConfigInfo), false, fieldSource, fieldType, subFieldsConfig, hasSubField, fieldTransformer,
                    false, isSortable(fieldType), false));
        });
        return new ListStructure(providerId, fieldConfigList, modelTransformer);
    }
}
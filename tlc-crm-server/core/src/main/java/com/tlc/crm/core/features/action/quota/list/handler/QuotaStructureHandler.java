package com.tlc.crm.core.features.action.quota.list.handler;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheConfig;
import com.tlc.cache.CacheManager;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.StringUtil;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.list.ListStructure;
import com.tlc.crm.common.list.StructureHandler;
import com.tlc.crm.common.list.model.ConfigHolder;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.FieldConfigSource;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.ModelTransformer;
import com.tlc.crm.common.sql.resource.METDATAPROVIDER;
import com.tlc.crm.common.sql.resource.METFIELDS;
import com.tlc.crm.common.sql.resource.METFIELDSQLSOURCE;
import com.tlc.crm.common.sql.resource.METFIELDSQLTABLE;
import com.tlc.crm.common.sql.resource.METFIELDTRANSFORMERS;
import com.tlc.crm.common.sql.resource.METMODELTRANSFORMERS;
import com.tlc.crm.core.features.action.quota.list.fields.QuotaListFields;
import com.tlc.crm.core.sql.resource.OMQFFORECASTCATEGORYFIELDCONFIG;
import com.tlc.crm.entity.forecastcategory.fields.ForecastCategoryFields;
import com.tlc.crm.entity.forecastcategory.manager.ForecastCategoryManager;
import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;
import com.tlc.crm.entity.sql.resource.METSYSTEMFIELDSCONFIGURATION;
import com.tlc.crm.entity.sql.resource.OMQFFORECASTCATEGORY;
import com.tlc.crm.security.sql.DataStoreProvider;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Handles quota-related {@link ListStructure} creation and retrieval.
 *
 * @author kishore J
 * @version 1.0
 */
public class QuotaStructureHandler extends StructureHandler {

    private static final String QUOTA_CONFIG = "quotaConfig";

    private final ForecastCategoryManager forecastCategoryManager;
    private final Cache<String, ListStructure> structureCache;

    private QuotaStructureHandler() {
        final CacheConfig<String, ListStructure> config = new CacheConfig<>();

        config.setExpireAfterAccess(TimeUnit.MINUTES.toMillis(10));
        structureCache = CacheManager.getInstance().createCache(config);
        forecastCategoryManager = ForecastCategoryManager.getInstance();
    }

    private static class InstanceHolder {
        private static final QuotaStructureHandler INSTANCE = new QuotaStructureHandler();
    }

    /**
     * <p>
     * Fetches the {@link QuotaStructureHandler}
     * </p>
     *
     * @return The {@link QuotaStructureHandler}
     */
    public static QuotaStructureHandler get() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Fetches the forecast category config table
     * </p>
     *
     * @return The forecast category config table
     */
    private Table getForecastCategoryConfigTable() {
        return Table.get(OMQFFORECASTCATEGORYFIELDCONFIG.TABLE);
    }

    /**
     * <p>
     * Fetches the system field table
     * </p>
     *
     * @return the system field table
     */
    private Table getSystemFieldTable() {
        return Table.get(METFIELDS.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param userId   Represents the user id
     * @param orgId    Represents an organization id
     * @param provider Represents the provider name
     * @return The {@link ListStructure}
     */
    @Override
    public ListStructure getStructure(final Long userId, final Long orgId, final String provider) {
        final String cacheKey = buildCacheKey(orgId, provider);
        final ListStructure existingListStructure = structureCache.get(cacheKey);

        if (Objects.isNull(existingListStructure) || isStructureOutdated(existingListStructure, orgId)) {
            final ListStructure listStructure = fetchStructure(provider, orgId, userId);

            structureCache.put(cacheKey, listStructure);
            return listStructure;
        }
        return existingListStructure;
    }

    /**
     * <p>
     * Fetches the configured fields.
     * </p>
     *
     * @param orgId    Represents the organization ID
     * @param userId   Represents the user ID
     * @param provider Represents the provider name
     * @return The collection of {@link FieldConfig} which contains the field configuration.
     */
    public Collection<FieldConfig> fetchConfiguredFields(final Long orgId, final Long userId, final String provider) {
        final List<FieldConfig> fieldConfigs = new ArrayList<>();
        final ReadableDataStore secureReadableOrgDataStore = DataStoreProvider.getSecureReadableOrgDataStore(orgId);
        final ListStructure listStructure = getStructure(userId, orgId, provider);
        final Long providerId = listStructure.getProviderId();
        final Table systemFieldTable = getSystemFieldTable();
        final DataContainer systemFieldContainer = getSystemFieldContainer(userId, providerId, systemFieldTable, secureReadableOrgDataStore);
        final DataContainer forecastCategoryFieldContainer = getForecastCategoryFieldContainer(orgId, userId, providerId);

        if (systemFieldContainer.isEmpty() && forecastCategoryFieldContainer.isEmpty()) {
            fieldConfigs.addAll(listStructure.getFields().stream().toList());
        } else {
            final Table forecastCategoryTable = forecastCategoryManager.getTable();
            final Table forecastCategoryConfigTable = getForecastCategoryConfigTable();
            final Table systemConfigTable = Table.get(METSYSTEMFIELDSCONFIGURATION.TABLE);

            fieldConfigs.addAll(systemFieldContainer.getRows(systemFieldTable).map(row -> {
                final FieldConfig fieldConfig = listStructure.getField(row.get(METFIELDS.NAME));
                final Row systemFieldConfigRow = systemFieldContainer.getRow(systemConfigTable,
                        new WhereClause(Criteria.eq(systemConfigTable.getColumn(METSYSTEMFIELDSCONFIGURATION.FIELD_ID),
                                row.getPKValue())));

                if (Objects.nonNull(systemFieldConfigRow)) {
                    fieldConfig.setOrder(systemFieldConfigRow.get(METSYSTEMFIELDSCONFIGURATION.ORDER));
                    fieldConfig.setConfig(systemFieldConfigRow.get(METSYSTEMFIELDSCONFIGURATION.CONFIG));
                }

                return fieldConfig;
            }).toList());

            fieldConfigs.addAll(forecastCategoryFieldContainer.getRows(forecastCategoryTable).map(row -> {
                final FieldConfig fieldConfig = listStructure.getField(row.get(OMQFFORECASTCATEGORY.ID).toString());

                if (Objects.nonNull(fieldConfig)) {
                    final Row categoryFieldConfigRow = forecastCategoryFieldContainer.getRow(forecastCategoryConfigTable,
                            new WhereClause(Criteria.eq(forecastCategoryConfigTable.getColumn(OMQFFORECASTCATEGORYFIELDCONFIG.CATEGORY_ID),
                                    row.getPKValue())));

                    if (Objects.nonNull(categoryFieldConfigRow)) {
                        fieldConfig.setOrder(categoryFieldConfigRow.get(OMQFFORECASTCATEGORYFIELDCONFIG.ORDER));
                        fieldConfig.setConfig(categoryFieldConfigRow.get(OMQFFORECASTCATEGORYFIELDCONFIG.CONFIG));
                    }
                }
                return fieldConfig;
            }).toList());
        }
        return fieldConfigs;
    }

    /**
     * <p>
     * Update the configured fields.
     * </p>
     *
     * @param userId       Represents the  User ID
     * @param provider     Represents the   Provider ID
     * @param orgId        Represents the  Organisation ID
     * @param fieldConfigs Which contains the collection of {@link FieldConfig}
     */
    public void updateConfiguredFields(final Long orgId, final Long userId, final String provider,
                                       final Collection<FieldConfig> fieldConfigs) {
        final OrgDataStore orgDataStore = DataStoreProvider.getSecureOrgDataStore(orgId);
        final Table systemFieldTable = getSystemFieldTable();
        final Collection<FieldConfig> quotaFields = new ArrayList<>();
        final Collection<FieldConfig> forecastCategoryFields = new ArrayList<>();

        for (final FieldConfig fieldConfig : fieldConfigs) {

            if (fieldConfig.isPersistent()) {
                forecastCategoryFields.add(fieldConfig);
            } else {
                quotaFields.add(fieldConfig);
            }
        }
        final ListStructure listStructure = getStructure(userId, orgId, provider);
        final Long providerId = listStructure.getProviderId();
        final DataContainer targetContainer = DataContainer.create();
        final Table systemConfigTable = Table.get(METSYSTEMFIELDSCONFIGURATION.TABLE);
        final DataContainer systemFieldContainer = getSystemFieldContainer(userId, providerId, systemFieldTable, orgDataStore);
        final DataContainer forecastCategoryFieldContainer = getForecastCategoryFieldContainer(orgId, userId, providerId);
        final List<ConfigHolder> quotaConfigHolders = new ArrayList<>(quotaFields.size());

        for (final FieldConfig fieldConfig : quotaFields) {
            quotaConfigHolders.add(new ConfigHolder(fieldConfig, null));
        }

        if (systemFieldContainer.isEmpty()) {
            addSystemFieldRows(targetContainer, systemConfigTable, userId, providerId, quotaConfigHolders);
        } else {
            updateSystemFieldRows(systemFieldContainer, targetContainer, systemConfigTable, userId, providerId, quotaConfigHolders);
        }

        if (forecastCategoryFieldContainer.isEmpty()) {
            loadForecastFieldRows(targetContainer, forecastCategoryFieldContainer, orgId, userId, providerId, forecastCategoryFields);
        } else {
            updateForecastCategoryFieldRows(userId, providerId, forecastCategoryFields, targetContainer, forecastCategoryFieldContainer);
        }
        listStructure.setFieldConfigs(fieldConfigs);
        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads forecast category field configuration rows to the data container.
     * </p>
     *
     * @param targetContainer   Holds the updated information of custom field values
     * @param userId            Represents the user id
     * @param providerId        Represents the provider id
     * @param fieldConfigs      Which contains the collection of {@link FieldConfig}
     */
    private void loadForecastFieldRows(final DataContainer targetContainer, DataContainer sourceContainer,
                                       final Long orgId, final Long userId, final Long providerId,
                                       final Collection<FieldConfig> fieldConfigs) {
        final DataContainer forecastCategoryContainer;

        if (Objects.isNull(sourceContainer) || sourceContainer.isEmpty()) {
            forecastCategoryContainer = getForecastCategoryFieldContainer(orgId, userId, providerId);
        } else {
            forecastCategoryContainer = sourceContainer;
        }
        final Table forecastCategoryConfigTable = getForecastCategoryConfigTable();
        final Column categoryIdColumn = forecastCategoryConfigTable.getColumn(OMQFFORECASTCATEGORYFIELDCONFIG.CATEGORY_ID);
        final Collection<Long> categoryIds = new HashSet<>();

        forecastCategoryContainer.indexRows(categoryIdColumn);

        for (final FieldConfig quotaFieldConfig : fieldConfigs) {
            final Long categoryId = quotaFieldConfig.getFieldId();

            if (categoryIds.contains(categoryId)) {
                continue;
            }
            categoryIds.add(categoryId);

            final Row forecastCategoryRow = forecastCategoryContainer.getIndexedRow(categoryIdColumn, categoryId);
            final boolean isExistingRecord = Objects.nonNull(forecastCategoryRow)
                    && Objects.nonNull(forecastCategoryRow.get(OMQFFORECASTCATEGORYFIELDCONFIG.PROVIDER_ID))
                    && Objects.equals(forecastCategoryRow.get(OMQFFORECASTCATEGORYFIELDCONFIG.USER_ID), userId)
                    && Objects.equals(forecastCategoryRow.get(OMQFFORECASTCATEGORYFIELDCONFIG.PROVIDER_ID), providerId);

            if (isExistingRecord) {
                forecastCategoryRow.set(OMQFFORECASTCATEGORYFIELDCONFIG.ORDER, quotaFieldConfig.getOrder());
                forecastCategoryRow.set(OMQFFORECASTCATEGORYFIELDCONFIG.CONFIG, quotaFieldConfig.getConfigAsString());
                targetContainer.updateRow(forecastCategoryRow);
            } else {
                final Row existingRow = DataStoreProvider.getOrgDataStore(orgId).get(forecastCategoryConfigTable,
                        new WhereClause(Criteria.eq(forecastCategoryConfigTable.getColumn(OMQFFORECASTCATEGORYFIELDCONFIG.CATEGORY_ID), categoryId))
                                .and(Criteria.eq(forecastCategoryConfigTable.getColumn(OMQFFORECASTCATEGORYFIELDCONFIG.USER_ID), userId))
                                .and(Criteria.eq(forecastCategoryConfigTable.getColumn(OMQFFORECASTCATEGORYFIELDCONFIG.PROVIDER_ID), providerId))).getRow(forecastCategoryConfigTable);

                if (Objects.isNull(existingRow)) {
                    loadForecastCategoryFieldConfigRow(forecastCategoryConfigTable, userId, quotaFieldConfig.getOrder(), providerId, targetContainer, quotaFieldConfig);
                } else {
                    existingRow.set(OMQFFORECASTCATEGORYFIELDCONFIG.ORDER, quotaFieldConfig.getOrder());
                    existingRow.set(OMQFFORECASTCATEGORYFIELDCONFIG.CONFIG, quotaFieldConfig.getConfigAsString());
                    targetContainer.updateRow(existingRow);
                }
            }
        }
    }

    /**
     * <p>
     * Loads a forecast category field configuration row into the data container.
     * </p>
     *
     * @param categoryFieldConfigTable Represents the forecast category field configurations table
     * @param userId                   Represents the user id
     * @param order                    Represents the order of the field configuration
     * @param providerId               Represents the provider id
     * @param targetContainer          The data container to add the new row to
     * @param fieldConfig              Represents the{@link FieldConfig}
     */
    private void loadForecastCategoryFieldConfigRow(final Table categoryFieldConfigTable, final Long userId,
                                                    final int order, final Long providerId, final DataContainer targetContainer,
                                                    final FieldConfig fieldConfig) {
        final Row forecastCategoryFieldConfigRow = new Row(categoryFieldConfigTable);

        forecastCategoryFieldConfigRow.set(OMQFFORECASTCATEGORYFIELDCONFIG.USER_ID, userId);
        forecastCategoryFieldConfigRow.set(OMQFFORECASTCATEGORYFIELDCONFIG.PROVIDER_ID, providerId);
        forecastCategoryFieldConfigRow.set(OMQFFORECASTCATEGORYFIELDCONFIG.CATEGORY_ID, fieldConfig.getFieldId());
        forecastCategoryFieldConfigRow.set(OMQFFORECASTCATEGORYFIELDCONFIG.ORDER, order);
        forecastCategoryFieldConfigRow.set(OMQFFORECASTCATEGORYFIELDCONFIG.CONFIG, fieldConfig.getConfigAsString());
        targetContainer.addNewRow(forecastCategoryFieldConfigRow);
    }

    /**
     * <p>
     * Updates the forecast category fields configuration rows
     * </p>
     *
     * @param userId            Represents the user id
     * @param providerId        Represents the provider id
     * @param quotaFieldConfigs Which contains the collection of {@link FieldConfig}
     * @param targetContainer   Holds the updated information of forecast category field values.
     * @param sourceContainer   Holds the updated information of forecast category field values.
     */
    private void updateForecastCategoryFieldRows(final Long userId, final Long providerId,
                                                 final Collection<FieldConfig> quotaFieldConfigs, final DataContainer targetContainer, final DataContainer sourceContainer) {
        final Table forecastCategoryConfigTable = getForecastCategoryConfigTable();
        final Column categoryIdColumn = forecastCategoryConfigTable.getColumn(OMQFFORECASTCATEGORYFIELDCONFIG.CATEGORY_ID);

        sourceContainer.indexRows(categoryIdColumn);
        final Set<Long> categoryIds = sourceContainer.getRows(forecastCategoryConfigTable)
                .map(row -> (Long) row.get(OMQFFORECASTCATEGORYFIELDCONFIG.CATEGORY_ID)).collect(Collectors.toSet());
        final Collection<Long> processedCategoryIds = new HashSet<>();

        for (final FieldConfig fieldConfig : quotaFieldConfigs) {
            final Long categoryId = fieldConfig.getFieldId();

            if (processedCategoryIds.contains(categoryId)) {
                continue;
            }
            processedCategoryIds.add(categoryId);
            final int order = fieldConfig.getOrder();
            final Row forecateCategoryFieldConfigRow = sourceContainer.getIndexedRow(categoryIdColumn, categoryId);

            if (categoryIds.remove(categoryId)) {
                forecateCategoryFieldConfigRow.set(OMQFFORECASTCATEGORYFIELDCONFIG.ORDER, order);
                forecateCategoryFieldConfigRow.set(OMQFFORECASTCATEGORYFIELDCONFIG.CONFIG, fieldConfig.getConfigAsString());
                targetContainer.updateRow(forecateCategoryFieldConfigRow);
            } else {
                loadForecastCategoryFieldConfigRow(forecastCategoryConfigTable, userId, order, providerId, targetContainer, fieldConfig);
            }
        }

        for (final Long categoryId : categoryIds) {
            final Row forecateCategoryFieldConfigRow = sourceContainer.getIndexedRow(categoryIdColumn, categoryId);

            if (Objects.nonNull(forecateCategoryFieldConfigRow)) {
                targetContainer.deleteRow(forecateCategoryFieldConfigRow);
            }
        }
    }

    /**
     * /**
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
        final Table providerTable = Table.get(METDATAPROVIDER.TABLE);
        final Table systemFieldsTable = getSystemFieldTable();
        final Table transformerTable = Table.get(METFIELDTRANSFORMERS.TABLE);
        final Table entityTransformerTable = Table.get(METMODELTRANSFORMERS.TABLE);
        final Table dataSourceTable = Table.get(METFIELDSQLSOURCE.TABLE);
        final Table dataSourceSQLTable = Table.get(METFIELDSQLTABLE.TABLE);
        final Column fieldIdColumn = dataSourceTable.getColumn(METFIELDSQLSOURCE.FIELD_ID);
        final ReadableDataStore readableDataStore = DataStoreProvider.getSecureReadableOrgDataStore(orgId);
        final SelectQuery selectQuery = SelectQuery.get(providerTable);
        final List<JoinClause> innerJoins = DmlUtil.getJoins(providerTable, systemFieldsTable, dataSourceTable, dataSourceSQLTable);

        selectQuery.addSelectClause(providerTable, systemFieldsTable, dataSourceTable, dataSourceSQLTable, transformerTable, entityTransformerTable);
        selectQuery.addJoinClause(innerJoins);
        selectQuery.addJoinClause(new JoinClause(systemFieldsTable.getColumn(METFIELDS.TRANSFORMER_ID), transformerTable.getPKColumn(), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(providerTable.getColumn(METDATAPROVIDER.TRANSFORMER_ID), entityTransformerTable.getPKColumn(), JoinClause.JoinType.LEFT));
        selectQuery.addOrderByClause(new OrderByClause(systemFieldsTable.getColumn(METFIELDS.ORDER), OrderByClause.OrderType.ASCENDING));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(providerTable.getColumn(METDATAPROVIDER.NAME), providerName)));
        final DataContainer dataContainer = readableDataStore.get(selectQuery);
        final Row providerRow = dataContainer.getRow(providerTable);

        if (Objects.isNull(providerRow)) {
            throw ErrorCode.get(CommonErrorCodes.LIST_PROVIDER_NOT_FOUND, "i18n.entity.list.provider.not.found");
        }
        final List<FieldConfig> fieldConfigList = new ArrayList<>();
        final Long providerId = providerRow.get(METDATAPROVIDER.ID);
        final Long providerTransformerId = providerRow.get(METDATAPROVIDER.TRANSFORMER_ID);
        final ModelTransformer modelTransformer;

        if (Objects.isNull(providerTransformerId)) {
            modelTransformer = null;
        } else {
            modelTransformer = convertRowToTransformer(dataContainer.getRow(entityTransformerTable, providerTransformerId));
        }
        dataContainer.indexRows(fieldIdColumn);

        dataContainer.getRows(systemFieldsTable).forEach(fieldRow -> {
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

            final Row dataSourceRow = dataContainer.getIndexedRow(fieldIdColumn, fieldId);
            final Long tableId = dataSourceRow.get(METFIELDSQLSOURCE.TABLE_ID);
            final String columnName = dataSourceRow.get(METFIELDSQLSOURCE.COLUMN);
            final Integer column_type = dataSourceRow.get(METFIELDSQLSOURCE.TYPE);
            final Integer flag = dataSourceRow.get(METFIELDSQLSOURCE.FLAG);
            final int order = fieldRow.get(METFIELDS.ORDER);
            final Row tableRow = dataContainer.getRow(dataSourceSQLTable, tableId);
            final String tableName = tableRow.get(METFIELDSQLTABLE.TABLE_NAME);
            final FieldConfigSource fieldSource = new FieldConfigSource(tableName, columnName, column_type, flag);
            final FieldTransformer fieldTransformer = Objects.isNull(transformerId) ? null : buildTransformer(dataContainer.getRow(transformerTable, transformerId));

            fieldConfigList.add(new FieldConfig(fieldId, fieldIdAsString, fieldName, displayName, primary, sortFlag, order, searchable,
                    hidden, new FieldConfig.Config(fieldConfigInfo), false, fieldSource, null, null, false, fieldTransformer,
                    false, false, false));
        });

        loadForecastCategoryFields(orgId, userId, providerId, fieldConfigList);
        return new ListStructure(providerId, fieldConfigList, modelTransformer);
    }

    /**
     * <p>
     * Loads forecast category fields for the given organization, user, and provider
     * </p>
     *
     * @param orgId        The organization identifier
     * @param userId       The user identifier
     * @param providerId   The provider identifier
     * @param fieldConfigs The collection into which forecast category field, configurations will be loaded
     */
    private void loadForecastCategoryFields(final long orgId, final long userId, final long providerId,
                                            final Collection<FieldConfig> fieldConfigs) {
        final Table forecastCategoryTable = forecastCategoryManager.getTable();
        final Table transformerTable = Table.get(METFIELDTRANSFORMERS.TABLE);
        final Table forecastCategoryConfigTable = getForecastCategoryConfigTable();
        final Column categoryIdColumn = forecastCategoryConfigTable.getColumn(OMQFFORECASTCATEGORYFIELDCONFIG.CATEGORY_ID);
        final Collection<ForecastCategory> forecastCategories = forecastCategoryManager.getAll(orgId);
        final Column orderColumn = forecastCategoryConfigTable.getColumn(OMQFFORECASTCATEGORYFIELDCONFIG.ORDER);
        final Column configColumn = forecastCategoryConfigTable.getColumn(OMQFFORECASTCATEGORYFIELDCONFIG.CONFIG);
        final DataContainer forecastCategoryContainer = getForecastCategoryFieldContainer(orgId, userId, providerId);
        final Row transformerRow = readableDataStore().get(transformerTable,
                new WhereClause(Criteria.eq(transformerTable.getColumn(METFIELDTRANSFORMERS.NAME), ListActionFields.FORECAST_CATEGORY))).getRow(transformerTable);
        final FieldTransformer fieldTransformer = Objects.isNull(transformerRow) ? null : buildTransformer(transformerRow);

        forecastCategories.forEach(forecastCategory -> {
            final Long forecastCategoryId = forecastCategory.id();
            final String categoryName = forecastCategory.getName();
            final Row configRow = forecastCategoryContainer.getRow(forecastCategoryTable, new WhereClause(Criteria.eq(categoryIdColumn, forecastCategoryId)));
            final boolean hasConfig = Objects.nonNull(configRow);
            final String fieldIdString = forecastCategoryId.toString();
            final int order;
            final String config;

            if (hasConfig) {
                order = configRow.get(orderColumn);
                config = configRow.get(configColumn);
            } else {
                order = 0;
                config = getDefaultConfig().toString();
            }
            final boolean isBestCase = forecastCategory.isBestCase();
            final boolean isCommitted = forecastCategory.isCommittedCategory();
            final JsonObject configJson = Json.object(config);

            if (isBestCase || isCommitted) {
                final JsonObject quotaConfig = Objects.requireNonNullElseGet(configJson.optJsonObject(QUOTA_CONFIG), Json::object);

                quotaConfig.put(ForecastCategoryFields.TYPE, isBestCase ? QuotaListFields.BEST_CASE : QuotaListFields.COMMITTED);
                configJson.put(QUOTA_CONFIG, quotaConfig);
            }
            final I18nKey forecastCategoryDisplayName = forecastCategory.getDisplayName();
            final I18nKey displayName = Objects.nonNull(forecastCategoryDisplayName) && !isBestCase ? forecastCategoryDisplayName : new I18nKey(categoryName);

            fieldConfigs.add(new FieldConfig(forecastCategoryId, fieldIdString, fieldIdString, displayName,
                    false, 1, order, true, false, new FieldConfig.Config(configJson), true, new FieldConfigSource(forecastCategoryTable.getName(), null, 0, 1),
                    null, Collections.emptyList(), false, fieldTransformer, hasConfig, false, false));
        });
    }

    /**
     * <p>
     * Fetches the forecast category field {@link DataContainer}
     * </p>
     *
     * @param orgId      Represents the organization ID
     * @param userId     Represents the user ID
     * @param providerId Represents the provider id
     * @return The forecast category field {@link DataContainer}
     */
    private DataContainer getForecastCategoryFieldContainer(final long orgId, final long userId, final long providerId) {
        final Table forecastCategoryTable = forecastCategoryManager.getTable();
        final Table forecastCategoryConfigTable = getForecastCategoryConfigTable();
        final SelectQuery selectQuery = SelectQuery.get(forecastCategoryTable);
        final Column providerIdColumn = forecastCategoryConfigTable.getColumn(OMQFFORECASTCATEGORYFIELDCONFIG.PROVIDER_ID);
        final Column userIdColumn = forecastCategoryConfigTable.getColumn(OMQFFORECASTCATEGORYFIELDCONFIG.USER_ID);
        final Column categoryCategoryId = forecastCategoryConfigTable.getColumn(OMQFFORECASTCATEGORYFIELDCONFIG.CATEGORY_ID);

        selectQuery.addSelectClause(List.of(forecastCategoryTable.getColumn(OMQFFORECASTCATEGORY.NAME), categoryCategoryId,
                providerIdColumn, userIdColumn, forecastCategoryConfigTable.getColumn(OMQFFORECASTCATEGORYFIELDCONFIG.CONFIG),
                forecastCategoryConfigTable.getColumn(OMQFFORECASTCATEGORYFIELDCONFIG.ORDER)));
        selectQuery.addJoinClause(new JoinClause(forecastCategoryTable.getPKColumn(), categoryCategoryId, JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(providerIdColumn, providerId)).and(Criteria.eq(userIdColumn, userId)));

        return DataStoreProvider.getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Builds and returns the default configuration for quota list fields.
     * </p>
     *
     * @return a {@link JsonObject} containing default field configuration values
     */
    private JsonObject getDefaultConfig() {
        final JsonObject defaultConfig = Json.object();

        defaultConfig.put(ListActionFields.IS_ENABLED_FOR_WEB, true);
        return defaultConfig;
    }

    /**
     * <p>
     * Builds the key for the structure cache. It contains org id and provider name.
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param provider Represents the provider name
     * @return Cache key
     */
    private String buildCacheKey(final Long orgId, final String provider) {
        return StringUtil.join(StringUtil.Delimiter.COLON, String.valueOf(orgId), provider);
    }

    /**
     * <p>
     * Determines whether the cached {@link ListStructure} is outdated.
     * </p>
     *
     * @param structure Represents the cached list structure to validate
     * @param orgId     Represents the organization ID
     * @return true if the structure is outdated and needs to be refreshed, false otherwise
     */
    private boolean isStructureOutdated(final ListStructure structure, final Long orgId) {
        final long persistentFieldCount = structure.getFields().stream().filter(FieldConfig::isPersistent).count();

        return forecastCategoryManager.getTotalRecords(orgId) != persistentFieldCount;
    }
}

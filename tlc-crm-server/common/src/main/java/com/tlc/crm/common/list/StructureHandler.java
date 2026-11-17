package com.tlc.crm.common.list;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.list.model.ConfigHolder;
import com.tlc.crm.common.list.model.FieldConfig;
import com.tlc.crm.common.list.model.SubFieldConfig;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.ModelTransformer;
import com.tlc.crm.common.list.transformer.TransformerActionContainer;
import com.tlc.crm.common.sql.resource.METCONFIGUREDSUBFIELD;
import com.tlc.crm.common.sql.resource.METFIELDS;
import com.tlc.crm.common.sql.resource.METFIELDTRANSFORMERS;
import com.tlc.crm.common.sql.resource.METMODELTRANSFORMERS;
import com.tlc.crm.common.sql.resource.METSYSTEMFIELDSCONFIGURATION;
import com.tlc.crm.common.sql.resource.OMETCONFIGUREDSUBFIELD;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Handler class responsible for managing the structure defining the fields and their configurations for various providers
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public abstract class StructureHandler {

    private final TransformerActionContainer transformerActionContainer;

    protected StructureHandler() {
        this.transformerActionContainer = TransformerActionContainer.getInstance();
    }

    /**
     * <p>
     * Retrieves the list structure based on the provided user ID, organization ID, and provider.
     * </p>
     *
     * @param userId   Represents the user id
     * @param orgId    Represents an organization id
     * @param provider Represents the provider name
     * @return {@link ListStructure}
     */
    protected abstract ListStructure getStructure(final Long userId, final Long orgId, final String provider);

    /**
     * Builds the {@link FieldTransformer}
     *
     * @param row Holds the details of the transformer.
     * @return {@link FieldTransformer}
     */
    protected FieldTransformer buildTransformer(final Row row) {
        final String name = row.get(METFIELDTRANSFORMERS.NAME);

        return transformerActionContainer.fetchFieldTransformer(name);
    }

    /**
     * Converts {@link Row} to {@link ModelTransformer}
     *
     * @param row Holds the transformer row values
     * @return {@link ModelTransformer}
     */
    protected ModelTransformer convertRowToTransformer(final Row row) {
        final String name = row.get(METMODELTRANSFORMERS.NAME);

        return transformerActionContainer.fetchEntityTransformer(name);
    }

    /**
     * Adds the system field row into the container
     *
     * @param targetContainer   Holds the updated information of system field values.
     * @param systemConfigTable Represents the system configuration table.
     * @param userId            User ID
     * @param providerId        Provider ID
     * @param configHolders     To be process the fields in to the data container
     */
    protected void addSystemFieldRows(final DataContainer targetContainer, final Table systemConfigTable, final Long userId,
                                    final Long providerId, final List<ConfigHolder> configHolders) {
        final Table systemSubFieldTable = Table.get(METCONFIGUREDSUBFIELD.TABLE);

        for (final ConfigHolder configHolder : configHolders) {
            final FieldConfig field = configHolder.fieldConfig();

            if (!field.isDynamicField()) {
                loadSystemFieldConfigRow(systemConfigTable, systemSubFieldTable, userId, field.getOrder(), providerId,
                        targetContainer, configHolder);
            }
        }
    }

    /**
     * Gets the system field container.
     *
     * @param userId               User ID
     * @param providerId           Provider ID
     * @param fieldsTable          Represents the custom configuration table
     * @param secureReadableOrgDataStore Represents the secure readable organisation data store
     * @return {@link DataContainer} Holds the updated information of system field values
     */
    protected DataContainer getSystemFieldContainer(final Long userId, final Long providerId, final Table fieldsTable,
                                                    final ReadableDataStore secureReadableOrgDataStore) {
        final Table systemFieldConfigTable = Table.get(METSYSTEMFIELDSCONFIGURATION.TABLE);
        final Table subFieldConfigTable = Table.get(METCONFIGUREDSUBFIELD.TABLE);
        final SelectQuery query = SelectQuery.get(fieldsTable);

        query.addJoinClause(new JoinClause(fieldsTable.getPKColumn(), systemFieldConfigTable.getColumn(METSYSTEMFIELDSCONFIGURATION.FIELD_ID)));
        query.addJoinClause(new JoinClause(systemFieldConfigTable.getPKColumn(),
                subFieldConfigTable.getColumn(METCONFIGUREDSUBFIELD.CONFIGURED_FIELD_ID), JoinClause.JoinType.LEFT));
        query.addSelectClause(List.of(fieldsTable.getColumn(METFIELDS.NAME), systemFieldConfigTable.getColumn(METSYSTEMFIELDSCONFIGURATION.FIELD_ID),
                subFieldConfigTable.getColumn(METCONFIGUREDSUBFIELD.CONFIGURED_FIELD_ID), subFieldConfigTable.getColumn(METCONFIGUREDSUBFIELD.SUB_FIELD_ID),
                systemFieldConfigTable.getColumn(METSYSTEMFIELDSCONFIGURATION.PROVIDER_ID), systemFieldConfigTable.getColumn(METSYSTEMFIELDSCONFIGURATION.USER_ID),
                systemFieldConfigTable.getColumn(METSYSTEMFIELDSCONFIGURATION.CONFIG), systemFieldConfigTable.getColumn(METSYSTEMFIELDSCONFIGURATION.ORDER)));

        query.setWhereClause(new WhereClause(Criteria.eq(systemFieldConfigTable.getColumn(METSYSTEMFIELDSCONFIGURATION.PROVIDER_ID),
                providerId)).and(Criteria.eq(systemFieldConfigTable.getColumn(METSYSTEMFIELDSCONFIGURATION.USER_ID), userId)));

        return secureReadableOrgDataStore.get(query);
    }

    /**
     * Updates the system field rows into container.
     *
     * @param systemDataContainer Holds the updated information of system field values.
     * @param systemConfigTable   Represents the system configuration table.
     * @param userId              User ID
     * @param providerId          Provider ID
     * @param configHolders       To be process the fields in to the data container
     */
    protected void updateSystemFieldRows(final DataContainer systemDataContainer, final DataContainer targetContainer,
                                       final Table systemConfigTable, final Long userId, final Long providerId,
                                       final List<ConfigHolder> configHolders) {
        updateSystemFieldRows(systemDataContainer, targetContainer, systemConfigTable, userId, providerId, configHolders, null);
    }

    /**
     * Updates the system field rows into container.
     *
     * @param systemDataContainer Holds the updated information of system field values.
     * @param systemConfigTable   Represents the system configuration table.
     * @param userId              User ID
     * @param providerId          Provider ID
     * @param configHolders       To be process the fields in to the data container
     * @param fieldEnableType     Represents the {@link FieldConfig.FieldEnableType} of the list being updated
     */
    protected void updateSystemFieldRows(final DataContainer systemDataContainer, final DataContainer targetContainer,
                                       final Table systemConfigTable, final Long userId, final Long providerId,
                                       final List<ConfigHolder> configHolders, final FieldConfig.FieldEnableType fieldEnableType) {
        final Column systemFieldIdColumn = systemConfigTable.getColumn(METSYSTEMFIELDSCONFIGURATION.FIELD_ID);
        final Table subFieldConfigTable = Table.get(METCONFIGUREDSUBFIELD.TABLE);
        final Column subFieldIdColumn = subFieldConfigTable.getColumn(METCONFIGUREDSUBFIELD.SUB_FIELD_ID);
        final Column configuredFieldIdColumn = subFieldConfigTable.getColumn(METCONFIGUREDSUBFIELD.CONFIGURED_FIELD_ID);

        systemDataContainer.indexRows(systemFieldIdColumn);

        final Set<Long> systemFieldConfigCurrentIds = systemDataContainer.getRows(systemConfigTable)
                .map(row -> (Long) row.get(METSYSTEMFIELDSCONFIGURATION.FIELD_ID)).collect(Collectors.toSet());

        for (final ConfigHolder configHolder : configHolders) {
            final FieldConfig fieldConfig = configHolder.fieldConfig();
            final Long fieldId = fieldConfig.getFieldId();
            final int order = fieldConfig.getOrder();
            final Row systemFieldConfigRow = systemDataContainer.getIndexedRow(systemFieldIdColumn, fieldId);
            final Set<Long> subFieldConfigCurrentIds = Objects.isNull(systemFieldConfigRow) ? null : systemDataContainer.getRows(subFieldConfigTable,
                            new WhereClause(Criteria.eq(configuredFieldIdColumn, systemFieldConfigRow.getPKValue())))
                    .map(row -> (Long) row.get(METCONFIGUREDSUBFIELD.SUB_FIELD_ID)).collect(Collectors.toSet());

            if (systemFieldConfigCurrentIds.remove(fieldId)) {
                final Collection<SubFieldConfig> subFieldConfigs = configHolder.subFieldConfigs();

                systemFieldConfigRow.set(METSYSTEMFIELDSCONFIGURATION.ORDER, order);
                systemFieldConfigRow.set(METSYSTEMFIELDSCONFIGURATION.CONFIG,
                        mergeEnableFlag(systemFieldConfigRow.get(METSYSTEMFIELDSCONFIGURATION.CONFIG), fieldConfig, fieldEnableType));
                targetContainer.updateRow(systemFieldConfigRow);

                if (Objects.nonNull(subFieldConfigs) && !subFieldConfigs.isEmpty()) {
                    int subFieldOrder = 0;

                    for (final SubFieldConfig subFieldConfig : subFieldConfigs) {
                        final Long subFieldId = subFieldConfig.getFieldSubType().id();

                        if (Objects.nonNull(subFieldConfigCurrentIds) && subFieldConfigCurrentIds.remove(subFieldId)) {
                            final Row subFieldConfigRow = systemDataContainer.getRow(subFieldConfigTable,
                                    new WhereClause(Criteria.eq(subFieldIdColumn, subFieldId)));

                            if (Objects.nonNull(subFieldConfigRow)) {
                                systemFieldConfigRow.set(METCONFIGUREDSUBFIELD.ORDER, ++subFieldOrder);

                                targetContainer.updateRow(subFieldConfigRow);
                            }
                        } else {
                            final Row subFieldConfigRow = new Row(subFieldConfigTable);

                            subFieldConfigRow.set(OMETCONFIGUREDSUBFIELD.CONFIGURED_FIELD_ID, systemFieldConfigRow.getPKValue());
                            subFieldConfigRow.set(OMETCONFIGUREDSUBFIELD.SUB_FIELD_ID, subFieldConfig.getFieldSubType().id());
                            subFieldConfigRow.set(OMETCONFIGUREDSUBFIELD.ORDER, order);
                            subFieldConfig.setConfigured(true);

                            targetContainer.addNewRow(subFieldConfigRow);
                        }
                    }
                }
            } else {
                loadSystemFieldConfigRow(systemConfigTable, subFieldConfigTable, userId, order, providerId, targetContainer, configHolder);
            }

            if (Objects.nonNull(subFieldConfigCurrentIds)) {

                for (final Long subFieldConfigCurrentId : subFieldConfigCurrentIds) {
                    final Row subFielConfigRow = systemDataContainer.getRow(subFieldConfigTable,
                            new WhereClause(Criteria.eq(subFieldIdColumn, subFieldConfigCurrentId)));

                    if (Objects.nonNull(subFielConfigRow)) {
                        targetContainer.deleteRow(subFielConfigRow);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Merges only the enable flag of the given list type into the persisted config, so the flags of the other
     * list types stored in the same config are retained
     * </p>
     *
     * @param persistedConfig Represents the config stored in the DB row
     * @param fieldConfig     Represents the {@link FieldConfig} holding the requested enable flag
     * @param fieldEnableType Represents the {@link FieldConfig.FieldEnableType} being updated
     * @return The merged config as string
     */
    protected String mergeEnableFlag(final String persistedConfig, final FieldConfig fieldConfig,
                                     final FieldConfig.FieldEnableType fieldEnableType) {
        if (Objects.isNull(fieldEnableType) || Objects.isNull(persistedConfig) || persistedConfig.isBlank()) {
            return fieldConfig.getConfigAsString();
        }
        final String enableKey = fieldEnableType.getName();
        final JsonObject persistedJson = Json.object(persistedConfig);

        persistedJson.put(enableKey, Json.object(fieldConfig.getConfigAsString()).optBoolean(enableKey));

        return persistedJson.toString();
    }

    /**
     * Loads system field configuration row.
     *
     * @param systemConfigTable   Represents the system configuration table.
     * @param systemSubFieldTable Represents the system sub-field configuration table.
     * @param userId              Represents user id
     * @param order               Represents the sorting order
     * @param providerId          Provider ID
     * @param targetContainer     Holds the updated information of system field values.
     * @param configHolder        To be process the fields in to the data container
     */
    protected void loadSystemFieldConfigRow(final Table systemConfigTable, final Table systemSubFieldTable, final Long userId,
                                  final int order, final Long providerId, final DataContainer targetContainer,
                                  final ConfigHolder configHolder) {
        final Row systemFieldConfigRow = new Row(systemConfigTable);
        final FieldConfig fieldConfig = configHolder.fieldConfig();

        systemFieldConfigRow.set(METSYSTEMFIELDSCONFIGURATION.USER_ID, userId);
        systemFieldConfigRow.set(METSYSTEMFIELDSCONFIGURATION.PROVIDER_ID, providerId);
        systemFieldConfigRow.set(METSYSTEMFIELDSCONFIGURATION.FIELD_ID, fieldConfig.getFieldId());
        systemFieldConfigRow.set(METSYSTEMFIELDSCONFIGURATION.ORDER, order);
        systemFieldConfigRow.set(METSYSTEMFIELDSCONFIGURATION.CONFIG, fieldConfig.getConfigAsString());
        targetContainer.addNewRow(systemFieldConfigRow);

        if (fieldConfig.hasSubField()) {
            final Collection<SubFieldConfig> subFieldConfigs = configHolder.subFieldConfigs();

            if (Objects.nonNull(subFieldConfigs) && !subFieldConfigs.isEmpty()) {

                for (final SubFieldConfig subFieldConfig : subFieldConfigs) {

                    if (!subFieldConfig.isConfigured()) {
                        final Row subConfigRow = new Row(systemSubFieldTable);

                        subConfigRow.set(METCONFIGUREDSUBFIELD.CONFIGURED_FIELD_ID, systemFieldConfigRow.getPKValue());
                        subConfigRow.set(METCONFIGUREDSUBFIELD.SUB_FIELD_ID, subFieldConfig.getFieldSubType().id());
                        subConfigRow.set(METCONFIGUREDSUBFIELD.ORDER, order);
                        subFieldConfig.setConfigured(true);

                        targetContainer.addNewRow(subConfigRow);
                    }
                }
            }
        }
    }

    /**
     * Checks the given field is valid for apply sort.
     *
     * @param fieldType Refer the {@link FieldType}
     * @return Whether sort is apply return true otherwise false
     */
    protected boolean isSortable(final FieldType fieldType) {
        return Objects.nonNull(fieldType) && switch (fieldType.storeType()) {
            case SINGLE_STRING, SINGLE_NUMBER, DATE, TIME, IDENTIFIER, DROPDOWN, CONTACT, COMPANY, COMPANY_ONBOARD,
                 CONTACT_ONBOARD, PHONE, EMAIL, MONETARY, TIME_RANGE, DATE_RANGE, DATE_TIME_RANGE,
                 ADDRESS, DEAL_VALUE, TERRITORY, DATE_TIME, EMAILS, PHONES, DEAL, OWNER, AUTO_NUMBER -> true;
            default -> false;
        };
    }

    /**
     * <p>
     * Gets the readable data store
     * </p>
     *
     * @return The {@link ReadableDataStore}
     */
    protected ReadableDataStore readableDataStore() {
        return DataStoreProvider.getReadableDataStore();
    }
}

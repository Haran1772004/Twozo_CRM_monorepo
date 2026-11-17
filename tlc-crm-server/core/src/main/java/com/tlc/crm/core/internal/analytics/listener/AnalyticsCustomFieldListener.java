package com.tlc.crm.core.internal.analytics.listener;

import com.tlc.commons.code.ErrorCode;

import com.tlc.commons.listeners.Callback;
import com.tlc.crm.core.api.analytics.entity.config.fields.FieldTypeCapabilityRegistry;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModuleType;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMCustomModularField;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;
import com.tlc.crm.core.api.analytics.service.module.ReportModularFieldService;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.sql.resource.CRMCUSTOMMODULARFIELD;

import com.tlc.crm.entity.sql.resource.OMETFIELDS;

import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.fields.service.FieldTypeService;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.listeners.CompleteRowListener;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Row listener that syncs analytics modular-field records for custom fields.
 * Author: karthick V
 * Version: 1.0
 */
public final class AnalyticsCustomFieldListener implements CompleteRowListener {

    private final FieldTypeService fieldTypeService;
    private final FieldSourceService fieldSourceService;
    private final ReportModularFieldService reportModularFieldService;
    private final CRMViewConfigManager<CRMCustomModularField> customModularFieldManager;
    private final CRMViewConfigManager<ReportModularField> reportFieldManager;

    /**
     * Creates listener dependencies.
     */
    private AnalyticsCustomFieldListener() {
        final CRMReportManagerAPI crmReportManagerAPI = CRMReportManagerAPI.INSTANCE();

        this.fieldTypeService = FieldTypeService.getInstance();
        this.fieldSourceService = FieldSourceService.getInstance();
        this.reportModularFieldService = ReportModularFieldService.getInstance();
        this.customModularFieldManager = crmReportManagerAPI.getCRMCustomModularFieldManager();
        this.reportFieldManager = crmReportManagerAPI.getReportFieldManager();
    }

    private final static class Instance {
        private static final AnalyticsCustomFieldListener INSTANCE = new AnalyticsCustomFieldListener();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link AnalyticsCustomFieldListener} singleton
     */
    public static AnalyticsCustomFieldListener getInstance() {
        return AnalyticsCustomFieldListener.Instance.INSTANCE;
    }

    @Override
    public void addRows(final Collection<Row> rows) {
        handleCustomFieldRows(rows);
    }

    @Override
    public void updateRows(final Collection<Row> rows) {
        handleCustomFieldRows(rows);
    }

    @Override
    public void deleteRows(final Collection<Row> rows) {

    }

    /**
     * Processes inserted or updated custom-field rows.
     *
     * @param rows changed rows
     */
    private void handleCustomFieldRows(final Collection<Row> rows) {
        for (final Row row : rows) {
            final StoreType storeType = fieldTypeService.get(row.get(OMETFIELDS.FIELD_TYPE_ID)).storeType();

            if (FieldTypeCapabilityRegistry.isAnalyticsCapable(storeType)) {
                processCustomModularField(storeType, row);
            }
        }
    }

    /**
     * Creates or updates modular-field records for a custom field row.
     *
     * @param storeType custom-field store type
     * @param row       source row data
     */
    private void processCustomModularField(final StoreType storeType, final Row row) {
        final Long orgId = row.getOrgId();
        final Long fieldId = row.getPKValue();
        final CRMCustomModularField existingField = getExistingCustomField(orgId, fieldId);
        final CRMCustomModularField customField = buildCustomModularField(storeType, existingField, row);

        if (Objects.isNull(existingField)) {
            reportFieldManager.create(orgId, List.of(ReportModularField.builder().orgId(orgId).isCustom(true)
                    .customModularField(customField).build()));
            return;
        }

        if (row.get(OMETFIELDS.IS_ACTIVE)) {
            customModularFieldManager.update(customField);
            return;
        }

        deactivateReportField(orgId, existingField.id());
    }

    /**
     * Marks report modular field as inactive for the provided custom field.
     *
     * @param orgId   organization identifier
     * @param fieldId custom modular-field identifier
     */
    private void deactivateReportField(final Long orgId, final Long fieldId) {
        reportModularFieldService.getByOrgId(orgId, new Callback<>() {

            @Override
            public void onSuccess(final Map<Long, ReportModularField> reportFieldMap) {
                try {
                    reportFieldMap.values().stream().filter(reportModularField ->
                                    Objects.equals(reportModularField.getCustomFieldId(), fieldId)).findFirst()
                            .ifPresent(reportField -> {
                                reportField.setIsActive(false);
                                reportField.setOrgId(orgId);
                                reportFieldManager.update(reportField);
                            });
                } catch (Exception exception) {
                    throw ErrorCode.get(AnalyticsErrorCodes.CUSTOM_MODULAR_FIELD_FETCH_FAILED);
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                throw ErrorCode.get(AnalyticsErrorCodes.CUSTOM_MODULAR_FIELD_FETCH_FAILED);
            }
        });
    }

    /**
     * Fetches existing custom modular-field record for a CRM field.
     *
     * @param orgId   organization identifier
     * @param fieldId CRM field identifier
     * @return existing custom modular-field or {@code null}
     */
    private CRMCustomModularField getExistingCustomField(final Long orgId, final Long fieldId) {
        try {
            final Collection<CRMCustomModularField> customModularFields = customModularFieldManager.getAll(orgId,
                    Criteria.eq(customModularFieldManager.getTable().getColumn(CRMCUSTOMMODULARFIELD.CRM_FIELD_ID), fieldId));

            return customModularFields.isEmpty() ? null : customModularFields.stream().findFirst().get();
        } catch (Exception exception) {
            throw ErrorCode.get(AnalyticsErrorCodes.CUSTOM_MODULAR_FIELD_FETCH_FAILED);
        }
    }

    /**
     * Builds modular-field model from row payload.
     *
     * @param storeType     custom-field store type
     * @param existingField existing model, if present
     * @param row           source row data
     * @return prepared custom modular-field model
     */
    private CRMCustomModularField buildCustomModularField(final StoreType storeType, final CRMCustomModularField existingField,
                                                          final Row row) {
        return CRMCustomModularField.builder().id(Objects.isNull(existingField) ? null : existingField.id())
                .orgId(row.get(OMETFIELDS.ORG_ID)).crmFieldId(row.getPKValue()).fieldName(row.get(OMETFIELDS.NAME))
                .moduleId(Objects.requireNonNull(ModuleType.getModuleTypeByName(fieldSourceService.getSource((Long) row.get(OMETFIELDS.SOURCE_ID))
                        .getName().toLowerCase())).getTypeCode())
                .fieldConfigAsString(FieldTypeCapabilityRegistry.getConfig(storeType).configAsJson().toString())
                .build();
    }
}

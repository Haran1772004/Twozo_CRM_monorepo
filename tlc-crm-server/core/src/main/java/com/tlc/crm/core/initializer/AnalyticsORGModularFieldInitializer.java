package com.tlc.crm.core.initializer;

import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.internal.manager.OrganizationManager;

import com.tlc.crm.core.api.analytics.entity.config.fields.FieldTypeCapabilityRegistry;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMCustomModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModuleType;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;
import com.tlc.crm.core.api.analytics.manager.config.field.SystemModularFieldManager;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldSourceService;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensures every organization has all required modular fields (system and custom) saved in the DB.
 *
 * <p>Run this at startup or anytime to fill in missing fields — it only inserts what's absent,
 * so it is safe to run multiple times.
 *
 * @author saravanan
 * @version 1.0
 */
public class AnalyticsORGModularFieldInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsORGModularFieldInitializer.class);

    private final OrganizationManager organizationManager;
    private final FieldSourceService fieldSourceService;
    private final SystemModularFieldManager systemModularFieldManager;
    private final CRMViewConfigManager<ReportModularField> reportFieldManager;

    private AnalyticsORGModularFieldInitializer() {
        final CRMReportManagerAPI crmReportManagerAPI = CRMReportManagerAPI.INSTANCE();
        this.organizationManager = OrganizationManager.getInstance();
        this.fieldSourceService = FieldSourceService.getInstance();
        this.systemModularFieldManager = SystemModularFieldManager.INSTANCE();
        this.reportFieldManager = crmReportManagerAPI.getReportFieldManager();
    }

    private final static class Instance {
        private static final AnalyticsORGModularFieldInitializer INSTANCE = new AnalyticsORGModularFieldInitializer();
    }

    public static AnalyticsORGModularFieldInitializer getInstance() {
        return AnalyticsORGModularFieldInitializer.Instance.INSTANCE;
    }

    /**
     * Triggers the modular field backfill for all organizations.
     */
    public void load() {
        loadModularFieldsForAllORGs();
    }

    /**
     * Loads all missing system and analytics-capable custom modular fields
     * for every organization, skipping existing entries and preventing duplicates.
     */
    private void loadModularFieldsForAllORGs() {

        final Collection<Long> orgIDs = organizationManager.getAll().stream().map(CrmOrg::id).toList();
        final Collection<Long> systemModularFieldsIds = systemModularFieldManager.fetchAllFieldIds();
        final FieldService fieldService = FieldService.getInstance();
        final Map<Long, Integer> sourceToModuleIdCache = new HashMap<>();

        final int totalOrg = orgIDs.size();
        int processedOrgs = 0;
        int totalFieldsCreated = 0;
        LOGGER.info("Analytics ModularFieldInit: Starting modular field load for {} orgs", totalOrg);

        for (final Long orgId : orgIDs) {
            final long orgStartTime = System.currentTimeMillis();
            int orgCreatedCount = 0;

            final Collection<ReportModularField> existingFields;
            try {
                existingFields = reportFieldManager.getAll(orgId, Collections.emptyList());
            } catch (Exception e) {
                LOGGER.warn("Analytics ModularFieldInit: Failed to check existing report fields for org: {}", orgId, e);
                processedOrgs++;
                continue;
            }

            final Set<Long> existingSystemFieldIds = new HashSet<>();
            final Set<Long> existingCustomCrmFieldIds = new HashSet<>();

            if (existingFields != null) {
                for (final ReportModularField existingField : existingFields) {
                    if (existingField.getSystemFieldId() != null) {
                        existingSystemFieldIds.add(existingField.getSystemFieldId());
                    }

                    if (existingField.isCustom() && existingField.getCustomModularField() != null
                            && existingField.getCustomModularField().getCrmFieldId() != null) {
                        existingCustomCrmFieldIds.add(existingField.getCustomModularField().getCrmFieldId());
                    }
                }
            }

            final Set<Long> scheduledSystemFieldIds = new HashSet<>();
            final Set<Long> scheduledCustomFieldIds = new HashSet<>();
            final List<ReportModularField> reportModularFields = new ArrayList<>();

            for (final Long fieldId : systemModularFieldsIds) {
                if (fieldId == null || existingSystemFieldIds.contains(fieldId) || !scheduledSystemFieldIds.add(fieldId)) {
                    continue;
                }

                reportModularFields.add(ReportModularField.builder().systemFieldId(fieldId).orgId(orgId).build());
            }

            Collection<FieldInfo> orgFields = Collections.emptyList();
            try {
                orgFields = fieldService.getOrgFields(orgId);
            } catch (Exception e) {
                LOGGER.warn("Analytics ModularFieldInit: Failed to fetch org fields for org: {}", orgId, e);
            }

            for (final FieldInfo fieldInfo : orgFields) {
                final Long crmFieldId = fieldInfo.id();

                try {
                    final StoreType storeType = fieldInfo.fieldStore().storeType();

                    if (!FieldTypeCapabilityRegistry.isAnalyticsCapable(storeType)
                            || crmFieldId == null
                            || existingCustomCrmFieldIds.contains(crmFieldId)
                            || !scheduledCustomFieldIds.add(crmFieldId)) {
                        continue;
                    }

                    final Long sourceId = fieldInfo.sourceId();
                    Integer moduleId = sourceToModuleIdCache.get(sourceId);

                    if (moduleId == null) {
                        final String sourceName = fieldSourceService.getSource(sourceId).getName().toLowerCase();
                        final ModuleType moduleType = ModuleType.getModuleTypeByName(sourceName);

                        if (moduleType == null) {
                            LOGGER.warn("Analytics ModularFieldInit: No ModuleType found for source: {} | org: {}", sourceName, orgId);
                            continue;
                        }

                        moduleId = moduleType.getTypeCode();
                        sourceToModuleIdCache.put(sourceId, moduleId);
                    }

                    final CRMCustomModularField crmCustomModularField = CRMCustomModularField.builder()
                            .crmFieldId(crmFieldId)
                            .fieldName(fieldInfo.i18nName())
                            .moduleId(moduleId)
                            .fieldConfigAsString(FieldTypeCapabilityRegistry.getConfig(storeType).configAsJson().toString())
                            .build();

                    reportModularFields.add(ReportModularField.builder()
                            .orgId(orgId)
                            .customModularField(crmCustomModularField)
                            .isCustom(true)
                            .build());
                } catch (Exception e) {
                    LOGGER.warn("Analytics ModularFieldInit: Failed to process custom field for org: {} | fieldId: {}", orgId, crmFieldId, e);
                }
            }

            if (!reportModularFields.isEmpty()) {
                try {
                    reportFieldManager.create(orgId, reportModularFields);
                    orgCreatedCount += reportModularFields.size();
                } catch (Exception batchException) {
                    LOGGER.warn("Analytics ModularFieldInit: Batch create failed for org: {}. Falling back to per-field create.", orgId, batchException);

                    for (final ReportModularField reportModularField : reportModularFields) {
                        try {
                            reportFieldManager.create(orgId, List.of(reportModularField));
                            orgCreatedCount++;
                        } catch (Exception fieldException) {
                            LOGGER.warn("Analytics ModularFieldInit: Failed to create modular field for org: {}", orgId, fieldException);
                        }
                    }
                }
            }

            totalFieldsCreated += orgCreatedCount;
            processedOrgs++;
            LOGGER.info("Analytics ModularFieldInit: Completed org: {} | Fields created: {} | Time: {}ms",
                    orgId, orgCreatedCount, System.currentTimeMillis() - orgStartTime);
        }

        LOGGER.info("Analytics ModularFieldInit: Modular field load complete | Total orgs processed: {} | Total fields created: {}",
                processedOrgs, totalFieldsCreated);
    }
}

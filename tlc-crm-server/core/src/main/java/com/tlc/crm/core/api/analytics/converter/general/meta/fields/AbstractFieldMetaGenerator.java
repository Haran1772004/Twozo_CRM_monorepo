package com.tlc.crm.core.api.analytics.converter.general.meta.fields;

import com.tlc.analytics.config.action.AnalyticsRequest;

import com.tlc.commons.code.ErrorCode;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModuleType;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSystemModularField;
import com.tlc.crm.core.api.analytics.integ.managers.config.layout.CRMFieldModuleRelationManager;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMFieldModuleRelation;
import com.tlc.crm.core.api.analytics.permission.handler.AnalyticsPermissionHandler;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.sql.resource.CRMFIELDMODULERELATION;

import com.tlc.sql.common.api.dml.Criteria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Provides an abstract way for Meta Generation
 * </p>
 *
 * @author Ryan
 * @version 1.0
 **/
abstract class AbstractFieldMetaGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFieldMetaGenerator.class);
    protected final CRMFieldModuleRelationManager crmFieldModuleRelationManager;
    private final AnalyticsPermissionHandler analyticsPermissionHandler;

    /**
     * Creates field-meta generator dependencies.
     */
    public AbstractFieldMetaGenerator() {
        this.crmFieldModuleRelationManager = CRMFieldModuleRelationManager.INSTANCE();
        this.analyticsPermissionHandler = AnalyticsPermissionHandler.getInstance();
    }

    /**
     * <p>
     * Returns list containing unauthorized field Ids
     * </p>
     *
     * @param analyticsRequest    Represents the {@link AnalyticsRequest} details
     * @param reportModularFields The collection of report modular fields to filter
     * @return The collection of authorized report modular fields
     */
    public Collection<ReportModularField> getAuthorizedReportModularField(final AnalyticsRequest analyticsRequest,
                                                                          final Collection<ReportModularField> reportModularFields) {
        final Long orgId = analyticsRequest.orgId();
        final Collection<Integer> authorizedModuleIds = analyticsPermissionHandler.getAuthorizedModuleIds(orgId, analyticsRequest.roleId());

        return getAuthorizedReportModularField(orgId, authorizedModuleIds, getUnauthorizedAnalyticsFields(orgId, authorizedModuleIds),
                reportModularFields);
    }

    /**
     * <p>
     * Returns list of authorized report modular fields using pre-fetched authorized module IDs
     * and pre-fetched unauthorized analytics field IDs. This completely avoids any DB calls,
     * making it safe to call in a loop.
     * </p>
     *
     * @param orgId                      the organization ID
     * @param authorizedModuleIds         Contains moduleIds that are accessible by the user
     * @param unauthorizedAnalyticsFields Contains analytics field IDs that are not authorized
     * @param reportModularFields         The collection of report modular fields to filter
     * @return The collection of authorized report modular fields
     */
    public Collection<ReportModularField> getAuthorizedReportModularField(final Long orgId,
                                                                          final Collection<Integer> authorizedModuleIds,
                                                                          final Collection<Long> unauthorizedAnalyticsFields,
                                                                          final Collection<ReportModularField> reportModularFields) {
        final Collection<ReportModularField> accessReportModularFields = new HashSet<>();

        for (final ReportModularField reportModularField : reportModularFields) {

            if (Objects.isNull(reportModularField)) {
                continue;
            }

            if (reportModularField.isCustom()) {
                final ModularField customField = reportModularField.getCustomModularField();

                if (Objects.isNull(customField)) {
                    continue;
                }

                final boolean isRelatedModuleAuthorized = isRelatedModuleAuthorized(customField, authorizedModuleIds);

                if (authorizedModuleIds.contains(customField.getModuleId())
                        && isRelatedModuleAuthorized) {
                    accessReportModularFields.add(reportModularField);
                }
                continue;
            }

            final CRMSystemModularField systemModularField = reportModularField.getSystemModularField();

            if (Objects.isNull(systemModularField)) {
                continue;
            }

            final boolean isRelatedModuleAuthorized = isRelatedModuleAuthorized(systemModularField, authorizedModuleIds);

            if (authorizedModuleIds.contains(systemModularField.getModuleId())
                    && !unauthorizedAnalyticsFields.contains(systemModularField.getAnaFieldId())
                    && isRelatedModuleAuthorized) {
                accessReportModularFields.add(reportModularField);
            }
        }
        return accessReportModularFields;
    }

    /**
     * <p>
     * Checks if the related module associated with the given report modular field is authorized
     * for the current user based on their authorized module IDs.
     * </p>
     *
     * @param reportModularField     The report modular field to check authorization for
     * @param authorizedModuleIds    The collection of module IDs that are authorized for the user
     * @return true if the related module is authorized or no related module exists; false otherwise
     */
    private boolean isRelatedModuleAuthorized(final ReportModularField reportModularField, final Collection<Integer> authorizedModuleIds) {
        final ModularField modularField = reportModularField.getModularField();
        return isRelatedModuleAuthorized(modularField, authorizedModuleIds);
    }

    /**
     * <p>
     * Checks if the related module associated with the given {@link ModularField} is authorized
     * for the current user based on their authorized module IDs.
     * </p>
     *
     * @param modularField        the {@link ModularField} whose related module authorization is checked
     * @param authorizedModuleIds the collection of module IDs that are authorized for the user
     * @return true if the related module is authorized or no related module exists; false otherwise
     */
    private boolean isRelatedModuleAuthorized(final ModularField modularField, final Collection<Integer> authorizedModuleIds) {

        if (Objects.isNull(modularField) || Objects.isNull(modularField.getModularFieldConfig())) {
            return true;
        }

        final Integer relatedModuleId = modularField.getModularFieldConfig().getRelatedModuleId();

        if (Objects.isNull(relatedModuleId) || Objects.equals(relatedModuleId, NumericalConstant.ZERO)) {
            return true;
        }

        final ModuleType moduleType = ModuleType.getModuleType(relatedModuleId);
        final Integer resolvedModuleId = Objects.isNull(moduleType) ? null : moduleType.getTypeCode();

        return Objects.isNull(resolvedModuleId) || authorizedModuleIds.contains(resolvedModuleId);
    }

    /**
     * <p>
     * Returns the collection of unauthorized analytics field IDs based on the authorized module IDs.
     * Exposed as package-private so subclasses can pre-compute and reuse across loops.
     * </p>
     *
     * @param orgId               It represents the organization ID
     * @param authorizedModuleIds Contains moduleIds that are accessible by the user
     * @return The collection of unauthorized analytics field IDs
     */
    Collection<Long> getUnauthorizedAnalyticsFields(final Long orgId, final Collection<Integer> authorizedModuleIds) {
        try {
            final Map<Long, Collection<Integer>> unAuthorizedFieldsMap = new HashMap<>();
            final Collection<CRMFieldModuleRelation> crmFieldModuleRelations = crmFieldModuleRelationManager
                    .getAll(orgId, Criteria.in(crmFieldModuleRelationManager.getTable().getColumn(CRMFIELDMODULERELATION.PRIMARY_MODULE_ID),
                            authorizedModuleIds));

            for (final CRMFieldModuleRelation crmFieldModuleRelation : crmFieldModuleRelations) {
                unAuthorizedFieldsMap.computeIfAbsent(crmFieldModuleRelation.getModularFieldId(), key -> new ArrayList<>())
                        .add(crmFieldModuleRelation.getSecondaryModuleId());
            }
            unAuthorizedFieldsMap.entrySet().removeIf(entry -> authorizedModuleIds.containsAll(entry.getValue()));

            return unAuthorizedFieldsMap.keySet();
        } catch (Exception exception) {
            LOGGER.error("Analytics Permission: Error occurred while getting unauthorized field Ids for orgId {}, authorizedModuleIds {}, reason {}",
                    orgId, authorizedModuleIds, exception.getMessage());
            throw ErrorCode.get(AnalyticsErrorCodes.INVALID_PERMISSION);
        }
    }
}

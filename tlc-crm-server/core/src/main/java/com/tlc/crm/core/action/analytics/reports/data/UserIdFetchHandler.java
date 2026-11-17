package com.tlc.crm.core.action.analytics.reports.data;

import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.executor.CodeBlockExecutor;

import com.tlc.analytics.core.api.GraphAxis;

import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSystemModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.service.module.CRMSystemModularFieldService;
import com.tlc.crm.core.api.analytics.service.util.ModularFieldUtil;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.models.StoreType;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Fetches the user id for the {@link GraphAxis}
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public class UserIdFetchHandler {
    private final CRMSystemModularFieldService systemModularFieldService;
    private final FieldService fieldService;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserIdFetchHandler.class);

    private UserIdFetchHandler() {
        systemModularFieldService = CRMSystemModularFieldService.getInstance();
        fieldService = FieldService.getInstance();
    }

    private static class Instance {
        private static final UserIdFetchHandler USER_ID_FETCH_HANDLER = new UserIdFetchHandler();
    }

    public static UserIdFetchHandler getINSTANCE() {
        return Instance.USER_ID_FETCH_HANDLER;
    }

    /**
     * <p>
     * Gets the user ids based on the org id and list of {@link GraphAxis}
     * </p>
     *
     * @param orgId     Represents an organisation id
     * @param graphAxes Contains the list of {@link GraphAxis} object
     * @return The collection of user ids
      * @param reportFieldMap   pre-fetched map of field ID to {@link ReportModularField}, or {@code null} if not available
     */
    @NonNull
    public final Collection<Long> getUserIds(final @NonNull Long orgId, final @NonNull List<GraphAxis<Object>> graphAxes,
                                             final Map<Long, ReportModularField> reportFieldMap) {
        final Collection<Long> userIds = new HashSet<>();

        try {
            final Set<Long> allFieldIds = graphAxes.stream()
                    .filter(axis -> !axis.getColumnMap().isEmpty())
                    .map(axis -> {
                        final ReportField reportField = axis.getReportField();

                        return Objects.nonNull(reportField) ? reportField.getFieldId()
                                : axis.getColumnMap().keySet().stream().findFirst().map(id ->
                                axis.getColumnToFieldIdMap().get(id)).orElse(null);}).filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            final Map<Long, ReportModularField> reportModularFieldMap = ModularFieldUtil.filterReportFields(reportFieldMap, allFieldIds);
            final Set<Long> systemModularFieldIds = new HashSet<>();

            reportModularFieldMap.values().forEach(reportModField -> {
                if (reportModField.getIsActive() && !reportModField.isCustom()) {
                    systemModularFieldIds.add(reportModField.getSystemFieldId());
                }
            });

            final Map<Long, CRMSystemModularField> systemModularFieldMap = systemModularFieldService.getAll(systemModularFieldIds);

            CodeBlockExecutor.forEachSafely(graphAxes,
                    graphAxis ->
                    {
                        final Optional<Long> optionalColumnId = graphAxis.getColumnMap().keySet().stream().findFirst();

                        if (optionalColumnId.isPresent()) {
                            final ReportField reportField = graphAxis.getReportField();

                            final Long fieldId = Objects.nonNull(reportField) ? reportField.getFieldId()
                                    : graphAxis.getColumnToFieldIdMap().get(optionalColumnId.get());

                            if (Objects.isNull(fieldId)) {
                                return;
                            }
                            final ReportModularField reportModularField = reportModularFieldMap.get(fieldId);

                            if (Objects.nonNull(reportModularField) && reportModularField.getIsActive()) {
                                final ModularField modularField = reportModularField.isCustom()
                                        ? reportModularField.getCustomModularField()
                                        : systemModularFieldMap.get(reportModularField.getSystemFieldId());

                                setUserIds(orgId, optionalColumnId.get(), modularField, userIds, graphAxis);
                            }
                        }
                    });
        } catch (Exception exception) {
            LOGGER.error("Analytics: Failed to fetch user ids for orgId: {}", orgId, exception);
            throw ErrorCode.get(AnalyticsErrorCodes.USER_ID_FETCH_ACTION_FAILED,
                    "i18n.analytics.user.id.fetch.action.failed");
        }

        return userIds;
    }

    /**
     * <p>
     * Sets the user ids based on the given modular field
     * </p>
     *
     * @param orgId         Represents an organisation id
     * @param columnId      Represents the ANA column id
     * @param modularField  Represents the resolved modular field
     * @param userIds       Contains the collection of user ids
     * @param graphAxis     Represents the {@link GraphAxis} object
     */
    private void setUserIds(final Long orgId,
                            final Long columnId,
                            final ModularField modularField,
                            final Collection<Long> userIds,
                            final GraphAxis<Object> graphAxis) {
        final Long crmFieldId = (modularField != null) ? modularField.getCrmFieldId() : null;

        if (Objects.nonNull(crmFieldId)) {
            final FieldInfo fieldInfo = fieldService.getField(orgId, crmFieldId);

            if (StoreType.OWNER == fieldInfo.storeType() || StoreType.OWNERS == fieldInfo.storeType()) {
                AxisValueUtils.collectEntityIds(graphAxis, userIds);
            }
        }
    }
}

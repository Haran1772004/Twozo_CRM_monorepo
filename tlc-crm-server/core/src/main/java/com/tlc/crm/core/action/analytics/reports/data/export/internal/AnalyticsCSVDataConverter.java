package com.tlc.crm.core.action.analytics.reports.data.export.internal;

import com.tlc.analytics.core.api.GraphAxis;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.api.analytics.converter.data.DisplayValueResolver;
import com.tlc.crm.core.api.analytics.entity.config.fields.AnalyticsField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSystemModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;

import com.tlc.crm.core.api.analytics.service.field.AnalyticsFieldService;
import com.tlc.crm.core.api.analytics.service.module.ReportModularFieldService;
import com.tlc.crm.core.api.analytics.service.module.CRMSystemModularFieldService;
import com.tlc.crm.core.api.analytics.service.util.ModularFieldUtil;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import com.tlc.i18n.I18nResolver;

import com.tlc.pk.export.csvexporter.data.CSVDataConverter;
import com.tlc.pk.export.csvexporter.data.Data;
import com.tlc.pk.export.csvexporter.data.Row;

import lombok.Getter;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Abstract base class for converting analytics data into CSV format.
 *
 * <p>This class provides a common structure for converting a list of {@link GraphAxis} objects into
 * a {@link Data} object containing CSV headers and rows. Subclasses must implement the logic for extracting
 * headers and rows based on specific requirements by overriding the {@code extractHeaders} and
 * {@code extractAndConvertData} methods.
 *
 * <p>This class implements {@link CSVDataConverter} with a generic type {@code List<GraphAxis<Object>>}.
 * It includes a common {@code convertToCSVRows} method that delegates the conversion logic to the subclass-specific
 * implementations of the abstract methods.
 *
 * @author Saravanan
 * @version 1.0
 */
@Getter
public abstract class AnalyticsCSVDataConverter implements CSVDataConverter<List<GraphAxis<Object>>, ReportModularField> {
    private final Logger LOGGER = LoggerFactory.getLogger( AnalyticsCSVDataConverter.class );
    private static final String HYPHEN = "-";
    private final DisplayValueResolver displayValueResolver;
    private final ReportModularFieldService reportModularFieldService;
    private final CRMSystemModularFieldService systemModularFieldService;
    private final AnalyticsFieldService analyticsFieldService;
    private final FieldTypeService fieldTypeService;
    private final FieldService fieldService;

    protected AnalyticsCSVDataConverter() {
        this.displayValueResolver = DisplayValueResolver.INSTANCE();
        this.reportModularFieldService = ReportModularFieldService.getInstance();
        this.systemModularFieldService = CRMSystemModularFieldService.getInstance();
        this.analyticsFieldService = AnalyticsFieldService.getInstance();
        this.fieldTypeService = FieldTypeService.getInstance();
        this.fieldService = FieldService.getInstance();
    }

    /**
     * <p>Converts the provided {@link GraphAxis} list into a {@link Data} object containing
     * CSV-compatible headers and rows.</p>
     *
     * @param orgId            the organization identifier to scope the data extraction
     * @param graphAxes        the list of {@link GraphAxis} objects representing the input data
     * @param userMap          a mapping of user IDs to {@link CrmUser} objects, used for resolving user-related fields
     * @param modularFieldMap  a map of {@link ReportModularField} keyed by field ID
     * @param i18nResolver     an instance of {@link I18nResolver} for handling internationalization
     * @return a {@link Data} object containing headers and rows suitable for CSV export
     */
    @Override
    public @NonNull Data convertToCSVRows(final @NonNull Long orgId,
                                          final @NonNull List<GraphAxis<Object>> graphAxes,
                                          final Map<Long, CrmUser> userMap,
                                          final @NonNull Map<Long, ReportModularField> modularFieldMap,
                                          final @NonNull I18nResolver i18nResolver) {
        return Data.builder()
                .headers(Optional.of(extractHeaders(graphAxes)))
                .rows(extractAndConvertData(orgId, graphAxes, userMap, modularFieldMap, i18nResolver))
                .stringConverter(cell -> String.valueOf(cell.getValue()))
                .build();
    }

    /**
     * Resolves the actual displayable value for a given field cell, based on its store type.
     *
     * @param orgId                  the organization ID, used for context in resolution
     * @param columnId               the column ID for field identification
     * @param value                  the raw value to resolve
     * @param userMap                a map of user IDs to {@link CrmUser} objects used for resolving owner names
     * @param storeType              the store type of the column, used to determine the resolution logic
     * @param i18nResolver           an instance of {@link I18nResolver} for handling internationalization
     * @return the resolved display value, such as a user full name or a formatted/export-ready value
     */
    protected Object resolveCellValue(final Long orgId, final Long columnId, final Object value,
                                      final Map<Long, CrmUser> userMap, final StoreType storeType, final I18nResolver i18nResolver) {
        if ((Objects.equals(StoreType.OWNER, storeType) || Objects.equals(StoreType.OWNERS, storeType))) {
            if (Objects.isNull(value)) {
                return HYPHEN;
            }

            if (value instanceof Collection<?> collection && collection.isEmpty()) {
                return HYPHEN;
            }

            if (Objects.toString(value).trim().isEmpty()) {
                return HYPHEN;
            }

            final long ownerId = NumberUtils.parseNumber(value).longValue();
            final CrmUser crmUser = userMap.get(ownerId);

            if (Objects.isNull(crmUser)) {
                LOGGER.info("CRM_ANALYTICS_EXPORT_OWNER: UNRESOLVED ownerId={}, columnId={}, userMapKeys={}",
                        ownerId, columnId, userMap.keySet());
            }

            return Objects.nonNull(crmUser) ? crmUser.getFullName() : value;
        }

        return displayValueResolver.resolveExportFileValue(orgId, columnId, value, i18nResolver);
    }

    /**
     * Resolves the {@link StoreType} for a collection of field IDs by fetching their
     * corresponding {@link ModularField} definitions and associated metadata.
     *
     * @param orgId           the organization ID, used for context-specific field lookups
     * @param fieldIds        the collection of field IDs to resolve
     * @param reportFieldMap  a map of field ID to {@link ReportModularField} objects containing modular field metadata
     * @return a map of field ID to its resolved {@link StoreType}; an empty map if {@code fieldIds} is empty
     */
    protected Map<Long, StoreType> resolveStoreTypes(final @NonNull Long orgId, final @NonNull Collection<Long> fieldIds, final Map<Long, ReportModularField> reportFieldMap) {
        try {
            final Map<Long, ReportModularField> reportModularFieldMap = ModularFieldUtil.filterReportFields(reportFieldMap, fieldIds);
            final Collection<ReportModularField> reportModularFields = reportModularFieldMap.values();
            final Set<Long> allCrmFieldIds = new HashSet<>();
            final Set<Long> allAnaFieldIds = new HashSet<>();

            for (final ReportModularField reportModularField : reportModularFields) {
                final ModularField modularField = reportModularField.getModularField();

                if (Objects.isNull(modularField)) {
                    continue;
                }

                if (!reportModularField.isCustom()) {
                    final CRMSystemModularField systemModularField = reportModularField.getSystemModularField();

                    if (Objects.nonNull(systemModularField)) {
                        if (Objects.isNull(systemModularField.getAnaFieldId())) {
                            if (Objects.nonNull(systemModularField.getCrmFieldId())) {
                                allCrmFieldIds.add(systemModularField.getCrmFieldId());
                            }
                        } else {
                            allAnaFieldIds.add(systemModularField.getAnaFieldId());
                        }
                    }
                } else if (Objects.nonNull(modularField.getCrmFieldId())) {
                    allCrmFieldIds.add(modularField.getCrmFieldId());
                }
            }

            final Map<Long, FieldInfo> fieldInfoMap = allCrmFieldIds.isEmpty() ? Collections.emptyMap() : fieldService.getFieldsById(orgId, allCrmFieldIds);
            final Map<Long, StoreType> anaFieldStoreTypeMap = new HashMap<>();

            if (!allAnaFieldIds.isEmpty()) {
                final Map<Long, AnalyticsField> analyticsFieldMap = analyticsFieldService.getAnalyticsFieldsByIds(allAnaFieldIds);
                final Set<Long> fieldTypeIds = new HashSet<>();
                final Map<Long, Long> anaFieldToFieldTypeId = new HashMap<>();

                for (final Map.Entry<Long, AnalyticsField> entry : analyticsFieldMap.entrySet()) {
                    final Long fieldTypeId = entry.getValue().getFieldTypeId();

                    fieldTypeIds.add(fieldTypeId);
                    anaFieldToFieldTypeId.put(entry.getKey(), fieldTypeId);
                }

                final Map<Long, StoreType> fieldTypeStoreTypeMap = fieldTypeService.getFieldTypesByIds(fieldTypeIds).stream()
                        .collect(Collectors.toMap(FieldType::id, FieldType::storeType));

                for (final Map.Entry<Long, Long> entry : anaFieldToFieldTypeId.entrySet()) {
                    anaFieldStoreTypeMap.put(entry.getKey(), fieldTypeStoreTypeMap.get(entry.getValue()));
                }
            }

            final Map<Long, StoreType> result = new HashMap<>(reportModularFieldMap.size());

            for (final Map.Entry<Long, ReportModularField> entry : reportModularFieldMap.entrySet()) {
                final ReportModularField reportModularField = entry.getValue();
                final ModularField modularField = reportModularField.getModularField();

                if (Objects.isNull(modularField)) {
                    continue;
                }

                if (reportModularField.isCustom()) {
                    final FieldInfo fieldInfo = fieldInfoMap.get(modularField.getCrmFieldId());

                    if (Objects.nonNull(fieldInfo)) {
                        result.put(entry.getKey(), fieldInfo.storeType());
                    }
                } else {
                    final CRMSystemModularField systemModularField = reportModularField.getSystemModularField();

                    if (Objects.nonNull(systemModularField)) {
                        final Long anaFieldId = systemModularField.getAnaFieldId();

                        if (Objects.isNull(anaFieldId)) {
                            final FieldInfo fieldInfo = fieldInfoMap.get(systemModularField.getCrmFieldId());

                            if (Objects.nonNull(fieldInfo)) {
                                result.put(entry.getKey(), fieldInfo.storeType());
                            }
                        } else {
                            result.put(entry.getKey(), anaFieldStoreTypeMap.get(anaFieldId));
                        }
                    }
                }
            }

            return result;
        } catch (final Exception exception) {
            throw ErrorCode.get(AnalyticsErrorCodes.MODULAR_FIELD_NOT_FOUND);
        }
    }

    /**
     * Extracts column headers from the provided {@link GraphAxis} list.
     * <p>This method must be implemented by subclasses to define header extraction logic.
     */
    abstract protected @NonNull List<String> extractHeaders(final @NonNull List<GraphAxis<Object>> graphAxes);

    /**
     * Extracts and converts data from the given {@link GraphAxis} list into a collection of CSV {@link Row} objects.
     *
     * @param orgId                 the organization identifier to scope the data extraction
     * @param graphAxes             the list of {@link GraphAxis} objects representing the data to be converted
     * @param userMap               a mapping of user IDs to {@link CrmUser} objects, used for resolving user-related data
     * @param reportModularFieldMap a map of field ID to {@link ReportModularField} objects containing modular field metadata
     * @param i18nResolver          an instance of {@link I18nResolver} for handling internationalization
     * @return a collection of {@link Row} objects containing the extracted and converted data
     */
    abstract protected @NonNull Collection<Row> extractAndConvertData(final @NonNull Long orgId,
                                                                      final @NonNull List<GraphAxis<Object>> graphAxes,
                                                                      final Map<Long, CrmUser> userMap,
                                                                      final @NonNull Map<Long, ReportModularField> reportModularFieldMap,
                                                                      final @NonNull I18nResolver i18nResolver);
}
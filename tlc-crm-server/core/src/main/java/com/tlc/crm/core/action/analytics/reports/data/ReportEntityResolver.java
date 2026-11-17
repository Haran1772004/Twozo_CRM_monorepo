package com.tlc.crm.core.action.analytics.reports.data;

import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.core.api.AxisValue;
import com.tlc.analytics.core.api.GraphAxis;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.commons.util.executor.CodeBlockExecutor;

import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.service.util.ModularFieldUtil;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.models.StoreType;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Resolves entity IDs and display names (Contact, Company) from {@link GraphAxis} data
 * for analytics report processing.
 * </p>
 *
 * @author karthick V
 * @version 1.0
 */
public class ReportEntityResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger( ReportEntityResolver.class );

    private final FieldService fieldService;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;

    private ReportEntityResolver() {
        this.fieldService = FieldService.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.companyManager = CompanyManager.getInstance();
    }

    private static class Instance {
        private static final ReportEntityResolver INSTANCE = new ReportEntityResolver();
    }

    /**
     * Returns the singleton resolver instance.
     *
     * @return The {@link ReportEntityResolver} instance.
     */
    public static ReportEntityResolver getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Replaces entity IDs stored in graph-axis values with their corresponding
     * display names in-place.
     *
     * <p>For each axis backed by a <em>custom</em> field whose {@link StoreType}
     * is {@link StoreType#CONTACT} or {@link StoreType#COMPANY}, the numeric ID
     * value(s) are substituted with the contact first-name or company name
     * respectively.  Axes that are not custom, or that belong to a different
     * store-type, are left untouched.</p>
     *
     * <p>All matching contact / company names are resolved in a single batch
     * lookup per entity type before any substitution takes place.</p>
     *
     * @param orgId     the organisation id
     * @param graphAxes the list of graph axes to process (modified in-place)
      * @param reportFieldMap   pre-fetched map of field ID to {@link ReportModularField}, or {@code null} if not available
     */
    public void replaceEntityIdsWithDisplayNames(final @NonNull Long orgId,
                                                 final @NonNull List<GraphAxis<Object>> graphAxes,
                                                 final Map<Long, ReportModularField> reportFieldMap) {
        final Map<GraphAxis<Object>, StoreType> axisToStoreType = new LinkedHashMap<>();
        final Set<Long> contactIds = new HashSet<>();
        final Set<Long> companyIds = new HashSet<>();

        try {
            final Set<Long> allFieldIds = graphAxes.stream()
                    .filter(axis -> !axis.getColumnMap().isEmpty())
                    .map(axis -> {
                        final ReportField reportField = axis.getReportField();
                        final Optional<Long> columnId = axis.getColumnMap().keySet().stream().findFirst();

                        return Objects.nonNull(reportField) ? reportField.getFieldId()
                                : columnId.map(id -> axis.getColumnToFieldIdMap().get(id)).orElse(null);
                    }).filter(Objects::nonNull).collect(Collectors.toSet());
            final Map<Long, ReportModularField> reportModularFieldMap = ModularFieldUtil.filterReportFields(reportFieldMap, allFieldIds);

            CodeBlockExecutor.forEachSafely(graphAxes, graphAxis ->
            {
                if (graphAxis.getColumnMap().isEmpty()) {
                    return;
                }

                final Optional<Long> optionalColumnId = graphAxis.getColumnMap().keySet().stream().findFirst();
                final ReportField reportField = graphAxis.getReportField();
                Long fieldId = (Objects.nonNull(reportField)) ? reportField.getFieldId() : null;

                if (Objects.isNull(fieldId) && optionalColumnId.isPresent()) {
                    final Map<Long, Long> columnToFieldIdMap = graphAxis.getColumnToFieldIdMap();

                    if (Objects.nonNull(columnToFieldIdMap) && !columnToFieldIdMap.isEmpty()) {
                        fieldId = columnToFieldIdMap.get(optionalColumnId.get());
                    }
                }

                if (Objects.isNull(fieldId)) {
                    return;
                }

                final ReportModularField reportModularField = reportModularFieldMap.get(fieldId);

                if ( Objects.isNull( reportModularField ) || !reportModularField.getIsActive()) {
                    return;
                }

                final ModularField modularField = reportModularField.getModularField();
                final Long crmFieldId = (modularField != null) ? modularField.getCrmFieldId() : null;

                if (crmFieldId == null) {
                    return;
                }

                final FieldInfo fieldInfo = fieldService.getField(orgId, crmFieldId);
                final StoreType storeType = fieldInfo.storeType();

                if (storeType.isContactType() || storeType.isCompanyType()) {
                    axisToStoreType.put(graphAxis, storeType);
                    collectEntityIds(graphAxis, storeType.isContactType() ? contactIds : companyIds);
                }
            });
        } catch (Exception exception) {
            LOGGER.error( "Failed to resolve entity IDs from graph axes. Reason: ", exception );
            throw ErrorCode.get(AnalyticsErrorCodes.USER_ID_FETCH_ACTION_FAILED,
                    "i18n.analytics.user.id.fetch.action.failed");
        }

        final Map<Long, String> contactNameMap = fetchContactNames(orgId, contactIds);
        final Map<Long, String> companyNameMap = fetchCompanyNames(orgId, companyIds);

        axisToStoreType.forEach((graphAxis, storeType) ->
                substituteAxisValuesWithNames(graphAxis, storeType.isContactType() ? contactNameMap : companyNameMap));
    }

    /**
     * <p>
     * Fetches contact display names for the given IDs in batches to avoid
     * large IN clause performance degradation.
     * </p>
     *
     * @param orgId      the organization ID
     * @param contactIds the contact IDs to resolve
     * @return map of contact id → full name
     */
    private Map<Long, String> fetchContactNames(final Long orgId, final Set<Long> contactIds) {
        if (contactIds.isEmpty()) {
            return Collections.emptyMap();
        }

        if (contactIds.size() <= NumericalConstant.ONE_HUNDRED) {
            return contactManager.getContactsByIds(orgId, contactIds).stream()
                    .collect(Collectors.toMap(Contact::id, Contact::getFullName));
        }

        final Map<Long, String> result = new HashMap<>(contactIds.size());
        final List<Long> idList = new ArrayList<>(contactIds);

        for (int index = NumericalConstant.ZERO; index < idList.size(); index += NumericalConstant.ONE_HUNDRED) {
            final Set<Long> batch = new HashSet<>(idList.subList(index, Math.min(index + NumericalConstant.ONE_HUNDRED, idList.size())));

            contactManager.getContactsByIds(orgId, batch)
                    .forEach(contact -> result.put(contact.id(), contact.getFullName()));
        }

        return result;
    }

    /**
     * <p>
     * Fetches company display names for the given IDs in batches to avoid
     * large IN clause performance degradation.
     * </p>
     *
     * @param orgId      the organisation id
     * @param companyIds the company IDs to resolve
     * @return map of company id → company name
     */
    private Map<Long, String> fetchCompanyNames(final Long orgId, final Set<Long> companyIds) {
        if (companyIds.isEmpty()) {
            return Collections.emptyMap();
        }

        if (companyIds.size() <= NumericalConstant.ONE_HUNDRED) {
            return companyManager.getCompaniesByIds(orgId, companyIds).stream()
                    .collect(Collectors.toMap(Company::id, Company::getName));
        }

        final Map<Long, String> result = new HashMap<>(companyIds.size());
        final List<Long> idList = new ArrayList<>(companyIds);

        for (int index = NumericalConstant.ZERO; index < idList.size(); index += NumericalConstant.ONE_HUNDRED) {
            final Set<Long> batch = new HashSet<>(idList.subList(index, Math.min(index + NumericalConstant.ONE_HUNDRED, idList.size())));

            companyManager.getCompaniesByIds(orgId, batch)
                    .forEach(company -> result.put(company.id(), company.getName()));
        }

        return result;
    }

    /**
     * <p>
     * Collects the numeric entity ID(s) from a graph axis into {@code entityIds}.
     * Delegates to {@link AxisValueUtils#collectEntityIds}.
     * </p>
     *
     * @param graphAxis the axis to extract IDs from
     * @param entityIds the collection to accumulate IDs into
     */
    private void collectEntityIds(final GraphAxis<Object> graphAxis,
                                  final Collection<Long> entityIds) {
        AxisValueUtils.collectEntityIds(graphAxis, entityIds);
    }

    /**
     * <p>
     * Substitutes all numeric ID value(s) stored in the given axis with their
     * display names from {@code nameMap}.
     * </p>
     *
     * <p>The substitution covers:</p>
     * <ul>
     *   <li>The single {@code value} field (when present)</li>
     *   <li>Every entry in the {@code axisValues} list</li>
     *   <li>Every value in the {@code columnMap}</li>
     * </ul>
     * If an ID is not found in {@code nameMap} the original numeric string is
     * kept as a safe fallback. Empty/null array values are left as-is.
     *
     * @param graphAxis the axis to update in-place
     * @param nameMap   map of entity id → display name
     */
    private void substituteAxisValuesWithNames(final GraphAxis<Object> graphAxis,
                                                final Map<Long, String> nameMap) {
        final Object value = graphAxis.getValue();

        if (value != null && !AxisValueUtils.isEmptyArrayLike(value)) {
            final Object resolved = resolveArrayLikeValue(value, nameMap);

            if (resolved != null) {
                graphAxis.setValue(resolved);
            } else {
                AxisValueUtils.parseToLong(value)
                        .ifPresent(id -> graphAxis.setValue(nameMap.getOrDefault(id, String.valueOf(id))));
            }
        }
        final ListIterator<AxisValue<Object>> iterator = graphAxis.getAxisValues().listIterator();

        while (iterator.hasNext()) {
            final Object axisVal = iterator.next().getValue();

            if (axisVal != null && !AxisValueUtils.isEmptyArrayLike(axisVal)) {
                final Object resolved = resolveArrayLikeValue(axisVal, nameMap);

                if (resolved != null) {
                    iterator.set(AxisValue.create(resolved));
                } else {
                    AxisValueUtils.parseToLong(axisVal)
                            .ifPresent(id -> iterator.set(AxisValue.create((Object) nameMap.getOrDefault(id, String.valueOf(id)))));
                }
            }
        }

        graphAxis.getColumnMap().replaceAll((colId, val) ->
        {
            if (val == null || AxisValueUtils.isEmptyArrayLike(val)) {
                return val;
            }

            final Object resolved = resolveArrayLikeValue(val, nameMap);

            if (resolved != null) {
                return resolved;
            }

            return AxisValueUtils.parseToLong(val)
                    .map(id -> (Object) nameMap.getOrDefault(id, String.valueOf(id)))
                    .orElse(val);
        });
    }

    /**
     * Resolves array-like values (Collection, Java arrays, string array representations
     * like {@code "[1, 2, 3]"}) by replacing each numeric ID with its display name.
     * Delegates element iteration to {@link AxisValueUtils#mapArrayLikeValue}.
     *
     * @param value   the value to resolve
     * @param nameMap map of entity id → display name
     * @return a {@link List} of resolved names if the value is array-like, or {@code null} if scalar
     */
    private List<String> resolveArrayLikeValue(final Object value, final Map<Long, String> nameMap) {
        return AxisValueUtils.mapArrayLikeValue(value, element ->
                AxisValueUtils.parseToLong(element)
                        .map(id -> nameMap.getOrDefault(id, String.valueOf(id)))
                        .orElse(String.valueOf(element)));
    }

}
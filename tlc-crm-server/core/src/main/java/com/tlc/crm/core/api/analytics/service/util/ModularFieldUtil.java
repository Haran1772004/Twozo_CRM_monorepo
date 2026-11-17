package com.tlc.crm.core.api.analytics.service.util;

import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for common operations on {@link ReportModularField} and {@link ModularField}.
 *
 * <p>Provides utility methods for filtering report fields by ID subsets or by module.
 * Field resolution is handled directly by {@link ReportModularField#getModularField()}.</p>
 *
 * @author Karthick V
 * @version 1.0
 */
public final class ModularFieldUtil {

    private ModularFieldUtil() {
    }

    /**
     * Filters a {@code reportFieldMap} to only the entries whose keys are in {@code fieldIds},
     * returning the filtered sub-map.
     *
     * @param reportFieldMap the full report field map
     * @param fieldIds       the field IDs to retain
     * @return filtered sub-map containing only matching entries
     */
    public static Map<Long, ReportModularField> filterReportFields(final Map<Long, ReportModularField> reportFieldMap,
                                                                   final Collection<Long> fieldIds) {
        final Map<Long, ReportModularField> filtered = new HashMap<>();

        for (final Long fieldId : fieldIds) {
            final ReportModularField field = reportFieldMap.get(fieldId);

            if (Objects.nonNull(field)) {
                filtered.put(fieldId, field);
            }
        }
        return filtered;
    }

    /**
     * Filters a collection of {@link ReportModularField}s to only those belonging to the given module.
     *
     * @param moduleId     the module identifier to filter by
     * @param reportFields the full collection of report modular fields
     * @return list of report modular fields belonging to the specified module
     */
    public static List<ReportModularField> filterByModule(final Integer moduleId,
                                                          final Collection<ReportModularField> reportFields) {
        return reportFields.stream()
                .filter(reportModularField -> {
                    final ModularField modularField = reportModularField.getModularField();

                    return Objects.nonNull(modularField) && Objects.equals(modularField.getModuleId(), moduleId);
                })
                .toList();
    }
}

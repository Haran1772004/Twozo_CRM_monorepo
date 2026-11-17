package com.tlc.crm.core.action.analytics.reports.data.export.internal;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.api.analytics.general.keys.common.CommonKeys;

import java.util.Objects;

/**
 * Utility methods for safe export pagination bounds.
 *
 * @author Saravanan
 * @version 1.0
 */
final class ExportPaginationUtil {

    private ExportPaginationUtil() {
    }

    /**
     * Resolves the start index from limit payload.
     *
     * <p>If limit is missing or start is non-positive, the provided default is returned.</p>
     *
     * @param requestLimitJson optional limit object from request payload
     * @param defaultStartIndex fallback start index
     * @return validated start index
     */
    static int getStartIndex(final JsonObject requestLimitJson,
                             final int defaultStartIndex) {
        if (Objects.isNull(requestLimitJson)) {
            return defaultStartIndex;
        }

        final int requestedStart = requestLimitJson.optInt(CommonKeys.START, defaultStartIndex);
        return requestedStart <= NumericalConstant.ZERO ? defaultStartIndex : requestedStart;
    }

    /**
     * Resolves the inclusive end index from start index and requested count.
     *
     * <p>If limit is missing, the export is treated as unbounded by returning {@link Integer#MAX_VALUE}.</p>
     *
     * @param requestLimitJson optional limit object from request payload
     * @param startIndex resolved start index
     * @param defaultCount fallback count when requested count is invalid
     * @return inclusive end index
     */
    static int getEndIndex(final JsonObject requestLimitJson,
                           final int startIndex,
                           final int defaultCount) {
        if (Objects.isNull(requestLimitJson)) {
            return Integer.MAX_VALUE;
        }

        final int requestedCount = requestLimitJson.optInt(CommonKeys.COUNT, defaultCount);
        final int safeCount = requestedCount <= NumericalConstant.ZERO ? defaultCount : requestedCount;
        return startIndex + safeCount - NumericalConstant.ONE;
    }
}



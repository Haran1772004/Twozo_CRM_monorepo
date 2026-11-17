package com.tlc.analytics.core.api.internal.request.dynamic.drillDown;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.Map;

/**
 * <p>
 *   Represents the drill-down information for a specific context or view
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@Getter
@Builder
public class DrillDownInfo
{
    private @NonNull String currentColumnName;

    /***
     * Long          - Current DrillDown Level,
     * DrillDownPath - Contains the DrillDown Data and path info.
     * **/
    private @NonNull Map<Long, DrillDownPath> drillDownPath;
}

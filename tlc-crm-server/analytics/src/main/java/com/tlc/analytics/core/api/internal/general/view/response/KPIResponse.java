package com.tlc.analytics.core.api.internal.general.view.response;

import com.tlc.analytics.core.api.ViewResponse;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.analytics.core.api.KPIRequest;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Represents a response containing widget data.
 *
 * @author Jamesh
 * @version 1.0
 */
@Getter
@SuperBuilder
public class KPIResponse implements ViewResponse<KPIRequest>
{

    @NonNull
    private final List<GraphAxis<Object>> graphAxes;

    @NotNull
    @Override
    public @NonNull JsonObject getJsonResponse( KPIRequest request )
    {
        return Json.object();
    }

    /**
     * Represents a response containing widget data.
     */
    @Override
    public final @NonNull List<GraphAxis<Object>> getGraphAxis( final @NonNull KPIRequest request )
    {
        return graphAxes;
    }
}

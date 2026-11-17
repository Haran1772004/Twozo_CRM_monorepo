package com.tlc.analytics.config.model.view.report.dynamic.underlying;

import com.tlc.analytics.config.keys.view.ViewModelKeys;
import com.tlc.analytics.config.model.view.report.AnalyticsReport;

import com.tlc.entity.annotation.JsonKey;

import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Collection;

/**
 * <p>
 * A model that manages the underlying data details
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
@Getter
@SuperBuilder
public class AnalyticsUnderlyingData extends AnalyticsReport
{
    @JsonKey( key = ViewModelKeys.VIEW_ID )
    @NotNull( groups  = { Group.Create.class, Group.Update.class, Group.Delete.class },
              message = "i18n.analytics.underlying.invalid.view.id" )
    private Long viewId;

    private Collection<Long> configuredFields;

    private Collection<AnalyticsConfiguredField> configuredFieldDetails;

    private Collection<AnalyticsFilterDetail> filterDetails;

    private AnalyticsLimit limit;
    private AnalyticsSort analyticsSort;
}

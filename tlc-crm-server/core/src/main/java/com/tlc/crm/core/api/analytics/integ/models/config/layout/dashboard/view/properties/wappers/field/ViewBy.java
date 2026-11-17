package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.field;

import com.tlc.crm.core.api.analytics.entity.config.fields.bucket.FieldBucketUnit;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.ANAField;
import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Responsible for holds the {@link ViewBy} details
 *
 * @author james
 * @version 1.0
 */
@Getter
@Setter
@SuperBuilder
public class ViewBy
{
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.invalid.metrics.info" )
    private ANAField metricsInfo;

    @Valid
    private FieldBucketUnit fieldBucketUnit;

    private boolean isBucketApplied;

    private boolean isDeleted;

    private boolean isPeriodicFilterField;

    private boolean isDuplicate;
}

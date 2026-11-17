package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers;

import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.field.Duration;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.field.Metrics;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.field.ViewBy;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Responsible for holds the {@link ANAFieldsWrapper} details
 *
 * @author james
 * @version 1.0
 */
@Getter
@Setter
@SuperBuilder
public class ANAFieldsWrapper
{
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.field.wrapper.invalid.metrics" )
    private final Metrics metrics;

    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.field.wrapper.invalid.view.by" )
    private final Collection<ViewBy> viewBy;

    @Nullable
    private final Duration duration;

    private final int order;
}

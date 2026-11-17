package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.field;

import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.ANAField;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.filter.CustomFilter;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.filter.PeriodicFilter;

import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Collection;

/**
 * Responsible for holds the {@link Metrics} details
 *
 * @author james
 * @version 1.0
 */
@Getter
@Setter
@SuperBuilder
public class Metrics
{

    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.invalid.metrics.info" )
    private ANAField metricsInfo;

    private boolean isDeleted;

    @Valid
    private Collection<PeriodicFilter> periodicFilter;

    @Valid
    private Collection<CustomFilter> customFilters;

    private int order;

    private boolean isDuplicate;
}

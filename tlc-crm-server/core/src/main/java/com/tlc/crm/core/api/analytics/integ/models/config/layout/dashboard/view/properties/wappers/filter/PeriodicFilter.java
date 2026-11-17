package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.filter;

import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.ANAField;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Responsible for holds the {@link PeriodicFilter} details
 *
 * @author james
 * @version 1.0
 */
@Getter
@Setter
@SuperBuilder
public class PeriodicFilter
{
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.invalid.metrics.info" )
    private ANAField metricsInfo;

    private boolean isDeleted;
}

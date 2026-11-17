package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers;

import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.filter.OperatorType;
import com.tlc.analytics.config.model.view.report.ReportField;

import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Responsible for holds the {@link ANAField} details
 *
 * @author james
 * @version 1.0
 */
@Getter
@SuperBuilder
public class ANAField
{
    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.field.invalid.root.table.id" )
    private Long rootTableId;

    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.field.invalid.field.id" )
    private Long crmFieldId;

    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.field.invalid.column.id" )
    private Long anaColumnId;

    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.field.invalid.order" )
    private Long order;

    @Valid
    @Nullable
    private OperationType operationType;

    @Valid
    @Nullable
    private OperatorType operatorType;

    @NotNull( groups = { Group.Create.class }, message = "i18n.crm.analytics.field.invalid.report.fields" )
    private Collection<ReportField> reportFields;

    @Valid
    private Collection<FilterField> filterFields;

    private Collection<Long> columnIds;
}

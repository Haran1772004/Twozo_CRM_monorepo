package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMREPORTVIEWFIELDMAPPER;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Table;
import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Collection;

/**
 * <p>
 * Manages CRM Filed and Analytics view
 * </p>
 *
 * @author James
 * @version 1.0
 */

@Getter
@SuperBuilder
@Table( name = CRMREPORTVIEWFIELDMAPPER.TABLE )
public class CRMRViewFieldMapper extends CRMBaseModel
{
    @Column( name = CRMREPORTVIEWFIELDMAPPER.VIEW_ID )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.crm.analytics.field.mapper.invalid.view.id"  )
    private Long viewId;

    @Column( name = CRMREPORTVIEWFIELDMAPPER.CRM_VIEW_ID )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.crm.analytics.field.mapper.invalid.crm.view.id"  )
    private Long crmViewId;

    @Column( name = CRMREPORTVIEWFIELDMAPPER.MODULAR_FIELD_ID )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.crm.analytics.field.mapper.invalid.modular.field.id"  )
    private Long modularFieldId;

    @Column( name = CRMREPORTVIEWFIELDMAPPER.ANA_ACTUAL_COLUMN )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.crm.analytics.field.mapper.invalid.actual.column"  )
    private Long anaActualColumnId;

    @Column( name = CRMREPORTVIEWFIELDMAPPER.ANA_ACTUAL_OPERATION )
    private Long anaActualOperationId;

    @Column( name = CRMREPORTVIEWFIELDMAPPER.ANA_ACTUAL_COMPARATOR )
    private Long anaActualComparatorId;

    @Column( name = CRMREPORTVIEWFIELDMAPPER.IS_FILTER_FILED )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.crm.analytics.field.mapper.invalid.filter.column"  )
    private boolean isFilterColumn;

    // To Map the Filter with Y column.
    @Column( name = CRMREPORTVIEWFIELDMAPPER.ANA_Y_COLUMN_ID )
    private Long anaYColumnID;

    @Column( name = CRMREPORTVIEWFIELDMAPPER.IS_BUCKET_BY_APPLIED )
    private boolean isBuckedApplied;

    @Column( name = CRMREPORTVIEWFIELDMAPPER.IS_UNIT_APPLIED )
    private boolean isUnitApplied;

    @Column( name = CRMREPORTVIEWFIELDMAPPER.IS_PERIODIC_FILTER_APPLIED )
    private boolean isPeriodFilterApplied;

    @Column( name = CRMREPORTVIEWFIELDMAPPER.FILTER_ORDER )
    private Long filterOrder;

    @Column( name = CRMREPORTVIEWFIELDMAPPER.ORDER )
    private int order;

    @Column( name = CRMREPORTVIEWFIELDMAPPER.IS_PERIODIC_FILTER_FIELD )
    private boolean isPeriodicFilterField;

    @Column( name = CRMREPORTVIEWFIELDMAPPER.AXIS )
    private Integer axis;

    @Join( relation   = Relation.ONE_TO_MANY,
           nullable   = true,
           joinTables = @JoinTable( aliasName   = "mapperFields",
                                     joinColumns = @JoinColumn( referenceColumn = "VIEW_FIELD_MAPPER" ),
                                     entityClass = CRMRMapperColumn.class ) )
    @Valid
    private Collection<CRMRMapperColumn> mapperColumns;
}

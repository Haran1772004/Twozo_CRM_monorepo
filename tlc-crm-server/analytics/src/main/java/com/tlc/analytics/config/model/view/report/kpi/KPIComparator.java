package com.tlc.analytics.config.model.view.report.kpi;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.sql.resource.ANAWIDGETCOMPARATOR;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the {@link KPIComparator}
 * </p>
 *
 * @author james
 * @version 1.0
 */
@Getter
@SuperBuilder
@Table( name = ANAWIDGETCOMPARATOR.TABLE )
public class KPIComparator extends BaseModel
{

    @Column( name = ANAWIDGETCOMPARATOR.VIEW_ID )
    private Long viewId;

    @Column( name = ANAWIDGETCOMPARATOR.COMPARATOR_TYPE )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.widget.comparator.invalid.type" )
    private Integer comparatorType;

    @Column( name = ANAWIDGETCOMPARATOR.COMPARATOR_SIDE )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.widget.comparator.invalid.side" )
    private Integer comparatorSide;

    @Column( name = ANAWIDGETCOMPARATOR.FROM )
    private Long from;

    @Column( name = ANAWIDGETCOMPARATOR.TO )
    private Long to;

    @Column( name = ANAWIDGETCOMPARATOR.CREATED_TIME )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.widget.comparator.invalid.created.time" )
    private Long createdTime;

    @Column( name = ANAWIDGETCOMPARATOR.MODIFIED_TIME )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.widget.comparator.invalid.modified.time" )
    private Long modifiedTime;

    public KPIComparatorType getComparatorType()
    {
        return KPIComparatorType.getComparatorType( comparatorType );
    }

    public OperationType getOperationType()
    {
        return OperationType.getOperationType( comparatorType );
    }

    public KPIComparatorSide getComparatorSide()
    {
        return KPIComparatorSide.getComparatorSide( comparatorSide );
    }
}

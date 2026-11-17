package com.tlc.analytics.config.model.table;

import com.tlc.analytics.config.keys.CommonBaseModelKeys;
import com.tlc.analytics.config.model.BaseModel;

import com.tlc.analytics.sql.resource.ANACOLUMNRELATION;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Table;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the {@link ColumnRelation}
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
@Table( name = ANACOLUMNRELATION.TABLE )
@SuperBuilder
@Getter
public class ColumnRelation extends BaseModel
{
    @JsonKey( key = CommonBaseModelKeys.REL_ID )
    @Column( name = ANACOLUMNRELATION.REL_ID )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.related.column.invalid.rel.id" )
    private Long relId;

    @JsonKey( key = CommonBaseModelKeys.PARENT_COLUMN_ID )
    @Column( name = ANACOLUMNRELATION.PARENT_COL_ID )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.related.column.invalid.parent.column.id" )
    private Long parentColId;

    @JsonKey( key = CommonBaseModelKeys.CHILD_COLUMN_ID )
    @Column( name = ANACOLUMNRELATION.CHILD_COL_ID )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.related.column.invalid.child.column.id" )
    private Long childColId;

    @JsonKey( key = CommonBaseModelKeys.JOIN_CONDITION )
    @Column( name = ANACOLUMNRELATION.REL_CONDITION )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.related.column.invalid.join.condition" )
    private Integer joinCondition;

    /**
     * Resolves join condition enum from persisted join condition id.
     *
     * @return The resolved {@link JoinCondition}.
     */
    public JoinCondition getJoinCondition()
    {
        return JoinCondition.getJoinTypeByCode( joinCondition );
    }

}

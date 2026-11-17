package com.tlc.analytics.config.model.table;

import com.tlc.analytics.config.keys.table.TableRelationKeys;
import com.tlc.analytics.config.model.CommonBaseModel;

import com.tlc.analytics.sql.resource.ANACOLUMNRELATION;
import com.tlc.analytics.sql.resource.ANATABLERELATION;
import com.tlc.entity.annotation.Cascade;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Table;

import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the {@link TableRelation}
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@Table( name = ANATABLERELATION.TABLE )
@SuperBuilder
@Getter
public class TableRelation extends CommonBaseModel
{
    @JsonKey( key = TableRelationKeys.PARENT_TABLE_ID)
    @Column( name = ANATABLERELATION.PARENT_TABLE_ID )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.relation.invalid.parent.table.id" )
    private Long parentTableId;

    @JsonKey( key = TableRelationKeys.CHILD_TABLE_ID )
    @Column( name = ANATABLERELATION.CHILD_TABLE_ID )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.relation.invalid.child.table.id" )
    private Long childTableId;

    @JsonKey( key = TableRelationKeys.REL_TYPE )
    @Column( name = ANATABLERELATION.REL_TYPE )
    @Getter( AccessLevel.NONE )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.relation.invalid.type.id" )
    private Integer relTypeId;

    @JsonKey( key = TableRelationKeys.REL_COL_INFO )
    @Join( relation = Relation.ONE_TO_ONE,
           cascades = { Cascade.SELECT, Cascade.PERSIST },
           joinTables = @JoinTable( joinColumns = @JoinColumn( referenceColumn = ANACOLUMNRELATION.REL_ID ),
                                    entityClass = ColumnRelation.class ) )
    @Valid
    private ColumnRelation columnRelation;
}
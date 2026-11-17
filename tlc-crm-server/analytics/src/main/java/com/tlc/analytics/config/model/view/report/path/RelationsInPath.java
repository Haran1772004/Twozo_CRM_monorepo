package com.tlc.analytics.config.model.view.report.path;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.sql.resource.ANARELATIONSINPATH;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Table;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the {@link RelationsInPath} details
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
@Table( name = ANARELATIONSINPATH.TABLE )
@SuperBuilder
@Getter
public class RelationsInPath extends BaseModel
{
    @JsonKey( key = "pathId" )
    @Column( name = ANARELATIONSINPATH.PATH_ID )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.relation.invalid.path.id" )
    private Long pathId;

    @JsonKey( key = "relId" )
    @Column( name = ANARELATIONSINPATH.RELATION_ID )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.relation.invalid.relation.id" )
    private Long relationId;

    @JsonKey( key = "relationIndex" )
    @Column( name = ANARELATIONSINPATH.RELATION_INDEX )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.relation.invalid.rel.index" )
    private Integer relationIndex;

    @Accessors( fluent = true )
    @JsonKey( key = "isReverseJoin" )
    @Column( name = ANARELATIONSINPATH.IS_REVERSE_JOIN )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.relation.invalid.rel.reverse.join" )
    private Boolean isReverseJoin;
}

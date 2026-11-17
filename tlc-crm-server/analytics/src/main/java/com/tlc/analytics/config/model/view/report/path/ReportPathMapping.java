package com.tlc.analytics.config.model.view.report.path;

import com.tlc.analytics.config.keys.table.ReportPathMappingKeys;
import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.sql.resource.ANAPATH;
import com.tlc.analytics.sql.resource.ANAREPORTPATHMAPPING;

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

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the {@link ReportPathMapping} details
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table( name = ANAREPORTPATHMAPPING.TABLE )
public class ReportPathMapping extends BaseModel
{
    @JsonKey( key = ReportPathMappingKeys.PATH_ID )
    @Column( name = ANAREPORTPATHMAPPING.PATH_ID )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.path.mapping.invalid.path.id" )
    private Long pathId;

    @JsonKey( key = ReportPathMappingKeys.VIEW_ID )
    @Column( name = ANAREPORTPATHMAPPING.VIEW_ID )
    @NotNull( groups = { Group.Create.class },
              message = "i18n.analytics.path.mapping.invalid.view.id" )
    private Long viewId;

    @JsonKey( key = ReportPathMappingKeys.PATH_INFO )
    @Join( relation = Relation.ONE_TO_ONE,
           cascades = { Cascade.SELECT, Cascade.PERSIST },
           joinTables = @JoinTable( joinColumns = @JoinColumn( localColumn = ANAREPORTPATHMAPPING.PATH_ID,
                                                               referenceColumn = ANAPATH.ID ),
                                    entityClass = PathInfo.class ) )
    @Valid
    private PathInfo pathInfo;
}

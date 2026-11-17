package com.tlc.analytics.config.model.view.report.path;

import com.tlc.analytics.config.keys.table.TableRelationKeys;
import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.sql.resource.ANAPATH;
import com.tlc.analytics.sql.resource.ANARELATIONSINPATH;

import com.tlc.entity.annotation.Cascade;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Table;

import jakarta.validation.Valid;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Collection;

/**
 * <p>
 * A model that manages the {@link PathInfo} details
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
@Table( name = ANAPATH.TABLE)
@SuperBuilder
@Getter
public class PathInfo extends BaseModel
{
    @JsonKey( key = TableRelationKeys.REL_IN_PATHS )
    @Join( relation = Relation.ONE_TO_MANY,
           cascades = { Cascade.SELECT, Cascade.PERSIST },
           joinTables = @JoinTable( joinColumns = @JoinColumn( referenceColumn = ANARELATIONSINPATH.PATH_ID ),
                                    entityClass = RelationsInPath.class ) )
    @Valid
    private Collection<RelationsInPath> relationsInPaths;
}

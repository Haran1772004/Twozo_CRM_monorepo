package com.tlc.analytics.config.model.view.layout;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.analytics.config.model.view.ViewDetails;
import com.tlc.analytics.config.keys.view.layout.BaseViewKeys;

import com.tlc.entity.annotation.Cascade;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Relation;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;


/**
 * <p>
 * A model that manages the base view layout details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@SuperBuilder
@Getter
public class BaseViewLayout extends BaseModel
{
    @JsonKey( key = BaseViewKeys.VIEW_ID)
    @Column( name = BaseViewKeys.VIEW_ID_COLUMN )
    private Long viewId;

    @JsonKey( key = BaseViewKeys.VIEW )
    @Join( relation = Relation.ONE_TO_ONE,
           cascades = Cascade.ALL,
           joinTables = @JoinTable( aliasName = "view",
                                    joinColumns = @JoinColumn( localColumn = BaseViewKeys.VIEW_ID_COLUMN,
                                                               referenceColumn = BaseViewKeys.ID ),
                                    entityClass = ViewDetails.class ) )
    @NotNull
    private ViewDetails viewDetails;

    private boolean isSharable;
}

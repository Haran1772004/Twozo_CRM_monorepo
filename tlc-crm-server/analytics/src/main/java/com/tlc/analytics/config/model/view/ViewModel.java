package com.tlc.analytics.config.model.view;

import com.tlc.analytics.config.keys.view.ViewModelKeys;
import com.tlc.analytics.sql.resource.ANAVIEWDETAILS;
import com.tlc.entity.annotation.Cascade;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Column;
import com.tlc.analytics.config.model.BaseModel;
import com.tlc.entity.annotation.Relation;
import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that manages the view details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 * @see BaseModel
 */
@SuperBuilder
@Getter
public class ViewModel extends BaseModel
{
    @JsonKey( key = ViewModelKeys.VIEW_ID )
    @Column( name = ViewModelKeys.VIEW_ID_COLUMN )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.view.model.invalid.view.id" )
    private Long viewId;

    @JsonKey( key = ViewModelKeys.VIEW_DETAILS )
    @Join( relation = Relation.ONE_TO_ONE,
           cascades = { Cascade.PERSIST, Cascade.SELECT },
           joinTables = @JoinTable( aliasName = "viewDetails", joinColumns = @JoinColumn( localColumn = ViewModelKeys.VIEW_ID_COLUMN,
                                                                                          referenceColumn = ANAVIEWDETAILS.ID ),
                                    entityClass = ViewDetails.class ) )
    @NotNull( groups = { Group.Create.class }, message = "" )
    @Valid
    private ViewDetails viewDetails;
}

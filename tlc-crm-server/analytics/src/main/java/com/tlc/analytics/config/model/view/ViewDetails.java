package com.tlc.analytics.config.model.view;

import com.tlc.analytics.sql.resource.ANAPERMANENTLINK;
import com.tlc.analytics.sql.resource.ANAVIEWDETAILS;
import com.tlc.analytics.config.keys.view.ViewKeys;
import com.tlc.analytics.config.model.view.share.PermanentLink;
import com.tlc.analytics.config.model.CommonBaseModel;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.annotation.JsonKey;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Table;
import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.SuperBuilder;


/**
 * <p>
 * A model that manages the view details
 * </p>
 *
 * @author Aswath
 * @version 1.0
 * @see CommonBaseModel
 */
@SuperBuilder
@Getter
@Table( name = ANAVIEWDETAILS.TABLE )
public class ViewDetails extends CommonBaseModel
{
    @JsonKey( key = ViewKeys.VIEW_TYPE_ID )
    @Getter( AccessLevel.NONE )
    @Column( name = ANAVIEWDETAILS.VIEW_TYPE )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.view.details.invalid.view.type" )
    private Integer viewTypeId;

    @JsonKey( key = ViewKeys.CREATOR )
    @Column( name = ANAVIEWDETAILS.CREATED_BY )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.view.details.invalid.created.by" )
    private Long createdBy;

    @JsonKey( key = ViewKeys.EDITOR )
    @Column( name = ANAVIEWDETAILS.MODIFIED_BY )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.view.details.invalid.modified.by" )
    private Long modifiedBy;

    @JsonKey( key = ViewKeys.CREATOR_AT )
    @Column( name = ANAVIEWDETAILS.CREATED_TIME )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.view.details.invalid.created.time" )
    private Long createdTime;

    @JsonKey( key = ViewKeys.IS_CUSTOM_VIEW )
    @Column( name = ANAVIEWDETAILS.IS_CUSTOM_VIEW )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.view.details.invalid.custom.view" )
    private Boolean isCustomView;

    @JsonKey( key = ViewKeys.IS_HIDDEN )
    @Column( name = ANAVIEWDETAILS.IS_HIDDEN )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.view.details.invalid.hidden.view" )
    private Boolean isHidden;

    @JsonKey( key = ViewKeys.DESCRIPTION )
    @Column( name = ANAVIEWDETAILS.DESCRIPTION )
    @NotEmpty( groups = { Group.Create.class }, message = "i18n.analytics.view.details.invalid.description" )
    @Size( max = 200 )
    private String description;

    @JsonKey( key = ViewKeys.PERMA_LINK )
    @Join( relation = Relation.ONE_TO_ONE,
           nullable = true,
           joinTables = @JoinTable( aliasName = "permanentLink", joinColumns = @JoinColumn( referenceColumn = ANAPERMANENTLINK.VIEW_ID ),
                                    entityClass = PermanentLink.class ) )
    @Valid
    private PermanentLink permanentLink;

    @Column( name = ANAVIEWDETAILS.MOVE_TO_TRASH )
    @NotNull( groups = { Group.Create.class }, message = "i18n.analytics.view.details.invalid.move.to.trash" )
    private Boolean moveToTrash;

    public ViewType getViewType()
    {
        return ViewType.getType( viewTypeId );
    }
}

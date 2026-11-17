package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;

import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMViewType;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Table( name="CRMViewType" )
public class CRMReportsViewType extends CRMBaseModel
{

    @Column( name = "TYPE" )
    @NotNull( groups = { Group.Create.class },
            message = "" )
    @Size( max = 50 )
    private String viewType;

    public String getDisplayName()
    {
        return getViewType().getI18nKeyOfDisplayName();
    }

    public CRMViewType getViewType()
    {
        return CRMViewType.getCrmViewType( id().intValue() );
    }

    public String getAliasName()
    {
        return getViewType().getAliasName();
    }
}

package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.experimental.SuperBuilder;


@SuperBuilder
@Getter
@Table( name="CRMANAViewMapper" )
public class CRMANAViewMapper extends CRMBaseModel
{
    @Column( name = "ANA_VIEW_DETAILS_ID" )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.crm.analytics.field.mapper.invalid.view.details.id"  )
    private Long anaViewDetailsId;

    @Column( name = "ANA_VIEW_ID" )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.crm.analytics.field.mapper.invalid.view.id"  )
    private Long anaViewId;

    @Column( name = "CRM_VIEW_ID" )
    @NotNull( groups = { Group.Create.class, Group.Update.class }, message = "i18n.crm.analytics.field.mapper.invalid.crm.view.id"  )
    private Long crmViewId;
}

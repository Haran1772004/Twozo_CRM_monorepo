package com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMFIELDMODULERELATION;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * A model that represents the {@link CRMFieldModuleRelation}
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */
@SuperBuilder
@Getter
@Table(name = CRMFIELDMODULERELATION.TABLE)
public class CRMFieldModuleRelation extends CRMBaseModel {
    @Column(name = CRMFIELDMODULERELATION.MODULAR_FIELD_ID)
    @NotNull(groups = {Group.Create.class}, message = "i18n.crm.analytics.field.mapper.invalid.modular.field.id")
    private Long modularFieldId;

    @Column(name = CRMFIELDMODULERELATION.PRIMARY_MODULE_ID)
    @NotNull(message = "i18n.crm.analytics.invalid.primary.module.id")
    private Integer primaryModuleId;

    @Column(name = CRMFIELDMODULERELATION.SECONDARY_MODULE_ID)
    @NotNull(message = "i18n.crm.analytics.invalid.secondary.module.id")
    private Integer secondaryModuleId;
}

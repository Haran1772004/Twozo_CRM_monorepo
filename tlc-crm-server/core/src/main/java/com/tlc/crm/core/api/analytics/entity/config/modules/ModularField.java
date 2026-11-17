package com.tlc.crm.core.api.analytics.entity.config.modules;

import com.tlc.commons.json.Json;
import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMMODULARFIELD;
import com.tlc.crm.core.sql.resource.CRMSYSTEMMODULARFIELD;

import com.tlc.entity.annotation.Column;

import com.tlc.i18n.I18nKey;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Objects;

/**
 * <p>
 * A model that manages the crm modular fields
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
@SuperBuilder
public class ModularField extends CRMBaseModel
{

    @Column( name = CRMSYSTEMMODULARFIELD.MODULE_ID )
    @NotNull( message = "i18n.crm.analytics.invalid.modular.id")
    private Integer moduleId;

    @Column( name = CRMSYSTEMMODULARFIELD.FIELD_NAME )
    @NotNull( message = "i18n.crm.analytics.invalid.field.name" )
    @Size( max = 100, message = "i18n.crm.analytics.field.name.max.length" )
    private I18nKey fieldName;

    @Column( name = CRMMODULARFIELD.CRM_FIELD_ID )
    private Long crmFieldId;

    @Column( name = CRMSYSTEMMODULARFIELD.FIELD_CONFIG)
    private String fieldConfigAsString;

    /**
     * Returns parsed modular field configuration from serialized json.
     *
     * @return The {@link ModularFieldConfig}, or {@code null} when config is absent.
     */
    public ModularFieldConfig getModularFieldConfig() {
        return Objects.isNull(fieldConfigAsString) ? null : new ModularFieldConfig(Json.object(fieldConfigAsString));
    }

    /**
     * Returns the module type mapped for this modular field.
     *
     * @return The resolved {@link ModuleType}.
     */
    public ModuleType getModuleType() { return ModuleType.getModuleType( moduleId ); }
}
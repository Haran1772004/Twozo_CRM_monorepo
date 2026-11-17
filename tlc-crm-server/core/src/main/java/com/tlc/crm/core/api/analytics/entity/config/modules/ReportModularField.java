package com.tlc.crm.core.api.analytics.entity.config.modules;

import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;
import com.tlc.crm.core.sql.resource.CRMSYSTEMMODULARFIELD;
import com.tlc.crm.core.sql.resource.CRMCUSTOMMODULARFIELD;
import com.tlc.crm.core.sql.resource.OMCRMMODULARFIELD;

import com.tlc.entity.annotation.Join;
import com.tlc.entity.annotation.Table;
import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.JoinTable;
import com.tlc.entity.annotation.JoinColumn;
import com.tlc.entity.annotation.Relation;
import com.tlc.entity.annotation.Cascade;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Represents analytics modular field entry that maps to either system or custom modular field.
 * Author: karthick V
 * Version: 1.0
 */
@Getter
@SuperBuilder
@Table(name = OMCRMMODULARFIELD.TABLE)
public class ReportModularField extends CRMBaseModel {

    @Column(name = OMCRMMODULARFIELD.CRM_SYSTEM_MODULAR_FIELD_ID)
    private Long systemFieldId;

    @Column(name = OMCRMMODULARFIELD.CRM_CUSTOM_MODULAR_FIELD_ID)
    private Long customFieldId;

    @Column(name = OMCRMMODULARFIELD.IS_CUSTOM)
    private boolean isCustom;

    @Column(name = OMCRMMODULARFIELD.IS_ACTIVE)
    @Setter
    private Boolean isActive;

    @Join( relation = Relation.ONE_TO_ONE, nullable = true, cascades = { Cascade.SELECT },
            joinTables = @JoinTable( joinColumns = @JoinColumn(
                    localColumn = OMCRMMODULARFIELD.CRM_SYSTEM_MODULAR_FIELD_ID,
                    referenceColumn = CRMSYSTEMMODULARFIELD.ID
            ), entityClass = CRMSystemModularField.class ) )
    private CRMSystemModularField systemModularField;

    @Join( relation = Relation.ONE_TO_ONE, nullable = true, cascades = { Cascade.PERSIST, Cascade.SELECT },
            joinTables = @JoinTable( joinColumns = @JoinColumn(
                    localColumn = OMCRMMODULARFIELD.CRM_CUSTOM_MODULAR_FIELD_ID,
                    referenceColumn = CRMCUSTOMMODULARFIELD.ID
            ), entityClass = CRMCustomModularField.class ) )
    private CRMCustomModularField customModularField;

    /**
     * Returns the resolved modular field entity based on custom flag.
     *
     * @return The active {@link ModularField} instance.
     */
    public ModularField getModularField() {
        if (isCustom) {
            return customModularField;
        } else {
            return systemModularField;
        }
    }
}
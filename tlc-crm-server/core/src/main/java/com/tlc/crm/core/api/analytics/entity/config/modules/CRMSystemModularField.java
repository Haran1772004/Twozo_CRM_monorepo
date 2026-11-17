package com.tlc.crm.core.api.analytics.entity.config.modules;

import com.tlc.crm.core.sql.resource.CRMSYSTEMMODULARFIELD;

import com.tlc.entity.annotation.Column;
import com.tlc.entity.annotation.Table;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Represents system modular field mapping used in analytics configuration.
 * Author: karthick V
 * Version: 1.0
 */
@Getter
@SuperBuilder
@Table(name = CRMSYSTEMMODULARFIELD.TABLE)
public class CRMSystemModularField extends ModularField {

    @Column(name = CRMSYSTEMMODULARFIELD.ANALYTICS_FIELD_ID)
    private Long anaFieldId;

    private Long operationalFieldId;
}

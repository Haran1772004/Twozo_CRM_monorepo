package com.tlc.crm.core.api.analytics.entity.config.modules;

import com.tlc.crm.core.sql.resource.CRMCUSTOMMODULARFIELD;

import com.tlc.entity.annotation.Table;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Represents custom modular field mapping used in analytics configuration.
 * Author: karthick V
 * Version: 1.0
 */
@Getter
@SuperBuilder
@Table(name = CRMCUSTOMMODULARFIELD.TABLE)
public class CRMCustomModularField extends ModularField {

}

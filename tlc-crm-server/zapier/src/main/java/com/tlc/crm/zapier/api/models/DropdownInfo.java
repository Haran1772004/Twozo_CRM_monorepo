package com.tlc.crm.zapier.api.models;

import com.tlc.crm.entity.models.type.Option;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

/**
 * Represents metadata for a dynamic dropdown field used in Zapier integrations.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Getter
@Setter
public final class DropdownInfo {

    private String name;
    private Collection<Option> choices;
    private boolean isMultiSelect;
}

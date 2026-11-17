package com.tlc.crm.auto.assignment.api.model;

import com.tlc.i18n.I18nKey;

import java.util.Collection;

/**
 * Represents the {@link AutoAssignmentTemplate} details.
 *
 * @param id                 Refers the template id.
 * @param name               Refers the name of the template.
 * @param description        Refers the description of the template.
 * @param assignmentModule   Refers the assignment module.
 * @param criteriaSets       Refers the criteria sets.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public record AutoAssignmentTemplate(Long id, I18nKey name, I18nKey description,
                                     AssignmentModule assignmentModule, Collection<AutoAssignmentCriteriaSet> criteriaSets) {
}

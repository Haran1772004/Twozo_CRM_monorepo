package com.tlc.crm.workflow.api.model;

import com.tlc.i18n.I18nKey;

import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;

import java.util.Collection;

/**
 * Represents the {@link WorkflowTemplate} details.
 *
 * @param id            Refers the template id.
 * @param name          Refers the name of the template.
 * @param description   Refers the description of the template.
 * @param triggerSource Refers the {@link TriggerSource}.
 * @param triggerEvent  Refers the {@link TriggerEvent}.
 * @param category      Refers the {@link TemplateCategory}.
 * @param criteriaSets  Refers the criteria sets.
 * @param actions       Refers the template actions.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public record WorkflowTemplate(Long id, I18nKey name, I18nKey description,
                               TriggerSource triggerSource, TriggerEvent triggerEvent,
                               TemplateCategory category, Collection<WorkflowCriteriaSet> criteriaSets,
                               Collection<WorkflowAction> actions) {
}

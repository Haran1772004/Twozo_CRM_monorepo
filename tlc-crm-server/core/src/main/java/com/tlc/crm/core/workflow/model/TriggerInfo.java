package com.tlc.crm.core.workflow.model;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.entity.fields.source.Source;

import java.util.Collection;

/**
 * Refers the information required for trigger the workflow events.
 *
 * @param source            Refers the source.
 * @param action            Refers the action.
 * @param modelSource       Refers the {@link ModelSource} of model.
 * @param entityTriggerIds  Refers the primary ids of the previous entity data.
 * @param needNestedTrigger A flag which indicates whether trigger nested model of an entity or not.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public record TriggerInfo(Source source, Action action, ModelSource modelSource, Collection<Long> entityTriggerIds,
                          boolean needNestedTrigger) {
}

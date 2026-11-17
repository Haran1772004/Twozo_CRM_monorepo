package com.tlc.crm.workflow.api.model;

import java.util.Collection;

/**
 * A model that holds the trigger action related information
 *
 * @param sourceId      Represents the source id
 * @param triggerActionSources It holds the collection of {@link TriggerActionSource}
 * @author KarthickV
 * @version 1.0
 */
public record TriggerAction(Short sourceId, Collection<TriggerActionSource> triggerActionSources) {

}

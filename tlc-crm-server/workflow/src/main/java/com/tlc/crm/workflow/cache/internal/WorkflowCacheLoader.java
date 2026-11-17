package com.tlc.crm.workflow.cache.internal;

import com.tlc.crm.workflow.cache.StepOptionCache;
import com.tlc.crm.workflow.cache.TriggerActionCache;
import com.tlc.crm.workflow.cache.TriggerActionEventCache;
import com.tlc.crm.workflow.cache.TriggerActionFieldCache;
import com.tlc.crm.workflow.cache.TriggerActionSourceCache;
import com.tlc.crm.workflow.cache.WorkflowComparatorCache;
import com.tlc.crm.workflow.cache.WorkflowFieldCache;
import com.tlc.crm.workflow.cache.WorkflowTemplateCache;

/**
 * <p>
 * Acts as gateway to load and the clear all the caches that are related to the workflow
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class WorkflowCacheLoader {

    private final WorkflowComparatorCache workflowComparatorCache;
    private final WorkflowTemplateCache workflowTemplateCache;
    private final WorkflowFieldCache workflowFieldCache;
    private final StepOptionCache stepOptionCache;
    private final TriggerActionCache triggerActionCache;
    private final TriggerActionSourceCache triggerActionSourceCache;
    private final TriggerActionEventCache triggerActionEventCache;
    private final TriggerActionFieldCache triggerActionFieldCache;

    private WorkflowCacheLoader() {
        this.workflowComparatorCache = WorkflowComparatorCache.getInstance();
        this.workflowTemplateCache = WorkflowTemplateCache.getInstance();
        this.workflowFieldCache = WorkflowFieldCache.getInstance();
        this.stepOptionCache = StepOptionCache.getInstance();
        this.triggerActionCache = TriggerActionCache.getInstance();
        this.triggerActionEventCache = TriggerActionEventCache.getInstance();
        this.triggerActionFieldCache = TriggerActionFieldCache.getInstance();
        this.triggerActionSourceCache = TriggerActionSourceCache.getInstance();
    }

    private static class Instance {
        private static final WorkflowCacheLoader INSTANCE = new WorkflowCacheLoader();
    }

    /**
     * Gets the {@link WorkflowCacheLoader} instance
     *
     * @return {@link WorkflowCacheLoader}
     */
    public static WorkflowCacheLoader getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Loads the all the caches related to workflow
     * </p>
     */
    public void load() {
        workflowComparatorCache.load();
        workflowTemplateCache.load();
        workflowFieldCache.load();
        stepOptionCache.load();
        triggerActionCache.load();
        triggerActionFieldCache.load();
        triggerActionSourceCache.load();
        triggerActionEventCache.load();
    }

    /**
     * <p>
     * Clears all the caches related to workflow
     * </p>
     */
    public void clear() {
        workflowComparatorCache.clear();
        workflowTemplateCache.clear();
        workflowFieldCache.clear();
        stepOptionCache.clear();
        triggerActionCache.clear();
        triggerActionFieldCache.clear();
        triggerActionSourceCache.clear();
        triggerActionEventCache.clear();
    }
}

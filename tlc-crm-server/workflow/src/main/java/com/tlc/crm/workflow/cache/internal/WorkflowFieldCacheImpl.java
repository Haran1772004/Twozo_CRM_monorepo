package com.tlc.crm.workflow.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.crm.workflow.api.manager.WorkflowFieldManager;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.cache.WorkflowFieldCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the implementation for {@link WorkflowFieldCache}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class WorkflowFieldCacheImpl implements WorkflowFieldCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(WorkflowFieldCacheImpl.class);

    private final Cache<Long, Map<Long, WorkflowField>> workflowFieldCache;
    private final WorkflowFieldManager workflowFieldManager;

    private WorkflowFieldCacheImpl() {
        this.workflowFieldCache = CacheManager.getInstance().createCache();
        this.workflowFieldManager = WorkflowFieldManager.getInstance();
    }

    private static class Instance {
        private static final WorkflowFieldCacheImpl INSTANCE = new WorkflowFieldCacheImpl();
    }

    /**
     * <p>
     * Retrieves the {@link WorkflowFieldCacheImpl} instance
     * </p>
     *
     * @return {@link WorkflowFieldCacheImpl}
     */
    public static WorkflowFieldCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionSourceId Represents the trigger action source id
     * @return {@link Collection} of {@link WorkflowField}
     */
    @Override
    public Collection<WorkflowField> getWorkflowFieldsBySource(final Long triggerActionSourceId) {
        final Map<Long, WorkflowField> workflowFields = workflowFieldCache.get(triggerActionSourceId);

        return Objects.isNull(workflowFields) ? null : workflowFields.values();
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the workflow field id
     * @return {@link WorkflowField}
     */
    @Override
    public WorkflowField getWorkflowField(final Long id) {
        final List<WorkflowField> workflowFields = new ArrayList<>();

        workflowFieldCache.forEach((triggerActionSourceId, workflowField) -> {

            if (workflowField.containsKey(id)) {
                workflowFields.add(workflowField.get(id));
            }
        });

        return workflowFields.isEmpty() ? null : workflowFields.get(0);
    }

    /**
     * {@inheritDoc}
     *
     * @param workflowFields Represents the {@link Collection} of {@link WorkflowField}
     */
    @Override
    public void setWorkflowFields(final Collection<WorkflowField> workflowFields) {
        workflowFieldCache.putAll(groupWorkflowFields(workflowFields));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Collection<WorkflowField> workflowFields = workflowFieldManager.getAllWorkflowFields();

        workflowFieldCache.putAll(groupWorkflowFields(workflowFields));
        LOGGER.info("Workflow: Total number of workflow fields data loaded into cache : {}", workflowFields.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        workflowFieldCache.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @param workflowField Represents the {@link WorkflowField}
     */
    @Override
    public void setWorkflowField(final WorkflowField workflowField) {
        workflowFieldCache.computeIfAbsent(workflowField.getTriggerActionSource().id(), triggerActionSourceId -> new HashMap<>()).put(workflowField.getId(), workflowField);
    }

    /**
     * <p>
     * Groups the {@link WorkflowField} based on trigger action source id and workflowField id
     * </p>
     *
     * @param workflowFields {@link Collection} of {@link WorkflowField}
     * @return {@link Map} of grouped workflow fields
     */
    private Map<Long, Map<Long, WorkflowField>> groupWorkflowFields(final Collection<WorkflowField> workflowFields) {
        return workflowFields.stream().collect(Collectors.groupingBy(workflowField -> workflowField.getTriggerActionSource().id(),
                Collectors.toMap(WorkflowField::getId, Function.identity())));
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Map} of {@link Collection} of {@link WorkflowField} for the given alias names
     */
    public Map<String, WorkflowField> getWorkflowFields(final Collection<String> aliasNames) {
        final Map<String, WorkflowField> workflowFields = new HashMap<>();

        workflowFieldCache.forEach((triggerActionSourceId, fields) -> {

            for (final WorkflowField workflowField : fields.values()) {
                final String aliasName = workflowField.getAliasName();

                if (aliasNames.contains(aliasName)) {
                    workflowFields.put(aliasName, workflowField);
                }
            }
        });

        return workflowFields;
    }

    /**
     * {@inheritDoc}
     *
     * @param aliasName Represents the alias name
     * @return {@link WorkflowField}
     */
    @Override
    public WorkflowField getWorkflowField(final String aliasName) {
        final List<WorkflowField> workflowFields = new ArrayList<>();

        workflowFieldCache.forEach((triggerActionSourceId, fields) -> {

            for (final WorkflowField workflowField : fields.values()) {

                if (aliasName.contains(workflowField.getAliasName())) {
                    workflowFields.add(workflowField);
                }
            }
        });
        return workflowFields.isEmpty() ? null : workflowFields.get(0);
    }
}

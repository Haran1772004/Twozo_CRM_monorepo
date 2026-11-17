package com.tlc.crm.workflow.action.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;

import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;
import com.tlc.crm.workflow.api.model.WorkflowComparator;
import com.tlc.crm.workflow.api.model.WorkflowComparator.Category;
import com.tlc.crm.workflow.cache.service.WorkflowComparatorService;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.i18n.I18nResolver;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Manages {@link WorkflowComparator} related dropdown actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@StaticDropdown(name = "comparators")
public class WorkflowComparatorList extends AbstractDropdownAction {

    private final WorkflowComparatorService workflowComparatorService;

    public WorkflowComparatorList() {
        this.workflowComparatorService = WorkflowComparatorService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return The {@link OptionContainer} that holds the comparator details.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final OptionContainer optionContainer = new OptionContainer();
        final JsonObject data = dropdownRequest.data();
        final Long fieldTypeId = data.getLong(WorkflowAttributes.FIELD_TYPE_ID);
        final Collection<WorkflowComparator> workflowComparators = workflowComparatorService.getComparatorsByFieldTypeId(fieldTypeId);

        if (!workflowComparators.isEmpty()) {
            final Function<Category, Collection<WorkflowComparator>> comparatorsRetriever = category ->
                    workflowComparators.stream().filter(comparator -> {
                        final Category comparatorCategory = comparator.getCategory();

                        return Objects.equals(Category.ALL, comparatorCategory) || Objects.equals(category, comparatorCategory);
                    }).sorted(Comparator.comparing(WorkflowComparator::getId)).toList();

            final TriggerEvent triggerEvent = TriggerEvent.get(data.getShort(WorkflowAttributes.TRIGGER_EVENT));
            final Collection<WorkflowComparator> comparators = switch (triggerEvent) {
                case CREATED -> comparatorsRetriever.apply(Category.CREATE);
                case EDITED -> comparatorsRetriever.apply(Category.UPDATE);
                case DELETED -> comparatorsRetriever.apply(Category.DELETE);
            };
            final I18nResolver i18nResolver = dropdownRequest.i18nResolver();
            final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
            final Collection<WorkflowComparator> workFlowComparators = applyCriteria(comparators, dropdownRequest.getCriteria(),
                    List.of(workflowComparator -> i18nResolver.get(workflowComparator.getName())), Optional.empty());
            final int start = dropdownLimit.start();
            final long totalRecords = workFlowComparators.size();

            optionContainer.setTotalRecords(totalRecords);

            if (totalRecords > 0 && start <= totalRecords) {
                final Collection<WorkflowComparator> workflowComparatorList = applyLimit(workFlowComparators, start, dropdownLimit.limit());

                for (final WorkflowComparator workflowComparator : workflowComparatorList) {
                    final JsonObject jsonObject = Json.object();

                    jsonObject.put(WorkflowAttributes.ID, workflowComparator.getId());
                    jsonObject.put(WorkflowAttributes.NAME, dropdownRequest.i18nResolver().get(workflowComparator.getName()));
                    jsonObject.put(WorkflowAttributes.HAS_VALUE, workflowComparator.hasValue());

                    optionContainer.addOption(jsonObject);
                }
            }
        }

        return optionContainer;
    }
}

package com.tlc.crm.core.action.dropdown.contact;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.field.contact.LifeCycleStageFields;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.crm.entity.fields.option.OptionKeys;

import java.util.Collection;

/**
 * Manages {@link LifeCycleStage} related dropdown actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Dropdown(name = "lifeCycleStageList", fieldAlias = "lifeCycleStage")
public class LifeCycleStageList extends AbstractDropdownAction {

    private final LifeCycleStageManager lifeCycleStageManager;

    public LifeCycleStageList() {
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final Long orgId = dropdownRequest.getOrgId();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final long totalRecords = lifeCycleStageManager.getStagesCount(orgId, dropdownCriteria);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<LifeCycleStage> lifeCycleStages = lifeCycleStageManager.getLifeCycleStageDropdownList(orgId, false,
                    dropdownLimit, dropdownCriteria);

            final String valueKey = LifeCycleStageFields.VALUE.getName();
            final String nameKey = LifeCycleStageFields.NAME.getName();
            final String defaultKey = LifeCycleStageFields.IS_DEFAULT.getName();
            boolean isDefaultStatusLoaded = false;

            for (final LifeCycleStage lifeCycleStage : lifeCycleStages) {

                if (isDefaultStatusLoaded) {
                    optionContainer.addOption(lifeCycleStage.id(), lifeCycleStage.getName());
                } else {
                    final JsonObject lifeCycleStageJson = Json.object();

                    lifeCycleStageJson.put(defaultKey, true);
                    lifeCycleStageJson.put(valueKey, lifeCycleStage.id());
                    lifeCycleStageJson.put(nameKey, lifeCycleStage.getName());
                    optionContainer.addOption(lifeCycleStageJson);
                    isDefaultStatusLoaded = true;
                }
            }
        }
        optionContainer.setTotalRecords(lifeCycleStageManager.getTotalRecordsCount(orgId, dropdownCriteria));
        return optionContainer;
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest The request object containing parameters for filtering or context.
     * @return An {@link OptionContainer} with the relevant dropdown options.
     */
    @Override
    public OptionContainer fetchZapierData(final DropdownRequest dropdownRequest) {
        return getOptionContainer(dropdownRequest);
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest The request object containing parameters for filtering or context.
     * @return An {@link OptionContainer} with the relevant dropdown options.
     */
    @Override
    public OptionContainer fetchRestAPIData(final DropdownRequest dropdownRequest) {
        return getOptionContainer(dropdownRequest);
    }

    /**
     * Retrieves an {@link OptionContainer} containing lifecycle stage options based on the provided {@link DropdownRequest}.
     *
     * @param dropdownRequest The request object containing parameters for filtering or context.
     * @return An {@link OptionContainer} with the relevant dropdown options.
     */
    private OptionContainer getOptionContainer(final DropdownRequest dropdownRequest) {
        final OptionContainer optionContainer = new OptionContainer(OptionKeys.ID, OptionKeys.NAME);
        final Long orgId = dropdownRequest.getOrgId();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final long totalRecords = lifeCycleStageManager.getStagesCount(orgId, dropdownCriteria);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<LifeCycleStage> lifeCycleStages = lifeCycleStageManager.getLifeCycleStageDropdownList(orgId, false,
                    dropdownLimit, dropdownCriteria);

            for (final LifeCycleStage lifecycleStage : lifeCycleStages) {
                optionContainer.addOption(lifecycleStage.id(), lifecycleStage.getName());
            }
        }

        return optionContainer;
    }
}

package com.tlc.crm.core.action.dropdown.contact;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.fields.option.OptionKeys;

import java.util.Collection;

/**
 * <p>
 * Manages contact {@link LifeCycleStage} and {@link LifeCycleStatus} related dropdown actions.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@StaticDropdown(name = "lifecycleStageStatusList")
public class LifecycleStageStatusList extends AbstractDropdownAction {

    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final LifeCycleStageManager lifeCycleStageManager;

    public LifecycleStageStatusList() {
        lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return The {@link OptionContainer}
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final Long orgId = dropdownRequest.getOrgId();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final long totalRecords = lifeCycleStatusManager.getTotalRecordsCount(orgId, null, dropdownCriteria);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<LifeCycleStage> lifeCycleStages = lifeCycleStageManager.getLifeCycleStageDropdownList(orgId, true,
                    dropdownLimit, dropdownCriteria);

            for (final LifeCycleStage lifeCycleStage : lifeCycleStages) {
                final JsonObject option = Json.object();
                final JsonArray lifeCycleStatusArray = Json.array();
                final Collection<LifeCycleStatus> statuses = lifeCycleStage.getStatuses();

                option.put(OptionKeys.NAME, lifeCycleStage.getName());

                for (final LifeCycleStatus lifeCycleStatus : statuses) {
                    final JsonObject lifeCycleStatusJson = Json.object();

                    lifeCycleStatusJson.put(OptionKeys.NAME, lifeCycleStatus.getName());
                    lifeCycleStatusJson.put(OptionKeys.VALUE, lifeCycleStatus.id());

                    lifeCycleStatusArray.put(lifeCycleStatusJson);
                }
                option.put(OptionKeys.OPTIONS, lifeCycleStatusArray);
                optionContainer.addOption(option);
            }
        }
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
        final OptionContainer optionContainer = new OptionContainer();
        final Long orgId = dropdownRequest.getOrgId();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final long totalRecords = lifeCycleStatusManager.getTotalRecordsCount(orgId, null, dropdownCriteria);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<LifeCycleStage> lifeCycleStages = lifeCycleStageManager.getLifeCycleStageDropdownList(orgId, true,
                    dropdownLimit, dropdownCriteria);

            for (final LifeCycleStage lifecycleStage : lifeCycleStages) {
                final String lifecycleStageName = lifecycleStage.getName();

                for (final LifeCycleStatus lifeCycleStatus : lifecycleStage.getStatuses()) {
                    final JsonObject jsonObject = Json.object();

                    jsonObject.put(OptionKeys.ID, lifeCycleStatus.id());
                    jsonObject.put(OptionKeys.NAME, String.join(" - ", lifeCycleStatus.getName(), lifecycleStageName));

                    optionContainer.addOption(jsonObject);
                }
            }
        }

        return optionContainer;
    }
}

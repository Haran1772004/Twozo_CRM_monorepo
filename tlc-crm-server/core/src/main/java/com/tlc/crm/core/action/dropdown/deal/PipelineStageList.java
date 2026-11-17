package com.tlc.crm.core.action.dropdown.deal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.option.OptionKeys;

import java.util.Collection;

/**
 * Manages {@link PipelineStage} related dropdown actions.
 *
 * @author karthick.V
 * @version 1.0
 */
@Dropdown(name = "pipelineStage", fieldAlias = "pipelineStage")
public class PipelineStageList extends AbstractDropdownAction {

    private final PipelineStageManager pipelineStageManager;

    public PipelineStageList() {
        pipelineStageManager = PipelineStageManager.getInstance();
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
        final JsonObject data = dropdownRequest.data();
        final String dependentIdKey = FieldKeys.DEPENDENT_ID.getName();
        final Long dependentId = data.containsKey(dependentIdKey) ? data.getLong(dependentIdKey) : null;
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final long totalRecords = pipelineStageManager.getTotalCount(orgId, dependentId, dropdownCriteria);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<PipelineStage> pipelineStages = pipelineStageManager.getDropdownList(orgId,
                    dependentId, dropdownLimit, dropdownCriteria);
            final boolean isRestApiClient = CrmRequest.ClientType.REST_API == dropdownRequest.getClientType();

            for (PipelineStage pipelineStage : pipelineStages) {
                final JsonObject option = Json.object();

                if (isRestApiClient) {
                    option.put(OptionKeys.ID, pipelineStage.id());
                } else {
                    option.put(OptionKeys.VALUE, pipelineStage.id());
                    option.put(OptionKeys.ORDER, pipelineStage.getOrder());
                    option.put(OptionKeys.IS_EDITABLE_OPTION, true);
                    option.put(OptionKeys.IS_DEFAULT, pipelineStage.isDefault());
                }
                option.put(OptionKeys.NAME, pipelineStage.getName());
                optionContainer.addOption(option);
            }
        }

        return optionContainer;
    }
}

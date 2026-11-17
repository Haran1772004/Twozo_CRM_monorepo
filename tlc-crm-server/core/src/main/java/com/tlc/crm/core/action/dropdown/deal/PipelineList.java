package com.tlc.crm.core.action.dropdown.deal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.crm.entity.fields.option.OptionKeys;

import java.util.Collection;

/**
 * Manages {@link Pipeline} related dropdown actions.
 *
 * @author karthick.V
 * @version 1.0
 */
@Dropdown(name = "pipeline", fieldAlias = "pipeline")
public class PipelineList extends AbstractDropdownAction {

    private final PipelineManager pipelineManager;

    public PipelineList() {
        pipelineManager = PipelineManager.getInstance();
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
        final long totalRecords = pipelineManager.getTotalCount(orgId, dropdownCriteria);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final CrmRequest.ClientType clientType = dropdownRequest.getClientType();
            final Collection<Pipeline> pipelines = pipelineManager.getPipelineList(orgId,
                    dropdownRequest.getUserId(), dropdownLimit, dropdownCriteria, false);

            if (CrmRequest.ClientType.ZAPIER == clientType || CrmRequest.ClientType.REST_API == clientType) {
                pipelines.forEach(pipeline -> {
                    final JsonObject pipelineJson = Json.object();

                    pipelineJson.put(OptionKeys.ID, pipeline.id());
                    pipelineJson.put(OptionKeys.NAME, pipeline.getName());

                    optionContainer.addOption(pipelineJson);
                });
            } else {
                for (final Pipeline pipeline : pipelines) {
                    final JsonObject option = Json.object();

                    option.put(OptionKeys.NAME, pipeline.getName());
                    option.put(OptionKeys.VALUE, pipeline.id());
                    option.put(OptionKeys.ORDER, pipeline.getOrder());
                    option.put(OptionKeys.IS_EDITABLE_OPTION, true);
                    option.put(OptionKeys.IS_DEFAULT, pipeline.isSelected());
                    optionContainer.addOption(option);
                }
            }
        }

        return optionContainer;
    }
}

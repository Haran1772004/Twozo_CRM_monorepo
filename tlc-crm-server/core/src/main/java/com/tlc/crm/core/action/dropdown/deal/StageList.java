package com.tlc.crm.core.action.dropdown.deal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.fields.option.OptionKeys;

import java.util.Collection;

/**
 * Represents a static dropdown for pipeline stage list, which includes the pipeline details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@StaticDropdown(name = "stageList")
public final class StageList extends AbstractDropdownAction {

    private final PipelineManager pipelineManager;
    private final PipelineStageManager pipelineStageManager;

    public StageList() {
        this.pipelineManager = PipelineManager.getInstance();
        this.pipelineStageManager = PipelineStageManager.getInstance();
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
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final long totalRecords = pipelineStageManager.getTotalCount(orgId, null, dropdownCriteria);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > NumericalConstant.ZERO && dropdownLimit.start() <= totalRecords) {
            final Collection<Pipeline> pipelines = pipelineManager.getPipelineForStageList(orgId, dropdownLimit, dropdownCriteria);

            for (final Pipeline pipeline : pipelines) {
                final JsonObject option = Json.object();
                final JsonArray pipelineStages = Json.array();

                option.put(OptionKeys.NAME, pipeline.getName());

                for (final PipelineStage pipelineStage : pipeline.getPipelineStages()) {
                    final JsonObject pipelineStageJson = Json.object();

                    pipelineStageJson.put(OptionKeys.NAME, pipelineStage.getName());
                    pipelineStageJson.put(OptionKeys.VALUE, pipelineStage.id());
                    pipelineStages.put(pipelineStageJson);
                }
                option.put(OptionKeys.OPTIONS, pipelineStages);
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
        final DropdownCriteria criteria = dropdownRequest.getCriteria();
        final long totalRecords = pipelineStageManager.getTotalCount(orgId, null, criteria);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<Pipeline> pipelines = pipelineManager.getPipelineForStageList(orgId, dropdownLimit, criteria);

            for (final Pipeline pipeline : pipelines) {
                final String pipelineName = pipeline.getName();

                for (final PipelineStage pipelineStage : pipeline.getPipelineStages()) {
                    final JsonObject pipelineStageJson = Json.object();

                    pipelineStageJson.put(OptionKeys.NAME, String.join(" - ", pipelineStage.getName(), pipelineName));
                    pipelineStageJson.put(OptionKeys.ID, pipelineStage.id());

                    optionContainer.addOption(pipelineStageJson);
                }
            }
        }

        return optionContainer;
    }

}
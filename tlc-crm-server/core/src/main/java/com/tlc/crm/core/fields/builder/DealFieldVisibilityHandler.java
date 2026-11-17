package com.tlc.crm.core.fields.builder;

import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.dropdown.deal.State;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.internal.FieldVisibilityCriteria;
import com.tlc.crm.entity.models.internal.FieldVisibilityCriteriaGroup;
import com.tlc.crm.entity.models.internal.FieldVisibilityCriteriaGroup.Connector;
import com.tlc.crm.entity.service.FieldService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Handles the {@link FieldVisibilityCriteriaGroup} related to {@link Deal} based on the {@link FieldInfo}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class DealFieldVisibilityHandler implements FieldVisibilityHandler {

    private final PipelineManager pipelineManager;
    private final FieldService fieldService;

    private static class Instance {
        private static final DealFieldVisibilityHandler INSTANCE = new DealFieldVisibilityHandler();
    }

    private DealFieldVisibilityHandler() {
        pipelineManager = PipelineManager.getInstance();
        fieldService = FieldService.getSecureInstance();
    }

    /**
     * <p>
     * Gets the {@link DealFieldVisibilityHandler} Instance
     * </p>
     *
     * @return {@link DealFieldVisibilityHandler}
     */
    public static DealFieldVisibilityHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents an organization id
     * @param fieldInfo Represents the {@link FieldInfo}
     */
    @Override
    public Optional<FieldVisibilityCriteriaGroup> getCriteriaGroup(final long orgId, final FieldInfo fieldInfo) {
        return switch (fieldInfo.aliasName()) {
            case "wonReason" -> Optional.of(getCriteriaGroup(orgId, State.WON.getName()));
            case "lostReason" -> Optional.of(getCriteriaGroup(orgId, State.LOST.getName()));
            case "dealClosedOn" -> Optional.of(getCriteriaGroup(orgId, State.WON.getName(), State.LOST.getName()));
            case "expectedCloseDate" -> fieldService.getDefaultFieldByAliasName(Source.DEAL,
                    DealFields.FORECAST_CATEGORY).map(forecastCategoryField -> {
                final FieldVisibilityCriteriaGroup fieldVisibilityCriteriaGroup = new FieldVisibilityCriteriaGroup();
                final FieldVisibilityCriteriaGroup childFieldVisibilityCriteriaGroup = new FieldVisibilityCriteriaGroup();
                final FieldVisibilityCriteria fieldVisibilityCriteria = new FieldVisibilityCriteria();

                fieldVisibilityCriteria.setCondition(FieldVisibilityCriteria.Condition.NOT_NULL);
                fieldVisibilityCriteria.setFieldId(forecastCategoryField.id());
                fieldVisibilityCriteria.setValidationMessage("i18n.deal.field.expected.close.date.validation.messages.placeholder");
                childFieldVisibilityCriteriaGroup.setFieldVisibilityCriteria(List.of(fieldVisibilityCriteria));

                fieldVisibilityCriteriaGroup.setChildFieldVisibilityCriteriaGroups(List.of(childFieldVisibilityCriteriaGroup));

                return fieldVisibilityCriteriaGroup;
            });
            default -> Optional.empty();
        };
    }

    /**
     * <p>
     * Gets the {@link FieldVisibilityCriteriaGroup} based on the given stages
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param stages Represents the name of the pipeline stage
     * @return {@link FieldVisibilityCriteriaGroup}
     */
    private FieldVisibilityCriteriaGroup getCriteriaGroup(final long orgId, final String... stages) {
        final Map<Long, Collection<PipelineStage>> pipelines = pipelineManager.getPipelines(orgId, List.of(stages));
        final FieldVisibilityCriteriaGroup fieldVisibilityCriteriaGroup = new FieldVisibilityCriteriaGroup();
        final Collection<FieldVisibilityCriteriaGroup> fieldVisibilityCriteriaGroups = new ArrayList<>();
        final String pipelineFieldKey = DealFields.PIPELINE;
        final String pipelineStageFieldKey = DealFields.PIPELINE_STAGE;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, List.of(pipelineFieldKey, pipelineStageFieldKey));
        final Long pipelineFieldId = fields.get(pipelineFieldKey).id();
        final Long pipelineStageFieldId = fields.get(pipelineStageFieldKey).id();
        final Connector orConnector = Connector.OR;
        final Connector andConnector = Connector.AND;

        for (final Map.Entry<Long, Collection<PipelineStage>> pipeline : pipelines.entrySet()) {
            final FieldVisibilityCriteriaGroup childFieldVisibilityCriteriaGroup = new FieldVisibilityCriteriaGroup();
            final Collection<FieldVisibilityCriteriaGroup> childFieldVisibilityCriteriaGroups = new ArrayList<>();

            childFieldVisibilityCriteriaGroups.add(buildCriteriaGroup(pipelineFieldId, pipeline.getKey()));
            final Collection<FieldVisibilityCriteriaGroup> stagesCriteriaGroups = pipeline.getValue().stream().map(pipelineStage -> buildCriteriaGroup(pipelineStageFieldId, pipelineStage.id())).collect(Collectors.toList());

            if (stages.length > 1) {
                final FieldVisibilityCriteriaGroup stagesCriteriaGroup = new FieldVisibilityCriteriaGroup();

                stagesCriteriaGroup.setConnector(orConnector);
                stagesCriteriaGroup.setChildFieldVisibilityCriteriaGroups(stagesCriteriaGroups);
                childFieldVisibilityCriteriaGroups.add(stagesCriteriaGroup);
            } else {
                childFieldVisibilityCriteriaGroups.addAll(stagesCriteriaGroups);
            }

            childFieldVisibilityCriteriaGroup.setChildFieldVisibilityCriteriaGroups(childFieldVisibilityCriteriaGroups);
            childFieldVisibilityCriteriaGroup.setConnector(andConnector);
            fieldVisibilityCriteriaGroups.add(childFieldVisibilityCriteriaGroup);
        }

        if (pipelines.size() > 1) {
            fieldVisibilityCriteriaGroup.setConnector(orConnector);
        }

        fieldVisibilityCriteriaGroup.setChildFieldVisibilityCriteriaGroups(fieldVisibilityCriteriaGroups);

        return fieldVisibilityCriteriaGroup;
    }
}


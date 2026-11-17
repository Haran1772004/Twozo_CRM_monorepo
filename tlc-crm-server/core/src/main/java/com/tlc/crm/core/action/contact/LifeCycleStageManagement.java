package com.tlc.crm.core.action.contact;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.LifeCycleRuleDetails;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.api.contact.model.converter.LifeCycleStageConverter;
import com.tlc.crm.core.field.contact.LifeCycleStageFields;
import com.tlc.crm.core.internal.contact.handler.LifeCycleMigrationHandler;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.web.WebAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link LifeCycleStage} related web actions
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 * @see CrmConfigAction
 */
@WebAction(path = "/lifecycle/stage/mgmt")
public class LifeCycleStageManagement extends CrmConfigAction<LifeCycleStage> {

    private static final byte INCREMENT_COUNT = 1;
    private static final byte MAX_STAGE_LIMIT = 10;

    private final ConverterConfigBuilder converterConfigBuilder;
    private final LifeCycleStageConverter lifeCycleStageConverter;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final LifeCycleMigrationHandler lifeCycleMigrationHandler;

    public LifeCycleStageManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        lifeCycleStageConverter = LifeCycleStageConverter.getInstance();
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
        lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        lifeCycleMigrationHandler = LifeCycleMigrationHandler.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString("type");
        final JsonObject data = requestJson.getJsonObject("data");
        final Long orgId = request.orgId();

        switch (type) {
            case "updateOrder" -> updateOrder(orgId, data, response);
            case "updateStatus" -> updateStatus(orgId, data, response);
            case "updateRules" -> updateRules(orgId, data, response);
            case "getRules" -> response.put(LifeCycleStageFields.RULES.getName(), getStageRules(orgId));
            case "getAllStages" -> getAllStages(orgId, response);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigManager<LifeCycleStage> getConfigManager() {
        return lifeCycleStageManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LifeCycleStage convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return lifeCycleStageConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject convert(final LifeCycleStage lifeCycleStage) {
        return lifeCycleStageConverter.convert(lifeCycleStage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LifeCycleStage convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return lifeCycleStageConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization id.
     * @param userId Represents the user id.
     * @param action Perform action like create, update or delete.
     * @param stages Collection of lifecycle stages.
     * @return True, if the details are valid, otherwise return false.
     */
    @Override
    protected boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<LifeCycleStage> stages) {
        return switch (action) {
            case CREATE -> {
                if (MAX_STAGE_LIMIT < lifeCycleStageManager.getStagesCount(orgId) + stages.size()) {
                    throw ErrorCode.get(ContactErrorCodes.LIFE_CYCLE_STAGE_COUNT_EXCEEDED, "i18n.lifecycle.stage.count.exceeded");
                }
                yield validate(orgId, stages);
            }
            case UPDATE, PARTIAL_UPDATE -> validate(orgId, stages);
            case DELETE -> {
                final Set<Long> stageIds = stages.stream().map(LifeCycleStage::id).collect(Collectors.toSet());

                yield !lifeCycleStageManager.isSystemStages(orgId, stageIds);
            }
            default -> true;
        };
    }

    /**
     * <p>
     * Validates the details of the lifecycle stages.
     * </p>
     *
     * @param orgId  Represents the organisation id.
     * @param stages Holds the details of the stages.
     * @return A flag that indicates whether the given details is valid or not.
     */
    private boolean validate(final Long orgId, final Collection<LifeCycleStage> stages) {
        final Collection<String> stageNames = new HashSet<>();
        final Collection<String> statusNames = new HashSet<>();
        final Collection<LifeCycleStatus> lifeCycleStatuses = new ArrayList<>();

        for (final LifeCycleStage stage : stages) {
            final Collection<LifeCycleStatus> statuses = stage.getStatuses();

            if (Objects.nonNull(statuses)) {
                for (final LifeCycleStatus status : statuses) {
                    statusNames.add(status.getName());
                    lifeCycleStatuses.add(status);
                }
            }
            stageNames.add(stage.getName());
        }

        if (stageNames.size() != stages.size() || lifeCycleStageManager.exists(orgId, stages)) {
            throw ErrorCode.get(ContactErrorCodes.DUPLICATE_LIFE_CYCLE_STAGE, "i18n.lifecycle.duplicate.stage.found");
        }

        if (statusNames.size() != lifeCycleStatuses.size() || lifeCycleStatusManager.exists(orgId, lifeCycleStatuses)) {
            throw ErrorCode.get(ContactErrorCodes.DUPLICATE_LIFE_CYCLE_STATUS, "i18n.lifecycle.duplicate.status.found");
        }
        return true;
    }

    /**
     * <p>
     * Fetches all the {@link LifeCycleStage} of an organisation.
     * </p>
     *
     * @param orgId    Represents an organisation id.
     * @param response Holds the response data.
     */
    private void getAllStages(final Long orgId, final CrmResponse response) {
        final Collection<LifeCycleStage> lifeCycleStages = lifeCycleStageManager.getAll(orgId);
        final JsonArray stagesJson = Json.array();
        final JsonArray disabledStagesJson = Json.array();

        for (final LifeCycleStage lifeCycleStage : lifeCycleStages) {
            final JsonObject stageJson = convert(lifeCycleStage);

            if (lifeCycleStage.isActive()) {
                stagesJson.put(stageJson);
            } else {
                disabledStagesJson.put(stageJson);
            }
        }
        disabledStagesJson.forEach(stagesJson::put);
        response.put(LifeCycleStageFields.STAGES.getName(), stagesJson);
    }

    /**
     * <p>
     * Fetches the {@link LifeCycleStage} rules of an organisation.
     * </p>
     *
     * @param orgId Represents an organisation id.
     * @return Lifecycle stage rules as {@link JsonArray}
     */
    private JsonArray getStageRules(final Long orgId) {
        final Collection<LifeCycleRuleDetails> rules = lifeCycleStageManager.getRules(orgId);
        final JsonArray rulesJson = Json.array();

        for (final LifeCycleRuleDetails rule : rules) {
            final JsonObject ruleJson = Json.object();

            ruleJson.put(LifeCycleStageFields.TYPE.getName(), rule.getType().getId());
            ruleJson.put(LifeCycleStageFields.STAGE_ID.getName(), rule.getStage().id());
            rulesJson.put(ruleJson);
        }
        return rulesJson;
    }

    /**
     * <p>
     * Updates the {@link LifeCycleStage} rules of an organisation.
     * </p>
     *
     * @param orgId    Represents an organisation id.
     * @param data     Holds the request data.
     * @param response Holds the response data.
     */
    private void updateRules(final Long orgId, final JsonObject data, final CrmResponse response) {
        final JsonArray rulesJson = data.getJsonArray(LifeCycleStageFields.RULES.getName());
        final Collection<Pair<Long, LifeCycleRuleDetails>> lifeCycleStageRules = new HashSet<>(2);
        final int size = rulesJson.size();

        for (int index = 0; index < size; index++) {
            final JsonObject ruleJson = rulesJson.getJsonObject(index);
            final LifeCycleRuleDetails.LifeCycleRuleType rule = LifeCycleRuleDetails.LifeCycleRuleType.get(ruleJson.getShort(LifeCycleStageFields.TYPE.getName()));
            final LifeCycleRuleDetails lifeCycleRuleDetails = new LifeCycleRuleDetails(rule);
            final Long lifeCycleStageId = ruleJson.getLong(LifeCycleStageFields.STAGE_ID.getName());
            final LifeCycleStage lifeCycleStage = new LifeCycleStage();

            lifeCycleStage.setId(lifeCycleStageId);
            lifeCycleRuleDetails.setOrgId(orgId);
            lifeCycleRuleDetails.setStage(lifeCycleStage);

            lifeCycleStageRules.add(Pair.of(lifeCycleStageId, lifeCycleRuleDetails));
        }

        if (!lifeCycleStageRules.isEmpty()) {
            final Map<Long, Long> defaultStatusIds = lifeCycleStatusManager.getDefaultStatusIdsByStage(orgId, lifeCycleStageRules.stream().map(Pair::getKey).toList());

            for (final Pair<Long, LifeCycleRuleDetails> lifeCycleStageRule : lifeCycleStageRules) {
                final Long stageId = lifeCycleStageRule.getKey();
                final Long defaultStatusId = defaultStatusIds.get(stageId);

                if (Objects.isNull(defaultStatusId)) {
                    throw ErrorCode.get(ContactErrorCodes.DEFAULT_LIFE_CYCLE_STATUS_NOT_FOUND,
                            "i18n.contact.lifecycle.defaultStatus.not.found");
                }
                final LifeCycleStatus lifeCycleStatus = new LifeCycleStatus();

                lifeCycleStatus.setId(defaultStatusId);
                lifeCycleStageRule.getValue().setStatus(lifeCycleStatus);
            }
        }

        lifeCycleStageManager.updateRules(orgId, lifeCycleStageRules.stream().map(Pair::getValue).toList());
        response.put(FieldKeys.MESSAGE.getName(), response.resolveI18nString("i18n.lifecycle.rule.update.success"));
    }

    /**
     * <p>
     * Updates the active status of the {@link LifeCycleStage} of an organisation.
     * </p>
     *
     * @param orgId    Represents an organisation id.
     * @param data     Holds the request data.
     * @param response Holds the response data.
     */
    private void updateStatus(final Long orgId, final JsonObject data, final CrmResponse response) {
        final long stageId = data.getLong(LifeCycleStageFields.ID.getName());
        final boolean isActive = data.getBoolean(LifeCycleStageFields.IS_ACTIVE.getName());

        if (lifeCycleStageManager.isDeactivatableStage(orgId, stageId)) {
            if (!isActive) {
                lifeCycleMigrationHandler.handleStageRemoval(orgId, Set.of(stageId));
            }
            lifeCycleStageManager.updateStatus(orgId, stageId, isActive);
            response.put(FieldKeys.MESSAGE.getName(), response.resolveI18nString("i18n.lifecycle.stage.activeStatus.update.success"));
        } else {
            response.put(FieldKeys.MESSAGE.getName(), response.resolveI18nString("i18n.lifecycle.stage.activeStatus.update.restricted"));
        }
    }

    /**
     * <p>
     * Updates the order of the {@link LifeCycleStage} of an organisation.
     * </p>
     *
     * @param orgId    Represents an organisation id.
     * @param data     Holds the request data.
     * @param response Holds the response data.
     */
    private void updateOrder(final Long orgId, final JsonObject data, final CrmResponse response) {
        final JsonArray jsonArray = data.getJsonArray(LifeCycleStageFields.IDS.getName());
        final TreeMap<Short, Long> stageOrders = new TreeMap<>(Comparator.naturalOrder());
        final int size = jsonArray.size();

        for (short index = 0; index < size; index++) {
            stageOrders.put((short) (index + INCREMENT_COUNT), jsonArray.getLong(index));
        }

        if (lifeCycleStageManager.exists(orgId, Set.copyOf(stageOrders.values()))) {
            final Map.Entry<Short, Long> lastStage = stageOrders.lastEntry();

            if (lifeCycleStageManager.isLastStage(orgId, lastStage.getValue())) {
                stageOrders.remove(lastStage.getKey());
                lifeCycleStageManager.updateOrder(orgId, stageOrders);
                response.put(FieldKeys.MESSAGE.getName(), response.resolveI18nString("i18n.lifecycle.stage.order.update.success"));
            } else {
                response.put(FieldKeys.MESSAGE.getName(), response.resolveI18nString("i18n.lifecycle.stage.order.change.restricted"));
            }
        } else {
            throw ErrorCode.get(ContactErrorCodes.LIFE_CYCLE_STAGE_NOT_FOUND, response.resolveI18nString("i18n.lifecycle.stage.not.found"));
        }
    }
}

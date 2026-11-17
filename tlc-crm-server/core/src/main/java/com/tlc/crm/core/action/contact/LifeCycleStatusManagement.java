package com.tlc.crm.core.action.contact;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.api.contact.model.converter.LifeCycleStatusConverter;
import com.tlc.crm.core.field.contact.LifeCycleStatusFields;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.fields.FieldKeys;

import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link LifeCycleStatus} related web actions.
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 * @see CrmConfigAction
 */
@WebAction(path = "/lifecycle/status/mgmt")
public class LifeCycleStatusManagement extends CrmConfigAction<LifeCycleStatus> {

    private static final byte INCREMENT_COUNT = 1;
    private static final byte MAX_STATUS_LIMIT = 10;

    private final ConverterConfigBuilder converterConfigBuilder;
    private final LifeCycleStatusConverter lifeCycleStatusConverter;
    private final LifeCycleStatusManager lifeCycleStatusManager;

    public LifeCycleStatusManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        lifeCycleStatusConverter = LifeCycleStatusConverter.getInstance();
        lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
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
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigManager<LifeCycleStatus> getConfigManager() {
        return lifeCycleStatusManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LifeCycleStatus convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return lifeCycleStatusConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject convert(final LifeCycleStatus lifeCycleStatus) {
        return lifeCycleStatusConverter.convert(lifeCycleStatus);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LifeCycleStatus convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return lifeCycleStatusConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization id.
     * @param userId   Represents the user id.
     * @param action   Perform action like create, update or delete.
     * @param statuses Collection of lifecycle statuses.
     * @return True, if the details are valid, otherwise return false.
     */
    @Override
    protected boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<LifeCycleStatus> statuses) {
        return switch (action) {
            case CREATE -> {
                final Optional<Long> stageId = statuses.stream().map(LifeCycleStatus::getStageId).filter(Objects::nonNull).findAny();

                if (stageId.isEmpty()) {
                    throw ErrorCode.get(ContactErrorCodes.LIFE_CYCLE_STAGE_NOT_FOUND, "i18n.lifecycle.stage.not.found");
                }

                if (MAX_STATUS_LIMIT < lifeCycleStatusManager.getStatusesCount(orgId, stageId.get()) + statuses.size()) {
                    throw ErrorCode.get(ContactErrorCodes.LIFE_CYCLE_STATUS_COUNT_EXCEEDED, "i18n.lifecycle.status.count.exceeded");
                }
                yield validate(orgId, statuses);
            }
            case UPDATE, PARTIAL_UPDATE -> validate(orgId, statuses);
            case DELETE -> {
                final Set<Long> statusIds = statuses.stream().map(LifeCycleStatus::id).collect(Collectors.toSet());

                if (lifeCycleStatusManager.isDeletable(orgId, statusIds)) {
                    yield true;
                }
               throw ErrorCode.get(ContactErrorCodes.DELETE_ACTION_FAILED, "i18n.lifecycle.status.delete.restricted");
            }
            default -> true;
        };
    }

    /**
     * <p>
     * Validates the details of the lifecycle statuses.
     * </p>
     *
     * @param orgId    Represents the organisation id.
     * @param statuses Holds the details of the statuses.
     * @return A flag that indicates whether the given details are valid or not.
     */
    private boolean validate(final Long orgId, final Collection<LifeCycleStatus> statuses) {
        final Collection<String> statusNames = new HashSet<>();
        Long stageId = null;

        for (final LifeCycleStatus status : statuses) {
            if (Objects.isNull(stageId)) {
                stageId = status.getStageId();
            }
            statusNames.add(status.getName());
        }

        if (statusNames.size() != statuses.size() || lifeCycleStatusManager.exists(orgId, statuses)) {
            throw ErrorCode.get(ContactErrorCodes.DUPLICATE_LIFE_CYCLE_STATUS, "i18n.lifecycle.duplicate.status.found");
        }
        return true;
    }

    /**
     * <p>
     * Updates the order of the {@link LifeCycleStatus} of an organisation.
     * </p>
     *
     * @param orgId    Represents an organisation id.
     * @param data     Holds the request data.
     * @param response Holds the response data.
     */
    private void updateOrder(final Long orgId, final JsonObject data, final CrmResponse response) {
        final JsonArray jsonArray = data.getJsonArray(LifeCycleStatusFields.IDS.getName());
        final TreeMap<Short, Long> statusOrders = new TreeMap<>(Comparator.naturalOrder());
        final int size = jsonArray.size();

        for (short index = 0; index < size; index++) {
            statusOrders.put((short) (index + INCREMENT_COUNT), jsonArray.getLong(index));
        }

        if (!lifeCycleStatusManager.exists(orgId, Set.copyOf(statusOrders.values()))) {
            throw ErrorCode.get(ContactErrorCodes.LIFE_CYCLE_STATUS_NOT_FOUND, response.resolveI18nString("i18n.lifecycle.status.not.found"));
        }
        final Map<Long, LifeCycleStatus.Label> labeledStatuses = lifeCycleStatusManager.getLabelsByStatus(orgId, Set.copyOf(statusOrders.values()));
        final Map.Entry<Short, Long> lastStatus = statusOrders.lastEntry();

        if (LifeCycleStatus.Label.CLOSED_LOST == labeledStatuses.remove(lastStatus.getValue())) {
            statusOrders.remove(lastStatus.getKey());
            final Map.Entry<Short, Long> lastPreviousStatus = statusOrders.lastEntry();

            if (!labeledStatuses.isEmpty()) {
                if (LifeCycleStatus.Label.CLOSED_WON == labeledStatuses.remove(lastPreviousStatus.getValue())) {
                    statusOrders.remove(lastPreviousStatus.getKey());
                } else {
                    response.put(FieldKeys.MESSAGE.getName(), response.resolveI18nString("i18n.lifecycle.status.order.change.restricted"));
                    return;
                }
            }
            lifeCycleStatusManager.updateOrder(orgId, statusOrders);
            response.put(FieldKeys.MESSAGE.getName(), response.resolveI18nString("i18n.lifecycle.status.order.update.success"));
        } else {
            response.put(FieldKeys.MESSAGE.getName(), response.resolveI18nString("i18n.lifecycle.status.order.change.restricted"));
        }
    }
}

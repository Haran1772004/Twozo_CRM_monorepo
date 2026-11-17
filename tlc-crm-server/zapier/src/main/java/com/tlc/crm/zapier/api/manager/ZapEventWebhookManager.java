package com.tlc.crm.zapier.api.manager;

import com.tlc.commons.code.ErrorCode;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.crm.zapier.api.models.ZapEvent;
import com.tlc.crm.zapier.api.models.ZapEventWebhook;
import com.tlc.crm.zapier.internal.status.ZapierErrorCodes;
import com.tlc.crm.zapier.sql.resource.OMZAPEVENTWEBHOOK;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

/**
 * Manager the DB related handling of Zapier event webhooks.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ZapEventWebhookManager extends AbstractWebhookManager<ZapEventWebhook> {

    private ZapEventWebhookManager() {
    }

    private static class Instance {
        private static final ZapEventWebhookManager TRIGGER_HOOK_MANAGER = new ZapEventWebhookManager();
    }

    /**
     * Gets the instance of {@code ZapTriggerEventHookManager}.
     *
     * @return The {@code ZapTriggerEventHookManager} instance.
     */
    public static ZapEventWebhookManager getInstance() {
        return Instance.TRIGGER_HOOK_MANAGER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(OMZAPEVENTWEBHOOK.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param zapEventWebhook The model object to be saved.
     */
    @Override
    public ZapEventWebhook save(final ZapEventWebhook zapEventWebhook) {
        final Table table = getTable();
        final Long orgId = zapEventWebhook.orgId();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final DataContainer targetContainer = DataContainer.create();
        final DataContainer dataContainer = orgDataStore.get(table,
                new WhereClause(Criteria.eq(table.getColumn(OMZAPEVENTWEBHOOK.ZAP_ID), zapEventWebhook.getZapId())));
        final Row row = Optional.ofNullable(dataContainer.getRow(table)).orElseGet(() -> new Row(table));
        final long currentTime = Instant.now().toEpochMilli();

        if (row.isNewRow()) {
            row.setOrgId(orgId);
            row.set(OMZAPEVENTWEBHOOK.CREATED_TIME, currentTime);
            zapEventWebhook.setId(row.getPKValue());
        }

        row.set(OMZAPEVENTWEBHOOK.USER_ID, zapEventWebhook.getUserId());
        row.set(OMZAPEVENTWEBHOOK.ZAP_ID, zapEventWebhook.getZapId());
        row.set(OMZAPEVENTWEBHOOK.URL, zapEventWebhook.getUrl());
        row.set(OMZAPEVENTWEBHOOK.IS_SUBSCRIBED, zapEventWebhook.isSubscribed());
        row.set(OMZAPEVENTWEBHOOK.MODIFIED_TIME, currentTime);
        row.set(OMZAPEVENTWEBHOOK.EVENT, zapEventWebhook.getZapEvent().getId());

        targetContainer.updateRow(row);
        orgDataStore.commitChanges(targetContainer);

        return zapEventWebhook;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId The ID of the organization to which the entity belongs.
     * @param id    The unique identifier of the entity.
     * @return The {@code ZapTriggerWebhook}.
     */
    @Override
    public ZapEventWebhook get(final Long orgId, final Long id) {
        return Optional.ofNullable(orgDataStore(orgId).get(getTable(), id)).map(this::convertRowToModel)
                .orElseThrow(() -> ErrorCode.get(ZapierErrorCodes.WEBHOOK_SUBSCRIPTION_RECORD_NOT_FOUND, "i18n.crm.zapier.subscribed.webhook.not.found"));
    }

    /**
     * {@inheritDoc}
     *
     * @param row The row containing entity data.
     * @return The {@code ZapTriggerWebhook}.
     */
    @Override
    public ZapEventWebhook convertRowToModel(final Row row) {
        final ZapEventWebhook zapEventWebhook = new ZapEventWebhook();

        zapEventWebhook.setId(row.getPKValue());
        zapEventWebhook.setOrgId(row.getOrgId());
        zapEventWebhook.setZapId(row.get(OMZAPEVENTWEBHOOK.ZAP_ID));
        zapEventWebhook.setUrl(row.get(OMZAPEVENTWEBHOOK.URL));
        zapEventWebhook.setUserId(row.get(OMZAPEVENTWEBHOOK.USER_ID));
        zapEventWebhook.setZapEvent(ZapEvent.get(row.get(OMZAPEVENTWEBHOOK.EVENT)));
        zapEventWebhook.setSubscribed(row.get(OMZAPEVENTWEBHOOK.IS_SUBSCRIBED));
        zapEventWebhook.setCreatedTime(row.get(OMZAPEVENTWEBHOOK.CREATED_TIME));
        zapEventWebhook.setModifiedTime(row.get(OMZAPEVENTWEBHOOK.MODIFIED_TIME));

        return zapEventWebhook;
    }

    /**
     * Retrieves all subscribed webhook URLs for a given organization, user, and Zap event.
     *
     * @param orgId    The ID of the organization.
     * @param userId   The ID of the user.
     * @param zapEvent The Zap event type to filter subscriptions.
     * @return A collection of {@link ZapEventWebhook} objects representing all matching webhook subscriptions.
     */
    public Collection<ZapEventWebhook> getAllSubscribedHookUrl(final Long orgId, final Long userId, final ZapEvent zapEvent) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(OMZAPEVENTWEBHOOK.URL));
        selectQuery.addOrderByClause(new OrderByClause(table.getColumn(OMZAPEVENTWEBHOOK.MODIFIED_TIME), OrderByClause.OrderType.DESCENDING));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMZAPEVENTWEBHOOK.USER_ID), userId))
                .and(Criteria.eq(table.getColumn(OMZAPEVENTWEBHOOK.IS_SUBSCRIBED), true))
                .and(Criteria.eq(table.getColumn(OMZAPEVENTWEBHOOK.EVENT), zapEvent.getId())));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(table).map(row -> {
            final ZapEventWebhook zapEventWebhook = new ZapEventWebhook();

            zapEventWebhook.setOrgId(orgId);
            zapEventWebhook.setUserId(userId);
            zapEventWebhook.setZapEvent(zapEvent);
            zapEventWebhook.setUrl(row.get(OMZAPEVENTWEBHOOK.URL));

            return zapEventWebhook;
        }).toList();
    }

    /**
     * Checks whether a Zap exists for the given organization, user, and event.
     *
     * @param orgId    The ID of the organization.
     * @param userId   The ID of the user.
     * @param zapEvent The Zap event to check for.
     * @return {@code true} if the configuration exists; {@code false} otherwise.
     */
    public boolean exists(final Long orgId, final Long userId, final ZapEvent zapEvent) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMZAPEVENTWEBHOOK.USER_ID), userId))
                .and(Criteria.eq(table.getColumn(OMZAPEVENTWEBHOOK.IS_SUBSCRIBED), true))
                .and(Criteria.eq(table.getColumn(OMZAPEVENTWEBHOOK.EVENT), zapEvent.getId())));

        return orgDataStore(orgId).get(countQuery) > 0;
    }
}

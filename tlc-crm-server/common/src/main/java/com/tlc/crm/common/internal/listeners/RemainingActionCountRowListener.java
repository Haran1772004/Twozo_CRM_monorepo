package com.tlc.crm.common.internal.listeners;

import com.tlc.crm.security.permission.model.ActionLimitDetails;
import com.tlc.crm.security.permission.model.LimitActionType;
import com.tlc.crm.common.iam.registry.ActionLimitRegistry;
import com.tlc.crm.security.sql.resource.OIAMPACTIONLIMITDETAILS;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.RowDeleteListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * A Listener that listens delete actions of remaining action limit count entry.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class RemainingActionCountRowListener implements RowDeleteListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemainingActionCountRowListener.class);

    private final ActionLimitRegistry limitRegistry;

    private final static class Instance {
        private static final RemainingActionCountRowListener ACTION_COUNT_ROW_LISTENER = new RemainingActionCountRowListener();
    }

    public static RemainingActionCountRowListener getInstance() {
        return Instance.ACTION_COUNT_ROW_LISTENER;
    }

    private RemainingActionCountRowListener() {
        limitRegistry = ActionLimitRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param actionCountRows Refers the collection of remaining action count rows.
     */
    @Override
    public void deleteRows(final Collection<Row> actionCountRows) {
        for (final Row actionCountRow : actionCountRows) {
            final Long orgId = actionCountRow.getOrgId();
            final Long userId = actionCountRow.get(OIAMPACTIONLIMITDETAILS.USER_ID);
            final LimitActionType limitActionType = LimitActionType.get(actionCountRow.get(OIAMPACTIONLIMITDETAILS.ACTION_TYPE));

            limitRegistry.clear(orgId, List.of(new ActionLimitDetails(orgId, userId, limitActionType)));
            LOGGER.debug("Cleared the remaining action limit count. Org ID: {}, User ID: {}, Limit Action Type: {}", orgId, userId, limitActionType.name());
        }
    }
}

package com.tlc.crm.core.api.insights.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.crm.core.api.insights.models.EntityInsightsGroup;
import com.tlc.crm.core.api.insights.manager.EntityInsightsRecomputeRequestManager;
import com.tlc.crm.core.api.insights.models.EntityInsightsIdentifierType;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.listeners.RowDeleteListener;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * Listens the deal delete event.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class DealListener implements RowDeleteListener {

    private final EntityInsightsRecomputeRequestManager entityInsightsRecomputeRequestManager;

    private DealListener() {
        this.entityInsightsRecomputeRequestManager = EntityInsightsRecomputeRequestManager.getInstance();
    }

    private static final class InstanceHolder {
        private static final DealListener INSTANCE = new DealListener();
    }

    /**
     * <p>
     * Gets the {@link DealListener} singleton.
     * </p>
     *
     * @return The {@link DealListener} instance.
     */
    public static DealListener getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void deleteRows(final Collection<Row> rows) {
        if (Objects.isNull(rows) || rows.isEmpty()) {
            return;
        }
        Long orgId = null;
        final Collection<Long> contactIds = new HashSet<>();

        for (final Row row : rows) {
            final Long contactId = row.get(MDODEAL.CONTACT_ID);

            if (Objects.isNull(contactId)) {
                continue;
            }
            final Long rowOrgId = row.getOrgId();

            if (Objects.isNull(orgId)) {
                orgId = rowOrgId;
            } else if (!orgId.equals(rowOrgId)) {
                throw ErrorCode.get(ErrorCodes.INVALID_DATA);
            }
            contactIds.add(contactId);
        }

        if (Objects.isNull(orgId) || contactIds.isEmpty()) {
            return;
        }
        entityInsightsRecomputeRequestManager.add(orgId, Source.CONTACT, contactIds, EntityInsightsGroup.DEAL, EntityInsightsIdentifierType.ENTITY);
    }
}
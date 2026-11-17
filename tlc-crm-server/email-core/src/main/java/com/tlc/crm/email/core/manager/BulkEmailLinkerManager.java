package com.tlc.crm.email.core.manager;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.internal.event.contact.publisher.ContactCreatedEventHandler;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.bulk.api.manager.BulkMailManager;
import com.tlc.crm.core.notification.email.bulk.api.model.BulkMail;
import com.tlc.crm.core.notification.email.bulk.api.model.MailInfo;
import com.tlc.crm.core.sql.resource.OMEMBULKEMAIL;

import com.tlc.crm.email.core.EmailLinkerService;
import com.tlc.crm.email.notification.AbstractNotificationManager;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Handles and facilitates db related operations for entities related to {@link BulkMail}.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public final class BulkEmailLinkerManager extends AbstractNotificationManager<BulkMail> {

    private final BulkMailManager bulkMailManager;
    private final ContactManager contactManager;
    private final ContactCreatedEventHandler contactCreatedEventHandler;

    private static class Instance {
        private static final BulkEmailLinkerManager INSTANCE = new BulkEmailLinkerManager();
    }

    private BulkEmailLinkerManager() {
        this.bulkMailManager = BulkMailManager.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.contactCreatedEventHandler = ContactCreatedEventHandler.getInstance();
    }

    /**
     * Gets the {@link BulkEmailLinkerManager} instance
     *
     * @return {@link BulkEmailLinkerManager}
     */
    public static BulkEmailLinkerManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the bulk mail table
     *
     * @return the {@link Table} of bulk mail
     */
    @Override
    public Table getTable() {
        return Table.get(OMEMBULKEMAIL.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param bulkMail Represents the {@link BulkMail} details
     */
    @Override
    public BulkMail create(final BulkMail bulkMail) {
        final Long orgId = bulkMail.orgId();
        final DataContainer targetContainer = DataContainer.create();

        loadBulkMailIntoContainer(true, bulkMail, targetContainer);
        orgDataStore(orgId).commitChanges(targetContainer);
        contactCreatedEventHandler.triggerEvents(orgId, bulkMail.getSenderId(), contactManager.getContactIds(targetContainer));

        return bulkMail;
    }

    /**
     * <p>
     * Updates the given {@link BulkMail}
     * </p>
     *
     * @param bulkMail Represents the {@link BulkMail}
     */
    public void update(final BulkMail bulkMail) {
        final Long bulkMailId = bulkMail.id();
        final Long orgId = bulkMail.orgId();
        final DataContainer existingMailContainer = bulkMailManager.fetchBulkMailDataContainer(orgId, List.of(bulkMailId));
        final DataContainer targetContainer = DataContainer.create();

        BulkMailManager.getInstance().loadBulkMailIntoContainer(false, bulkMail, existingMailContainer, targetContainer);
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * Loads a bulk mail details into the {@link DataContainer}
     *
     * @param isCreateEvent   Indicates whether the action is related to create or update event
     * @param bulkMail        Which holds the {@link BulkMail} attributes
     * @param targetContainer Which holds the information of bulk mail field values provided by user
     */
    private void loadBulkMailIntoContainer(final boolean isCreateEvent, final BulkMail bulkMail, final DataContainer targetContainer) {
        final Long bulkMailId = bulkMail.id();
        final DataContainer existingMailContainer = Objects.isNull(bulkMailId) ? null : fetchBulkMailDataContainer(bulkMail.orgId(), bulkMailId);
        final EmailLinkerService emailLinkerService = EmailLinkerService.getInstance();

        for (final MailInfo info: bulkMail.getMailInfo()) {
            final EntityOutboundEmail entityOutboundEmail = info.getOutboundEmail();

            emailLinkerService.createNewContactForRecipients(bulkMail.orgId(), bulkMail.getSenderId(), entityOutboundEmail, false, targetContainer);
        }
        BulkMailManager.getInstance().loadBulkMailIntoContainer(isCreateEvent, bulkMail, existingMailContainer, targetContainer);
    }

    /**
     * <p>
     * Fetches the bulk mail {@link DataContainer} for the given bulk mail id
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param bulkMailId Represents the bulk mail id
     * @return the {@link DataContainer}
     */
    private DataContainer fetchBulkMailDataContainer(final long orgId, final Long bulkMailId) {
        final Table bulkMailTable = getTable();
        final Column bulkMailPkColumn = bulkMailTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(bulkMailTable);

        selectQuery.addSelectClause(bulkMailTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(bulkMailPkColumn, bulkMailId)));

        return orgDataStore(orgId).get(selectQuery);
    }
}
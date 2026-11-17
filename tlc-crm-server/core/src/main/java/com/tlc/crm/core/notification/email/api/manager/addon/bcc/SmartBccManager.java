package com.tlc.crm.core.notification.email.api.manager.addon.bcc;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.notification.email.api.manager.EntityMailManager;
import com.tlc.crm.core.notification.email.api.manager.addon.AbstractEmailAddOnManager;
import com.tlc.crm.core.notification.email.api.model.email.SmartBcc;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCONEMAILFIELDS;

import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.sql.resource.OMETEMAIL;

import com.tlc.crm.email.notification.sql.resource.OMEMSMARTBCCADDRESS;
import com.tlc.crm.email.notification.sql.resource.MEMPARENTBCCADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;

import com.tlc.email.common.model.address.EmailAddress;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Implements the smart bcc related DB actions
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class SmartBccManager extends AbstractEmailAddOnManager<SmartBcc> {

    private final ContactManager contactManager;
    private final EmailManager emailManager;
    private final EntityMailManager entityMailManager;
    private final FieldService fieldService;

    private static class Instance {
        private static final SmartBccManager INSTANCE = new SmartBccManager();
    }

    private SmartBccManager() {
        contactManager = ContactManager.getInstance();
        emailManager = EmailManager.getInstance();
        fieldService = FieldService.getInstance();
        entityMailManager = EntityMailManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link SmartBccManager} Instance
     * </p>
     *
     * @return {@link SmartBccManager}
     */
    public static SmartBccManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the parent bcc address table
     * </p>
     *
     * @return {@link Table} of parent bcc address table
     */
    private Table getParentBccAddressTable() {
        return Table.get(MEMPARENTBCCADDRESS.TABLE);
    }

    /**
     * <p>
     * Gets the email address table
     * </p>
     *
     * @return the email address table
     */
    private Table getAddressTable() {
        return Table.get(OMEMADDRESS.TABLE);
    }

    /**
     * <p>
     * Gets the smart bcc address table
     * </p>
     *
     * @return {@link Table} the smart bcc address
     */
    @Override
    public Table getTable() {
        return Table.get(OMEMSMARTBCCADDRESS.TABLE);
    }

    /**
     * <p>
     * Retrieves the parent BCC address table.
     * </p>
     *
     * @return the parent BCC address {@link Table}
     */
    @Override
    protected Table getParentAddressTable() {
        return this.getParentBccAddressTable();
    }

    /**
     * <p>
     * Checks whether the recipients have the valid smart bcc address which is allocated for the given organization
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param addresses Represents the {@link Collection} of email address
     * @return true, if the given address is valid smart bcc address
     */
    public boolean hasValidSmartBccAddress(final Long orgId, final Collection<String> addresses) {
        final Table smartBccAddressTable = getTable();
        final Table addressTable = getAddressTable();
        final CountQuery countQuery = CountQuery.get(addressTable);

        countQuery.addJoinClause(List.of(
                new JoinClause(addressTable.getPKColumn(), smartBccAddressTable.getColumn(OMEMSMARTBCCADDRESS.EMAIL_ID)),
                new JoinClause(smartBccAddressTable.getColumn(OMEMSMARTBCCADDRESS.PARENT_BCC_ID), getParentBccAddressTable().getPKColumn())));
        countQuery.setWhereClause(new WhereClause(Criteria.in(addressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), addresses)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Fetches the smart bcc address for the given organization id
     * </p>
     *
     * @param orgId Represents an organization id
     * @return the smart bcc address
     */
    public String fetchSmartBccAddress(final Long orgId) {
        final Table addressTable = getAddressTable();
        final Table smartBccAddressTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(addressTable);

        selectQuery.addJoinClause(List.of(
                new JoinClause(addressTable.getPKColumn(), smartBccAddressTable.getColumn(OMEMSMARTBCCADDRESS.EMAIL_ID)),
                new JoinClause(smartBccAddressTable.getColumn(OMEMSMARTBCCADDRESS.PARENT_BCC_ID), getParentBccAddressTable().getPKColumn())));
        selectQuery.addSelectClause(addressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(addressTable);

        return Objects.isNull(row) ? null : row.get(OMEMADDRESS.EMAIL_ADDRESS);
    }

    /**
     * <p>
     * Load the rows related to contacts into a container for the provided email Ids that are not have any existing contacts within the organization.
     * </p>
     *
     * @param contact        An entity which holds the contact attributes
     * @param conversationId Represents the conversation id
     * @param dataContainer  A container which holds values of contact creation related rows
     */
    public void loadContactRowIntoContainer(final Contact contact, final Long conversationId, final DataContainer dataContainer) {
        final Long currentTime = Instant.now().toEpochMilli();
        final Row contactRow = new Row(contactManager.getTable());
        final Row contactEmailRow = new Row(contactManager.getContactEmailTable());
        final Row emailRow = new Row(emailManager.getTable());
        final String email = Objects.requireNonNull(contact.getEmails().stream().findFirst().orElse(null)).getValue();
        final String emailKey = ContactFields.EMAILS;
        final Long contactId = contactRow.getPKValue();
        final Long ownerId = contact.getOwnerId();
        final Long emailPkValue = emailRow.getPKValue();

        contactRow.set(MCOCONTACT.FIRST_NAME, contact.getFirstName());
        contactRow.set(MCOCONTACT.LIFE_CYCLE_STAGE_ID, contact.getLifeCycleStage().id());
        contactRow.set(MCOCONTACT.LIFE_CYCLE_STATUS_ID, contact.getLifeCycleStatus().id());
        contactRow.set(MCOCONTACT.CREATED_TIME, currentTime);
        contactRow.set(MCOCONTACT.MODIFIED_TIME, currentTime);
        contactRow.set(MCOCONTACT.LAST_ASSIGNED_AT, currentTime);
        contactRow.set(MCOCONTACT.OWNER_ID, ownerId);
        contactRow.set(MCOCONTACT.UPDATED_BY, ownerId);
        contactRow.set(MCOCONTACT.CREATED_BY, ownerId);
        contactRow.set(MCOCONTACT.PRIMARY_EMAIL, emailPkValue);
        contact.setId(contactId);

        emailRow.set(OMETEMAIL.VALUE, email);
        emailRow.set(OMETEMAIL.TYPE, Email.Type.WORK.getId());

        contactEmailRow.set(MCONEMAILFIELDS.EMAIL_ID, emailPkValue);
        contactEmailRow.set(MCONEMAILFIELDS.ENTITY_ID, contactId);
        contactEmailRow.set(MCONEMAILFIELDS.FIELD_ID, fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, emailKey).get(emailKey).id());

        dataContainer.addNewRow(contactRow);
        dataContainer.addNewRow(emailRow);
        dataContainer.addNewRow(contactEmailRow);

        entityMailManager.loadContactConversation(contact.orgId(), List.of(contactId), conversationId, dataContainer);
    }

    /**
     * <p>
     * Builds the contact for the email address which have no contacts in an organization
     * </p>
     *
     * @param orgId                   Represents an organization id
     * @param defaultLifeCycleStageId Represents the default lifecycle stage id
     * @param emailAddress            Represents an email Address
     * @param ownerId                 Represents an owner id
     * @return {@link Contact}
     */
    public Contact buildContact(final Long orgId, final Long defaultLifeCycleStageId, final Long defaultLifeCycleStatusId,
                                final String emailAddress, final Long ownerId) {
        final Contact contact = new Contact();
        final Email email = new Email();
        final LifeCycleStage lifeCycleStage = new LifeCycleStage();
        final LifeCycleStatus lifeCycleStatus = new LifeCycleStatus();

        email.setValue(emailAddress);
        lifeCycleStage.setId(defaultLifeCycleStageId);
        lifeCycleStatus.setId(defaultLifeCycleStatusId);
        contact.setOrgId(orgId);
        contact.setOwnerId(ownerId);
        contact.setFirstName(EmailAddress.extractNameFromEmail(emailAddress));
        contact.addEmail(email);
        contact.setLifeCycleStage(lifeCycleStage);
        contact.setLifeCycleStatus(lifeCycleStatus);

        return contact;
    }

    /**
     * <p>
     * Loads a row into the data container for the Smart BCC address.
     * </p>
     *
     * @param orgId         Represents the organization ID
     * @param emailId       Represents the email ID
     * @param parentEmailId Represents the parent email ID
     * @param dataContainer Represents the {@link DataContainer} where the row will be added
     */
    protected void loadRowIntoContainer(final Long orgId, final Long emailId, final Long parentEmailId, final DataContainer dataContainer) {
        final Row row = new Row(getTable());

        row.set(OMEMSMARTBCCADDRESS.PARENT_BCC_ID, parentEmailId);
        row.set(OMEMSMARTBCCADDRESS.EMAIL_ID, emailId);
        row.setOrgId(orgId);

        dataContainer.addNewRow(row);

        orgDataStore(orgId).commitChanges(dataContainer);
    }
}

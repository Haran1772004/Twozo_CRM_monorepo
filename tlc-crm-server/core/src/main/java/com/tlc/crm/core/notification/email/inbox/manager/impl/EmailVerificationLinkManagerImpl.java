package com.tlc.crm.core.notification.email.inbox.manager.impl;

import com.tlc.crm.core.notification.email.inbox.api.model.EmailVerificationLink;
import com.tlc.crm.core.notification.email.inbox.manager.EmailVerificationLinkManager;
import com.tlc.crm.email.notification.email.api.manager.MailManager;
import com.tlc.crm.email.notification.model.UserSyncAddressInfo;
import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMVERIFICATIONLINKDETAILS;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import java.util.Optional;

/**
 * <p>
 * Provides the implementation of {@link EmailVerificationLinkManager}, responsible for
 * persisting and retrieving {@link EmailVerificationLink} entities from the datastore.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailVerificationLinkManagerImpl implements EmailVerificationLinkManager {

    private final MailManager mailManager;

    /**
     * <p>
     * Singleton instance holder for {@link EmailVerificationLinkManagerImpl}.
     * </p>
     */
    private static class Instance {
        private static final EmailVerificationLinkManager INSTANCE = new EmailVerificationLinkManagerImpl();
    }

    /**
     * <p>
     * Private constructor to enforce singleton access through {@link #getInstance()}.
     * </p>
     */
    private EmailVerificationLinkManagerImpl() {
        this.mailManager = MailManager.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link ReadableDataStore} used for performing read operations
     * against the database.
     * </p>
     *
     * @return the readable datastore instance
     */
    private ReadableDataStore getReadableDataStore() {
        return DataStoreProvider.getReadableDataStore();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link EmailVerificationLinkManager}.
     * </p>
     *
     * @return the singleton manager instance
     */
    public static EmailVerificationLinkManager getInstance() {
        return EmailVerificationLinkManagerImpl.Instance.INSTANCE;
    }

    /**
     * <p>
     * Retrieves the table definition for {@link OMEMVERIFICATIONLINKDETAILS}.
     * </p>
     *
     * @return the verification email link details table
     */
    private Table getTable() {
        return Table.get(OMEMVERIFICATIONLINKDETAILS.TABLE);
    }

    /**
     * <p>
     * Retrieves the table definition for {@link OMEMADDRESS}.
     * </p>
     *
     * @return the email address table
     */
    private Table getEmailAddressTable() {
        return Table.get(OMEMADDRESS.TABLE);
    }

    /**
     * <p>
     * Retrieves a secure {@link OrgDataStore} associated with the given organization ID.
     * </p>
     *
     * @param orgId the organization identifier
     * @return the corresponding organization datastore
     */
    protected final OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Persists the provided {@link EmailVerificationLink} into the database.
     * </p>
     *
     * @param emailVerificationLink the verification link to save
     */
    @Override
    public void save(final EmailVerificationLink emailVerificationLink) {
        final Table table = getTable();
        final Row row = new Row(table);
        final long orgId = emailVerificationLink.getOrgId();
        final Long userId = emailVerificationLink.getUserId();
        final DataContainer targetContainer = DataContainer.create();
        final EmailAddress emailAddress = new EmailAddress(emailVerificationLink.getEmailAddress().getEmailAddress());

        mailManager.loadEmailAddress(orgId, userId, emailAddress, targetContainer);

        row.setOrgId(orgId);
        row.set(OMEMVERIFICATIONLINKDETAILS.USER_ID, userId);
        row.set(OMEMVERIFICATIONLINKDETAILS.EMAIL_ID, emailAddress.id());
        row.set(OMEMVERIFICATIONLINKDETAILS.TRACKING_ID, emailVerificationLink.getTrackingId());
        row.set(OMEMVERIFICATIONLINKDETAILS.EXPIRATION_TIME, emailVerificationLink.getExpireTime());
        row.set(OMEMVERIFICATIONLINKDETAILS.CATEGORY, emailVerificationLink.getCategory().getId());

        emailVerificationLink.setId(row.getPKValue());
        targetContainer.addNewRow(row);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Retrieves an {@link EmailVerificationLink} by its tracking identifier.
     * </p>
     *
     * @param trackingId the tracking identifier used to locate the link
     * @return an {@link Optional} containing the {@link EmailVerificationLink},
     * or an empty {@link Optional} if not found
     */
    @Override
    public Optional<EmailVerificationLink> get(final String trackingId) {
        final Table table = getTable();
        final Table emailAddressTable = getEmailAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addJoinClause(
                new JoinClause(table.getColumn(OMEMVERIFICATIONLINKDETAILS.EMAIL_ID), emailAddressTable.getPKColumn())
        );
        selectQuery.setWhereClause(
                new WhereClause(Criteria.eq(table.getColumn(OMEMVERIFICATIONLINKDETAILS.TRACKING_ID), trackingId))
        );
        final DataContainer dataContainer = getReadableDataStore().get(selectQuery);

        return dataContainer.isEmpty() ? Optional.empty()
                : Optional.of(convertRowToModel(dataContainer.getRow(table), dataContainer));
    }

    /**
     * <p>
     * Converts a database {@link Row} and its related data into an {@link EmailVerificationLink} model.
     * </p>
     *
     * @param row           the row from {@link OMEMVERIFICATIONLINKDETAILS}
     * @param dataContainer the data container holding related rows
     * @return a populated {@link EmailVerificationLink} model
     */
    private EmailVerificationLink convertRowToModel(final Row row, final DataContainer dataContainer) {
        final EmailVerificationLink emailVerificationLink = new EmailVerificationLink();
        final Table emailAddressTable = getEmailAddressTable();

        emailVerificationLink.setId(row.getPKValue());
        emailVerificationLink.setOrgId(row.getOrgId());
        emailVerificationLink.setTrackingId(row.get(OMEMVERIFICATIONLINKDETAILS.TRACKING_ID));
        emailVerificationLink.setUserId(row.get(OMEMVERIFICATIONLINKDETAILS.USER_ID));
        emailVerificationLink.setExpireTime(row.get(OMEMVERIFICATIONLINKDETAILS.EXPIRATION_TIME));
        emailVerificationLink.setCategory(EmailVerificationLink.Category.get(row.get(OMEMVERIFICATIONLINKDETAILS.CATEGORY)));
        final Row emailRow = dataContainer.getRow(emailAddressTable, (Long) row.get(OMEMVERIFICATIONLINKDETAILS.EMAIL_ID));
        final UserSyncAddressInfo syncAddressInfo = new UserSyncAddressInfo(emailRow.get(OMEMADDRESS.EMAIL_ADDRESS));

        syncAddressInfo.setId(emailRow.getPKValue());
        emailVerificationLink.setEmailAddress(syncAddressInfo);

        return emailVerificationLink;
    }
}
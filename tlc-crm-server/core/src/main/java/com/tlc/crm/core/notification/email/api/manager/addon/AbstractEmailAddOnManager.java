package com.tlc.crm.core.notification.email.api.manager.addon;

import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.email.notification.AbstractNotificationManager;
import com.tlc.crm.email.notification.NotificationModel;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.iam.common.models.User;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides an abstract base class for managing add-on email notifications.
 * </p>
 *
 * @param <T> The type of notification model handled by this manager.
 * @author Mohamed Yasar K
 * @version 1.0
 */
public abstract class AbstractEmailAddOnManager<T extends NotificationModel> extends AbstractNotificationManager<T> {

    private final CoreMailManager mailManager;

    /**
     * <p>
     * Constructs an {@code AbstractEmailAddOnManager} and initializes the mail manager instance.
     * </p>
     */
    protected AbstractEmailAddOnManager() {
        this.mailManager = CoreMailManager.getInstance();
    }

    /**
     * <p>
     * Loads the row into the provided data container.
     * </p>
     *
     * @param orgId         The organization ID.
     * @param emailId       The email ID.
     * @param parentEmailId The parent email ID.
     * @param dataContainer The {@link DataContainer} where the row will be loaded.
     */
    protected abstract void loadRowIntoContainer(final Long orgId, final Long emailId,
                                                 final Long parentEmailId, final DataContainer dataContainer);

    /**
     * <p>
     * Returns the parent address table associated with this manager.
     * </p>
     *
     * @return The {@link Table} representing the parent address table.
     */
    protected abstract Table getParentAddressTable();

    /**
     * <p>
     * Loads the primary email address rows into the data container.
     * </p>
     *
     * @param emailAddresses The collection of {@link EmailAddress} objects to be loaded.
     */
    public void loadParentAddressesIntoContainer(final Collection<EmailAddress> emailAddresses) {
        if (Objects.isNull(emailAddresses) || emailAddresses.isEmpty()) {
            return;
        }
        final Table parentAddressTable = getParentAddressTable();
        final Column addressTableColumn = parentAddressTable.getColumn(CoreEmailFields.EMAIL_ADDRESS.name());
        final Collection<String> emails = emailAddresses.stream()
                .map(EmailAddress::getEmailAddress)
                .collect(Collectors.toUnmodifiableSet());

        final DataContainer dataContainer = writableDataStore().get(parentAddressTable,
                new WhereClause(Criteria.in(addressTableColumn, emails)));
        final DataContainer targetContainer = DataContainer.create();

        for (final EmailAddress emailAddress : emailAddresses) {
            final String emailId = emailAddress.getEmailAddress();
            final Row row = dataContainer.getRow(parentAddressTable,
                    new WhereClause(Criteria.eq(addressTableColumn, emailId)));

            if (Objects.isNull(row)) {
                final Row parentAddressRow = new Row(parentAddressTable);

                parentAddressRow.set(CoreEmailFields.EMAIL_ADDRESS.name(), emailId);

                targetContainer.addNewRow(parentAddressRow);
            }
        }

        if (!targetContainer.isEmpty()) {
            writableDataStore().commitChanges(targetContainer);
        }
    }

    /**
     * <p>
     * Retrieves a row of the parent email address.
     * </p>
     *
     * @return A {@link Row} containing the parent email address information.
     */
    public Row getParentAddress() {
        final Table parentAddressTable = getParentAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(parentAddressTable);

        selectQuery.addSelectClause(parentAddressTable.getColumn(CoreEmailFields.EMAIL_ADDRESS.name()));

        return writableDataStore().get(selectQuery).getRow(parentAddressTable);
    }

    /**
     * <p>
     * Loads the email address-related rows into the data container.
     * </p>
     *
     * @param crmOrgId         The CRM organization ID.
     * @param crmUserId        The CRM user ID.
     * @param users            The {@link Collection} of users associated with the email addresses.
     * @param emailAddress     The primary {@link EmailAddress}.
     * @param primaryAddressId The primary address ID.
     */
    public void loadEmailAddress(final Long crmOrgId, final Long crmUserId, final Collection<User> users,
                                 final EmailAddress emailAddress, final Long primaryAddressId) {
        final DataContainer dataContainer = DataContainer.create();

        for (final User user : users) {
            mailManager.loadEmailAddress(crmOrgId, crmUserId, new EmailAddress(user.getEmail()), dataContainer);
        }
        mailManager.loadEmailAddress(crmOrgId, crmUserId, emailAddress, dataContainer);
        loadRowIntoContainer(crmOrgId, emailAddress.id(), primaryAddressId, dataContainer);
    }
}
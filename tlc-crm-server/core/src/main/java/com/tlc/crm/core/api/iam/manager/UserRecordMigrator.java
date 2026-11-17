package com.tlc.crm.core.api.iam.manager;

import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.handler.EventDataHandler;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.internal.event.CoreEvent;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MPOPRODUCT;
import com.tlc.crm.entity.features.api.activity.manager.ActivityManager;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.sql.resource.OMACACTIVITY;
import com.tlc.crm.notification.app.Event;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.validator.TlcModel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.tlc.crm.notification.app.event.NotificationEvent.EMAIL_ALERT_EVENT;

/**
 * <p>
 * Migrates the given {@link Collection} of user records to another user
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class UserRecordMigrator {

    private static final String TRANSFER_USER_RECORDS = "transferUserRecords";
    private final DealManager dealManager;
    private final ContactManager contactManager;
    private final ActivityManager activityManager;
    private final ProductManager productManager;
    private final CompanyManager companyManager;
    private final EventDataHandler eventDataHandler;

    private UserRecordMigrator() {
        dealManager = DealManager.getInstance();
        contactManager = ContactManager.getInstance();
        activityManager = ActivityManager.getInstance();
        productManager = ProductManager.getInstance();
        companyManager = CompanyManager.getInstance();
        eventDataHandler = EventDataHandler.getInstance();
    }

    private static class Instance {
        private static final UserRecordMigrator INSTANCE = new UserRecordMigrator();
    }

    /**
     * Gets the {@link UserRecordMigrator} instance
     *
     * @return the {@link UserRecordMigrator} instance
     */
    public static UserRecordMigrator getInstance() {
        return UserRecordMigrator.Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore}
     * </p>
     *
     * @param orgId Represents an organization id
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Updates the target user id for contact, deal, company, product and activity for the given {@link Collection} of source user ids
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param targetUserId  Represent the target user id to update
     * @param sourceUserIds Represents the {@link Collection} of source user ids to be updated
     */
    public void transferUserRecords(final Long orgId, final Long targetUserId, final Collection<Long> sourceUserIds) {
        final List<UpdateQuery> updateQueries = new ArrayList<>();
        final Collection<Contact> contacts = contactManager.getContacts(orgId, sourceUserIds);
        final Collection<Company> companies = companyManager.getCompanies(orgId, sourceUserIds);
        final Collection<Deal> deals = dealManager.getDeals(orgId, sourceUserIds);
        final Collection<Product> products = productManager.getProducts(orgId, sourceUserIds);

        updateQueries.add(getUpdateQuery(contactManager.getTable(), Source.CONTACT, MCOCONTACT.OWNER_ID, targetUserId, sourceUserIds));
        updateQueries.add(getUpdateQuery(dealManager.getTable(), Source.DEAL, MDODEAL.OWNER_ID, targetUserId, sourceUserIds));
        updateQueries.add(getUpdateQuery(companyManager.getTable(), Source.COMPANY, MCOCOMPANY.OWNER_ID, targetUserId, sourceUserIds));
        updateQueries.add(getUpdateQuery(activityManager.getTable(), Source.ACTIVITY, OMACACTIVITY.ASSIGNEE_ID, targetUserId, sourceUserIds));
        updateQueries.add(getUpdateQuery(productManager.getTable(), Source.PRODUCT, MPOPRODUCT.OWNER_ID, targetUserId, sourceUserIds));

        orgDataStore(orgId).update(updateQueries);
        final Collection<Long> targetUserIds = List.of(targetUserId);

        if (!contacts.isEmpty()) {
            processPostEvents(orgId, targetUserId, contactManager.getContacts(orgId, targetUserIds), contacts);
        }

        if (!companies.isEmpty()) {
            processPostEvents(orgId, targetUserId, companyManager.getCompanies(orgId, targetUserIds), companies);
        }

        if (!deals.isEmpty()) {
           final Collection<Deal> currentDeals = dealManager.getDeals(orgId, targetUserIds);

            processPostEvents(orgId, targetUserId, currentDeals, deals);
            processDeal(orgId, targetUserId, new ArrayList<>(currentDeals), deals);
        }

        if (!products.isEmpty()) {
            processPostEvents(orgId, targetUserId, productManager.getProducts(orgId, targetUserIds), products);
        }
    }

    /**
     * <p>
     * Gets the update query for updating the user records based on given details
     * </p>
     *
     * @param table         Represents the table which is needs to be updates
     * @param source        Represents the {@link Source}
     * @param columnName    Represents the column name
     * @param targetUserId  Represent the target user id to update
     * @param sourceUserIds Represents the {@link Collection} of source user ids to be updated
     */
    private UpdateQuery getUpdateQuery(final Table table, final Source source, final String columnName, final Long targetUserId, final Collection<Long> sourceUserIds) {
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        if (!Objects.equals(Source.ACTIVITY, source)) {
            updateQuery.addUpdateColumn(table.getColumn(FieldKeys.LAST_ASSIGNED_AT.getName()), Instant.now().toEpochMilli());
        }

        updateQuery.addUpdateColumn(table.getColumn(columnName), targetUserId);
        updateQuery.setWhereClause(new WhereClause(Criteria.in(table.getColumn(columnName), sourceUserIds)));

        return updateQuery;
    }

    /**
     * <p>
     * Initiates the post process events
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param userId         Represents the user id
     * @param oldModelValues Which contains collections of old {@link TlcModel}
     * @param newModelValues Which contains collections of new {@link TlcModel}
     */
    private void processPostEvents(final long orgId, final long userId, final Collection<? extends TlcModel> oldModelValues, final Collection<? extends TlcModel> newModelValues) {
        final EventData eventData = new SecureEventData(orgId, userId);

        eventData.setValues(newModelValues);
        eventData.setPreviousValues(oldModelValues);
        eventData.setAction(Action.UPDATE);
        eventData.setApiType(TRANSFER_USER_RECORDS);

        eventDataHandler.handleEvent(EMAIL_ALERT_EVENT, eventData, Event.Type.POST);
    }

    /**
     * <p>
     * Processes  {@link Deal} and triggers quota-related event handling.
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param userId        Represents the user id
     * @param updatedDeals  Which contains collections of updated {@link Deal}
     * @param previousDeals Which contains collections of preview {@link Deal}
     */
    private void processDeal(final long orgId, final long userId, final Collection<Deal> updatedDeals,
                             final Collection<Deal> previousDeals) {
        final EventData eventData = new SecureEventData(orgId, userId);
        final Map<Long, Long> previousOwnerMap = new HashMap<>();
        final Set<Long> previousDealIds = new HashSet<>();

        for (final Deal deal : previousDeals) {
            previousDealIds.add(deal.id());
            previousOwnerMap.put(deal.id(), deal.getOwnerId());
        }
        final Collection<Deal> persistedDeals = dealManager.get(orgId, previousDealIds);

        for (final Deal persistedDeal : persistedDeals) {
            final Long previousOwnerId = previousOwnerMap.get(persistedDeal.id());

            if (Objects.nonNull(previousOwnerId)) {
                persistedDeal.setOwnerId(previousOwnerId);
            }
        }

        updatedDeals.removeIf(deal -> !previousOwnerMap.containsKey(deal.id()));
        eventData.setValues(updatedDeals);
        eventData.setPreviousValues(persistedDeals);
        eventData.setAction(Action.UPDATE);
        eventData.setApiType(TRANSFER_USER_RECORDS);

        eventDataHandler.handleEvent(CoreEvent.QUOTA_DEAL_EVENT, eventData, Event.Type.POST);
    }
}
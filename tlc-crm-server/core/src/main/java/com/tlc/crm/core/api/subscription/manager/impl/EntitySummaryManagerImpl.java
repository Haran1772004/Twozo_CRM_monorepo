package com.tlc.crm.core.api.subscription.manager.impl;

import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.subscription.manager.EntitySummaryManager;
import com.tlc.crm.core.api.subscription.model.DataLossSummary;
import com.tlc.crm.core.sql.resource.MDODEAL;

import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.currency.api.model.BaseCurrency;
import com.tlc.crm.entity.features.api.activity.manager.ActivityManager;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.sql.resource.OMACACTIVITY;

import com.tlc.crm.fields.sql.resource.OMETMONETARY;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.exchange.rate.model.ReadableExchangeRateContainer;

import com.tlc.l10n.model.currency.Currency;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link EntitySummaryManager} related actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class EntitySummaryManagerImpl implements EntitySummaryManager {

    private final DealManager dealManager;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final ActivityManager activityManager;
    private final MonetaryManager monetaryManager;
    private final BaseCurrencyService baseCurrencyService;

    private EntitySummaryManagerImpl() {
        dealManager = DealManager.getInstance();
        contactManager = ContactManager.getInstance();
        companyManager = CompanyManager.getInstance();
        activityManager = ActivityManager.getInstance();
        monetaryManager = MonetaryManager.getInstance();
        baseCurrencyService = BaseCurrencyService.getInstance();
    }

    private static final class InstanceHolder {
        private static final EntitySummaryManagerImpl ENTITY_SUMMARY_MANAGER = new EntitySummaryManagerImpl();
    }

    /**
     * Gets the {@link EntitySummaryManager} instance.
     *
     * @return {@link EntitySummaryManager}.
     */
    public static EntitySummaryManager getInstance() {
        return InstanceHolder.ENTITY_SUMMARY_MANAGER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @return {@link DataLossSummary}.
     */
    @Override
    public DataLossSummary get(final long orgId) {
        final DataLossSummary dataLossSummary = new DataLossSummary();

        dataLossSummary.setOpenDealsValue(getOpenDealsValue(orgId));
        dataLossSummary.setOpenDealsCount(getOpenDealsCount(orgId));
        dataLossSummary.setContactsCount(getContactsCount(orgId));
        dataLossSummary.setCompaniesCount(getCompaniesCount(orgId));
        dataLossSummary.setScheduledActivitiesCount(getScheduledActivitiesCount(orgId));

        return dataLossSummary;
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore} for the given organization.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @return the corresponding {@link OrgDataStore}.
     */
    private OrgDataStore getOrgDataStore(final long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * <p>
     * Fetches the open deals value of the given organization.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @return {@link Monetary}.
     */
    private Monetary getOpenDealsValue(final long orgId) {
        final Table dealTable = dealManager.getTable();
        final Table monetaryTable = monetaryManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(dealTable);

        selectQuery.addSelectClause(monetaryTable);
        selectQuery.addJoinClause(new JoinClause(dealTable.getColumn(MDODEAL.MONETARY_ID), monetaryTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getColumn(MDODEAL.STATE), Deal.State.OPEN.id())));
        final Stream<Row> monetaryRows = getOrgDataStore(orgId).get(selectQuery).getRows(monetaryTable);
        final BaseCurrency baseCurrency = baseCurrencyService.getBaseCurrency(orgId);
        final ReadableExchangeRateContainer exchangeRateContainer = baseCurrency.getReadableExchangeRateContainer();
        final Currency currency = baseCurrency.getDefaultCurrency();
        final Monetary dealValue = new Monetary();

        dealValue.setCurrency(currency);
        dealValue.setValue(BigDecimal.ZERO);

        return monetaryRows.reduce(dealValue, (monetary, monetaryRow) -> {
            final BigDecimal value = BigDecimal.valueOf((double) monetaryRow.get(OMETMONETARY.VALUE));
            final Currency valueCurrency = Currency.getCurrencyById(monetaryRow.get(OMETMONETARY.CURRENCY_ID));

            monetary.addValue(exchangeRateContainer.getExchangeRate(valueCurrency).multiply(value));
            return monetary;
        }, (monetary, ignoredMonetary) -> monetary);
    }

    /**
     * <p>
     * Fetches the open deals count of the given organization.
     * </p>
     *
     * @param orgId Represents the organization id.
     */
    private int getOpenDealsCount(final long orgId) {
        final Table dealTable = dealManager.getTable();
        final CountQuery countQuery = CountQuery.get(dealTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(dealTable.getColumn(MDODEAL.STATE), Deal.State.OPEN.id())));
        return Math.toIntExact(getOrgDataStore(orgId).get(countQuery));
    }

    /**
     * <p>
     * Fetches the contacts count of the given organization.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @return the total number of contacts.
     */
    private int getContactsCount(final long orgId) {
        final Table contactTable = contactManager.getTable();

        return Math.toIntExact(getOrgDataStore(orgId).get(CountQuery.get(contactTable)));
    }

    /**
     * <p>
     * Fetches the companies count of the given organization.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @return the total number of companies.
     */
    private int getCompaniesCount(final long orgId) {
        final Table companyTable = companyManager.getTable();

        return Math.toIntExact(getOrgDataStore(orgId).get(CountQuery.get(companyTable)));
    }

    /**
     * <p>
     * Fetches the scheduled activities count of the given organization.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @return the total number of scheduled activities.
     */
    private int getScheduledActivitiesCount(final long orgId) {
        final Table activityTable = activityManager.getTable();
        final CountQuery countQuery = CountQuery.get(activityTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(activityTable.getColumn(OMACACTIVITY.STATUS),
                Set.of(Status.UPCOMING.getId(), Status.OVERDUE.getId()))));
        return Math.toIntExact(getOrgDataStore(orgId).get(countQuery));
    }
}
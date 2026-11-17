package com.tlc.crm.plan.and.billing.api.manager.impl;

import com.tlc.commons.code.ErrorCode;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.models.Action;

import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.common.models.SortConfig;

import com.tlc.crm.entity.manager.field.type.AddressManager;

import com.tlc.crm.fields.sql.resource.OMETADDRESS;

import com.tlc.crm.plan.and.billing.api.manager.OrgPlanManager;
import com.tlc.crm.plan.and.billing.api.models.BillingDetails;
import com.tlc.crm.plan.and.billing.api.models.Invoice;
import com.tlc.crm.plan.and.billing.api.models.InvoiceSummary;
import com.tlc.crm.plan.and.billing.api.models.Invoices;
import com.tlc.crm.plan.and.billing.api.models.OrgSubscriptionPlan;
import com.tlc.crm.plan.and.billing.api.models.OrgSubscriptions;
import com.tlc.crm.plan.and.billing.api.models.PlanActivationRequest;
import com.tlc.crm.plan.and.billing.api.models.PlanCancellationData;
import com.tlc.crm.plan.and.billing.api.models.PlanContractState;
import com.tlc.crm.plan.and.billing.api.models.SubscriptionStatus;
import com.tlc.crm.plan.and.billing.calculator.PlanExpirationCalculator;
import com.tlc.crm.plan.and.billing.fields.SubscriptionPlanFields;
import com.tlc.crm.plan.and.billing.internal.status.SubscriptionErrorCodes;
import com.tlc.crm.plan.and.billing.sql.resource.OMPBBILLINGCONTACT;
import com.tlc.crm.plan.and.billing.sql.resource.OMPBCANCELLATIONFEEDBACK;
import com.tlc.crm.plan.and.billing.sql.resource.OMPBCANCELLATIONREASON;
import com.tlc.crm.plan.and.billing.sql.resource.OMPBINVOICE;
import com.tlc.crm.plan.and.billing.sql.resource.OMPBORGANIZATIONDISCOUNTS;
import com.tlc.crm.plan.and.billing.sql.resource.OMPBORGANIZATIONSUBSCRIPTIONS;
import com.tlc.crm.plan.and.billing.sql.resource.OMPBORGSPECIFICDISCOUNTS;
import com.tlc.crm.plan.and.billing.sql.resource.OMPBPLANTRANSACTION;
import com.tlc.crm.plan.and.billing.sql.resource.OMPBTRANSACTION;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.entity.manager.TwozoApplicationManager;

import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.l10n.temporal.model.Period;
import com.tlc.payment.gateway.models.Address;
import com.tlc.payment.gateway.models.Payer;
import com.tlc.payment.gateway.models.BillingAmount;
import com.tlc.payment.gateway.models.PaymentStatus;

import com.tlc.plans.and.billings.manager.SubscriptionPlanManager;
import com.tlc.plans.and.billings.models.BillingCycle;
import com.tlc.plans.and.billings.models.CancellationFeedbacks;
import com.tlc.plans.and.billings.models.CancellationReason;
import com.tlc.plans.and.billings.models.Discount;
import com.tlc.plans.and.billings.models.Discounts;
import com.tlc.plans.and.billings.models.PlanType;
import com.tlc.plans.and.billings.models.PricingScheme;
import com.tlc.plans.and.billings.models.SubscriptionPlan;
import com.tlc.plans.and.billings.sql.resource.MPBCANCELLATIONFEEDBACK;
import com.tlc.plans.and.billings.sql.resource.MPBDISCOUNTS;
import com.tlc.plans.and.billings.sql.resource.MPBPRICINGSCHEMEDISCOUNTS;
import com.tlc.plans.and.billings.sql.resource.MPBPRICINGSCHEMES;
import com.tlc.plans.and.billings.sql.resource.MPBSUBSCRIPTIONPLANS;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.util.TableUtils;

import java.time.Instant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tlc.crm.common.models.CommonFields.CRM;

/**
 * Manager class for handling CRUD, retrieval, and mapping of organization subscription plans,
 * including associated discounts and contract states.
 *
 * <p>
 * This manager coordinates with {@link SubscriptionPlanManager} and the appropriate
 * organization data stores to support lifecycle operations of plans (creation, update, fetch)
 * with proper consideration for discounts and contract state filters.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class OrgPlanManagerImpl implements OrgPlanManager {

    private final SubscriptionPlanManager planManager;
    private final AddressManager addressManager;
    private final TwozoApplicationManager twozoApplicationManager;
    private final AdminDataStore adminDataStore;

    private OrgPlanManagerImpl() {
        planManager = SubscriptionPlanManager.getInstance();
        addressManager = AddressManager.getInstance();
        twozoApplicationManager = TwozoApplicationManager.getInstance();
        adminDataStore = DataStoreProvider.getAdminDataStore();
    }

    private static final class INSTANCE_HOLDER {
        private static final OrgPlanManagerImpl PLAN_MANAGER = new OrgPlanManagerImpl();
    }

    /**
     * Returns the singleton instance of OrgPlanManager.
     *
     * @return OrgPlanManager instance
     */
    public static OrgPlanManagerImpl getInstance() {
        return INSTANCE_HOLDER.PLAN_MANAGER;
    }

    /**
     * Provides the OrgDataStore appropriate to the given organization ID.
     *
     * @param orgId Organization primary key
     * @return OrgDataStore for the specified organization
     */
    private OrgDataStore getOrgDataStore(final long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * Retrieves the OMPBORGANIZATIONSUBSCRIPTIONS table representation.
     *
     * @return Table instance for organization plans
     */
    private Table getOrgPlanTable() {
        return Table.get(OMPBORGANIZATIONSUBSCRIPTIONS.TABLE);
    }

    /**
     * Retrieves the OMPBORGANIZATIONDISCOUNTS table representation.
     *
     * @return Table instance for plan discounts
     */
    private Table getOrgPlanDiscountTable() {
        return Table.get(OMPBORGANIZATIONDISCOUNTS.TABLE);
    }

    /**
     * Retrieves the OMPBBILLINGCONTACT table representation.
     *
     * @return Table instance for billing contact
     */
    private Table getBillingContactTable() {
        return Table.get(OMPBBILLINGCONTACT.TABLE);
    }

    /**
     * Retrieves the OMPBPLANTRANSACTION table representation.
     *
     * @return Table instance for plan transaction
     */
    private Table getPlanTransactionTable() {
        return Table.get(OMPBPLANTRANSACTION.TABLE);
    }

    /**
     * Retrieves the OMPBTRANSACTION table representation.
     *
     * @return Table instance for transaction
     */
    private Table getTransactionTable() {
        return Table.get(OMPBTRANSACTION.TABLE);
    }

    /**
     * Retrieves the OMPBINVOICE table representation.
     *
     * @return Table instance for invoice
     */
    private Table getInvoiceTable() {
        return Table.get(OMPBINVOICE.TABLE);
    }

    /**
     * Retrieves the OMPBCANCELLATIONFEEDBACK table representation.
     *
     * @return Table instance for cancellation feedback.
     */
    private Table getCancellationFeedbackTable() {
        return Table.get(OMPBCANCELLATIONFEEDBACK.TABLE);
    }

    /**
     * Retrieves the OMPBCANCELLATIONREASON table representation.
     *
     * @return Table instance for cancellation reason.
     */
    private Table getCancellationReasonTable() {
        return Table.get(OMPBCANCELLATIONREASON.TABLE);
    }

    /**
     * Retrieves the OMPBORGSPECIFICDISCOUNTS table representation.
     *
     * @return Table instance for org specific discounts.
     */
    private Table getOrgSpecificDiscountTable() {
        return Table.get(OMPBORGSPECIFICDISCOUNTS.TABLE);
    }

    /**
     * Fetches the current plan ID for the given organization and PlanType.
     *
     * @param orgId    Organization ID
     * @param planType PlanType filter
     * @return Plan primary ids currently active for the organization, matching the type.
     */
    @Override
    public Collection<OrgSubscriptionPlan> getCurrentPlans(final long orgId, final PlanType planType) {
        final Table orgPlanTable = getOrgPlanTable();
        final Table planTable = planManager.getPlanTable();
        final Table pricingSchemeTable = planManager.getPricingSchemeTable();
        final Collection<Column> planTableColumns = new ArrayList<>(TableUtils.getAllColumns(orgPlanTable));

        planTableColumns.add(planTable.getColumn(MPBSUBSCRIPTIONPLANS.ORDER));
        planTableColumns.add(planTable.getColumn(MPBSUBSCRIPTIONPLANS.PLAN_TYPE));
        planTableColumns.add(pricingSchemeTable.getColumn(MPBPRICINGSCHEMES.VALUE));
        final DataContainer currentPlanContainer = getCurrentPlanContainer(orgId, planTableColumns, planType);

        return currentPlanContainer.getRows(orgPlanTable).map(currentPlanRow -> {
            final Long planId = currentPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.PLAN_ID);
            final Long pricingSchemeId = currentPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.PRICING_SCHEME_ID);
            final Row planRow = currentPlanContainer.getRow(planTable, planId);
            final Row pricingSchemeRow = currentPlanContainer.getRow(pricingSchemeTable, pricingSchemeId);
            final SubscriptionPlan subscriptionPlan = new SubscriptionPlan();
            final PricingScheme pricingScheme = new PricingScheme();

            pricingScheme.setId(pricingSchemeId);
            pricingScheme.setValue(pricingSchemeRow.get(MPBPRICINGSCHEMES.VALUE));
            subscriptionPlan.setPricingScheme(pricingScheme);
            subscriptionPlan.setOrder(planRow.get(MPBSUBSCRIPTIONPLANS.ORDER));
            subscriptionPlan.setPlanType(PlanType.get(planRow.get(MPBSUBSCRIPTIONPLANS.PLAN_TYPE)));
            subscriptionPlan.setId(planId);

            return buildOrgSubscription(subscriptionPlan, currentPlanRow, currentPlanContainer);
        }).collect(Collectors.toSet());
    }

    /**
     * Updates existing organization subscriptions and appropriately syncs discounts.
     *
     * @param orgSubscriptions Updated subscription set for the organization
     */
    @Override
    public void upsert(final OrgSubscriptions orgSubscriptions) {
        final DataContainer targetContainer = DataContainer.create();
        final Long orgId = orgSubscriptions.orgId();
        final Collection<OrgSubscriptionPlan> orgPlans = orgSubscriptions.getOrgPlans();
        final Collection<Long> orgPlanIds = orgPlans.stream().map(OrgSubscriptionPlan::id).filter(Objects::nonNull).collect(Collectors.toSet());
        final DataContainer sourceContainer = orgPlanIds.isEmpty() ? DataContainer.create() : fetchOldDataContainer(orgId, orgPlanIds);
        final Map<Long, Action> planEventMap = new HashMap<>();
        final OrgDataStore orgDataStore = getOrgDataStore(orgId);
        final BillingDetails billingDetails = orgSubscriptions.getBillingDetails();
        final Collection<Long> discountIds = fetchOrgSpecificDiscountIds(orgId, Discount.Type.CUSTOM_DISCOUNT, Discount.Type.CANCELLATION_DISCOUNT);

        if (!discountIds.isEmpty()) {
            final Table orgSpecificDiscountTable = getOrgSpecificDiscountTable();
            final DataContainer orgSpecificDiscountContainer = orgDataStore.get(orgSpecificDiscountTable,
                    new WhereClause(Criteria.in(orgSpecificDiscountTable.getColumn(OMPBORGSPECIFICDISCOUNTS.DISCOUNT_ID), discountIds)));

            sourceContainer.append(orgSpecificDiscountContainer);
        }

        for (final OrgSubscriptionPlan orgSubscriptionPlan : orgPlans) {
            final boolean isCreateEvent = Objects.isNull(orgSubscriptionPlan.id());
            final Action action = isCreateEvent ? Action.CREATE : Action.UPDATE;

            loadOrgPlanRowIntoContainer(isCreateEvent, orgSubscriptionPlan, sourceContainer, targetContainer);
            planEventMap.put(orgSubscriptionPlan.id(), action);
        }

        if (Objects.nonNull(billingDetails)) {
            final DataContainer billingContactContainer = fetchBillingContactContainer(orgId, billingDetails.getContact());

            loadBillingDetailsIntoContainer(planEventMap, orgSubscriptions, billingContactContainer, targetContainer);
        }
        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * Upgrades the current plan into new plan.
     *
     * @param orgId         Represents the organisation ID.
     * @param transactionId Represents the transaction ID of the new plan.
     */
    @Override
    public void activatePlan(final Long orgId, final Long transactionId) {
        final Table planTable = planManager.getPlanTable();
        final Table pricingSchemeTable = planManager.getPricingSchemeTable();
        final Table planTransactionTable = getPlanTransactionTable();
        final Table transactionTable = getTransactionTable();
        final Table orgPlanTable = getOrgPlanTable();
        final Table invoiceTable = getInvoiceTable();
        final Collection<Column> selectColumns = new ArrayList<>();
        final DataContainer targetContainer = DataContainer.create();

        final Column transactionPkColumn = transactionTable.getPKColumn();
        final Column orgPlanIdColumn = planTransactionTable.getColumn(OMPBPLANTRANSACTION.ORG_PLAN_ID);
        final Column transactionIdColumn = planTransactionTable.getColumn(OMPBPLANTRANSACTION.TRANSACTION_ID);
        final Column planOrderColumn = planTable.getColumn(MPBSUBSCRIPTIONPLANS.ORDER);
        final Column planTypeColumn = planTable.getColumn(MPBSUBSCRIPTIONPLANS.PLAN_TYPE);

        final WhereClause whereClause = new WhereClause(Criteria.eq(transactionPkColumn, transactionId));
        final SelectQuery selectQuery = SelectQuery.get(orgPlanTable);

        selectQuery.addSelectClause(orgPlanTable, transactionTable, invoiceTable);
        selectQuery.addSelectClause(planTypeColumn);
        selectQuery.addSelectClause(planOrderColumn);
        selectQuery.addSelectClause(pricingSchemeTable.getColumn(MPBPRICINGSCHEMES.BILLING_CYCLE));
        selectQuery.addSelectClause(planTransactionTable.getColumn(OMPBPLANTRANSACTION.PLAN_META));
        selectQuery.addSelectClause(orgPlanIdColumn);
        selectQuery.addSelectClause(transactionIdColumn);
        selectQuery.addJoinClause(new JoinClause(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.PLAN_ID), planTable.getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.PRICING_SCHEME_ID), pricingSchemeTable.getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(orgPlanTable.getPKColumn(), orgPlanIdColumn));
        selectQuery.addJoinClause(new JoinClause(transactionIdColumn, transactionPkColumn));
        selectQuery.addJoinClause(new JoinClause(transactionPkColumn, invoiceTable.getColumn(OMPBINVOICE.TRANSACTION_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(whereClause);
        selectQuery.addOrderByClause(new OrderByClause(planTypeColumn));
        selectColumns.add(planTypeColumn);
        selectColumns.add(planOrderColumn);
        selectColumns.addAll(TableUtils.getAllColumns(orgPlanTable));
        final DataContainer currentPlanContainer = getCurrentPlanContainer(orgId, selectColumns, PlanType.values());
        final DataContainer upcomingPlanContainer = getOrgDataStore(orgId).get(selectQuery);
        final Stream<Row> orgPlanRows = upcomingPlanContainer.getRows(orgPlanTable);
        final Row transactionRow = upcomingPlanContainer.getRow(transactionTable, transactionId);

        transactionRow.set(OMPBTRANSACTION.STATUS, PaymentStatus.SUCCESS.getId());
        transactionRow.set(OMPBTRANSACTION.COMPLETION_STATUS, true);
        targetContainer.updateRow(transactionRow);

        orgPlanRows.forEach(orgPlanRow -> {
            final Long planId = orgPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.PLAN_ID);
            final Long pricingSchemeId = orgPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.PRICING_SCHEME_ID);
            final Row pricingSchemeRow = upcomingPlanContainer.getRow(pricingSchemeTable, pricingSchemeId);
            final Row planTransactionRow = upcomingPlanContainer.getRow(planTransactionTable,
                    new WhereClause(Criteria.eq(orgPlanIdColumn, orgPlanRow.getPKValue())));

            final JsonObject planMetaJson = Json.object((String) planTransactionRow.get(OMPBPLANTRANSACTION.PLAN_META));

            if (Objects.equals(Action.UPDATE.name(), planMetaJson.getString(SubscriptionPlanFields.ACTION))) {
                final int planQuantity = (int) orgPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.QUANTITY) + planMetaJson.getInt(SubscriptionPlanFields.QUANTITY);

                orgPlanRow.set(OMPBORGANIZATIONSUBSCRIPTIONS.QUANTITY, planQuantity);
            } else {
                final Row planRow = upcomingPlanContainer.getRow(planTable, planId);
                final short planOrder = planRow.get(MPBSUBSCRIPTIONPLANS.ORDER);
                final PlanType planType = PlanType.get(planRow.get(MPBSUBSCRIPTIONPLANS.PLAN_TYPE));

                final Consumer<PlanContractState> newPlanContractStateUpdater = contractState -> {
                    orgPlanRow.set(OMPBORGANIZATIONSUBSCRIPTIONS.CONTRACT_STATE, contractState.getId());

                    switch (contractState) {
                        case PRESENT -> {
                            if (PlanType.ORDINARY == planType) {
                                final DateTime startTime = new DateTime(Instant.now().toEpochMilli());
                                final BillingCycle billingCycle = BillingCycle.get(pricingSchemeRow.get(MPBPRICINGSCHEMES.BILLING_CYCLE));

                                orgPlanRow.set(OMPBORGANIZATIONSUBSCRIPTIONS.START_TIME, startTime);
                                orgPlanRow.set(OMPBORGANIZATIONSUBSCRIPTIONS.EXPIRATION_TIME, PlanExpirationCalculator.calculate(startTime, billingCycle));
                            } else {
                                final Row ordinaryPlanRow = upcomingPlanContainer.getRow(orgPlanTable, planMetaJson.getLong(SubscriptionPlanFields.ORDINARY_PLAN_ID));

                                orgPlanRow.set(OMPBORGANIZATIONSUBSCRIPTIONS.START_TIME, ordinaryPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.START_TIME));
                                orgPlanRow.set(OMPBORGANIZATIONSUBSCRIPTIONS.EXPIRATION_TIME, ordinaryPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.EXPIRATION_TIME));
                            }
                        }
                    }
                };
                orgPlanRow.set(OMPBORGANIZATIONSUBSCRIPTIONS.SUBSCRIPTION_STATUS, SubscriptionStatus.PAID.getId());

                if (!loadOldPlanCancellationIntoContainer(planId, planOrder, planType, newPlanContractStateUpdater, currentPlanContainer, targetContainer)) {
                    newPlanContractStateUpdater.accept(PlanContractState.PRESENT);
                }
            }
            targetContainer.updateRow(orgPlanRow);
        });
        final Invoice invoice = buildInvoice(orgId, transactionId, Invoice.Status.PAID);

        loadInvoiceDetailsIntoContainer(invoice, upcomingPlanContainer, targetContainer);
        getOrgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * Updates the payment transaction status for a given organization and transaction ID.
     *
     * @param orgId         the unique identifier of the organization to which the transaction belongs
     * @param transactionId the unique identifier of the transaction to update
     * @param paymentStatus the new payment status to set for the transaction (e.g., SUCCESS, FAILED, PENDING)
     */
    @Override
    public void updateTransactionStatus(final long orgId, final Long transactionId, final PaymentStatus paymentStatus) {
        final Table transactionTable = getTransactionTable();
        final Table invoiceTable = getInvoiceTable();
        final SelectQuery selectQuery = SelectQuery.get(transactionTable);

        selectQuery.addJoinClause(new JoinClause(transactionTable.getPKColumn(), invoiceTable.getColumn(OMPBINVOICE.TRANSACTION_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(transactionTable.getPKColumn(), transactionId)));
        final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);
        final Row transactionRow = dataContainer.getRow(transactionTable);
        final Invoice invoice = buildInvoice(orgId, transactionId, Invoice.Status.getByPaymentStatus(paymentStatus));
        final DataContainer targetContainer = DataContainer.create();

        transactionRow.set(OMPBTRANSACTION.STATUS, paymentStatus.getId());
        transactionRow.set(OMPBTRANSACTION.COMPLETION_STATUS, true);
        targetContainer.updateRow(transactionRow);
        loadInvoiceDetailsIntoContainer(invoice, dataContainer, targetContainer);
        getOrgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * Fetches a single organization subscription plan by its unique ID.
     *
     * @param orgId Organization ID
     * @param id    Subscription plan ID
     * @return OrgSubscriptionPlan instance or null if not found
     */
    @Override
    public OrgSubscriptionPlan get(final long orgId, final long id) {
        return get(orgId, Set.of(id)).stream().findAny().orElseGet(() -> null);
    }

    /**
     * Fetches multiple organization subscription plans by IDs.
     *
     * @param orgId Organization ID
     * @param ids   Set of subscription plan IDs
     * @return Collection of OrgSubscriptionPlans
     */
    @Override
    public Collection<OrgSubscriptionPlan> get(final long orgId, final Collection<Long> ids) {
        final Table orgPlanTable = getOrgPlanTable();
        final WhereClause whereClause = new WhereClause(Criteria.in(orgPlanTable.getPKColumn(), ids));
        final DataContainer dataContainer = getOrgDataStore(orgId).get(buildSelectQuery(whereClause));
        final Collection<SubscriptionPlan> subscriptionPlans = planManager.buildSubscriptionPlansFromDc(dataContainer);

        return subscriptionPlans.stream().map(subscriptionPlan -> buildOrgSubscription(subscriptionPlan, dataContainer)).toList();
    }

    /**
     * Fetches all organization plans matching contract state and optional plan types.
     *
     * @param orgId             Organization ID
     * @param planContractState Contract state filter (CURRENT, UPCOMING, etc.)
     * @param planTypes         Optional filter for PlanType
     * @return OrgSubscriptions object containing filtered plans
     */
    @Override
    public OrgSubscriptions get(final long orgId, final PlanContractState planContractState, final PlanType... planTypes) {
        final Table planTable = planManager.getPlanTable();
        final Table orgPlanTable = getOrgPlanTable();

        final WhereClause contractStateCriteria = new WhereClause(Criteria.eq(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.CONTRACT_STATE), planContractState.getId()));
        final WhereClause criteria;

        if (Objects.isNull(planTypes) || 0 == planTypes.length) {
            criteria = contractStateCriteria;
        } else {
            final Collection<Short> planTypeIds = Arrays.stream(planTypes).map(PlanType::getId).collect(Collectors.toSet());

            criteria = contractStateCriteria.and(Criteria.in(planTable.getColumn(MPBSUBSCRIPTIONPLANS.PLAN_TYPE), planTypeIds));
        }

        final OrgDataStore orgDataStore = getOrgDataStore(orgId);
        final DataContainer dataContainer = orgDataStore.get(buildSelectQuery(criteria));
        final Collection<SubscriptionPlan> subscriptionPlans = planManager.buildSubscriptionPlansFromDc(dataContainer);
        final OrgSubscriptions orgSubscriptions = new OrgSubscriptions();

        for (final SubscriptionPlan subscriptionPlan : subscriptionPlans) {
            orgSubscriptions.addOrgPlans(buildOrgSubscription(subscriptionPlan, dataContainer));
        }
        return orgSubscriptions;
    }

    /**
     * Adds a new billing contact for the organization, represented by the supplied Payer object.
     * Use this method when onboarding a customer or setting up billing information for the first time.
     *
     * @param userId         User ID
     * @param billingContact the Payer model containing the billing contact details to be stored
     */
    @Override
    public void addBillingContact(final Long userId, final Payer billingContact) {
        final Table billingContactTable = getBillingContactTable();
        final Row billingContactRow = new Row(billingContactTable);
        final DataContainer targetContainer = DataContainer.create();

        loadBillingContactIntoContainer(userId, billingContactRow, billingContact, null, targetContainer);
        getOrgDataStore(billingContact.orgId()).commitChanges(targetContainer);
    }

    /**
     * Updates the existing billing contact information for the organization with new details provided in the Payer object.
     * Use this method to change, replace, or correct previously saved billing contact data.
     *
     * @param userId         User ID
     * @param billingContact the Payer model containing the updated billing contact information
     */
    @Override
    public void updateBillingContact(final Long userId, final Payer billingContact) {
        final Table billingContactTable = getBillingContactTable();
        final Long orgId = billingContact.orgId();
        final DataContainer sourceContainer = fetchBillingContactContainer(orgId, billingContact);
        final Row billingContactRow = sourceContainer.getRow(billingContactTable);
        final DataContainer targetContainer = DataContainer.create();

        if (Objects.isNull(billingContactRow)) {
            throw ErrorCode.get(SubscriptionErrorCodes.BILLING_CONTACT_NOT_EXISTS);
        }
        loadBillingContactIntoContainer(userId, billingContactRow, billingContact, sourceContainer, targetContainer);
        getOrgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * Retrieves the billing contact information (payer details) for the specified organization.
     *
     * @param orgId the unique identifier of the organization whose billing contact is to be fetched
     * @return a Payer object containing the billing contact information for the given organization
     */
    @Override
    public Payer getBillingContact(final long orgId) {
        final DataContainer dataContainer = fetchBillingContactContainer(orgId, null);
        final Optional<Row> optionalContactRow = Optional.ofNullable(dataContainer.getRow(getBillingContactTable()));

        return optionalContactRow.map(contactRow -> convertRowIntoBillingContact(contactRow, dataContainer)).orElse(null);
    }

    /**
     * Retrieves a summary of invoices for the specified organization.
     *
     * @param orgId the unique identifier of the organization whose invoice summary is to be fetched
     * @return an {@link InvoiceSummary} object representing the summarized invoice data for the organization
     */
    @Override
    public InvoiceSummary getInvoiceSummary(final long orgId) {
        final Table invoiceTable = getInvoiceTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(invoiceTable.getColumn(OMPBINVOICE.STATUS), Invoice.Status.PAID.getId()));
        final OrderByClause orderByClause = new OrderByClause(invoiceTable.getColumn(OMPBINVOICE.BILLING_DATE), OrderByClause.OrderType.DESCENDING);
        final DataContainer dataContainer = fetchInvoiceContainer(orgId, whereClause, orderByClause, new LimitClause(1));
        final Optional<Row> optionalInvoiceRow = Optional.ofNullable(dataContainer.getRow(invoiceTable));

        return optionalInvoiceRow.map(invoiceRow -> {
            final Invoice invoice = convertRowIntoInvoice(invoiceRow, dataContainer);
            final InvoiceSummary invoiceSummary = new InvoiceSummary();
            final Table orgPlanTable = getOrgPlanTable();
            final Table pricingSchemeTable = planManager.getPricingSchemeTable();

            final Column billingCycleColumn = pricingSchemeTable.getColumn(MPBPRICINGSCHEMES.BILLING_CYCLE);
            final Column valueColumn = pricingSchemeTable.getColumn(MPBPRICINGSCHEMES.VALUE);
            final Column startTimeColumn = orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.START_TIME);
            final Column statusColumn = orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.SUBSCRIPTION_STATUS);
            final Column pricingSchemeIdColumn = orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.PRICING_SCHEME_ID);
            final Column quantityColumn = orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.QUANTITY);

            final DataContainer currentPlanContainer = getCurrentPlanContainer(orgId, Set.of(startTimeColumn, statusColumn,
                    billingCycleColumn, pricingSchemeIdColumn, quantityColumn, valueColumn), PlanType.ORDINARY);

            final Row orgPlanRow = currentPlanContainer.getRow(orgPlanTable);
            final Row pricingSchemeRow = currentPlanContainer.getRow(pricingSchemeTable, (long) orgPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.PRICING_SCHEME_ID));
            final SubscriptionStatus subscriptionStatus = SubscriptionStatus.get(orgPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.SUBSCRIPTION_STATUS));

            if (SubscriptionStatus.CANCELLED != subscriptionStatus) {
                final Invoice nextEstimatedInvoice = new Invoice();
                final DateTime nextBillingDate = PlanExpirationCalculator.calculate(orgPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.START_TIME),
                        BillingCycle.get(pricingSchemeRow.get(MPBPRICINGSCHEMES.BILLING_CYCLE)));

                nextEstimatedInvoice.setBillingDate(new Date(nextBillingDate.getUtcTimeStamp()));
                nextEstimatedInvoice.setAmount((double) pricingSchemeRow.get(MPBPRICINGSCHEMES.VALUE) *
                        (int) orgPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.QUANTITY));
                nextEstimatedInvoice.setCurrency(Currency.USD);
                invoiceSummary.setNextEstimatedInvoice(nextEstimatedInvoice);
            }
            invoiceSummary.setLastInvoice(invoice);

            return invoiceSummary;
        }).orElse(null);
    }

    /**
     * Retrieves a list of invoices for the specified organization, sorted and limited according to provided configurations.
     *
     * @param orgId       the unique identifier of the organization whose invoices are to be listed
     * @param sortConfig  configuration specifying the sorting order and criteria for the invoice list
     * @param limitConfig configuration specifying pagination or record-limit settings for the invoice list
     * @return an Invoices object containing the list of invoices matching the given criteria for the organization
     */
    @Override
    public Invoices getInvoiceList(final long orgId, final SortConfig sortConfig, final LimitConfig limitConfig) {
        final OrderByClause orderByClause = new OrderByClause(fetchColumn(sortConfig.fieldName()), sortConfig.type().getOrderType());
        final DataContainer dataContainer = fetchInvoiceContainer(orgId, null, orderByClause, limitConfig.getLimitClause());
        final Stream<Row> invoiceRows = dataContainer.getRows(getInvoiceTable());
        final Invoices invoices = new Invoices();

        invoiceRows.forEach(invoiceRow -> invoices.add(convertRowIntoInvoice(invoiceRow, dataContainer)));
        return invoices;
    }

    /**
     * Retrieves the total count of invoices associated with the specified organization.
     *
     * @param orgId the unique identifier of the organization whose invoices are to be counted
     * @return the total number of invoices for the given organization
     */
    @Override
    public long getTotalInvoiceCount(final long orgId) {
        final Table invoiceTable = getInvoiceTable();

        return getOrgDataStore(orgId).get(CountQuery.get(invoiceTable));
    }

    /**
     * Checks if billing details exist for the specified organization.
     *
     * @param orgId the unique identifier of the organization
     * @return true if billing details are present for the organization; false otherwise
     */
    @Override
    public boolean billingDetailsExists(final long orgId) {
        return 0 < getOrgDataStore(orgId).get(CountQuery.get(getBillingContactTable()));
    }

    /**
     * Fetches the collection of {@link Discount} for the organization.
     *
     * @param orgId         the unique identifier of the organization.
     * @param planType      represents the type of the plan.
     * @param discountTypes represents the type of the discount.
     * @return Collection of {@link Discount}.
     */
    @Override
    public Map<Long, Discounts> getOrgDiscounts(final long orgId, final PlanType planType, final Discount.Type... discountTypes) {
        final Map<Long, Discounts> discountsMap = new HashMap<>();
        final Collection<Long> discountIds = fetchOrgSpecificDiscountIds(orgId, discountTypes);

        if (!discountIds.isEmpty()) {
            final Table pricingSchemeDiscountTable = planManager.getPricingSchemeDiscountTable();
            final Table pricingSchemeTable = planManager.getPricingSchemeTable();
            final Table planTable = planManager.getPlanTable();
            final Table discountTable = planManager.getDiscountTable();
            final Table orgSpecificDiscountTable = getOrgSpecificDiscountTable();

            final SelectQuery selectQuery = SelectQuery.get(discountTable);
            final Column discountPkColumn = discountTable.getPKColumn();

            selectQuery.addSelectClause(discountTable, pricingSchemeDiscountTable, orgSpecificDiscountTable);
            selectQuery.addJoinClause(new JoinClause(discountPkColumn, pricingSchemeDiscountTable.getColumn(MPBPRICINGSCHEMEDISCOUNTS.DISCOUNT_ID)));
            selectQuery.addJoinClause(new JoinClause(pricingSchemeDiscountTable.getColumn(MPBPRICINGSCHEMEDISCOUNTS.PRICING_SCHEME_ID), pricingSchemeTable.getPKColumn()));
            selectQuery.addJoinClause(new JoinClause(pricingSchemeTable.getColumn(MPBPRICINGSCHEMES.PLAN_ID), planTable.getPKColumn()));
            selectQuery.addJoinClause(new JoinClause(discountPkColumn, orgSpecificDiscountTable.getColumn(OMPBORGSPECIFICDISCOUNTS.DISCOUNT_ID), JoinClause.JoinType.LEFT));

            selectQuery.setWhereClause(new WhereClause(Criteria.in(discountPkColumn, discountIds))
                    .and(Criteria.eq(planTable.getColumn(MPBSUBSCRIPTIONPLANS.PLAN_TYPE), planType.getId())));
            final DataContainer dataContainer = getOrgDataStore(orgId).get(selectQuery);
            final Stream<Row> pricingSchemeDiscountRows = dataContainer.getRows(pricingSchemeDiscountTable);

            pricingSchemeDiscountRows.forEach(pricingSchemeDiscountRow -> {
                final Long pricingSchemeId = pricingSchemeDiscountRow.get(MPBPRICINGSCHEMEDISCOUNTS.PRICING_SCHEME_ID);
                final Long discountId = pricingSchemeDiscountRow.get(MPBPRICINGSCHEMEDISCOUNTS.DISCOUNT_ID);
                final Row discountRow = dataContainer.getRow(discountTable, discountId);
                final Discounts discounts = discountsMap.computeIfAbsent(pricingSchemeId, priceId -> new Discounts());
                final Discount discount = planManager.convertRowIntoDiscount(discountRow);
                final Row orgSpecificDiscountRow = dataContainer.getRow(orgSpecificDiscountTable,
                        new WhereClause(Criteria.eq(orgSpecificDiscountTable.getColumn(OMPBORGSPECIFICDISCOUNTS.DISCOUNT_ID), discountId)));

                if (Objects.nonNull(orgSpecificDiscountRow)) {
                    discount.setStartTime(orgSpecificDiscountRow.get(OMPBORGSPECIFICDISCOUNTS.START_DATE));
                    discount.setExpirationTime(orgSpecificDiscountRow.get(OMPBORGSPECIFICDISCOUNTS.EXPIRATION_DATE));
                }
                discounts.add(discount);
            });
        }

        return discountsMap;
    }

    /**
     * Checks whether the cancellation discount already exists for the given organization.
     *
     * @param orgId      The unique identifier of the organization.
     * @param discountId The unique identifier of the discount.
     * @return Cancellation discount existence.
     */
    @Override
    public boolean isDiscountAlreadyActivated(final long orgId, final long discountId) {
        final Table orgSpecificDiscountTable = getOrgSpecificDiscountTable();
        final CountQuery countQuery = CountQuery.get(orgSpecificDiscountTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(orgSpecificDiscountTable.getColumn(OMPBORGSPECIFICDISCOUNTS.DISCOUNT_ID), discountId)));
        return 0 != getOrgDataStore(orgId).get(countQuery);
    }

    /**
     * Stores the cancellation discount for the organization.
     *
     * @param orgId      The unique identifier of the organization.
     * @param discountId The unique identifier of the discount.
     */
    @Override
    public void activateDiscount(final long orgId, final long discountId) {
        final Period expirationPeriod = planManager.getDiscount(discountId).getExpirationPeriod();
        final Table orgSpecificDiscountTable = getOrgSpecificDiscountTable();
        final Row orgSpecificDiscountRow = new Row(orgSpecificDiscountTable);

        orgSpecificDiscountRow.setOrgId(orgId);
        orgSpecificDiscountRow.set(OMPBORGSPECIFICDISCOUNTS.DISCOUNT_ID, discountId);

        if (Objects.nonNull(expirationPeriod)) {
            orgSpecificDiscountRow.set(OMPBORGSPECIFICDISCOUNTS.START_DATE, TemporalServiceProvider.get().getUtcDateTime());
            orgSpecificDiscountRow.set(OMPBORGSPECIFICDISCOUNTS.EXPIRATION_DATE, expirationPeriod.calculateUtcTime());
        }
        getOrgDataStore(orgId).addRow(orgSpecificDiscountRow);
    }


    /**
     * Cancels the organization subscriptions.
     *
     * @param planCancellationData plans cancellation data of the organization
     */
    @Override
    public void cancelPlan(final PlanCancellationData planCancellationData) {
        final Long orgId = planCancellationData.orgId();
        final Long userId = planCancellationData.getUserId();
        final Table orgPlanTable = getOrgPlanTable();
        final OrgDataStore orgDataStore = getOrgDataStore(orgId);
        final Row orgPlanRow = orgDataStore.get(orgPlanTable, planCancellationData.id());
        final DataContainer targetContainer = DataContainer.create();

        loadCancellationFeedbacksIntoContainer(orgId, userId, planCancellationData.getCancellationFeedbacks(), targetContainer);
        loadCancellationReasonsIntoContainer(orgId, userId, planCancellationData.getCancellationReasons(), targetContainer);

        orgPlanRow.set(OMPBORGANIZATIONSUBSCRIPTIONS.SUBSCRIPTION_STATUS, SubscriptionStatus.CANCELLED.getId());
        targetContainer.updateRow(orgPlanRow);

        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * Reactivates the cancelled plan of an organization.
     *
     * @param orgId  The unique identifier of an organization.
     * @param planId The unique identifier of the plan.
     */
    @Override
    public void reactivatePlan(final long orgId, final long planId) {
        final Table orgPlanTable = getOrgPlanTable();
        final UpdateQuery updateQuery = UpdateQuery.get(orgPlanTable);

        updateQuery.addUpdateColumn(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.SUBSCRIPTION_STATUS), SubscriptionStatus.PAID.getId());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(orgPlanTable.getPKColumn(), planId)));
        getOrgDataStore(orgId).update(updateQuery);
    }

    /**
     * Checks if the given plan is the current active plan for the specified organization.
     *
     * @param orgId  the unique identifier of the organization
     * @param planId the unique identifier of the plan to check
     * @return true if the plan is the current active plan for the organization, false otherwise
     */
    @Override
    public boolean isCurrentPlan(final long orgId, final long planId) {
        final Table orgPlanTable = getOrgPlanTable();
        final CountQuery countQuery = CountQuery.get(orgPlanTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(orgPlanTable.getPKColumn(), planId))
                .and(Criteria.eq(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.CONTRACT_STATE), PlanContractState.PRESENT.getId())));
        return 0 != getOrgDataStore(orgId).get(countQuery);
    }

    /**
     * Checks if the given plan is expired or not.
     *
     * @param orgId  the unique identifier of the organization
     * @param planId the unique identifier of the plan to check
     * @return true if the plan is expired, false otherwise
     */
    @Override
    public boolean isPlanExpired(final long orgId, final long planId) {
        final Table orgPlanTable = getOrgPlanTable();
        final CountQuery countQuery = CountQuery.get(orgPlanTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(orgPlanTable.getPKColumn(), planId))
                .and(Criteria.gt(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.EXPIRATION_TIME), Instant.now().toEpochMilli())));
        return 0 == getOrgDataStore(orgId).get(countQuery);
    }

    /**
     * Checks if the given plan is paid plan or not.
     *
     * @param orgId  the unique identifier of the organization
     * @param planId the unique identifier of the plan to check
     * @return true if the plan is paid plan, false otherwise
     */
    @Override
    public boolean isPaidPlan(final Long orgId, final Long planId) {
        final Table orgPlanTable = getOrgPlanTable();
        final CountQuery countQuery = CountQuery.get(orgPlanTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(orgPlanTable.getPKColumn(), planId))
                .and(Criteria.eq(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.SUBSCRIPTION_STATUS), SubscriptionStatus.PAID.getId())));
        return 0 != getOrgDataStore(orgId).get(countQuery);
    }

    /**
     * Retrieves the count of organization subscription plans that require expiration updates.
     *
     * @return A count of {@link OrgSubscriptions} representing plans that are expired or about to expire and need processing.
     */
    @Override
    public long getPlansRequiringExpirationUpdateCount() {
        final Table orgPlanTable = getOrgPlanTable();
        final WhereClause whereClause = new WhereClause(Criteria.in(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.SUBSCRIPTION_STATUS), SubscriptionStatus.getActiveStatusIds()))
                .and(Criteria.le(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.EXPIRATION_TIME), Instant.now().toEpochMilli()));

        final CountQuery countQuery = CountQuery.get(orgPlanTable);

        countQuery.setWhereClause(whereClause);
        return adminDataStore.get(countQuery);
    }

    /**
     * Retrieves the collection of organization subscription plans that require expiration updates.
     *
     * @param start Starting index of the {@link OrgSubscriptions}.
     * @param limit Count to fetch the {@link OrgSubscriptions}.
     * @return A {@link Collection} of {@link OrgSubscriptions} representing plans that are expired or about to expire and need processing.
     */
    @Override
    public Collection<OrgSubscriptions> getPlansRequiringExpirationUpdate(final int start, final int limit) {
        final Table orgPlanTable = getOrgPlanTable();
        final WhereClause whereClause = new WhereClause(Criteria.in(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.SUBSCRIPTION_STATUS), SubscriptionStatus.getActiveStatusIds()))
                .and(Criteria.le(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.EXPIRATION_TIME), Instant.now().toEpochMilli()));

        return getPlansWithBasicDetails(whereClause, new LimitClause(start, limit));
    }

    /**
     * Retrieves the upcoming subscription plans for the given set of organization IDs.
     *
     * @param orgIds a Collection of organization IDs for which to fetch upcoming plans.
     * @return A {@link Collection} of {@link OrgSubscriptions} representing plans that are scheduled to become active for the given organizations.
     */
    @Override
    public Collection<OrgSubscriptions> getUpcomingPlans(final Collection<Long> orgIds) {
        if (20 < orgIds.size()) {
            throw ErrorCode.get(SubscriptionErrorCodes.RETRIEVAL_LIMIT_EXCEEDED, "Please provide the org IDs count below 20");
        }
        final Table orgPlanTable = getOrgPlanTable();
        final WhereClause whereClause = new WhereClause(Criteria.in(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.ORG_ID), orgIds))
                .and(Criteria.eq(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.SUBSCRIPTION_STATUS), SubscriptionStatus.PAID.getId()))
                .and(Criteria.eq(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.CONTRACT_STATE), PlanContractState.UPCOMING.getId()));

        return getPlansWithBasicDetails(whereClause, null);
    }

    /**
     * Switches subscription plans based on the provided plan activation data.
     * <p>
     * This performs plan expiration and activation in a batch operation using the details
     * in each {@link PlanActivationRequest} instance.
     *
     * @param planActivationRequests a {@link Collection} of {@link PlanActivationRequest} objects specifying old and upcoming plan associations by organization.
     */
    @Override
    public void switchPlan(final Collection<PlanActivationRequest> planActivationRequests) {
        final Table orgPlanTable = getOrgPlanTable();
        final Column orgPlanPKColumn = orgPlanTable.getPKColumn();
        final Column orgIdColumn = orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.ORG_ID);
        final Column contractStateColumn = orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.CONTRACT_STATE);
        final Column statusColumn = orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.SUBSCRIPTION_STATUS);
        final Column startTimeColumn = orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.START_TIME);
        final Column expirationTimeColumn = orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.EXPIRATION_TIME);
        final List<UpdateQuery> updateQueries = new ArrayList<>();

        for (final PlanActivationRequest planActivationRequest : planActivationRequests) {
            final UpdateQuery expiredPlanUpdateQuery = UpdateQuery.get(orgPlanTable);
            final Long orgId = planActivationRequest.getOrgId();
            final Long upcomingPlanId = planActivationRequest.getUpcomingPlanId();
            final OrgSubscriptionPlan oldSubscriptionPlan = planActivationRequest.getOldSubscriptionPlan();
            final WhereClause orgIdCriteria = new WhereClause(Criteria.eq(orgIdColumn, orgId));
            final SubscriptionStatus expirationStatus = SubscriptionStatus.TRIAL == oldSubscriptionPlan.getStatus()
                    ? SubscriptionStatus.TRIAL_EXPIRED : SubscriptionStatus.EXPIRED;

            expiredPlanUpdateQuery.addUpdateColumn(statusColumn, expirationStatus.getId());
            expiredPlanUpdateQuery.setWhereClause(orgIdCriteria.and(Criteria.eq(orgPlanPKColumn, oldSubscriptionPlan.id())));

            if (Objects.nonNull(upcomingPlanId)) {
                final UpdateQuery upcomingPlanUpdateQuery = UpdateQuery.get(orgPlanTable);
                final DateTime startTime = new DateTime(Instant.now().toEpochMilli());
                final DateTime expirationTime = PlanExpirationCalculator.calculate(startTime, planActivationRequest.getUpcomingPlanBillingCycle());

                expiredPlanUpdateQuery.addUpdateColumn(contractStateColumn, PlanContractState.PREVIOUS.getId());
                upcomingPlanUpdateQuery.addUpdateColumn(contractStateColumn, PlanContractState.PRESENT.getId());
                upcomingPlanUpdateQuery.addUpdateColumn(startTimeColumn, startTime);
                upcomingPlanUpdateQuery.addUpdateColumn(expirationTimeColumn, expirationTime);
                upcomingPlanUpdateQuery.setWhereClause(orgIdCriteria.and(Criteria.eq(orgPlanPKColumn, upcomingPlanId)));

                updateQueries.add(upcomingPlanUpdateQuery);
            }
            updateQueries.add(expiredPlanUpdateQuery);
        }
        adminDataStore.update(updateQueries);
    }

    /**
     * Fetches the {@link PaymentStatus} by using transaction ID.
     *
     * @param transactionId Represents the transaction ID.
     * @return {@link PaymentStatus}.
     */
    @Override
    public Optional<PaymentStatus> getPaymentStatus(final long transactionId) {
        final Table transactionTable = getTransactionTable();
        final Optional<Row> optionalTransactionRow = getTransactionRow(transactionId, Set.of(transactionTable.getColumn(OMPBTRANSACTION.STATUS)));

        return optionalTransactionRow.map(transactionRow -> PaymentStatus.get(transactionRow.get(OMPBTRANSACTION.STATUS)));
    }

    /**
     * Aggregates usage statistics from the database for specific discount categories.
     * <p>
     * This method performs a grouped count operation on the discount application
     * records, filtered by the provided {@code discountTypes}. It is primarily
     * used to track the popularity or reach of specific promotional campaigns.
     * </p>
     *
     * @param orgId         Represents the organization ID.
     * @param discountTypes One or more {@link Discount.Type} constants to include in the aggregation. If empty, no results are returned.
     * @return A {@link Map} mapping each Discount ID to its total number of recorded applications in the database.
     */
    @Override
    public Map<Long, Integer> getDiscountsAppliedCount(final long orgId, final Discount.Type... discountTypes) {
        final Table discountTable = planManager.getDiscountTable();
        final Table orgPlanDiscountTable = getOrgPlanDiscountTable();
        final Column discountIdColumn = orgPlanDiscountTable.getColumn(OMPBORGANIZATIONDISCOUNTS.DISCOUNT_ID);
        final SelectQuery selectQuery = SelectQuery.get(orgPlanDiscountTable);

        selectQuery.addSelectClause(discountIdColumn);
        selectQuery.addSelectClause(orgPlanDiscountTable.getPKColumn().countWithDistinct());
        selectQuery.addJoinClause(new JoinClause(discountIdColumn, discountTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(discountTable.getColumn(MPBDISCOUNTS.TYPE), Set.of(discountTypes))));
        selectQuery.addGroupByClause(new GroupByClause(discountIdColumn));
        final Stream<Row> orgPlanDiscountRows = getOrgDataStore(orgId).get(selectQuery).getRows(orgPlanDiscountTable);

        return orgPlanDiscountRows.collect(Collectors.toMap(
                orgPlanDiscountRow -> orgPlanDiscountRow.get(OMPBORGANIZATIONDISCOUNTS.DISCOUNT_ID),
                orgPlanDiscountRow -> orgPlanDiscountRow.get(OMPBORGANIZATIONDISCOUNTS.ID)));
    }

    /**
     * Retrieves the total number of times a specific discount has been applied within a particular organization.
     * <p>
     * This method queries the database to aggregate usage records, filtering by
     * both the organization and the discount ID. It is typically used for
     * validating usage limits or generating organization-level reports.
     * </p>
     *
     * @param orgId      The unique identifier of the organization.
     * @param discountId The unique identifier of the discount to check.
     * @return The total count of applications for the specified discount
     * within the given organization. Returns 0 if no records are found.
     */
    @Override
    public int getDiscountAppliedCount(final long orgId, final long discountId) {
        final Table orgPlanDiscountTable = getOrgPlanDiscountTable();
        final Column discountIdColumn = orgPlanDiscountTable.getColumn(OMPBORGANIZATIONDISCOUNTS.DISCOUNT_ID);
        final SelectQuery selectQuery = SelectQuery.get(orgPlanDiscountTable);

        selectQuery.addSelectClause(orgPlanDiscountTable.getPKColumn().countWithDistinct());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(discountIdColumn, discountId)));
        final Optional<Row> optionalOrgPlanDiscountRow = Optional.ofNullable(getOrgDataStore(orgId).get(selectQuery).getRow(orgPlanDiscountTable));

        return optionalOrgPlanDiscountRow.map(orgPlanDiscountRow ->
                Integer.valueOf(orgPlanDiscountRow.get(OMPBORGANIZATIONDISCOUNTS.ID).toString())).orElse(0);
    }

    /**
     * Retrieves the activation (usage) status for a collection of discounts within a specific organization.
     * <p>
     * This method performs a bulk lookup to determine if the specified discounts
     * have already been activated or applied to records for the given organization.
     * It is designed to optimize performance by reducing database round-trips
     * when checking multiple discount IDs simultaneously.
     * </p>
     *
     * @param orgId       The unique identifier of the organization.
     * @param discountIds A {@link Collection} of unique discount identifiers to check.
     * @return A {@link Map} where the key is the Discount ID (Long) and the value
     * is a Boolean indicating the activation status ({@code true} if activated/applied,
     * {@code false} otherwise).
     */
    @Override
    public Map<Long, Boolean> getDiscountsActivationStatus(final long orgId, final Collection<Long> discountIds) {
        final Table orgSpecificDiscountTable = getOrgSpecificDiscountTable();
        final Column discountIdColumn = orgSpecificDiscountTable.getColumn(OMPBORGSPECIFICDISCOUNTS.DISCOUNT_ID);
        final SelectQuery selectQuery = SelectQuery.get(orgSpecificDiscountTable);

        selectQuery.addSelectClause(discountIdColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(discountIdColumn, discountIds)));
        final Stream<Row> orgPlanDiscountRows = getOrgDataStore(orgId).get(selectQuery).getRows(orgSpecificDiscountTable);
        final Collection<Long> activatedDiscountIds = orgPlanDiscountRows.map(orgPlanDiscountRow ->
                (Long) orgPlanDiscountRow.get(OMPBORGSPECIFICDISCOUNTS.DISCOUNT_ID)).collect(Collectors.toSet());

        return discountIds.stream().collect(Collectors.toMap(Function.identity(), activatedDiscountIds::contains));
    }

    /**
     * Checks whether a specific transaction has reached a terminal completion state.
     * <p>
     * This method is used to determine if a transaction (identified by the PayU Transaction ID,
     * e.g., {@code 5703_3753_1764837621514}) has been successfully processed by the system.
     * It serves as a binary check primarily for UI flow control, such as determining
     * whether to stop a loading spinner or proceed to a success page.
     * </p>
     *
     * @param transactionId The unique identifier of the transaction to check.
     * @return {@code true} if the transaction is confirmed as completed; {@code false} if the transaction is pending.
     */
    @Override
    public Boolean getTransactionCompletionStatus(final long transactionId) {
        final Table transactionTable = getTransactionTable();
        final Optional<Row> optionalTransactionRow = getTransactionRow(transactionId, Set.of(transactionTable.getColumn(OMPBTRANSACTION.COMPLETION_STATUS)));

        return optionalTransactionRow.map(transactionRow -> (boolean) transactionRow.get(OMPBTRANSACTION.COMPLETION_STATUS)).orElse(null);
    }

    /**
     * Retrieves a specific row from the transaction table based on its primary key and a requested set of columns.
     * <p>
     * This method constructs a {@link SelectQuery} dynamically to fetch only the necessary data points,
     * optimizing database performance by avoiding 'SELECT *' operations. It is designed to be used by
     * higher-level resolvers or services that need granular access to transaction records.
     * </p>
     *
     * @param transactionId The unique identifier (Primary Key) of the transaction to retrieve.
     * @param columns       A {@link Set} of {@link Column} objects representing the fields to be included in the selection.
     * @return An {@link Optional} containing the {@link Row} if a record exists for the given ID; otherwise, {@link Optional#empty()}.
     */
    private Optional<Row> getTransactionRow(final long transactionId, final Set<Column> columns) {
        final Table transactionTable = getTransactionTable();
        final SelectQuery selectQuery = SelectQuery.get(transactionTable);

        selectQuery.addSelectClause(columns);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(transactionTable.getPKColumn(), transactionId)));
        return Optional.ofNullable(adminDataStore.get(selectQuery).getRow(transactionTable));
    }

    /**
     * Retrieves organization subscription plans with their basic details including plan type and billing cycle.
     * <p>
     * This method constructs and executes a SQL query joining organization subscription,
     * subscription plan, and pricing scheme tables, using the provided whereClause to filter results.
     * It aggregates the resulting rows into OrgSubscriptions objects grouped by organization ID,
     * each containing a collection of {@link OrgSubscriptionPlan} with detailed subscription and pricing information.
     *
     * @param whereClause the conditions to apply to filter the subscriptions in the query
     * @param limitClause Limit to apply for fetching the records
     * @return a {@link Collection} of {@link OrgSubscriptions} objects with basic subscription plan details based on the filter
     */
    private Collection<OrgSubscriptions> getPlansWithBasicDetails(final WhereClause whereClause, final LimitClause limitClause) {
        final Table orgPlanTable = getOrgPlanTable();
        final Table planTable = planManager.getPlanTable();
        final Table pricingSchemeTable = planManager.getPricingSchemeTable();
        final Column planIdColumn = orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.PLAN_ID);
        final Column pricingSchemeIdColumn = orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.PRICING_SCHEME_ID);
        final SelectQuery selectQuery = SelectQuery.get(orgPlanTable);

        selectQuery.addSelectClause(planIdColumn);
        selectQuery.addSelectClause(pricingSchemeIdColumn);
        selectQuery.addSelectClause(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.ORG_ID));
        selectQuery.addSelectClause(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.SUBSCRIPTION_STATUS));
        selectQuery.addSelectClause(planTable.getColumn(MPBSUBSCRIPTIONPLANS.PLAN_TYPE));
        selectQuery.addSelectClause(pricingSchemeTable.getColumn(MPBPRICINGSCHEMES.BILLING_CYCLE));

        selectQuery.addJoinClause(new JoinClause(planIdColumn, planTable.getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(pricingSchemeIdColumn, pricingSchemeTable.getPKColumn()));

        selectQuery.setWhereClause(whereClause);
        selectQuery.setLimitClause(limitClause);

        final DataContainer dataContainer = adminDataStore.get(selectQuery);
        final Stream<Row> orgPlanRows = dataContainer.getRows(orgPlanTable);
        final Map<Long, OrgSubscriptions> orgSubscriptionsMap = new HashMap<>();

        orgPlanRows.forEach(orgPlanRow -> {
            final Long orgId = orgPlanRow.getOrgId();
            final long planId = orgPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.PLAN_ID);
            final long pricingSchemeId = orgPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.PRICING_SCHEME_ID);
            final Row planRow = dataContainer.getRow(planTable, planId);
            final Row pricingSchemeRow = dataContainer.getRow(pricingSchemeTable, pricingSchemeId);
            final OrgSubscriptions orgSubscriptions = orgSubscriptionsMap.computeIfAbsent(orgId, organisationId -> {
                final OrgSubscriptions orgSubscriptionPlans = new OrgSubscriptions();

                orgSubscriptionPlans.setOrgId(orgId);
                return orgSubscriptionPlans;
            });

            final OrgSubscriptionPlan orgSubscriptionPlan = new OrgSubscriptionPlan();
            final SubscriptionPlan subscriptionPlan = new SubscriptionPlan();
            final PricingScheme pricingScheme = new PricingScheme();

            pricingScheme.setId(pricingSchemeId);
            pricingScheme.setBillingCycle(BillingCycle.get(pricingSchemeRow.get(MPBPRICINGSCHEMES.BILLING_CYCLE)));

            subscriptionPlan.setId(planId);
            subscriptionPlan.setPlanType(PlanType.get(planRow.get(MPBSUBSCRIPTIONPLANS.PLAN_TYPE)));
            subscriptionPlan.setPricingScheme(pricingScheme);

            orgSubscriptionPlan.setId(orgPlanRow.getPKValue());
            orgSubscriptionPlan.setOrgId(orgId);
            orgSubscriptionPlan.setStatus(SubscriptionStatus.get(orgPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.SUBSCRIPTION_STATUS)));
            orgSubscriptionPlan.setPlan(subscriptionPlan);

            orgSubscriptions.addOrgPlans(orgSubscriptionPlan);
        });

        return orgSubscriptionsMap.values();
    }

    /**
     * Loads the collection of {@link CancellationReason} objects into the target {@link DataContainer} for the specified organization and user.
     * For each {@link CancellationReason}, a new Row is created with organization ID, user ID, and reason ID, then added to the container.
     *
     * @param orgId               the organization ID
     * @param userId              the user ID associated with the cancellation reasons
     * @param cancellationReasons collection of {@link CancellationReason} objects to load
     * @param targetContainer     the {@link DataContainer} that will hold the loaded rows
     */
    private void loadCancellationReasonsIntoContainer(final Long orgId, final Long userId,
                                                      final Collection<CancellationReason> cancellationReasons,
                                                      final DataContainer targetContainer) {
        final Table reasonTable = getCancellationReasonTable();

        for (final CancellationReason cancellationReason : cancellationReasons) {
            final Row reasonRow = new Row(reasonTable);

            reasonRow.setOrgId(orgId);
            reasonRow.set(OMPBCANCELLATIONREASON.USER_ID, userId);
            reasonRow.set(OMPBCANCELLATIONREASON.REASON_ID, cancellationReason.getId());
            targetContainer.addNewRow(reasonRow);
        }
    }

    /**
     * Loads the {@link CancellationFeedbacks} into the target {@link DataContainer} for the specified organization and user.
     * It creates a feedback Row with application ID and feedback JSON string, then associates it with an org feedback Row recording the user and org details.
     *
     * @param orgId                 the organization ID
     * @param userId                the user ID associated with the feedback
     * @param cancellationFeedbacks the {@link CancellationFeedbacks} object containing feedback data
     * @param targetContainer       the {@link DataContainer} to receive the feedback rows
     */
    private void loadCancellationFeedbacksIntoContainer(final Long orgId, final Long userId,
                                                        final CancellationFeedbacks cancellationFeedbacks,
                                                        final DataContainer targetContainer) {
        final Table feedbackTable = planManager.getCancellationFeedbackTable();
        final Table orgFeedbackTable = getCancellationFeedbackTable();
        final Row feedbackRow = new Row(feedbackTable);
        final Row orgFeedbackRow = new Row(orgFeedbackTable);

        feedbackRow.set(MPBCANCELLATIONFEEDBACK.APPLICATION_ID, twozoApplicationManager.getByName(CRM).getId());
        feedbackRow.set(MPBCANCELLATIONFEEDBACK.FEEDBACK, cancellationFeedbacks.toJsonString());
        targetContainer.addNewRow(feedbackRow);

        orgFeedbackRow.set(OMPBCANCELLATIONFEEDBACK.FEEDBACK_ID, feedbackRow.getPKValue());
        orgFeedbackRow.set(OMPBCANCELLATIONFEEDBACK.USER_ID, userId);
        orgFeedbackRow.setOrgId(orgId);
        targetContainer.addNewRow(orgFeedbackRow);
    }

    /**
     * Fetches the set of discount IDs available for the given organization and discount types,
     * applying limits and organization-specific logic for both custom and cancellation discounts.
     *
     * @param orgId         The organization ID for which discounts should be fetched
     * @param discountTypes Varargs list of discount types to fetch. Can include custom and/or cancellation discount types.
     * @return Set of discount IDs eligible for the organization across requested discount types
     */
    private Collection<Long> fetchOrgSpecificDiscountIds(final long orgId, final Discount.Type... discountTypes) {
        final Table discountTable = planManager.getDiscountTable();
        final Table orgSpecificDiscountTable = getOrgSpecificDiscountTable();

        final Column discountPkColumn = discountTable.getPKColumn();
        final Column maxApplyLimitColumn = discountTable.getColumn(MPBDISCOUNTS.MAX_APPLY_LIMIT);
        final Column orgSpecificDiscountIdColumn = orgSpecificDiscountTable.getColumn(OMPBORGSPECIFICDISCOUNTS.DISCOUNT_ID);

        final SelectQuery discountQuery = SelectQuery.get(discountTable);
        final SelectQuery orgSpecificDiscountLimitQuery = SelectQuery.get(orgSpecificDiscountTable);
        final Collection<Short> discountTypeIds = Arrays.stream(discountTypes).filter(Objects::nonNull).map(Discount.Type::getId).toList();

        orgSpecificDiscountLimitQuery.addSelectClause(orgSpecificDiscountTable.getColumn(OMPBORGSPECIFICDISCOUNTS.USED_COUNT));
        orgSpecificDiscountLimitQuery.setWhereClause(new WhereClause(Criteria.eq(orgSpecificDiscountIdColumn, discountPkColumn)));
        final WhereClause discountCriteria = new WhereClause(Criteria.in(discountTable.getColumn(MPBDISCOUNTS.TYPE), discountTypeIds))
                .and(Criteria.gt(orgSpecificDiscountTable.getColumn(OMPBORGSPECIFICDISCOUNTS.EXPIRATION_DATE), TemporalServiceProvider.get().getUtcDateTime()))
                .and(new WhereClause(Criteria.eq(maxApplyLimitColumn, null))
                        .or(Criteria.gt(maxApplyLimitColumn, orgSpecificDiscountLimitQuery)));

        discountQuery.addSelectClause(discountTable.getPKColumn());
        discountQuery.addJoinClause(new JoinClause(discountPkColumn, orgSpecificDiscountIdColumn));
        discountQuery.setWhereClause(discountCriteria);
        final DataContainer dataContainer = getOrgDataStore(orgId).get(discountQuery);

        return dataContainer.getRows(discountTable).map(Row::getPKValue).collect(Collectors.toSet());
    }

    /**
     * Cancels old plans of the given type and propagates their cancelled status into the target data container.
     * The cancellation is done by setting the SUBSCRIPTION_STATUS to CANCELLED in the target container.
     *
     * @param planType             the subscription plan type for which old subscriptions should be cancelled
     * @param currentPlanContainer the container holding current plan rows (source of truth for what's active now)
     * @param targetContainer      the container where the cancellations should be written/applied
     */
    private boolean loadOldPlanCancellationIntoContainer(final Long planId, final short planOrder, final PlanType planType,
                                                         final Consumer<PlanContractState> newPlanContractStateUpdater,
                                                         final DataContainer currentPlanContainer, final DataContainer targetContainer) {
        final Table planTable = planManager.getPlanTable();
        final Table orgPlanTable = getOrgPlanTable();
        final WhereClause whereClause;

        if (PlanType.ORDINARY == planType) {
            final WhereClause planTypeCriteria = new WhereClause(Criteria.eq(planTable.getColumn(MPBSUBSCRIPTIONPLANS.PLAN_TYPE), planType.getId()));
            final Stream<Row> planRows = currentPlanContainer.getRows(planTable, planTypeCriteria);
            final Collection<Long> planIds = planRows.map(Row::getPKValue).collect(Collectors.toSet());

            whereClause = planIds.isEmpty() ? null : new WhereClause(Criteria.in(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.PLAN_ID), planIds));
        } else {
            whereClause = new WhereClause(Criteria.eq(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.PLAN_ID), planId));
        }

        if (Objects.nonNull(whereClause)) {
            final Optional<Row> optionalCurrentPlanRow = Optional.ofNullable(currentPlanContainer.getRow(orgPlanTable, whereClause));

            return optionalCurrentPlanRow.map(currentPlanRow -> {
                final DateTime expirationTime = currentPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.EXPIRATION_TIME);
                final Row planRow = currentPlanContainer.getRow(planTable, (long) currentPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.PLAN_ID));
                final Instant currentTime = Instant.now();
                final Instant expiryTime = Instant.ofEpochMilli(expirationTime.getUtcTimeStamp());
                final SubscriptionStatus subscriptionStatus = SubscriptionStatus.get(currentPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.SUBSCRIPTION_STATUS));
                final boolean isAdvancedPlan = planOrder > (short) planRow.get(MPBSUBSCRIPTIONPLANS.ORDER);
                final boolean isTrial = SubscriptionStatus.TRIAL == subscriptionStatus;
                final boolean isExpired = currentTime.isAfter(expiryTime);

                if (isTrial || isExpired || isAdvancedPlan) {
                    if (!isTrial && !isExpired) {
                        currentPlanRow.set(OMPBORGANIZATIONSUBSCRIPTIONS.SUBSCRIPTION_STATUS, SubscriptionStatus.CANCELLED.getId());
                    }
                    currentPlanRow.set(OMPBORGANIZATIONSUBSCRIPTIONS.CONTRACT_STATE, PlanContractState.PREVIOUS.getId());
                    newPlanContractStateUpdater.accept(PlanContractState.PRESENT);
                    targetContainer.updateRow(currentPlanRow);
                }
                return true;
            }).orElse(false);
        }
        return false;
    }

    /**
     * Constructs and initializes a new Invoice object with provided organization, transaction,
     * invoice ID, and status values. Also sets the current billing date and a placeholder for the PDF URL.
     *
     * @param orgId         the unique identifier of the organization associated with the invoice
     * @param transactionId the transaction identifier linked to this invoice
     * @param status        the invoice status (e.g., PAID, FAILED, CLOSED) to set
     * @return a populated Invoice instance with supplied fields
     */
    private Invoice buildInvoice(final Long orgId, final Long transactionId, final Invoice.Status status) {
        final Invoice invoice = new Invoice();

        invoice.setOrgId(orgId);
        invoice.setTransactionId(transactionId);
        invoice.setStatus(status);
        invoice.setBillingDate(new Date(Instant.now().toEpochMilli()));
        invoice.setPdfUrl("PDF");//TODO Replace generated invoice PDF URL.

        return invoice;
    }

    /**
     * Loads details from the provided Invoice object into a target DataContainer for storage or processing.
     * Maps Invoice fields to a new Row in the invoice data table and inserts it into the container.
     *
     * @param invoice         the Invoice instance whose details should be loaded
     * @param targetContainer the container (data structure) to which the invoice details are added
     */
    private void loadInvoiceDetailsIntoContainer(final Invoice invoice, final DataContainer planContainer,
                                                 final DataContainer targetContainer) {
        final Long transactionId = invoice.getTransactionId();
        final Table invoiceTable = getInvoiceTable();
        final Row invoiceRow = 0 == planContainer.size(invoiceTable) ? new Row(invoiceTable) : planContainer.getRow(invoiceTable);

        if (invoiceRow.isNewRow()) {
            invoiceRow.setOrgId(invoice.getOrgId());
            invoiceRow.set(OMPBINVOICE.TRANSACTION_ID, transactionId);
        }
        invoiceRow.set(OMPBINVOICE.BILLING_DATE, invoice.getBillingDate());
        invoiceRow.set(OMPBINVOICE.STATUS, invoice.getStatus().getId());
        invoiceRow.set(OMPBINVOICE.PDF_URL, invoice.getPdfUrl());
        targetContainer.updateRow(invoiceRow);
    }

    /**
     * Fetches the current plan row for the given organization and PlanType.
     *
     * @param orgId     Organization ID
     * @param planTypes PlanTypes filter
     * @return Plan row currently active for the organization, matching the type.
     */
    private DataContainer getCurrentPlanContainer(final long orgId, final Collection<Column> selectColumns, final PlanType... planTypes) {
        final Table planTable = planManager.getPlanTable();
        final Table pricingSchemeTable = planManager.getPricingSchemeTable();
        final Table orgPlanTable = getOrgPlanTable();

        final SelectQuery selectQuery = SelectQuery.get(orgPlanTable);
        final Collection<Short> planTypeIds = Arrays.stream(planTypes).map(PlanType::getId).collect(Collectors.toSet());

        selectQuery.addSelectClause(selectColumns);
        selectQuery.addJoinClause(new JoinClause(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.PLAN_ID), planTable.getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.PRICING_SCHEME_ID), pricingSchemeTable.getPKColumn()));
        selectQuery.setWhereClause(
                new WhereClause(Criteria.in(planTable.getColumn(MPBSUBSCRIPTIONPLANS.PLAN_TYPE), planTypeIds))
                        .and(Criteria.eq(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.CONTRACT_STATE), PlanContractState.PRESENT.getId()))
        );

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * Builds a SelectQuery with the correct joins and select clauses for main plan retrieval.
     *
     * @param whereClause Filtering criteria
     * @return Constructed SelectQuery
     */
    private SelectQuery buildSelectQuery(final WhereClause whereClause) {
        final Table planTable = planManager.getPlanTable();
        final Table orgPlanTable = getOrgPlanTable();
        final Table pricingSchemeTable = planManager.getPricingSchemeTable();
        final Table orgPlanDiscountTable = getOrgPlanDiscountTable();
        final Table discountTable = planManager.getDiscountTable();

        final Column planPkColumn = planTable.getPKColumn();
        final Column planOrderColumn = planTable.getColumn(MPBSUBSCRIPTIONPLANS.ORDER);

        final SelectQuery selectQuery = SelectQuery.get(orgPlanTable);

        selectQuery.addSelectClause(planTable.getColumn(MPBSUBSCRIPTIONPLANS.NAME));
        selectQuery.addSelectClause(planOrderColumn);
        selectQuery.addSelectClause(planTable.getColumn(MPBSUBSCRIPTIONPLANS.PLAN_TYPE));
        selectQuery.addSelectClause(planTable.getColumn(MPBSUBSCRIPTIONPLANS.SIMPLIFIED_DESCRIPTION));
        selectQuery.addSelectClause(orgPlanTable, pricingSchemeTable, orgPlanDiscountTable, discountTable);

        selectQuery.addJoinClause(new JoinClause(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.PLAN_ID), planPkColumn));
        selectQuery.addJoinClause(new JoinClause(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.PRICING_SCHEME_ID), pricingSchemeTable.getPKColumn()));
        selectQuery.addJoinClause(new JoinClause(orgPlanTable.getPKColumn(), orgPlanDiscountTable.getColumn(OMPBORGANIZATIONDISCOUNTS.ORG_PLAN_ID), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(orgPlanDiscountTable.getColumn(OMPBORGANIZATIONDISCOUNTS.DISCOUNT_ID), discountTable.getPKColumn(), JoinClause.JoinType.LEFT));

        selectQuery.setWhereClause(whereClause);
        selectQuery.addOrderByClause(new OrderByClause(planOrderColumn));

        return selectQuery;
    }

    /**
     * Loads or updates a OrgSubscriptionPlan and maps its discounts into a DataContainer.
     *
     * @param isCreateEvent       Whether this is for creation (true) or update (false)
     * @param orgSubscriptionPlan Plan data from the request
     * @param sourceContainer     Source DataContainer for update context, or null
     * @param targetContainer     Target DataContainer to be committed
     */
    private void loadOrgPlanRowIntoContainer(final boolean isCreateEvent, final OrgSubscriptionPlan orgSubscriptionPlan,
                                             final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table orgPlanTable = getOrgPlanTable();
        final SubscriptionPlan plan = orgSubscriptionPlan.getPlan();
        final Long orgId = orgSubscriptionPlan.orgId();
        final Long planId = orgSubscriptionPlan.id();
        final Row orgPlanRow = isCreateEvent ? new Row(orgPlanTable) : sourceContainer.getRow(orgPlanTable, planId);

        if (Objects.isNull(orgPlanRow)) {
            throw ErrorCode.get(SubscriptionErrorCodes.PLAN_NOT_FOUND);
        }
        final Long orgPlanId = orgPlanRow.getPKValue();
        final Collection<Discount> discounts = orgSubscriptionPlan.getDiscounts();
        final SubscriptionStatus status = orgSubscriptionPlan.getStatus();
        final PlanContractState contractState = orgSubscriptionPlan.getContractState();
        final DateTime startTime = orgSubscriptionPlan.getStartTime();
        final DateTime expirationTime = orgSubscriptionPlan.getExpirationTime();

        if (isCreateEvent) {
            orgPlanRow.setOrgId(orgId);
            orgSubscriptionPlan.setId(orgPlanId);
            orgPlanRow.set(OMPBORGANIZATIONSUBSCRIPTIONS.QUANTITY, orgSubscriptionPlan.getQuantity());
        }
        orgPlanRow.set(OMPBORGANIZATIONSUBSCRIPTIONS.PLAN_ID, plan.getId());
        orgPlanRow.set(OMPBORGANIZATIONSUBSCRIPTIONS.PRICING_SCHEME_ID, plan.getPricingScheme().getId());

        if (Objects.nonNull(status)) {
            orgPlanRow.set(OMPBORGANIZATIONSUBSCRIPTIONS.SUBSCRIPTION_STATUS, status.getId());
        }

        if (Objects.nonNull(startTime)) {
            orgPlanRow.set(OMPBORGANIZATIONSUBSCRIPTIONS.START_TIME, startTime);
        }

        if (Objects.nonNull(expirationTime)) {
            orgPlanRow.set(OMPBORGANIZATIONSUBSCRIPTIONS.EXPIRATION_TIME, expirationTime);
        }

        if (Objects.nonNull(contractState)) {
            orgPlanRow.set(OMPBORGANIZATIONSUBSCRIPTIONS.CONTRACT_STATE, contractState.getId());
        }

        if (Objects.nonNull(discounts) && !discounts.isEmpty()) {
            loadOrgPlanDiscountRowIntoContainer(isCreateEvent, orgId, orgPlanId, discounts, sourceContainer, targetContainer);
        }

        targetContainer.updateRow(orgPlanRow);

        if (!isCreateEvent && Objects.nonNull(discounts)) {
            deleteUnmappedDiscounts(orgPlanId, discounts, sourceContainer, targetContainer);
        }
    }

    /**
     * Removes discount associations missing from the update for a given OrgPlan.
     *
     * @param orgPlanId       Org Plan ID context
     * @param discounts       Discounts currently present
     * @param sourceContainer Source DataContainer
     * @param targetContainer Target DataContainer
     */
    private void deleteUnmappedDiscounts(final long orgPlanId, final Collection<Discount> discounts,
                                         final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table orgPlanDiscountTable = getOrgPlanDiscountTable();
        final Collection<Long> discountIds = discounts.stream().map(Discount::getId).collect(Collectors.toSet());

        final WhereClause unmappedPlanDiscountCriteria =
                new WhereClause(Criteria.eq(orgPlanDiscountTable.getColumn(OMPBORGANIZATIONDISCOUNTS.ORG_PLAN_ID), orgPlanId))
                        .and(Criteria.notIn(orgPlanDiscountTable.getColumn(OMPBORGANIZATIONDISCOUNTS.DISCOUNT_ID), discountIds));

        sourceContainer.getRows(orgPlanDiscountTable, unmappedPlanDiscountCriteria).forEach(targetContainer::deleteRow);
    }

    /**
     * Loads discount rows into the DataContainer for an organization subscription plan.
     *
     * @param isCreateEvent   Whether operation is create or update
     * @param orgId           Organization ID context
     * @param orgPlanId       Org Plan ID context
     * @param discounts       List of discount objects
     * @param sourceContainer Source DataContainer for updates
     * @param targetContainer Target DataContainer for commit
     */
    private void loadOrgPlanDiscountRowIntoContainer(final boolean isCreateEvent, final long orgId, final long orgPlanId,
                                                     final Collection<Discount> discounts, final DataContainer sourceContainer,
                                                     final DataContainer targetContainer) {
        final Table orgPlanDiscountTable = getOrgPlanDiscountTable();
        final Table orgSpecificDiscountTable = getOrgSpecificDiscountTable();
        final WhereClause[] orgPlanDiscountCriteria = {null};
        final Supplier<WhereClause> orgPlanDiscountCriteriaProvider = () -> {
            if (Objects.isNull(orgPlanDiscountCriteria[0])) {
                return orgPlanDiscountCriteria[0] = new WhereClause(Criteria.eq(orgPlanDiscountTable.getColumn(OMPBORGANIZATIONDISCOUNTS.ORG_PLAN_ID), orgPlanId));
            }
            return orgPlanDiscountCriteria[0];
        };

        for (final Discount discount : discounts) {
            final Long discountId = discount.getId();
            final Row orgPlanDiscountRow;

            if (isCreateEvent) {
                orgPlanDiscountRow = new Row(orgPlanDiscountTable);
            } else {
                final WhereClause planDiscountCriteria = orgPlanDiscountCriteriaProvider.get()
                        .and(Criteria.eq(orgPlanDiscountTable.getColumn(OMPBORGANIZATIONDISCOUNTS.DISCOUNT_ID), discountId));
                final Row oldOrgPlanDiscountRow = sourceContainer.getRow(orgPlanDiscountTable, planDiscountCriteria);

                orgPlanDiscountRow = Objects.isNull(oldOrgPlanDiscountRow) ? new Row(orgPlanDiscountTable) : oldOrgPlanDiscountRow;
            }

            if (orgPlanDiscountRow.isNewRow()) {
                orgPlanDiscountRow.setOrgId(orgId);
                orgPlanDiscountRow.set(OMPBORGANIZATIONDISCOUNTS.ORG_PLAN_ID, orgPlanId);
                orgPlanDiscountRow.set(OMPBORGANIZATIONDISCOUNTS.DISCOUNT_ID, discountId);

                targetContainer.addNewRow(orgPlanDiscountRow);
            }
            final Optional<Row> optionalOrgSpecificDiscountRow = Optional.ofNullable(sourceContainer.getRow(orgSpecificDiscountTable,
                    new WhereClause(Criteria.eq(orgSpecificDiscountTable.getColumn(OMPBORGSPECIFICDISCOUNTS.DISCOUNT_ID), discountId))));

            optionalOrgSpecificDiscountRow.ifPresent(orgSpecificDiscountRow -> {
                final int usedCount = orgSpecificDiscountRow.get(OMPBORGSPECIFICDISCOUNTS.USED_COUNT);

                orgSpecificDiscountRow.set(OMPBORGSPECIFICDISCOUNTS.USED_COUNT, usedCount + 1);
                targetContainer.updateRow(orgSpecificDiscountRow);
            });
        }
    }

    /**
     * Builds a domain model OrgSubscriptionPlan by aggregating plan, discounts, status, and related fields from a DataContainer.
     *
     * @param plan          SubscriptionPlan domain object
     * @param dataContainer DataContainer to lookup related rows
     * @return Fully populated OrgSubscriptionPlan domain object
     */
    private OrgSubscriptionPlan buildOrgSubscription(final SubscriptionPlan plan, final DataContainer dataContainer) {
        final Table orgPlanTable = getOrgPlanTable();
        final Row orgPlanRow = dataContainer.getRow(orgPlanTable,
                new WhereClause(Criteria.eq(orgPlanTable.getColumn(OMPBORGANIZATIONSUBSCRIPTIONS.PLAN_ID), plan.getId())));

        return buildOrgSubscription(plan, orgPlanRow, dataContainer);
    }

    /**
     * Builds a domain model OrgSubscriptionPlan by aggregating plan, discounts, status, and related fields from a DataContainer.
     *
     * @param plan          {@link SubscriptionPlan} domain object
     * @param dataContainer {@link DataContainer} to lookup related rows
     * @return Fully populated {@link OrgSubscriptionPlan} domain object
     */
    private OrgSubscriptionPlan buildOrgSubscription(final SubscriptionPlan plan, final Row orgPlanRow, final DataContainer dataContainer) {
        final OrgSubscriptionPlan orgSubscriptionPlan = new OrgSubscriptionPlan();
        final Table discountTable = planManager.getDiscountTable();
        final Table orgPlanDiscountTable = getOrgPlanDiscountTable();

        final Stream<Row> orgPlanDiscountRows = dataContainer.getRows(orgPlanDiscountTable,
                new WhereClause(Criteria.eq(orgPlanDiscountTable.getColumn(OMPBORGANIZATIONDISCOUNTS.ORG_PLAN_ID), orgPlanRow.getPKValue())));

        orgSubscriptionPlan.setId(orgPlanRow.getPKValue());
        orgSubscriptionPlan.setPlan(plan);
        orgSubscriptionPlan.setOrgId(orgPlanRow.getOrgId());
        orgSubscriptionPlan.setStartTime(orgPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.START_TIME));
        orgSubscriptionPlan.setExpirationTime(orgPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.EXPIRATION_TIME));
        orgSubscriptionPlan.setStatus(SubscriptionStatus.get(orgPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.SUBSCRIPTION_STATUS)));
        orgSubscriptionPlan.setContractState(PlanContractState.get(orgPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.CONTRACT_STATE)));
        orgSubscriptionPlan.setQuantity(orgPlanRow.get(OMPBORGANIZATIONSUBSCRIPTIONS.QUANTITY));

        orgPlanDiscountRows.forEach(orgPlanDiscountRow -> {
            final long discountId = orgPlanDiscountRow.get(OMPBORGANIZATIONDISCOUNTS.DISCOUNT_ID);
            final Row discountRow = dataContainer.getRow(discountTable, discountId);

            orgSubscriptionPlan.addDiscount(planManager.convertRowIntoDiscount(discountRow));
        });

        return orgSubscriptionPlan;
    }

    /**
     * Retrieves old DataContainer state for a set of org plan IDs.
     *
     * @param orgId      Organization ID
     * @param orgPlanIds Collection of org plan IDs to fetch
     * @return DataContainer containing rows before update
     */
    private DataContainer fetchOldDataContainer(final Long orgId, final Collection<Long> orgPlanIds) {
        final Table orgPlanTable = getOrgPlanTable();
        final Table orgPlanDiscountTable = getOrgPlanDiscountTable();
        final Column orgPlanPkColumn = orgPlanTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(orgPlanTable);

        selectQuery.addJoinClause(new JoinClause(orgPlanPkColumn, orgPlanDiscountTable.getColumn(OMPBORGANIZATIONDISCOUNTS.ORG_PLAN_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(orgPlanPkColumn, orgPlanIds)));

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * Loads the billing and transaction details from the given OrgSubscriptions object
     * into the provided target data containers. Depending on whether this is an event for
     * creation or update, it initializes or updates billing contact and address records,
     * sets up a new transaction row, and adds corresponding org plan transactions.
     *
     * @param planEventMap            true if this operation is for creating new records, false for updating
     * @param orgSubscriptions        the subscription/event context containing the subscription, billing, and payer details
     * @param billingContactContainer temporary storage or fetch for billing contact/address data (can be empty or from DB)
     * @param targetContainer         container where rows are inserted or updated (commits changes to DB)
     */
    private void loadBillingDetailsIntoContainer(final Map<Long, Action> planEventMap, final OrgSubscriptions orgSubscriptions,
                                                 final DataContainer billingContactContainer, final DataContainer targetContainer) {
        final Long orgId = orgSubscriptions.orgId();
        final Table transactionTable = getTransactionTable();
        final Table billingContactTable = getBillingContactTable();
        final Table planTransactionTable = getPlanTransactionTable();
        final BillingDetails billingDetails = orgSubscriptions.getBillingDetails();
        final Payer contact = billingDetails.getContact();
        final BillingAmount billingAmount = billingDetails.getPrice();
        final Row transactionRow = new Row(transactionTable);
        final Long transactionId = transactionRow.getPKValue();
        final Row billingContactRow = 0 == billingContactContainer.size(billingContactTable)
                ? new Row(billingContactTable) : billingContactContainer.getRow(billingContactTable);

        loadBillingContactIntoContainer(orgSubscriptions.getUserId(), billingContactRow, contact, billingContactContainer, targetContainer);

        transactionRow.setOrgId(orgId);
        transactionRow.set(OMPBTRANSACTION.BILLING_CONTACT_ID, contact.id());
        transactionRow.set(OMPBTRANSACTION.TRANSACTION_ID, billingDetails.getTransactionId());
        transactionRow.set(OMPBTRANSACTION.AMOUNT, billingAmount.getAmount());
        transactionRow.set(OMPBTRANSACTION.TAX, billingAmount.getTax());
        transactionRow.set(OMPBTRANSACTION.DISCOUNT, billingAmount.getDiscount());
        transactionRow.set(OMPBTRANSACTION.CONVENIENCE_FEE, billingAmount.getConvenienceFee());
        transactionRow.set(OMPBTRANSACTION.TOTAL_AMOUNT, billingAmount.getTotalAmount());
        transactionRow.set(OMPBTRANSACTION.CURRENCY, billingAmount.getCurrency().getId());
        transactionRow.set(OMPBTRANSACTION.STATUS, PaymentStatus.PAYMENT_PENDING.getId());
        targetContainer.addNewRow(transactionRow);
        billingDetails.setLocalTransactionId(transactionId);
        final Long ordinaryPlanId = orgSubscriptions.getOrdinaryPlan().map(OrgSubscriptionPlan::id).orElse(null);

        for (final OrgSubscriptionPlan orgPlan : orgSubscriptions.getOrgPlans()) {
            final Row planTransactionRow = new Row(planTransactionTable);
            final JsonObject planMetaJson = buildPlanMetaJson(planEventMap, orgPlan);

            if (PlanType.ADD_ON == orgPlan.getPlan().getPlanType()) {
                planMetaJson.put(SubscriptionPlanFields.ORDINARY_PLAN_ID, ordinaryPlanId);
            }
            planTransactionRow.setOrgId(orgId);
            planTransactionRow.set(OMPBPLANTRANSACTION.ORG_PLAN_ID, orgPlan.id());
            planTransactionRow.set(OMPBPLANTRANSACTION.TRANSACTION_ID, transactionId);
            planTransactionRow.set(OMPBPLANTRANSACTION.PLAN_META, planMetaJson.toString());
            targetContainer.addNewRow(planTransactionRow);
        }
    }

    /**
     * Loads billing contact details into the specified target data container.
     * Handles both new and existing billing contact rows, sets organization and user identifiers,
     * assigns address and contact info fields, and updates the target container with modified data.
     *
     * @param userId            the user identifier associated with the billing contact
     * @param billingContactRow the data row representing the billing contact (may be new or existing)
     * @param contact           the Payer object containing contact details to set
     * @param sourceContainer   the source DataContainer used for address information
     * @param targetContainer   the target DataContainer where the billing contact will be updated
     */
    private void loadBillingContactIntoContainer(final long userId, final Row billingContactRow,
                                                 final Payer contact, final DataContainer sourceContainer,
                                                 final DataContainer targetContainer) {
        final Address address = contact.getAddress();

        if (billingContactRow.isNewRow()) {
            billingContactRow.setOrgId(contact.orgId());
            billingContactRow.set(OMPBBILLINGCONTACT.USER_ID, userId);
        }

        if (Objects.isNull(address)) {
            billingContactRow.set(OMPBBILLINGCONTACT.ADDRESS_ID, null);
        } else {
            loadAddressIntoContainer(billingContactRow.isNewRow(), address, sourceContainer, targetContainer);

            billingContactRow.set(OMPBBILLINGCONTACT.ADDRESS_ID, address.id());
        }
        final Long contactId = billingContactRow.getPKValue();

        contact.setId(contactId);
        billingContactRow.set(OMPBBILLINGCONTACT.FIRST_NAME, contact.getFirstName());
        billingContactRow.set(OMPBBILLINGCONTACT.LAST_NAME, contact.getLastName());
        billingContactRow.set(OMPBBILLINGCONTACT.COMPANY, contact.getCompany());
        billingContactRow.set(OMPBBILLINGCONTACT.EMAIL, contact.getEmail());
        billingContactRow.set(OMPBBILLINGCONTACT.PHONE, contact.getPhoneWithCountryCode());
        targetContainer.updateRow(billingContactRow);
    }

    /**
     * Builds the metadata JSON object describing the org's subscription plan and action type.
     * Includes the CREATE or UPDATE action, and the quantity of the plan.
     *
     * @param planEventMap true if it's a create event, false for update
     * @param orgPlan      the organization plan instance associated with the subscription
     * @return a JsonObject containing plan meta fields (action, quantity)
     */
    private JsonObject buildPlanMetaJson(final Map<Long, Action> planEventMap, final OrgSubscriptionPlan orgPlan) {
        final JsonObject planMetaJson = Json.object();

        planMetaJson.put(SubscriptionPlanFields.ACTION, planEventMap.get(orgPlan.id()));
        planMetaJson.put(SubscriptionPlanFields.QUANTITY, orgPlan.getQuantity());
        return planMetaJson;
    }

    /**
     * Loads the provided Address object data into the billing contact table.
     * If isCreateEvent is true, initializes a new address row, otherwise, fetches an existing one.
     * Sets org ID, primary key, and address-specific fields, and updates the address row in the target container.
     *
     * @param isCreateEvent   true if creating a new address record
     * @param address         address entity to persist
     * @param sourceContainer source data container for addresses (used for fetching existing rows)
     * @param targetContainer destination container to update/insert address row
     */
    private void loadAddressIntoContainer(final boolean isCreateEvent, final Address address,
                                          final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table addressTable = addressManager.getTable();
        final Country country = address.getCountry();
        final Row addressRow = isCreateEvent ? new Row(addressTable) : Optional.ofNullable(sourceContainer.getRow(addressTable))
                .orElseGet(() -> new Row(addressTable));

        if (addressRow.isNewRow()) {
            addressRow.setOrgId(address.orgId());
        }
        address.setId(addressRow.getPKValue());
        addressRow.set(OMETADDRESS.ADDRESS_LINE1, address.getAddressLine1());
        addressRow.set(OMETADDRESS.ADDRESS_LINE2, address.getAddressLine2());
        addressRow.set(OMETADDRESS.CITY, address.getCity());
        addressRow.set(OMETADDRESS.STATE, address.getState());
        addressRow.set(OMETADDRESS.ZIPCODE, address.getZip());

        if (Objects.nonNull(country)) {
            addressRow.set(OMETADDRESS.COUNTRY, country.getId());
        }
        targetContainer.updateRow(addressRow);
    }

    /**
     * Retrieves a DataContainer for the billing contact, including their address information,
     * based either on their ID (fetch from database with join on address), or as a new/empty data container if ID is null.
     *
     * @param orgId   the organization ID for filtering lookup
     * @param contact Payer/contact entity from which to fetch ID and details
     * @return a DataContainer with the billing contact and their joined address, or a new DataContainer
     */
    private DataContainer fetchBillingContactContainer(final Long orgId, final Payer contact) {
        final Long contactId = Objects.isNull(contact) ? null : contact.id();
        final Table addressTable = addressManager.getTable();
        final Table billingContactTable = getBillingContactTable();
        final SelectQuery selectQuery = SelectQuery.get(billingContactTable);

        selectQuery.addJoinClause(new JoinClause(billingContactTable.getColumn(OMPBBILLINGCONTACT.ADDRESS_ID), addressTable.getPKColumn(), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(Objects.isNull(contactId) ? null : new WhereClause(Criteria.eq(billingContactTable.getPKColumn(), contactId)));
        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * Converts a billing contact data row and associated address data into a Payer (billing contact) object.
     * Extracts and maps user details as well as linked address information from the provided data container.
     *
     * @param contactRow    the data row containing billing contact fields
     * @param dataContainer the container holding associated address row and other dependent data
     * @return a Payer object populated with contact and address details from the data rows
     */
    private Payer convertRowIntoBillingContact(final Row contactRow, final DataContainer dataContainer) {
        final Payer payer = new Payer();
        final Row addressRow = dataContainer.getRow(addressManager.getTable());

        payer.setId(contactRow.getPKValue());
        payer.setFirstName(contactRow.get(OMPBBILLINGCONTACT.FIRST_NAME));
        payer.setLastName(contactRow.get(OMPBBILLINGCONTACT.LAST_NAME));
        payer.setCompany(contactRow.get(OMPBBILLINGCONTACT.COMPANY));
        payer.setEmail(contactRow.get(OMPBBILLINGCONTACT.EMAIL));
        payer.setPhoneWithCountryCode(contactRow.get(OMPBBILLINGCONTACT.PHONE));

        if (Objects.nonNull(addressRow)) {
            payer.setAddress(convertRowIntoBillingAddress(addressRow));
        }

        return payer;
    }

    /**
     * Converts an address data row into an Address object, mapping all relevant address fields.
     *
     * @param addressRow the data row containing address fields
     * @return an Address object populated with address details from the row
     */
    private Address convertRowIntoBillingAddress(final Row addressRow) {
        final Address address = new Address();
        final Integer countryId = addressRow.get(OMETADDRESS.COUNTRY);

        address.setId(addressRow.getPKValue());
        address.setAddressLine1(addressRow.get(OMETADDRESS.ADDRESS_LINE1));
        address.setAddressLine2(addressRow.get(OMETADDRESS.ADDRESS_LINE2));
        address.setCity(addressRow.get(OMETADDRESS.CITY));
        address.setState(addressRow.get(OMETADDRESS.STATE));
        address.setZip(addressRow.get(OMETADDRESS.ZIPCODE));

        if (Objects.nonNull(countryId)) {
            address.setCountry(Country.getCountryById(countryId));
        }

        return address;
    }

    /**
     * Converts a data row representing an invoice and its associated data container into an Invoice object.
     * Retrieves invoice details from the invoiceRow and additional transaction details from the dataContainer,
     * mapping all necessary fields including ID, organization, billing date, PDF URL, transaction ID, status,
     * amount, and currency.
     *
     * @param invoiceRow    the data row containing invoice fields
     * @param dataContainer the container providing access to related transaction data
     * @return a fully populated Invoice object based on the data rows
     */
    private Invoice convertRowIntoInvoice(final Row invoiceRow, final DataContainer dataContainer) {
        final Invoice invoice = new Invoice();
        final Row transactionRow = dataContainer.getRow(getTransactionTable(), (long) invoiceRow.get(OMPBINVOICE.TRANSACTION_ID));

        invoice.setId(invoiceRow.getPKValue());
        invoice.setOrgId(invoiceRow.getOrgId());
        invoice.setBillingDate(invoiceRow.get(OMPBINVOICE.BILLING_DATE));
        invoice.setPdfUrl(invoiceRow.get(OMPBINVOICE.PDF_URL));
        invoice.setTransactionId(invoiceRow.get(OMPBINVOICE.TRANSACTION_ID));
        invoice.setStatus(Invoice.Status.get(invoiceRow.get(OMPBINVOICE.STATUS)));
        invoice.setAmount(transactionRow.get(OMPBTRANSACTION.AMOUNT));
        invoice.setCurrency(Currency.getCurrencyById(transactionRow.get(OMPBTRANSACTION.CURRENCY)));

        return invoice;
    }

    /**
     * Fetches a DataContainer containing invoices for the specified organization.
     * Includes joined transaction data (currency, amount) and applies ordering and limiting.
     *
     * @param orgId         identifier of the organization to fetch invoices for
     * @param whereClause   the filtering criteria for the query results
     * @param orderByClause the ordering criteria for the query results
     * @param limitClause   the limiting criteria to restrict the number of results
     * @return a DataContainer encapsulating the fetched invoice and transaction data
     */
    private DataContainer fetchInvoiceContainer(final long orgId, final WhereClause whereClause,
                                                final OrderByClause orderByClause, final LimitClause limitClause) {
        final Table invoiceTable = getInvoiceTable();
        final Table transactionTable = getTransactionTable();
        final SelectQuery selectQuery = SelectQuery.get(invoiceTable);

        selectQuery.addSelectClause(invoiceTable);
        selectQuery.addSelectClause(transactionTable.getColumn(OMPBTRANSACTION.CURRENCY));
        selectQuery.addSelectClause(transactionTable.getColumn(OMPBTRANSACTION.AMOUNT));
        selectQuery.addJoinClause(new JoinClause(invoiceTable.getColumn(OMPBINVOICE.TRANSACTION_ID), transactionTable.getPKColumn()));
        selectQuery.setWhereClause(whereClause);
        selectQuery.setLimitClause(limitClause);
        selectQuery.addOrderByClause(orderByClause);

        return getOrgDataStore(orgId).get(selectQuery);
    }

    /**
     * Retrieves the appropriate database column for a given field name related to invoice sorting.
     * Supports specific fields like billing date, invoice number (primary key), status, and amount.
     * Throws an error if the provided fieldName does not correspond to a valid column.
     *
     * @param fieldName the field name for which to fetch the corresponding database column
     * @return the Column object corresponding to the provided field name
     * @throws IllegalArgumentException if the fieldName is invalid or unsupported
     */
    private Column fetchColumn(final String fieldName) {
        final Table invoiceTable = getInvoiceTable();

        return switch (fieldName) {
            case SubscriptionPlanFields.DATE -> invoiceTable.getColumn(OMPBINVOICE.BILLING_DATE);
            case SubscriptionPlanFields.INVOICE_NUMBER -> invoiceTable.getPKColumn();
            case SubscriptionPlanFields.STATUS -> invoiceTable.getColumn(OMPBINVOICE.STATUS);
            case SubscriptionPlanFields.AMOUNT -> getTransactionTable().getColumn(OMPBTRANSACTION.AMOUNT);
            default -> throw ErrorCode.get(SubscriptionErrorCodes.INVALID_SORT_FIELD);
        };
    }
}

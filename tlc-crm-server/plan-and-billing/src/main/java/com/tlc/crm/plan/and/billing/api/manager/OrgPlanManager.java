package com.tlc.crm.plan.and.billing.api.manager;

import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.common.models.SortConfig;

import com.tlc.crm.plan.and.billing.api.manager.impl.OrgPlanManagerImpl;
import com.tlc.crm.plan.and.billing.api.models.InvoiceSummary;
import com.tlc.crm.plan.and.billing.api.models.Invoices;
import com.tlc.crm.plan.and.billing.api.models.OrgSubscriptionPlan;
import com.tlc.crm.plan.and.billing.api.models.OrgSubscriptions;
import com.tlc.crm.plan.and.billing.api.models.PlanActivationRequest;
import com.tlc.crm.plan.and.billing.api.models.PlanCancellationData;
import com.tlc.crm.plan.and.billing.api.models.PlanContractState;

import com.tlc.payment.gateway.models.Payer;
import com.tlc.payment.gateway.models.PaymentStatus;

import com.tlc.plans.and.billings.manager.SubscriptionPlanManager;
import com.tlc.plans.and.billings.models.Discount;
import com.tlc.plans.and.billings.models.Discounts;
import com.tlc.plans.and.billings.models.PlanType;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

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
public interface OrgPlanManager {

    /**
     * Returns the singleton instance of OrgPlanManager.
     *
     * @return OrgPlanManager instance
     */
    static OrgPlanManager getInstance() {
        return OrgPlanManagerImpl.getInstance();
    }

    /**
     * Fetches the current plans for the given organization and PlanType.
     *
     * @param orgId    Organization ID
     * @param planType PlanType filter
     * @return Plans currently active for the organization, matching the type.
     */
    Collection<OrgSubscriptionPlan> getCurrentPlans(final long orgId, final PlanType planType);

    /**
     * Updates existing organization subscriptions and appropriately syncs discounts.
     *
     * @param orgSubscriptions Updated subscription set for the organization
     */
    void upsert(final OrgSubscriptions orgSubscriptions);

    /**
     * Upgrades the current plan into new plan.
     *
     * @param orgId         Represents the organisation ID.
     * @param transactionId Represents the transaction ID of the new plan.
     */
    void activatePlan(final Long orgId, final Long transactionId);

    /**
     * Updates the payment transaction status for a given organization and transaction ID.
     * This method should be called to reflect the current state of the transaction
     * in the system, such as success, failure, or pending, based on payment gateway notifications or internal processing.
     *
     * @param orgId         the unique identifier of the organization to which the transaction belongs
     * @param transactionId the unique identifier of the transaction to update
     * @param paymentStatus the new payment status to set for the transaction (e.g., SUCCESS, FAILED, PENDING)
     */
    void updateTransactionStatus(final long orgId, final Long transactionId, final PaymentStatus paymentStatus);

    /**
     * Fetches a single organization subscription plan by its unique ID.
     *
     * @param orgId Organization ID
     * @param id    Subscription plan ID
     * @return OrgSubscriptionPlan instance or null if not found
     */
    OrgSubscriptionPlan get(final long orgId, final long id);

    /**
     * Fetches multiple organization subscription plans by IDs.
     *
     * @param orgId Organization ID
     * @param ids   Set of subscription plan IDs
     * @return Collection of OrgSubscriptionPlans
     */
    Collection<OrgSubscriptionPlan> get(final long orgId, final Collection<Long> ids);

    /**
     * Fetches all organization plans matching contract state and optional plan types.
     *
     * @param orgId             Organization ID
     * @param planContractState Contract state filter (CURRENT, UPCOMING, etc.)
     * @param planTypes         Optional filter for PlanType
     * @return OrgSubscriptions object containing filtered plans
     */
    OrgSubscriptions get(final long orgId, final PlanContractState planContractState, final PlanType... planTypes);

    /**
     * Adds a new billing contact for the organization, represented by the supplied Payer object.
     * Use this method when onboarding a customer or setting up billing information for the first time.
     *
     * @param userId         User ID
     * @param billingContact the Payer model containing the billing contact details to be stored
     */
    void addBillingContact(final Long userId, final Payer billingContact);

    /**
     * Updates the existing billing contact information for the organization with new details provided in the Payer object.
     * Use this method to change, replace, or correct previously saved billing contact data.
     *
     * @param userId         User ID
     * @param billingContact the Payer model containing the updated billing contact information
     */
    void updateBillingContact(final Long userId, final Payer billingContact);

    /**
     * Retrieves the billing contact information (payer details) for the specified organization.
     *
     * @param orgId the unique identifier of the organization whose billing contact is to be fetched
     * @return a Payer object containing the billing contact information for the given organization
     */
    Payer getBillingContact(final long orgId);

    /**
     * Retrieves a summary of invoices for the specified organization.
     *
     * @param orgId the unique identifier of the organization whose invoice summary is to be fetched
     * @return an {@link InvoiceSummary} object representing the summarized invoice data for the organization
     */
    InvoiceSummary getInvoiceSummary(final long orgId);

    /**
     * Retrieves a list of invoices for the specified organization, sorted and limited according to provided configurations.
     *
     * @param orgId       the unique identifier of the organization whose invoices are to be listed
     * @param sortConfig  configuration specifying the sorting order and criteria for the invoice list
     * @param limitConfig configuration specifying pagination or record-limit settings for the invoice list
     * @return an Invoices object containing the list of invoices matching the given criteria for the organization
     */
    Invoices getInvoiceList(final long orgId, final SortConfig sortConfig, final LimitConfig limitConfig);

    /**
     * Retrieves the total count of invoices associated with the specified organization.
     *
     * @param orgId the unique identifier of the organization whose invoices are to be counted
     * @return the total number of invoices for the given organization
     */
    long getTotalInvoiceCount(final long orgId);

    /**
     * Checks if billing details exist for the specified organization.
     *
     * @param orgId the unique identifier of the organization
     * @return true if billing details are present for the organization; false otherwise
     */
    boolean billingDetailsExists(final long orgId);

    /**
     * Fetches the collection of {@link Discount} for the organization.
     *
     * @param orgId         the unique identifier of the organization.
     * @param planType      represents the type of the plan.
     * @param discountTypes represents the type of the discount.
     * @return Collection of {@link Discount}.
     */
    Map<Long, Discounts> getOrgDiscounts(final long orgId, final PlanType planType, final Discount.Type... discountTypes);

    /**
     * Checks whether the discount already activated for the given organization.
     *
     * @param orgId      The unique identifier of the organization.
     * @param discountId The unique identifier of the discount.
     * @return A flag that represents the discount is already activated or not.
     */
    boolean isDiscountAlreadyActivated(final long orgId, final long discountId);

    /**
     * Stores the cancellation discount for the organization.
     *
     * @param orgId      The unique identifier of the organization.
     * @param discountId The unique identifier of the discount.
     */
    void activateDiscount(final long orgId, final long discountId);

    /**
     * Cancels the organization subscriptions.
     *
     * @param planCancellationData plans cancellation data of the organization
     */
    void cancelPlan(final PlanCancellationData planCancellationData);

    /**
     * Reactivates the cancelled plan of an organization.
     *
     * @param orgId  The unique identifier of an organization.
     * @param planId The unique identifier of the plan.
     */
    void reactivatePlan(final long orgId, final long planId);

    /**
     * Checks if the given plan is the current active plan for the specified organization.
     *
     * @param orgId  the unique identifier of the organization
     * @param planId the unique identifier of the plan to check
     * @return true if the plan is the current active plan for the organization, false otherwise
     */
    boolean isCurrentPlan(final long orgId, final long planId);

    /**
     * Checks if the given plan is expired or not.
     *
     * @param orgId  the unique identifier of the organization
     * @param planId the unique identifier of the plan to check
     * @return true if the plan is expired, false otherwise
     */
    boolean isPlanExpired(final long orgId, final long planId);

    /**
     * Checks if the given plan is paid plan or not.
     *
     * @param orgId  the unique identifier of the organization
     * @param planId the unique identifier of the plan to check
     * @return true if the plan is paid plan, false otherwise
     */
    boolean isPaidPlan(final Long orgId, final Long planId);

    /**
     * Retrieves the count of organization subscription plans that require expiration updates.
     *
     * @return A count of {@link OrgSubscriptions} representing plans that are expired or about to expire and need processing.
     */
    long getPlansRequiringExpirationUpdateCount();

    /**
     * Retrieves the collection of organization subscription plans that require expiration updates.
     *
     * @param start Starting index of the {@link OrgSubscriptions}.
     * @param limit Count to fetch the {@link OrgSubscriptions}.
     * @return A {@link Collection} of {@link OrgSubscriptions} representing plans that are expired or about to expire and need processing.
     */
    Collection<OrgSubscriptions> getPlansRequiringExpirationUpdate(final int start, final int limit);

    /**
     * Retrieves the upcoming subscription plans for the given set of organization IDs.
     *
     * @param orgIds a Collection of organization IDs for which to fetch upcoming plans.
     * @return A {@link Collection} of {@link OrgSubscriptions} representing plans that are scheduled to become active for the given organizations.
     */
    Collection<OrgSubscriptions> getUpcomingPlans(final Collection<Long> orgIds);

    /**
     * Switches subscription plans based on the provided plan activation data.
     *
     * This performs plan expiration and activation in a batch operation using the details
     * in each {@link PlanActivationRequest} instance.
     *
     * @param planActivationData a {@link Collection} of {@link PlanActivationRequest} objects specifying old and upcoming plan associations by organization.
     */
    void switchPlan(final Collection<PlanActivationRequest> planActivationData);

    /**
     * Fetches the {@link PaymentStatus} by using transaction ID.
     *
     * @param transactionId Represents the transaction ID.
     * @return {@link PaymentStatus}.
     */
    Optional<PaymentStatus> getPaymentStatus(final long transactionId);

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
    Map<Long, Integer> getDiscountsAppliedCount(final long orgId, final Discount.Type... discountTypes);

    /**
     * Retrieves the total number of times a specific discount has been applied within a particular organization.
     * <p>
     * This method queries the database to aggregate usage records, filtering by
     * both the organization and the discount ID. It is typically used for
     * validating usage limits or generating organization-level reports.
     * </p>
     *
     * @param orgId The unique identifier of the organization.
     * @param discountId The unique identifier of the discount to check.
     * @return The total count of applications for the specified discount
     * within the given organization. Returns 0 if no records are found.
     */
    int getDiscountAppliedCount(final long orgId, final long discountId);

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
    Map<Long, Boolean> getDiscountsActivationStatus(final long orgId, final Collection<Long> discountIds);

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
    Boolean getTransactionCompletionStatus(final long transactionId);
}

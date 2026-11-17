package com.tlc.plans.and.billings.manager;

import com.tlc.plans.and.billings.manager.impl.SubscriptionPlanManagerImpl;
import com.tlc.plans.and.billings.models.CancellationReason;
import com.tlc.plans.and.billings.models.Discount;
import com.tlc.plans.and.billings.models.Feature;
import com.tlc.plans.and.billings.models.FeatureGroup;
import com.tlc.plans.and.billings.models.PlanType;
import com.tlc.plans.and.billings.models.SubscriptionPlan;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;

/**
 * <p>
 * This manager interfaces with the database tables via SQL APIs to retrieve and
 * assemble fully populated SubscriptionPlan objects used throughout the Plans and Billings module.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface SubscriptionPlanManager {

    static SubscriptionPlanManager getInstance() {
        return SubscriptionPlanManagerImpl.getInstance();
    }

    /**
     * Retrieves the database table representation for subscription plans.
     *
     * @return subscription plans Table instance.
     */
    Table getPlanTable();

    /**
     * Retrieves the database table representation for pricing schemes.
     *
     * @return pricing schemes Table instance.
     */
    Table getPricingSchemeTable();

    /**
     * Retrieves the database table representation for pricing scheme discounts.
     *
     * @return pricing scheme discounts Table instance.
     */
    Table getPricingSchemeDiscountTable();

    /**
     * Retrieves the database table representation for cancellation feedback.
     *
     * @return cancellation feedback Table instance.
     */
    Table getCancellationFeedbackTable();

    /**
     * Retrieves the database table representation for discounts.
     *
     * @return discounts Table instance.
     */
    Table getDiscountTable();

    /**
     * Fetches all active subscription plans filtered by the given plan type,
     * and constructs fully populated SubscriptionPlan objects including pricing schemes,
     * features, feature groups, and discounts.
     *
     * @param planType the type of plan to fetch.
     * @return collection of fully initialized active SubscriptionPlan instances.
     */
    Collection<SubscriptionPlan> getActivePlans(final PlanType planType);

    /**
     * Retrieves the default trial subscription plan.
     *
     * @return the {@link SubscriptionPlan} object representing the default trial plan.
     */
    SubscriptionPlan getDefaultTrialPlan();

    /**
     * Builds a collection of SubscriptionPlan instances from the raw DataContainer rows.
     *
     * <p>This method maps database rows into domain objects and links related entities such as features,
     * feature groups, discounts, and pricing schemes.</p>
     *
     * @param dataContainer the container holding query result rows.
     * @return collection of populated SubscriptionPlan objects.
     */
    Collection<SubscriptionPlan> buildSubscriptionPlansFromDc(final DataContainer dataContainer);

    /**
     * Converts a discount database row into a Discount domain object.
     *
     * @param discountRow the database row containing discount data.
     * @return the Discount instance.
     */
    Discount convertRowIntoDiscount(final Row discountRow);

    /**
     * Fetches the {@link Feature} of the plan based on plan type.
     *
     * @return Collection of {@link FeatureGroup}.
     */
    Collection<FeatureGroup> getPlanFeatures();

    /**
     * Fetches the collection of {@link CancellationReason}
     *
     * @return Collection of {@link CancellationReason}.
     */
    Collection<CancellationReason> getCancellationReasonList();

    /**
     * Fetches the collection of {@link Discount}.
     *
     * @return Collection of {@link Discount}.
     */
    Collection<Discount> getCancellationDiscounts();

    /**
     * Fetches the collection of {@link FeatureGroup} of the given plan.
     *
     * @param planId The unique identifier of the plan.
     * @return Collection of {@link FeatureGroup}
     */
    Collection<FeatureGroup> getPlanFeatureGroups(final long planId);

    /**
     * Retrieves a specific discount record based on its unique identifier.
     *
     * @param discountId The unique ID of the discount to be retrieved.
     * @return The {@link Discount} object associated with the ID.
     */
    Discount getDiscount(final long discountId);
}
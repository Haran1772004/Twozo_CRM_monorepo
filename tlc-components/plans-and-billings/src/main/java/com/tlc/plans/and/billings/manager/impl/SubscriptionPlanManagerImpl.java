package com.tlc.plans.and.billings.manager.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;

import com.tlc.i18n.I18nKey;

import com.tlc.plans.and.billings.models.CancellationReason;
import com.tlc.plans.and.billings.sql.resource.MPBCANCELLATIONFEEDBACK;
import com.tlc.plans.and.billings.sql.resource.MPBCANCELLATIONREASON;
import com.tlc.plans.and.billings.internal.status.SubscriptionErrorCodes;
import com.tlc.plans.and.billings.manager.SubscriptionPlanManager;
import com.tlc.plans.and.billings.models.BillingCycle;
import com.tlc.plans.and.billings.models.Discount;
import com.tlc.plans.and.billings.models.Feature;
import com.tlc.plans.and.billings.models.FeatureGroup;
import com.tlc.plans.and.billings.models.PlanType;
import com.tlc.plans.and.billings.models.PricingBasis;
import com.tlc.plans.and.billings.models.PricingScheme;
import com.tlc.plans.and.billings.models.SubscriptionPlan;
import com.tlc.plans.and.billings.sql.resource.MPBDISCOUNTS;
import com.tlc.plans.and.billings.sql.resource.MPBFEATUREGROUPS;
import com.tlc.plans.and.billings.sql.resource.MPBFEATURES;
import com.tlc.plans.and.billings.sql.resource.MPBPLANFEATURES;
import com.tlc.plans.and.billings.sql.resource.MPBPRICINGSCHEMEDISCOUNTS;
import com.tlc.plans.and.billings.sql.resource.MPBPRICINGSCHEMES;
import com.tlc.plans.and.billings.sql.resource.MPBSUBSCRIPTIONPLANS;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * This manager interfaces with the database tables via SQL APIs to retrieve and
 * assemble fully populated SubscriptionPlan objects used throughout the Plans and Billings module.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SubscriptionPlanManagerImpl implements SubscriptionPlanManager {

    private final AdminDataStore adminDataStore;

    private SubscriptionPlanManagerImpl() {
        adminDataStore = SqlServiceProvider.get().getAdminDataStore();
    }

    private static final class INSTANCE_HOLDER {
        private static final SubscriptionPlanManagerImpl PLAN_MANAGER = new SubscriptionPlanManagerImpl();
    }

    public static SubscriptionPlanManagerImpl getInstance() {
        return INSTANCE_HOLDER.PLAN_MANAGER;
    }

    /**
     * Retrieves the database table representation for subscription plans.
     *
     * @return subscription plans Table instance.
     */
    @Override
    public Table getPlanTable() {
        return Table.get(MPBSUBSCRIPTIONPLANS.TABLE);
    }

    /**
     * Retrieves the database table representation for pricing schemes.
     *
     * @return pricing schemes Table instance.
     */
    @Override
    public Table getPricingSchemeTable() {
        return Table.get(MPBPRICINGSCHEMES.TABLE);
    }

    /**
     * Retrieves the database table representation for pricing scheme discounts.
     *
     * @return pricing scheme discounts Table instance.
     */
    @Override
    public Table getPricingSchemeDiscountTable() {
        return Table.get(MPBPRICINGSCHEMEDISCOUNTS.TABLE);
    }

    /**
     * Retrieves the database table representation for discounts.
     *
     * @return discounts Table instance.
     */
    @Override
    public Table getDiscountTable() {
        return Table.get(MPBDISCOUNTS.TABLE);
    }

    /**
     * Retrieves the MPBCANCELLATIONFEEDBACK table representation.
     *
     * @return Table instance for cancellation feedback.
     */
    @Override
    public Table getCancellationFeedbackTable() {
        return Table.get(MPBCANCELLATIONFEEDBACK.TABLE);
    }

    /**
     * Retrieves the database table representation for feature groups.
     *
     * @return feature groups Table instance.
     */
    private Table getFeatureGroupTable() {
        return Table.get(MPBFEATUREGROUPS.TABLE);
    }

    /**
     * Retrieves the database table representation for features.
     *
     * @return features Table instance.
     */
    private Table getFeatureTable() {
        return Table.get(MPBFEATURES.TABLE);
    }

    /**
     * Retrieves the MPBCANCELLATIONREASON table representation.
     *
     * @return Table instance for cancellation reason.
     */
    private Table getCancellationReasonTable() {
        return Table.get(MPBCANCELLATIONREASON.TABLE);
    }

    /**
     * Retrieves the database table representation for plan features.
     *
     * @return plan features Table instance.
     */
    private Table getPlanFeatureTable() {
        return Table.get(MPBPLANFEATURES.TABLE);
    }

    /**
     * Creates a filtered derived table view for subscription plans based on active status.
     *
     * @param isActive whether to filter active plans.
     * @return derived Table object representing active or inactive plans.
     */
    private Table getPlanTable(final boolean isActive) {
        final Table planTable = getPlanTable();
        final SelectQuery selectQuery = SelectQuery.get(planTable);

        selectQuery.addSelectClause(planTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(planTable.getColumn(MPBSUBSCRIPTIONPLANS.IS_ACTIVE), isActive)));

        return DerivedTable.get(planTable.getName(), planTable.getAlias(), selectQuery);
    }

    /**
     * Creates a filtered derived table view for discounts based on active status.
     *
     * @param isActive whether to filter active discounts.
     * @return derived Table object representing active or inactive discounts.
     */
    private Table getDefaultDiscountTable(final boolean isActive) {
        final Table discountTable = getDiscountTable();
        final SelectQuery selectQuery = SelectQuery.get(discountTable);

        selectQuery.addSelectClause(discountTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(discountTable.getColumn(MPBDISCOUNTS.IS_ACTIVE), isActive))
                .and(Criteria.in(discountTable.getColumn(MPBDISCOUNTS.TYPE), Discount.Type.getDefaultDiscountTypeIds())));

        return DerivedTable.get(discountTable.getName(), discountTable.getAlias(), selectQuery);
    }

    /**
     * Fetches all active subscription plans filtered by the given plan type,
     * and constructs fully populated SubscriptionPlan objects including pricing schemes,
     * features, feature groups, and discounts.
     *
     * @param planType the type of plan to fetch.
     * @return collection of fully initialized active SubscriptionPlan instances.
     */
    @Override
    public Collection<SubscriptionPlan> getActivePlans(final PlanType planType) {
        final Table planTable = getPlanTable(true);
        final Table pricingSchemeTable = getPricingSchemeTable();
        final Table discountTable = getDefaultDiscountTable(true);
        final Table pricingSchemeDiscountTable = getPricingSchemeDiscountTable();

        final Column planPkColumn = planTable.getPKColumn();

        final SelectQuery selectQuery = SelectQuery.get(planTable);

        selectQuery.addJoinClause(new JoinClause(planPkColumn, pricingSchemeTable.getColumn(MPBPRICINGSCHEMES.PLAN_ID)));
        selectQuery.addJoinClause(new JoinClause(pricingSchemeTable.getPKColumn(), pricingSchemeDiscountTable.getColumn(MPBPRICINGSCHEMEDISCOUNTS.PRICING_SCHEME_ID), JoinClause.JoinType.LEFT));
        selectQuery.addJoinClause(new JoinClause(pricingSchemeDiscountTable.getColumn(MPBPRICINGSCHEMEDISCOUNTS.DISCOUNT_ID), discountTable.getPKColumn(), JoinClause.JoinType.LEFT));

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(planTable.getColumn(MPBSUBSCRIPTIONPLANS.PLAN_TYPE), planType.getId())));
        selectQuery.addOrderByClause(new OrderByClause(planTable.getColumn(MPBSUBSCRIPTIONPLANS.ORDER)));

        final DataContainer dataContainer = adminDataStore.get(selectQuery);

        return buildSubscriptionPlansFromDc(dataContainer);
    }

    /**
     * Retrieves the default trial subscription plan.
     *
     * @return the {@link SubscriptionPlan} object representing the default trial plan.
     */
    @Override
    public SubscriptionPlan getDefaultTrialPlan() {
        final Table planTable = getPlanTable();
        final Table pricingSchemeTable = getPricingSchemeTable();
        final SelectQuery selectQuery = SelectQuery.get(planTable);
        final WhereClause whereClause = new WhereClause(Criteria.eq(planTable.getColumn(MPBSUBSCRIPTIONPLANS.IS_DEFAULT_TRIAL_PLAN), true));

        selectQuery.addJoinClause(new JoinClause(planTable.getPKColumn(), pricingSchemeTable.getColumn(MPBPRICINGSCHEMES.PLAN_ID)));
        selectQuery.setWhereClause(whereClause);
        final DataContainer dataContainer = adminDataStore.get(selectQuery);

        return buildSubscriptionPlansFromDc(dataContainer).stream().findAny().orElseThrow(() -> ErrorCode.get(SubscriptionErrorCodes.DEFAULT_TRIAL_PLAN_NOT_FOUND));
    }

    /**
     * Builds a collection of SubscriptionPlan instances from the raw DataContainer rows.
     *
     * <p>This method maps database rows into domain objects and links related entities such as features,
     * feature groups, discounts, and pricing schemes.</p>
     *
     * @param dataContainer the container holding query result rows.
     * @return collection of populated SubscriptionPlan objects.
     */
    @Override
    public Collection<SubscriptionPlan> buildSubscriptionPlansFromDc(final DataContainer dataContainer) {
        final Table planTable = getPlanTable();
        final Table pricingSchemeTable = getPricingSchemeTable();
        final Table discountTable = getDiscountTable();

        final Map<Long, Discount> discountMap = dataContainer.getRows(discountTable)
                .collect(Collectors.toMap(Row::getPKValue, this::convertRowIntoDiscount));

        return dataContainer.getRows(pricingSchemeTable).map(pricingSchemeRow -> {
            final PricingScheme pricingScheme = convertRowIntoPricingScheme(pricingSchemeRow, discountMap, dataContainer);
            final long planId = pricingSchemeRow.get(MPBPRICINGSCHEMES.PLAN_ID);
            final Row planRow = dataContainer.getRow(planTable, planId);

            return convertRowIntoPlan(planRow, pricingScheme);
        }).toList();
    }

    /**
     * Converts a discount database row into a Discount domain object.
     *
     * @param discountRow the database row containing discount data.
     * @return the Discount instance.
     */
    @Override
    public Discount convertRowIntoDiscount(final Row discountRow) {
        final Discount discount = new Discount();

        discount.setId(discountRow.getPKValue());
        discount.setName(discountRow.get(MPBDISCOUNTS.NAME));
        discount.setDescription(discountRow.get(MPBDISCOUNTS.DESCRIPTION));
        discount.setSimplifiedDescription(discountRow.get(MPBDISCOUNTS.SIMPLIFIED_DESCRIPTION));
        discount.setValue(discountRow.get(MPBDISCOUNTS.VALUE));
        discount.setReferenceId(discountRow.get(MPBDISCOUNTS.REFERENCE_ID));
        discount.setStartTime(discountRow.get(MPBDISCOUNTS.START_TIME));
        discount.setExpirationTime(discountRow.get(MPBDISCOUNTS.EXPIRATION_TIME));
        discount.setExpirationPeriod(discountRow.get(MPBDISCOUNTS.EXPIRATION_PERIOD));
        discount.setMaxApplyLimit(discountRow.get(MPBDISCOUNTS.MAX_APPLY_LIMIT));
        discount.setType(Discount.Type.get(discountRow.get(MPBDISCOUNTS.TYPE)));
        discount.setActive(discountRow.get(MPBDISCOUNTS.IS_ACTIVE));

        return discount;
    }

    /**
     * Fetches the {@link Feature} of the plan based on plan type.
     *
     * @return Collection of {@link FeatureGroup}.
     */
    @Override
    public Collection<FeatureGroup> getPlanFeatures() {
        final Table featureTable = getFeatureTable();
        final Table featureGroupTable = getFeatureGroupTable();
        final SelectQuery selectQuery = SelectQuery.get(featureGroupTable);

        selectQuery.addJoinClause(new JoinClause(featureGroupTable.getPKColumn(), featureTable.getColumn(MPBFEATURES.GROUP_ID)));
        final DataContainer dataContainer = adminDataStore.get(selectQuery);
        final Stream<Row> featureGroupRows = dataContainer.getRows(featureGroupTable);

        return featureGroupRows.map(featureGroupRow -> convertRowIntoFeatureGroup(featureGroupRow, dataContainer)).toList();
    }

    /**
     * Fetches the collection of {@link CancellationReason}
     *
     * @return Collection of {@link CancellationReason}.
     */
    @Override
    public Collection<CancellationReason> getCancellationReasonList() {
        final Table cancellationReasonTable = getCancellationReasonTable();
        final SelectQuery selectQuery = SelectQuery.get(cancellationReasonTable);

        selectQuery.addOrderByClause(new OrderByClause(cancellationReasonTable.getColumn(MPBCANCELLATIONREASON.ORDER)));

        return adminDataStore.get(selectQuery)
                .getRows(cancellationReasonTable)
                .map(cancellationReasonRow -> {
                    final CancellationReason cancellationReason = new CancellationReason();

                    cancellationReason.setId(cancellationReasonRow.getPKValue());
                    cancellationReason.setReason(cancellationReasonRow.get(MPBCANCELLATIONREASON.NAME));
                    cancellationReason.setOrder(cancellationReasonRow.get(MPBCANCELLATIONREASON.ORDER));

                    return cancellationReason;
                }).collect(Collectors.toSet());
    }

    /**
     * Fetches the collection of {@link Discount}.
     *
     * @return Collection of {@link Discount}.
     */
    @Override
    public Collection<Discount> getCancellationDiscounts() {
        final Table discountTable = getDiscountTable();
        final DataContainer dataContainer = adminDataStore.get(discountTable,
                new WhereClause(Criteria.eq(discountTable.getColumn(MPBDISCOUNTS.TYPE), Discount.Type.CANCELLATION_DISCOUNT.getId())));

        return dataContainer.getRows(discountTable).map(this::convertRowIntoDiscount).collect(Collectors.toSet());
    }

    /**
     * Fetches the collection of {@link FeatureGroup} of the given plan.
     *
     * @param planId The unique identifier of the plan.
     * @return Collection of {@link FeatureGroup}
     */
    @Override
    public Collection<FeatureGroup> getPlanFeatureGroups(final long planId) {
        final Table planFeatureTable = getPlanFeatureTable();
        final Table featureTable = getFeatureTable();
        final Table featureGroupTable = getFeatureGroupTable();
        final SelectQuery selectQuery = SelectQuery.get(featureGroupTable);

        selectQuery.addSelectClause(featureGroupTable);
        selectQuery.addJoinClause(new JoinClause(featureGroupTable.getPKColumn(), featureTable.getColumn(MPBFEATURES.GROUP_ID)));
        selectQuery.addJoinClause(new JoinClause(featureTable.getPKColumn(), planFeatureTable.getColumn(MPBPLANFEATURES.FEATURE_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(planFeatureTable.getColumn(MPBPLANFEATURES.PLAN_ID), planId)));
        final DataContainer dataContainer = adminDataStore.get(selectQuery);
        final Stream<Row> featureGroupRows = dataContainer.getRows(featureGroupTable);

        return featureGroupRows.map(featureGroupRow -> convertRowIntoFeatureGroup(featureGroupRow, dataContainer)).toList();
    }

    /**
     * Retrieves a specific discount record based on its unique identifier.
     *
     * @param discountId The unique ID of the discount to be retrieved.
     * @return The {@link Discount} object associated with the ID.
     */
    @Override
    public Discount getDiscount(final long discountId) {
        final Table discountTable = getDiscountTable();
        final Row discountRow = adminDataStore.get(discountTable, discountId);

        if (Objects.isNull(discountRow)) {
            throw ErrorCode.get(SubscriptionErrorCodes.DISCOUNT_NOT_FOUND);
        }
        return convertRowIntoDiscount(discountRow);
    }

    /**
     * Converts a feature group database row into a FeatureGroup domain object.
     *
     * @param featureGroupRow the database row containing feature group data.
     * @return the FeatureGroup instance.
     */
    private FeatureGroup convertRowIntoFeatureGroup(final Row featureGroupRow, final DataContainer dataContainer) {
        final Long featureGroupId = featureGroupRow.getPKValue();
        final Table featureTable = getFeatureTable();
        final Stream<Row> featureRows = dataContainer.getRows(featureTable,
                new WhereClause(Criteria.eq(featureTable.getColumn(MPBFEATURES.GROUP_ID), featureGroupId)));
        final FeatureGroup featureGroup = new FeatureGroup();

        featureGroup.setId(featureGroupId);
        featureGroup.setName(featureGroupRow.get(MPBFEATUREGROUPS.NAME));
        featureGroup.setOrder(featureGroupRow.get(MPBFEATUREGROUPS.ORDER));
        featureRows.forEach(featureRow -> featureGroup.addFeature(convertRowIntoFeature(featureRow, featureGroup)));

        return featureGroup;
    }

    /**
     * Converts a feature database row into a Feature domain object, linking the FeatureGroup.
     *
     * @param featureRow   the database row containing feature data.
     * @param featureGroup the associated FeatureGroup, may be null.
     * @return the Feature instance.
     */
    private Feature convertRowIntoFeature(final Row featureRow, final FeatureGroup featureGroup) {
        final Feature feature = new Feature();

        feature.setId(featureRow.getPKValue());
        feature.setName(featureRow.get(MPBFEATURES.NAME));
        feature.setDescription(featureRow.get(MPBFEATURES.DESCRIPTION));
        feature.setOrder(featureRow.get(MPBFEATURES.ORDER));
        feature.setConfig(Feature.createConfig((String) featureRow.get(MPBFEATURES.CONFIG)));
        feature.setFeatureGroup(featureGroup);

        return feature;
    }

    /**
     * Converts a pricing scheme row into a PricingScheme domain object including applied discounts.
     *
     * @param pricingSchemeRow the database row containing pricing scheme data.
     * @param discountMap      map of Discount instances by discount ID.
     * @param dataContainer    the entire data container holding associated data rows.
     * @return the PricingScheme instance.
     */
    private PricingScheme convertRowIntoPricingScheme(final Row pricingSchemeRow, final Map<Long, Discount> discountMap,
                                                      final DataContainer dataContainer) {
        final Table pricingSchemeDiscountTable = getPricingSchemeDiscountTable();
        final Long pricingSchemeId = pricingSchemeRow.getPKValue();
        final PricingScheme pricingScheme = new PricingScheme();

        final Stream<Row> pricingSchemeDiscountRows = dataContainer.getRows(pricingSchemeDiscountTable,
                new WhereClause(Criteria.eq(pricingSchemeDiscountTable.getColumn(MPBPRICINGSCHEMEDISCOUNTS.PRICING_SCHEME_ID), pricingSchemeId)));

        pricingScheme.setId(pricingSchemeId);
        pricingScheme.setValue(pricingSchemeRow.get(MPBPRICINGSCHEMES.VALUE));
        pricingScheme.setPricingBasis(PricingBasis.get(pricingSchemeRow.get(MPBPRICINGSCHEMES.PRICING_BASIS)));
        pricingScheme.setBillingCycle(BillingCycle.get(pricingSchemeRow.get(MPBPRICINGSCHEMES.BILLING_CYCLE)));

        pricingSchemeDiscountRows.forEach(pricingSchemeDiscountRow -> {
            final Long discountId = pricingSchemeDiscountRow.get(MPBPRICINGSCHEMEDISCOUNTS.DISCOUNT_ID);

            if (discountMap.containsKey(discountId)) {
                pricingScheme.addDiscount(discountMap.get(discountId));
            }
        });

        return pricingScheme;
    }

    /**
     * Converts a plan row into a SubscriptionPlan domain object, fully populating it
     * with pricing scheme and features including key features.
     *
     * @param planRow       the database row containing the plan data.
     * @param pricingScheme the related PricingScheme object.
     * @return a fully constructed SubscriptionPlan instance.
     */
    private SubscriptionPlan convertRowIntoPlan(final Row planRow, final PricingScheme pricingScheme) {
        final Long planId = planRow.getPKValue();
        final Short order = planRow.get(MPBSUBSCRIPTIONPLANS.ORDER);
        final Short planType = planRow.get(MPBSUBSCRIPTIONPLANS.PLAN_TYPE);
        final String keyFeatures = planRow.get(MPBSUBSCRIPTIONPLANS.KEY_FEATURES);

        final SubscriptionPlan subscriptionPlan = new SubscriptionPlan();

        subscriptionPlan.setId(planId);
        subscriptionPlan.setName((I18nKey) planRow.get(MPBSUBSCRIPTIONPLANS.NAME));
        subscriptionPlan.setDescription((I18nKey) planRow.get(MPBSUBSCRIPTIONPLANS.DESCRIPTION));
        subscriptionPlan.setOrder(planRow.get(MPBSUBSCRIPTIONPLANS.ORDER));
        subscriptionPlan.setSimplifiedDescription((I18nKey) planRow.get(MPBSUBSCRIPTIONPLANS.SIMPLIFIED_DESCRIPTION));
        subscriptionPlan.setPricingScheme(pricingScheme);

        if (Objects.nonNull(order)) {
            subscriptionPlan.setOrder(order);
        }

        if (Objects.nonNull(planType)) {
            subscriptionPlan.setPlanType(PlanType.get(planType));
        }

        if (Objects.nonNull(keyFeatures)) {
            final JsonArray keyFeaturesArray = Json.array(keyFeatures);
            final int size = keyFeaturesArray.size();

            for (int index = 0; index < size; index++) {
                final String keyFeature = keyFeaturesArray.getString(index);
                final Feature feature = new Feature();

                feature.setName(new I18nKey(keyFeature));
                feature.setOrder((short) index);
                subscriptionPlan.addKeyFeature(feature);
            }
        }

        return subscriptionPlan;
    }
}
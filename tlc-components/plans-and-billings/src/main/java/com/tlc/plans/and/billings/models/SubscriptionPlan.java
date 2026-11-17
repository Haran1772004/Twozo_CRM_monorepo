package com.tlc.plans.and.billings.models;

import com.tlc.i18n.I18nKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Represents a subscription plan that includes pricing, features, and plan type.
 * Used in the billing engine to define plan offerings.
 *
 * @author Sathish
 * @version 1.0
 */
public class SubscriptionPlan implements Comparable<SubscriptionPlan> {

    private Long id;
    private I18nKey name;
    private I18nKey description;
    private I18nKey simplifiedDescription;
    private PlanType planType;
    private PricingScheme pricingScheme;
    private Map<Long, FeatureGroup> featureGroups;
    private Collection<Feature> keyFeatures;
    private short order;
    private boolean isActive;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public I18nKey getName() {
        return name;
    }

    public void setName(final I18nKey name) {
        this.name = name;
    }

    public void setName(final String name) {
        this.name = new I18nKey(name);
    }

    public I18nKey getDescription() {
        return description;
    }

    public void setDescription(final I18nKey description) {
        this.description = description;
    }

    public void setDescription(final String description) {
        this.description = new I18nKey(description);
    }

    public I18nKey getSimplifiedDescription() {
        return simplifiedDescription;
    }

    public void setSimplifiedDescription(final I18nKey simplifiedDescription) {
        this.simplifiedDescription = simplifiedDescription;
    }

    public void setSimplifiedDescription(final String simplifiedDescription) {
        this.simplifiedDescription = new I18nKey(simplifiedDescription);
    }

    public PlanType getPlanType() {
        return planType;
    }

    public void setPlanType(final PlanType planType) {
        this.planType = planType;
    }

    public PricingScheme getPricingScheme() {
        return pricingScheme;
    }

    public void setPricingScheme(final PricingScheme pricingScheme) {
        this.pricingScheme = pricingScheme;
    }

    public Collection<FeatureGroup> getFeatureGroups() {
        return Objects.isNull(featureGroups) ? Collections.emptySet() : new TreeSet<>(featureGroups.values());
    }

    public void setFeatureGroups(final Collection<FeatureGroup> featureGroups) {
        this.featureGroups = Objects.isNull(this.featureGroups) ? new HashMap<>() : this.featureGroups;

        for (final FeatureGroup featureGroup : featureGroups) {
            this.featureGroups.put(featureGroup.getId(), featureGroup);
        }
    }

    public void addFeatureGroup(final FeatureGroup featureGroup) {
        featureGroups = Objects.isNull(featureGroups) ? new HashMap<>() : featureGroups;

        featureGroups.put(featureGroup.getId(), featureGroup);
    }

    public void addFeature(final Feature feature) {
        featureGroups = Objects.isNull(featureGroups) ? new HashMap<>() : featureGroups;

        final FeatureGroup featureGroup = feature.getFeatureGroup();
        final FeatureGroup existingFeatureGroup = featureGroups.computeIfAbsent(featureGroup.getId(), id -> featureGroup);

        existingFeatureGroup.addFeature(feature);
    }

    public Collection<Feature> getKeyFeatures() {
        return Objects.isNull(keyFeatures) ? Collections.emptySet() : new TreeSet<>(keyFeatures);
    }

    public void setKeyFeatures(final Collection<Feature> keyFeatures) {
        this.keyFeatures = Objects.isNull(this.keyFeatures) ? new ArrayList<>() : this.keyFeatures;

        this.keyFeatures.addAll(keyFeatures);
    }

    public void addKeyFeature(final Feature keyFeature) {
        keyFeatures = Objects.isNull(keyFeatures) ? new ArrayList<>() : keyFeatures;

        keyFeatures.add(keyFeature);
    }

    public short getOrder() {
        return order;
    }

    public void setOrder(final short order) {
        this.order = order;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(final boolean active) {
        isActive = active;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final SubscriptionPlan subscriptionPlan) {
        return Short.compare(this.order, subscriptionPlan.getOrder());
    }
}

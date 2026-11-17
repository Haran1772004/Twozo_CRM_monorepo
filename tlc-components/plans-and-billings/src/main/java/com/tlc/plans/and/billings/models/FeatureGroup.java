package com.tlc.plans.and.billings.models;

import com.tlc.i18n.I18nKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Represents a group of related features in a subscription plan.
 * Used to logically organize features and control display order.
 *
 * <p>Example groups might include "Analytics", "Deal Management", or "Integrations".</p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FeatureGroup implements Comparable<FeatureGroup> {

    private Long id;
    private I18nKey name;
    private Collection<Feature> features;
    private short order;

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

    public Collection<Feature> getFeatures() {
        return new TreeSet<>(features);
    }

    public void setFeatures(final Collection<Feature> features) {
        this.features = Objects.isNull(this.features) ? new ArrayList<>() : this.features;

        this.features.addAll(features);
    }

    public void addFeature(final Feature feature) {
        features = Objects.isNull(features) ? new ArrayList<>() : features;

        features.add(feature);
    }

    public short getOrder() {
        return order;
    }

    public void setOrder(final short order) {
        this.order = order;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final FeatureGroup featureGroup) {
        return Short.compare(this.order, featureGroup.getOrder());
    }
}

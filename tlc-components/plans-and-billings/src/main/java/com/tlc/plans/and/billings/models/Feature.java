package com.tlc.plans.and.billings.models;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.i18n.I18nKey;
import com.tlc.plans.and.billings.fields.SubscriptionPlanFields;
import com.tlc.plans.and.billings.sql.resource.MPBSUBSCRIPTIONPLANS;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.resolver.PKPatternResolver;
import com.tlc.sql.oltp.resolver.OltpPKPatternResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a feature included in a subscription plan.
 * Each feature belongs to a group and may have configurable limits or toggles.
 *
 * @author Sathish
 * @version 1.0
 */
public class Feature implements Comparable<Feature> {

    private Long id;
    private I18nKey name;
    private I18nKey description;
    private FeatureGroup featureGroup;
    private Config config;
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

    public I18nKey getDescription() {
        return description;
    }

    public void setDescription(final I18nKey description) {
        this.description = description;
    }

    public FeatureGroup getFeatureGroup() {
        return featureGroup;
    }

    public void setFeatureGroup(final FeatureGroup featureGroup) {
        this.featureGroup = featureGroup;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(final Config config) {
        this.config = config;
    }

    public short getOrder() {
        return order;
    }

    public void setOrder(final short order) {
        this.order = order;
    }

    /**
     * Factory method to create a feature config object from a String representation.
     *
     * @param configString String containing config data
     * @return {@link Config} instance
     */
    public static Config createConfig(final String configString) {
        return new Config(Json.object(configString));
    }

    /**
     * Factory method to create a feature config object from a JSON representation.
     *
     * @param configJson JSON containing config data
     * @return {@link Config} instance
     */
    public static Config createConfig(final JsonObject configJson) {
        return new Config(configJson);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Feature feature) {
        return Short.compare(this.order, feature.getOrder());
    }

    /**
     * Represents configuration settings for a feature such as enablement and limits.
     */
    public static class Config {

        private final Map<Long, PlanConfig> planConfigs;

        private Config(final JsonObject configJson) {
            final JsonArray planConfigArray = configJson.getJsonArray(SubscriptionPlanFields.PLAN_CONFIG);
            final Table planTable = Table.get(MPBSUBSCRIPTIONPLANS.TABLE);
            final PKPatternResolver pkPatternResolver = new OltpPKPatternResolver(planTable);
            final int size = planConfigArray.size();

            this.planConfigs = new HashMap<>();

            for (int index = 0; index < size; index++) {
                final JsonObject planConfigJson = planConfigArray.getJsonObject(index);
                final String planIdPattern = planConfigJson.getString(SubscriptionPlanFields.PLAN_ID);
                final Long planId = pkPatternResolver.resolve(planIdPattern);

                planConfigJson.remove(SubscriptionPlanFields.PLAN_ID);
                planConfigs.put(planId, new PlanConfig(planConfigJson));
            }
        }

        public Map<Long, PlanConfig> getAll() {
            return planConfigs;
        }

        public JsonObject toJson() {
            final JsonObject jsonObject = Json.object();

            for (final Map.Entry<Long, PlanConfig> planConfigEntry : planConfigs.entrySet()) {
                jsonObject.put(planConfigEntry.getKey().toString(), planConfigEntry.getValue().toJson());
            }
            return jsonObject;
        }
    }

    /**
     * Represents plan configuration settings for a feature such as enablement and limits.
     */
    public static class PlanConfig {

        private final boolean isEnabled;
        private final Integer accessibleCount;
        private final String supportingHours;
        private final String supportingDays;

        private final JsonObject planConfigJson;

        private PlanConfig(final JsonObject planConfigJson) {
            isEnabled = planConfigJson.getBoolean(SubscriptionPlanFields.IS_ENABLED);
            accessibleCount = planConfigJson.getNullableValue(SubscriptionPlanFields.ACCESSIBLE_COUNT);
            supportingHours = planConfigJson.getNullableString(SubscriptionPlanFields.SUPPORTING_HOURS);
            supportingDays = planConfigJson.getNullableString(SubscriptionPlanFields.SUPPORTING_DAYS);

            this.planConfigJson = planConfigJson;
        }

        public boolean isEnabled() {
            return isEnabled;
        }

        public Integer getAccessibleCount() {
            return accessibleCount;
        }

        public String getSupportingHours() {
            return supportingHours;
        }

        public String getSupportingDays() {
            return supportingDays;
        }

        public JsonObject getPlanConfigJson() {
            return planConfigJson;
        }

        public JsonObject toJson() {
            return planConfigJson;
        }
    }
}

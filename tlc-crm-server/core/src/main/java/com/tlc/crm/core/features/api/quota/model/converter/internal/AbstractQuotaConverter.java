package com.tlc.crm.core.features.api.quota.model.converter.internal;

import com.tlc.commons.converter.model.ConverterData;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.CrmModelConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.api.product.model.Variation;
import com.tlc.crm.core.features.api.quota.fields.QuotaFields;
import com.tlc.crm.core.features.api.quota.manager.QuotaHolderManagerImpl;
import com.tlc.crm.core.features.api.quota.model.Quota;
import com.tlc.crm.core.features.api.quota.model.QuotaMetric;
import com.tlc.crm.core.features.api.quota.model.QuotaTimePeriod;
import com.tlc.crm.core.features.api.quota.model.QuotaTimeRange;
import com.tlc.crm.core.features.api.quota.model.QuotaType;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.l10n.temporal.model.DateTime;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Abstract base converter for {@link Quota} models
 *
 * @param <Q> the concrete {@link Quota} type
 * @param <J> the JSON representation type
 * @author kishore J
 * @version 1.0
 */
public abstract class AbstractQuotaConverter<Q extends Quota, J extends JsonObject> extends CrmModelConverter<Q, J> {

    protected final UserService userService;
    protected final CrmUserConverter crmUserConverter;
    protected final QuotaHolderManagerImpl quotaHolderManagerImpl;
    protected final BaseCurrencyService baseCurrencyService;

    protected AbstractQuotaConverter() {
        userService = UserService.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
        quotaHolderManagerImpl = QuotaHolderManagerImpl.getInstance();
        baseCurrencyService = BaseCurrencyService.getInstance();
    }

    /**
     * <p>
     * Loads the given quota model using values from the provided JSON payload.
     * </p>
     *
     * @param converterData the container holding the JSON payload along with
     *                      contextual information such as organization and user
     * @param quota         Represents the {@link Quota}
     */
    protected void loadQuotaFromJson(final ConverterData<JsonObject> converterData, final Q quota) {
        final JsonObject dataJson = converterData.dataToBeConverted();

        if (dataJson.containsKey(QuotaFields.ID)) {
            quota.setId(dataJson.getLong(QuotaFields.ID));
        }
        setJsonToModel(converterData.orgId(), converterData.ownerId(), dataJson, quota);
    }

    /**
     * <p>
     * Converts the given {@link Quota} model into its JSON representation
     * </p>
     *
     * @param quota the quota model to be converted
     * @return a {@link JsonObject} representing the quota
     */
    protected JsonObject convertModelToJson(final Q quota) {
        final JsonObject quotaJson = Json.object();
        final QuotaType quotaType = quota.getQuotaType();
        final QuotaMetric quotaMetric = quota.getQuotaMetric();
        final QuotaTimePeriod quotaTimePeriod = quota.getQuotaTimePeriod();
        final QuotaTimeRange quotaTimeRange = quota.getQuotaTimeRange();
        final Collection<Pipeline> pipelines = quota.getPipelines();
        final Collection<Product> products = quota.getProducts();
        final Collection<Variation> variations = quota.getVariations();
        final Collection<Territory> territories = quota.getTerritories();

        quotaJson.put(QuotaFields.ID, quota.id()).put(QuotaFields.START_TIME, quota.getStartTime().getTimeStamp())
                .put(QuotaFields.END_TIME, quota.getEndTime().getTimeStamp());

        quotaJson.put(QuotaFields.TYPE, quotaType.getId())
                .put(QuotaFields.METRIC, quotaMetric.getId())
                .put(QuotaFields.TIME_PERIOD, quotaTimePeriod.getId())
                .put(QuotaFields.TIME_RANGE, quotaTimeRange.getId());

        if (Objects.nonNull(pipelines)) {
            quotaJson.put(QuotaFields.PIPELINE_IDS, pipelines.stream().map(Pipeline::id).collect(Collectors.toSet()));
        }

        if (Objects.nonNull(territories)) {
            quotaJson.put(QuotaFields.TERRITORY_IDS, territories.stream().map(Territory::id).collect(Collectors.toSet()));
        }

        if (Objects.nonNull(products) || Objects.nonNull(variations)) {
            final JsonArray productArray = Json.array();

            if (Objects.nonNull(products)) {
                products.forEach(product -> productArray.put(Json.object().put(QuotaFields.PRODUCT, product.id())));
            }

            if (Objects.nonNull(variations)) {
                variations.forEach(variation -> productArray.put(Json.object()
                        .put(QuotaFields.PRODUCT, variation.getProductId())
                        .put(QuotaFields.VARIATION_ID, variation.id())));
            }
            quotaJson.put(QuotaFields.PRODUCTS, productArray);
        }

        if (quotaMetric.isDealValue()) {
            quotaJson.put(QuotaFields.TARGET, quota.getTarget());
        } else {
            quotaJson.put(QuotaFields.TARGET, quota.getTarget().longValue());
        }
        return quotaJson;
    }

    /**
     * <p>
     * Sets the {@link JsonObject} to {@link Quota}
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents an user id
     * @param dataJson Represents the data {@link JsonObject}
     * @param quota    Represents the {@link Quota}
     */
    private void setJsonToModel(final long orgId, final long userId, final JsonObject dataJson, final Q quota) {
        final CrmUser crmUser = userService.getUser(orgId, userId);

        quota.setOrgId(orgId);
        quota.setCreatedBy(crmUser);
        quota.setUpdatedBy(crmUser);

        if (dataJson.containsKey(QuotaFields.TIME_PERIOD)) {
            quota.setQuotaTimePeriod(QuotaTimePeriod.getById(dataJson.getShort(QuotaFields.TIME_PERIOD)));
        }

        if (dataJson.containsKey(QuotaFields.TIME_RANGE)) {
            quota.setQuotaTimeRange(QuotaTimeRange.getById(dataJson.getShort(QuotaFields.TIME_RANGE)));
        }

        if (dataJson.containsKey(QuotaFields.TYPE)) {
            quota.setQuotaType(QuotaType.getById(dataJson.getShort(QuotaFields.TYPE)));
        }

        if (dataJson.containsKey(QuotaFields.START_TIME)) {
            quota.setStartTime(new DateTime(dataJson.getLong(QuotaFields.START_TIME)));
        }

        if (dataJson.containsKey(QuotaFields.END_TIME)) {
            quota.setEndTime(new DateTime(dataJson.getLong(QuotaFields.END_TIME)));
        }

        if (dataJson.containsKey(QuotaFields.METRIC)) {
            quota.setQuotaMetric(QuotaMetric.getById(dataJson.getShort(QuotaFields.METRIC)));
        }
        quota.setTarget(dataJson.getDouble(QuotaFields.TARGET));

        if (dataJson.containsKey(QuotaFields.PIPELINE_IDS)) {
            final JsonArray pipelineArray = dataJson.getJsonArray(QuotaFields.PIPELINE_IDS);
            final int size = pipelineArray.size();
            final Collection<Pipeline> pipelines = new ArrayList<>(size);

            for (int index = 0; index < size; index++) {
                final Pipeline pipeline = new Pipeline();

                pipeline.setId(pipelineArray.getLong(index));
                pipelines.add(pipeline);
            }
            quota.setPipelines(pipelines);
        }

        if (dataJson.containsKey(QuotaFields.TERRITORY_IDS)) {
            final JsonArray territoryArray = dataJson.getJsonArray(QuotaFields.TERRITORY_IDS);
            final int size = territoryArray.size();
            final Collection<Territory> territories = new ArrayList<>(size);

            for (int index = 0; index < size; index++) {
                final Territory territory = new Territory();

                territory.setId(territoryArray.getLong(index));
                territories.add(territory);
            }
            quota.setTerritories(territories);
        }

        if (dataJson.containsKey(QuotaFields.PRODUCTS)) {
            final JsonArray productArray = dataJson.getJsonArray(QuotaFields.PRODUCTS);
            final int size = productArray.size();
            final Collection<Product> products = new ArrayList<>();
            final Collection<Variation> variations = new ArrayList<>();

            for (int index = 0; index < size; index++) {
                final JsonObject productJson = productArray.getJsonObject(index);
                final Long productId = productJson.getLong(QuotaFields.PRODUCT);

                if (productJson.containsKey(QuotaFields.VARIATION_ID)) {
                    final Variation variation = new Variation();

                    variation.setId(productJson.getLong(QuotaFields.VARIATION_ID));
                    variation.setProductId(productId);
                    variations.add(variation);
                } else {
                    final Product product = new Product();

                    product.setId(productId);
                    products.add(product);
                }
            }
            quota.setProducts(products);
            quota.setVariations(variations);
        }
    }
}
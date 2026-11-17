package com.tlc.crm.core.action.deal.handler;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * The {@link DynamicDealFieldEvaluatorHandlerImpl} is responsible for evaluating and computing values for deal fields
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public final class DynamicDealFieldEvaluatorHandlerImpl implements DynamicDealFieldEvaluatorHandler {

    private static final String WEIGHTED_VALUE = "weightedValue";
    private static final String ROTTING_DAYS = "rottingDays";
    private static final String AGE = "age";
    private static final String EXPECTED_DEAL_VALUE = "expectedDealValue";
    private static final String DEAL_VALUE_IN_BASE_CURRENCY = "dealValueInBaseCurrency";
    private static final String PRODUCT_AMOUNT = "productAmount";

    private final BaseCurrencyService baseCurrencyService;
    private final DateTimeUtil dateTimeUtil;

    private DynamicDealFieldEvaluatorHandlerImpl() {
        baseCurrencyService = BaseCurrencyService.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
    }

    private static class InstanceHolder {
        private static final DynamicDealFieldEvaluatorHandlerImpl INSTANCE = new DynamicDealFieldEvaluatorHandlerImpl();
    }

    /**
     * <p>
     * Gets the {@link DynamicDealFieldEvaluatorHandlerImpl} instance
     * </p>
     *
     * @return The {@link DynamicDealFieldEvaluatorHandlerImpl}
     */
    public static DynamicDealFieldEvaluatorHandlerImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldName Represents the field name
     * @param deal      Represents the {@link Deal}
     */
    public void computeFieldValue(final String fieldName, final Deal deal) {
        final long orgId = deal.orgId();
        final Currency defaultCurrency = baseCurrencyService.getDefaultCurrency(orgId);

        switch (fieldName) {
            case AGE ->
                    deal.setAge(TimeUnit.MILLISECONDS.toDays(Instant.now().toEpochMilli() - deal.getCreatedTime().getTimeStamp()));
            case ROTTING_DAYS -> {
                final Pipeline pipeline = deal.getPipeline();

                if (Objects.nonNull(pipeline)) {
                    final long timeDifference = TimeUnit.MILLISECONDS.toDays(
                            Instant.now().toEpochMilli() - dateTimeUtil.getTimeStamp(deal.getModifiedTime()));
                    final Short defaultRottingDays = deal.getPipeline().getRottenDays();

                    deal.setRottenDays(timeDifference > defaultRottingDays ? timeDifference - defaultRottingDays : 0);
                }
            }
            case WEIGHTED_VALUE -> {
                final PipelineStage dealPipelineStage = deal.getPipelineStage();

                if (Objects.nonNull(dealPipelineStage)) {
                    final BigDecimal totalDealValue = DealManager.getInstance().getDealsValueByPipelineAndStage(orgId, deal.getPipeline().id(),
                            dealPipelineStage.id());
                    final Double probability = dealPipelineStage.getProbability();
                    final BigDecimal value = BigDecimal.valueOf((totalDealValue.multiply(BigDecimal.valueOf(probability)).doubleValue()) / 100);
                    final Monetary weightedValue = new Monetary();

                    weightedValue.setCurrency(defaultCurrency);
                    weightedValue.setOrgId(orgId);
                    weightedValue.setValue(value);
                    deal.setWeightedRevenue(weightedValue);
                }
            }
            case EXPECTED_DEAL_VALUE -> {
                final Monetary value = deal.getDealValue();
                final PipelineStage dealPipelineStage = deal.getPipelineStage();

                if (Objects.nonNull(value) && Objects.nonNull(dealPipelineStage)) {
                    final Monetary expectedValue = new Monetary();
                    final BigDecimal currencyExchangeRate = baseCurrencyService.formatCalibratedValue(orgId, value.getValue(), value.getCurrency());

                    expectedValue.setOrgId(orgId);
                    expectedValue.setValue(currencyExchangeRate.multiply(BigDecimal.valueOf(dealPipelineStage.getProbability() / 100)));
                    expectedValue.setCurrency(defaultCurrency);
                    deal.setExpectedDealValue(expectedValue);
                }
            }
            case DEAL_VALUE_IN_BASE_CURRENCY -> {
                final Monetary dealValue = deal.getDealValue();

                if (Objects.nonNull(dealValue)) {
                    final Monetary baseCurrencyValue = new Monetary();
                    final BigDecimal value = dealValue.getValue();
                    final BigDecimal exchangeRate = baseCurrencyService.getCurrencyExchangeRate(orgId, dealValue.getCurrency());

                    baseCurrencyValue.setValue(Objects.isNull(exchangeRate) ? value : value.divide(exchangeRate, NumericalConstant.FIVE, RoundingMode.HALF_EVEN));
                    baseCurrencyValue.setCurrency(defaultCurrency);
                    baseCurrencyValue.setOrgId(orgId);
                    deal.setDealValueInBaseCurrency(baseCurrencyValue);
                }
            }
            case PRODUCT_AMOUNT -> {
                final ProductDetailsHolder productDetailsHolder = deal.getProductDetailsHolder();

                if (Objects.nonNull(productDetailsHolder) && Objects.equals(defaultCurrency, productDetailsHolder.getCurrency())) {
                    final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

                    if (Objects.nonNull(productDetails)) {
                        final double productAmount = productDetails.stream()
                                .mapToDouble(detail -> detail.getQuantity() * detail.getPrice()).sum();
                        final Monetary monetary = new Monetary();

                        monetary.setOrgId(orgId);
                        monetary.setCurrency(defaultCurrency);
                        monetary.setValue(BigDecimal.valueOf(productAmount));
                        deal.setProductAmount(monetary);
                    }
                }
            }
        }
    }
}
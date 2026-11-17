package com.tlc.crm.core.features.api.quota.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.models.CrmModel;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.api.product.model.Variation;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;

/**
 * A model that represents the quota details
 *
 * @author kishore J
 * @version 1.0
 */
public abstract class Quota implements TlcModel, CrmModel {

    /**
     * <p>
     * Represents the entities a quota can be restricted to.
     * </p>
     */
    public enum RelatedEntityType {
        PIPELINE,
        TERRITORY,
        PRODUCT
    }

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.quota.invalid.quota.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.quota.invalid.org.id")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.quota.invalid.metric")
    private QuotaMetric quotaMetric;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.quota.invalid.time.period")
    private QuotaTimePeriod quotaTimePeriod;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.quota.invalid.time.range")
    private QuotaTimeRange quotaTimeRange;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.quota.invalid.type")
    private QuotaType quotaType;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.quota.invalid.target")
    @Digits(integer = 16, fraction = 4, groups = {Group.Create.class, Group.Update.class}, message = "i18n.quota.target.value.limit.exceed")
    private Double target;

    @NotNull(groups = {Group.Create.class}, message = "i18n.quota.invalid.created.user")
    private CrmUser createdBy;

    @NotNull(groups = {Group.Update.class}, message = "i18n.quota.invalid.updated.user")
    private CrmUser updatedBy;

    @NotNull(groups = {Group.Create.class}, message = "i18n.quota.invalid.start.time")
    private DateTime startTime;

    @NotNull(groups = {Group.Create.class}, message = "i18n.quota.invalid.end.time")
    private DateTime endTime;
    private DateTime createdTime;
    private DateTime updatedTime;

    private Collection<Territory> territories;
    private Collection<Pipeline> pipelines;
    private Collection<Product> products;
    private Collection<Variation> variations;

    private Collection<Long> userIds;

    public CrmUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final CrmUser createdBy) {
        this.createdBy = createdBy;
    }

    public CrmUser getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(final CrmUser updatedBy) {
        this.updatedBy = updatedBy;
    }

    public QuotaType getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(final QuotaType quotaType) {
        this.quotaType = quotaType;
    }

    public QuotaMetric getQuotaMetric() {
        return quotaMetric;
    }

    public void setQuotaMetric(final QuotaMetric quotaMetric) {
        this.quotaMetric = quotaMetric;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public QuotaTimePeriod getQuotaTimePeriod() {
        return quotaTimePeriod;
    }

    public void setQuotaTimePeriod(final QuotaTimePeriod quotaTimePeriod) {
        this.quotaTimePeriod = quotaTimePeriod;
    }

    public QuotaTimeRange getQuotaTimeRange() {
        return quotaTimeRange;
    }

    public void setQuotaTimeRange(final QuotaTimeRange quotaTimeRange) {
        this.quotaTimeRange = quotaTimeRange;
    }

    public Double getTarget() {
        return target;
    }

    public void setTarget(final Double target) {
        this.target = target;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(final DateTime startTime) {
        this.startTime = startTime;
    }

    public DateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(final DateTime endTime) {
        this.endTime = endTime;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public DateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(final DateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Collection<Territory> getTerritories() {
        return territories;
    }

    public void setTerritories(final Collection<Territory> territories) {
        this.territories = territories;
    }

    public Collection<Pipeline> getPipelines() {
        return pipelines;
    }

    public void setPipelines(final Collection<Pipeline> pipelines) {
        this.pipelines = pipelines;
    }

    public Collection<Product> getProducts() {
        return products;
    }

    public void setProducts(final Collection<Product> products) {
        this.products = products;
    }

    public Collection<Variation> getVariations() {
        return variations;
    }

    public void setVariations(final Collection<Variation> variations) {
        this.variations = variations;
    }

    public double getTargetValue() {
        return NumberUtils.parseNumber(target).doubleValue();
    }

    public void setUserIds(final Collection<Long> userIds) {
        this.userIds = userIds;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        throw ErrorCode.get(CoreErrorCodes.UNSUPPORTED_OPERATION);
    }

    @Override
    public Collection<Long> userIds() {
        return userIds;
    }
}

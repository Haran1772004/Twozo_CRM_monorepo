package com.tlc.crm.core.api.product.model;

import com.tlc.crm.common.action.search.annotation.Searchable;
import com.tlc.crm.common.email.alert.annotation.EmailAlertField;
import com.tlc.crm.common.email.alert.annotation.EmailAlertTemplate;
import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.SearchIndex;
import com.tlc.crm.core.validator.annotation.ProductValidator;
import com.tlc.crm.core.zapier.ZapTrigger;
import com.tlc.crm.entity.dropdown.DefaultOption;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.common.action.search.annotation.SearchField;
import com.tlc.crm.entity.validator.annotation.Field;
import com.tlc.crm.entity.validator.annotation.FieldsValidator;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditEntityName;
import com.tlc.crm.entity.validator.annotation.auditlog.AuditField;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.Notifiable;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Update;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

@EmailAlertTemplate(templates = {EmailNotificationTemplate.PRODUCT_ASSIGNED, EmailNotificationTemplate.PRODUCT_TRANSFERRED})
@Notifiable(types = AppNotificationEventType.PRODUCT_ASSIGNED, modelName = CrmModelType.PRODUCT)
@Searchable(index = SearchIndex.PRODUCT)
@FieldsValidator(groups = {Create.class, Update.class}, message = "i18n.entity.fields.validation.failed")
@ProductValidator(groups = {Create.class, Update.class}, message = "i18n.product.validation.failed")
@ZapTrigger(source = Source.PRODUCT)
public class Product extends Entity {

    public static final String ENTITY_TYPE = "Product";

    @SearchField(name = "id", readMethodName = "id")
    @NotNull(groups = {Update.class, Delete.class}, message = "i18n.product.invalid.id")
    private Long id;

    @AuditEntityName
    @AuditField(aliasName = "name", displayName = "i18n.product.fields.name")
    @SearchField(name = "name")
    @Field(aliasName = "name")
    @Size(max = 255, groups = {Create.class, Update.class}, message = "i18n.product.name.length.exceeded")
    private String name;

    @Field(aliasName = "description")
    @Size(max = 255, groups = {Create.class, Update.class}, message = "i18n.product.description.length.exceeded")
    private String description;

    @Field(aliasName = "skuNumber")
    @Size(max = 16, groups = {Create.class, Update.class}, message = "i18n.product.skuNumber.length.exceeded")
    private String skuNumber;

    @SearchField(name = "productCode")
    @Field(aliasName = "productCode")
    @Size(max = 20, groups = {Create.class, Update.class}, message = "i18n.product.productCode.length.exceeded")
    private String productCode;

    @Field(aliasName = "category")
    private Option category;

    @Field(aliasName = "type")
    private Option type;

    private Variation defaultVariation;

    @Field(aliasName = "validTill")
    private Long validTill;

    @Field(aliasName = "taxable")
    private Option taxable;

    @Field(aliasName = "quantityInStock")
    private Long quantityInStock;

    @Field(aliasName = "quantityOrdered")
    private Long quantityOrdered;

    @Field(aliasName = "activeStatus")
    private Option activeStatus;

    private Collection<PriceTag> baseCurrencyPriceTags;

    private boolean notifiable;

    @EmailAlertField(
            name = "userId",
            isUserField = true,
            fieldType = EmailAlertFieldType.NUMBER,
            recipientTemplates = {EmailNotificationTemplate.PRODUCT_ASSIGNED, EmailNotificationTemplate.PRODUCT_TRANSFERRED},
            placeholderTemplates = {EmailNotificationTemplate.PRODUCT_ASSIGNED, EmailNotificationTemplate.PRODUCT_TRANSFERRED},
            requirePreviousValue = true
    )
    private Long ownerId;

    public Product() {
        super(ENTITY_TYPE);
    }

    @Override
    public Long id() {
        return id;
    }

    /**
     * <p>
     * An enum contains the various {@link ActiveStatus} data
     * </p>
     */
    public enum ActiveStatus implements DefaultOption {

        YES("i18n.product.active.status.yes", true),
        NO("i18n.product.active.status.no");

        private final String name;
        private final boolean isDefault;

        ActiveStatus(final String name) {
            this.name = Objects.requireNonNull(name);
            this.isDefault = false;
        }

        ActiveStatus(final String name, final boolean isDefault) {
            this.name = Objects.requireNonNull(name);
            this.isDefault = isDefault;
        }

        /**
         * {@inheritDoc}
         *
         * @return The option name
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * {@inheritDoc}
         *
         * @return true If the option is editable, otherwise it will return false
         */
        @Override
        public boolean isEditable() {
            return false;
        }

        /**
         * {@inheritDoc}
         *
         * @return true If the option is default, otherwise it will return false
         */
        @Override
        public boolean isDefault() {
            return isDefault;
        }

        /**
         * <p>
         * Retrieves a collection of default options represented by the {@link ActiveStatus} enum values.
         * </p>
         *
         * @return The collection of {@link DefaultOption}
         */
        public static Collection<DefaultOption> getOptions() {
            return Arrays.asList(ActiveStatus.values());
        }
    }

    /**
     * <p>
     * An enum contains the various {@link Type} data
     * </p>
     */
    public enum Type implements DefaultOption {

        UNIT("i18n.product.type.option.unit"),
        SUBSCRIPTION("i18n.product.type.option.subscription");

        private final String name;

        Type(final String name) {
            this.name = Objects.requireNonNull(name);
        }

        /**
         * {@inheritDoc}
         *
         * @return The option name
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * {@inheritDoc}
         *
         * @return true If the option is editable, otherwise it will return false
         */
        @Override
        public boolean isEditable() {
            return false;
        }

        /**
         * {@inheritDoc}
         *
         * @return true If the option is default, otherwise it will return false
         */
        @Override
        public boolean isDefault() {
            return false;
        }

        /**
         * <p>
         * Retrieves a collection of default options represented by the {@link Type} enum values.
         * </p>
         *
         * @return The collection of {@link DefaultOption}
         */
        public static Collection<DefaultOption> getOptions() {
            return Arrays.asList(Type.values());
        }
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSkuNumber() {
        return skuNumber;
    }

    public void setSkuNumber(String skuNumber) {
        this.skuNumber = skuNumber;
    }

    public Long getValidTill() {
        return validTill;
    }

    public void setValidTill(final Long validTill) {
        this.validTill = validTill;
    }

    public Option getTaxable() {
        return taxable;
    }

    public void setTaxable(final Option isTaxable) {
        this.taxable = isTaxable;
    }

    public Long getQuantityInStock() {
        return quantityInStock;
    }

    public void setQuantityInStock(final Long quantityInStock) {
        this.quantityInStock = quantityInStock;
    }

    public Long getQuantityOrdered() {
        return quantityOrdered;
    }

    public void setQuantityOrdered(final Long quantityOrdered) {
        this.quantityOrdered = quantityOrdered;
    }

    public Option getActiveStatus() {
        return activeStatus;
    }

    public void setActiveStatus(final Option activeStatus) {
        this.activeStatus = activeStatus;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public Option getType() {
        return type;
    }

    public void setType(final Option type) {
        this.type = type;
    }

    public Variation getDefaultVariation() {
        return defaultVariation;
    }

    public void setDefaultVariation(final Variation defaultVariation) {
        this.defaultVariation = defaultVariation;
    }

    public void addPriceTag(final PriceTag priceTag) {
        if (Objects.isNull(defaultVariation)) {
            this.defaultVariation = new Variation();
        }
        defaultVariation.addPriceTag(priceTag);
    }

    public void setOwnerId(final Long ownerId) {
        this.ownerId = ownerId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public Option getCategory() {
        return category;
    }

    public void setCategory(final Option category) {
        this.category = category;
    }

    public Collection<PriceTag> getBaseCurrencyPriceTags() {
        return baseCurrencyPriceTags;
    }

    public void setBaseCurrencyPriceTags(final Collection<PriceTag> priceTags) {
        this.baseCurrencyPriceTags = priceTags;
    }

    public void setNameWithVariation(final String productName, final String variationName) {
        this.name = String.format("%s (%s)", productName, variationName);
    }

    @Override
    public void setSalesOwner(final CrmUser salesOwner) {

        if (Objects.nonNull(salesOwner)) {
            this.ownerId = salesOwner.id();
            super.setSalesOwner(salesOwner);
        }
    }

    @Override
    public boolean notifiable() {
        return notifiable;
    }

    @Override
    public void setNotifiable(final boolean notifiable) {
        this.notifiable = notifiable;
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * <p>
     * An enum contains the various {@link Taxable} data
     * </p>
     */
    public enum Taxable implements DefaultOption {

        YES("i18n.product.taxable.yes", true),
        NO("i18n.product.taxable.no");

        private final String name;
        private final boolean isDefault;

        Taxable(final String name) {
            this.name = Objects.requireNonNull(name);
            this.isDefault = false;
        }

        Taxable(final String name, final boolean isDefault) {
            this.name = Objects.requireNonNull(name);
            this.isDefault = isDefault;
        }

        /**
         * {@inheritDoc}
         *
         * @return The option name
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * {@inheritDoc}
         *
         * @return true If the option is editable, otherwise it will return false
         */
        @Override
        public boolean isEditable() {
            return false;
        }

        /**
         * {@inheritDoc}
         *
         * @return true If the option is default, otherwise it will return false
         */
        @Override
        public boolean isDefault() {
            return isDefault;
        }

        /**
         * <p>
         * Retrieves a collection of default options represented by the {@link Taxable} enum values
         * </p>
         *
         * @return The collection of {@link DefaultOption}
         */
        public static Collection<DefaultOption> getOptions() {
            return Arrays.asList(Taxable.values());
        }
    }
}
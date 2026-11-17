package com.tlc.crm.common.list.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.i18n.I18nKey;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Encapsulates the configuration details of a field.
 * </p>
 *
 * @author Kowsalya SP
 * @version 1.0
 */

public class FieldConfig {

    private final Long fieldId;
    private final String idAsString;
    private final String name;
    private final I18nKey displayName;
    private final boolean primary;
    private final int sort;
    private int order;
    private final boolean search;
    private final boolean hidden;
    private Config config;
    private final boolean isPersistent;
    private final FieldConfigSource source;
    private final FieldType fieldType;
    private final Collection<SubFieldConfig> subFieldsConfig;
    private final boolean hasSubField;
    private final FieldTransformer transformer;
    private final boolean isConfigured;
    private final boolean isSortable;
    private final boolean isDynamicField;

    public FieldConfig(final Long fieldId, final String idAsString, final String name, final I18nKey displayName, final boolean primary,
                       final int sort, final int order, final boolean search, final boolean hidden, final Config config,
                       final boolean isPersistent, final FieldConfigSource source, final FieldType fieldType,
                       final Collection<SubFieldConfig> subFieldsConfig, final boolean hasSubField, final FieldTransformer transformer,
                       final boolean isConfigured, final boolean isSortable, final boolean isDynamicField) {
        this.fieldId = fieldId;
        this.idAsString = idAsString;
        this.name = name;
        this.displayName = displayName;
        this.primary = primary;
        this.sort = sort;
        this.order = order;
        this.search = search;
        this.hidden = hidden;
        this.config = config;
        this.isPersistent = isPersistent;
        this.source = source;
        this.fieldType = fieldType;
        this.subFieldsConfig = subFieldsConfig;
        this.hasSubField = hasSubField;
        this.transformer = transformer;
        this.isConfigured = isConfigured;
        this.isSortable = isSortable;
        this.isDynamicField = isDynamicField;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public String getIdAsString() {
        return idAsString;
    }

    public String getName() {
        return name;
    }

    public I18nKey getDisplayName() {
        return displayName;
    }

    public boolean isPrimary() {
        return primary;
    }

    public int getSort() {
        return sort;
    }

    public int getOrder() {
        return order;
    }

    public boolean isSearch() {
        return search;
    }

    public boolean isHidden() {
        return hidden;
    }

    public Config getConfig() {
        return config;
    }

    public boolean isPersistent() {
        return isPersistent;
    }

    public FieldConfigSource getSource() {
        return source;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public Collection<SubFieldConfig> getSubFieldsConfig() {
        return subFieldsConfig;
    }

    public boolean hasSubField() {
        return hasSubField;
    }

    public FieldTransformer getTransformer() {
        return transformer;
    }

    public boolean isConfigured() {
        return isConfigured;
    }

    public boolean isSortable() {
        return isSortable;
    }

    public void setEnabled(final boolean isEnabled, final FieldEnableType fieldEnableType) {
        final JsonObject configJson = Objects.isNull(config) ? Json.object() : config.configAsJson;

        configJson.put(fieldEnableType.getName(), isEnabled);
        this.config = new Config(configJson.toString());
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    public void setConfig(final String config) {
        this.config = new Config(config);
    }

    /**
     * <p>
     * Gets the field config as string
     * </p>
     *
     * @return the {@link String} of field config
     */
    public String getConfigAsString() {
        return config.configAsJson.toString();
    }

    public boolean isDynamicField() {
        return this.isDynamicField;
    }

    /**
     * <p>
     * Represents a field's pre-defined configs.
     * </p>
     *
     * @author Ajay
     * @version 1.0
     */
    public static class Config {

        private static final String IS_DEFAULT = "isDefault";
        private static final String IS_MANDATORY = "isMandatory";
        private static final String IS_IGNORED = "isIgnored";
        private static final String FIELD_TYPE = "fieldType";
        private static final String IS_ENABLED_FOR_MOBILE = "isEnabledForMobile";
        private static final String IS_ENABLED_FOR_WEB = "isEnabledForWeb";
        private static final String TYPE = "type";
        private static final String QUOTA_CONFIG = "quotaConfig";

        private final boolean isEmpty;
        private final boolean isDefault;
        private final boolean isMandatory;
        private final boolean isIgnored;
        private final String fieldType;
        private final boolean isClickable;
        private final String linkType;
        private final boolean isEnabledForMobile;
        private final boolean isEnabledForWeb;
        private final boolean isEnabledForContactSummaryCompanyList;
        private final boolean isEnabledForContactSummaryDealList;
        private final boolean isEnabledForCompanySummaryContactList;
        private final boolean isEnabledForCompanySummaryDealList;
        private final boolean isEnabledForDealSummaryContactList;
        private final boolean isEnabledForDealSummaryProductList;
        private final boolean isEnabledForProductSummaryDealList;
        private final boolean isEnabledForDealArchiveList;
        private final boolean isEnabledForQuotaDealList;
        private final boolean isEnabledForMarketingList;
        private final boolean isEnabledForSegmentContactList;
        private final boolean isEnabledForDealQuotationList;
        private final boolean isEnabledForContactQuotationList;
        private final boolean isEnabledForCompanyQuotationList;
        private final boolean isEnabledForDealInvoiceList;
        private final boolean isEnabledForContactInvoiceList;
        private final boolean isEnabledForCompanyInvoiceList;
        private String type;

        private final JsonObject configAsJson;

        public Config(final String config) {
            this(Objects.isNull(config) || config.isBlank() ? Json.object() : Json.object(config));
        }

        public Config(final JsonObject configJson) {
            this.configAsJson = configJson;
            isDefault = configJson.optBoolean(IS_DEFAULT);
            isMandatory = configJson.optBoolean(IS_MANDATORY);
            isIgnored = configJson.optBoolean(IS_IGNORED);
            fieldType = configJson.optString(FIELD_TYPE, null);
            isEmpty = 0 == configJson.size();
            isClickable = configJson.optBoolean(ListActionFields.IS_CLICKABLE);
            linkType = configJson.optString(ListActionFields.LINK_TYPE, null);
            isEnabledForMobile = configJson.optBoolean(IS_ENABLED_FOR_MOBILE);
            isEnabledForWeb = configJson.optBoolean(IS_ENABLED_FOR_WEB);
            isEnabledForContactSummaryCompanyList = configJson.optBoolean(FieldEnableType.IS_ENABLED_FOR_CONTACT_SUMMARY_COMPANY_LIST.getName());
            isEnabledForContactSummaryDealList = configJson.optBoolean(FieldEnableType.IS_ENABLED_FOR_CONTACT_SUMMARY_DEAL_LIST.getName());
            isEnabledForCompanySummaryContactList = configJson.optBoolean(FieldEnableType.IS_ENABLED_FOR_COMPANY_SUMMARY_CONTACT_LIST.getName());
            isEnabledForCompanySummaryDealList = configJson.optBoolean(FieldEnableType.IS_ENABLED_FOR_COMPANY_SUMMARY_DEAL_LIST.getName());
            isEnabledForDealSummaryContactList = configJson.optBoolean(FieldEnableType.IS_ENABLED_FOR_DEAL_SUMMARY_CONTACT_LIST.getName());
            isEnabledForDealSummaryProductList = configJson.optBoolean(FieldEnableType.IS_ENABLED_FOR_DEAL_SUMMARY_PRODUCT_LIST.getName());
            isEnabledForProductSummaryDealList = configJson.optBoolean(FieldEnableType.IS_ENABLED_FOR_PRODUCT_SUMMARY_DEAL_LIST.getName());
            isEnabledForDealArchiveList = configJson.optBoolean(FieldEnableType.IS_ENABLED_FOR_DEAL_ARCHIVE_LIST.getName());
            isEnabledForQuotaDealList = configJson.optBoolean(FieldEnableType.IS_ENABLED_FOR_QUOTA_DEAL_LIST.getName());
            isEnabledForMarketingList = configJson.optBoolean(FieldEnableType.IS_ENABLED_FOR_MARKETING_LIST.getName());
            isEnabledForSegmentContactList = configJson.optBoolean(FieldEnableType.IS_ENABLED_FOR_SEGMENT_CONTACT_LIST.getName());
            isEnabledForDealQuotationList = configJson.optBoolean(FieldEnableType.IS_ENABLED_FOR_DEAL_QUOTATION_LIST.getName());
            isEnabledForContactQuotationList = configJson.optBoolean(FieldEnableType.IS_ENABLED_FOR_CONTACT_QUOTATION_LIST.getName());
            isEnabledForCompanyQuotationList = configJson.optBoolean(FieldEnableType.IS_ENABLED_FOR_COMPANY_QUOTATION_LIST.getName());
            isEnabledForDealInvoiceList = configJson.optBoolean(FieldEnableType.IS_ENABLED_FOR_DEAL_INVOICE_LIST.getName());
            isEnabledForContactInvoiceList = configJson.optBoolean(FieldEnableType.IS_ENABLED_FOR_CONTACT_INVOICE_LIST.getName());
            isEnabledForCompanyInvoiceList = configJson.optBoolean(FieldEnableType.IS_ENABLED_FOR_COMPANY_INVOICE_LIST.getName());
            final JsonObject quotaConfig = configJson.optJsonObject(QUOTA_CONFIG);

            if (Objects.nonNull(quotaConfig)) {
                type = quotaConfig.optString(TYPE, null);
            }
        }

        /**
         * <p>
         * Checks whether the field is enabled for mobile view
         * </p>
         *
         * @return true, if the field is enabled for mobile as default
         */
        public boolean isEnabledForMobile() {
            return isEnabledForMobile;
        }

        /**
         * <p>
         * Checks whether the field is enabled for web view
         * </p>
         *
         * @return true, if the field is enabled for web
         */
        public boolean isEnabledForWeb() {
            return isEnabledForWeb;
        }

        /**
         * <p>
         * Checks whether the field is enabled for deal list action in contact summary view
         * </p>
         *
         * @return True if the field is enabled, false otherwise
         */
        public boolean isEnabledForContactSummaryDealList() {
            return isEnabledForContactSummaryDealList;
        }

        /**
         * <p>
         * Checks whether the field is enabled for company list action in contact summary view
         * </p>
         *
         * @return True if the field is enabled, false otherwise
         */
        public boolean isEnabledForContactSummaryCompanyList() {
            return isEnabledForContactSummaryCompanyList;
        }

        /**
         * <p>
         * Checks whether the field is enabled for contact list action in company usview
         * </p>
         *
         * @return True if the field is enabled, false otherwise
         */
        public boolean isEnabledForCompanySummaryContactList() {
            return isEnabledForCompanySummaryContactList;
        }

        /**
         * <p>
         * Checks whether the field is enabled for deal list action in contact usview
         * </p>
         *
         * @return True if the field is enabled, false otherwise
         */
        public boolean isEnabledForCompanySummaryDealList() {
            return isEnabledForCompanySummaryDealList;
        }

        /**
         * <p>
         * Checks whether the field is enabled for deal list action in contact usview
         * </p>
         *
         * @return True if the field is enabled, false otherwise
         */
        public boolean isEnabledForDealSummaryContactList() {
            return isEnabledForDealSummaryContactList;
        }

        /**
         * <p>
         * Checks whether the field is enabled for deal list action in contact usview
         * </p>
         *
         * @return True if the field is enabled, false otherwise
         */
        public boolean isEnabledForDealSummaryProductList() {
            return isEnabledForDealSummaryProductList;
        }

        /**
         * <p>
         * Checks whether the field is enabled for deal list action in contact usview
         * </p>
         *
         * @return True if the field is enabled, false otherwise
         */
        public boolean isEnabledForProductSummaryDealList() {
            return isEnabledForProductSummaryDealList;
        }

        /**
         * <p>
         * Checks whether the field is enabled for deal archive list action
         * </p>
         *
         * @return True if the field is enabled, false otherwise
         */
        public boolean isEnabledForDealArchiveList() {
            return isEnabledForDealArchiveList;
        }

        /**
         * <p>
         * Checks whether the field is enabled for quota deal list action
         * </p>
         *
         * @return True if the field is enabled, false otherwise
         */
        public boolean isEnabledForQuotaDealList() {
            return isEnabledForQuotaDealList;
        }

        /**
         * <p>
         * Checks whether the field is enabled for marketing contact list action
         * </p>
         *
         * @return True if the field is enabled, false otherwise
         */
        public boolean isEnabledForMarketingList() {
            return isEnabledForMarketingList;
        }

        /**
         * <p>
         * Checks whether the field is enabled for deal quotation list action
         * </p>
         *
         * @return True if the field is enabled, false otherwise
         */
        public boolean isEnabledForDealQuotationList() {
            return isEnabledForDealQuotationList;
        }

        /**
         * <p>
         * Checks whether the field is enabled for contact quotation list action
         * </p>
         *
         * @return True if the field is enabled, false otherwise
         */
        public boolean isEnabledForContactQuotationList() {
            return isEnabledForContactQuotationList;
        }

        /**
         * <p>
         * Checks whether the field is enabled for company quotation list action
         * </p>
         *
         * @return True if the field is enabled, false otherwise
         */
        public boolean isEnabledForCompanyQuotationList() {
            return isEnabledForCompanyQuotationList;
        }

        /**
         * <p>
         * Checks whether the field is enabled for deal invoice list action.
         * </p>
         *
         * @return True if the field is enabled, false otherwise
         */
        public boolean isEnabledForDealInvoiceList() {
            return isEnabledForDealInvoiceList;
        }

        /**
         * <p>
         * Checks whether the field is enabled for contact invoice list action.
         * </p>
         *
         * @return True if the field is enabled, false otherwise
         */
        public boolean isEnabledForContactInvoiceList() {
            return isEnabledForContactInvoiceList;
        }

        /**
         * <p>
         * Checks whether the field is enabled for company invoice list action.
         * </p>
         *
         * @return True if the field is enabled, false otherwise
         */
        public boolean isEnabledForCompanyInvoiceList() {
            return isEnabledForCompanyInvoiceList;
        }

        /**
         * <p>
         * Checks whether the field is enabled for the segment contact list action.
         * </p>
         *
         * @return True if the field is enabled, false otherwise
         */
        public boolean isEnabledForSegmentContactList() {
            return isEnabledForSegmentContactList;
        }

        /**
         * <p>
         * Checks whether the field is default.
         * </p>
         *
         * @return whether the field is default.
         */
        public boolean isDefaultField() {
            return isDefault;
        }

        /**
         * <p>
         * Checks whether the field is mandatory.
         * </p>
         *
         * @return whether the field is mandatory.
         */
        public boolean isMandatoryField() {
            return isMandatory;
        }

        /**
         * <p>
         * Checks whether the field is to be ignored.
         * </p>
         *
         * @return whether the field is to be ignored.
         */
        public boolean isIgnoredField() {
            return isIgnored;
        }

        /**
         * <p>
         * Checks whether {@link Config} is empty.
         * </p>
         *
         * @return whether {@link Config} is empty.
         */
        public boolean isEmpty() {
            return isEmpty;
        }

        /**
         * <p>
         * Gets the field type from config.
         * </p>
         *
         * @return whether the field type exists.
         */
        public String getFieldType() {
            return fieldType;
        }

        /**
         * Returns the type of hyperlink associated with the field, which also determines
         * where the field should be rendered—such as in summary views or on specific pages.
         *
         * @return The link type as a string.
         */
        public String getLinkType() {
            return this.linkType;
        }

        /**
         * Indicates whether the field is clickable in the UI.
         *
         * <p>This is typically used to determine whether a field should be rendered
         * as a hyperlink or interactive element in list or detail views.</p>
         *
         * @return {@code true} if the field is clickable; {@code false} otherwise
         */
        public boolean isClickable() {
            return this.isClickable;
        }

        /**
         * Returns the quota config type name.
         *
         * @return the type name, or null if not set
         */
        public String getType() {
            return type;
        }
    }

    /**
     * Represents the {@link FieldEnableType}, which holds the enablement type for fields
     * <p>
     * Defines whether a field is enabled for mobile or web view
     * </p>
     *
     * @author Kavinila E
     * @version 1.0
     */
    public enum FieldEnableType {

        IS_ENABLED_FOR_MOBILE("isEnabledForMobile"),
        IS_ENABLED_FOR_WEB("isEnabledForWeb"),
        IS_ENABLED_FOR_CONTACT_SUMMARY_COMPANY_LIST("isEnabledForContactSummaryCompanyList"),
        IS_ENABLED_FOR_CONTACT_SUMMARY_DEAL_LIST("isEnabledForContactSummaryDealList"),
        IS_ENABLED_FOR_COMPANY_SUMMARY_CONTACT_LIST("isEnabledForCompanySummaryContactList"),
        IS_ENABLED_FOR_COMPANY_SUMMARY_DEAL_LIST("isEnabledForCompanySummaryDealList"),
        IS_ENABLED_FOR_DEAL_SUMMARY_CONTACT_LIST("isEnabledForDealSummaryContactList"),
        IS_ENABLED_FOR_DEAL_SUMMARY_PRODUCT_LIST("isEnabledForDealSummaryProductList"),
        IS_ENABLED_FOR_PRODUCT_SUMMARY_DEAL_LIST("isEnabledForProductSummaryDealList"),
        IS_ENABLED_FOR_DEAL_ARCHIVE_LIST("isEnabledForDealArchiveList"),
        IS_ENABLED_FOR_QUOTA_DEAL_LIST("isEnabledForQuotaDealList"),
        IS_ENABLED_FOR_MARKETING_LIST("isEnabledForMarketingList"),
        IS_ENABLED_FOR_DEAL_QUOTATION_LIST("isEnabledForDealQuotationList"),
        IS_ENABLED_FOR_CONTACT_QUOTATION_LIST("isEnabledForContactQuotationList"),
        IS_ENABLED_FOR_COMPANY_QUOTATION_LIST("isEnabledForCompanyQuotationList"),
        IS_ENABLED_FOR_DEAL_INVOICE_LIST("isEnabledForDealInvoiceList"),
        IS_ENABLED_FOR_CONTACT_INVOICE_LIST("isEnabledForContactInvoiceList"),
        IS_ENABLED_FOR_COMPANY_INVOICE_LIST("isEnabledForCompanyInvoiceList"),
        IS_ENABLED_FOR_SEGMENT_CONTACT_LIST("isEnabledForSegmentContactList"),
        ;

        private final String name;

        FieldEnableType(final String name) {
            this.name = name;
        }

        /**
         * <p>
         * Gets the name of the field enable types
         * </p>
         *
         * @return the name
         */
        public String getName() {
            return name;
        }
    }
}

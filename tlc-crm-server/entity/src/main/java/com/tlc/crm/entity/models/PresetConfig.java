package com.tlc.crm.entity.models;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.l10n.temporal.TemporalServiceProvider;

import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Facilitates to convert preset config from {@link JsonObject}.
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class PresetConfig {

    private final JsonObject configJson;
    private boolean isDefaultField;
    private boolean addView;
    private boolean required;
    private Object defaultValue;
    private int order;
    private boolean immutable;
    private int rowOnIndexView;
    private int orderOnView;
    private String dropdown;
    private String dropdownType;
    private boolean showOnProfile;
    private int rowIndex;
    private boolean readOnly;
    private boolean isImmovable;
    private boolean isHidden;
    private boolean editableViaList;
    private String dependsOn;
    private long max;
    private int min;
    private boolean isFilterable;
    private int groupId;
    private boolean hasMapperTable;
    private int maxLimit;
    private boolean isDefaultSystemField;
    private boolean isFieldEnabled;
    private boolean isProductEnabled;
    private int minLength;
    private int maxLength;
    private final String filterDropdown;
    private final String filterDropdownType;
    private boolean isGroupedField;
    private final String filterFieldType;
    private final JsonObject patternJson;
    private final String requiredMessage;
    private final String minLengthMessage;
    private final String maxLengthMessage;
    private final String minMessage;
    private final String maxMessage;
    private final String maxDateMessage;
    private final String maxLimitMessage;
    private final boolean isDynamicField;
    private final String listFieldType;
    private String requestDataAccessKey;
    private String responseDataAccessKey;
    private final boolean isPrimaryField;
    private boolean isHiddenForMobile;
    private boolean isFieldGroupHead;
    private int mobileClientRowIndex;
    private JsonObject automationConfig;
    private final JsonObject summaryConfigJson;
    private final JsonObject mobileSummaryConfigJson;
    private final boolean hasSummaryConfig;
    private final boolean hasMobileSummaryConfig;
    private String category;
    private String subCategory;
    private int rowIndexOnViewV2;
    private int orderOnViewV2;
    private String summaryFieldType;
    private String mobileCategory;
    private String mobileSubCategory;
    private int mobileRowIndexOnView;
    private int mobileOrderOnView;
    private String mobileSummaryFieldType;
    private boolean isDefaultRuleCriteriaField;
    private String prefix;
    private Long noOfDigits;
    private Long startNumber;
    private boolean isDependableFieldAlwaysVisible;
    private boolean isNameField;
    private boolean overrideRequired;
    private final boolean hasOverrideRequired;

    public PresetConfig(final JsonObject jsonObject) {
        configJson = jsonObject;
        isDefaultField = configJson.optBoolean(Configuration.IS_DEFAULT_FIELD.getName());
        addView = configJson.optBoolean(Configuration.ADD_VIEW.getName());
        required = configJson.optBoolean(Configuration.REQUIRED.getName());
        defaultValue = configJson.opt(Configuration.DEFAULT.getName());
        order = configJson.optInt(Configuration.ORDER.getName(), 1);
        immutable = configJson.optBoolean(Configuration.IMMUTABLE.getName());
        rowOnIndexView = configJson.optInt(Configuration.ROW_INDEX_ON_VIEW.getName(), -1);
        orderOnView = configJson.optInt(Configuration.ORDER_ON_VIEW.getName(), -1);
        dropdown = configJson.optString(Configuration.DROPDOWN.getName(), null);
        dropdownType = configJson.optString(Configuration.DROPDOWN_TYPE.getName(), null);
        showOnProfile = configJson.optBoolean(Configuration.SHOW_ON_PROFILE.getName());
        rowIndex = configJson.optInt(Configuration.ROW_INDEX.getName(), -1);
        readOnly = configJson.optBoolean(Configuration.READ_ONLY.getName());
        isImmovable = configJson.optBoolean(Configuration.IS_IMMOVABLE.getName());
        isHidden = configJson.optBoolean(Configuration.IS_HIDDEN.getName());
        editableViaList = configJson.optBoolean(Configuration.EDITABLE_VIA_LIST.getName());
        dependsOn = configJson.optString(Configuration.DEPENDS_ON.getName(), null);
        max = configJson.optLong(Configuration.MAX.getName(), Long.MAX_VALUE);
        min = configJson.optInt(Configuration.MIN.getName(), 0);
        isFilterable = configJson.optBoolean(Configuration.IS_FILTERABLE.getName(), true);
        groupId = configJson.optInt(Configuration.GROUP_ID.getName(), 0);
        hasMapperTable = configJson.optBoolean(Configuration.HAS_MAPPER_TABLE.getName());
        maxLimit = configJson.optInt(Configuration.MAX_LIMIT.getName(), 0);
        isDefaultSystemField = configJson.optBoolean(Configuration.IS_DEFAULT_SYSTEM_FIELD.getName());
        isFieldEnabled = configJson.optBoolean(Configuration.IS_FIELD_ENABLED.getName());
        isProductEnabled = configJson.optBoolean(Configuration.IS_PRODUCT_ENABLED.getName());
        minLength = configJson.optInt(Configuration.MIN_LENGTH.getName(), 0);
        maxLength = configJson.optInt(Configuration.MAX_LENGTH.getName(), 255);
        filterDropdown = configJson.optString(Configuration.FILTER_DROPDOWN.getName(), null);
        isGroupedField = configJson.containsKey(Configuration.GROUP_ID.getName()) && isEditableViaList();
        configJson.put(Configuration.IS_GROUPED_FIELD.getName(), isGroupedField);
        filterDropdownType = configJson.optString(Configuration.FILTER_DROPDOWN_TYPE.getName(), null);
        filterFieldType = configJson.optString(Configuration.FILTER_FIELD_TYPE.getName(), null);
        patternJson = configJson.optJsonObject(Configuration.PATTERN.getName());
        isDynamicField = configJson.optBoolean(Configuration.IS_DYNAMIC_FIELD.getName(), false);
        listFieldType = configJson.optString(Configuration.LIST_FIELD_TYPE.getName(), null);
        requestDataAccessKey = configJson.getNullableString(Configuration.REQUEST_DATA_ACCESS_KEY.getName());
        responseDataAccessKey = configJson.getNullableString(Configuration.RESPONSE_DATA_ACCESS_KEY.getName());
        isPrimaryField = configJson.optBoolean(Configuration.IS_PRIMARY_FIELD.getName());
        isHiddenForMobile = configJson.optBoolean(Configuration.IS_HIDDEN_FOR_MOBILE.getName(), false);
        isFieldGroupHead = configJson.optBoolean(Configuration.IS_FIELD_GROUP_HEAD.getName());
        mobileClientRowIndex = configJson.optInt(Configuration.MOBILE_CLIENT_ROW_INDEX.getName(), -1);
        automationConfig = configJson.optJsonObject(Configuration.AUTOMATION_CONFIG.getName());
        summaryConfigJson = configJson.optJsonObject(Configuration.SUMMARY.getName());
        isDefaultRuleCriteriaField = configJson.optBoolean(Configuration.IS_DEFAULT_RULE_CRITERIA_FIELD.getName());
        mobileSummaryConfigJson = configJson.optJsonObject(Configuration.MOBILE_SUMMARY.getName());
        hasSummaryConfig = Objects.nonNull(summaryConfigJson);
        hasMobileSummaryConfig = Objects.nonNull(mobileSummaryConfigJson);
        prefix = configJson.optString(Configuration.PREFIX.getName(), "");
        noOfDigits = configJson.optLong(Configuration.NO_OF_DIGITS.getName(), 0);
        startNumber = configJson.optLong(Configuration.START_NUMBER.getName(), 0);
        requiredMessage = configJson.optString(Configuration.REQUIRED_MESSAGE.getName(), null);
        minLengthMessage = configJson.optString(Configuration.MIN_LENGTH_MESSAGE.getName(), null);
        maxLengthMessage = configJson.optString(Configuration.MAX_LENGTH_MESSAGE.getName(), null);
        minMessage = configJson.optString(Configuration.MIN_MESSAGE.getName(), null);
        maxMessage = configJson.optString(Configuration.MAX_MESSAGE.getName(), null);
        maxLimitMessage = configJson.optString(Configuration.MAX_LIMIT_MESSAGE.getName(), null);
        maxDateMessage = configJson.optString(Configuration.MAX_DATE_MESSAGE.getName(), null);
        isNameField = configJson.optBoolean(Configuration.IS_NAME_FIELD.getName());
        hasOverrideRequired = configJson.containsKey(Configuration.OVERRIDE_REQUIRED.getName());
        overrideRequired = configJson.optBoolean(Configuration.OVERRIDE_REQUIRED.getName());

        if (hasSummaryConfig) {
            category = summaryConfigJson.optString(Configuration.CATEGORY.getName(), null);
            subCategory = summaryConfigJson.optString(Configuration.SUB_CATEGORY.getName(), null);
            rowIndexOnViewV2 = summaryConfigJson.optInt(Configuration.ROW_INDEX_ON_VIEW.getName(), 0);
            orderOnViewV2 = summaryConfigJson.optInt(Configuration.ORDER_ON_VIEW.getName(), 0);
            summaryFieldType = summaryConfigJson.optString(Configuration.FIELD_TYPE.getName(), null);
            isDependableFieldAlwaysVisible = summaryConfigJson.optBoolean(Configuration.IS_DEPENDABLE_FIELD_ALWAYS_VISIBLE.getName());
        }

        if (hasMobileSummaryConfig) {
            mobileCategory = mobileSummaryConfigJson.optString(Configuration.CATEGORY.getName(), null);
            mobileSubCategory = mobileSummaryConfigJson.optString(Configuration.SUB_CATEGORY.getName(), null);
            mobileRowIndexOnView = mobileSummaryConfigJson.optInt(Configuration.ROW_INDEX_ON_VIEW.getName(), 0);
            mobileOrderOnView = mobileSummaryConfigJson.optInt(Configuration.ORDER_ON_VIEW.getName(), 0);
            mobileSummaryFieldType = mobileSummaryConfigJson.optString(Configuration.FIELD_TYPE.getName(), null);
        }
    }

    /**
     * <p>
     * Checks whether the field the default field or not
     * </p>
     *
     * @return If the is default field key present it will return true, otherwise false
     */
    public final boolean isDefaultField() {
        return isDefaultField;
    }

    /**
     * <p>
     * Checks whether add view is true or not
     * </p>
     *
     * @return if the add view key is present it will return true, otherwise false
     */
    public final boolean addView() {
        return addView;
    }

    /**
     * <p>
     * Checks whether the required true or not
     * </p>
     *
     * @return If the required key is present it will return true, otherwise false
     */
    public final boolean required() {
        return required;
    }

    /**
     * <p>
     * Retrieves the default value from the preset config
     * </p>
     *
     * @return If the default value present it will return original value, otherwise it will return null
     */
    public final <T> T defaultValue() {
        return (T) defaultValue;
    }

    /**
     * <p>
     * Retrieves the order from the preset config
     * </p>
     *
     * @return If the order key present it will return original value, otherwise it returns the default value
     */
    public final int order() {
        return order;
    }

    /**
     * <p>
     * Checks whether the immutable true or not
     * </p>
     *
     * @return If the immutable key is present it will return true, otherwise false
     */
    public final boolean immutable() {
        return immutable;
    }

    /**
     * <p>
     * Retrieves the row index from the preset config for the summary
     * </p>
     *
     * @return If the row index on view key present it returns the original value, otherwise it returns the default value
     */
    public final int rowIndexOnView() {
        return rowOnIndexView;
    }

    /**
     * <p>
     * Retrieves the order from the preset config for the summary
     * </p>
     *
     * @return If the order on view key present it returns the original value, otherwise it returns the default value
     */
    public final int orderOnView() {
        return orderOnView;
    }

    /**
     * <p>
     * Retrieves the dropdown value from the preset config
     * </p>
     *
     * @return If the dropdown key present it returns the original value, otherwise it returns the default value
     */
    public final String dropdown() {
        return dropdown;
    }

    /**
     * <p>
     * Retrieves the dropdown value from the preset config
     * </p>
     *
     * @return If the dropdown key present it returns the original value, otherwise it returns the default value
     */
    public final String dropdownTypeName() {
        return dropdownType;
    }

    /**
     * <p>
     * Retrieves the {@link DropdownType} from the preset config
     * </p>
     *
     * @return The dropdown type as a DropdownType enum, if not present return null
     */
    public final DropdownType dropdownType() {
        final String dropdownType = dropdownTypeName();

        if (Objects.nonNull(dropdownType)) {
            return DropdownType.valueOf(dropdownType.toUpperCase());
        }

        return null;
    }

    /**
     * <p>
     * Retrieves the filter dropdown type in the preset config
     * </p>
     *
     * @return the filter dropdown type
     */
    public final String filterDropdownTypeName() {
        return filterDropdownType;
    }

    /**
     * <p>
     * Retrieves the filter dropdown type from the preset config
     * </p>
     *
     * @return The dropdown type as a DropdownType enum, if not present return null
     */
    public final DropdownType filterDropdownType() {
        final String filterDropdownType = filterDropdownTypeName();

        if (Objects.nonNull(filterDropdownType)) {
            return DropdownType.valueOf(filterDropdownType.toUpperCase());
        }

        return dropdownType();
    }

    /**
     * <p>
     * Checks weather show on profile true or not for the summary
     * </p>
     *
     * @return If the show on profile key present it will return true, otherwise, it will return false
     */
    public final boolean showOnProfile() {
        return showOnProfile;
    }

    /**
     * <p>
     * Retrieves the row index from the present config
     * </p>
     *
     * @return If the row index key is present, it returns the original value, otherwise, it returns the default value
     */
    public final int rowIndex() {
        return rowIndex;
    }

    /**
     * <p>
     * Retrieves the depends on value from the preset config
     * </p>
     *
     * @return If the depends on key is present, it will return the original value, Otherwise, it returns the default value.
     */
    public final String dependsOn() {
        return dependsOn;
    }

    /**
     * <p>
     * Retrieves the max value in the preset config
     * </p>
     *
     * @return If the max key present it will return the original value, otherwise it returns the default value
     */
    public final long max() {
        return max;
    }

    /**
     * <p>
     * Retrieves the min value in the preset config
     * </p>
     *
     * @return If the min key present it will return the original value, otherwise it returns the default value
     */
    public final int min() {
        return min;
    }

    /**
     * <p>
     * Checks the immovable fields present or not in the preset config
     * </p>
     *
     * @return If the immovable key is present it will return true, otherwise false
     */
    public final boolean isImmovable() {
        return isImmovable;
    }

    /**
     * <p>
     * Checks the read only field present or not in the preset config
     * </p>
     *
     * @return If the readable key present it will return true, otherwise false
     */
    public final boolean readOnly() {
        return readOnly;
    }

    /**
     * <p>
     * Checks the hidden field present or not in the preset config
     * </p>
     *
     * @return If the hidden key present it will return true, otherwise false
     */
    public final boolean hidden() {
        return isHidden;
    }

    /**
     * <p>
     * Determines whether the field is shown in the update field list API
     * </p>
     *
     * @return true if the editableViaList key is present, otherwise false
     */
    public final boolean isEditableViaList() {
        return editableViaList;
    }

    /**
     * <p>
     * Checks the filterable fields present or not in the preset config.
     * </p>
     *
     * @return If the filterable key is present it will return true, otherwise false.
     */
    public final boolean isFilterable() {
        return isFilterable;
    }

    /**
     * <p>
     * Gets the group id
     * </p>
     *
     * @return The group id
     */
    public final int getGroupId() {
        return groupId;
    }

    /**
     * <p>
     * Checks if the field has a mapper table in the preset config
     * </p>
     *
     * @return true if the field has a mapper table; otherwise false
     */
    public final boolean hasMapperTable() {
        return hasMapperTable;
    }

    /**
     * <p>
     * Retrieves the max limit in the preset config
     * </p>
     *
     * @return If the max limit key is present it will return the original value, otherwise it returns the default value
     */
    public final int maxLimit() {
        return maxLimit;
    }

    /**
     * <p>
     * Checks whether the field is default system field.
     * </p>
     *
     * @return whether the field is default system field.
     */
    public final boolean isDefaultSystemField() {
        return isDefaultSystemField;
    }

    /**
     * <p>
     * Checks whether the field's enabled.
     * </p>
     *
     * @return whether the field's enabled.
     */
    public final boolean isFieldEnabled() {
        return isFieldEnabled;
    }

    /**
     * <p>
     * Checks whether the product's enabled.
     * </p>
     *
     * @return whether the product's enabled.
     */
    public final boolean isProductEnabled() {
        return isProductEnabled;
    }

    /**
     * Gets the pattern json associated with this field.
     *
     * @return The pattern json of the field.
     */
    public final JsonObject getPatternJson() {
        return patternJson;
    }

    /**
     * Gets the workflow config json associated with this field.
     *
     * @return The config json of the field.
     */
    public final JsonObject getAutomationConfig() {
        return automationConfig;
    }

    /**
     * <p>
     * Checks whether the field's dynamic.
     * </p>
     *
     * @return whether the field is dynamic field.
     */
    public final boolean isDynamicField() {
        return isDynamicField;
    }

    /**
     * <p>
     * Fetches the list field type
     * </p>
     *
     * @return the list field type
     */
    public String getListFieldType() {
        return listFieldType;
    }

    /**
     * <p>
     * Gets the preset config as json.
     * </p>
     *
     * @return whether the field's enabled.
     */
    public final JsonObject presetConfigAsJson() {
        return configJson;
    }

    /**
     * <p>
     * Creates a copy of the preset configuration as a new {@link JsonObject}
     * </p>
     *
     * @return a new {@link JsonObject} containing the copied preset configuration
     */
    public final JsonObject copyPresetConfigJson() {
        return Json.object(configJson.getBytes());
    }

    /**
     * <p>
     * Checks whether the field has summary config or not
     * </p>
     *
     * @return True if the field has summary config, false otherwise
     */
    public final boolean hasSummaryConfig() {
        return hasSummaryConfig;
    }

    /**
     * <p>
     * Retrieves the category of the field in the summary structure
     * </p>
     *
     * @return The category of the field in the summary
     */
    public final String getCategory() {
        return category;
    }

    /**
     * <p>
     * Retrieves the subCategory of the field in the summary structure
     * </p>
     *
     * @return The subCategory of the field in the summary
     */
    public final String getSubCategory() {
        return subCategory;
    }

    /**
     * <p>
     * Retrieves the row index of the field in the summary structure
     * </p>
     *
     * @return The row index of the field
     */
    public final int getRowIndexOnViewV2() {
        return rowIndexOnViewV2;
    }

    /**
     * <p>
     * Retrieves the order of the field in the row in the summary structure
     * </p>
     *
     * @return The order of the field in the row
     */
    public final int getOrderOnViewV2() {
        return orderOnViewV2;
    }

    /**
     * <p>
     * Fetches the summary field type
     * </p>
     *
     * @return The summary field types
     */
    public String getSummaryFieldType() {
        return summaryFieldType;
    }

    /**
     * <p>
     * Checks whether the dependable field is always visible in the summary even without a value.
     * </p>
     *
     * @return {@code true} if the dependable field should always be displayed, {@code false} otherwise
     */
    public boolean isDependableFieldAlwaysVisible() {
        return isDependableFieldAlwaysVisible;
    }


    /**
     * <p>
     * Checks whether the field has mobile summary config or not
     * </p>
     *
     * @return True if the field has mobile summary config, false otherwise
     */
    public final boolean hasMobileSummaryConfig() {
        return hasMobileSummaryConfig;
    }

    /**
     * <p>
     * Retrieves the category of the field in the mobile summary structure
     * </p>
     *
     * @return The category of the field in the mobile summary
     */
    public String getMobileCategory() {
        return mobileCategory;
    }

    /**
     * <p>
     * Retrieves the subCategory of the field in the mobile summary structure
     * </p>
     *
     * @return The subCategory of the field in the mobile summary
     */
    public String getMobileSubCategory() {
        return mobileSubCategory;
    }

    /**
     * <p>
     * Retrieves the row index of the field in the mobile summary structure
     * </p>
     *
     * @return The row index of the field in the mobile summary
     */
    public int getMobileRowIndexOnView() {
        return mobileRowIndexOnView;
    }

    /**
     * <p>
     * Retrieves the order of the field in the row in the summary structure
     * </p>
     *
     * @return The order of the field in the mobile summary row
     */
    public int getMobileOrderOnView() {
        return mobileOrderOnView;
    }

    /**
     * <p>
     * Fetches the mobile summary field type
     * </p>
     *
     * @return The mobile summary field type
     */
    public String getMobileSummaryFieldType() {
        return mobileSummaryFieldType;
    }

    /**
     * Checks whether the field is default rule criteria field.
     *
     * @return true, if the field is default rule criteria field otherwise false.
     */
    public boolean isDefaultRuleCriteriaField() {
        return isDefaultRuleCriteriaField;
    }

    /**
     * Gets the prefix of field type.
     *
     * @return The prefix of field type
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Gets the noOfDigits of field type.
     *
     * @return The noOfDigits of field type.
     */
    public Long getNoOfDigits() {
        return noOfDigits;
    }

    /**
     * Gets the start number of field.
     *
     * @return The start number of field.
     */
    public Long getStartNumber() {
        return startNumber;
    }

    /**
     * <p>
     * Retrieves the validation message displayed when a required field is left empty.
     * </p>
     *
     * @return The required field validation message, or null if not configured.
     */
    public String getRequiredMessage() {
        return requiredMessage;
    }

    /**
     * <p>
     * Retrieves the validation message displayed when the input length is below the minimum allowed.
     * </p>
     *
     * @return The minimum length validation message, or null if not configured.
     */
    public String getMinLengthMessage() {
        return minLengthMessage;
    }

    /**
     * <p>
     * Retrieves the validation message displayed when the input length exceeds the maximum allowed.
     * </p>
     *
     * @return The maximum length validation message, or null if not configured.
     */
    public String getMaxLengthMessage() {
        return maxLengthMessage;
    }

    /**
     * <p>
     * Retrieves the validation message displayed when the input value is below the minimum allowed.
     * </p>
     *
     * @return The minimum value validation message, or null if not configured.
     */
    public String getMinMessage() {
        return minMessage;
    }

    /**
     * <p>
     * Retrieves the validation message displayed when the input value exceeds the maximum allowed.
     * </p>
     *
     * @return The maximum value validation message, or null if not configured.
     */
    public String getMaxMessage() {
        return maxMessage;
    }

    /**
     * <p>
     * Retrieves the validation message displayed when the input date exceeds the maximum allowed date.
     * </p>
     *
     * @return The maximum date validation message, or null if not configured.
     */
    public String getMaxDateMessage() {
        return maxDateMessage;
    }

    /**
     * <p>
     * Retrieves the validation message displayed when the number of entries exceeds the maximum limit.
     * </p>
     *
     * @return The maximum limit validation message, or null if not configured.
     */
    public String getMaxLimitMessage() {
        return maxLimitMessage;
    }

    /**
     * <p>
     * Adds the value to the specified {@link Configuration}.
     * </p>
     *
     * @param configuration Represents the {@link Configuration}
     * @param value         Represents the field value
     */
    public void add(final Configuration configuration, final Object value) {

        switch (configuration) {
            case IS_DEFAULT_FIELD -> isDefaultField = (boolean) value;
            case ADD_VIEW -> addView = (boolean) value;
            case REQUIRED -> required = (boolean) value;
            case DEFAULT -> {}
            case ORDER -> order = (int) value;
            case IMMUTABLE -> immutable = (boolean) value;
            case ROW_INDEX_ON_VIEW -> rowOnIndexView = (int) value;
            case ORDER_ON_VIEW -> orderOnView = (int) value;
            case DROPDOWN -> dropdown = (String) value;
            case DROPDOWN_TYPE -> dropdownType = (String) value;
            case SHOW_ON_PROFILE -> showOnProfile = (boolean) value;
            case ROW_INDEX -> rowIndex = (int) value;
            case DEPENDS_ON -> dependsOn = (String) value;
            case MAX -> max = (long) value;
            case MIN -> min = (int) value;
            case IS_IMMOVABLE -> isImmovable = (boolean) value;
            case READ_ONLY -> readOnly = (boolean) value;
            case IS_FILTERABLE -> isFilterable = (boolean) value;
            case IS_HIDDEN -> isHidden = (boolean) value;
            case EDITABLE_VIA_LIST -> editableViaList = (boolean) value;
            case GROUP_ID -> groupId = (int) value;
            case HAS_MAPPER_TABLE -> hasMapperTable = (boolean) value;
            case IS_FIELD_ENABLED -> isFieldEnabled = (boolean) value;
            case MAX_LIMIT -> maxLimit = (int) value;
            case IS_DEFAULT_SYSTEM_FIELD -> isDefaultSystemField = (boolean) value;
            case MAX_LENGTH -> maxLength = (int) value;
            case MIN_LENGTH -> minLength = (int) value;
            case IS_PRODUCT_ENABLED -> isProductEnabled = (boolean) value;
            case IS_GROUPED_FIELD -> isGroupedField = (boolean) value;
            case IS_HIDDEN_FOR_MOBILE -> isHiddenForMobile = (boolean) value;
            case MOBILE_CLIENT_ROW_INDEX -> mobileClientRowIndex = (int) value;
        }
        configJson.put(configuration.getName(), value);
    }

    /**
     * <p>
     * Retrieves the min length in the preset config
     * </p>
     *
     * @return If the min key present it will return the original value, otherwise it returns the default value
     */
    public int minLength() {
        return minLength;
    }

    /**
     * <p>
     * Retrieves the max length in the preset config
     * </p>
     *
     * @return If the max key present it will return the original value, otherwise it returns the default value
     */
    public int maxLength() {
        return maxLength;
    }

    /**
     * <p>
     * Retrieves the filter dropdown name in the preset config
     * </p>
     *
     * @return the filter dropdown name
     */
    public String filterDropdown() {
        return filterDropdown;
    }

    /**
     * <p>
     * Retrieves the filter field type in the preset config
     * </p>
     *
     * @return the filter field type
     */
    public String getFilterFieldType() {
        return filterFieldType;
    }

    /**
     * <p>
     * Checks if the field is grouped
     * </p>
     *
     * @return true if the field is grouped, false otherwise
     */
    public boolean isGroupedField() {
        return isGroupedField;
    }

    /**
     * <p>
     * Checks if the field is grouped
     * </p>
     *
     * @return true if the field is grouped, false otherwise
     */
    public boolean isFieldGroupHead() {
        return isFieldGroupHead;
    }

    /**
     * <p>
     * Checks if the field is hidden for mobile view
     * </p>
     *
     * @return true if the field is hidden for mobile view
     */
    public boolean isHiddenForMobile() {
        return isHiddenForMobile;
    }

    /**
     * <p>
     * Checks whether configuration is contains or not
     * </p>
     *
     * @param configuration Represents the {@link Configuration}
     * @return If the configuration exists it will return true, otherwise false
     */
    public boolean contains(final Configuration configuration) {
        return configJson.containsKey(configuration.getName());
    }

    /**
     * <p>
     * Fetches the maximum value
     * </p>
     *
     * @return An {@link Optional} containing the maximum value if applicable,
     * or {@link Optional} if not required or not configured
     */
    public Optional<Long> getMaxValue() {
        final String maxValueKey = Configuration.MAX_VALUE.getName();

        if (configJson.containsKey(maxValueKey)) {
            final String maxValue = configJson.optString(maxValueKey, null);

            if (Objects.nonNull(maxValue)) {
                final CrmUser crmUser = LoggedInUserInfo.getUser();
                final UserRegionalPreference userRegionalPreference = crmUser.getPreferenceHolder().get(UserRegionalPreference.TYPE);

                if (Objects.equals(MaxValue.END_OF_THE_DAY.getName(), maxValue)) {
                    return Optional.ofNullable(TemporalServiceProvider.get().getEndTimeOfDay(userRegionalPreference.getTimezoneInfo()));
                }
            } else {
                return Optional.of(configJson.getLong(maxValueKey));
            }
        }
        return Optional.empty();
    }

    /**
     * <p>
     * Gets the row index for mobile.
     * </p>
     *
     * @return the row index for mobile.
     */
    public int mobileClientRowIndex() {
        return mobileClientRowIndex;
    }

    /**
     * Provides the field accessing key for the request data.
     *
     * @return Request data field access key.
     */
    public String requestDataAccessKey() {
        return requestDataAccessKey;
    }

    /**
     * Provides the field accessing key for the response data.
     *
     * @return Response data field access key.
     */
    public String responseDataAccessKey() {
        return responseDataAccessKey;
    }

    /**
     * <p>
     * Checks if the field is primary or not.
     * </p>
     *
     * @return true if the field is primary.
     */
    public boolean isPrimaryField() {
        return isPrimaryField;
    }

    /**
     * <p>
     * Checks if the field is name field or not.
     * </p>
     *
     * @return true if the field is name field
     */
    public boolean isNameField() {
        return isNameField;
    }

    /**
     * Checks whether to override the required validation for the field.
     */
    public boolean overrideRequired() {
        return overrideRequired;
    }

    /**
     * Checks whether it has override the required validation for the field or not.
     */
    public boolean hasOverrideRequired() {
        return hasOverrideRequired;
    }

    /**
     * An enum which contains the {@link Configuration} data
     */
    public enum Configuration {

        /**
         * Denotes whether the field is a default one.
         */
        IS_DEFAULT_FIELD("isDefaultField", true),
        /**
         * Denotes whether user can opt to view the field.
         */
        ADD_VIEW("addView", false),
        /**
         * Denotes whether the field is required.
         */
        REQUIRED("required", false),
        /**
         * Denotes the default value of the field.
         */
        DEFAULT("default", false),
        /**
         * Denotes the order of the field.
         */
        ORDER("order", true),
        /**
         * Denotes whether the field is immutable.
         */
        IMMUTABLE("immutable", true),
        /**
         * Denotes the row index for a field in summary.
         */
        ROW_INDEX_ON_VIEW("rowIndexOnView", true),
        /**
         * Denotes the order for a field in summary.
         */
        ORDER_ON_VIEW("orderOnView", true),
        /**
         * Denotes whether field is of dropdown type and if it is we add the field's name.
         */
        DROPDOWN("dropdown", true),
        /**
         * Denotes the dropdown type if a field is of dropdown.
         */
        DROPDOWN_TYPE("dropdownType", true),
        /**
         * Denotes whether the field is to be shown on the profile.
         */
        SHOW_ON_PROFILE("showOnProfile", true),
        /**
         * Denotes the row index for form renderer.
         */
        ROW_INDEX("rowIndex", true),
        /**
         * Denotes whether the field is depends on another.
         */
        DEPENDS_ON("dependsOn", true),
        /**
         * Denotes the max value for a numerical field.
         */
        MAX("max", false),
        /**
         * Denotes the min value for a numerical field.
         */
        MIN("min", false),
        /**
         * Denotes the sample value.
         */
        PLACE_HOLDER("placeHolder", true),
        /**
         * Denotes whether the field is applicable for drag & drop.
         */
        IS_IMMOVABLE("isImmovable", true),
        /**
         * Denotes whether the product is enabled.
         */
        IS_PRODUCT_ENABLED("isProductEnabled", false),
        /**
         * Denotes whether the field is readable and cannot be added.
         */
        READ_ONLY("readOnly", true),
        /**
         * Denotes whether the field is applicable for filter purpose.
         */
        IS_FILTERABLE("isFilterable", true),
        /**
         * Denotes whether the field can be hidden.
         */
        IS_HIDDEN("isHidden", false),
        /**
         * Denotes if the field can be shown in the update field list API
         */
        EDITABLE_VIA_LIST("editableViaList", true),
        /**
         * Denotes whether any field is dependable on this field.
         */
        GROUP_ID("groupId", true),
        /**
         * Denotes whether the field has mapper table.
         */
        HAS_MAPPER_TABLE("hasMapperTable", true),
        /**
         * Denotes whether the field is enabled for data fields.
         */
        IS_FIELD_ENABLED("isFieldEnabled", true),
        /**
         * Denotes the maximum values for a field (For ex. Emails field can have 10 max emails)
         */
        MAX_LIMIT("maxLimit", true),
        /**
         * Denotes whether the field is default i.e., can't be unselected
         */
        IS_DEFAULT_SYSTEM_FIELD("isDefaultSystemField", true),
        /**
         * Denotes the min length of the field
         */
        MIN_LENGTH("minLength", true),
        /**
         * Denotes the max length of the field
         */
        MAX_LENGTH("maxLength", true),
        /**
         * Denotes whether the products are available or not
         */
        PRODUCTS_AVAILABLE("productsAvailable", true),
        /**
         * Denotes whether the field is grouped
         */
        IS_GROUPED_FIELD("isGroupedField", true),
        /**
         * Denotes the filter dropdown name
         */
        FILTER_DROPDOWN("filterDropdown", true),
        /**
         * Denotes the filter dropdown type
         */
        FILTER_DROPDOWN_TYPE("filterDropdownType", true),

        /**
         * Denotes the regex to validate the field.
         */
        REGEX("regex", true),

        /**
         * Denotes the filter field type to validate the field.
         */
        FILTER_FIELD_TYPE("filterFieldType", true),

        /**
         * Denotes the referrer fields.
         */
        REFERRER_FIELDS("referrerFields", true),

        /**
         * Denotes the validation and its messages of the fields.
         */
        VALIDATION("validation", true),

        /**
         * Denotes whether the field is dynamic
         */
        IS_DYNAMIC_FIELD("isDynamicField", true),

        /**
         * Denotes the max value fields
         */
        MAX_VALUE("maxValue", true),

        /**
         * Denotes the list field type
         */
        LIST_FIELD_TYPE("listFieldType", true),

        /**
         * Denotes the field accessible key for the request data.
         */
        REQUEST_DATA_ACCESS_KEY("requestDataAccessKey", true),

        /**
         * Denotes the field accessible key for the response data.
         */
        RESPONSE_DATA_ACCESS_KEY("responseDataAccessKey", true),

        /**
         * Denotes the field is primary or not
         */
        IS_PRIMARY_FIELD("isPrimaryField", true),

        /**
         * Denotes the field is shown in mobile view or not
         */
        IS_HIDDEN_FOR_MOBILE("isHiddenForMobile", true),

        IS_FIELD_GROUP_HEAD("isFieldGroupHead", true),
        /**
         * Denotes the row index for form renderer for mobile client.
         */
        MOBILE_CLIENT_ROW_INDEX("mobileClientRowIndex", true),

        /**
         * Denotes the atomation config of the field.
         */
        AUTOMATION_CONFIG("automationConfig", true),

        /**
         * Denotes the summary config to display in the related section.
         */
        SUMMARY("summary", true),

        /**
         * Denotes the mobile summary config to display in the related section.
         */
        MOBILE_SUMMARY("mobileSummary", true),

        /**
         * Denotes the summary category to display in the related section.
         */
        CATEGORY("category", true),

        /**
         * Denotes the summary subcategory to display in the related section.
         */
        SUB_CATEGORY("subCategory", true),

        FIELD_TYPE("fieldType", true),

        IS_DUPLICATE_CHECK_FIELD("isDuplicateCheckEnabled", true),

        IS_DEFAULT_RULE_CRITERIA_FIELD("isDefaultRuleCriteriaField", true),
        PREFIX("prefix", true),
        START_NUMBER("startNumber", true),
        NO_OF_DIGITS("noOfDigits", true),

        IS_DEPENDABLE_FIELD_ALWAYS_VISIBLE("isDependableFieldAlwaysVisible", false),
        PATTERN("pattern", true),
        MAX_DATE_MESSAGE("maxDateMessage", true),
        MAX_LIMIT_MESSAGE("maxLimitMessage", true),
        REQUIRED_MESSAGE("requiredMessage", true),
        MAX_MESSAGE("maxMessage", true),
        MIN_MESSAGE("minMessage", true),
        MAX_LENGTH_MESSAGE("maxLengthMessage", true),
        MIN_LENGTH_MESSAGE("minLengthMessage", true),
        IS_NAME_FIELD("isNameField", true),
        OVERRIDE_REQUIRED("overrideRequired", true),
        ;

        private final String name;
        private final boolean isReadable;

        Configuration(final String name, final boolean isReadable) {
            this.name = name;
            this.isReadable = isReadable;
        }

        public String getName() {
            return name;
        }

        public boolean isReadable() {
            return isReadable;
        }
    }

    /**
     * <p>
     * Which contains max value types
     * </p>
     */
    enum MaxValue {
        END_OF_THE_DAY("endOfTheDay");

        private final String name;

        MaxValue(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
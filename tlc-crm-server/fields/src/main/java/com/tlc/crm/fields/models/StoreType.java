package com.tlc.crm.fields.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.fields.internal.status.FieldErrorCodes;

import java.util.Optional;

/**
 * <p>
 * Represents the types of field stores supported
 * </p>
 *
 * @author Selvakumar G
 * @version 1.0
 */
public enum StoreType {

    SINGLE_STRING("SINGLE_STRING", 1),
    TEXT("TEXT", 2),
    SINGLE_NUMBER("SINGLE_NUMBER", 3),
    DROPDOWN("DROPDOWN", 4),
    MULTI_SELECT("MULTI_SELECT", 5),
    OWNER("OWNER", 6),
    CONTACT("CONTACT", 7),
    COMPANY("COMPANY", 8),
    PHONE("PHONE", 9),
    EMAIL("EMAIL", 10),
    MONETARY("MONETARY", 11),
    TIME("TIME", 12),
    TIME_RANGE("TIME_RANGE", 13),
    DATE("DATE", 14),
    DATE_RANGE("DATE_RANGE", 15),
    DATE_TIME_RANGE("DATE_TIME_RANGE", 16),
    ADDRESS("ADDRESS", 17),
    COMPANY_ONBOARD("COMPANY_ONBOARD", 18),
    CONTACT_ONBOARD("CONTACT_ONBOARD", 19),
    DEAL_VALUE("DEAL_VALUE", 20),
    VARIATION("VARIATION", 21),
    PRICE_TAGS("PRICE_TAGS", 22),
    CONTACTS_ONBOARD("CONTACTS_ONBOARD", 23),
    PHONES("PHONES", 24),
    EMAILS("EMAILS", 25),
    DEAL("DEAL", 26),
    PRODUCT("PRODUCT", 27), // It is an input type of deal value subtype
    TERRITORY("TERRITORY", 28),
    TAGS("TAGS", 29),
    ICON("ICON", 30),
    NOTE("NOTE", 31),
    OWNERS("OWNERS", 32),
    DATE_TIME("DATE_TIME", 33),
    IDENTIFIER("IDENTIFIER", 34),
    AUTO_NUMBER("AUTO_NUMBER", 35),
    MARKETING_LIST("MARKETING_LIST", 36),
    BOOLEAN("BOOLEAN", 37),
    ;

    private final String name;
    private final int type;

    StoreType(final String name, final int type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Gets the type
     *
     * @return type number
     */
    public int getType() {
        return this.type;
    }

    /**
     * Gets the name of store type
     *
     * @return name
     */
    public String getName() {
        return this.name;
    }

    public static boolean isSortable(final StoreType storeType) {
        return switch (storeType) {
            case SINGLE_STRING, SINGLE_NUMBER, DATE, TIME, IDENTIFIER, DROPDOWN, CONTACT, COMPANY, COMPANY_ONBOARD,
                 CONTACT_ONBOARD, PHONE, EMAIL, MONETARY, TIME_RANGE, DATE_RANGE, DATE_TIME_RANGE,
                 ADDRESS, DEAL_VALUE, TERRITORY, DATE_TIME, EMAILS, PHONES, DEAL, OWNER, AUTO_NUMBER -> true;
            default -> false;
        };
    }

    /**
     * Gets the {@link StoreType} by type
     *
     * @param type represents the type ID
     * @return {@link StoreType}
     */
    public static StoreType valueOf(final int type) {
        return switch (type) {
            case 1 -> SINGLE_STRING;
            case 2 -> TEXT;
            case 3 -> SINGLE_NUMBER;
            case 4 -> DROPDOWN;
            case 5 -> MULTI_SELECT;
            case 6 -> OWNER;
            case 7 -> CONTACT;
            case 8 -> COMPANY;
            case 9 -> PHONE;
            case 10 -> EMAIL;
            case 11 -> MONETARY;
            case 12 -> TIME;
            case 13 -> TIME_RANGE;
            case 14 -> DATE;
            case 15 -> DATE_RANGE;
            case 16 -> DATE_TIME_RANGE;
            case 17 -> ADDRESS;
            case 18 -> COMPANY_ONBOARD;
            case 19 -> CONTACT_ONBOARD;
            case 20 -> DEAL_VALUE;
            case 21 -> VARIATION;
            case 22 -> PRICE_TAGS;
            case 23 -> CONTACTS_ONBOARD;
            case 24 -> PHONES;
            case 25 -> EMAILS;
            case 26 -> DEAL;
            case 27 -> PRODUCT;
            case 28 -> TERRITORY;
            case 29 -> TAGS;
            case 30 -> ICON;
            case 31 -> NOTE;
            case 32 -> OWNERS;
            case 33 -> DATE_TIME;
            case 34 -> IDENTIFIER;
            case 35 -> AUTO_NUMBER;
            case 36 -> MARKETING_LIST;
            case 37 -> BOOLEAN;
            default -> throw ErrorCode.get(FieldErrorCodes.INVALID_CRM_FIELD_STORE_TYPE);
        };
    }

    /**
     * <p>
     * Checks whether the given store type is used to store single value or not
     * </p>
     *
     * @param storeType Type of store
     * @return If the store type can hold only single value, it will return true otherwise false will be returned
     */
    public static boolean isSingleValueStore(final StoreType storeType) {
        return switch (storeType) {
            case SINGLE_STRING, TEXT, SINGLE_NUMBER, TIME, DATE, IDENTIFIER, AUTO_NUMBER -> true;
            default -> false;
        };
    }

    /**
     * <p>
     * Checks whether the given store type is used to store multi values or not
     * </p>
     *
     * @param storeType Type of store
     * @return If the store type can hold multi values, it will return true otherwise false will be returned
     */
    public static boolean isMultiValueStore(final StoreType storeType) {
        return switch (storeType) {
            case MULTI_SELECT, PRICE_TAGS, VARIATION -> true;
            default -> false;
        };
    }

    /**
     * Checks whether the {@link StoreType} is object store or not
     *
     * @param storeType Type of store
     * @return true if the given {@link StoreType} is object store
     */
    public static boolean isModelStore(final StoreType storeType) {
        return switch (storeType) {
            case OWNER, COMPANY, CONTACT, TIME_RANGE, DATE_RANGE, DATE_TIME_RANGE, ADDRESS, EMAIL, PHONE,
                    MONETARY, DROPDOWN, EMAILS, PHONES -> true;
            default -> false;
        };
    }

    /**
     * Checks whether the Store type is entity type or not
     *
     * @return true if the store type is entity type
     */
    public boolean isEntityType() {
        return switch (this) {
            case COMPANY, CONTACT, COMPANY_ONBOARD, CONTACT_ONBOARD, CONTACTS_ONBOARD -> true;
            default -> false;
        };
    }

    public boolean hasSubtypes() {
        return switch (this) {
            case DATE_RANGE, TIME_RANGE, DATE_TIME_RANGE, ADDRESS, EMAIL, PHONE, MONETARY, EMAILS, PHONES, DEAL_VALUE -> true;
            default -> false;
        };
    }

    /**
     * Checks whether the Store type is source depended on or not
     *
     * @return true if the store type is default
     */
    public static boolean isSourceDependentType(final StoreType storeType) {
        return switch (storeType) {
            case CONTACT_ONBOARD, CONTACTS_ONBOARD, COMPANY_ONBOARD, DEAL_VALUE, VARIATION, PRICE_TAGS, EMAILS, PHONES,
                 DEAL, ICON, TAGS, TERRITORY, PRODUCT, NOTE, OWNERS, DATE_TIME, IDENTIFIER, MARKETING_LIST -> true;
            default -> false;
        };
    }

    /**
     * Checks whether the source dependent field type has subType or not
     *
     * @return true if the default field type has subType
     */
    public boolean hasSourceDependentFieldSubTypes() {
        return switch (this) {
            case VARIATION, PRICE_TAGS -> true;
            default -> false;
        };
    }

    /**
     * <p>
     * Checks whether the given store type is used to store multi values or not
     * </p>
     *
     * @return If the store type can hold multi values, it will return true otherwise false will be returned
     */
    public boolean isMultiSelectType() {
        return switch (this) {
            case MULTI_SELECT, CONTACTS_ONBOARD -> true;
            default -> false;
        };
    }

    /**
     * Checks whether the Store type is array type or not
     *
     * @return true if the store type is array type
     */
    public boolean isArrayType() {
        return switch (this) {
            case PHONES, EMAILS, CONTACTS_ONBOARD, PRICE_TAGS, OWNERS -> true;
            default -> false;
        };
    }

    /**
     * Checks whether the store type corresponds to a selector,
     * which could be either {@link #DROPDOWN} or {@link #MULTI_SELECT}.
     *
     * @param storeType Refers the storetype.
     * @return true if the store type corresponds to a selector.
     */
    public static boolean isSelectorType(final StoreType storeType) {
        return switch (storeType) {
            case DROPDOWN, MULTI_SELECT -> true;
            default -> false;
        };
    }

    /**
     * Checks whether the store type corresponds to the monetary.
     *
     * @return true if the store type related to monetary; otherwise false.
     */
    public boolean isMonetaryType() {
        return switch (this) {
            case MONETARY, DEAL_VALUE -> true;
            default -> false;
        };
    }

    /**
     * Checks whether the store type corresponds to the owner.
     *
     * @return true if the store type related to owner; otherwise false.
     */
    public boolean isOwnerType() {
        return switch (this) {
            case OWNER, OWNERS -> true;
            default -> false;
        };
    }

    /**
     * Checks whether the store type is a contact type.
     *
     * @return true if the store type is contact-related; otherwise false.
     */
    public boolean isContactType() {
        return switch (this) {
            case CONTACT, CONTACT_ONBOARD, CONTACTS_ONBOARD -> true;
            default -> false;
        };
    }

    /**
     * Checks whether the store type is a company type.
     *
     * @return true if the store type is company-related; otherwise false.
     */
    public boolean isCompanyType() {
        return switch (this) {
            case COMPANY, COMPANY_ONBOARD -> true;
            default -> false;
        };
    }

    /**
     * Checks if the current store type is of a range type.
     *
     * @return {@code true} if the store type is a range {@code false} otherwise.
     */
    public boolean isRangeType() {
        return switch (this) {
            case DATE_RANGE, TIME_RANGE, DATE_TIME_RANGE -> true;
            default -> false;
        };
    }

    /**
     * <p>
     * Checks whether the Store type is option value containable or not
     * </p>
     *
     * @return true if the store type is option value containable type
     */
    public boolean isOptionValueContainable() {
        return switch (this) {
            case COMPANY, CONTACT, COMPANY_ONBOARD, CONTACT_ONBOARD, CONTACTS_ONBOARD, DROPDOWN, OWNER -> true;
            default -> false;
        };
    }

    /**
     * <p>
     * Resolves the {@link StoreType} to its base type
     * </p>
     *
     * @return The {@link StoreType}
     */
    public StoreType getBaseType() {
        return switch (this) {
            case CONTACT_ONBOARD -> CONTACT;
            case COMPANY_ONBOARD -> COMPANY;
            case DEAL_VALUE -> MONETARY;
            default -> this;
        };
    }

    /**
     * <p>
     * Finds the store type which is related to the source.
     * </p>
     *
     * @param source Represents the store type/ store subtype.
     * @return Source.
     */
    public static Optional<StoreType> getTypeBySource(final String source) {
        return Optional.ofNullable(switch (source.toLowerCase()) {
            case "contact" -> StoreType.CONTACT;
            case "company" -> StoreType.COMPANY;
            case "deal" -> StoreType.DEAL;
            case "product" -> StoreType.PRODUCT;
            default -> null;
        });
    }

    /**
     * <p>
     * Checks if a given type name exists in the {@link StoreType}
     * </p>
     *
     * @param typeName Represents the store type name
     * @return true if the type name exists, false otherwise.
     */
    public static boolean exists(final String typeName) {

        try {
            StoreType.valueOf(typeName);
            return true;
        } catch (final Exception exception) {
            return false;
        }
    }

    public boolean isDateType() {
        return switch (this) {
            case DATE, DATE_TIME -> true;
            default -> false;
        };
    }

    /**
     * <p>
     * Checks whether the store type is temporal or not
     * </p>
     *
     * @return true if the store type is temporal, false otherwise
     */
    public boolean isTemporalType() {
        return switch (this) {
            case DATE, TIME, DATE_TIME, DATE_RANGE, TIME_RANGE, DATE_TIME_RANGE -> true;
            default -> false;
        };
    }

    /**
     * <p>
     * Checks whether the store type is phone type or not
     * </p>
     *
     * @return true if the store type is phone type, false otherwise
     */
    public boolean isPhoneType() {
        return switch (this) {
            case PHONE, PHONES -> true;
            default -> false;
        };
    }

    /**
     * <p>
     * Checks whether the store type is text type or not
     * </p>
     *
     * @return true if the store type is text type, false otherwise
     */
    public boolean isTextType() {
        return switch (this) {
            case TEXT, NOTE -> true;
            default -> false;
        };
    }
}

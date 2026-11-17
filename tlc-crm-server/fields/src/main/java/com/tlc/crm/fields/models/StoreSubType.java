package com.tlc.crm.fields.models;

/**
 * <p>
 * Represents the subtypes of field types supported
 * </p>
 *
 * @author sundar
 * @version 1.0
 */
public enum StoreSubType {
    PHONE_TYPE("phoneType", "phone_type", 1),
    COUNTRY_CODE("countryCode", "country_code", 2),
    PHONE_NUMBER("phoneNumber", "phone_number",3),
    EMAIL_TYPE("emailType", "email_type",4),
    EMAIL_VALUE("emailValue", "email_value",5),
    CURRENCY("currency", "currency",6),
    VALUE("value", "value",7),
    FROM("from", "from",8),
    TO("to", "to",9),
    ADDRESS_LINE1("addressLine1", "address_line1",10),
    ADDRESS_LINE2("addressLine2", "address_line2",11),
    CITY("city", "city",12),
    STATE("state", "state",13),
    COUNTRY("country", "country",14),
    ZIPCODE("zipcode", "zipcode",15),
    LATITUDE("latitude", "latitude",16),
    LONGITUDE("longitude", "longitude",17),
    PRICE_TAGS("priceTags", "price_tags",18),
    NAME("name", "name",19),
    UNIT_PRICE("unitPrice", "unit_price",20),
    TAX("tax", "tax",21),
    DISCOUNT("discount", "disount",22),
    PRODUCT("product", "",23),
    ;

    private final String name;
    private final String aliasName;
    private final String apiPropertyName;
    private final int type;

    StoreSubType(final String aliasName, final String apiPropertyName, final int type) {
        this.name = name();
        this.aliasName = aliasName;
        this.apiPropertyName = apiPropertyName;
        this.type = type;
    }

    /**
     * Gets the corresponding key
     *
     * @return the key
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the corresponding alias name.
     *
     * @return The alias name.
     */
    public String getAliasName() {
        return this.aliasName;
    }

    /**
     * Returns the API property name associated with this field.
     *
     * @return the API property name
     */
    public String getApiPropertyName() {
        return apiPropertyName;
    }

    public static StoreSubType get(final String aliasName) {

        return switch (aliasName) {
            case "phoneType" -> PHONE_TYPE;
            case "countryCode" -> COUNTRY_CODE;
            case "phoneNumber" -> PHONE_NUMBER;
            case "emailType" -> EMAIL_TYPE;
            case "emailValue" -> EMAIL_VALUE;
            case "currency" -> CURRENCY;
            case "value" -> VALUE;
            case "from" -> FROM;
            case "to" -> TO;
            case "city" -> CITY;
            case "state" -> STATE;
            case "country" -> COUNTRY;
            case "zipcode" -> ZIPCODE;
            case "addressLine1" -> ADDRESS_LINE1;
            case "addressLine2" -> ADDRESS_LINE2;
            case "latitude" -> LATITUDE;
            case "longitude" -> LONGITUDE;
            case "product" -> PRODUCT;
            case "name" -> NAME;
            default -> null;
        };
    }

    /**
     * Gets the corresponding type
     *
     * @return the type
     */
    public int getType() {
        return this.type;
    }
}

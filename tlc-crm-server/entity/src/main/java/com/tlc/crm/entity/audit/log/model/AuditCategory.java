package com.tlc.crm.entity.audit.log.model;

/**
 * <p>
 * A model that represents the category of the audit log.
 * </p>
 *
 * @author Abishek
 * @version 1.0
 */
public enum AuditCategory {

    CONTACT((short) 1, "Contact", "contact"),
    COMPANY((short) 2, "Company", "company"),
    DEAL((short) 3, "Deal", "deal"),
    PRODUCT((short) 4, "Product", "product"),
    ACTIVITY((short) 5, "Activity", "activity"),
    ;

    private final short id;
    private final String name;
    private final String iconName;

    AuditCategory(final short id, final String name, final String iconName) {
        this.id = id;
        this.name = name;
        this.iconName = iconName;
    }

    public short getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIconName() {
        return iconName;
    }

    public static AuditCategory get(final String name) {
        return switch (name) {
            case "contact", "CONTACT", "Contact" -> CONTACT;
            case "company", "COMPANY", "Company" -> COMPANY;
            case "deal", "DEAL", "Deal" -> DEAL;
            case "product", "PRODUCT", "Product" -> PRODUCT;
            case "activity", "ACTIVITY", "Activity" -> ACTIVITY;
            default -> null;
        };
    }

    public static AuditCategory get(final short id) {
        return switch (id) {
            case 1 -> CONTACT;
            case 2 -> COMPANY;
            case 3 -> DEAL;
            case 4 -> PRODUCT;
            case 5 -> ACTIVITY;
            default -> null;
        };
    }
}

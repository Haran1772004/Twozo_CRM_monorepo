package com.tlc.crm.common.models;

/**
 * <p>
 * Represents all the indexes.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public enum SearchIndex {

    DEAL("deal"),
    CONTACT("contact"),
    COMPANY("company"),
    ACTIVITY("activity"),
    PRODUCT("product"),
    FILE("file"),
    NOTE("note"),
    ;

    private final String name;

    SearchIndex(final String name) {
        this.name = name;
    }

    /**
     * <p>
     * Gets the name of the state
     * </p>
     *
     * @return the name of State
     */
    public String getName() {
        return this.name;
    }

    /**
     * <p>
     * Gets {@link SearchIndex} .
     * </p>
     *
     * @param indexName input value referring category name.
     * @return {@link SearchIndex}.
     */
    public static SearchIndex getSearchIndex(final String indexName) {

        return switch (indexName.toLowerCase()) {
            case "deal" -> DEAL;
            case "contact" -> CONTACT;
            case "company" -> COMPANY;
            case "activity" -> ACTIVITY;
            case "product" -> PRODUCT;
            case "file" -> FILE;
            case "note" -> NOTE;
            default -> null;
        };
    }
}

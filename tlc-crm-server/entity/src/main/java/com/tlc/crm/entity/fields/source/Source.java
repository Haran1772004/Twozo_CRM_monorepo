package com.tlc.crm.entity.fields.source;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * Holds the source names.
 *
 * @author KarthickV
 * @version 1.0
 * <p>
 * Caution: The functionality of data import varies based on the source ID
 */
public enum Source {

    COMPANY("Company", (short) 1),
    CONTACT("Contact", (short) 2),
    DEAL("Deal", (short) 3),
    PRODUCT("Product", (short) 4),
    NOTE("Note", (short) 5),
    ACTIVITY("Activity", (short) 6),
    REPORTS("Reports", (short) 7),
    REPORTS_TRASH( "ReportsTrash", (short) 8),
    QUOTA_TEAM_VIEW("QuotaTeamView", (short) 9),
    QUOTA_HIERARCHY_VIEW("QuotaHierarchyView", (short) 10),
    QUOTATION("Quotation", (short) 11),
    INVOICE("Invoice", (short) 12),
    ;

    private final String name;
    private final short id;

    Source(final String name, final short id) {
        this.name = name;
        this.id = id;
    }

    /**
     * Checks whether the given source is registered or not
     *
     * @param source Represents the source to be checked
     * @return true if the given source is registered
     */
    public static boolean isPrimarySource(final Source source) {

        return switch (source) {
            case COMPANY, CONTACT, DEAL, PRODUCT -> true;
            default -> false;
        };
    }

    /**
     * Fetches the registered sources.
     *
     * @return Registered sources.
     */
    public static Collection<String> getPrimarySourceNames() {
        return Set.of(COMPANY.name, CONTACT.name, DEAL.name, PRODUCT.name);
    }

    /**
     * Gets the ID of the source
     *
     * @return the ID of the source
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the {@link Source} by type
     *
     * @param type An Integer value that represents the type of the source
     * @return the appropriate {@link Source} of provide type
     */
    public static Source get(final int type) {
        return switch (type) {
            case 1 -> COMPANY;
            case 2 -> CONTACT;
            case 3 -> DEAL;
            case 4 -> PRODUCT;
            case 5 -> NOTE;
            case 6 -> ACTIVITY;
            case 7 -> REPORTS;
            case 8 -> REPORTS_TRASH;
            case 9 -> QUOTA_TEAM_VIEW;
            case 10 -> QUOTA_HIERARCHY_VIEW;
            case 11 -> QUOTATION;
            case 12 -> INVOICE;
            default -> throw ErrorCode.get(EntityErrorCodes.INVALID_SOURCE);
        };
    }

    /**
     * Gets the name of the source
     *
     * @return source name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the {@link Source} by the name
     *
     * @param name Represents the source name
     * @return {@link Source}
     */
    public static Source get(final String name) {
        return switch (name.toLowerCase()) {
            case "company" -> COMPANY;
            case "contact" -> CONTACT;
            case "deal" -> DEAL;
            case "product" -> PRODUCT;
            case "note" -> NOTE;
            case "activity" -> ACTIVITY;
            case "reports" -> REPORTS;
            case "reportstrash" -> REPORTS_TRASH;
            case "quotateamview" -> QUOTA_TEAM_VIEW;
            case "quotahierarchyview" -> QUOTA_HIERARCHY_VIEW;
            case "quotation" -> QUOTATION;
            case "invoice" -> INVOICE;
            default -> throw ErrorCode.get(EntityErrorCodes.INVALID_SOURCE);
        };
    }

    /**
     * <p>
     *     check whether the current source is contact or not
     * </p>
     *
     * @return boolean whether the source is contact or not
     */
    public boolean isContact() {
        return Objects.equals(this, CONTACT);
    }

    /**
     * <p>
     *     check whether the current source is company or not
     * </p>
     *
     * @return boolean whether the source is company or not
     */
    public boolean isCompany() {
        return Objects.equals(this, COMPANY);
    }
}

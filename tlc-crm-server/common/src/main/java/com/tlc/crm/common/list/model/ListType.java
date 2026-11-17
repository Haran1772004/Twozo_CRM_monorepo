package com.tlc.crm.common.list.model;

/**
 * <p>
 * This enum represents the type of list actions
 * </p>
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public enum ListType {

    CONTACT_LIST((short) 1),
    COMPANY_LIST((short) 2),
    DEAL_LIST((short) 3),
    PRODUCT_LIST((short) 4),
    CONTACT_SUMMARY_COMPANY_LIST((short) 5),
    CONTACT_SUMMARY_DEAL_LIST((short) 6),
    COMPANY_SUMMARY_CONTACT_LIST((short) 7),
    COMPANY_SUMMARY_DEAL_LIST((short) 8),
    DEAL_SUMMARY_CONTACT_LIST((short) 9),
    DEAL_SUMMARY_PRODUCT_LIST((short) 10),
    PRODUCT_SUMMARY_DEAL_LIST((short) 11),
    ACTIVITY_LIST((short) 12),
    DEAL_ARCHIVE_LIST((short) 13),
    ACTIVITY_GOAL_LIST((short) 14),
    QUOTA_DEAL_LIST((short)15),
    DEAL_QUOTATION_LIST((short)16),
    MARKETING_CONTACT_LIST((short) 17),
    CONTACT_QUOTATION_LIST((short) 18),
    COMPANY_QUOTATION_LIST((short) 19),
    SEGMENT_CONTACT_LIST((short) 20),
    USER_LIST((short) 21),
    ROLE_LIST((short) 22),
    TEAM_LIST((short) 23),
    TERRITORY_LIST((short) 24),
    CONTACT_INVOICE_LIST((short) 25),
    COMPANY_INVOICE_LIST((short) 26),
    DEAL_INVOICE_LIST((short) 27),
    TAG_LIST((short) 28),
    TEMPLATE_TAG_LIST((short) 29),
    ;

    private final Short id;

    ListType(final Short id) {
        this.id = id;
    }

    public Short getId() {
        return id;
    }
}
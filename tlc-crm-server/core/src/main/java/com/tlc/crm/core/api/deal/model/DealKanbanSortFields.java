package com.tlc.crm.core.api.deal.model;

/**
 * An enum contains the kanban view sort field data
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public enum DealKanbanSortFields {

    DEAL_TITLE(1, "dealTitle"),
    DEAL_VALUE(2, "dealValue"),
    CONTACT_NAME(3, "contactName"),
    COMPANY_NAME(4, "companyName"),
    SALES_OWNER(5, "salesOwner"),
    DEAL_CLOSED_ON(6, "dealClosedOn"),
    CREATED_BY(7, "createdBy"),
    UPDATED_BY(8, "updatedBy"),
    LAST_ACTIVITY_DATE(9, "lastActivityDate"),
    UPDATED_AT(10, "updatedAt"),
    CREATED_AT(11, "createdAt"),
    ;

    private final int id;
    private final String name;

    DealKanbanSortFields(final int id, final String name) {
        this.name = name;
        this.id = id;
    }

    /**
     * <p>
     * Retrieves the id of the field
     * </p>
     *
     * @return The id of the field
     */
    public int getId() {
        return id;
    }

    /**
     * <p>
     * Retrieves the name of the field
     * </p>
     *
     * @return The name of the field
     */
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Gets the {@link DealKanbanSortFields} based on the provided id
     * </p>
     *
     * @param id Represents field id
     * @return The {@link DealKanbanSortFields}
     */
    public static DealKanbanSortFields getById(final int id) {
        return switch (id) {
            case 1 -> DEAL_TITLE;
            case 2 -> DEAL_VALUE;
            case 3 -> CONTACT_NAME;
            case 4 -> COMPANY_NAME;
            case 5 -> SALES_OWNER;
            case 6 -> DEAL_CLOSED_ON;
            case 7 -> CREATED_BY;
            case 8 -> UPDATED_BY;
            case 9 -> LAST_ACTIVITY_DATE;
            case 10 -> UPDATED_AT;
            default -> CREATED_AT;
        };
    }

    /**
     * <p>
     * Gets the {@link DealKanbanSortFields} based on the provided sort field name
     * </p>
     *
     * @param sortFieldName Represents the name of the field
     * @return The {@link DealKanbanSortFields}
     */
    public static DealKanbanSortFields getByName(final String sortFieldName) {
        return switch (sortFieldName.toLowerCase()) {
            case "dealtitle" -> DEAL_TITLE;
            case "dealvalue" -> DEAL_VALUE;
            case "contactname" -> CONTACT_NAME;
            case "companyname" -> COMPANY_NAME;
            case "salesowner" -> SALES_OWNER;
            case "dealclosedon" -> DEAL_CLOSED_ON;
            case "createdby" -> CREATED_BY;
            case "updatedby" -> UPDATED_BY;
            case "lastactivitydate" -> LAST_ACTIVITY_DATE;
            case "updatedat" -> UPDATED_AT;
            default -> CREATED_AT;
        };
    }
}
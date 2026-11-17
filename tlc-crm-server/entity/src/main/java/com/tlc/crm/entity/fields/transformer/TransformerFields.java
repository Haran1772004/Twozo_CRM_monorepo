package com.tlc.crm.entity.fields.transformer;

import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * <p>
 * Provides the fields related to the transformer of the entity.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum TransformerFields implements FieldNameHolder {

    ID("id"),
    NAME("name"),
    TITLE("title"),
    FIRST_NAME("firstName"),
    LAST_NAME("lastName"),
    EMAILS("emails"),
    PHONES("phones"),
    STATUS("status"),
    CREATOR("creator"),
    ASSIGNEE("assignee"),
    COLLABORATORS("collaborators"),
    OPTION("option"),
    CURRENCY_ID("currencyId"),
    USER_ID("userId"),
    SALES_OWNER("salesOwner"),
    CREATED_BY("createdBy"),
    UPDATED_BY("updatedBy"),
    ROW_STATE("rowState"),
    ACTIVITY_TITLE("activityTitle"),
    ICON_NAME("iconName"),
    ICON("icon"),
    CONTACT("Contact"),
    COMPANY("Company"),
    DEAL("Deal"),
    PRODUCT("Product"),
    COUNT_SUMMARY("countSummary"),
    COUNT("count"),
    DISPLAY_NAME("displayName"),
    PRIORITY("priority"),
    REMINDER("reminder"),
    AVAILABILITY("availability"),
    NOTE("note"),
    FACEBOOK("facebook"),
    TWITTER("twitter"),
    LINKEDIN("linkedIn"),
    WEBSITE("website"),
    PIPELINE_STAGE("pipelineStage"),
    EXPECTED_CLOSE_DATE("expectedCloseDate"),
    VALUE("value"),
    CONTACTS("contacts"),
    THUMBNAIL("thumbnail"),
    COMPANY_THUMBNAIL_NEEDED("company_thumbnail_needed"),
    SYMBOL("symbol"),
    TAGS("tags"),
    MARKETING_LIST("marketingList"),
    DISPLAY_ADDRESS("displayAddress"),
    COUNTRY("country"),
    IS_PRODUCT_AVAILABLE("is_product_available"),
    IS_MANUALLY_ADDED("isManuallyAdded"),
    IS_PRIMARY("isPrimary"),
    MODIFIED_BY("modifiedBy"),
    FOLLOWERS("followers"),
    RELATED_CONTACTS("relatedContacts"),
    CONTACT_THUMBNAIL_NEEDED("contactThumbnailNeeded"),
    COMPLETION_PERCENTAGE("completionPercentage"),
    IS_CURATED( "isCurated" ),
    SOURCE("source"),
    DEAL_ID("dealId"),
    COMPANY_ID("companyId"),
    ACTIVITY_ID("activityId"),
    CONTACT_ID("contactId"),
    LINK_PROPERTIES("linkProperties"),
    OWNER("owner"),
    ;

    private final String name;

    TransformerFields(final String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     *
     * @return Field name.
     */
    @Override
    public String getName() {
        return name;
    }
}

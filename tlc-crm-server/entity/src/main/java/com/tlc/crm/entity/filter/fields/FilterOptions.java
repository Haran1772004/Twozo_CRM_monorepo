package com.tlc.crm.entity.filter.fields;

import java.util.Objects;

/**
 * <p>
 * Provides the fields of the filter options.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum FilterOptions {

    //Contact
    CONTACT, FIRST_NAME, LAST_NAME, CONTACT_SOURCE, LIFE_CYCLE_STAGE, PHONES, EMAILS, OTHER_UNSUBSCRIBE_REASON,
    LIFE_CYCLE_STATUS, CONTACT_LOST_REASON, SUBSCRIPTION_STATUS, SUBSCRIPTION_TYPES, DEPARTMENT, DESIGNATION, DATE_OF_BIRTH, UNSUBSCRIBE_REASON,
    EMAIL_CONVERSATION, VOICE_CALL, TIMEZONE, MARKETING_LIST,

    //Deal
    CLOSED_DEALS, LOST_DEALS, OPEN_DEALS, WON_DEALS, DATE_OF_ENTERING_STAGE, DEAL_CLOSED_ON,
    DEAL_STAGE_UPDATED_AT, WON_REASON, LOST_REASON, LOST_TIME, STAGE_PROBABILITY, WON_TIME, DEAL_VALUE, CAMPAIGN, DEAL_SOURCE,
    PAYMENT_STATUS, DEAL, EXPECTED_CLOSE_DATE, INDEX, PIPELINE, PIPELINE_STAGE, RELATED_CONTACTS, VALUE, PRIMARY_CONTACT, WEIGHTED_VALUE,
    FORECAST_CATEGORY,

    //Product
    PRODUCTS, ACTIVE_STATUS, TAX, QUANTITY_IN_STOCK, VALID_TILL, TAXABLE, SKU_NUMBER, PRODUCT_CODE,
    CATEGORY, PRICE_TAGS, QUANTITY_ORDERED, BASE_CURRENCY_AMOUNT,

    //Company
    COMPANY, ADDRESS, WEBSITE, FACEBOOK, TWITTER, LINKEDIN, PARENT_COMPANY, SIC_CODE,
    INDUSTRY_TYPE, BUSINESS_TYPE, ANNUAL_REVENUE, EMPLOYEE_COUNT, ORGANISATION_STATUS, REMARKS, LAST_ASSIGNED_AT,

    //Common
    TAGS, OWNER, UPDATED_AT, VISIBLE_TO, CREATOR, CURRENCY, DESCRIPTION, TYPE, NAME, PHONE, EMAIL,
    SALES_OWNER, TERRITORY, TITLE, ID, CREATED_TIME, IMPORTED_TIME, TEAM, IS_ACTIVE, UPDATED_TIME,
    CREATED_BY, UPDATED_BY, WEB_FORMS, CREATED_AT, RECENT_NOTE, STATUS, FOLLOWER,

    //Activity
    DONE_ACTIVITIES, EMAIL_MESSAGE_COUNT, LAST_ACTIVITY_DATE, LAST_EMAIL_RECEIVED,
    LAST_EMAIL_SENT, NEXT_ACTIVITY_DATE, TOTAL_ACTIVITIES, ADD_TIME, ASSIGNEE_ID, IS_DONE, DUE_DATE,
    END_TIME, LAST_NOTIFICATION_TIME, LOCATION, MARK_AS_DONE_TIME, AVAILABILITY, COLLABORATORS, REMINDER,
    DURATION, START_TIME, ACTIVITY, UPCOMING_ACTIVITIES, LAST_ACTIVITY_TYPE, PRIORITY, TODAY_ACTIVITIES, CONTACT_IDS,
    COMPANY_ID, DEAL_ID, TYPE_ID
    ;

    /**
     * <p>
     * Gets the {@link FilterOptions} for the respective field name.
     * </p>
     *
     * @param fieldName name of the field.
     * @return {@link FilterOptions}.
     */
    public static FilterOptions get(final String fieldName) {
        return switch (fieldName.toLowerCase().trim()) {
            case "closeddeals" -> CLOSED_DEALS;
            case "doneactivities" -> DONE_ACTIVITIES;
            case "emailmessagecount" -> EMAIL_MESSAGE_COUNT;
            case "tags" -> TAGS;
            case "lastactivitydate" -> LAST_ACTIVITY_DATE;
            case "lastemailreceived" -> LAST_EMAIL_RECEIVED;
            case "lastemailsent" -> LAST_EMAIL_SENT;
            case "lostdeals" -> LOST_DEALS;
            case "nextactivitydate" -> NEXT_ACTIVITY_DATE;
            case "opendeals", "deals" -> OPEN_DEALS;
            case "company" -> COMPANY;
            case "owner" -> OWNER;
            case "createdat", "companycreated", "contactcreated", "dealcreated", "createdon" -> CREATED_AT;
            case "totalactivities" -> TOTAL_ACTIVITIES;
            case "updatedat" -> UPDATED_AT;
            case "visibleto" -> VISIBLE_TO;
            case "wondeals" -> WON_DEALS;
            case "products" -> PRODUCTS;
            case "creator" -> CREATOR;
            case "currency" -> CURRENCY;
            case "dateofenteringstage" -> DATE_OF_ENTERING_STAGE;
            case "dealclosedon" -> DEAL_CLOSED_ON;
            case "dealstageupdatedat" -> DEAL_STAGE_UPDATED_AT;
            case "wonreason" -> WON_REASON;
            case "lostreason" -> LOST_REASON;
            case "contactlostreason" -> CONTACT_LOST_REASON;
            case "losttime" -> LOST_TIME;
            case "stageprobability" -> STAGE_PROBABILITY;
            case "wontime" -> WON_TIME;
            case "dealvalue" -> DEAL_VALUE;
//            case "campaign" -> CAMPAIGN; <!--    TODO use this after campaign integrated -->
            case "dealsource" -> DEAL_SOURCE;
            case "paymentstatus" -> PAYMENT_STATUS;
            case "activestatus" -> ACTIVE_STATUS;
            case "tax" -> TAX;
            case "quantityinstock" -> QUANTITY_IN_STOCK;
            case "validtill" -> VALID_TILL;
            case "taxable" -> TAXABLE;
            case "address" -> ADDRESS;
            case "website" -> WEBSITE;
            case "addtime" -> ADD_TIME;
            case "assigneeid" -> ASSIGNEE_ID;
            case "contact" -> CONTACT;
            case "deal" -> DEAL;
            case "isdone" -> IS_DONE;
            case "duedate" -> DUE_DATE;
            case "endtime" -> END_TIME;
            case "starttime" -> START_TIME;
            case "availability" -> AVAILABILITY;
            case "collaborators" -> COLLABORATORS;
            case "reminder" -> REMINDER;
            case "duration" -> DURATION;
            case "status" -> STATUS;
            case "lastnotificationtime" -> LAST_NOTIFICATION_TIME;
            case "location" -> LOCATION;
            case "markasdonetime" -> MARK_AS_DONE_TIME;
            case "description" -> DESCRIPTION;
            case "type" -> TYPE;
            case "name" -> NAME;
            case "firstname" -> FIRST_NAME;
            case "lastname" -> LAST_NAME;
            case "contactsource" -> CONTACT_SOURCE;
            case "lifecyclestage" -> LIFE_CYCLE_STAGE;
            case "title" -> TITLE;
            case "expectedclosedate" -> EXPECTED_CLOSE_DATE;
            case "index" -> INDEX;
            case "pipeline" -> PIPELINE;
            case "pipelinestage" -> PIPELINE_STAGE;
            case "skunumber" -> SKU_NUMBER;
            case "productcode" -> PRODUCT_CODE;
            case "category" -> CATEGORY;
            case "pricetags" -> PRICE_TAGS;
            case "employeecount" -> EMPLOYEE_COUNT;
            case "facebook" -> FACEBOOK;
            case "twitter" -> TWITTER;
            case "linkedin" -> LINKEDIN;
            case "parentcompany" -> PARENT_COMPANY;
            case "siccode" -> SIC_CODE;
            case "industrytype" -> INDUSTRY_TYPE;
            case "organizationstatus" -> ORGANISATION_STATUS;
            case "businesstype" -> BUSINESS_TYPE;
            case "annualrevenue" -> ANNUAL_REVENUE;
            case "phone" -> PHONE;
            case "email" -> EMAIL;
            case "salesowner" -> SALES_OWNER;
            case "remarks" -> REMARKS;
            case "territory" -> TERRITORY;
            case "lifecyclestatus" -> LIFE_CYCLE_STATUS;
            case "subscriptionstatus" -> SUBSCRIPTION_STATUS;
            case "subscriptiontypes" -> SUBSCRIPTION_TYPES;
            case "department" -> DEPARTMENT;
            case "dateofbirth" -> DATE_OF_BIRTH;
            case "designation" -> DESIGNATION;
            case "emails" -> EMAILS;
            case "phones" -> PHONES;
            case "relatedcontacts" -> RELATED_CONTACTS;
            case "id" -> ID;
            case "createdtime" -> CREATED_TIME;
            case "emailconversation" -> EMAIL_CONVERSATION;
            case "voicecall" -> VOICE_CALL;
            case "importedtime" -> IMPORTED_TIME;
            case "team" -> TEAM;
            case "activity" -> ACTIVITY;
            case "isactive" -> IS_ACTIVE;
            case "value" -> VALUE;
            case "updatedtime" -> UPDATED_TIME;
            case "updatedby" -> UPDATED_BY;
            case "createdby" -> CREATED_BY;
            case "upcomingactivities" -> UPCOMING_ACTIVITIES;
            case "webforms" -> WEB_FORMS;
            case "timezone" -> TIMEZONE;
            case "lastactivitytype" -> LAST_ACTIVITY_TYPE;
            case "priority" -> PRIORITY;
            case "todayactivities" -> TODAY_ACTIVITIES;
            case "recentnote" -> RECENT_NOTE;
            case "primarycontact" -> PRIMARY_CONTACT;
            case "lastassignedat" -> LAST_ASSIGNED_AT;
            case "unsubscribereason" -> UNSUBSCRIBE_REASON;
            case "contactids" -> CONTACT_IDS;
            case "dealid" -> DEAL_ID;
            case "companyid" -> COMPANY_ID;
            case "typeid" -> TYPE_ID;
            case "quantityordered" -> QUANTITY_ORDERED;
            case "otherunsubscribereason" -> OTHER_UNSUBSCRIBE_REASON;
            case "basecurrencyamount" -> BASE_CURRENCY_AMOUNT;
            case "follower" -> FOLLOWER;
            case "weightedvalue" -> WEIGHTED_VALUE;
            case "forecastcategory" -> FORECAST_CATEGORY;
            case "marketinglist" -> MARKETING_LIST;

            default -> null;
        };
    }

    /**
     * <p>
     * Checks whether the filter option is countable or not
     * </p>
     *
     * @param filterOptions refers the filter option.
     * @return {@link FilterOptions}.
     */
    public static boolean isCountable(final FilterOptions filterOptions) {
        if (Objects.isNull(filterOptions)) {
            return false;
        }

        return switch (filterOptions) {
            case CLOSED_DEALS, OPEN_DEALS, WON_DEALS, DONE_ACTIVITIES, LOST_DEALS, UPCOMING_ACTIVITIES,
                    TOTAL_ACTIVITIES, EMAIL_MESSAGE_COUNT, PRODUCTS -> true;
            default -> false;
        };
    }
}

package com.tlc.crm.entity.search.fields;

import com.tlc.commons.util.StringUtil;

/**
 * <p>
 * Common search nested fields.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class CommonSearchNestedFields {

    public static final String CONTACT_DOT_ID = StringUtil.joinStringsWithDot(
            CommonSearchFields.CONTACT, CommonSearchFields.ID);
    public static final String CONTACT_DOT_COMPANY = StringUtil.joinStringsWithDot(
            CommonSearchFields.CONTACT, CommonSearchFields.COMPANY);
    public static final String CONTACT_COMPANY_DOT_ID = StringUtil.joinStringsWithDot(
            CommonSearchFields.CONTACT, CommonSearchFields.COMPANY, CommonSearchFields.ID);
    public static final String CONTACT_DOT_FOLLOWER_IDS = StringUtil.joinStringsWithDot(
            CommonSearchFields.CONTACT, CommonSearchFields.FOLLOWER_IDS);
    public static final String CONTACT_COMPANY_DOT_FOLLOWER_IDS = StringUtil.joinStringsWithDot(
            CommonSearchFields.DEAL, CommonSearchFields.COMPANY, CommonSearchFields.FOLLOWER_IDS);


    public static final String COMPANY_DOT_ID = StringUtil.joinStringsWithDot(
            CommonSearchFields.COMPANY, CommonSearchFields.ID);
    public static final String COMPANY_DOT_FOLLOWER_IDS = StringUtil.joinStringsWithDot(
            CommonSearchFields.COMPANY, CommonSearchFields.FOLLOWER_IDS);

    public static final String DEAL_DOT_ID = StringUtil.joinStringsWithDot(
            CommonSearchFields.DEAL, CommonSearchFields.ID);
    public static final String DEAL_DOT_CONTACT = StringUtil.joinStringsWithDot(
            CommonSearchFields.DEAL, CommonSearchFields.CONTACT);
    public static final String DEAL_DOT_COMPANY = StringUtil.joinStringsWithDot(
            CommonSearchFields.DEAL, CommonSearchFields.COMPANY);
    public static final String DEAL_CONTACT_DOT_ID = StringUtil.joinStringsWithDot(
            CommonSearchFields.DEAL, CommonSearchFields.CONTACT, CommonSearchFields.ID);
    public static final String DEAL_COMPANY_DOT_ID = StringUtil.joinStringsWithDot(
            CommonSearchFields.DEAL, CommonSearchFields.COMPANY, CommonSearchFields.ID);
    public static final String DEAL_DOT_FOLLOWERS_ID = StringUtil.joinStringsWithDot(
            CommonSearchFields.DEAL, CommonSearchFields.FOLLOWER_IDS);
    public static final String DEAL_CONTACT_DOT_FOLLOWER_IDS = StringUtil.joinStringsWithDot(
            CommonSearchFields.DEAL, CommonSearchFields.CONTACT, CommonSearchFields.FOLLOWER_IDS);
    public static final String DEAL_COMPANY_DOT_FOLLOWER_IDS = StringUtil.joinStringsWithDot(
            CommonSearchFields.CONTACT, CommonSearchFields.COMPANY, CommonSearchFields.FOLLOWER_IDS);
}
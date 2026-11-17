package com.tlc.crm.entity.internal.security.search.filter;

import com.tlc.commons.util.StringUtil;
import com.tlc.crm.entity.search.fields.CommonSearchFields;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Preset fields that are to be queried for "search" API.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class PresetSearchFilterFieldsWeb {

    private PresetSearchFilterFieldsWeb() {
    }

    private static final Collection<String> ADDRESS_FIELDS = List.of(
            StringUtil.joinStringsWithDot(CommonSearchFields.ADDRESS, CommonSearchFields.ADDRESS_LINE_1),
            StringUtil.joinStringsWithDot(CommonSearchFields.ADDRESS, CommonSearchFields.ADDRESS_LINE_2),
            StringUtil.joinStringsWithDot(CommonSearchFields.ADDRESS, CommonSearchFields.CITY),
            StringUtil.joinStringsWithDot(CommonSearchFields.ADDRESS, CommonSearchFields.COUNTRY),
            StringUtil.joinStringsWithDot(CommonSearchFields.ADDRESS, CommonSearchFields.STATE),
            StringUtil.joinStringsWithDot(CommonSearchFields.ADDRESS, CommonSearchFields.ZIPCODE));
    private static final Collection<String> CONTACT_FIELDS_COLLECTION = new ArrayList<>(List.of(CommonSearchFields.NAME,
            StringUtil.joinStringsWithDot(CommonSearchFields.EMAILS, CommonSearchFields.VALUE),
            StringUtil.joinStringsWithDot(CommonSearchFields.PHONES, CommonSearchFields.NUMBER),
            StringUtil.joinStringsWithDot(CommonSearchFields.COMPANY, CommonSearchFields.NAME),
            StringUtil.joinStringsWithDot(CommonSearchFields.COMPANY, CommonSearchFields.WEBSITE),
            StringUtil.joinStringsWithDot(CommonSearchFields.CUSTOM_FIELDS, CommonSearchFields.NAME),
            StringUtil.joinStringsWithDot(CommonSearchFields.NOTES, CommonSearchFields.NAME)));
    private static final Collection<String> COMPANY_FIELDS_COLLECTION = new ArrayList<>(List.of(
            CommonSearchFields.NAME, CommonSearchFields.WEBSITE,
            StringUtil.joinStringsWithDot(CommonSearchFields.CUSTOM_FIELDS, CommonSearchFields.NAME),
            StringUtil.joinStringsWithDot(CommonSearchFields.NOTES, CommonSearchFields.NAME)));
    private static final Collection<String> DEAL_FIELDS_COLLECTION = List.of(CommonSearchFields.NAME,
            StringUtil.joinStringsWithDot(CommonSearchFields.CONTACT, CommonSearchFields.NAME),
            StringUtil.joinStringsWithDot(CommonSearchFields.CONTACT, CommonSearchFields.EMAILS, CommonSearchFields.VALUE),
            StringUtil.joinStringsWithDot(CommonSearchFields.CONTACT, CommonSearchFields.PHONES, CommonSearchFields.NUMBER),
            StringUtil.joinStringsWithDot(CommonSearchFields.COMPANY, CommonSearchFields.NAME),
            StringUtil.joinStringsWithDot(CommonSearchFields.COMPANY, CommonSearchFields.WEBSITE),
            StringUtil.joinStringsWithDot(CommonSearchFields.CUSTOM_FIELDS, CommonSearchFields.NAME),
            StringUtil.joinStringsWithDot(CommonSearchFields.NOTES, CommonSearchFields.NAME));

    static {
        CONTACT_FIELDS_COLLECTION.addAll(ADDRESS_FIELDS);
        COMPANY_FIELDS_COLLECTION.addAll(ADDRESS_FIELDS);
    }

    public static final String[] CONTACT_FIELDS = CONTACT_FIELDS_COLLECTION.toArray(String[]::new);
    public static final String[] COMPANY_FIELDS = COMPANY_FIELDS_COLLECTION.toArray(String[]::new);
    public static final String[] DEAL_FIELDS = DEAL_FIELDS_COLLECTION.toArray(String[]::new);
    public static final String[] PRODUCT_FIELDS = {CommonSearchFields.NAME, CommonSearchFields.PRODUCT_CODE,
            StringUtil.joinStringsWithDot(CommonSearchFields.CUSTOM_FIELDS, CommonSearchFields.NAME)};
    public static final String[] ACTIVITY_FIELDS = {CommonSearchFields.NAME,
            StringUtil.joinStringsWithDot(CommonSearchFields.ACTIVITY_TYPE, CommonSearchFields.NAME),
            StringUtil.joinStringsWithDot(CommonSearchFields.NOTE, CommonSearchFields.NAME)};
    public static final String NOTE_AND_FILE_FIELDS = CommonSearchFields.NAME;
}
package com.tlc.crm.entity.internal.security.search.filter.mobile;

import com.tlc.commons.util.StringUtil;
import com.tlc.crm.entity.search.fields.CommonSearchFields;

/**
 * <p>
 * Preset fields that are to be queried for "search" API.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class PresetSearchFilterFieldsMobile {

    private PresetSearchFilterFieldsMobile() {
    }

    public static final String[] CONTACT_FIELDS = {CommonSearchFields.NAME,
            StringUtil.joinStringsWithDot(CommonSearchFields.PRIMARY_EMAIL, CommonSearchFields.VALUE),
            StringUtil.joinStringsWithDot(CommonSearchFields.PHONES, CommonSearchFields.NUMBER)};
    public static final String COMPANY_FIELD = CommonSearchFields.NAME;
    public static final String DEAL_FIELD = CommonSearchFields.NAME;
    public static final String PRODUCT_FIELD = CommonSearchFields.NAME;
    public static final String ACTIVITY_FIELD = CommonSearchFields.NAME;
    public static final String NOTE_AND_FILE_FIELDS = CommonSearchFields.NAME;
}
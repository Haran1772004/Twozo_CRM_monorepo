package com.tlc.crm.entity.search.loader.impl;

import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.crm.entity.search.fields.PresetIndexFields;
import com.tlc.crm.entity.search.fields.PresetIndexFieldsMobile;
import com.tlc.crm.entity.search.script.ScriptKeysMobileClient;
import com.tlc.search.model.ScriptInfo;
import com.tlc.search.model.SearchRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;

/**
 * <p>
 * Loads mobile client required meta into {@link SearchRequest}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class MobileSearchRequestLoader extends AbstractSearchRequestLoader {

    private final PresetIndexFieldsMobile presetIndexFieldsMobile;
    private final ScriptKeysMobileClient scriptKeysMobileClient;

    private MobileSearchRequestLoader() {
        presetIndexFieldsMobile = PresetIndexFieldsMobile.getInstance();
        scriptKeysMobileClient = ScriptKeysMobileClient.getInstance();
    }

    private static final class Instance {
        private static final MobileSearchRequestLoader INSTANCE = new MobileSearchRequestLoader();
    }

    /**
     * Gets the {@link MobileSearchRequestLoader} instance
     *
     * @return {@link MobileSearchRequestLoader}
     */
    public static MobileSearchRequestLoader getInstance() {
        return MobileSearchRequestLoader.Instance.INSTANCE;
    }

    /**
     * <p>
     * Loads search request for globally accessed entities.
     * </p>
     *
     * @param index         Index's name.
     * @param searchRequest {@link SearchRequest}.
     */
    public void loadRequestForGlobalAccessibleModules(final String index, final SearchRequest searchRequest) {
        searchRequest.includeFields(presetIndexFieldsMobile.getGlobalAccessRequiredFields(index));
    }

    /**
     * <p>
     * Loads search request for company index.
     * </p>
     *
     * @param searchRequest {@link SearchRequest}.
     */
    public void loadCompanyIndexRequest(final SearchRequest searchRequest) {
        searchRequest.includeFields(presetIndexFieldsMobile.getRequiredFields(CommonSearchFields.COMPANY));
    }

    /**
     * <p>
     * Loads search request for product index.
     * </p>
     *
     * @param searchRequest {@link SearchRequest}.
     */
    public void loadProductIndexRequest(final SearchRequest searchRequest) {
        searchRequest.includeFields(presetIndexFieldsMobile.getRequiredFields(CommonSearchFields.PRODUCT));
    }

    /**
     * <p>
     * Loads search request for activity index.
     * </p>
     *
     * @param searchRequest        {@link SearchRequest}.
     * @param dealPermissionMap    Key value pair referring deal related permissions.
     * @param contactPermissionMap Key value pair referring contact related permissions.
     * @param companyPermissionMap Key value pair referring company related permissions.
     */
    @Override
    public void loadActivityIndexRequest(final SearchRequest searchRequest, final Map<String, Map<String, Object>> dealPermissionMap,
                                         final Map<String, Map<String, Object>> contactPermissionMap,
                                         final Map<String, Map<String, Object>> companyPermissionMap) {
        searchRequest.includeFields(presetIndexFieldsMobile.getRequiredFields(CommonSearchFields.ACTIVITY));
    }

    /**
     * <p>
     * Loads search request for file/note index.
     * </p>
     *
     * @param searchRequest        {@link SearchRequest}.
     * @param contactPermissionMap Key value pair referring contact related permissions.
     * @param companyPermissionMap Key value pair referring company related permissions.
     */
    @Override
    public void loadNoteAndFileIndexRequest(final SearchRequest searchRequest, final Map<String, Map<String, Object>> contactPermissionMap,
                                            final Map<String, Map<String, Object>> companyPermissionMap, final String index) {
        searchRequest.includeFields(presetIndexFieldsMobile.getRequiredFields(index));
        searchRequest.excludeFields(presetIndexFieldsMobile.getRequiredFields(index));
    }

    /**
     * <p>
     * Loads search request for deal index.
     * </p>
     *
     * @param searchRequest        {@link SearchRequest}.
     * @param contactPermissionMap Key value pair referring contact related permissions.
     * @param companyPermissionMap Key value pair referring company related permissions.
     */
    public void loadDealIndexRequest(final SearchRequest searchRequest, final Map<String, Map<String, Object>> contactPermissionMap,
                                     final Map<String, Map<String, Object>> companyPermissionMap) {
        setScriptsForDealIndex(searchRequest, contactPermissionMap, companyPermissionMap);
    }

    /**
     * <p>
     * Gets {@link PresetIndexFields}.
     * </p>
     *
     * @return {@link PresetIndexFields}.
     */
    @Override
    protected PresetIndexFields getPresetIndexFields() {
        return presetIndexFieldsMobile;
    }

    /**
     * <p>
     * Sets script for deal index.
     * Permission map states: null = no access, empty = global access, populated = restricted access (needs script).
     * </p>
     *
     * @param searchRequest        {@link SearchRequest}.
     * @param contactPermissionMap Key value pair referring contact related permissions.
     * @param companyPermissionMap Key value pair referring company related permissions.
     */
    private void setScriptsForDealIndex(final SearchRequest searchRequest,
                                        final Map<String, Map<String, Object>> contactPermissionMap,
                                        final Map<String, Map<String, Object>> companyPermissionMap) {
        // restricted = not null and not empty (needs scripting)
        final boolean isContactRestricted = isRestricted(contactPermissionMap);
        final boolean isCompanyRestricted = isRestricted(companyPermissionMap);

        final String scriptId = scriptKeysMobileClient.getScriptIdForDealIndex(isContactRestricted, isCompanyRestricted);

        // No module needs scripting - include global/required fields and return
        if (Objects.isNull(scriptId)) {
            if (Objects.isNull(contactPermissionMap)) {
                searchRequest.includeFields(presetIndexFieldsMobile.getRequiredFields(CommonSearchFields.DEAL));
            } else {
                searchRequest.includeFields(presetIndexFieldsMobile.getGlobalAccessRequiredFields(CommonSearchFields.DEAL));
                searchRequest.excludeFields(presetIndexFieldsMobile.getGlobalAccessIgnoredFields(CommonSearchFields.DEAL));
            }
            return;
        }

        final Map<String, Object> paramsMap = new HashMap<>();
        final String field;

        searchRequest.includeFields(presetIndexFieldsMobile.getRequiredFields(CommonSearchFields.DEAL));

        switch (scriptId) {
            case ScriptKeysMobileClient.DEAL_CONTACT_COMPANY_SCRIPT_MOBILE -> {
                // Both restricted - script handles both
                putScriptIntoParamsMap(paramsMap, CONTACT_PERMISSION, contactPermissionMap);
                putScriptIntoParamsMap(paramsMap, COMPANY_PERMISSION, companyPermissionMap);
                field = FIELDS;
            }
            case ScriptKeysMobileClient.DEAL_CONTACT_SCRIPT_MOBILE -> {
                // Company is not restricted - include entity fields if accessible
                if (!Objects.isNull(companyPermissionMap)) {
                    searchRequest.includeFields(presetIndexFieldsMobile.getEntityFields(CommonSearchFields.COMPANY));
                }
                putScriptIntoParamsMap(paramsMap, CONTACT_PERMISSION, contactPermissionMap);
                field = CommonSearchFields.CONTACT;
            }
            case ScriptKeysMobileClient.DEAL_COMPANY_SCRIPT_MOBILE -> {
                // Contact is not restricted - include entity fields if accessible
                if (!Objects.isNull(contactPermissionMap)) {
                    searchRequest.includeFields(presetIndexFieldsMobile.getEntityFields(CommonSearchFields.CONTACT));
                }
                putScriptIntoParamsMap(paramsMap, COMPANY_PERMISSION, companyPermissionMap);
                field = CommonSearchFields.COMPANY;
            }
            default -> field = null;
        }

        searchRequest.setScripts(List.of(new ScriptInfo(scriptId, field, paramsMap)));
    }

    /**
     * <p>
     * Checks if a permission map represents restricted access (not null and not empty).
     * </p>
     *
     * @param permissionMap The permission map to check.
     * @return true if restricted (populated), false if null (no access) or empty (global access).
     */
    private boolean isRestricted(final Map<String, Map<String, Object>> permissionMap) {
        return permissionMap != null && !permissionMap.isEmpty();
    }
}
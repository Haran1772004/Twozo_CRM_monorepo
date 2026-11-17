package com.tlc.crm.entity.search.loader.impl;

import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.crm.entity.search.fields.PresetIndexFields;
import com.tlc.crm.entity.search.fields.PresetIndexFieldsWeb;
import com.tlc.crm.entity.search.script.ScriptKeysWebClient;
import com.tlc.search.model.ScriptInfo;
import com.tlc.search.model.SearchRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;

/**
 * <p>
 * Loads web client required meta into {@link SearchRequest}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class WebSearchRequestLoader extends AbstractSearchRequestLoader {

    private final PresetIndexFieldsWeb presetIndexFieldsWeb;
    private final ScriptKeysWebClient scriptKeysWebClient;

    private WebSearchRequestLoader() {
        presetIndexFieldsWeb = PresetIndexFieldsWeb.getInstance();
        scriptKeysWebClient = ScriptKeysWebClient.getInstance();
    }

    private static final class Instance {
        private static final WebSearchRequestLoader INSTANCE = new WebSearchRequestLoader();
    }

    /**
     * Gets the {@link WebSearchRequestLoader} instance
     *
     * @return {@link WebSearchRequestLoader}
     */
    public static WebSearchRequestLoader getInstance() {
        return WebSearchRequestLoader.Instance.INSTANCE;
    }

    /**
     * <p>
     * Loads search request for company index.
     * </p>
     *
     * @param searchRequest {@link SearchRequest}.
     */
    public void loadCompanyIndexRequest(final SearchRequest searchRequest) {
        searchRequest.includeFields(presetIndexFieldsWeb.getGlobalAccessRequiredFields(CommonSearchFields.COMPANY));
        searchRequest.excludeFields(presetIndexFieldsWeb.getGlobalAccessIgnoredFields(CommonSearchFields.COMPANY));
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
     * Loads search request for product index.
     * </p>
     *
     * @param searchRequest {@link SearchRequest}.
     */
    public void loadProductIndexRequest(final SearchRequest searchRequest) {
        searchRequest.includeFields(presetIndexFieldsWeb.getRequiredFields(CommonSearchFields.PRODUCT));
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
    public void loadActivityIndexRequest(final SearchRequest searchRequest, final Map<String, Map<String, Object>> dealPermissionMap,
                                         final Map<String, Map<String, Object>> contactPermissionMap,
                                         final Map<String, Map<String, Object>> companyPermissionMap) {
        setScriptsForActivityIndex(searchRequest, dealPermissionMap, contactPermissionMap, companyPermissionMap);
    }

    /**
     * <p>
     * Loads search request for note and file index.
     * </p>
     *
     * @param searchRequest        {@link SearchRequest}.
     * @param contactPermissionMap Key value pair referring contact related permissions.
     * @param companyPermissionMap Key value pair referring company related permissions.
     * @param index                Index name (note or file).
     */
    public void loadNoteAndFileIndexRequest(final SearchRequest searchRequest, final Map<String, Map<String, Object>> contactPermissionMap,
                                            final Map<String, Map<String, Object>> companyPermissionMap, final String index) {
        setScriptsForNoteAndFileIndex(searchRequest, index, contactPermissionMap, companyPermissionMap);
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
        searchRequest.includeFields(presetIndexFieldsWeb.getGlobalAccessRequiredFields(index));
        searchRequest.excludeFields(presetIndexFieldsWeb.getGlobalAccessIgnoredFields(index));
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
        return presetIndexFieldsWeb;
    }

    /**
     * <p>
     * Sets script for activity index.
     * Permission map states: null = no access, empty = global access, populated = restricted access (needs script).
     * </p>
     *
     * @param searchRequest        {@link SearchRequest}.
     * @param dealPermissionMap    Key value pair referring deal related permissions.
     * @param contactPermissionMap Key value pair referring contact related permissions.
     * @param companyPermissionMap Key value pair referring company related permissions.
     */
    private void setScriptsForActivityIndex(final SearchRequest searchRequest,
                                            final Map<String, Map<String, Object>> dealPermissionMap,
                                            final Map<String, Map<String, Object>> contactPermissionMap,
                                            final Map<String, Map<String, Object>> companyPermissionMap) {
        final boolean isDealRestricted = isRestricted(dealPermissionMap);
        final boolean isContactRestricted = isRestricted(contactPermissionMap);
        final boolean isCompanyRestricted = isRestricted(companyPermissionMap);
        final String scriptId = scriptKeysWebClient.getScriptIdForActivityIndex(
                isDealRestricted, isContactRestricted, isCompanyRestricted);

        if (Objects.isNull(scriptId)) {
            includeFieldsForAccessibleModules(searchRequest, CommonSearchFields.ACTIVITY,
                    dealPermissionMap, contactPermissionMap, companyPermissionMap);
            return;
        }

        final Map<String, Object> paramsMap = new HashMap<>();

        searchRequest.includeFields(presetIndexFieldsWeb.getRequiredFields(CommonSearchFields.ACTIVITY));

        switch (scriptId) {
            case ScriptKeysWebClient.ACTIVITY_ALL_ENTITIES_SCRIPT -> {
                putScriptIntoParamsMap(paramsMap, DEAL_PERMISSION, dealPermissionMap);
                putScriptIntoParamsMap(paramsMap, CONTACT_PERMISSION, contactPermissionMap);
                putScriptIntoParamsMap(paramsMap, COMPANY_PERMISSION, companyPermissionMap);
                searchRequest.setScripts(List.of(new ScriptInfo(scriptId, FIELDS, paramsMap)));
            }
            case ScriptKeysWebClient.ACTIVITY_CONTACT_COMPANY_SCRIPT -> {
                resolveNonRestrictedModuleParams(searchRequest, paramsMap, DEAL_PERMISSION, CommonSearchFields.DEAL, dealPermissionMap);
                putScriptIntoParamsMap(paramsMap, CONTACT_PERMISSION, contactPermissionMap);
                putScriptIntoParamsMap(paramsMap, COMPANY_PERMISSION, companyPermissionMap);
                searchRequest.setScripts(List.of(new ScriptInfo(scriptId, FIELDS, paramsMap)));
            }
            case ScriptKeysWebClient.ACTIVITY_DEAL_CONTACT_SCRIPT -> {
                resolveNonRestrictedModuleParams(searchRequest, paramsMap, COMPANY_PERMISSION, CommonSearchFields.COMPANY, companyPermissionMap);
                putScriptIntoParamsMap(paramsMap, DEAL_PERMISSION, dealPermissionMap);
                putScriptIntoParamsMap(paramsMap, CONTACT_PERMISSION, contactPermissionMap);
                searchRequest.setScripts(List.of(new ScriptInfo(scriptId, FIELDS, paramsMap)));
            }
            case ScriptKeysWebClient.ACTIVITY_DEAL_COMPANY_SCRIPT -> {
                resolveNonRestrictedModuleParams(searchRequest, paramsMap, CONTACT_PERMISSION, CommonSearchFields.CONTACT, contactPermissionMap);
                putScriptIntoParamsMap(paramsMap, DEAL_PERMISSION, dealPermissionMap);
                putScriptIntoParamsMap(paramsMap, COMPANY_PERMISSION, companyPermissionMap);
                searchRequest.setScripts(List.of(new ScriptInfo(scriptId, FIELDS, paramsMap)));
            }
            case ScriptKeysWebClient.ACTIVITY_DEAL_SCRIPT -> {
                resolveNonRestrictedModuleParams(searchRequest, paramsMap, CONTACT_PERMISSION, CommonSearchFields.CONTACT, contactPermissionMap);
                resolveNonRestrictedModuleParams(searchRequest, paramsMap, COMPANY_PERMISSION, CommonSearchFields.COMPANY, companyPermissionMap);
                putScriptIntoParamsMap(paramsMap, DEAL_PERMISSION, dealPermissionMap);
                searchRequest.setScripts(List.of(new ScriptInfo(scriptId, CommonSearchFields.DEAL, paramsMap)));
            }
            case ScriptKeysWebClient.ACTIVITY_CONTACT_SCRIPT -> {
                resolveNonRestrictedModuleParams(searchRequest, paramsMap, DEAL_PERMISSION, CommonSearchFields.DEAL, dealPermissionMap);
                resolveNonRestrictedModuleParams(searchRequest, paramsMap, COMPANY_PERMISSION, CommonSearchFields.COMPANY, companyPermissionMap);
                putScriptIntoParamsMap(paramsMap, CONTACT_PERMISSION, contactPermissionMap);
                searchRequest.setScripts(List.of(new ScriptInfo(scriptId, FIELDS, paramsMap)));
            }
            case ScriptKeysWebClient.ACTIVITY_COMPANY_SCRIPT -> {
                resolveNonRestrictedModuleParams(searchRequest, paramsMap, DEAL_PERMISSION, CommonSearchFields.DEAL, dealPermissionMap);
                resolveNonRestrictedModuleParams(searchRequest, paramsMap, CONTACT_PERMISSION, CommonSearchFields.CONTACT, contactPermissionMap);
                putScriptIntoParamsMap(paramsMap, COMPANY_PERMISSION, companyPermissionMap);
                searchRequest.setScripts(List.of(new ScriptInfo(scriptId, FIELDS, paramsMap)));
            }
        }
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
        final boolean isContactRestricted = isRestricted(contactPermissionMap);
        final boolean isCompanyRestricted = isRestricted(companyPermissionMap);
        final String scriptId = scriptKeysWebClient.getScriptIdForDealIndex(isContactRestricted, isCompanyRestricted);

        if (Objects.isNull(scriptId)) {
            includeFieldsForAccessibleModules(searchRequest, CommonSearchFields.DEAL,
                    null, contactPermissionMap, companyPermissionMap);
            return;
        }

        final Map<String, Object> paramsMap = new HashMap<>();

        searchRequest.includeFields(presetIndexFieldsWeb.getRequiredFields(CommonSearchFields.DEAL));

        switch (scriptId) {
            case ScriptKeysWebClient.DEAL_CONTACT_COMPANY_SCRIPT_WEB -> {
                putScriptIntoParamsMap(paramsMap, CONTACT_PERMISSION, contactPermissionMap);
                putScriptIntoParamsMap(paramsMap, COMPANY_PERMISSION, companyPermissionMap);
                searchRequest.setScripts(List.of(new ScriptInfo(scriptId, FIELDS, paramsMap)));
            }
            case ScriptKeysWebClient.DEAL_CONTACT_SCRIPT_WEB -> {
                resolveNonRestrictedModuleParams(searchRequest, paramsMap, COMPANY_PERMISSION, CommonSearchFields.COMPANY, companyPermissionMap);
                putScriptIntoParamsMap(paramsMap, CONTACT_PERMISSION, contactPermissionMap);
                searchRequest.setScripts(List.of(new ScriptInfo(scriptId,CommonSearchFields.CONTACT, paramsMap)));
            }
            case ScriptKeysWebClient.DEAL_COMPANY_SCRIPT_WEB -> {
                resolveNonRestrictedModuleParams(searchRequest, paramsMap, CONTACT_PERMISSION, CommonSearchFields.CONTACT, contactPermissionMap);
                putScriptIntoParamsMap(paramsMap, COMPANY_PERMISSION, companyPermissionMap);
                searchRequest.setScripts(List.of(new ScriptInfo(scriptId, FIELDS, paramsMap)));
            }
        }
    }

    /**
     * <p>
     * Sets script for note and file index.
     * Permission map states: null = no access, empty = global access, populated = restricted access (needs script).
     * </p>
     *
     * @param searchRequest        {@link SearchRequest}.
     * @param index                Index name (note or file).
     * @param contactPermissionMap Key value pair referring contact related permissions.
     * @param companyPermissionMap Key value pair referring company related permissions.
     */
    private void setScriptsForNoteAndFileIndex(final SearchRequest searchRequest, final String index,
                                               final Map<String, Map<String, Object>> contactPermissionMap,
                                               final Map<String, Map<String, Object>> companyPermissionMap) {
        final boolean isContactRestricted = isRestricted(contactPermissionMap);
        final boolean isCompanyRestricted = isRestricted(companyPermissionMap);
        final String scriptId = scriptKeysWebClient.getScriptIdForNoteAndFileIndex(isContactRestricted, isCompanyRestricted);

        // No module is restricted - selectively include fields only for accessible (non-null) modules
        if (Objects.isNull(scriptId)) {
            includeFieldsForAccessibleModules(searchRequest, index,
                    null, contactPermissionMap, companyPermissionMap);
            return;
        }

        final Map<String, Object> paramsMap = new HashMap<>();

        searchRequest.includeFields(presetIndexFieldsWeb.getRequiredFields(index));

        switch (scriptId) {
            case ScriptKeysWebClient.NOTE_OR_FILE_CONTACT_COMPANY_SCRIPT -> {
                putScriptIntoParamsMap(paramsMap, CONTACT_PERMISSION, contactPermissionMap);
                putScriptIntoParamsMap(paramsMap, COMPANY_PERMISSION, companyPermissionMap);
                searchRequest.setScripts(List.of(new ScriptInfo(scriptId, FIELDS, paramsMap)));
            }
            case ScriptKeysWebClient.NOTE_OR_FILE_CONTACT_SCRIPT -> {
                resolveNonRestrictedModuleParams(searchRequest, paramsMap, COMPANY_PERMISSION, CommonSearchFields.COMPANY, companyPermissionMap);
                putScriptIntoParamsMap(paramsMap, CONTACT_PERMISSION, contactPermissionMap);
                searchRequest.setScripts(List.of(new ScriptInfo(scriptId, CommonSearchFields.CONTACT, paramsMap)));
            }
            case ScriptKeysWebClient.NOTE_OR_FILE_COMPANY_SCRIPT -> {
                resolveNonRestrictedModuleParams(searchRequest, paramsMap, CONTACT_PERMISSION, CommonSearchFields.CONTACT, contactPermissionMap);
                putScriptIntoParamsMap(paramsMap, COMPANY_PERMISSION, companyPermissionMap);
                searchRequest.setScripts(List.of(new ScriptInfo(scriptId, FIELDS, paramsMap)));
            }
        }
    }

    /**
     * <p>
     * Handles a non-restricted module's params in the script context.
     * null (no access) -> marks as NOT_PERMITTED.
     * empty (global access) -> includes entity fields and marks as GLOBAL.
     * </p>
     *
     * @param searchRequest {@link SearchRequest}.
     * @param paramsMap     Script params being built.
     * @param permissionKey The permission key for this module (e.g., DEAL_PERMISSION).
     * @param entityField   The entity field name (e.g., Deal).
     * @param permissionMap The permission map for this module (null or empty, never populated here).
     */
    private void resolveNonRestrictedModuleParams(final SearchRequest searchRequest,
                                                  final Map<String, Object> paramsMap,
                                                  final String permissionKey,
                                                  final String entityField,
                                                  final Map<String, Map<String, Object>> permissionMap) {
        if (Objects.isNull(permissionMap)) {
            paramsMap.put(permissionKey, NOT_PERMITTED);
        } else {
            searchRequest.includeFields(presetIndexFieldsWeb.getEntityFields(entityField));
            paramsMap.put(permissionKey, CommonSearchFields.GLOBAL);
        }
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
        return Objects.nonNull(permissionMap) && !permissionMap.isEmpty();
    }

    /**
     * <p>
     * Includes fields selectively when no module needs scripting (scriptId is null).
     * Includes base required fields for the index, then entity fields only for modules
     * that have access (non-null). Modules with no access (null) are skipped entirely.
     * </p>
     *
     * @param searchRequest       {@link SearchRequest}.
     * @param index               Index name (e.g., activity, deal, note, file).
     * @param dealPermissionMap   Deal permission map (null if deal not applicable for this index).
     * @param contactPermissionMap Contact permission map.
     * @param companyPermissionMap Company permission map.
     */
    private void includeFieldsForAccessibleModules(final SearchRequest searchRequest, final String index,
                                                   final Map<String, Map<String, Object>> dealPermissionMap,
                                                   final Map<String, Map<String, Object>> contactPermissionMap,
                                                   final Map<String, Map<String, Object>> companyPermissionMap) {
        searchRequest.includeFields(presetIndexFieldsWeb.getRequiredFields(index));

        if (Objects.nonNull(dealPermissionMap)) {
            searchRequest.includeFields(presetIndexFieldsWeb.getEntityFields(CommonSearchFields.DEAL));
        }

        if (Objects.nonNull(contactPermissionMap)) {
            searchRequest.includeFields(presetIndexFieldsWeb.getEntityFields(CommonSearchFields.CONTACT));
        }

        if (Objects.nonNull(companyPermissionMap)) {
            searchRequest.includeFields(presetIndexFieldsWeb.getEntityFields(CommonSearchFields.COMPANY));
        }
    }
}

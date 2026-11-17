package com.tlc.crm.entity.search.loader.impl;

import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.crm.entity.search.fields.PresetIndexFields;
import com.tlc.crm.entity.search.loader.SearchRequestLoader;
import com.tlc.crm.entity.search.script.ScriptKeysWebClient;
import com.tlc.search.model.ScriptInfo;
import com.tlc.search.model.SearchRequest;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Builds search requests by enabling scripts/fields based on index.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public abstract class AbstractSearchRequestLoader implements SearchRequestLoader {

    protected static final String FIELDS = "fields";
    protected static final String CONTACT_PERMISSION = "contactPermission";
    protected static final String COMPANY_PERMISSION = "companyPermission";
    protected static final String DEAL_PERMISSION = "dealPermission";
    protected static final String NOT_PERMITTED = "notPermitted";

    protected AbstractSearchRequestLoader() {
    }

    /**
     * <p>
     * Loads search request for contact index.
     * </p>
     *
     * @param searchRequest        {@link SearchRequest}.
     * @param companyPermissionMap Key value pair referring company related permissions.
     */
    public void loadContactIndexRequest(final SearchRequest searchRequest, final Map<String, Map<String, Object>> companyPermissionMap) {
        final PresetIndexFields presetIndexFields = getPresetIndexFields();

        if (Objects.isNull(companyPermissionMap)) {
            searchRequest.includeFields(presetIndexFields.getRequiredFields(CommonSearchFields.CONTACT));
        } else if (companyPermissionMap.isEmpty()) {
            searchRequest.includeFields(presetIndexFields.getGlobalAccessRequiredFields(CommonSearchFields.CONTACT));
            searchRequest.excludeFields(presetIndexFields.getGlobalAccessIgnoredFields(CommonSearchFields.CONTACT));
        } else {
            searchRequest.includeFields(presetIndexFields.getRequiredFields(CommonSearchFields.CONTACT));

            for (final Map.Entry<String, Map<String, Object>> scriptParamsEntry : companyPermissionMap.entrySet()) {
                final String scriptId = switch (scriptParamsEntry.getKey()) {
                    case CommonSearchFields.TERRITORY -> ScriptKeysWebClient.CONTACT_COMPANY_TERRITORY_SCRIPT;
                    case CommonSearchFields.RESTRICTED -> ScriptKeysWebClient.CONTACT_COMPANY_RESTRICTED_SCRIPT;
                    case CommonSearchFields.FOLLOWER -> ScriptKeysWebClient.CONTACT_COMPANY_FOLLOWER_SCRIPT;
                    default -> null;
                };
                searchRequest.setScripts(List.of(new ScriptInfo(scriptId, CommonSearchFields.COMPANY, scriptParamsEntry.getValue())));
            }
        }
    }

    /**
     * <p>
     * Gets {@link PresetIndexFields}.
     * </p>
     *
     * @return {@link PresetIndexFields}.
     */
    protected abstract PresetIndexFields getPresetIndexFields();

    /**
     * <p>
     * Sets scripts in the params map.
     * </p>
     *
     * @param paramsMap       Map holding details pertaining to scripts.
     * @param permissionKey   Permission key.
     * @param scriptParamsMap Key value pair referring entity's permissions.
     */
    protected void putScriptIntoParamsMap(final Map<String, Object> paramsMap, final String permissionKey,
                                          final Map<String, Map<String, Object>> scriptParamsMap) {
        for (final Map.Entry<String, Map<String, Object>> scriptParamsEntry : scriptParamsMap.entrySet()) {
            paramsMap.put(permissionKey, scriptParamsEntry.getKey());
            paramsMap.putAll(scriptParamsEntry.getValue());
        }
    }
}
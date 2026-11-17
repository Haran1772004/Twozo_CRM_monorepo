package com.tlc.crm.entity.search.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.common.action.AbstractCrmAction;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;

import com.tlc.search.SearchService;
import com.tlc.search.fields.ScriptKeys;
import com.tlc.search.internal.script.ScriptFileParser;
import com.tlc.search.model.ScriptDefinition;
import com.tlc.search.service.ScriptService;
import com.tlc.search.service.provider.ScriptResourceRegistrar;
import com.tlc.search.service.provider.SearchServiceProvider;

import com.tlc.web.HttpMethod;
import com.tlc.web.HttpStatus;
import com.tlc.web.WebAction;

import java.io.InputStream;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Manages stored script upload operations for the search engine.
 * This endpoint is secured with a password header instead of session-based authentication.
 * </p>
 *
 * <p>
 * Discovers individual {@code .painless} script files from the {@code stored_scripts/} directory
 * across all modules on the classpath and uploads the parsed scripts to the search engine.
 * </p>
 *
 * @author Ajay
 * @version 1.0
 */
@WebAction(path = "/search/script/mgmt", method = HttpMethod.POST)
public class ScriptUploadManagement extends AbstractCrmAction {

    private final String configuredPassword;

    public ScriptUploadManagement() {
        final Configurations configurations = ConfHolder.getInstance().getProperties(ScriptKeys.SEARCH_CONFIG);

        this.configuredPassword = configurations.getConfig(ScriptKeys.AUTH_PASSWORD_CONFIG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final CrmRequest request, final CrmResponse crmResponse) throws Exception {
        final String authHeader = request.header(ScriptKeys.AUTH_HEADER);

        if (Objects.isNull(authHeader) || !configuredPassword.equals(authHeader)) {
            sendError(crmResponse, HttpStatus.UNAUTHORIZED, ErrorCode.get(ErrorCodes.AUTHORIZATION_FAILED, "Invalid or missing authentication"));
            return;
        }

        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.optString(ScriptKeys.TYPE, ScriptKeys.UPLOAD_ALL);
        final SearchService searchService = SearchServiceProvider.get();
        final ScriptService scriptService = searchService.scriptService();

        switch (type) {
            case ScriptKeys.UPLOAD_ALL -> {
                final Map<String, InputStream> scriptFiles = ScriptResourceRegistrar.get().getScriptFiles();
                final Collection<ScriptDefinition> scripts = ScriptFileParser.parse(scriptFiles);

                scriptService.uploadAll(scripts);
                crmResponse.put(ScriptKeys.STATUS, "success");
                crmResponse.put(ScriptKeys.MESSAGE, String.format("Uploaded %d scripts", scripts.size()));
            }
            case ScriptKeys.UPLOAD -> {
                final String scriptId = requestJson.getString(ScriptKeys.SCRIPT_ID);
                final String scriptSource = requestJson.getString(ScriptKeys.SCRIPT_SOURCE);

                if (Objects.isNull(scriptId) || Objects.isNull(scriptSource)) {
                    throw ErrorCode.get(ErrorCodes.INVALID_DATA, "scriptId and scriptSource are required");
                }

                scriptService.upload(new ScriptDefinition(scriptId, scriptSource));
                crmResponse.put(ScriptKeys.STATUS, "success");
                crmResponse.put(ScriptKeys.MESSAGE, String.format("Uploaded script: %s", scriptId));
            }
            case ScriptKeys.DELETE -> {
                final String scriptId = requestJson.getString(ScriptKeys.SCRIPT_ID);

                if (Objects.isNull(scriptId)) {
                    throw ErrorCode.get(ErrorCodes.INVALID_DATA, "scriptId is required");
                }

                scriptService.delete(scriptId);
                crmResponse.put(ScriptKeys.STATUS, "success");
                crmResponse.put(ScriptKeys.MESSAGE, String.format("Deleted script: %s", scriptId));
            }
            default -> throw ErrorCode.get(ErrorCodes.INVALID_DATA, String.format("Unknown type: %s", type));
        }

        crmResponse.setStatus(HttpStatus.OK);
        crmResponse.send();
    }

}


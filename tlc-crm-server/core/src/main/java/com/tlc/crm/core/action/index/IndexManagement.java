package com.tlc.crm.core.action.index;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.common.action.AbstractCrmAction;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MDODEAL;
import com.tlc.crm.core.sql.resource.MPOPRODUCT;
import com.tlc.crm.entity.sql.resource.OMACACTIVITY;
import com.tlc.crm.entity.search.fields.CommonSearchFields;
import com.tlc.search.SearchService;
import com.tlc.search.fields.ScriptKeys;
import com.tlc.search.service.IndexService;
import com.tlc.search.service.provider.SearchServiceProvider;

import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpStatus;
import com.tlc.web.WebAction;

import java.util.Objects;

/**
 * <p>
 * Provides the total document count for a given index in the search engine.
 * This endpoint is secured with a password header instead of session-based authentication.
 * </p>
 *
 * @author Ajay
 * @version 1.0
 */
@WebAction(path = "/search/index/mgmt", method = HttpMethod.POST)
public class IndexManagement extends AbstractCrmAction {

    private final String configuredPassword;

    public IndexManagement() {
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
            sendError(crmResponse, HttpStatus.FORBIDDEN, ErrorCode.get(ErrorCodes.ACCESS_DENIED));
            return;
        }

        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(CommonSearchFields.TYPE);
        final String index = requestJson.getString(CommonSearchFields.INDEX);

        if (index.isBlank()) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA);
        }

        switch (type) {
            case CommonSearchFields.INDEX_COUNT -> handleIndexCount(index, crmResponse);
            case CommonSearchFields.META_COUNT -> handleMetaCount(index, crmResponse);
        }
    }

    /**
     * <p>
     * Handles the request to get the total document count for a specified index.
     * </p>
     *
     * @param index       Index name.
     * @param crmResponse {@link CrmResponse}.
     */
    private void handleIndexCount(final String index, final CrmResponse crmResponse) {
        final SearchService searchService = SearchServiceProvider.get();
        final IndexService indexService = searchService.indexService();
        final long count = indexService.count(index);

        crmResponse.put(CommonSearchFields.INDEX, index);
        crmResponse.put(CommonSearchFields.COUNT, count);
        crmResponse.setStatus(HttpStatus.OK);
        crmResponse.send();
    }

    /**
     * <p>
     * Handles the request to get the total meta count for a specified index.
     * </p>
     *
     * @param index       Index name.
     * @param crmResponse {@link CrmResponse}.
     */
    private void handleMetaCount(final String index, final CrmResponse crmResponse) {
        final Table table = switch (index) {
            case CommonSearchFields.COMPANY -> Table.get(MCOCOMPANY.TABLE);
            case CommonSearchFields.CONTACT -> Table.get(MCOCONTACT.TABLE);
            case CommonSearchFields.DEAL -> Table.get(MDODEAL.TABLE);
            case CommonSearchFields.PRODUCT -> Table.get(MPOPRODUCT.TABLE_NAME);
            case CommonSearchFields.ACTIVITY -> Table.get(OMACACTIVITY.TABLE);
            default -> throw ErrorCode.get(ErrorCodes.INVALID_DATA);
        };
        crmResponse.put(CommonSearchFields.COUNT, adminDataStore().get(CountQuery.get(table)));
        crmResponse.setStatus(HttpStatus.OK);
        crmResponse.send();
    }

    /**
     * <p>
     * Gets the {@link AdminDataStore}
     * </p>
     *
     * @return the {@link AdminDataStore}
     */
    private AdminDataStore adminDataStore() {
        return SqlServiceProvider.get().getAdminDataStore();
    }
}
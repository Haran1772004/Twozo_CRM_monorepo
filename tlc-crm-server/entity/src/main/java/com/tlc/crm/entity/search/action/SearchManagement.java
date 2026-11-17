package com.tlc.crm.entity.search.action;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;

import com.tlc.crm.entity.search.action.handler.SearchClient;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;

import com.tlc.web.WebAction;

/**
 * <p>
 * Manages the search related web actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@SourceAuthorizer(relatedApiTypes = SourceAuthorizer.RelatedApiType.SEARCH, needRelatedApiHandling = true)
@WebAction(path = "/search")
public class SearchManagement extends AuthorizationController {

    private final SearchClient webClient;
    private final SearchClient mobileClient;

    public SearchManagement() {
        webClient = SearchClient.getWebClient();
        mobileClient = SearchClient.getMobileClient();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        webClient.process(request, crmResponse);
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the {@link CrmRequest}
     * @param response Represents the {@link CrmResponse}
     */
    @Override
    protected void handleMobileRequest(final CrmRequest request, final CrmResponse response) {
        mobileClient.process(request, response);
    }
}
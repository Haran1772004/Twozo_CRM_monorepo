package com.tlc.crm.entity.search.action.handler;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.entity.search.action.handler.impl.SearchMobileClient;
import com.tlc.crm.entity.search.action.handler.impl.SearchWebClient;

/**
 * <p>
 * Processes search related api requests.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface SearchClient {

    /**
     * <p>
     * Processes search requests.
     * </p>
     *
     * @param request     {@link CrmRequest}.
     * @param crmResponse {@link CrmResponse}.
     */
    void process(final CrmRequest request, final CrmResponse crmResponse);

    /**
     * <p>
     * Gets the {@link SearchWebClient}.
     * </p>
     *
     * @return {@link SearchClient}.
     */
    static SearchClient getWebClient() {
        return SearchWebClient.getInstance();
    }

    /**
     * <p>
     * Gets the {@link SearchMobileClient}.
     * </p>
     *
     * @return {@link SearchClient}.
     */
    static SearchClient getMobileClient() {
        return SearchMobileClient.getInstance();
    }
}
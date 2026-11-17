package com.tlc.crm.entity.search.loader;

import com.tlc.crm.entity.search.loader.impl.MobileSearchRequestLoader;
import com.tlc.crm.entity.search.loader.impl.WebSearchRequestLoader;
import com.tlc.search.model.SearchRequest;

import java.util.Map;

/**
 * <p>
 * Loads search request based on the index.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface SearchRequestLoader {

    /**
     * <p>
     * Gets the web client request loader.
     * </p>
     *
     * @return {@link SearchRequestLoader}.
     */
    static SearchRequestLoader getWebClientRequestLoader() {
        return WebSearchRequestLoader.getInstance();
    }

    /**
     * <p>
     * Gets the mobile client request loader.
     * </p>
     *
     * @return {@link SearchRequestLoader}.
     */
    static SearchRequestLoader getMobileClientRequestLoader() {
        return MobileSearchRequestLoader.getInstance();
    }

    /**
     * <p>
     * Loads search request for contact index.
     * </p>
     *
     * @param searchRequest        {@link SearchRequest}.
     * @param companyPermissionMap Key value pair referring company related permissions.
     */
    void loadContactIndexRequest(final SearchRequest searchRequest, final Map<String, Map<String, Object>> companyPermissionMap);

    /**
     * <p>
     * Loads search request for company index.
     * </p>
     *
     * @param searchRequest {@link SearchRequest}.
     */
    void loadCompanyIndexRequest(final SearchRequest searchRequest);

    /**
     * <p>
     * Loads search request for deal index.
     * </p>
     *
     * @param searchRequest        {@link SearchRequest}.
     * @param contactPermissionMap Key value pair referring contact related permissions.
     * @param companyPermissionMap Key value pair referring company related permissions.
     */
    void loadDealIndexRequest(final SearchRequest searchRequest, final Map<String, Map<String, Object>> contactPermissionMap,
                              final Map<String, Map<String, Object>> companyPermissionMap);

    /**
     * <p>
     * Loads search request for product index.
     * </p>
     *
     * @param searchRequest {@link SearchRequest}.
     */
    void loadProductIndexRequest(final SearchRequest searchRequest);

    /**
     * <p>
     * Loads search request for activity index.
     * </p>
     *
     * @param searchRequest        {@link SearchRequest}.
     * @param contactPermissionMap Key value pair referring contact related permissions.
     * @param companyPermissionMap Key value pair referring company related permissions.
     */
    void loadActivityIndexRequest(final SearchRequest searchRequest, final Map<String, Map<String, Object>> dealPermissionMap,
                                  final Map<String, Map<String, Object>> contactPermissionMap,
                                  final Map<String, Map<String, Object>> companyPermissionMap);

    /**
     * <p>
     * Loads search request for note and file index.
     * </p>
     *
     * @param searchRequest        {@link SearchRequest}.
     * @param contactPermissionMap Key value pair referring contact related permissions.
     * @param companyPermissionMap Key value pair referring company related permissions.
     */
    void loadNoteAndFileIndexRequest(final SearchRequest searchRequest, final Map<String, Map<String, Object>> contactPermissionMap,
                                     final Map<String, Map<String, Object>> companyPermissionMap, final String index);

    /**
     * <p>
     * Loads search request for globally accessed entities.
     * </p>
     *
     * @param index         Index's name.
     * @param searchRequest {@link SearchRequest}.
     */
    void loadRequestForGlobalAccessibleModules(final String index, final SearchRequest searchRequest);
}

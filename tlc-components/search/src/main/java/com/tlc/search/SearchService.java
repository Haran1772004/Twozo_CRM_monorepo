package com.tlc.search;

import com.tlc.commons.service.Service;
import com.tlc.search.service.ClusterService;
import com.tlc.search.service.IndexService;
import com.tlc.search.service.ReadableDocumentService;
import com.tlc.search.service.OrgDocumentService;
import com.tlc.search.service.ScriptService;
import com.tlc.search.service.WritableDocumentService;

/**
 * <p>
 * Provides the service for pre searching process and searching the available data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface SearchService extends Service {

    /**
     * <p>
     * Provides the service related to document. (like - index the document, update the indexed document, delete the indexed document, etc,...)
     * </p>
     *
     * @return {@link WritableDocumentService} provides the service related to document.
     */
    WritableDocumentService getWritableService();

    /**
     * <p>
     * Provides the readable service to execute the services like retrieve, search, count, etc,...)
     * </p>
     *
     * @param orgId    Represents the organisation id.
     * @return {@link ReadableDocumentService} Which is used to execute read operations.
     */
    ReadableDocumentService getReadableService(final Long orgId);

    /**
     * <p>
     * Provides the service to execute the organisation related services like readable and writable)
     * </p>
     *
     * @param orgId    Represents the organisation id.
     * @return {@link OrgDocumentService} Which is used to execute read and write operations.
     */
    OrgDocumentService getOrgDocumentService(final Long orgId);

    /**
     * <p>
     * Provides the index service to execute the index related services (like - create, clone, delete, etc,...)
     * </p>
     *
     * @return {@link IndexService} Which is used to execute index related operations.
     */
    IndexService indexService();

    /**
     * <p>
     * Provides the cluster service to execute the cluster related services.
     * </p>
     *
     * @return {@link ClusterService} Which is used to execute cluster related operations.
     */
    ClusterService clusterService();

    /**
     * <p>
     * Provides the script service to manage stored scripts in the search engine.
     * </p>
     *
     * @return {@link ScriptService} Which is used to execute script related operations.
     */
    ScriptService scriptService();
}

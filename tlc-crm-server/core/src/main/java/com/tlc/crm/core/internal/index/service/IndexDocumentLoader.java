package com.tlc.crm.core.internal.index.service;

import com.tlc.crm.core.internal.index.service.impl.IndexDocumentLoaderImpl;
import com.tlc.validator.TlcModel;

import java.util.Map;

/**
 * <p>
 * Loads data into the document for indexing.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface IndexDocumentLoader {

    /**
     * <p>
     * Gets the {@link IndexDocumentLoader} Instance
     * </p>
     *
     * @return {@link IndexDocumentLoader}
     */
    static IndexDocumentLoader get() {
        return IndexDocumentLoaderImpl.getInstance();
    }

    /**
     * <p>
     * Loads follower ids for contact index.
     * </p>
     *
     * @param doc Holds the entity data to be indexed.
     */
    void loadFollowerIdsForContactIndex(final Long orgId, final Map<String, Object> doc);

    /**
     * <p>
     * Loads follower ids for company index.
     * </p>
     *
     * @param doc Holds the entity data to be indexed.
     */
    void loadFollowerIdsForCompanyIndex(final Long orgId, final Map<String, Object> doc);

    /**
     * <p>
     * Loads follower ids for deal index.
     * </p>
     *
     * @param doc Holds the entity data to be indexed.
     */
    void loadFollowerIdsForDealIndex(final Long orgId, final Map<String, Object> doc);

    /**
     * <p>
     * Loads follower ids for activity index.
     * </p>
     *
     * @param doc Holds the entity data to be indexed.
     */
    void loadFollowerIdsForActivityIndex(final Long orgId, final Map<String, Object> doc);

    /**
     * <p>
     * Load User, Territory, Followers related fields in the doc.
     * </p>
     *
     * @param doc Holds the entity data to be indexed.
     */
    void loadPermissionRelatedFieldsForNoteIndex(final Long orgId, final Map<String, Object> doc);

    /**
     * <p>
     * Load User, Territory, Followers related fields in the doc.
     * </p>
     *
     * @param doc Holds the entity data to be indexed.
     */
    void loadPermissionRelatedFieldsForFileIndex(final Long orgId, final Map<String, Object> doc);

    /**
     * <p>
     * Load follower ids onto an entity card map (root + nested paths per mapping).
     * </p>
     *
     * @param orgId     An organization's id.
     * @param className {@link TlcModel} name.
     * @param doc       Holds the entity data to be indexed.
     */
    void loadFollowerIds(final Long orgId, final String className, final Map<String, Object> doc);

    /**
     * <p>
     * Loads follower ids on a source-owned note/file document (nested source entity + root).
     * </p>
     *
     * @param orgId  Organization's id.
     * @param source Entity source name ({@code deal}/{@code contact}/{@code company}).
     * @param doc    Note/file document that already contains the nested source map.
     */
    void loadFollowerIdsForSourceOwnedDoc(final Long orgId, final String source, final Map<String, Object> doc);
}
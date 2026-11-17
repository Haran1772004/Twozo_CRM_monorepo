package com.tlc.crm.core.features.internal.dataimport.search;

import com.tlc.crm.core.features.internal.dataimport.container.RevertContainer;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.search.impl.ModelSearchIndexerImpl;

import java.util.Collection;

/**
 * The handler designed to facilitate and coordinate
 * search-related activities for the imported and reverted models.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ModelSearchIndexer {

    /**
     * Gets the instance of {@link ModelSearchIndexer}.
     *
     * @return {@link ModelSearchIndexer}.
     */
    static ModelSearchIndexer getInstance() {
        return ModelSearchIndexerImpl.getInstance();
    }

    /**
     * Indexes the imported models for the search-related operations, excluding those with indices listed as invalid.
     *
     * @param orgId                Represents an organization's id.
     * @param sourceModelContainer Is a repository that encompasses all the model values extracted from the source record container.
     * @param invalidIndices       Which contains the indices of invalid models.
     */
    void indexImportedModels(final Long orgId, final SourceModelContainer sourceModelContainer, final Collection<Integer> invalidIndices);

    /**
     * Indexes the reverted models for the search-related operations, excluding those with indices listed as invalid.
     *
     * @param orgId           Represents an organization's id.
     * @param revertContainer Which contains the models that are eligible for the revert process, accompanied by their corresponding data containers containing the relevant rows.
     * @param invalidIndices  Which contains the indices of invalid models.
     */
    void indexRevertedModels(final Long orgId, final RevertContainer revertContainer, final Collection<Integer> invalidIndices);
}

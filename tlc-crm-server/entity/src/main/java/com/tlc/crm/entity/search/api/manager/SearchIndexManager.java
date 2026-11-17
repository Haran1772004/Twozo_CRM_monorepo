package com.tlc.crm.entity.search.api.manager;

import com.tlc.crm.entity.search.api.manager.impl.SearchIndexManagerImpl;
import com.tlc.crm.entity.search.api.model.Index;

import com.tlc.search.model.AnalysisComponent;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;

/**
 * <p>
 * Provides the {@link Index} related DB fetch action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface SearchIndexManager {

    static SearchIndexManager getInstance() {
        return SearchIndexManagerImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the table of the index.
     * </p>
     *
     * @return MCSIndex table.
     */
    Table getTable();

    /**
     * <>
     * Gets the index components table.
     * </>
     *
     * @return the index components table.
     */
    Table getIndexComponentsTable();

    /**
     * <p>
     * Fetches all the indexes available in the DB.
     * </p>
     *
     * @return Collection of {@link Index} holds the details of index.
     */
    Collection<Index> getAll();

    /**
     * <p>
     * Fetches the {@link Index} for the given id.
     * </p>
     *
     * @param id Represents the index id.
     * @return {@link Index} holds the details of index.
     */
    Index get(final Long id);

    /**
     * <p>
     * Fetches the {@link Index} for the given name.
     * </p>
     *
     * @param name Represents the index name.
     * @return {@link Index} holds the details of index.
     */
    Index getByName(final String name);

    /**
     * <p>
     * Gets {@link AnalysisComponent}.
     * </p>
     *
     * @param componentRow {@link Row}.
     * @return {@link AnalysisComponent}.
     */
    AnalysisComponent getAnalyzerComponent(final Row componentRow);
}

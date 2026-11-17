package com.tlc.crm.entity.search.api.manager;

import com.tlc.crm.entity.search.api.manager.impl.SearchFieldManagerImpl;
import com.tlc.crm.entity.search.api.model.IndexField;

import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;

/**
 * <p>
 * Provides the {@link IndexField} related DB fetch action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface SearchFieldManager {

    static SearchFieldManager getInstance() {
        return SearchFieldManagerImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the table of the fields.
     * </p>
     *
     * @return MCSFields table.
     */
    Table getTable();

    /**
     * <p>
     * Fetches all the available {@link IndexField} in the DB.
     * </p>
     *
     * @return Collection of {@link IndexField} holds the details of the index field.
     */
    Collection<IndexField> getAll();

    /**
     * <p>
     * Fetches the collection of {@link IndexField} for the given index id.
     * </p>
     *
     * @param indexId For fetch the corresponding collection of {@link IndexField}.
     * @return Collection of {@link IndexField} holds the details of the field of index.
     */
    Collection<IndexField> getByIndex(final Long indexId);

    /**
     * <p>
     * Fetches the collection of {@link IndexField} for the given index ids.
     * </p>
     *
     * @param indexIds For fetch the corresponding collection of {@link IndexField}.
     * @return Collection of {@link IndexField} holds the details of the field of index.
     */
    Collection<IndexField> getByIndexes(final Collection<Long> indexIds);
}

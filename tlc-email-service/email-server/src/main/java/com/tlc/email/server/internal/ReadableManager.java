package com.tlc.email.server.internal;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Provides methods to retrieve email entities from the underlying data source.
 * Focuses solely on data retrieval operations.
 *
 * @param <T> the type of the email model
 * @author kowsalya SP
 * @version 1.0
 */
public interface ReadableManager<T> {

    /**
     * Gets the {@link ReadableDataStore}
     *
     * @return {@link ReadableDataStore}
     */
    default ReadableDataStore readableDataStore() {
        return SqlServiceProvider.get().getAdminDataStore();
    }

    /**
     * Fetches a row that has the given id.
     *
     * @param id Refers the email id.
     */
    Optional<T> get(final long id);

    /**
     * Fetches a row that has the given id.
     *
     * @param ids Refers the email ids.
     */
    Collection<T> get(final Collection<Long> ids);

    /**
     * Converts the row data into model entity
     *
     * @param row           email row to be converted.
     * @param dataContainer A container which holds the updated information about the emails
     * @return email
     */
    T convertRowToModel(final Row row, final DataContainer dataContainer);

    /**
     * Checks whether any rows are having the given id.
     *
     * @param id Entity id.
     * @return true if exists, false otherwise.
     */
    boolean exists(final long id);

    /**
     * <p>
     * Checks whether the collection of email model exists or not.
     * </p>
     *
     * @param ids Represents the {@link Collection} of email ids
     * @return true, if the given {@link Collection} of email are exists; otherwise false
     */
    boolean exists(final Set<Long> ids);
}

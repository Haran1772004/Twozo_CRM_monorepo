package com.tlc.email.server.internal;

import com.tlc.sql.common.api.ds.WritableDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import com.tlc.email.common.model.EmailModel;

import java.util.Collection;

/**
 * Provides methods to modifying email entities from the underlying data source.
 *
 * @param <T> the type of the email model
 * @author kowsalya SP
 * @version 1.0
 */
public interface WritableManager<T> extends ReadableManager<T> {

    /**
     * Gets the {@link WritableDataStore}
     *
     * @return {@link WritableDataStore}
     */
    default WritableDataStore writableDataStore() {
        return SqlServiceProvider.get().getAdminDataStore();
    }

    /**
     * Creates a new email data using the provided {@link EmailModel}.
     *
     * @param emailModel the data required to create the email
     */
    void create(final T emailModel);

    /**
     * Updates an existing record of email.
     *
     * @param emailModel the updated email data
     */
    void update(final T emailModel);

    /**
     * Updates an existing record of email.
     *
     * @param emailModel the updated email data
     */
    void update(final Collection<T> emailModel);

    /**
     * Deletes the rows for the given ids.
     *
     * @param ids Refers the ids
     */
    void delete(final Collection<Long> ids);

    /**
     * Deletes the row for the given id.
     *
     * @param id Refers the reference of email model
     */
    void delete(final Long id);
}

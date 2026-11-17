package com.tlc.sync.service.oauth.internal.manager;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Table;

import com.tlc.sync.service.oauth.internal.manager.impl.SyncManagerImpl;
import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import com.tlc.sync.service.oauth.internal.email.resolver.EmailSyncTokenResolver;
import com.tlc.sync.service.oauth.model.RefreshableToken;
import com.tlc.sync.service.oauth.model.request.TokenReadRequest;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Provides the functionalities for managing and retrieving the sync tokens.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface SyncManager {

    /**
     * <p>
     * Gets the instance for {@link EmailSyncTokenResolver}.
     * </p>
     *
     * @return {@link EmailSyncTokenResolver}
     */
    static SyncManager getInstance() {
        return SyncManagerImpl.getInstance();
    }

    /**
     * <p>
     * Gets the refreshable token table
     * </p>
     *
     * @return the refreshable token table
     */
    Table getRefreshableTokenTable();

    /**
     * <p>
     * Gets the refresh token for the given email address based on the @{@link SyncServiceCategory}
     * </p>
     *
     * @param tokenReadRequest Represents the {@link TokenReadRequest}
     * @return the refresh token
     */
    Optional<String> getNonRefreshableToken(final TokenReadRequest tokenReadRequest);

    /**
     * <p>
     * Stores the refresh token
     * </p>
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    void storeNonRefreshableToken(final TokenWriteRequest tokenWriteRequest);

    /**
     * <p>
     * Loads the refreshable token into container
     * </p>
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     * @param tokenContainer    Represents the token container
     * @param targetContainer   Represents the target container
     */
    void loadRefreshableTokenIntoContainer(final TokenWriteRequest tokenWriteRequest, final DataContainer tokenContainer, final DataContainer targetContainer);

    /**
     * <p>
     * Gets the collection of {@link RefreshableToken}
     * </p>
     *
     * @return the collection of {@link RefreshableToken}
     */
    Collection<RefreshableToken> getRefreshableTokens();

    /**
     * <p>
     * Updates the refreshable token
     * </p>
     *
     * @param refreshableToken Represents the {@link RefreshableToken} to be updated
     */
    void updateRefreshableToken(final RefreshableToken refreshableToken);
}

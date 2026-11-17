package com.tlc.crm.common.iam.cache.internal.permission;

import com.tlc.web.Action;

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * <p>
 * An interface that offers functionality for managing authorizers related to the {@link Action}
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PermissionAuthorizerStore {

    static PermissionAuthorizerStore getInstance() {
        return PermissionAuthorizerStoreImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the annotations registered in the given {@link Action}.
     * </p>
     *
     * @param action {@link Action} which handles the web request.
     * @return Collection of annotations.
     */
    Collection<Annotation> get(final Action action);

    /**
     * <p>
     * Loads all the authorizers registered in the given {@link Action}.
     * </p>
     *
     * @param action {@link Action} which is manages the request.
     */
    void loadAuthorizers(final Action action);
}

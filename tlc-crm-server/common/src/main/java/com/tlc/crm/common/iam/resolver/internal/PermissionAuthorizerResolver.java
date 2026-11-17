package com.tlc.crm.common.iam.resolver.internal;

import com.tlc.web.Action;

import java.lang.annotation.Annotation;

import java.util.Collection;

/**
 * <p>
 * A resolver that resolves the permission authorizer of the {@link Action}
 * </P>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PermissionAuthorizerResolver {

    /**
     * <p>
     * Gets the {@link PermissionAuthorizerResolver} instance
     * </p>
     *
     * @return The {@link PermissionAuthorizerResolver} instance
     */
    static PermissionAuthorizerResolver getInstance() {
        return PermissionAuthorizerResolverImpl.getInstance();
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
}

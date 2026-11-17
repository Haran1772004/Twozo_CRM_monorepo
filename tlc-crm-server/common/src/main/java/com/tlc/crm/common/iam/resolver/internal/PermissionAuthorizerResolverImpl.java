package com.tlc.crm.common.iam.resolver.internal;

import com.tlc.crm.common.iam.cache.internal.permission.PermissionAuthorizerStore;
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
public class PermissionAuthorizerResolverImpl implements PermissionAuthorizerResolver {

    private final PermissionAuthorizerStore permissionAuthorizerStore;

    private PermissionAuthorizerResolverImpl() {
        permissionAuthorizerStore = PermissionAuthorizerStore.getInstance();
    }

    private static class Instance {
        private static final PermissionAuthorizerResolverImpl INSTANCE = new PermissionAuthorizerResolverImpl();
    }

    /**
     * <p>
     * Gets the {@link PermissionAuthorizerResolver} instance
     * </p>
     *
     * @return The {@link PermissionAuthorizerResolverImpl} instance
     */
    public static PermissionAuthorizerResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param action {@link Action} which handles the web request.
     * @return Collection of annotations.
     */
    @Override
    public Collection<Annotation> get(final Action action) {
        final Collection<Annotation> annotations = permissionAuthorizerStore.get(action);

        if (annotations.isEmpty()) {
            permissionAuthorizerStore.loadAuthorizers(action);

            return permissionAuthorizerStore.get(action);
        }
        return annotations;
    }
}

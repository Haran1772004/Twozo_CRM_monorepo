package com.tlc.crm.common.iam.service.internal.permission;

import com.tlc.crm.common.iam.resolver.internal.PermissionAuthorizerResolver;
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
public class PermissionAuthorizerServiceImpl implements PermissionAuthorizerService {

    private final PermissionAuthorizerResolver permissionAuthorizerResolver;

    private PermissionAuthorizerServiceImpl() {
        permissionAuthorizerResolver = PermissionAuthorizerResolver.getInstance();
    }

    private static class Instance {
        private static final PermissionAuthorizerServiceImpl INSTANCE = new PermissionAuthorizerServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link PermissionAuthorizerService} instance
     * </p>
     *
     * @return The {@link PermissionAuthorizerServiceImpl} instance
     */
    public static PermissionAuthorizerServiceImpl getInstance() {
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
        return permissionAuthorizerResolver.get(action);
    }
}

package com.tlc.crm.common.iam.service.internal.permission;

import com.tlc.web.Action;

import java.lang.annotation.Annotation;

import java.util.Collection;

/**
 * <p>
 * A service that provides the permission authorizer of the {@link Action}
 * </P>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PermissionAuthorizerService {

    /**
     * <p>
     * Gets the {@link PermissionAuthorizerService} instance
     * </p>
     *
     * @return The {@link PermissionAuthorizerService} instance
     */
    static PermissionAuthorizerService getInstance() {
        return PermissionAuthorizerServiceImpl.getInstance();
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

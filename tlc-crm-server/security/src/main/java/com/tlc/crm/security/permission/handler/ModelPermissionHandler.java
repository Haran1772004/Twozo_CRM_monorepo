package com.tlc.crm.security.permission.handler;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Handles the permission of the models
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public interface ModelPermissionHandler extends PermissionHandler {

    /**
     * <p>
     * Checks whether the model is editable or not
     * </p>
     *
     * @param orgId   Represents the organisation ID
     * @param modelId Represents the model ID
     * @return True if the model can be edited, false otherwise
     */
    boolean isEditable(final Long orgId, final Long modelId);

    /**
     * <p>
     * Checks whether the model is deletable or not
     * </p>
     *
     * @param orgId   Represents the organisation ID
     * @param modelId Represents the model ID
     * @return True if the model can be deleted, false otherwise
     */
    boolean isDeletable(final Long orgId, final Long modelId);

    /**
     * <p>
     * Gets the model permission access map
     * </p>
     *
     * @param orgId    Represents the organisation ID
     * @param modelIds Represents the collection of model id
     * @return The map containing key as model id and value as action access map
     */
    Map<Long, Map<String, Boolean>> getModelsActionPermissions(final Long orgId, final Collection<Long> modelIds);
}
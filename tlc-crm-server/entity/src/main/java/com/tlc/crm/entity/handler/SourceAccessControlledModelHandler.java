package com.tlc.crm.entity.handler;

import com.tlc.crm.common.models.AccessControlledModel;
import com.tlc.crm.security.model.PermissionHandlerRegistry;
import com.tlc.crm.security.model.PermissionHandlerType;
import com.tlc.crm.security.permission.handler.ModelPermissionHandler;
import com.tlc.crm.security.permission.handler.PermissionHandler;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 *  Handles the accessibility of the {@link AccessControlledModel}
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class SourceAccessControlledModelHandler implements AccessControlledModelHandler {

    private final ModelPermissionHandler modelPermissionHandler;
    private final boolean isPermissionHandlerEnabled;

    private SourceAccessControlledModelHandler() {
        final PermissionHandler permissionHandler = PermissionHandlerRegistry.getInstance().getPermissionHandler(PermissionHandlerType.SOURCE_MODEL_PERMISSION);

        modelPermissionHandler = Objects.nonNull(permissionHandler) ? (ModelPermissionHandler) permissionHandler : null;
        isPermissionHandlerEnabled = Objects.nonNull(modelPermissionHandler);
    }

    private static class Instance {
        private static final SourceAccessControlledModelHandler ACCESS_CONFIGURABLE_MODEL_HANDLER = new SourceAccessControlledModelHandler();
    }

    /**
     * <p>
     * Gets the instance of {@link SourceAccessControlledModelHandler}
     * </p>
     *
     * @return The instance of {@link SourceAccessControlledModelHandler}
     */
    public static SourceAccessControlledModelHandler getInstance() {
        return Instance.ACCESS_CONFIGURABLE_MODEL_HANDLER;
    }

    /**
     * <p>
     * Checks whether the model is editable or not
     * </p>
     *
     * @param orgId   Represents the organisation ID
     * @param modelId Represents the model ID
     * @return True if the model can be edited, false otherwise
     */
    public boolean isEditable(final Long orgId, final Long modelId) {
        return isPermissionHandlerEnabled && modelPermissionHandler.isEditable(orgId, modelId);
    }

    /**
     * <p>
     * Checks whether the model is deletable or not
     * </p>
     *
     * @param orgId   Represents the organisation ID
     * @param modelId Represents the model ID
     * @return True if the model can be deleted, false otherwise
     */
    public boolean isDeletable(final Long orgId, final Long modelId) {
        return isPermissionHandlerEnabled && modelPermissionHandler.isDeletable(orgId, modelId);
    }

    /**
     * <p>
     * Gets the model permission access map
     * </p>
     *
     * @param orgId    Represents the organisation ID
     * @param modelIds Represents the collection of model id
     * @return The map containing key as model id and value as action access map
     */
    public Map<Long, Map<String, Boolean>> getModelsPermissionAccess(final Long orgId, final Collection<Long> modelIds) {
        return isPermissionHandlerEnabled && !modelIds.isEmpty() ? modelPermissionHandler.getModelsActionPermissions(orgId, modelIds) : Collections.emptyMap();
    }
}
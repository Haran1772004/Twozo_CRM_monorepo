package com.tlc.crm.core.iam.role.permission.controller;

import com.tlc.crm.core.features.api.dataimport.manager.ImportManager;
import com.tlc.crm.core.features.api.dataimport.model.ImportType;
import com.tlc.crm.core.iam.role.permission.controller.request.ImportLimitControllerRequest;

import com.tlc.crm.entity.role.permission.controller.AbstractLimitController;

import com.tlc.crm.security.permission.controller.request.LimitControllerRequest;
import com.tlc.crm.security.permission.model.LimitActionType;

/**
 * <p>
 * Checker for checking the limitations of create the records via import.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ImportLimitController extends AbstractLimitController<ImportLimitControllerRequest> {

    private final ImportManager importManager;

    public ImportLimitController() {
        importManager = ImportManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param checkerRequest Represents the {@link LimitControllerRequest}.
     * @return True, if the action limit has not exceeded.
     */
    @Override
    public boolean canProceed(final ImportLimitControllerRequest checkerRequest) {
        return canProceed(checkerRequest.isCreateEvent() ? LimitActionType.IMPORT_CREATE : LimitActionType.IMPORT_UPDATE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId               Represents an organisation id.
     * @param userId              Represents an user id.
     * @param limitActionType Represents the {@link LimitActionType}.
     * @return Used count of the action.
     */
    @Override
    protected long getActionCount(final long orgId, final long userId, final LimitActionType limitActionType) {
        return switch (limitActionType) {
            case IMPORT_CREATE -> importManager.getImportCountOnDay(orgId, userId, ImportType.INSERT);
            case IMPORT_UPDATE -> importManager.getImportCountOnDay(orgId, userId, ImportType.UPDATE);
            default -> 0;
        };
    }
}

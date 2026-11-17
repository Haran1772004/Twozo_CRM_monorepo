package com.tlc.crm.core.iam.role.permission.validator;

import com.tlc.crm.core.features.api.dataimport.manager.ImportManager;
import com.tlc.crm.core.features.api.dataimport.model.ImportType;

import com.tlc.crm.entity.role.permission.validator.AbstractLimitValidator;

import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.crm.security.permission.model.LimitActionType;
import com.tlc.crm.security.permission.annotation.validator.ActionLimitValidator;

/**
 * <p>
 * Checker for checking the limitations of create the records via import.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@ActionLimitValidator(type = LimitActionType.IMPORT_CREATE)
public class ImportRecordCreationLimitValidator extends AbstractLimitValidator {

    private final ImportManager importManager;

    private static final class Instance {
        private static final ImportRecordCreationLimitValidator INSTANCE = new ImportRecordCreationLimitValidator();
    }

    private ImportRecordCreationLimitValidator() {
        importManager = ImportManager.getInstance();
    }

    @InstanceProvider
    public static ImportRecordCreationLimitValidator getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organisation id.
     * @param userId Represents an user id.
     * @return Used count of the action.
     */
    @Override
    protected long getActionCount(final long orgId, final long userId) {
        return importManager.getImportCountOnDay(orgId, userId, ImportType.INSERT);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link LimitActionType}.
     */
    @Override
    protected LimitActionType getLimitActionType() {
        return LimitActionType.IMPORT_CREATE;
    }
}

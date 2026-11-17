package com.tlc.crm.core.features.validator.internal;

import com.tlc.attachment.io.Extension;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.crm.core.features.field.dataimport.ImportFields;
import com.tlc.crm.core.features.validator.annotation.ImportFileValidator;
import com.tlc.file.common.models.Attachment;
import com.tlc.crm.entity.validator.ValidatorContext;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Collection;

/**
 * Validates the {@link Attachment} entity of an import and returns appropriate message.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class ImportFileValidatorImpl implements ConstraintValidator<ImportFileValidator, ImportInfo> {

    private static final int MAX_FILE_SIZE_IN_MEGABYTES = 1024 * 1024 * 50;

    /**
     * {@inheritDoc}
     *
     * @param importInfo Refers the {@link ImportInfo} details.
     * @param context    Context in which the constraint is evaluated.
     */
    @Override
    public boolean isValid(final ImportInfo importInfo, final ConstraintValidatorContext context) {
        final ValidatorContext validatorContext = new ValidatorContext(context);
        final Attachment attachment = importInfo.getAttachment();
        final Collection<Extension> supportedExtensions = Arrays.asList(Extension.CSV, Extension.XLS, Extension.XLSX);

        if (!supportedExtensions.contains(Extension.get(attachment.getExtension()))) {
            addConstraintViolationMessage(validatorContext, "i18n.import.attachment.invalid.extension", ImportFields.ATTACHMENT.getName());
        }

        if (attachment.getFileSize() > MAX_FILE_SIZE_IN_MEGABYTES) {
            addConstraintViolationMessage(validatorContext, "i18n.import.attachment.size.exceeds", ImportFields.ATTACHMENT.getName());
        }

        return !validatorContext.isErrorsFound();
    }

    /**
     * Adds the constraint violation message to the validation context.
     *
     * @param validatorContext Refers the validator context.
     * @param message          Violation message.
     */
    private void addConstraintViolationMessage(final ValidatorContext validatorContext, final String message, final String fieldName) {
        validatorContext.setErrorsFound(true);
        validatorContext.getContext().buildConstraintViolationWithTemplate(message).addPropertyNode(fieldName).addConstraintViolation();
    }
}

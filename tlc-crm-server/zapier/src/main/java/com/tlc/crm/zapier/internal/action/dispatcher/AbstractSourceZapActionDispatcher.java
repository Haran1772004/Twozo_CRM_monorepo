package com.tlc.crm.zapier.internal.action.dispatcher;

import com.tlc.commons.util.LoggerUtil;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.internal.event.index.publisher.IndexDataPublisher;

import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;

import com.tlc.validator.ModelValidator;
import com.tlc.validator.TlcModel;
import com.tlc.validator.service.provider.ValidatorServiceProvider;

import jakarta.validation.ConstraintViolation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract base implementation of the {@link SourceZapActionDispatcher} interface.
 * Provides common functionality and shared utility methods for handling Zapier actions
 * related to source entities.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
abstract class AbstractSourceZapActionDispatcher implements SourceZapActionDispatcher {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractSourceZapActionDispatcher.class);

    protected final OptionService optionService;
    protected final FieldService fieldService;

    private final IndexDataPublisher indexDataPublisher;

    protected AbstractSourceZapActionDispatcher() {
        this.indexDataPublisher = IndexDataPublisher.getInstance();
        this.optionService = OptionService.getInstance();
        this.fieldService = FieldService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param model  The model to validate.
     * @param groups Optional validation groups to apply.
     * @return A collection of validation error messages. If the model is valid, the collection will be empty.
     */
    @Override
    public Collection<String> validate(final TlcModel model, final Class<?>... groups) {
        final ModelValidator validator = ValidatorServiceProvider.get().modelValidator();
        final Set<ConstraintViolation<TlcModel>> violations = new HashSet<>(validator.validate(model, groups));

        return violations.stream().map(ConstraintViolation::getMessage).toList();
    }

    /**
     * Publishes index data for the specified organization ID, model, and action.
     *
     * @param orgId    The ID of the organization for which the data is indexed.
     * @param tlcModel The model data that needs to be indexed.
     * @param action   The action triggering this indexing operation.
     */
    protected final void index(final long orgId, final TlcModel tlcModel, final Action action) {
        try {
            indexDataPublisher.publishIndexData(orgId, List.of(tlcModel), action);
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Zapier Action Execution: Failed to index model. Id: {}, Org Id: {}, Reason: {}", tlcModel.id(), orgId, exception.getMessage());
        }
    }

}

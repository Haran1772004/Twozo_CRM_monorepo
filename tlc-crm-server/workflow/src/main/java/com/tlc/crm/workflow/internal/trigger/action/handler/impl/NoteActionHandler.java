package com.tlc.crm.workflow.internal.trigger.action.handler.impl;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.features.api.note.manager.SourceNoteManager;
import com.tlc.crm.core.features.api.note.model.NoteHolder;
import com.tlc.crm.core.features.api.note.model.converter.SourceNoteConverter;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.validator.TlcModel;

import com.tlc.crm.workflow.api.model.WorkflowAction;
import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.internal.trigger.action.formatter.NoteJsonFormatter;

/**
 * The handler which handles the {@link com.tlc.crm.entity.features.api.note.model.Note} related actions of a {@link com.tlc.crm.workflow.api.model.Workflow}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class NoteActionHandler extends AbstractActionHandler implements RelatedSourceActionHandler {

    private final SourceNoteConverter sourceNoteConverter;
    private final SourceNoteManager sourceNoteManager;
    private final NoteJsonFormatter noteJsonFormatter;

    private NoteActionHandler() {
        this.sourceNoteConverter = SourceNoteConverter.getInstance();
        this.sourceNoteManager = SourceNoteManager.getInstance();
        this.noteJsonFormatter = NoteJsonFormatter.getInstance();
    }

    private static class Instance {
        private static final NoteActionHandler INSTANCE = new NoteActionHandler();
    }

    /**
     * Gets the instance of {@link NoteActionHandler}.
     *
     * @return {@link NoteActionHandler} instance.
     */
    public static NoteActionHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                  The ID of the organization associated with the source.
     * @param userId                 The ID of the user requesting the mapping.
     * @param source                 The source object to be mapped.
     * @param workflowAction         Refers the workflow action.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return {@link TlcModel} representing the mapped data related to the provided source.
     */
    @Override
    public NoteHolder mapRelatedSource(final Long orgId, final Long userId, final Source source,
                                       final WorkflowAction workflowAction, final ActionExecutionContext actionExecutionContext) {
        final JsonObject noteJson = noteJsonFormatter.format(orgId, workflowAction.getId(), workflowAction.getFieldMappingAsJson(), actionExecutionContext, workflowAction.getTriggerActionSource());

        noteJson.put(ActionAttributes.SOURCE, source.getName());
        final NoteHolder noteHolder = sourceNoteConverter.convert(new ConverterConfig(orgId, userId, null, false, noteJson), noteJson);
        final NoteHolder sourceNoteHolder = sourceNoteManager.create(noteHolder);

        index(orgId, noteHolder, Action.CREATE);
        return sourceNoteHolder;
    }
}

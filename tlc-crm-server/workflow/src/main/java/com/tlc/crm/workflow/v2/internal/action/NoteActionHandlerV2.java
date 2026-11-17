package com.tlc.crm.workflow.v2.internal.action;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.features.api.note.manager.SourceNoteManager;
import com.tlc.crm.core.features.api.note.model.NoteHolder;
import com.tlc.crm.core.features.api.note.model.converter.SourceNoteConverter;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.internal.trigger.action.handler.impl.AbstractActionHandler;
import com.tlc.crm.workflow.internal.trigger.action.tracker.ActionFailureCauseTracker;
import com.tlc.crm.workflow.v2.api.model.ActionConfiguration;
import com.tlc.crm.workflow.v2.internal.action.formatter.NoteJsonFormatterV2;
import com.tlc.crm.workflow.v2.internal.execution.NodeExecutionContext;

/**
 * Handles the note actions for the workflow.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
final class NoteActionHandlerV2 extends AbstractActionHandler {

    private final SourceNoteConverter sourceNoteConverter;
    private final SourceNoteManager sourceNoteManager;
    private final NoteJsonFormatterV2 noteJsonFormatter;

    private NoteActionHandlerV2() {
        this.sourceNoteConverter = SourceNoteConverter.getInstance();
        this.sourceNoteManager = SourceNoteManager.getInstance();
        this.noteJsonFormatter = NoteJsonFormatterV2.getInstance();
    }

    private static class Instance {
        private static final NoteActionHandlerV2 INSTANCE = new NoteActionHandlerV2();
    }

    /**
     * Gets the instance of {@link NoteActionHandlerV2}.
     *
     * @return {@link NoteActionHandlerV2} instance.
     */
    static NoteActionHandlerV2 getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Maps the related source.
     *
     * @param orgId                     The organization id.
     * @param userId                    The user id.
     * @param source                    The source.
     * @param actionConfiguration       The action configuration.
     * @param nodeExecutionContext      The node execution context.
     * @param actionFailureCauseTracker The action failure cause tracker.
     * @return The note holder.
     */
    NoteHolder mapRelatedSource(final Long orgId, final Long userId, final Source source,
                                final ActionConfiguration actionConfiguration,
                                final NodeExecutionContext nodeExecutionContext, final ActionFailureCauseTracker actionFailureCauseTracker) {
        final JsonObject noteJson = noteJsonFormatter.format(orgId, actionConfiguration.id(), actionConfiguration.getFieldMappingAsJson(), nodeExecutionContext, actionConfiguration.getTriggerActionSource());

        noteJson.put(ActionAttributes.SOURCE, source.getName());
        final NoteHolder noteHolder = sourceNoteConverter.convert(new ConverterConfig(orgId, userId, null, false, noteJson), noteJson);
        final NoteHolder sourceNoteHolder = sourceNoteManager.create(noteHolder);

        index(orgId, noteHolder, Action.CREATE);
        return sourceNoteHolder;
    }
}

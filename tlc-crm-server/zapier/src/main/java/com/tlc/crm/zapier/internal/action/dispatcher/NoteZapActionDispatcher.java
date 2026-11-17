package com.tlc.crm.zapier.internal.action.dispatcher;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.features.api.note.manager.SourceNoteManager;
import com.tlc.crm.core.features.api.note.model.NoteHolder;
import com.tlc.crm.core.features.api.note.model.converter.SourceNoteConverter;

import com.tlc.crm.zapier.api.models.ZapAction;
import com.tlc.crm.zapier.api.models.ZapActionRequestContext;
import com.tlc.crm.zapier.fields.ZapAttributes;
import com.tlc.crm.zapier.internal.status.ZapierErrorCodes;
import com.tlc.crm.zapier.internal.action.formatter.NoteZapActionJsonFormatter;

import com.tlc.validator.type.Group;

import java.util.Collection;

/**
 * Implementation of {@link NoteZapActionDispatcher} for handling
 * Zapier actions related to note entities.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class NoteZapActionDispatcher extends AbstractSourceZapActionDispatcher {

    private final SourceNoteConverter sourceNoteConverter;
    private final SourceNoteManager sourceNoteManager;
    private final NoteZapActionJsonFormatter noteZapActionJsonFormatter;

    private NoteZapActionDispatcher() {
        this.sourceNoteConverter = SourceNoteConverter.getInstance();
        this.sourceNoteManager = SourceNoteManager.getInstance();
        this.noteZapActionJsonFormatter = NoteZapActionJsonFormatter.getInstance();
    }

    private static final class Instance {
        private static final NoteZapActionDispatcher ZAP_ACTION_DISPATCHER = new NoteZapActionDispatcher();
    }

    public static NoteZapActionDispatcher getInstance() {
        return Instance.ZAP_ACTION_DISPATCHER;
    }

    /**
     * {@inheritDoc}
     *
     * @param zapActionRequestContext The context containing the request, response, and action details.
     */
    @Override
    public void dispatch(final ZapActionRequestContext zapActionRequestContext) {
        final Long orgId = zapActionRequestContext.getOrgId();
        final Long userId = zapActionRequestContext.getUserId();
        final ZapAction zapAction = zapActionRequestContext.getZapAction();
        final CrmResponse response = zapActionRequestContext.getResponse();

        switch (zapAction) {
            case ADD_NOTE_TO_COMPANY, ADD_NOTE_TO_CONTACT, ADD_NOTE_TO_DEAL -> {
                try {
                    final JsonObject noteJson = noteZapActionJsonFormatter.format(orgId, zapAction, zapActionRequestContext.getJsonObject());
                    final NoteHolder noteHolder = sourceNoteConverter.convert(new ConverterConfig(orgId, userId, null, false, noteJson), noteJson);
                    final Collection<String> errors = validate(noteHolder, Group.Create.class);

                    if (errors.isEmpty()) {
                        sourceNoteManager.create(noteHolder);
                        index(orgId, noteHolder, Action.CREATE);

                        response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.entity.note.create.success"));
                        response.send();
                    } else {
                        response.setErrorCode(ErrorCode.get(ZapierErrorCodes.DATA_VALIDATION_FAILED));
                        response.put(ZapAttributes.ERRORS, errors);
                        response.send();
                    }
                } catch (Exception exception) {
                    LOGGER.error("Zapier Action Execution: Failed to execute the note action. OrgId: {}, Reason: {}", orgId, exception.getMessage());

                    response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.entity.note.create.failed"));
                    response.setErrorCode(ErrorCode.get(ZapierErrorCodes.UPDATE_ACTION_FAILED));
                    response.send();
                }
            }
        }
    }
}

package com.tlc.crm.zapier.internal.action.dispatcher;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.core.features.api.tag.SourceTagManager;
import com.tlc.crm.core.model.SourceTags;

import com.tlc.crm.zapier.api.models.ZapAction;
import com.tlc.crm.zapier.api.models.ZapActionRequestContext;
import com.tlc.crm.zapier.fields.ZapAttributes;
import com.tlc.crm.zapier.internal.action.formatter.TagZapActionJsonFormatter;
import com.tlc.crm.zapier.internal.status.ZapierErrorCodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link TagZapActionDispatcher} for handling
 * Zapier actions related to tag entities.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class TagZapActionDispatcher implements ZapActionDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(TagZapActionDispatcher.class);

    private final TagZapActionJsonFormatter tagZapActionJsonFormatter;
    private final SourceTagManager sourceTagManager;

    private TagZapActionDispatcher() {
        this.tagZapActionJsonFormatter = TagZapActionJsonFormatter.getInstance();
        this.sourceTagManager = SourceTagManager.getInstance();
    }

    private static final class Instance {
        private static final TagZapActionDispatcher ZAP_ACTION_DISPATCHER = new TagZapActionDispatcher();
    }

    public static TagZapActionDispatcher getInstance() {
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
            case ADD_TAGS_TO_COMPANY, ADD_TAGS_TO_CONTACT, ADD_TAGS_TO_DEAL -> {
                try {
                    final JsonObject tagJson = tagZapActionJsonFormatter.format(orgId, zapAction, zapActionRequestContext.getJsonObject());

                    final JsonArray tagArray = tagJson.getJsonArray(ZapAttributes.TAGS);
                    final SourceTags sourceTags = new SourceTags(tagJson.getString(ZapAttributes.SOURCE));
                    final int tagArraySize = tagArray.size();

                    for (int index = 0; index < tagArraySize; index++) {
                        final JsonObject jsonObject = tagArray.getJsonObject(index);

                        sourceTags.addTagId(jsonObject.getLong(ZapAttributes.ID));
                    }
                    sourceTags.setUserId(userId);
                    sourceTags.addEntityId(tagJson.getLong(ZapAttributes.ENTITY_ID));
                    sourceTagManager.mapTagsWithSource(orgId, sourceTags);

                    response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.entity.tag.create.success"));
                    response.send();
                } catch (Exception exception) {
                    LOGGER.error("Zapier Action Execution: Failed to execute the tag action. OrgId: {}, Reason: {}", orgId, exception.getMessage());

                    response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.entity.tag.create.failed"));
                    response.setErrorCode(ErrorCode.get(ZapierErrorCodes.UPDATE_ACTION_FAILED));
                    response.send();
                }
            }
        }
    }
}

package com.tlc.crm.zapier.internal.action.dispatcher;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.core.features.api.followers.source.manager.SourceFollowersManager;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.zapier.api.models.ZapAction;
import com.tlc.crm.zapier.api.models.ZapActionRequestContext;
import com.tlc.crm.zapier.fields.ZapAttributes;
import com.tlc.crm.zapier.internal.status.ZapierErrorCodes;
import com.tlc.crm.zapier.internal.action.formatter.FollowerZapActionJsonDispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of {@link FollowerZapActionDispatcher} for handling
 * Zapier actions related to follower entities.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class FollowerZapActionDispatcher implements ZapActionDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(FollowerZapActionDispatcher.class);

    private final FollowerZapActionJsonDispatcher followerZapActionJsonDispatcher;
    private final SourceFollowersManager sourceFollowersManager;

    private FollowerZapActionDispatcher() {
        this.followerZapActionJsonDispatcher = FollowerZapActionJsonDispatcher.getInstance();
        this.sourceFollowersManager = SourceFollowersManager.getInstance();
    }

    private static final class Instance {
        private static final FollowerZapActionDispatcher ZAP_ACTION_DISPATCHER = new FollowerZapActionDispatcher();
    }

    public static FollowerZapActionDispatcher getInstance() {
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
            case ADD_FOLLOWER_TO_COMPANY, ADD_FOLLOWER_TO_CONTACT, ADD_FOLLOWER_TO_DEAL -> {
                try {
                    final JsonObject followerJson = followerZapActionJsonDispatcher.format(orgId, zapAction, zapActionRequestContext.getJsonObject());
                    final Source source = Source.get(followerJson.getString(ZapAttributes.SOURCE));
                    final JsonArray followerJsonArray = followerJson.getJsonArray(ZapAttributes.FOLLOWER_IDS);
                    final Collection<Long> followersIds = new HashSet<>();

                    followerJsonArray.forEach(followerId -> followersIds.add((Long) followerId));
                    sourceFollowersManager.assignFollowers(orgId, userId, source, Set.of(followerJson.getLong(ZapAttributes.ENTITY_ID)), followersIds);

                    response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.entity.follower.create.success"));
                    response.send();
                } catch (Exception exception) {
                    LOGGER.error("Zapier Action Execution: Failed to execute the follower action. OrgId: {}, Reason: {}", orgId, exception.getMessage());

                    response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.action.entity.follower.create.failed"));
                    response.setErrorCode(ErrorCode.get(ZapierErrorCodes.UPDATE_ACTION_FAILED));
                    response.send();
                }
            }
        }
    }
}

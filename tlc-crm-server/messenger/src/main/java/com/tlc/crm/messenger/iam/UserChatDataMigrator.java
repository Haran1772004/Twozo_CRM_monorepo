package com.tlc.crm.messenger.iam;

import com.tlc.crm.common.iam.UserDataMigrator;
import com.tlc.crm.common.iam.UserDataTransferAction;
import com.tlc.crm.messenger.api.chat.manager.ChatManager;

import java.util.Collection;

/**
 * Migrates the user chat data from the source users to the targeted user
 *
 * @author Maharaja S
 * @version 1.0
 */
@UserDataMigrator(name = "transferUserChatData")
public class UserChatDataMigrator implements UserDataTransferAction {

    private final ChatManager chatManager;

    public UserChatDataMigrator() {
        chatManager = ChatManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents the org id
     * @param targetUserId  Represents the target user id to update
     * @param sourceUserIds Represents the {@link Collection} of source user ids to be updated
     */
    @Override
    public void transferUserRecords(final Long orgId, final Long targetUserId, final Collection<Long> sourceUserIds) {
        chatManager.transferUserChat(orgId, sourceUserIds, targetUserId);
    }
}
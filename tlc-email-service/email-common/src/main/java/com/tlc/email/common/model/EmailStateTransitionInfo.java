package com.tlc.email.common.model;

import com.tlc.email.common.model.type.Email.MessageCategory;
import com.tlc.email.common.model.type.Email.State;

/**
 * <p>
 * An entity that holds the attributes for the transition of email state
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class EmailStateTransitionInfo {

    private String oldMessageId;
    private String newMessageId;
    private String parentFolderId;
    private State oldState;
    private State newState;
    private MessageCategory messageCategory;

    public String getOldMessageId() {
        return oldMessageId;
    }

    public void setOldMessageId(final String oldMessageId) {
        this.oldMessageId = oldMessageId;
    }

    public String getNewMessageId() {
        return newMessageId;
    }

    public void setNewMessageId(final String newMessageId) {
        this.newMessageId = newMessageId;
    }

    public String getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(final String parentFolderId) {
        this.parentFolderId = parentFolderId;
    }

    public State getOldState() {
        return oldState;
    }

    public void setOldState(final State oldState) {
        this.oldState = oldState;
    }

    public State getNewState() {
        return newState;
    }

    public void setNewState(final State newState) {
        this.newState = newState;
    }

    public MessageCategory getMessageCategory() {
        return messageCategory;
    }

    public void setMessageCategory(final MessageCategory messageCategory) {
        this.messageCategory = messageCategory;
    }
}
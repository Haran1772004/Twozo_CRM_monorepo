package com.tlc.crm.core.features.action.call.model;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.CrmModel;

import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;

import com.tlc.crm.core.conversation.api.model.ConversableModel;
import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.notification.model.call.CallType;

import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Represents a log entry for a phone call.
 *
 * @author Sathish
 * @version 1.0
 */
public class CallLog implements CrmModel, ConversableModel {

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.common.entity.invalid.id")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.call.invalid.participant")
    private Contact participant;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.call.invalid.type")
    private CallType callType;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.call.invalid.startTime")
    private DateTime startTime;

    private String agenda;
    private CrmUser createdBy;
    private Deal relatedDeal;
    private Outcome outcome;
    private DateTime loggedTime;
    private DateTime updatedTime;
    private Collection<Long> userIds;

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Contact getParticipant() {
        return participant;
    }

    public void setParticipant(final Contact participant) {
        this.participant = participant;
    }

    public CallType getCallType() {
        return callType;
    }

    public void setCallType(final CallType callType) {
        this.callType = callType;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(final DateTime startTime) {
        this.startTime = startTime;
    }

    public String getAgenda() {
        return agenda;
    }

    public void setAgenda(final String agenda) {
        this.agenda = agenda;
    }

    public CrmUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final CrmUser createdBy) {
        this.createdBy = createdBy;

        addUserId(createdBy.id());
    }

    public Deal getRelatedDeal() {
        return relatedDeal;
    }

    public void setRelatedDeal(final Deal relatedDeal) {
        this.relatedDeal = relatedDeal;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public void setOutcome(final Outcome outcome) {
        this.outcome = outcome;
    }

    public DateTime getLoggedTime() {
        return loggedTime;
    }

    public void setLoggedTime(final DateTime loggedTime) {
        this.loggedTime = loggedTime;
    }

    public DateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(final DateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public void addUserId(final Long userId) {
        userIds = Objects.isNull(userIds) ? new HashSet<>() : userIds;

        userIds.add(userId);
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of user ids
     */
    @Override
    public Collection<Long> userIds() {
        return userIds;
    }

    @Override
    public Object identity() {
        return null;
    }
}

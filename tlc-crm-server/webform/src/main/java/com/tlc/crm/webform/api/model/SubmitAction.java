package com.tlc.crm.webform.api.model;

import com.tlc.i18n.I18nKey;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Update;
import com.tlc.validator.type.Group.Delete;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.validator.constraints.URL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * A Model which represents the webForm field style details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class SubmitAction {

    @NotNull(groups = {Update.class, Delete.class}, message = "i18n.webForm.submitAction.invalid.id")
    private Long id;

    @Size(max = 100, groups = {Create.class, Update.class}, message = "i18n.webForm.submitAction.thankYou.message.max.size")
    private String thankYouMessage;

    private I18nKey i18nMessage;

    @URL(groups = {Create.class, Update.class}, message = "i18n.webForm.submitAction.invalid.redirect.url")
    private String redirectUrl;

    private boolean isThankYouMessageEnabled;

    private boolean isRedirectEnabled;

    private boolean isEmailNotificationEnabled;

    @Valid
    private Collection<RecipientEmail> recipientEmails;

    private boolean isContactCreationEnabled;

    private boolean isDealCreationWithContactEnabled;

    public String getThankYouMessage() {
        return thankYouMessage;
    }

    public void setThankYouMessage(final String thankYouMessage) {
        this.thankYouMessage = thankYouMessage;
    }

    public I18nKey getI18nMessage() {
        return i18nMessage;
    }

    public void setI18nMessage(final I18nKey i18nMessage) {
        this.i18nMessage = i18nMessage;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(final String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public boolean isEmailNotificationEnabled() {
        return isEmailNotificationEnabled;
    }

    public void setEmailNotificationEnabled(final boolean emailNotificationEnabled) {
        isEmailNotificationEnabled = emailNotificationEnabled;
    }

    public boolean isContactCreationEnabled() {
        return isContactCreationEnabled;
    }

    public void setContactCreationEnabled(final boolean contactCreationEnabled) {
        isContactCreationEnabled = contactCreationEnabled;
    }

    public boolean isDealCreationWithContactEnabled() {
        return isDealCreationWithContactEnabled;
    }

    public void setDealCreationWithContactEnabled(final boolean dealCreationWithContactEnabled) {
        isDealCreationWithContactEnabled = dealCreationWithContactEnabled;
    }

    public Collection<RecipientEmail> getRecipientEmails() {
        return recipientEmails;
    }

    public void setRecipientEmails(final Collection<RecipientEmail> recipientEmails) {
        this.recipientEmails = recipientEmails;
    }

    public void addRecipientEmail(final RecipientEmail recipientEmail) {
        this.recipientEmails = Objects.isNull(this.recipientEmails) ? new ArrayList<>() : this.recipientEmails;

        this.recipientEmails.add(recipientEmail);
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long id() {
        return id;
    }

    public boolean isThankYouMessageEnabled() {
        return isThankYouMessageEnabled;
    }

    public void setThankYouMessageEnabled(final boolean thankYouMessageEnabled) {
        isThankYouMessageEnabled = thankYouMessageEnabled;
    }

    public boolean isRedirectEnabled() {
        return isRedirectEnabled;
    }

    public void setRedirectEnabled(final boolean redirectEnabled) {
        isRedirectEnabled = redirectEnabled;
    }
}

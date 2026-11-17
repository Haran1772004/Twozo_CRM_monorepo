package com.tlc.crm.messenger.campaign.model;

import com.tlc.commons.language.Language;
import com.tlc.crm.common.models.ViewType;
import com.tlc.social.common.whatsapp.model.template.WATemplate;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * <p>
 * Represents the WhatsApp template filter preference of a user.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WATemplateFilterPreference implements TlcModel {

    private Long id;
    private Long orgId;
    private Long userId;
    private Long businessPhoneNumberId;
    private ViewType viewType;
    private Collection<WATemplate.Category> categories;
    private Collection<WACampaignTemplateStatus> statuses;
    private Collection<Language> languages;

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public Long getBusinessPhoneNumberId() {
        return businessPhoneNumberId;
    }

    public void setBusinessPhoneNumberId(final Long businessPhoneNumberId) {
        this.businessPhoneNumberId = businessPhoneNumberId;
    }

    public ViewType getViewType() {
        return viewType;
    }

    public void setViewType(final ViewType viewType) {
        this.viewType = viewType;
    }

    public Collection<WATemplate.Category> getCategories() {
        return categories;
    }

    public void setCategories(final Collection<WATemplate.Category> categories) {
        this.categories = categories;
    }

    public Collection<WACampaignTemplateStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(final Collection<WACampaignTemplateStatus> statuses) {
        this.statuses = statuses;
    }

    public Collection<Language> getLanguages() {
        return languages;
    }

    public void setLanguages(final Collection<Language> languages) {
        this.languages = languages;
    }

    public Collection<Short> getLanguageIds() {
        return Objects.nonNull(languages) ? languages.stream().map(Language::getId).toList() : Collections.emptyList();
    }

    public Collection<Short> getCategoryIds() {
        return Objects.nonNull(categories) ? categories.stream().map(WATemplate.Category::getId).toList() : Collections.emptyList();
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * <p>
     * Gets the default filter preference
     * </p>
     *
     * @return the {@link WATemplateFilterPreference}
     */
    public static WATemplateFilterPreference getDefaultPreference(final Long orgId, final Long userId) {
        final WATemplateFilterPreference waTemplateFilterPreference = new WATemplateFilterPreference();

        waTemplateFilterPreference.setOrgId(orgId);
        waTemplateFilterPreference.setUserId(userId);
        waTemplateFilterPreference.setViewType(ViewType.GRID);

        return waTemplateFilterPreference;
    }
}
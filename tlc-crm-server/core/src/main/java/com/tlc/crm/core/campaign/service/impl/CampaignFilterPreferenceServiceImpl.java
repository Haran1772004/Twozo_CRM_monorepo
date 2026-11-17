package com.tlc.crm.core.campaign.service.impl;

import com.tlc.crm.core.campaign.manager.CampaignFilterPreferenceManager;
import com.tlc.crm.core.campaign.model.CampaignFilterUserPreference;
import com.tlc.crm.core.campaign.model.CampaignCategory;
import com.tlc.crm.core.campaign.model.CampaignFolder;
import com.tlc.crm.core.campaign.resolver.CampaignFilterPreferenceResolver;
import com.tlc.crm.core.campaign.service.CampaignFilterPreferenceService;

import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * Provides the default implementation of {@link CampaignFilterPreferenceService}.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 * @see CampaignFilterPreferenceService
 */
public class CampaignFilterPreferenceServiceImpl implements CampaignFilterPreferenceService {

    private final CampaignFilterPreferenceManager preferenceManager;
    private final CampaignFilterPreferenceResolver preferenceResolver;

    private static class InstanceHolder {
        private static final CampaignFilterPreferenceServiceImpl INSTANCE = new CampaignFilterPreferenceServiceImpl();
    }

    private CampaignFilterPreferenceServiceImpl() {
        preferenceManager = CampaignFilterPreferenceManager.getInstance();
        preferenceResolver = CampaignFilterPreferenceResolver.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link CampaignFilterPreferenceServiceImpl}.
     * </p>
     *
     * @return {@link CampaignFilterPreferenceServiceImpl}.
     */
    public static CampaignFilterPreferenceServiceImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization id.
     * @param userId   Represents the user id.
     * @param category Represents the campaign category.
     * @return {@link CampaignFilterUserPreference}.
     */
    @Override
    public CampaignFilterUserPreference get(final Long orgId, final Long userId, final CampaignCategory category) {
        return preferenceResolver.get(orgId, userId, category);
    }

    /**
     * {@inheritDoc}
     *
     * @param filterUserPreference Represents the selected filter preference.
     * @return The existing, created, or updated {@link CampaignFilterUserPreference}.
     */
    @Override
    public void upsert(final CampaignFilterUserPreference filterUserPreference) {
        final CampaignCategory category = filterUserPreference.getCategory();
        final CampaignFilterUserPreference existingPreference = get(filterUserPreference.orgId(), filterUserPreference.getUserId(), category);

        if (Objects.nonNull(existingPreference)) {
            if (isChanged(existingPreference, filterUserPreference)) {
                filterUserPreference.setId(existingPreference.id());
                preferenceResolver.update(filterUserPreference);
            }
        } else {
            preferenceManager.create(filterUserPreference);
        }
    }

    /**
     * <p>
     * Checks whether the preference is changed or not
     * </p>
     *
     * @param existingPreference Represents the existing preference
     * @param preference         Represents the preference to be updated
     * @return true if changed
     */
    private boolean isChanged(final CampaignFilterUserPreference existingPreference, final CampaignFilterUserPreference preference) {
        if (!Objects.equals(existingPreference.getCampaignFilterGroup(), preference.getCampaignFilterGroup())) {
            return true;
        }
        if (!Objects.equals(existingPreference.getCampaignDefaultFilter(), preference.getCampaignDefaultFilter())) {
            return true;
        }
        if (!Objects.equals(getFolderId(existingPreference.getCampaignFolder()), getFolderId(preference.getCampaignFolder()))) {
            return true;
        }

        return !new HashSet<>(existingPreference.getSelectedStatusIds()).equals(new HashSet<>(preference.getSelectedStatusIds()));
    }

    /**
     * <p>
     * Gets the folder ids
     * </p>
     *
     * @param folder Represents the {@link CampaignFolder}
     * @return the folder id
     */
    private static Long getFolderId(final CampaignFolder folder) {
        return Objects.isNull(folder) ? null : folder.id();
    }
}
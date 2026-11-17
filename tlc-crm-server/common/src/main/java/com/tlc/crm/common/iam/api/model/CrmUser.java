package com.tlc.crm.common.iam.api.model;

import com.tlc.attachment.model.profilePicture.DisplayPicture;

import com.tlc.crm.common.iam.api.preferences.model.Preference;
import com.tlc.crm.common.iam.api.preferences.model.UserPreference;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;

import com.tlc.crm.security.iam.BaseCrmUser;

import com.tlc.iam.common.models.Organization;
import com.tlc.iam.common.models.User;

import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.notification.model.alert.EmailNotifiableModel;

import java.util.Collection;

/**
 * <p>
 * Represents the {@link CrmUser} details
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface CrmUser extends BaseCrmUser, EmailNotifiableModel {

    /**
     * <p>
     * Sets the user id in {@link CrmUser}
     * </p>
     *
     * @param userId Represents the user id
     */
    void setId(final Long userId);

    /**
     * <p>
     * Sets the Iam user in Crm user
     * </p>
     *
     * @param user Represents the {@link User} details
     */
    void setUser(final User user);

    /**
     * <p>
     * Gets the iam user id
     * </p>
     *
     * @return The iam user id
     */
    Long getIamId();

    /**
     * <p>
     * Gets the user first name
     * </p>
     *
     * @return The user first name
     */
    String getFirstName();

    /**
     * <p>
     * Gets the user last name
     * </p>
     *
     * @return The user last name
     */
    String getLastName();

    /**
     * <p>
     * Gets the user email
     * </p>
     *
     * @return The user email
     */
    String getEmail();

    /**
     * <p>
     * Gets the {@link CrmRole} details
     * </p>
     *
     * @return The {@link CrmRole} details
     */
    CrmRole getRole();

    /**
     * {@inheritDoc}
     *
     * @return mobile number
     */
    String getMobile();

    /**
     * <p>
     * Gets the full name
     * </p>
     *
     * @return The full name
     */
    String getFullName();

    /**
     * <p>
     * Gets the {@link CrmOrg} details
     * </p>
     *
     * @return The crm org details
     */
    CrmOrg getCrmOrg();

    /**
     * <p>
     * Sets the job title
     * </p>
     *
     * @param jobTitle Represents the job title
     */
    void setJobTitle(final String jobTitle);

    /**
     * <p>
     * Gets the job title
     * </p>
     *
     * @return the job title
     */
    String getJobTitle();

    /**
     * <p>
     * Set the pipeline id
     * </p>
     *
     * @param pipelineId Represents the pipeline id
     */
    void setPipelineId(final Long pipelineId);

    /**
     * <p>
     * Gets the pipeline id
     * </p>
     *
     * @return the pipeline id
     */
    Long getPipelineId();

    /**
     * <p>
     * Gets the reporting manager
     * </p>
     *
     * @return the reporting manager
     */
    CrmUser getReportingManager();

    /**
     * <p>
     * Gets the user last login time
     * </p>
     *
     * @return the last login time
     */
    DateTime getLastLoginTime();

    /**
     * <p>
     * Gets the user state
     * </p>
     *
     * @return the user state
     */
    User.State getState();

    /**
     * <p>
     * Sets the {@link CrmRole} details
     * </p>
     *
     * @param crmRole An entity that holds the {@link CrmRole} details
     */
    void setRole(final CrmRole crmRole);

    /**
     * <p>
     * Sets the iam id
     * </p>
     *
     * @param iamId Represents the iam id
     */
    void setIamId(final Long iamId);

    /**
     * <p>
     * Sets the teams
     * </p>
     *
     * @param teams The collection of {@link Team}
     */
    void setTeams(final Collection<Team> teams);

    /**
     * <p>
     * Adds a team to the collection of teams
     * </p>
     *
     * @param team Represents the {@link Team} details
     */
    void addTeam(final Team team);

    /**
     * <p>
     * Gets the teams
     * </p>
     *
     * @return A collection of teams
     */
    Collection<Team> getTeams();

    /**
     * <p>
     * Sets the territories
     * </p>
     *
     * @param territories Contains the collection of territories
     */
    void setTerritories(final Collection<Territory> territories);

    /**
     * <p>
     * Gets the territories
     * </p>
     *
     * @return The collection of {@link Territory}
     */
    Collection<Territory> getTerritories();

    /**
     * <p>
     * Sets the reporting manager
     * </p>
     *
     * @param crmUser Represents the {@link CrmUser}
     */
    void setReportingManager(final CrmUser crmUser);

    /**
     * <p>
     * Sets the user last login time
     * </p>
     *
     * @param lastLoginTime Represents the last login time
     */
    void setLastLoginTime(final DateTime lastLoginTime);

    /**
     * <p>
     * Sets the user state
     * </p>
     *
     * @param state Represents the user state
     */
    void setState(final User.State state);

    /**
     * <p>
     * Fetches the thumbnail for user
     * </p>
     *
     * @return the {@link DisplayPicture}
     */
    DisplayPicture getThumbnail();

    /**
     * Fetches the original picture of user.
     *
     * @return The {@link DisplayPicture}.
     */
    DisplayPicture getOriginalPicture();

    /**
     * <p>
     * Sets the preferenceHolder related to the user.
     * </p>
     *
     * @param preferenceHolder the {@link Preference} of the user.
     */
    void setPreferenceHolder(final UserPreferenceHolder preferenceHolder);

    /**
     * <p>
     * Gets the preferenceHolder related to the user
     * </p>
     *
     * @return the {@link UserPreferenceHolder} of the user
     */
    UserPreferenceHolder getPreferenceHolder();

    /**
     * <p>
     * Adds the user preference
     * </p>
     *
     * @param userPreference Represents the {@link Preference}
     */
    void addPreference(final UserPreference userPreference);

    /**
     * <p>
     * Gets the {@link Organization}
     * </p>
     *
     * @return The {@link Organization}
     */
    Organization getOrganization();

    /**
     * <p>
     * Gets the reporting manager
     * </p>
     *
     * @return {@link CrmUser}
     */
    CrmUser reportingManager();

    /**
     * <p>
     * Sets the organization is for this {@link CrmUser}.
     * </p>
     *
     * @param orgId Represents an org id
     */
    void setOrgId(final Long orgId);
}

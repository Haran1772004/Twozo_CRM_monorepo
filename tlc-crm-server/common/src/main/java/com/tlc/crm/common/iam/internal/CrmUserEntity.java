package com.tlc.crm.common.iam.internal;

import com.tlc.attachment.model.profilePicture.DisplayPicture;

import com.tlc.crm.common.email.alert.annotation.EmailAlertField;
import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.api.preferences.model.UserPreference;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.RoleService;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.common.service.UserPreferenceHolderService;
import com.tlc.iam.common.models.Organization;
import com.tlc.iam.common.models.User;
import com.tlc.l10n.temporal.model.DateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Represents a {@link CrmUser} with specific functionality
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class CrmUserEntity implements CrmUser {

    private transient UserPreferenceHolder userPreferenceHolder;

    @EmailAlertField(
            name = "userId",
            isUserField = true,
            fieldType = EmailAlertFieldType.NUMBER,
            recipientTemplates = {EmailNotificationTemplate.USER_INVITATION, EmailNotificationTemplate.FORGOT_PASSWORD, EmailNotificationTemplate.USER_DEACTIVATION}
    )
    private Long id;
    private Long iamId;
    private Long orgId;
    private transient CrmRole role;
    private String firstName;
    private String lastName;
    private String email;
    private String mobile;
    private transient CrmOrg crmOrg;
    private String fullName;
    private Long pipelineId;
    private String jobTitle;
    private transient Collection<Team> teams;
    private transient Collection<Territory> territories;
    private transient Collection<Long> territoryIds;
    private transient Collection<Long> teamIds;
    private transient CrmUser reportingManager;
    private DateTime lastLoginTime;
    private transient User.State state;
    private transient DisplayPicture thumbnail;
    private transient DisplayPicture originalPicture;
    private transient Organization organization;

    public CrmUserEntity() {
    }

    /**
     * {@inheritDoc}
     *
     * @param user Represents the {@link User} details
     */
    @Override
    public void setUser(final User user) {
        if (Objects.isNull(orgId)) {
            orgId = OrganizationService.getInstance().getOrgId(user.orgId());
        }

        if (Objects.isNull(id) && Objects.nonNull(user.id())) {
            id = UserService.getInstance().getUserId(orgId, user.id());
        }
        firstName = user.getFirstName();
        lastName = user.getLastName();
        email = user.getEmail();
        mobile = user.getMobile();
        fullName = user.getFullName();
        jobTitle = user.getJobTitle();
        lastLoginTime = user.getLastLoginTime();
        final User.State userState = user.getUserState();

        state = Objects.isNull(userState) ? null : userState;
        thumbnail = user.getThumbnail();
        originalPicture = user.getOriginalPicture();
        final Organization organization = user.getOrganization();

        if (Objects.nonNull(organization)) {
            this.organization = organization;
        }
    }

    /**
     * <p>
     * Sets the org id into the {@link CrmUserEntity}
     * </p>
     *
     * @param orgId Represents the org id
     */
    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the user id
     */
    @Override
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * <p>
     * Sets the iam user id into the {@link CrmUserEntity}
     * </p>
     *
     * @param iamId Represents the iam user id
     */
    public void setIamId(final Long iamId) {
        this.iamId = iamId;
    }

    /**
     * <p>
     * Sets the {@link CrmRole} details into {@link CrmUserEntity}
     * </p>
     *
     * @param role Represents the {@link CrmRole} details
     */
    public void setRole(final CrmRole role) {
        this.role = role;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link CrmRole} details
     */
    @Override
    public CrmRole getRole() {

        if (Objects.isNull(role)) {
            this.role = RoleService.getInstance().getRoleByUser(orgId, id);
        }
        return role;
    }

    /**
     * {@inheritDoc}
     *
     * @return The iam id
     */
    @Override
    public Long getIamId() {
        return iamId;
    }

    /**
     * {@inheritDoc}
     *
     * @return The first name
     */
    @Override
    public String getFirstName() {
        return firstName;
    }

    /**
     * {@inheritDoc}
     *
     * @return The last name
     */
    @Override
    public String getLastName() {
        return lastName;
    }

    /**
     * {@inheritDoc}
     *
     * @return The email
     */
    @Override
    public String getEmail() {
        return email;
    }

    /**
     * {@inheritDoc}
     *
     * @return The user id
     */
    @Override
    public Long id() {
        return id;
    }

    /**
     * {@inheritDoc}
     *
     * @return The organization id
     */
    @Override
    public Long orgId() {
        return orgId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object identity() {
        return null;
    }

    /**
     * <p>
     * Sets the {@link CrmOrg}
     * </p>
     *
     * @param crmOrg Represents the {@link CrmOrg}
     */
    public void setCrmOrg(final CrmOrg crmOrg) {
        this.crmOrg = crmOrg;
    }

    /**
     * <>
     * Gets the {@link CrmOrg}
     * </>
     *
     * @return The {@link CrmOrg} details
     */
    public CrmOrg getCrmOrg() {
        return crmOrg;
    }

    /**
     * {@inheritDoc}
     *
     * @return mobile number
     */
    public String getMobile() {
        return mobile;
    }

    /**
     * {@inheritDoc}
     *
     * @return The full name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * {@inheritDoc}
     *
     * @param jobTitle Represents the job title
     */
    @Override
    public void setJobTitle(final String jobTitle) {
        this.jobTitle = jobTitle;
    }

    /**
     * {@inheritDoc}
     *
     * @return the job title
     */
    @Override
    public String getJobTitle() {
        return jobTitle;
    }

    /**
     * {@inheritDoc}
     *
     * @param pipelineId Represents the pipeline id
     */
    @Override
    public void setPipelineId(final Long pipelineId) {
        this.pipelineId = pipelineId;
    }

    /**
     * {@inheritDoc}
     *
     * @return the pipeline id
     */
    @Override
    public Long getPipelineId() {
        return pipelineId;
    }

    /**
     * {@inheritDoc}
     *
     * @param team Represents the team
     */
    @Override
    public void addTeam(final Team team) {
        this.teams = Objects.isNull(this.teams) ? new ArrayList<>() : this.teams;

        this.teams.add(team);
    }

    /**
     * {@inheritDoc}
     *
     * @param teams Contains the collection of {@link Team}
     */
    @Override
    public void setTeams(final Collection<Team> teams) {
        this.teams = teams;
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of {@link Team}
     */
    @Override
    public Collection<Team> getTeams() {
        return teams;
    }

    /**
     * {@inheritDoc}
     *
     * @param territories Contains the collection of territories
     */
    @Override
    public void setTerritories(final Collection<Territory> territories) {
        this.territories = territories;
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of {@link Territory}
     */
    @Override
    public Collection<Territory> getTerritories() {
        return territories;
    }

    /**
     * {@inheritDoc}
     *
     * @return the reporting manager
     */
    @Override
    public CrmUser getReportingManager() {
        if (Objects.isNull(reportingManager)) {
            final Long reportingManagerId = CrmUserManager.getSecureInstance().getReportingManagerId(orgId, id);

            return Objects.isNull(reportingManagerId) ? null : UserService.getInstance().getUser(orgId, reportingManagerId);
        }
        return reportingManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param reportingManager Represents the {@link CrmUser}
     */
    @Override
    public void setReportingManager(final CrmUser reportingManager) {
        this.reportingManager = reportingManager;
    }

    /**
     * {@inheritDoc}
     *
     * @return the user last login time
     */
    @Override
    public DateTime getLastLoginTime() {
        return lastLoginTime;
    }

    /**
     * {@inheritDoc}
     *
     * @return the user state
     */
    @Override
    public User.State getState() {
        return state;
    }

    /**
     * {@inheritDoc}
     *
     * @param lastLoginTime Represents the user last login time
     */
    @Override
    public void setLastLoginTime(final DateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    /**
     * {@inheritDoc}
     *
     * @param state Represents the user state
     */
    @Override
    public void setState(final User.State state) {
        this.state = state;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link DisplayPicture}
     */
    @Override
    public DisplayPicture getThumbnail() {
        return thumbnail;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link DisplayPicture}.
     */
    @Override
    public DisplayPicture getOriginalPicture() {
        return originalPicture;
    }

    /**
     * <p>
     * Sets the preferences related to the user.
     * </p>
     *
     * @param preferenceHolder Represents the {@link UserPreference}.
     */
    @Override
    public void setPreferenceHolder(final UserPreferenceHolder preferenceHolder) {
        if (Objects.isNull(userPreferenceHolder)) {
            userPreferenceHolder = preferenceHolder;
        } else {
            userPreferenceHolder.addAll(preferenceHolder.getAll());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link UserPreferenceHolder}
     */
    @Override
    public UserPreferenceHolder getPreferenceHolder() {
        this.userPreferenceHolder = Objects.isNull(userPreferenceHolder) ? UserPreferenceHolderService.get().get(orgId, id) : userPreferenceHolder;

        return userPreferenceHolder;
    }

    /**
     * {@inheritDoc}
     *
     * @param userPreference Represents the {@link UserPreference}
     */
    @Override
    public void addPreference(final UserPreference userPreference) {
        if (Objects.isNull(userPreferenceHolder)) {
            userPreferenceHolder = new UserPreferenceHolder();
        }
        userPreferenceHolder.setOrgId(userPreference.orgId());
        userPreferenceHolder.setUserId(userPreference.userId());
        userPreferenceHolder.add(userPreference);
    }

    /**
     * {@inheritDoc}
     *
     * @return the role ID
     */
    @Override
    public Long getRoleId() {
        return getRole().id();
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Collection} of team IDs
     */
    @Override
    public Collection<Long> getTeamIds() {

        if (Objects.isNull(teamIds)) {
            this.teamIds = TeamManager.getInstance().getUserTeamIds(orgId, id);
        }
        return teamIds;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link Collection} of territory IDs
     */
    @Override
    public Collection<Long> getTerritoryIds() {

        if (Objects.isNull(territoryIds)) {
            this.territoryIds = TerritoryManager.getInstance().getUserTerritoryIds(orgId, id);
        }
        return territoryIds;
    }

    @Override
    public Organization getOrganization() {
        return organization;
    }

    public Long getId() {
        return this.id();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link CrmUser}
     */
    public CrmUser reportingManager() {
        return reportingManager;
    }
}

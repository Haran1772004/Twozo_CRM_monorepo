package com.tlc.crm.common.iam.api.model.converter;

import com.tlc.attachment.model.profilePicture.DisplayPicture;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.RandomIdGenerator;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.converter.OptionJsonConverter;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.preferences.manager.OrgPreferenceManager;
import com.tlc.crm.common.iam.api.preferences.model.converter.UserPreferenceConverter;
import com.tlc.crm.common.iam.api.preferences.model.holder.OrgPreferenceHolder;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.common.iam.api.preferences.model.impl.OrgRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserNotificationPreference;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.field.IamFields;
import com.tlc.crm.common.iam.internal.CrmUserEntity;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.models.NotificationState;
import com.tlc.crm.security.iam.DefaultRole;
import com.tlc.iam.common.fields.OrganizationFields;
import com.tlc.iam.common.fields.ProfilePictureFields;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.UserConverter;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.web.HttpUtil;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Provides various methods to convert the data from / into {@link CrmUser}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class CrmUserConverter implements OptionJsonConverter<CrmUser> {

    private final OrganizationService organizationService;
    private final UserConverter userConverter;
    private final CrmUserManager crmUserManager;
    private final UserService userService;
    private final UserPreferenceConverter preferenceConverter;
    private final OrgPreferenceManager orgPreferenceManager;
    private final DateTimeUtil dateTimeUtil;
    private final RandomIdGenerator randomIdGenerator;
    private final String profilePicRedirectionUrl;

    private static class Instance {
        private static final CrmUserConverter INSTANCE = new CrmUserConverter();
    }

    private CrmUserConverter() {
        organizationService = OrganizationService.getInstance();
        userConverter = UserConverter.getInstance();
        crmUserManager = CrmUserManager.getInstance();
        userService = UserService.getInstance();
        preferenceConverter = UserPreferenceConverter.getInstance();
        orgPreferenceManager = OrgPreferenceManager.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
        randomIdGenerator = RandomIdGenerator.getInstance();
        final Configurations configurations = ConfHolder.getInstance().getProperties("profile-picture").getNestedConfigs("redirectUrl");
        this.profilePicRedirectionUrl = configurations.getConfig("user");
    }

    /**
     * <p>
     * Gets the {@link CrmUserConverter} Instance
     * </p>
     *
     * @return The instance of {@link CrmUserConverter}
     */
    public static CrmUserConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts json attributes into {@link CrmUser}
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param jsonObject Holds the {@link CrmUser}attributes
     * @return An {@link CrmUser}
     */
    public CrmUser convert(final Long orgId, final JsonObject jsonObject) {
        final CrmUser crmUser = new CrmUserEntity();
        final String idKey = CrmUserFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            crmUser.setId(jsonObject.getLong(idKey));
        }
        setJsonDataToModel(orgId, false, crmUser, jsonObject);

        return crmUser;
    }

    /**
     * <p>
     * Converts partial data into {@link CrmUser}
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param jsonObject Holds the {@link CrmUser} attributes
     * @return The {@link CrmUser}
     */
    public CrmUser convertPartialData(final Long orgId, final JsonObject jsonObject) {
        final CrmUser crmUser = crmUserManager.getCrmUser(orgId, jsonObject.getLong(CrmUserFields.ID.getName()));

        setJsonDataToModel(orgId, true, crmUser, jsonObject);

        return crmUser;
    }

    /**
     * <p>
     * Sets the Json data to the {@link CrmUser}
     * </p>
     *
     * @param orgId         Represents the organization id
     * @param isPartialData Represents whether the data id partial or not.
     * @param crmUser       An entity that holds org email attributes
     * @param jsonObject    Holds the org email attributes to be set into the entity
     */
    private void setJsonDataToModel(final Long orgId, final boolean isPartialData, final CrmUser crmUser, final JsonObject jsonObject) {
        final User user = new User();
        final CrmRole role = new CrmRole();
        final String reportingManagerIdKey = CrmUserFields.REPORTING_MANAGER_ID.getName();
        final String teamIdsKey = CrmUserFields.TEAM_IDS.getName();
        final String roleIdKey = CrmUserFields.ROLE_ID.getName();
        final String pipelineIdKey = CrmUserFields.PIPELINE_ID.getName();
        final String preferencesKey = CrmUserFields.PREFERENCES.getName();
        final Long userId = crmUser.id();

        userConverter.mapJsonDataToUser(user, organizationService.getIamOrgId(orgId), jsonObject);
        crmUser.setUser(user);
        role.setId(jsonObject.containsKey(roleIdKey) ? jsonObject.getLong(roleIdKey) : crmUser.getRole().id());
        crmUser.setRole(role);
        crmUser.setPipelineId(jsonObject.containsKey(pipelineIdKey) ? jsonObject.getLong(pipelineIdKey) : null);

        if (jsonObject.containsKey(reportingManagerIdKey)) {
            if (jsonObject.isKeyPresentWithNullValue(reportingManagerIdKey)) {
                crmUser.setReportingManager(null);
            } else {
                final CrmUser reportingManager = new CrmUserEntity();

                reportingManager.setId(jsonObject.getLong(reportingManagerIdKey));
                crmUser.setReportingManager(reportingManager);
            }
        }

        if (jsonObject.containsKey(teamIdsKey)) {
            if (jsonObject.isKeyPresentWithNullValue(teamIdsKey)) {
                crmUser.setTeams(null);
            } else {
                final JsonArray jsonArray = jsonObject.getJsonArray(teamIdsKey);
                final Collection<Team> teams = new ArrayList<>();

                jsonArray.forEach(teamId -> {
                    final Team team = new Team();

                    team.setId((Long) teamId);
                    teams.add(team);
                });
                crmUser.setTeams(teams);
            }
        }

        if (jsonObject.containsKey(preferencesKey)) {
            crmUser.setPreferenceHolder(preferenceConverter.convert(crmUser.orgId(), userId, jsonObject.getJsonObject(preferencesKey)));
        } else if (!isPartialData) {
            final OrgPreferenceHolder orgPreferenceHolder = orgPreferenceManager.get(orgId);
            final OrgRegionalPreference orgRegionalPreference = orgPreferenceHolder.get(OrgRegionalPreference.TYPE);
            final UserRegionalPreference userRegionalPreference = new UserRegionalPreference();
            final UserPreferenceHolder userPreferenceHolder = new UserPreferenceHolder();

            userRegionalPreference.setUserId(userId);
            userRegionalPreference.setLanguage(orgRegionalPreference.getLanguage());
            userRegionalPreference.setTimezoneInfo(orgRegionalPreference.getTimezoneInfo());
            userRegionalPreference.setDateFormat(orgRegionalPreference.getDateFormat());
            userRegionalPreference.setTimeFormat(orgRegionalPreference.getTimeFormat());

            userPreferenceHolder.add(userRegionalPreference);
            userPreferenceHolder.add(new UserNotificationPreference(NotificationState.ON));
            userPreferenceHolder.setOrgId(orgId);
            userPreferenceHolder.setUserId(userId);
            crmUser.setPreferenceHolder(userPreferenceHolder);
        }
    }

    /**
     * <p>
     * Converts the {@link CrmUser} data into {@link JsonObject}
     * </p>
     *
     * @param crmUser   Represents the {@link CrmUser} details
     * @param isPartial A flag that indicates whether the data is partial or not
     * @return Converted the crm user {@link JsonObject}
     */
    public JsonObject convert(final CrmUser crmUser, final boolean isPartial, final boolean showThumbnail) {
        final JsonObject jsonObject = Json.object();
        final Long id = crmUser.id();

        jsonObject.put(CrmUserFields.ID.getName(), id);
        jsonObject.put(CrmUserFields.NAME.getName(), crmUser.getFullName());

        if (showThumbnail) {
            final DisplayPicture thumbnail = crmUser.getThumbnail();

            if (Objects.nonNull(thumbnail)) {
                jsonObject.put(CrmUserFields.THUMBNAIL.getName(), fetchProfileJson(thumbnail, crmUser.orgId(), id));
            }
        }

        if (!isPartial) {
            final CrmRole role = crmUser.getRole();
            final User.State state = crmUser.getState();
            final CrmUser reportingManager = crmUser.getReportingManager();

            if (Objects.nonNull(role)) {
                jsonObject.put(CrmUserFields.ROLE.getName(), RoleConverter.getInstance().convert(role, true));

                if (role.getConfig().isAdmin()) {
                    jsonObject.put(CrmUserFields.ROLE_TYPE.getName(), DefaultRole.ACCOUNT_ADMIN);
                }
            }
            jsonObject.put(CrmUserFields.EMAIL.getName(), crmUser.getEmail());
            jsonObject.put(CrmUserFields.STATE.getName(), Objects.isNull(state) ? null : state.getName());
            jsonObject.put(CrmUserFields.LAST_LOGIN_TIME.getName(), dateTimeUtil.getTimeStamp(crmUser.getLastLoginTime()));

            if (Objects.nonNull(reportingManager)) {
                jsonObject.put(CrmUserFields.REPORTING_MANAGER.getName(), convert(reportingManager, true, true));
            }
        }

        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link CrmUser} details into {@link JsonObject} details
     * </p>
     *
     * @param crmUser Represents the {@link CrmUser} details
     * @return The {@link JsonObject}
     */
    public JsonObject convertUserToJson(final CrmUser crmUser) {
        final JsonObject jsonObject = Json.object();
        final CrmRole role = crmUser.getRole();

        if (Objects.nonNull(role)) {
            jsonObject.put(CrmUserFields.ROLE.getName(), RoleConverter.getInstance().convert(role, true));
        }

        jsonObject.put(CrmUserFields.LAST_LOGIN_TIME.getName(), dateTimeUtil.getTimeStamp(crmUser.getLastLoginTime()));
        jsonObject.put(CrmUserFields.ID.getName(), crmUser.id());
        jsonObject.put(CrmUserFields.NAME.getName(), crmUser.getFullName());
        jsonObject.put(CrmUserFields.EMAIL.getName(), crmUser.getEmail());
        jsonObject.put(CrmUserFields.MOBILE.getName(), crmUser.getMobile());

        return jsonObject;
    }

    /**
     * <p>
     * Converts the collection of {@link CrmUser} into {@link JsonArray}.
     * </p>
     *
     * @param currentUserId Represents the current user id.
     * @param users         Holds the details of the {@link CrmUser}.
     * @return Converted {@link JsonArray}
     */
    public JsonArray convertUsersIntoJson(final long currentUserId, final Collection<CrmUser> users) {
        final JsonArray jsonArray = Json.array();
        final User.State activeState = User.State.ACTIVE;

        for (final CrmUser crmUser : users) {
            jsonArray.put(Json.object()
                    .put(IamFields.VALUE, crmUser.id())
                    .put(IamFields.NAME, crmUser.getFullName())
                    .put(IamFields.TYPE, IamFields.USER)
                    .put(IamFields.CONFIG, Json.object()
                            .put(IamFields.IS_ACTIVE, Objects.equals(activeState, crmUser.getState()))
                            .put(IamFields.IS_LOGGED_IN_USER, Objects.equals(currentUserId, crmUser.id()))));
        }

        return jsonArray;
    }

    /**
     * <p>
     * Converts the {@link CrmUser} data into {@link JsonObject}
     * </p>
     *
     * @param crmUser Represents the {@link CrmUser} details
     * @return Converted the crm user {@link JsonObject}
     */
    public JsonObject convert(final CrmUser crmUser) {
        final JsonObject jsonObject = Json.object();
        final DisplayPicture thumbnail = crmUser.getThumbnail();
        final Long id = crmUser.id();

        jsonObject.put(CrmUserFields.ID.getName(), id);
        jsonObject.put(CrmUserFields.NAME.getName(), crmUser.getFullName());
        jsonObject.put(CrmUserFields.EMAIL.getName(), crmUser.getEmail());
        jsonObject.put(CrmUserFields.PHONE.getName(), crmUser.getMobile());

        if (Objects.nonNull(thumbnail)) {
            jsonObject.put(CrmUserFields.THUMBNAIL.getName(), fetchProfileJson(thumbnail, crmUser.orgId(), id));
        }

        return jsonObject;
    }

    /**
     * <p>
     * Fetches the profile json for user
     * </p>
     *
     * @param thumbnail Represents the {@link DisplayPicture}
     * @param userId    Represents the user id
     * @param orgId     Represents an organization id
     * @return the {@link JsonObject}
     */
    public JsonObject fetchProfileJson(final DisplayPicture thumbnail, final long orgId, final long userId) {
        final JsonObject profileJson = Json.object();
        final JsonObject userJson = Json.object();
        final String extension = thumbnail.extension().getName();
        final String imageType = thumbnail.type().getName();

        userJson.put(ProfilePictureFields.USER_ID, userService.getUser(orgId, userId).getIamId());
        userJson.put(ProfilePictureFields.ID, randomIdGenerator.generateUniqueId());
        profileJson.put(ProfilePictureFields.NAME, imageType);
        profileJson.put(ProfilePictureFields.EXTENSION, extension);
        profileJson.put(ProfilePictureFields.SIZE, thumbnail.size());
        profileJson.put(ProfilePictureFields.URL, HttpUtil.buildAbsoluteUrl(String.format("%s%s%s%s%s%s", profilePicRedirectionUrl,
                Base64.getEncoder().encodeToString(userJson.getBytes()), "/", imageType, ".", extension)));

        return profileJson;
    }

    /**
     * <p>
     * Loads the {@link Json} data into the {@link CrmUser}
     * </p>
     *
     * @param jsonObject Holds the {@link CrmUser} details
     * @param crmUsers   Represents the {@link CrmUser} details
     */
    public void loadJsonDataToModel(final JsonObject jsonObject, final Collection<CrmUser> crmUsers) {
        final JsonArray jsonArray = jsonObject.getJsonArray(CrmUserFields.USERS.getName());

        jsonArray.forEach(userId -> {
            final CrmUser crmUser = new CrmUserEntity();

            crmUser.setId((Long) userId);
            crmUsers.add(crmUser);
        });
    }

    /**
     * <p>
     * Converts the {@link User} details into {@link JsonObject}
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param user   Represents the {@link User} details
     * @return The {@link JsonObject} details
     */
    public JsonObject convert(final Long orgId, final Long userId, final User user) {
        final JsonObject jsonObject = userConverter.convert(user);
        final DisplayPicture thumbnail = user.getThumbnail();

        if (Objects.nonNull(thumbnail)) {
            final JsonObject profileJson = Json.object();
            final JsonObject userJson = Json.object();
            final String extension = thumbnail.extension().getName();
            final String imageType = thumbnail.type().getName();

            userJson.put(ProfilePictureFields.USER_ID, user.id());
            userJson.put(ProfilePictureFields.ID, randomIdGenerator.generateUniqueId());
            profileJson.put(ProfilePictureFields.NAME, imageType);
            profileJson.put(ProfilePictureFields.ID, thumbnail.getFileId());
            profileJson.put(ProfilePictureFields.EXTENSION, extension);
            profileJson.put(ProfilePictureFields.SIZE, thumbnail.size());
            profileJson.put(ProfilePictureFields.URL, HttpUtil.buildAbsoluteUrl(String.format("%s%s%s%s%s%s", profilePicRedirectionUrl,
                    Base64.getEncoder().encodeToString(userJson.getBytes()), "/", imageType, ".", extension)));
            jsonObject.put(imageType, profileJson);
        }
        final JsonObject orgJson = jsonObject.getJsonObject(CrmUserFields.ORGANIZATION.getName());

        jsonObject.put(CrmUserFields.ID.getName(), userId);
        jsonObject.put(CrmUserFields.ORG_ID.getName(), orgId);
        orgJson.put(OrganizationFields.ID, orgId);

        return jsonObject;
    }

    /**
     * Converts {@link CrmUser} into {@link JsonObject}
     *
     * @param crmUser The crm user entity with the attributes of crm user details.
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertModelToOptionJson(final CrmUser crmUser) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(CrmUserFields.VALUE.getName(), crmUser.id());
        jsonObject.put(CrmUserFields.NAME.getName(), crmUser.getFullName());

        return jsonObject;
    }
}

package com.tlc.crm.core.api.iam.model.converter;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.RandomIdGenerator;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.converter.RoleConverter;
import com.tlc.crm.common.iam.api.model.converter.TeamConverter;
import com.tlc.crm.common.iam.api.model.converter.TerritoryConverter;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.api.preferences.model.converter.UserPreferenceConverter;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.converter.PipelineConverter;
import com.tlc.crm.core.api.iam.model.UserDealPreference;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.displayPicture.DisplayPictureFields;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.UserConverter;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.web.HttpUtil;

import java.util.Base64;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Provides various methods to convert the data from / into {@link UserDealPreference}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class UserDealPreferenceConverter {

    private final TeamConverter teamConverter;
    private final TerritoryConverter territoryConverter;
    private final PipelineConverter pipelineConverter;
    private final CrmUserConverter crmUserConverter;
    private final RoleConverter roleConverter;
    private final UserService userService;
    private final UserConverter userConverter;
    private final UserPreferenceConverter preferenceConverter;
    private final DateTimeUtil dateTimeUtil;
    private final RandomIdGenerator randomIdGenerator;
    private final String profilePicRedirectionUrl;

    public UserDealPreferenceConverter() {
        teamConverter = TeamConverter.getInstance();
        territoryConverter = TerritoryConverter.getInstance();
        pipelineConverter = PipelineConverter.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
        roleConverter = RoleConverter.getInstance();
        userService = UserService.getInstance();
        userConverter = UserConverter.getInstance();
        preferenceConverter = UserPreferenceConverter.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
        randomIdGenerator = RandomIdGenerator.getInstance();
        final Configurations configurations = ConfHolder.getInstance().getProperties("profile-picture").getNestedConfigs("redirectUrl");
        this.profilePicRedirectionUrl = configurations.getConfig("user");
    }

    private static class Instance {
        private static final UserDealPreferenceConverter INSTANCE = new UserDealPreferenceConverter();
    }

    public static UserDealPreferenceConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts the {@link UserDealPreference} into {@link JsonObject}
     * </p>
     *
     * @param crmUser Represents the {@link CrmUser} details
     * @return The {@link JsonObject} details
     */
    public JsonObject convertSummary(final CrmUser crmUser) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(CrmUserFields.ID.getName(), crmUser.id());
        jsonObject.put(CrmUserFields.PROFILE.getName(), convertProfileToJson(crmUser));
        jsonObject.put(CrmUserFields.SUMMARY.getName(), convertSummaryToJson(crmUser));

        return jsonObject;
    }

    /**
     * <p>
     * Converts the user profile details to json
     * </p>
     *
     * @param crmUser Represents the {@link CrmUser} details
     * @return The {@link JsonArray} details
     */
    private JsonObject convertProfileToJson(final CrmUser crmUser) {
        final JsonObject jsonObject = Json.object();
        final JsonObject profilePicJson = Json.object();
        final DisplayPicture thumbnail = crmUser.getThumbnail();
        final DisplayPicture original = crmUser.getOriginalPicture();
        final Long iamId = userService.getUser(crmUser.orgId(), crmUser.id()).getIamId();

        jsonObject.put(CrmUserFields.NAME.getName(), crmUser.getFullName());
        jsonObject.put(CrmUserFields.JOB_TITLE.getName(), crmUser.getJobTitle());

        if (Objects.nonNull(thumbnail)) {
            final JsonObject thumbnailJson = Json.object();
            final JsonObject userJson = Json.object();
            final String extension = thumbnail.extension().getName();
            final String imageType = thumbnail.type().getName();

            userJson.put(DisplayPictureFields.USER_ID, iamId);
            userJson.put(DisplayPictureFields.ID, randomIdGenerator.generateUniqueId());
            thumbnailJson.put(DisplayPictureFields.NAME, imageType);
            thumbnailJson.put(DisplayPictureFields.SIZE, thumbnail.size());
            thumbnailJson.put(DisplayPictureFields.EXTENSION, extension);
            thumbnailJson.put(DisplayPictureFields.URL, HttpUtil.buildAbsoluteUrl(String.format("%s%s%s%s%s%s", profilePicRedirectionUrl,
                    Base64.getEncoder().encodeToString(userJson.getBytes()), "/", imageType, ".", extension)));
            profilePicJson.put(imageType, thumbnailJson);
        }

        if (Objects.nonNull(original)) {
            final JsonObject originalPicJson = Json.object();
            final JsonObject userJson = Json.object();
            final String extension = original.extension().getName();
            final String imageType = original.type().getName();

            userJson.put(DisplayPictureFields.USER_ID, iamId);
            originalPicJson.put(DisplayPictureFields.NAME, imageType);
            originalPicJson.put(DisplayPictureFields.SIZE, original.size());
            originalPicJson.put(DisplayPictureFields.EXTENSION, extension);
            originalPicJson.put(DisplayPictureFields.URL, HttpUtil.buildAbsoluteUrl(String.format("%s%s%s%s%s%s", profilePicRedirectionUrl,
                    Base64.getEncoder().encodeToString(userJson.getBytes()), "/", imageType, ".", extension)));
            profilePicJson.put(imageType, originalPicJson);
        }

        if (profilePicJson.size() > 0) {
            jsonObject.put(FieldKeys.PROFILE_PIC.getName(), profilePicJson);
        }

        return jsonObject;
    }

    /**
     * <p>
     * Converts the user summary details to json
     * </p>
     *
     * @param crmUser Represents the {@link CrmUser} details
     * @return The {@link JsonArray} details
     */
    private JsonObject convertSummaryToJson(final CrmUser crmUser) {
        final JsonObject jsonObject = Json.object();
        final CrmRole role = crmUser.getRole();
        final Collection<Team> teams = crmUser.getTeams();
        final Collection<Territory> territories = crmUser.getTerritories();
        final CrmUser reportingManger = crmUser.getReportingManager();
        final UserDealPreference dealPreference = crmUser.getPreferenceHolder().get(UserDealPreference.TYPE);
        final Pipeline pipeline = dealPreference.getPipeline();

        if (Objects.nonNull(pipeline)) {
            jsonObject.put(CrmUserFields.PIPELINE.getName(), pipelineConverter.convertPartialData(pipeline));
        }

        if (Objects.nonNull(reportingManger)) {
            jsonObject.put(CrmUserFields.REPORTING_MANAGER.getName(), crmUserConverter.convert(reportingManger, true, false));
        }

        if (Objects.nonNull(teams)) {
            final JsonArray teamJson = Json.array();

            teams.forEach(team -> teamJson.put(teamConverter.convertTeam(team)));
            jsonObject.put(CrmUserFields.TEAMS.getName(), teamJson);
        }

        if (Objects.nonNull(territories)) {
            final JsonArray territoryJson = Json.array();

            territories.forEach(territory -> territoryJson.put(territoryConverter.convertTerritoryToJson(territory)));
            jsonObject.put(CrmUserFields.TERRITORIES.getName(), territoryJson);
        }
        jsonObject.put(CrmUserFields.EMAIL.getName(), crmUser.getEmail());
        jsonObject.put(CrmUserFields.PHONE.getName(), crmUser.getMobile());
        jsonObject.put(CrmUserFields.ROLE.getName(), roleConverter.convert(role, true));
        jsonObject.put(CrmUserFields.LAST_LOGIN_TIME.getName(), dateTimeUtil.getTimeStamp(crmUser.getLastLoginTime()));

        return jsonObject;
    }

    /**
     * <p>
     * Converts {@link UserDealPreference} details into {@link JsonObject}
     * </p>
     *
     * @param crmUser Represents the {@link CrmUser} details
     * @param user    Represents the {@link User} details
     * @return The {@link JsonObject}
     */
    public JsonObject convert(final CrmUser crmUser, final User user) {
        final UserPreferenceHolder preferenceHolder = crmUser.getPreferenceHolder();
        final UserDealPreference dealPreference = preferenceHolder.get(UserDealPreference.TYPE);
        final Pipeline pipeline = dealPreference.getPipeline();
        final Collection<Team> teams = crmUser.getTeams();
        final CrmUser reportingManager = crmUser.getReportingManager();
        final JsonObject jsonObject = userConverter.convert(user);
        final UserRegionalPreference userRegionalPreference = preferenceHolder.get(UserRegionalPreference.TYPE);

        if (Objects.nonNull(pipeline)) {
            jsonObject.put(CrmUserFields.PIPELINE.getName(), pipelineConverter.convertPartialData(pipeline));
        }

        if (Objects.nonNull(teams)) {
            final JsonArray teamJson = Json.array();

            teams.forEach(team -> teamJson.put(teamConverter.convertTeam(team)));
            jsonObject.put(CrmUserFields.TEAMS.getName(), teamJson);
        }

        if (Objects.nonNull(reportingManager)) {
            jsonObject.put(CrmUserFields.REPORTING_MANAGER_ID.getName(), crmUserConverter.convert(reportingManager, true, false));
        }

        if (Objects.nonNull(userRegionalPreference)) {
            jsonObject.put(CrmUserFields.PREFERENCE.getName(), preferenceConverter.convertToJson(userRegionalPreference));
        }
        jsonObject.put(CrmUserFields.ID.getName(), crmUser.id());
        jsonObject.put(CrmUserFields.ROLE.getName(), roleConverter.convert(crmUser.getRole(), true));

        return jsonObject;
    }
}

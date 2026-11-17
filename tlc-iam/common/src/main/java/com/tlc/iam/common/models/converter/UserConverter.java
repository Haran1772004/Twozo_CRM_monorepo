package com.tlc.iam.common.models.converter;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.fields.OrganizationFields;
import com.tlc.iam.common.fields.ProfilePictureFields;
import com.tlc.iam.common.models.Organization;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.User;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.util.Locale;
import java.util.Objects;

/**
 * A converter that converts the data from {@link JsonObject} to {@link User} and vice versa
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class UserConverter {

    private final ProfilePictureConverter profilePictureConverter;
    private final DateTimeUtil dateTimeUtil;

    private static class Instance {
        private static final UserConverter INSTANCE = new UserConverter();
    }

    private UserConverter() {
        profilePictureConverter = ProfilePictureConverter.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
    }

    public static UserConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts {@link User} into {@link JsonObject}
     *
     * @param user {@link User} An entity that holds user information
     * @return {@link JsonObject}
     */
    public JsonObject convert(final User user) {
        return convert(user, false);
    }

    /**
     * Converts {@link User} into {@link JsonObject}
     *
     * @param user {@link User} An entity that holds user information
     * @param passwordHashRequired A flag which indicates whether to include the password salt to the json object or not.
     *                         <h2 style="color:red">This flag should only be enabled for internal purpose<h2>
     * @return {@link JsonObject}
     */
    public JsonObject convert(final User user, final boolean passwordHashRequired) {
        final JsonObject jsonObject = Json.object();
        final Organization organization = user.getOrganization();
        final DisplayPicture thumbnailPicture = user.getThumbnail();
        final DisplayPicture originalPicture = user.getOriginalPicture();
        final Long userId = user.id();

        jsonObject.put(UserFields.ID, userId);
        jsonObject.put(UserFields.ORG_ID, user.orgId());
        jsonObject.put(UserFields.FIRST_NAME, user.getFirstName());
        jsonObject.put(UserFields.LAST_NAME, user.getLastName());
        jsonObject.put(UserFields.EMAIL, user.getEmail());
        jsonObject.put(UserFields.MOBILE, user.getMobile());
        jsonObject.put(UserFields.TYPE_ID, Objects.isNull(user.getType()) ? null : user.getType().id());
        jsonObject.put(UserFields.LOCALE, user.getLocaleString());
        jsonObject.put(UserFields.STATE_ID, Objects.isNull(user.getUserState()) ? null : user.getUserState().id());
        jsonObject.put(UserFields.CREATED_TIME, dateTimeUtil.getTimeStamp(user.getCreatedTime()));
        jsonObject.put(UserFields.STATE_UPDATED_TIME, dateTimeUtil.getTimeStamp(user.getStateUpdatedTime()));
        jsonObject.put(UserFields.LAST_LOGIN_TIME, dateTimeUtil.getTimeStamp(user.getLastLoginTime()));
        jsonObject.put(UserFields.JOB_TITLE, user.getJobTitle());
        jsonObject.put(UserFields.IDP_USER_ID, user.getIdpUserId());
        jsonObject.put(UserFields.HAS_ACTIVE_PASSWORD, user.hasActivePassword());

        if (passwordHashRequired) {
            jsonObject.put(UserFields.PASSWORD, user.getPassword());
        }

        if (Objects.nonNull(organization)) {
            jsonObject.put(OrganizationFields.ORGANIZATION, OrganizationConverter.getInstance().convert(organization));
        }

        if (Objects.nonNull(thumbnailPicture)) {
            final JsonObject profileJson = Json.object();
            final String imageType = thumbnailPicture.type().getName();

            profileJson.put(ProfilePictureFields.ID, thumbnailPicture.getFileId());
            profileJson.put(ProfilePictureFields.NAME, imageType);
            profileJson.put(ProfilePictureFields.EXTENSION, thumbnailPicture.extension().getName());
            profileJson.put(ProfilePictureFields.SIZE, thumbnailPicture.size());

            jsonObject.put(imageType, profileJson);
        }

        if (Objects.nonNull(originalPicture)) {
            final JsonObject originalPicJson = Json.object();
            final String extension = originalPicture.extension().getName();
            final String imageType = originalPicture.type().getName();

            originalPicJson.put(ProfilePictureFields.ID, originalPicture.getFileId());
            originalPicJson.put(ProfilePictureFields.NAME, imageType);
            originalPicJson.put(ProfilePictureFields.EXTENSION, extension);
            originalPicJson.put(ProfilePictureFields.SIZE, originalPicture.size());
            jsonObject.put(imageType, originalPicJson);
        }

        return jsonObject;
    }

    /**
     * Converts {@link JsonObject} into {@link User}
     *
     * @param orgId      Organization id
     * @param jsonObject Holds user information
     * @return {@link User}
     */
    public User convert(final Long orgId, final JsonObject jsonObject) {
        final User user = new User();

        if (jsonObject.containsKey(UserFields.ID)) {
            user.setId(jsonObject.getLong(UserFields.ID));
        }

        mapJsonDataToUser(user, orgId, jsonObject);
        return user;
    }

    /**
     * Maps the Json data to {@link User}
     *
     * @param user       An entity that holds the user information
     * @param orgId      Organization id
     * @param jsonObject Holds the value of user entity attributes
     */
    public void mapJsonDataToUser(final User user, final Long orgId, final JsonObject jsonObject) {

        user.setFirstName(jsonObject.containsKey(UserFields.NAME) ? jsonObject.getString(UserFields.NAME) :
                jsonObject.containsKey(UserFields.FIRST_NAME) ? jsonObject.getString(UserFields.FIRST_NAME) : user.getFirstName());
        user.setEmail(jsonObject.containsKey(UserFields.EMAIL)
                ? jsonObject.getString(UserFields.EMAIL) : user.getEmail());
        user.setEmailVerified(jsonObject.containsKey(UserFields.IS_EMAIL_VERIFIED)
                ? (boolean) jsonObject.opt(UserFields.IS_EMAIL_VERIFIED) : user.isEmailVerified());
        user.setMobileNumberVerified(jsonObject.containsKey(UserFields.IS_MOBILE_NUMBER_VERIFIED)
                ? (boolean) jsonObject.opt(UserFields.IS_MOBILE_NUMBER_VERIFIED) : user.isMobileNumberVerified());
        user.setPassword(jsonObject.containsKey(UserFields.PASSWORD) ? jsonObject.getString(UserFields.PASSWORD) : user.getPassword());
        user.setIdpUserId(jsonObject.containsKey(UserFields.IDP_USER_ID) ? jsonObject.getString(UserFields.IDP_USER_ID) :
                user.getIdpUserId());

        if (jsonObject.containsKey(UserFields.HAS_ACTIVE_PASSWORD)) {
            user.setHasActivePassword(jsonObject.getBoolean(UserFields.HAS_ACTIVE_PASSWORD));
        }
        //TODO replace this logic with enum
        user.setLocaleString(jsonObject.optString(UserFields.LOCALE, Locale.getDefault().toString()));

        if (jsonObject.containsKey(UserFields.LAST_NAME)) {
            user.setLastName(jsonObject.isKeyPresentWithNullValue(UserFields.LAST_NAME) ? null : jsonObject.getString(UserFields.LAST_NAME));
        }

        if (jsonObject.containsKey(UserFields.MOBILE)) {
            user.setMobile(jsonObject.isKeyPresentWithNullValue(UserFields.MOBILE) ? null : jsonObject.getString(UserFields.MOBILE));
        }

        if (jsonObject.containsKey(UserFields.TYPE_ID)) {
            user.setType(User.Type.valueOf(jsonObject.getShort(UserFields.TYPE_ID)));
        }
        user.setUserState(jsonObject.containsKey(UserFields.STATE_ID) ? User.State.get(jsonObject.getShort(UserFields.STATE_ID))
                : user.getUserState());
        user.setOrgId(orgId);

        if (jsonObject.containsKey(UserFields.JOB_TITLE)) {
            user.setJobTitle(jsonObject.isKeyPresentWithNullValue(UserFields.JOB_TITLE) ? null : jsonObject.getString(UserFields.JOB_TITLE));
        }

        if (jsonObject.containsKey(UserFields.LAST_LOGIN_TIME)) {
            user.setLastLoginTime(dateTimeUtil.getTemporal(jsonObject.getLong(UserFields.LAST_LOGIN_TIME)));
        }

        if (jsonObject.containsKey(OrganizationFields.ORGANIZATION)) {
            user.setOrganization(OrganizationConverter.getInstance().convert(jsonObject.getJsonObject("organization")));
        }

        if (jsonObject.containsKey(ProfilePictureFields.THUMBNAIL)) {
            user.setThumbnail(profilePictureConverter.constructDisplayPicture(jsonObject.getJsonObject(ProfilePictureFields.THUMBNAIL), ProfilePicture.Quality.THUMBNAIL));
        }

        if (jsonObject.containsKey(ProfilePictureFields.ORIGINAL)) {
            user.setOriginalPicture(profilePictureConverter.constructDisplayPicture(jsonObject.getJsonObject(ProfilePictureFields.ORIGINAL), ProfilePicture.Quality.ORIGINAL));
        }
    }
}


package com.tlc.crm.common.action.iam.web;

import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.attachment.validator.Group;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.RandomIdGenerator;
import com.tlc.file.common.queue.AttachmentRemovalPublisher;
import com.tlc.iam.client.internal.action.SecureAction;

import com.tlc.iam.common.action.req.IamHttpRequest;
import com.tlc.iam.common.action.res.IamHttpResponse;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.ProfilePictureFields;
import com.tlc.iam.common.models.UserProfilePicture;
import com.tlc.iam.common.models.converter.ProfilePictureConverter;
import com.tlc.iam.common.models.validator.EntityValidator;
import com.tlc.iam.common.status.IamErrorCodes;

import com.tlc.web.HttpMethod;
import com.tlc.web.HttpUtil;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;

import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static com.tlc.web.HttpStatus.NOT_FOUND;

/**
 * A web action that handles the profile picture upload/delete requests
 *
 * @author Ashok Selvakumar
 * @version 1.0
 */
@WebAction(path = "/accounts/profile-picture", method = HttpMethod.POST)
public class ProfilePictureManagement extends SecureAction {

    private static final int MAX_DP_SIZE = 2097152; //2 MB

    private final ProfilePictureConverter profilePictureConverter;
    private final AttachmentRemovalPublisher attachmentRemovalPublisher;
    private final RandomIdGenerator randomIdGenerator;
    private final String profilePicRedirectionUrl;

    public ProfilePictureManagement() {
        profilePictureConverter = ProfilePictureConverter.getInstance();
        this.attachmentRemovalPublisher = AttachmentRemovalPublisher.getInstance();
        this.randomIdGenerator = RandomIdGenerator.getInstance();
        final Configurations configurations = ConfHolder.getInstance().getProperties("profile-picture").getNestedConfigs("redirectUrl");
        this.profilePicRedirectionUrl = configurations.getConfig("user");
    }

    /**
     * {@inheritDoc}
     *
     * @param iamHttpRequest Http request
     * @param response    Http response
     */
    @Override
    public void sProcess(final IamHttpRequest iamHttpRequest, final IamHttpResponse response) {
        final String type = iamHttpRequest.getRequestJson().getString(ProfilePictureFields.TYPE);

        switch (type) {
            case "get" -> getProfilePicture(iamHttpRequest, response);
            case "delete" -> deletePicture(iamHttpRequest, response);
            default -> response.setErrorCode(ErrorCode.get(IamErrorCodes.UNSUPPORTED_WEB_ACTION_TYPE));
        }

    }

    /**
     * Fetches the profile picture details
     *
     * @param iamHttpRequest Holds http request information
     * @param response    Holds response information
     */
    private void getProfilePicture(final IamHttpRequest iamHttpRequest, final IamHttpResponse response) {
        final JsonObject requestParams = convertRequestParamsToJson(iamHttpRequest);
        final UserProfilePicture userProfilePicture = profilePictureConverter.convert(requestParams, false);
        final Collection<String> errors = EntityValidator.validate(List.of(userProfilePicture), Group.Get.class);

        if (errors.isEmpty()) {
            final String imageType = userProfilePicture.getImageType();

            if (ProfilePicture.Quality.isValid(imageType)) {
                final Event<byte[]> event = new Event<>(IamEvents.DP_INFO_FETCH, requestParams.getBytes());
                final EventCallback<byte[]> callback = new EventCallback<>() {

                    @Override
                    public void onSuccess(final byte[] bytes) {
                        if (Objects.isNull(bytes)) {
                            sendError(response, null, ErrorCode.get(IamErrorCodes.PROFILE_PICTURE_NOT_FOUND, "i18n.iam.accounts.dp.not.found"));
                        } else if (bytes.length == 0) {
                            sendError(response, null, ErrorCode.get(IamErrorCodes.PROFILE_PICTURE_INFO_RETRIEVAL_FAILED, "i18n.iam.accounts.dp.retrieval.failed"));
                        } else {
                            final JsonObject jsonObject = Json.object(bytes);
                            final JsonObject userJson = Json.object();
                            final String extensionKey = ProfilePictureFields.EXTENSION;
                            final String nameKey = ProfilePictureFields.NAME;
                            final String sizeKey = ProfilePictureFields.SIZE;
                            final String extension = jsonObject.getString(extensionKey);

                            response.put(nameKey, jsonObject.opt(nameKey));
                            response.put(sizeKey, jsonObject.get(sizeKey));
                            response.put(extensionKey, extension);
                            userJson.put(ProfilePictureFields.USER_ID, iamHttpRequest.userId());
                            userJson.put(ProfilePictureFields.ID, randomIdGenerator.generateUniqueId());
                            response.put(ProfilePictureFields.URL, HttpUtil.buildAbsoluteUrl(String.format("%s%s%s%s%s%s", profilePicRedirectionUrl,
                                    Base64.getEncoder().encodeToString(userJson.getBytes()), "/", imageType, ".", extension)));
                            response.send();
                        }
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        sendError(response, null, ErrorCode.get(IamErrorCodes.PROFILE_PICTURE_INFO_RETRIEVAL_FAILED, "i18n.iam.accounts.dp.retrieval.failed"));
                    }
                };

                EventNotifier.get().send(event, callback);
            } else {
                sendError(response, null, ErrorCode.get(IamErrorCodes.UNSUPPORTED_PROFILE_PICTURE_TYPE, "i18n.iam.accounts.dp.unsupported.type"));
            }
        } else {
            response.setErrorCode(ErrorCode.get(IamErrorCodes.PROFILE_PICTURE_INFO_RETRIEVAL_FAILED));
            response.put(ProfilePictureFields.ERRORS, response.resolveI18nString(errors));
            response.send();
        }
    }

    /**
     * Handles the profile picture delete event
     *
     * @param iamHttpRequest Holds the request details
     * @param response    Holds the response details
     */
    private void deletePicture(final IamHttpRequest iamHttpRequest, final IamHttpResponse response) {
        final JsonObject requestParams = convertRequestParamsToJson(iamHttpRequest);
        final Collection<String> errors = EntityValidator.validate(List.of(profilePictureConverter.convert(requestParams, false)), Group.Remove.class);

        if (errors.isEmpty()) {
            final Event<byte[]> event = new Event<>(IamEvents.DP_DELETE, requestParams.getBytes());
            final EventCallback<byte[]> callback = new EventCallback<>() {

                @Override
                public void onSuccess(final byte[] bytes) {
                    final JsonObject jsonObject = Json.object(bytes);
                    final String message = jsonObject.optString(ProfilePictureFields.MESSAGE, "");

                    switch (message) {
                        case "success" -> {
                            response.put(ProfilePictureFields.MESSAGE, response.resolveI18nString("i18n.iam.accounts.dp.delete.success"));
                            final Collection<Long> attachmentIds = new HashSet<>();

                            jsonObject.getJsonArray(ProfilePictureFields.ATTACHMENTS).forEach(id -> attachmentIds.add((Long) id));
                            attachmentRemovalPublisher.publish(String.valueOf(iamHttpRequest.userId()), attachmentIds);
                        }
                        case "imageNotFound" -> {
                            response.setStatus(NOT_FOUND);
                            response.put(ProfilePictureFields.MESSAGE, response.resolveI18nString("i18n.iam.accounts.dp.not.found"));
                        }
                        default ->
                            sendError(response, null, ErrorCode.get(IamErrorCodes.PROFILE_PICTURE_DELETE_ACTION_FAILED, "i18n.iam.accounts.dp.delete.failed"));
                    }

                    response.send();
                }

                @Override
                public void onFailure(Throwable throwable) {
                    sendError(response, null, ErrorCode.get(IamErrorCodes.PROFILE_PICTURE_DELETE_ACTION_FAILED, "i18n.iam.accounts.dp.delete.failed", throwable));
                }
            };

            EventNotifier.get().send(event, callback);
        } else {
            response.put(ProfilePictureFields.ERRORS, response.resolveI18nString(errors));
            sendError(response, null, ErrorCode.get(IamErrorCodes.PROFILE_PICTURE_DELETE_ACTION_FAILED, "i18n.iam.accounts.dp.delete.failed"));
        }
    }

    /**
     * <p>
     * Converts the request params to {@link JsonObject}
     * </p>
     *
     * @param iamHttpRequest Represents the {@link IamHttpRequest}
     * @return the {@link JsonObject}
     */
    private JsonObject convertRequestParamsToJson(final IamHttpRequest iamHttpRequest) {
        final JsonObject data = iamHttpRequest.getRequestJson().getJsonObject(ProfilePictureFields.DATA);
        final JsonObject requestParams = Json.object();
        final String imageType = ProfilePictureFields.IMAGE_TYPE;

        requestParams.put(ProfilePictureFields.ORG_ID, iamHttpRequest.orgId());
        requestParams.put(ProfilePictureFields.USER_ID, iamHttpRequest.userId());
        requestParams.put(imageType, data.containsKey(imageType) ? data.getString(imageType) : null);

        return requestParams;
    }
}

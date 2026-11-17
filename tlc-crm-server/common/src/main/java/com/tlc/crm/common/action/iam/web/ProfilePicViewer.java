package com.tlc.crm.common.action.iam.web;

import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.attachment.storage.model.PreSignedUrlHeader;
import com.tlc.attachment.validator.Group;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.file.client.service.AttachmentService;
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
import com.tlc.web.HttpStatus;
import com.tlc.web.WebAction;
import com.tlc.web.WebExchange;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A web action that is used to stream the profile picture data
 *
 * @author Ashok Selvakumar
 * @version 1.0
 */
@WebAction(path = "/accounts/profile-picture/{userId}/{imageName}", method = HttpMethod.GET)
public class ProfilePicViewer extends SecureAction {

    private static final String FILE_NAME = "profile_picture.";
    private static final String CONTENT_DISPOSITION_FORMAT = "inline; filename=%s";
    private static final String CACHE_CONTROL_POLICY = "private, max-age=3600";

    private final ProfilePictureConverter profilePictureConverter;
    private final AttachmentService attachmentService;

    public ProfilePicViewer() {
        profilePictureConverter = ProfilePictureConverter.getInstance();
        this.attachmentService = AttachmentService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Http request
     * @param response Http response
     */
    @Override
    public void sProcess(final IamHttpRequest request, final IamHttpResponse response) {
        final String imageName = request.getParameter(ProfilePictureFields.IMAGE_NAME);
        final String userId = request.getParameter(ProfilePictureFields.USER_ID);

        if (Objects.nonNull(imageName) && Objects.nonNull(userId)) {
            final int lastIndexOfDot = imageName.lastIndexOf(".");
            final String imageType = imageName.substring(0, lastIndexOfDot);
            final String extension = imageName.substring(lastIndexOfDot + 1);

            if (!ProfilePicture.Extension.isValid(extension) || !ProfilePicture.Quality.isValid(imageType)) {
                response.setStatus(HttpStatus.NOT_FOUND);
                response.send();
            } else {
                final JsonObject userJson = Json.object(Base64.getDecoder().decode(userId));
                final JsonObject requestParams = convertRequestParamsToJson(request.orgId(), imageType, userJson.getLong(ProfilePictureFields.USER_ID));
                final UserProfilePicture userProfilePicture = profilePictureConverter.convert(requestParams, false);
                final Collection<String> errors = EntityValidator.validate(List.of(userProfilePicture), Group.PictureRendering.class);

                if (errors.isEmpty()) {
                    final Event<byte[]> event = new Event<>(IamEvents.DP_VIEW, requestParams.getBytes());
                    final EventCallback<byte[]> callback = new EventCallback<>() {

                        @Override
                        public void onSuccess(final byte[] responseBytes) {
                            if (null == responseBytes || responseBytes.length == 0) {
                                response.setStatus(HttpStatus.NOT_FOUND);
                            } else {
                                final JsonObject responseJson = Json.object(responseBytes);
                                final String status = responseJson.getString(ProfilePictureFields.STATUS);

                                switch (status) {
                                    case "success" -> {
                                        final String fileName = String.format("%s%s", FILE_NAME, extension);
                                        final String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
                                        final String encodedContentDisposition = String.format(CONTENT_DISPOSITION_FORMAT, encodedFileName);
                                        final String contentType = String.join("/", "image", extension);

                                        final String presignedUrl = attachmentService.getPresignedUrl(responseJson.getLong(ProfilePictureFields.ID), 1,
                                                Map.of(PreSignedUrlHeader.RESPONSE_CONTENT_TYPE, contentType, PreSignedUrlHeader.RESPONSE_CONTENT_DISPOSITION, encodedContentDisposition));

                                        response.setHeader(WebExchange.Header.CACHE_CONTROL.getName(), CACHE_CONTROL_POLICY);
                                        response.setTemporaryRedirectUrl(presignedUrl);
                                        response.send();
                                    }
                                    case "imageNotFound" -> {
                                        response.setStatus(HttpStatus.NOT_FOUND);
                                        response.put(ProfilePictureFields.MESSAGE, response.resolveI18nString("i18n.iam.accounts.dp.not.found"));
                                    }
                                    default -> {
                                        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
                                    }
                                }
                            }

                            response.send();
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
                            response.send();
                        }
                    };

                    EventNotifier.get().send(event, callback);
                } else {
                    response.setErrorCode(ErrorCode.get(IamErrorCodes.PROFILE_PICTURE_INFO_RETRIEVAL_FAILED));
                    response.put(ProfilePictureFields.ERRORS, response.resolveI18nString(errors));
                    response.send();
                }
            }
        }
    }

    /**
     * <p>
     * Converts the request params to {@link JsonObject}
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the {@link JsonObject}
     */
    private JsonObject convertRequestParamsToJson(final long orgId, final String imageType, final long userId) {
        final JsonObject requestParams = Json.object();

        requestParams.put(ProfilePictureFields.ORG_ID, orgId);
        requestParams.put(ProfilePictureFields.USER_ID, userId);
        requestParams.put(ProfilePictureFields.IMAGE_TYPE, imageType);

        return requestParams;
    }
}

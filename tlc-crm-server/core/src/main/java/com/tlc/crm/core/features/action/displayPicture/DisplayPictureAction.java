package com.tlc.crm.core.features.action.displayPicture;

import com.tlc.attachment.io.File;
import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.attachment.validator.Group;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.RandomIdGenerator;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.features.api.displayPicture.DisplayPictureManager;
import com.tlc.crm.core.features.internal.displayPicture.status.DisplayPictureErrorCode;

import com.tlc.crm.entity.fields.displayPicture.DisplayPictureFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.converter.DisplayPictureConverter;
import com.tlc.crm.entity.models.displayPicture.SourceDisplayPicture;

import com.tlc.file.common.queue.AttachmentRemovalPublisher;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.web.HttpUtil;
import com.tlc.web.WebAction;

import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Manages display picture related web actions
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 * @see AuthorizationController
 */
@WebAction(path = "/source/profile-picture")
public class DisplayPictureAction extends AuthorizationController {

    private final DisplayPictureConverter displayPictureConverter;
    private final AttachmentRemovalPublisher attachmentRemovalPublisher;
    private final ModelValidator modelValidator;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final DisplayPictureManager displayPictureManager;
    private final RandomIdGenerator randomIdGenerator;
    private final String profilePicRedirectUrl;

    public DisplayPictureAction() {
        displayPictureConverter = DisplayPictureConverter.getInstance();
        this.attachmentRemovalPublisher = AttachmentRemovalPublisher.getInstance();
        modelValidator = ValidatorServiceProvider.get().modelValidator();
        contactManager = ContactManager.getInstance();
        companyManager = CompanyManager.getInstance();
        displayPictureManager = DisplayPictureManager.getInstance();
        randomIdGenerator = RandomIdGenerator.getInstance();
        final Configurations configurations = ConfHolder.getInstance().getProperties("profile-picture").getNestedConfigs("redirectUrl");
        this.profilePicRedirectUrl = configurations.getConfig("source");
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(DisplayPictureFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(DisplayPictureFields.DATA);

        switch (type) {
            case "delete" -> deletePicture(request, crmResponse, data);
            case "get" -> getDisplayPicture(request, crmResponse, data);
            default -> crmResponse.setErrorCode(ErrorCode.get(DisplayPictureErrorCode.UNSUPPORTED_WEB_ACTION_TYPE));
        }
    }

    /**
     * <p>
     * Deletes the profile picture for the given entity
     * </p>
     *
     * @param request     Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     * @param data        Represents the {@link JsonObject}
     */
    private void deletePicture(final CrmRequest request, final CrmResponse crmResponse, final JsonObject data) {
        try {
            final SourceDisplayPicture picture = displayPictureConverter.convert(convertRequestToJson(null, request, data, crmResponse), false);
            final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(picture), Group.Remove.class);

            if (errors.isEmpty()) {
                final long orgId = picture.orgId();
                final Long entityId = picture.getEntityId();
                final String sourceName = picture.getSource();
                final Source source = Source.get(sourceName);

                if (isValidSource(orgId, entityId, source, crmResponse)) {
                    final Optional<DisplayPicture> displayPicture = displayPictureManager.getDisplayPicture(orgId, entityId,
                            source, ProfilePicture.Quality.ORIGINAL);

                    if (displayPicture.isPresent()) {
                        final Collection<Long> displayPictureIds = displayPictureManager.getDisplayPictureIds(orgId, entityId, source);

                        displayPictureManager.deleteDisplayPictureInfo(orgId, entityId, source);

                        if (!displayPictureIds.isEmpty()) {
                            attachmentRemovalPublisher.publish(String.valueOf(entityId), displayPictureIds);
                        }
                        crmResponse.put(DisplayPictureFields.SUCCESS, crmResponse.resolveI18nString("i18n.source.dp.deleted.successfully"));
                    } else {
                        crmResponse.setErrorCode(ErrorCode.get(DisplayPictureErrorCode.DP_NOT_FOUND, "i18n.source.dp.not.found"));
                    }
                } else {
                    crmResponse.setErrorCode(ErrorCode.get(DisplayPictureErrorCode.INVALID_SOURCE, "i18n.source.not.found"));
                }
            } else {
                final ErrorCode errorCode = ErrorCode.get(DisplayPictureErrorCode.VALIDATION_FAILED, "i18n.source.dp.deletion.failed");

                crmResponse.setErrorCode(errorCode);
                crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DELETE_ACTION_DATA_VALIDATION_FAILED), errors));
            }
        } catch (Exception e) {
            throw ErrorCode.get(DisplayPictureErrorCode.PROFILE_PICTURE_DELETION_ACTION_FAILED, crmResponse.resolveI18nString("i18n.source.dp.deletion.failed"));
        }
    }

    /**
     * <p>
     * Gets the display picture for the given entity
     * </p>
     *
     * @param request     Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     * @param data        Represents the {@link JsonObject}
     */
    private void getDisplayPicture(final CrmRequest request, final CrmResponse crmResponse, final JsonObject data) {
        final SourceDisplayPicture picture = displayPictureConverter.convert(convertRequestToJson(null, request, data, crmResponse), false);
        final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(picture), Group.Get.class);

        if (errors.isEmpty()) {
            final Long orgId = picture.orgId();
            final Long entityId = picture.getEntityId();
            final String sourceName = picture.getSource();
            final Source source = Source.get(sourceName);

            if (isValidSource(orgId, entityId, source, crmResponse)) {
                final String imageType = data.getString(DisplayPictureFields.IMAGE_TYPE);

                if (ProfilePicture.Quality.isValid(imageType)) {
                    final Optional<DisplayPicture> displayPicture = displayPictureManager.getDisplayPicture(orgId,
                            entityId, source, ProfilePicture.Quality.get(picture.getImageType()));

                    if (displayPicture.isPresent()) {
                        final JsonObject jsonObject = Json.object();

                        jsonObject.put(DisplayPictureFields.SOURCE, sourceName);
                        jsonObject.put(DisplayPictureFields.ENTITY_ID, entityId);
                        jsonObject.put(DisplayPictureFields.ID, randomIdGenerator.generateUniqueId());

                        final JsonObject displayPictureJson = displayPictureConverter.convert(displayPicture.get());
                        final String extensionKey = DisplayPictureFields.EXTENSION;
                        final String sizeKey = DisplayPictureFields.SIZE;
                        final String nameKey = DisplayPictureFields.NAME;
                        final String extension = displayPictureJson.getString(extensionKey);

                        crmResponse.put(nameKey, displayPictureJson.opt(nameKey));
                        crmResponse.put(sizeKey, displayPictureJson.get(sizeKey));
                        crmResponse.put(extensionKey, extension);
                        crmResponse.put(DisplayPictureFields.URL, HttpUtil.buildAbsoluteUrl(String.format("%s%s%s%s%s%s", profilePicRedirectUrl,
                                Base64.getEncoder().encodeToString(jsonObject.getBytes()), "/", imageType, ".", extension)));
                    } else {
                        crmResponse.setErrorCode(ErrorCode.get(DisplayPictureErrorCode.DP_NOT_FOUND, "i18n.source.dp.not.found"));
                    }
                }
            } else {
                crmResponse.setErrorCode(ErrorCode.get(DisplayPictureErrorCode.INVALID_SOURCE, "i18n.source.not.found"));
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(DisplayPictureErrorCode.PROFILE_PICTURE_FETCH_ACTION_FAILED, "i18n.source.dp.fetch.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DATA_VALIDATION_FAILED), errors));
        }
    }

    /**
     * <p>
     * Converts the request to {@link JsonObject}
     * </p>
     *
     * @param originalPicture Represents the original {@link File}
     * @param request         Represents the {@link CrmRequest}
     * @param data            Represents the {@link JsonObject}
     * @return the {@link JsonObject}
     */
    private JsonObject convertRequestToJson(final File originalPicture, final CrmRequest request, final JsonObject data, final CrmResponse crmResponse) {
        try {
            final JsonObject jsonObject = Json.object();
            final String entityIdKey = DisplayPictureFields.ENTITY_ID;
            final String sourceKey = DisplayPictureFields.SOURCE;
            final String imageTypeKey = DisplayPictureFields.IMAGE_TYPE;

            jsonObject.put(DisplayPictureFields.ORG_ID, request.orgId());
            jsonObject.put(DisplayPictureFields.USER_ID, request.userId());
            jsonObject.put(entityIdKey, data.containsKey(entityIdKey) ? data.getString(entityIdKey) : null);
            jsonObject.put(sourceKey, data.containsKey(sourceKey) ? data.getString(sourceKey).toLowerCase() : null);
            jsonObject.put(imageTypeKey, data.containsKey(imageTypeKey) ? data.getString(imageTypeKey) : null);

            if (Objects.nonNull(originalPicture)) {
                jsonObject.put(DisplayPictureFields.EXTENSION, originalPicture.extension().toLowerCase());
                jsonObject.put(DisplayPictureFields.SIZE, originalPicture.size());
            }

            return jsonObject;
        } catch (Exception e) {
            throw ErrorCode.get(DisplayPictureErrorCode.FILE_NOT_FOUND, crmResponse.resolveI18nString("18n.source.dp.not.found"));
        }
    }

    /**
     * <p>
     * Checks whether the source given is valid or not
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param entityId Represents the entity id
     * @param source   Represents the {@link Source}
     * @return true, if present
     */
    private boolean isValidSource(final long orgId, final Long entityId, final Source source, final CrmResponse crmResponse) {
        switch (source) {
            case CONTACT -> {
                if (!contactManager.exists(orgId, entityId)) {
                    return false;
                }
            }
            case COMPANY -> {
                if (!companyManager.exists(orgId, entityId)) {
                    return false;
                }
            }
            default -> throw ErrorCode.get(DisplayPictureErrorCode.INVALID_SOURCE, crmResponse.resolveI18nString("i18n.source.not.found"));
        }
        return true;
    }
}

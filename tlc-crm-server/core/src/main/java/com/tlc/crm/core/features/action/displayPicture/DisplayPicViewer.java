package com.tlc.crm.core.features.action.displayPicture;

import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.attachment.storage.model.PreSignedUrlHeader;
import com.tlc.attachment.validator.Group;

import com.tlc.crm.entity.fields.displayPicture.DisplayPictureFields;
import com.tlc.crm.entity.models.displayPicture.SourceDisplayPicture;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.core.features.api.displayPicture.DisplayPictureManager;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.validator.ModelValidator;

import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.web.HttpMethod;
import com.tlc.web.WebAction;
import com.tlc.web.WebExchange;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Manages display picture view web action
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 * @see AuthorizationController
 */
@WebAction(path = "/source/profile-picture/{source}/{image}", method = HttpMethod.GET)
public class DisplayPicViewer extends AuthorizationController {

    private static final String FILE_NAME = "profile_picture.";
    private static final String CONTENT_DISPOSITION_FORMAT = "inline; filename=%s";

    private final DisplayPictureManager displayPictureManager;
    private final ModelValidator modelValidator;

    public DisplayPicViewer() {
        displayPictureManager = DisplayPictureManager.getInstance();
        modelValidator = ValidatorServiceProvider.get().modelValidator();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final String image = request.getParam(DisplayPictureFields.IMAGE).findFirst().orElse(null);
        final String sourceDetail = request.getParam(DisplayPictureFields.SOURCE).findFirst().orElse(null);

        if (Objects.nonNull(image) && Objects.nonNull(sourceDetail)) {
            final int lastIndexOfDot = image.lastIndexOf(".");
            final String fileType = image.substring(0, lastIndexOfDot);
            final String extension = image.substring(lastIndexOfDot + 1);

            if (!ProfilePicture.Extension.isValid(extension) || !ProfilePicture.Quality.isValid(fileType)) {
                crmResponse.put(DisplayPictureFields.ERROR, crmResponse.resolveI18nString("i18n.source.dp.invalid.type"));
            } else {
                final long orgId = request.orgId();
                final long userId = request.userId();
                final JsonObject json = Json.object(Base64.getDecoder().decode(sourceDetail));
                final SourceDisplayPicture picture = convertToModel(orgId, userId, json, fileType);
                final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(picture), Group.Get.class);

                if (errors.isEmpty()) {
                    final String source = picture.getSource();
                    final Long entityId = picture.getEntityId();
                    final ProfilePicture.Quality imageQuality = ProfilePicture.Quality.get(fileType);
                    final Long pictureFileId = displayPictureManager.getDisplayPictureFileId(orgId, entityId, Source.get(source), imageQuality);

                    if (Objects.nonNull(pictureFileId)) {
                        final String fileName = String.format("%s%s", FILE_NAME, extension);
                        final String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
                        final String encodedContentDisposition = String.format(CONTENT_DISPOSITION_FORMAT, encodedFileName);
                        final String contentType = String.join("/", "image", extension);
                        final String presignedUrl = AttachmentService.getInstance().getPresignedUrl(pictureFileId, 1,
                                Map.of(PreSignedUrlHeader.RESPONSE_CONTENT_TYPE, contentType,
                                        PreSignedUrlHeader.RESPONSE_CONTENT_DISPOSITION, encodedContentDisposition));

                        crmResponse.setHeader(WebExchange.Header.CACHE_CONTROL.getName(), "private, max-age=3600");
                        crmResponse.setTemporaryRedirectUrl(presignedUrl);
                        crmResponse.send();
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Converts the {@link JsonObject} to {@link SourceDisplayPicture}
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param userId    Represents the user id
     * @param json      Represents the {@link JsonObject}
     * @param imageType Represents the image type
     * @return the {@link SourceDisplayPicture}
     */
    private SourceDisplayPicture convertToModel(final long orgId, final long userId, final JsonObject json, final String imageType) {
        final SourceDisplayPicture sourceDisplayPicture = new SourceDisplayPicture(orgId, userId);

        sourceDisplayPicture.setEntityId(json.getLong(DisplayPictureFields.ENTITY_ID));
        sourceDisplayPicture.setSource(json.getString(DisplayPictureFields.SOURCE));
        sourceDisplayPicture.setImageType(imageType);

        return sourceDisplayPicture;
    }
}

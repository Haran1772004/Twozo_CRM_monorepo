package com.tlc.crm.core.action.transformer;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.formats.Formats;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.features.api.displayPicture.DisplayPictureManager;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.fields.transformer.TransformerFields;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages company related transformer actions.
 *
 * @author eswari
 * @version 1.0
 */
@Transformer(name = "company", type = Transformer.Type.FIELD)
public class CompanyFieldTransformer implements FieldTransformer {

    private final CompanyConverter companyConverter;
    private final DisplayPictureManager displayPictureManager;

    public CompanyFieldTransformer() {
        companyConverter = CompanyConverter.getInstance();
        displayPictureManager = DisplayPictureManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId
     * @param fieldName  the name of the field to use for the transformation
     * @param entity the map of data to transform
     * @return the {@link Map} contains transformed value
     */
    @Override
    public Map<String, Object> transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        final Optional<Object> optValue = Optional.ofNullable(entity.get(fieldName));

        return optValue.map(fieldId -> {
            final Map<String, Object> transformedValue = new HashMap<>();
            final String nameKey = TransformerFields.NAME.getName();
            final String websiteKey = TransformerFields.WEBSITE.getName();
            final String thumbnailNeededKey = TransformerFields.COMPANY_THUMBNAIL_NEEDED.getName();
            final String columnFormat = Formats.COLUMN_FORMAT.get();

            transformedValue.put(TransformerFields.ID.getName(), fieldId);
            transformedValue.put(nameKey, entity.get(String.format(columnFormat, fieldName, nameKey)));
            transformedValue.put(websiteKey, entity.get(String.format(columnFormat, fieldName, websiteKey)));

            if (entity.containsKey(thumbnailNeededKey) && (boolean) entity.get(thumbnailNeededKey)) {
                final Long companyId = (Long) fieldId;
                final Optional<DisplayPicture> displayPicture =
                        displayPictureManager.getDisplayPicture(orgId, companyId, Source.COMPANY, ProfilePicture.Quality.THUMBNAIL);

                displayPicture.ifPresent(profilePicture -> transformedValue.put(TransformerFields.THUMBNAIL.getName(),
                        companyConverter.convertThumbnailToJson(companyId, Source.COMPANY.getName(), profilePicture).toMap()));
            }

            return transformedValue;
        }).orElseGet(() -> null);
    }
}
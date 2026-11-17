package com.tlc.crm.core.action.transformer;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.formats.Formats;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.features.api.displayPicture.DisplayPictureManager;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.fields.transformer.TransformerFields;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages contact related transformer actions.
 *
 * @author eswari
 * @version 1.0
 */
@Transformer(name = "contact", type = Transformer.Type.FIELD)
public class ContactFieldTransformer implements FieldTransformer {

    private final DisplayPictureManager displayPictureManager;
    private final ContactConverter contactConverter;

    public ContactFieldTransformer() {
        contactConverter = ContactConverter.getInstance();
        displayPictureManager = DisplayPictureManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organisation id.
     * @param fieldName  the name of the field to use for the transformation
     * @param entity the map of data to transform
     * @return the {@link Map} contains transformed value
     */
    @Override
    public Map<String, Object> transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        final Optional<Object> optValue = Optional.ofNullable(entity.get(fieldName));
        final String thumbnailNeededKey = TransformerFields.CONTACT_THUMBNAIL_NEEDED.getName();
        final String thumbnailKey = TransformerFields.THUMBNAIL.getName();

        return optValue.map(fieldId -> {
            final Map<String, Object> transformedValue = new HashMap<>();
            final String firstNameKey = TransformerFields.FIRST_NAME.getName();
            final String lastNameKey = TransformerFields.LAST_NAME.getName();
            final String columnFormat = Formats.COLUMN_FORMAT.get();

            transformedValue.put(TransformerFields.ID.getName(), fieldId);
            transformedValue.put(firstNameKey, entity.get(String.format(columnFormat, fieldName, firstNameKey)));
            transformedValue.put(lastNameKey, entity.get(String.format(columnFormat, fieldName, lastNameKey)));

            if (entity.containsKey(thumbnailNeededKey) && (boolean) entity.get(thumbnailNeededKey)) {
                final Long contactId = (Long) fieldId;
                final Optional<DisplayPicture> displayPicture = displayPictureManager.getDisplayPicture(orgId, contactId, Source.CONTACT, ProfilePicture.Quality.THUMBNAIL);

                displayPicture.ifPresent(profilePicture -> transformedValue.put(thumbnailKey,
                        contactConverter.convertThumbnailToJson(contactId, Source.CONTACT.getName(), profilePicture).toMap()));
            }
            return transformedValue;
        }).orElseGet(() -> null);
    }
}
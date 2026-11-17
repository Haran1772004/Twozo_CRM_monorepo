package com.tlc.crm.entity.action.transformer;

import com.tlc.crm.entity.fields.transformer.TransformerFields;
import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.icon.service.IconService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Transforms the icon field entry into required forms.
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
@Transformer(name = "icon", type = Transformer.Type.FIELD)
public class IconFieldTransformer implements FieldTransformer {

    private final IconService iconService;

    public IconFieldTransformer() {
        iconService = IconService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organisation id.
     * @param fieldName  Represents the field name used for the transformation
     * @param entity the map of data to transform
     * @return the {@link Map} contains transformed value
     */
    @Override
    public Map<String, Object> transform(final long orgId, final String fieldName, final Map<String, Object> entity) {
        final Optional<Object> optValue = Optional.ofNullable(entity.get(fieldName));

        return optValue.map(fieldId -> {
            final Map<String, Object> transformedValue = new HashMap<>();

            transformedValue.put(TransformerFields.ID.getName(), fieldId);
            transformedValue.put(TransformerFields.ICON_NAME.getName(), iconService.get((Long) fieldId).getName());

            return transformedValue;
        }).orElseGet(() -> null);
    }
}

package com.tlc.crm.entity.action.transformer;

import com.tlc.crm.common.list.transformer.FieldTransformer;
import com.tlc.crm.common.list.transformer.annotation.Transformer;
import com.tlc.crm.common.list.transformer.formats.Formats;
import com.tlc.crm.entity.fields.transformer.TransformerFields;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.l10n.model.country.Country;
import com.tlc.crm.entity.models.type.Address;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages {@link Address} related transformer actions.
 *
 * @author eswari
 * @version 1.0
 */
@Transformer(name = "address", type = Transformer.Type.FIELD)
public class AddressFieldTransformer implements FieldTransformer {

    private final FieldTypeService fieldTypeService;

    public AddressFieldTransformer() {
        fieldTypeService = FieldTypeService.getInstance();
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

        return optValue.map(fieldId -> {
            final Map<String, Object> transformedValue = new HashMap<>();
            final Map<StoreSubType, FieldSubType> subTypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.ADDRESS);
            final String columnFormat = Formats.COLUMN_FORMAT.get();
            final Collection<String> subFieldValues = new ArrayList<>();

            for (final Map.Entry<StoreSubType, FieldSubType> entry : subTypes.entrySet()) {
                final StoreSubType storeSubType = entry.getKey();
                final String name = storeSubType.getAliasName();
                final Object value = entity.get(String.format(columnFormat, fieldName, name));

                transformedValue.put(name, value);

                if (Objects.nonNull(value)) {

                    switch (storeSubType){
                        case COUNTRY -> subFieldValues.add(Country.getCountryById((Integer) value).getName());
                        default -> subFieldValues.add(String.valueOf(value));
                    }
                }
            }
            transformedValue.put(TransformerFields.ID.getName(), fieldId);
            transformedValue.put(TransformerFields.DISPLAY_ADDRESS.getName(), String.join(", ", subFieldValues));

            return transformedValue;
        }).orElseGet(() -> null);
    }
}
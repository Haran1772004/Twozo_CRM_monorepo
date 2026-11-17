package com.tlc.crm.entity.features.validator.internal;

import com.tlc.commons.color.Color;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.features.validator.annotation.TagValidator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * It validates {@link Tag} entity and returns appropriate message
 * </p>
 *
 * @author EswariNivethaVU
 * @version 1.0
 */
public class TagValidatorImpl implements ConstraintValidator<TagValidator, Tag> {

    private static final Set<Color> ALLOWED_COLORS = Set.of(
            Color.get((short) 1),
            Color.get((short) 2),
            Color.get((short) 3),
            Color.get((short) 4),
            Color.get((short) 5),
            Color.get((short) 6),
            Color.get((short) 7),
            Color.get((short) 8)
    );

    /**
     * {@inheritDoc}
     *
     * @param tag                        An entity that holds the tag attributes
     * @param constraintValidatorContext Context in which constraint is evaluated
     * @return true is valid
     */
    @Override
    public boolean isValid(final Tag tag, final ConstraintValidatorContext constraintValidatorContext) {
        final Object tagValue = tag.getName();

        if (tagValue instanceof String value && value.isBlank()) {
            return false;
        }

        if (!isColorValid(tag.getColor(), constraintValidatorContext)) {
            return false;
        }

        return (Objects.nonNull(tagValue) || Objects.nonNull(tag.getEntityIds()));
    }

    /**
     * Validates that the tag's color, if present, is one of the allowed colors.
     *
     * @param color   The color to validate
     * @param context Context in which constraint is evaluated
     * @return true if the color is null or allowed, false otherwise
     */
    private boolean isColorValid(final Color color, final ConstraintValidatorContext context) {
        if (!ALLOWED_COLORS.contains(color)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("i18n.tag.invalid.color")
                    .addPropertyNode("color")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
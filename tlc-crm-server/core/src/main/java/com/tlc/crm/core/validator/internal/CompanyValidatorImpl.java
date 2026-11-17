package com.tlc.crm.core.validator.internal;

import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.validator.annotation.CompanyValidator;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.validator.DefaultValidator;
import com.tlc.crm.entity.validator.ValidatorContext;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Map;
import java.util.Objects;

/**
 * Validates {@link Company} entity and returns appropriate message
 *
 * @author pandiaraj
 */
public class CompanyValidatorImpl extends DefaultValidator implements ConstraintValidator<CompanyValidator, Company> {

    private final FieldService fieldService = FieldService.getSecureInstance(true);

    /**
     * Checks whether all the entity are having the valid values or not
     *
     * @param company                    The company values has to be validated
     * @param constraintValidatorContext Validation context
     * @return If the given entity is valid, it will return true
     */
    @Override
    public boolean isValid(final Company company, final ConstraintValidatorContext constraintValidatorContext) {
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(company.orgId(), Company.ENTITY_TYPE);
        final ValidatorContext validatorContext = new ValidatorContext(constraintValidatorContext);
        final FieldInfo territoryField = fields.get(CompanyFields.TERRITORY);
        final FieldInfo tagField = fields.get(CompanyFields.TAGS);

        if (Objects.nonNull(tagField)) {
            final PresetConfig presetConfig = tagField.presetConfig();
            final boolean required = presetConfig.required();
            final SummaryInfo summaryInfo = company.getSummaryInfo();

            if (Objects.nonNull(summaryInfo)) {
                validateTags(company.orgId(), summaryInfo.getTags(), validatorContext, required);
            } else if (required) {
                addConstraintViolationMessage(validatorContext, "i18n.company.field.tags.required", tagField.aliasName());
            }
        }
        validateCreatedTime(company, validatorContext, fields.get(CompanyFields.CREATED_AT));
        validate(validatorContext, company, fields);
        validateTerritoryField(territoryField, Objects.isNull(company.getTerritory()), validatorContext);
        disableDefaultConstraintViolation(validatorContext);

        return !validatorContext.isErrorsFound();
    }
}
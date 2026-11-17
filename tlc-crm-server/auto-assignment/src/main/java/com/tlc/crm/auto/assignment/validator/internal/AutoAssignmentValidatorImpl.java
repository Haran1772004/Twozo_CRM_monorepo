package com.tlc.crm.auto.assignment.validator.internal;

import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.auto.assignment.api.model.AutoAssignmentRule;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteria;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteriaSet;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteriaValue;
import com.tlc.crm.auto.assignment.api.model.AssignmentRuleUser;
import com.tlc.crm.auto.assignment.validator.annotation.AutoAssignmentValidator;

import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.Territory;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.validator.ValidatorContext;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Validates the {@link AutoAssignmentRule} entity and returns appropriate message.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class AutoAssignmentValidatorImpl implements ConstraintValidator<AutoAssignmentValidator, AutoAssignmentRule> {

    private final FieldService fieldService;
    private final TerritoryManager territoryManager;
    private final CrmUserManager crmUserManager;

    public AutoAssignmentValidatorImpl() {
        this.fieldService = FieldService.getInstance();
        this.territoryManager = TerritoryManager.getInstance();
        this.crmUserManager = CrmUserManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param autoAssignmentRule {@link AutoAssignmentRule} to validate.
     * @param context            Context in which the constraint is evaluated.
     */
    @Override
    public boolean isValid(final AutoAssignmentRule autoAssignmentRule, final ConstraintValidatorContext context) {
        final ValidatorContext validatorContext = new ValidatorContext(context);
        final List<AutoAssignmentCriteriaSet> criteriaSets = (List<AutoAssignmentCriteriaSet>) autoAssignmentRule.getCriteriaSets();
        final Long orgId = autoAssignmentRule.orgId();

        if (Objects.nonNull(criteriaSets)) {
            final Collection<AutoAssignmentCriteria> totalCriteria = new HashSet<>();

            criteriaSets.forEach(set -> totalCriteria.addAll(set.getCriteriaList()));

            if (NumericalConstant.TEN < totalCriteria.size()) {
                addConstraintViolationMessage(validatorContext, "i18n.auto.assignment.criteria.size.exceeds");
            } else {
                final int size = criteriaSets.size();

                for (int index = 0; index < size; index++) {
                    final AutoAssignmentCriteriaSet assignmentCriteriaSet = criteriaSets.get(index);

                    if (index < (size - 1) && Objects.isNull(assignmentCriteriaSet.getConnector())) {
                        addConstraintViolationMessage(validatorContext, "i18n.auto.assignment.criteria.set.connector.required");
                        break;
                    }
                    final List<AutoAssignmentCriteria> criteriaList = (List<AutoAssignmentCriteria>) assignmentCriteriaSet.getCriteriaList();
                    final Collection<Long> fieldIds = criteriaList.stream().map(AutoAssignmentCriteria::getFieldId).collect(Collectors.toUnmodifiableSet());

                    if (!isValidCriteriaFields(orgId, fieldIds, autoAssignmentRule.getAssignmentModule().source())) {
                        addConstraintViolationMessage(validatorContext, "i18n.auto.assignment.criteria.fields.invalid");
                        break;
                    } else {
                        final int criteriaSize = criteriaList.size();
                        final int lastIndex = criteriaSize - 1;

                        for (int criteriaIndex = 0; criteriaIndex < criteriaSize; criteriaIndex++) {
                            final AutoAssignmentCriteria autoAssignmentCriteria = criteriaList.get(criteriaIndex);
                            final Collection<AutoAssignmentCriteriaValue> criteriaValues = autoAssignmentCriteria.getCriteriaValues();

                            if (criteriaIndex < lastIndex && Objects.isNull(autoAssignmentCriteria.getConnector())) {
                                addConstraintViolationMessage(validatorContext, "i18n.auto.assignment.criteria.connector.required");
                                break;
                            }

                            if (autoAssignmentCriteria.getComparator().hasValue() && (Objects.isNull(criteriaValues) || criteriaValues.isEmpty())) {
                                addConstraintViolationMessage(validatorContext, "i18n.auto.assignment.criteria.value.required");
                                break;
                            }
                        }
                    }
                }
            }
        }

        final Collection<Long> territoryUserIds = new HashSet<>();
        final Collection<Long> userIds = new HashSet<>();
        Long territoryId = null;

        for (final AssignmentRuleUser assignmentRuleUser : autoAssignmentRule.getAssignmentRuleUsers()) {
            final Territory territory = assignmentRuleUser.getTerritory();

            userIds.add(assignmentRuleUser.getUserId());

            if (Objects.nonNull(territory)) {
                if (Objects.isNull(territoryId)) {
                    territoryId = territory.id();
                }
                territoryUserIds.add(assignmentRuleUser.getUserId());
            }
        }

        if (Objects.nonNull(territoryId) && !territoryManager.areUsersPartOfTerritory(orgId, territoryId, territoryUserIds)) {
            addConstraintViolationMessage(validatorContext, "i18n.auto.assignment.rule.territory.users.invalid");
        }

        if (!crmUserManager.exists(orgId, userIds)) {
            addConstraintViolationMessage(validatorContext, "i18n.auto.assignment.rule.users.invalid");
        }

        return !validatorContext.isErrorsFound();
    }

    /**
     * Checks if the given collection of field IDs matches the fields associated with a specific source.
     *
     * @param orgId    Represents the organization ID.
     * @param fieldIds Represents the collection of field IDs.
     * @param source   The name of the source.
     * @return True if all field IDs are associated with the specified source, false otherwise
     */
    private boolean isValidCriteriaFields(final Long orgId, final Collection<Long> fieldIds, final Source source) {
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, source.getName());

        return fields.stream().map(FieldInfo::id).collect(Collectors.toSet()).containsAll(fieldIds);
    }

    /**
     * Adds the constraint violation message to the validation context.
     *
     * @param validatorContext Refers the validator context.
     * @param message          Violation message.
     */
    protected void addConstraintViolationMessage(final ValidatorContext validatorContext, final String message) {
        validatorContext.setErrorsFound(true);
        validatorContext.getContext().buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

}
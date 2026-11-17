package com.tlc.crm.entity.service.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.builder.CriteriaFieldBuilder;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaOption;
import com.tlc.crm.entity.filter.api.models.DropdownDetails;
import com.tlc.crm.entity.internal.status.FilterErrorCodes;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.resolver.filter.FilterFieldResolver;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.filter.FilterFieldService;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.service.FieldSourceService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Implements the {@link FilterFieldService} provides functionality for accessing filter fields
 *
 * @author Kishore J
 * @version 1.0
 * @see FilterFieldService
 */
public class FilterFieldServiceImpl implements FilterFieldService {

    private final FilterFieldResolver filterFieldResolver;
    private final CriteriaFieldBuilder criteriaFieldBuilder;
    private final FieldService fieldService;
    private final FieldSourceService fieldSourceService;

    private FilterFieldServiceImpl(final FieldService fieldService) {
        filterFieldResolver = FilterFieldResolver.getInstance();
        criteriaFieldBuilder = CriteriaFieldBuilder.getInstance();
        this.fieldService = fieldService;
        fieldSourceService = FieldSourceService.getInstance();
    }

    private static class InstanceHolder {
        private static final FilterFieldServiceImpl INSTANCE = new FilterFieldServiceImpl(FieldService.getInstance());
    }

    private static class SecureInstanceHolder {
        private static final FilterFieldServiceImpl INSTANCE = new FilterFieldServiceImpl(FieldService.getSecureInstance());
    }

    /**
     * <p>
     * Gets the instance of  {@link FilterFieldServiceImpl}.
     * </p>
     *
     * @return {@link FilterFieldServiceImpl}.
     */
    public static FilterFieldServiceImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Gets the non-secure instance of {@link FilterFieldServiceImpl}.
     * This instance uses the non-secure {@link FieldService} and should be used in
     * system/internal operations (e.g., org initialization) where no user security
     * context is available, to avoid recursive field-permission lookups.
     * </p>
     *
     * @return non-secure {@link FilterFieldServiceImpl}.
     */
    public static FilterFieldServiceImpl getSecureInstance() {
        return SecureInstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organization id.
     * @param sourceId   Used for fetch the corresponding entities.
     * @param comparator
     * @return Collection of {@link CriteriaField}.
     */
    @Override
    public Collection<CriteriaField> getFieldsBySourceId(final Long orgId, final Long sourceId,
                                                         final Comparator<CriteriaField> comparator,
                                                         final boolean isDropdownDetailsNeeded) {
        return getFieldsBySourceIds(orgId, Set.of(sourceId), comparator, isDropdownDetailsNeeded);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organization id.
     * @param sourceIds Used for fetch the corresponding entities.
     * @return Collection of {@link CriteriaField}.
     */
    @Override
    public Collection<CriteriaField> getFieldsBySourceIds(final Long orgId, final Set<Long> sourceIds,
                                                          final Comparator<CriteriaField> comparator,
                                                          final boolean isDropdownDetailsNeeded) {
        final Set<CriteriaField> criteriaFields = new TreeSet<>(comparator);

        criteriaFields.addAll(getCriteriaOptions(sourceIds));
        criteriaFields.addAll(List.copyOf(getCriteriaFields(orgId, sourceIds)));

        if (criteriaFields.isEmpty()) {
            throw ErrorCode.get(FilterErrorCodes.FILTER_FIELDS_NOT_FOUND);
        }

        if (isDropdownDetailsNeeded) {
            loadDropdownDetails(criteriaFields);
        }

        return criteriaFields;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization id.
     * @param id      Used for fetch the corresponding entity.
     * @param isField Represents the given filter field id is field or option.
     * @return {@link CriteriaField}.
     */
    @Override
    public CriteriaField getFieldById(final Long orgId, final Long id, final boolean isField, final boolean isDropdownDetailsNeeded) {
        final CriteriaField criteriaField;

        if (isField) {
            final Collection<FieldInfo> fields = fieldService.getAllFields(orgId);
            final Optional<FieldInfo> field = fields.stream().filter(crmField -> Objects.equals(crmField.id(), id)).findFirst();

            criteriaField = field.map(criteriaFieldBuilder::build).orElse(null);
        } else {
            criteriaField = groupAllFilterOptionsById().get(id);
        }

        if (Objects.isNull(criteriaField)) {
            throw ErrorCode.get(FilterErrorCodes.FILTER_FIELD_NOT_FOUND);
        }

        if (isDropdownDetailsNeeded) {
            loadDropdownDetails(Set.of(criteriaField));
        }

        return criteriaField;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization id.
     * @param id      Filter field id.
     * @param isField Represents the required filter field is field or option.
     * @return If the Filter field details is exists it will return true, otherwise it will return false
     */
    @Override
    public boolean exists(final Long orgId, final Long id, final boolean isField) {
        return isField ? fieldService.getAllFields(orgId).stream().anyMatch(field -> Objects.equals(field.id(), id))
                : groupAllFilterOptionsById().containsKey(id);
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId        For fetch the corresponding {@link DropdownDetails} entity.
     * @param isAdditionalOption A flag that represents the additional options or values dropdown details.
     * @return {@link DropdownDetails} entity that holds the values of the dropdown details.
     */
    @Override
    public Collection<DropdownDetails> getDropdownDetailsByFieldTypeId(final Long fieldTypeId, final boolean isAdditionalOption) {
        final Map<Boolean, Collection<DropdownDetails>> dropdownDetails = filterFieldResolver.getTypeDropdownDetails(fieldTypeId);

        return Objects.isNull(dropdownDetails) ? null : dropdownDetails.get(isAdditionalOption);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organization id.
     * @param name    Represents the name of the field.
     * @param isField A flag that indicates whether the required filter field is field or option.
     * @return {@link CriteriaField} entity.
     */
    @Override
    public <T extends CriteriaField> Collection<T> getFieldsByName(final Long orgId, final String name, final boolean isField) {
        if (isField) {
            return (Collection<T>) fieldService.getAllFields(orgId).stream().filter(field -> !field.isPersistent() && field.aliasName().equalsIgnoreCase(name)).map(criteriaFieldBuilder::build).toList();
        } else {
            return (Collection<T>) filterFieldResolver.getAllFilterOptions().values().stream().filter(field ->
                    field.getAliasName().equalsIgnoreCase(name)).toList();
        }
    }

    /**
     * <p>
     * Gets the set of {@link CriteriaOption} for the given source ids.
     * </p>
     *
     * @param sourceIds Used to fetch the corresponding {@link CriteriaField} entities.
     */
    private Collection<CriteriaOption> getCriteriaOptions(final Set<Long> sourceIds) {
        final Collection<CriteriaOption> filterOptions = new ArrayList<>();
        final Map<Long, Map<Long, CriteriaOption>> criteriaOptions = filterFieldResolver.getCriteriaOptions(sourceIds);

        if (Objects.nonNull(criteriaOptions)) {
            for (final Map<Long, CriteriaOption> options : criteriaOptions.values()) {
                filterOptions.addAll(options.values());
            }
        }

        return filterOptions;
    }

    /**
     * <p>
     * Gets the set of {@link CriteriaField} for the given source ids.
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param sourceIds Used to fetch the corresponding {@link CriteriaField} entities.
     */
    private Collection<CriteriaField> getCriteriaFields(final Long orgId, final Set<Long> sourceIds) {
        final Collection<String> sourceNames = fieldSourceService.getSources().stream().filter(fieldSource ->
                sourceIds.contains(fieldSource.id())).map(FieldSource::getName).toList();

        return criteriaFieldBuilder.build(fieldService.getFieldsBySource(orgId, sourceNames));
    }

    /**
     * <p>
     * Loads the dropdown details into respective {@link CriteriaField} entity.
     * </p>
     *
     * @param criteriaFields Set of {@link CriteriaField} holds the details of the filter fields.
     */
    private void loadDropdownDetails(final Set<CriteriaField> criteriaFields) {

        for (final CriteriaField criteriaField : criteriaFields) {
            final Long fieldTypeId = criteriaField.getFieldType().id();

            if (criteriaField instanceof CriteriaOption) {
                final Long fieldId = criteriaField.getId();

                if (filterFieldResolver.checkOptionDropdownDetailExists(fieldId)) {
                    filterFieldResolver.getOptionDropdownDetails(fieldId).forEach((isAdditionalField, dropdownDetails) -> {
                        criteriaField.setHasValueDropdown(!isAdditionalField);
                        criteriaField.addAllDropdownDetails((Set<DropdownDetails>) dropdownDetails);
                    });
                }
            }

            if (filterFieldResolver.checkTypeDropdownDetailExists(fieldTypeId)) {
                filterFieldResolver.getTypeDropdownDetails(fieldTypeId).forEach((isAdditionalField, dropdownDetails) -> {
                    criteriaField.setHasValueDropdown(!isAdditionalField);
                    criteriaField.addAllDropdownDetails((Set<DropdownDetails>) dropdownDetails);
                });
            }
        }
    }

    /**
     * <p>
     * Groups all the {@link CriteriaOption} by id
     * </p>
     *
     * @return Map of criteria options.
     */
    private Map<Long, CriteriaOption> groupAllFilterOptionsById() {
        return filterFieldResolver.getAllFilterOptions();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param name   Represents the name of the field
     * @param source Represents the source
     * @return {@link CriteriaField} entity
     */
    @Override
    public <T extends CriteriaField> Collection<T> getFieldsByName(final long orgId, final String name, final String source) {
        final Collection<T> fields = (Collection<T>) fieldService.getAllFields(orgId).stream().filter(field ->
                !field.isPersistent() && field.aliasName().equalsIgnoreCase(name) && field.fieldSource().getName().equalsIgnoreCase(source)).map(criteriaFieldBuilder::build).toList();

        if (fields.isEmpty()) {
            return (Collection<T>) filterFieldResolver.getAllFilterOptions().values().stream().filter(field -> field.getAliasName().equalsIgnoreCase(name) &&
                    field.getSourceType().getSource().getName().equalsIgnoreCase(source)).toList();
        }

        return fields;
    }
}
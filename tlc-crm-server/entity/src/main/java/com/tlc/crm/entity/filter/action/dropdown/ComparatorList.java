package com.tlc.crm.entity.filter.action.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.filter.fields.ComparatorFields;
import com.tlc.crm.entity.service.filter.ComparatorService;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.i18n.I18nResolver;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Manages {@link Comparator} related dropdown actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@StaticDropdown(name = "comparatorList")
public class ComparatorList extends AbstractDropdownAction {

    private final ComparatorService comparatorService;
    private final FieldTypeService fieldTypeService;

    public ComparatorList() {
        comparatorService = ComparatorService.getInstance();
        fieldTypeService = FieldTypeService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested comparator values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final I18nResolver i18nResolver = dropdownRequest.i18nResolver();
        final JsonObject data = dropdownRequest.data();
        final String fieldTypeIdKey = ComparatorFields.FIELD_TYPE_ID.getName();
        final String fieldSubTypeIdKey = ComparatorFields.FIELD_SUB_TYPE_ID.getName();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final Long fieldTypeId = (Long) data.opt(fieldTypeIdKey);
        final Collection<Comparator> comparators = data.containsKey(fieldTypeIdKey) ? comparatorService.getComparatorsByType(fieldTypeId)
                : data.containsKey(fieldSubTypeIdKey) ? comparatorService.getComparatorsBySubType(data.getLong(fieldSubTypeIdKey)) : Collections.emptyList();
        final Collection<Comparator> comparatorList = applyCriteria(comparators, dropdownRequest.getCriteria(),
                List.of(comparator -> i18nResolver.get(comparator.getName())), Optional.empty());
        final int start = dropdownLimit.start();
        final long totalRecords = comparatorList.size();
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && start <= totalRecords) {
            final Collection<Comparator> orderedComparators = applyLimit(comparatorList.stream()
                    .sorted(java.util.Comparator.comparingInt(Comparator::getOrder)).toList(), start, dropdownLimit.limit());
            final boolean isMultiSelectType = Objects.nonNull(fieldTypeId) && fieldTypeService.get(fieldTypeId).storeType().isMultiSelectType();

            for (final Comparator comparator : orderedComparators) {
                final JsonObject jsonObject = Json.object();

                jsonObject.put(ComparatorFields.ID.getName(), comparator.getId());
                jsonObject.put(ComparatorFields.NAME.getName(), i18nResolver.get(comparator.getName()));
                jsonObject.put(ComparatorFields.IS_DEFAULT.getName(), comparator.isDefault());
                jsonObject.put(ComparatorFields.HAS_VALUE.getName(), comparator.isValueNeeded());
                jsonObject.put(ComparatorFields.IS_MULTI_VALUE_ALLOWED.getName(), comparator.isMultiValueAllowed()
                        || (comparator.isValueNeeded() && isMultiSelectType));
                jsonObject.put(ComparatorFields.ORDER.getName(), comparator.getOrder());

                optionContainer.addOption(jsonObject);
            }
        }

        return optionContainer;
    }
}
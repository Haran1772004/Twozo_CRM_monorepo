package com.tlc.crm.entity.filter.action.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.resolver.source.SourceNameResolver;
import com.tlc.crm.entity.service.filter.FilterSourceService;

import com.tlc.crm.fields.models.FieldSource;
import com.tlc.i18n.I18nResolver;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 * Manages date intervals related dropdown actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@StaticDropdown(name = "filterSourceList")
public class FilterSourceList extends AbstractDropdownAction {

    private final FilterSourceService filterSourceService;
    private final SourceNameResolver sourceNameResolver;

    public FilterSourceList() {
        filterSourceService = FilterSourceService.getInstance();
        sourceNameResolver = SourceNameResolver.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested source values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final String idKey = FieldKeys.ID.getName();
        final String nameKey = FieldKeys.NAME.getName();
        final String alternateNameKey = FieldKeys.ALTERNATE_NAME.getName();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final Long orgId = dropdownRequest.getOrgId();
        final Collection<FieldSource> fieldSources = applyCriteria(filterSourceService.getAll(),
                dropdownRequest.getCriteria(), List.of(FieldSource::getName), Optional.empty());
        final int start = dropdownLimit.start();
        final long totalRecords = fieldSources.size();
        final OptionContainer optionContainer = new OptionContainer(totalRecords);
        final I18nResolver resolver = dropdownRequest.i18nResolver();

        if (totalRecords > 0 && start <= totalRecords) {

            for (final FieldSource source : applyLimit(fieldSources, start, dropdownLimit.limit())) {
                final JsonObject jsonObject = Json.object();
                final String sourceName = source.getName();

                jsonObject.put(idKey, source.id());
                jsonObject.put(nameKey, sourceName);
                jsonObject.put(alternateNameKey, sourceNameResolver.resolve(orgId, sourceName, resolver));

                optionContainer.addOption(jsonObject);
            }
        }

        return optionContainer;
    }
}

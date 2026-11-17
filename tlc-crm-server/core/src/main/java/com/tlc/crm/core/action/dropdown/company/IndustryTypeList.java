package com.tlc.crm.core.action.dropdown.company;

import com.tlc.crm.core.field.contact.CompanyFields;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages the industry type dropdown field options.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@StaticDropdown(name = "industryTypeList")
public class IndustryTypeList extends AbstractDropdownAction {

    private final OptionService optionService;
    private final FieldService fieldService;

    public IndustryTypeList() {
        this.optionService = OptionService.getInstance();
        this.fieldService = FieldService.getSecureInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final FieldInfo field = dropdownRequest.getField();
        final Optional<FieldInfo> optionalField = Objects.isNull(field) ? fieldService.getDefaultFieldByAliasName(Source.COMPANY, CompanyFields.INDUSTRY_TYPE) : Optional.of(field);
        final OptionContainer optionContainer = new OptionContainer(OptionKeys.ID, OptionKeys.NAME);

        optionalField.ifPresent(timezoneField -> {
            final Collection<Option> options = optionService.getOptions(dropdownRequest.getOrgId(), timezoneField,
                    dropdownRequest.getCriteria(), dropdownRequest.i18nResolver(), dropdownRequest.getOrder());
            final int start = dropdownLimit.start();
            final long totalRecords = options.size();

            if (totalRecords > 0 && start <= totalRecords) {

                for (final Option option : applyLimit(options, start, dropdownLimit.limit())) {
                    optionContainer.addOption(option.id(), option.getOption());
                }
            }
            optionContainer.setTotalRecords(totalRecords);
        });

        return optionContainer;
    }

}

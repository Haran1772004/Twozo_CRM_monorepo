package com.tlc.crm.core.action.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Address;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.service.FieldTypeService;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages {@link Company} related dropdown actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Dropdown(name = "companyList", fieldAlias = "company")
public class CompanyList extends AbstractDropdownAction {

    private final CompanyManager companyManager;
    private final CompanyConverter companyConverter;
    private final FieldService fieldService;
    private final FieldTypeService fieldTypeService;

    public CompanyList() {
        companyManager = CompanyManager.getInstance();
        companyConverter = CompanyConverter.getInstance();
        fieldService = FieldService.getSecureInstance();
        fieldTypeService = FieldTypeService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final Long orgId = dropdownRequest.getOrgId();
        final JsonObject data = dropdownRequest.data();
        final JsonObject criteria = dropdownRequest.data().optJsonObject(FieldKeys.CRITERIA.getName());
        final String field = Objects.isNull(criteria) ? null : criteria.optString(FieldKeys.FIELD.getName(), null);
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final int start = dropdownLimit.start();
        final boolean isFieldNull = Objects.isNull(field);
        final long totalRecords;
        final Collection<Company> companies;

        if (isFieldNull) {
            final boolean includeAddress = data.optBoolean(CompanyFields.INCLUDE_ADDRESS);
            totalRecords = companyManager.getTotalCount(orgId, dropdownCriteria);
            companies = (totalRecords > 0 && start <= totalRecords)
                    ? companyManager.getCompanyDropdownList(orgId, includeAddress, dropdownLimit, dropdownCriteria) : Collections.emptyList();
        } else {
            final FieldInfo fieldInfo = fieldService.getField(orgId, Long.parseLong(field));
            final String subField = criteria.optString(FieldKeys.SUB_FIELD.getName(), null);
            final FieldSubType fieldSubType = Objects.isNull(subField) ? null : fieldTypeService.getSubType(Long.parseLong(subField));

            totalRecords = companyManager.getCompanyDropdownCount(orgId, fieldInfo, fieldSubType, dropdownCriteria);
            companies = (totalRecords > 0 && start <= totalRecords)
                    ? companyManager.getCompanyDropdownList(orgId, fieldInfo, fieldSubType, dropdownLimit, dropdownCriteria)
                    : Collections.emptyList();
        }
        final OptionContainer optionContainer = new OptionContainer(totalRecords);
        final String valueKey = FieldKeys.VALUE.getName();
        final String nameKey = FieldKeys.NAME.getName();
        final String thumbnailKey = FieldKeys.THUMBNAIL.getName();

        for (final Company company : companies) {
            final JsonObject companyJson = Json.object();

            companyJson.put(valueKey, company.id());
            companyJson.put(nameKey, company.getName());

            if (isFieldNull && Objects.nonNull(company.getThumbnail())) {
                companyJson.put(thumbnailKey, companyConverter.convertThumbnailToJson(company.id(),
                        Company.ENTITY_TYPE, company.getThumbnail()));
            }
            final Address address = company.getAddress();

            if (Objects.nonNull(address)) {
                companyJson.put(CompanyFields.ADDRESS, formatAddress(address));
            }

            optionContainer.addOption(companyJson);
        }

        return optionContainer;
    }

    /**
     * <p>
     * Formats the given {@link Address} into a comma-separated string,
     * excluding null or blank fields.
     * </p>
     *
     * @param address Represents the {@link Address} to format.
     * @return The formatted address string, or {@code null} if all fields are empty.
     */
    private String formatAddress(final Address address) {
        return Stream.of(address.getAddressLine1(), address.getAddressLine2(),
                        address.getCity(), address.getState(), address.getZipcode())
                .filter(Objects::nonNull).map(String::trim)
                .filter(companyAddress -> !companyAddress.isEmpty()).collect(Collectors.joining(", "));
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest The request object containing parameters for filtering or context.
     * @return An {@link OptionContainer} with the relevant dropdown options.
     */
    @Override
    public OptionContainer fetchZapierData(final DropdownRequest dropdownRequest) {
        return getOptionContainer(dropdownRequest);
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest The request object containing parameters for filtering or context.
     * @return An {@link OptionContainer} with the relevant dropdown options.
     */
    @Override
    public OptionContainer fetchRestAPIData(final DropdownRequest dropdownRequest) {
        return getOptionContainer(dropdownRequest);
    }

    /**
     * Retrieves an {@link OptionContainer} containing company options based on the provided {@link DropdownRequest}.
     *
     * @param dropdownRequest The request object containing parameters for filtering or context.
     * @return An {@link OptionContainer} with the relevant dropdown options.
     */
    private OptionContainer getOptionContainer(final DropdownRequest dropdownRequest) {
        final OptionContainer optionContainer = new OptionContainer(OptionKeys.ID, OptionKeys.NAME);
        final Long orgId = dropdownRequest.getOrgId();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final long totalRecords = companyManager.getTotalCount(orgId, dropdownCriteria);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<Company> companies = companyManager.getCompanyDropdownList(orgId, dropdownLimit, dropdownCriteria);

            for (final Company company : companies) {
                optionContainer.addOption(company.id(), company.getName());
            }
        }

        return optionContainer;
    }
}
package com.tlc.crm.core.action.dropdown.invoice;

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
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.service.FieldTypeService;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * <p>
 * Represents the invoice company list
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@Dropdown(name = "invoiceCompanyList", fieldAlias = "invoiceCompany")
public class InvoiceCompanyList extends AbstractDropdownAction {

    private final CompanyManager companyManager;
    private final CompanyConverter companyConverter;
    private final FieldService fieldService;
    private final FieldTypeService fieldTypeService;

    public InvoiceCompanyList() {
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
        final JsonObject criteria = data.optJsonObject(FieldKeys.CRITERIA.getName());
        final String field = Objects.isNull(criteria) ? null : criteria.optString(FieldKeys.FIELD.getName(), null);
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final int start = dropdownLimit.start();
        final boolean isFieldNull = Objects.isNull(field);
        final long totalRecords;
        final Collection<Company> companies;

        if (isFieldNull) {
            totalRecords = companyManager.getTotalCount(orgId, dropdownCriteria);
            companies = (totalRecords > 0 && start <= totalRecords)
                    ? companyManager.getCompanyDropdownList(orgId, true, dropdownLimit, dropdownCriteria)
                    : Collections.emptyList();
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

        for (final Company company : companies) {
            final JsonObject companyJson = companyConverter.convertCompanyWithAddress(false, company);

            if (isFieldNull && Objects.nonNull(company.getThumbnail())) {
                companyJson.put(CompanyFields.THUMBNAIL, companyConverter.convertThumbnailToJson(company.id(),
                        Company.ENTITY_TYPE, company.getThumbnail()));
            }
            optionContainer.addOption(companyJson);
        }

        return optionContainer;
    }
}
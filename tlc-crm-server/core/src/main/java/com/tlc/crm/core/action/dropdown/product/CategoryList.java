package com.tlc.crm.core.action.dropdown.product;

import com.tlc.crm.common.action.CrmRequest;

import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.field.product.ProductFields;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.OptionService;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Manages Category related dropdown actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Dropdown(name = "category", fieldAlias = "category")
public class CategoryList extends AbstractDropdownAction {

    private final OptionService optionService;
    private final FieldService fieldService;

    public CategoryList() {
        optionService = OptionService.getInstance();
        fieldService = FieldService.getSecureInstance();
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
        final String category = ProductFields.CATEGORY;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Product.ENTITY_TYPE, category);
        final Collection<Option> categories = optionService.getOptions(dropdownRequest.getOrgId(), fields.get(category),
                dropdownRequest.getCriteria(), dropdownRequest.i18nResolver(), dropdownRequest.getOrder());
        final int start = dropdownLimit.start();
        final long totalRecords = categories.size();
        final CrmRequest.ClientType clientType = dropdownRequest.getClientType();

        final boolean isZapierClient = Objects.equals(CrmRequest.ClientType.ZAPIER, clientType);
        final boolean isRestApiClient = Objects.equals(CrmRequest.ClientType.REST_API, clientType);

        final OptionContainer optionContainer = isZapierClient || isRestApiClient
                ? new OptionContainer(OptionKeys.ID, OptionKeys.NAME)
                : new OptionContainer();

        if (totalRecords > 0 && start <= totalRecords) {
            optionContainer.setTotalRecords(totalRecords);

            if (isZapierClient || isRestApiClient) {

                for (final Option option : applyLimit(categories, start, dropdownLimit.limit())) {
                    optionContainer.addOption(option.id(), option.getOption());
                }
            } else {

                for (final Option option : applyLimit(categories, start, dropdownLimit.limit())) {
                    optionContainer.addOption(option.id(), option.getOption(), option.getOrder(), option.isEditable(), option.isDefault());
                }
            }
        }

        return optionContainer;
    }
}

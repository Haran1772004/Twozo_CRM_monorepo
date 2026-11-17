package com.tlc.crm.core.action.dropdown;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.field.product.ProductFields;
import com.tlc.crm.core.internal.product.status.ProductErrorCodes;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.fields.service.settings.SourceSettingsService;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages {@link Product} related dropdown actions.
 *
 * @author Karthick.V
 * @version 1.0
 */
@Dropdown(name = "productList", fieldAlias = "product")
public class ProductList extends AbstractDropdownAction {

    private final ProductManager productManager;

    public ProductList() {
        productManager = ProductManager.getInstance();
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

        if (SourceSettingsService.getInstance().isSourceEnabled(orgId, Source.PRODUCT.getName())) {
            final JsonObject data = dropdownRequest.data();
            final boolean includeVariation = data.optBoolean(ProductFields.INCLUDE_VARIATION, false);
            final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
            final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
            final long totalRecords = productManager.getTotalCount(orgId, dropdownCriteria);
            final CrmRequest.ClientType clientType = dropdownRequest.getClientType();
            final OptionContainer optionContainer = CrmRequest.ClientType.ZAPIER == clientType || CrmRequest.ClientType.REST_API == clientType
                    ? new OptionContainer(OptionKeys.ID, OptionKeys.NAME, totalRecords)
                    : new OptionContainer(totalRecords);

            if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
                final Collection<Product> products = productManager.getProductDropdownList(orgId, dropdownLimit, dropdownCriteria);

                if (includeVariation) {
                    final String value = OptionKeys.VALUE;
                    final String name = OptionKeys.NAME;
                    final String includeVariationKey = ProductFields.INCLUDE_VARIATION;

                    final Set<Long> productIds = products.stream().map(Product::id).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
                    final Collection<Long> variationProductIds = productManager.getVariationProductIds(orgId, productIds);

                    for (final Product product : products) {
                        final JsonObject jsonObject = Json.object();

                        jsonObject.put(value, product.id());
                        jsonObject.put(name, product.getName());
                        jsonObject.put(includeVariationKey, variationProductIds.contains(product.id()));

                        optionContainer.addOption(jsonObject);
                    }
                } else {
                    for (final Product product : products) {
                        optionContainer.addOption(product.id(), product.getName());
                    }
                }
            }

            return optionContainer;
        } else {
            throw ErrorCode.get(ProductErrorCodes.PRODUCT_IS_DISABLED, "i18n.product.disabled");
        }
    }
}

package com.tlc.crm.core.action.dropdown.invoice;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.features.invoice.model.InvoiceType;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.i18n.I18nResolutionAdapter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 * Manages {@link InvoiceType} related dropdown actions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@StaticDropdown(name = "invoiceTypeList")
public class InvoiceTypeList extends AbstractDropdownAction {

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final I18nResolutionAdapter i18nResolutionAdapter = dropdownRequest.getI18NResolutionAdapter();
        final Collection<InvoiceType> invoiceTypes = applyCriteria(Arrays.asList(InvoiceType.values()),
                dropdownRequest.getCriteria(), List.of(invoiceType -> i18nResolutionAdapter.get(invoiceType.getI18nKey())), Optional.empty());
        final int start = dropdownLimit.start();
        final long totalRecords = invoiceTypes.size();
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && start <= totalRecords) {

            for (final InvoiceType invoiceType : applyLimit(invoiceTypes, start, dropdownLimit.limit())) {
                final JsonObject jsonObject = Json.object();

                jsonObject.put(OptionKeys.VALUE, invoiceType.getValue());
                jsonObject.put(OptionKeys.NAME, i18nResolutionAdapter.get(invoiceType.getI18nKey()));

                optionContainer.addOption(jsonObject);
            }
        }

        return optionContainer;
    }
}
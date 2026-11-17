package com.tlc.crm.core.action.dropdown.deal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.Dropdown;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Manages lost reason related dropdown actions.
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
@Dropdown(name = "lostReasonList", fieldAlias = "lostReason")
public class LostReasonList extends AbstractDropdownAction {
    private final FieldService fieldService;
    private final OptionService optionService;

    public LostReasonList() {
        fieldService = FieldService.getSecureInstance();
        optionService = OptionService.getInstance();
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
        final String lostReasonKey = DealFields.LOST_REASON;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, lostReasonKey);
        final Collection<Option> options = optionService.getOptions(dropdownRequest.getOrgId(),
                fields.get(lostReasonKey), dropdownRequest.getCriteria(), dropdownRequest.i18nResolver(), dropdownRequest.getOrder());
        final int start = dropdownLimit.start();
        final long totalRecords = options.size();
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && start <= totalRecords) {
            final CrmRequest.ClientType clientType = dropdownRequest.getClientType();
            final Collection<Option> optionList = applyLimit(options, start, dropdownLimit.limit());

            if (CrmRequest.ClientType.ZAPIER == clientType || CrmRequest.ClientType.REST_API == clientType) {
                optionList.forEach(option -> {
                    final JsonObject jsonObject = Json.object();

                    jsonObject.put(OptionKeys.ID, option.id());
                    jsonObject.put(OptionKeys.NAME, option.getOption());

                    optionContainer.addOption(jsonObject);
                });
            } else {
                optionList.forEach(option -> optionContainer.addOption(option.id(), option.getOption(), option.getOrder(), option.isEditable(), option.isDefault()));
            }
        }

        return optionContainer;
    }
}

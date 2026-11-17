package com.tlc.crm.core.notification.email.template.action.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.notification.email.template.api.model.EntityMailTemplate;
import com.tlc.crm.core.notification.email.template.api.TemplateManager;

import com.tlc.crm.entity.dropdown.DropdownAction;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.fields.option.OptionKeys;

import java.util.Collection;

/**
 * Represents a static dropdown for email template list.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@StaticDropdown(name = "emailTemplateList")
public class EmailTemplateList extends AbstractDropdownAction {

    private final TemplateManager templateManager;

    public EmailTemplateList() {
        this.templateManager = TemplateManager.getInstance();
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
        final Long userId = dropdownRequest.getUserId();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final long totalRecords = templateManager.totalCount(orgId, userId, dropdownCriteria);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<EntityMailTemplate> templates = templateManager.getTemplateDropdownList(orgId, userId, dropdownLimit, dropdownCriteria);

            for (final EntityMailTemplate template : templates) {
                final JsonObject option = Json.object();

                option.put(OptionKeys.NAME, template.getName());
                option.put(OptionKeys.VALUE, template.id());

                optionContainer.addOption(option);
            }
        }

        return optionContainer;
    }
}
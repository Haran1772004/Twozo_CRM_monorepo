package com.tlc.crm.core.notification.email.template.action.dropdown;

import com.tlc.crm.core.notification.email.template.api.model.TemplateTag;
import com.tlc.crm.core.notification.email.template.api.TemplateTagManager;
import com.tlc.crm.entity.dropdown.DropdownAction;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;

import java.util.Collection;


/**
 * <p>
 * Manages {@link TemplateTag} related dropdown actions.
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@StaticDropdown(name = "templateTagList")
public class TemplateTagList extends AbstractDropdownAction {

    private final TemplateTagManager templateTagManager;

    public TemplateTagList() {
        templateTagManager = TemplateTagManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the tag values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest request) {
        final Long orgId = request.getOrgId();
        final DropdownLimit dropdownLimit = request.getLimit();
        final DropdownCriteria dropdownCriteria = request.getCriteria();
        final long totalRecords = templateTagManager.getTotalCount(orgId, dropdownCriteria);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<TemplateTag> templateTags = templateTagManager.getTagDropDownList(orgId, dropdownLimit,
                    dropdownCriteria);

            for (final TemplateTag templateTag : templateTags) {
                optionContainer.addOption(templateTag.id(), templateTag.getName());
            }
        }

        return optionContainer;
    }
}

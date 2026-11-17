package com.tlc.crm.entity.features.action.dropdown;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.features.api.tag.manager.TagManager;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;

import java.util.Collection;

/**
 * <p>
 * Manages {@link Tag} related dropdown actions.
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
@StaticDropdown(name = "tagList")
public class TagList extends AbstractDropdownAction {

    private final TagManager tagManager;

    public TagList() {
        tagManager = TagManager.getInstance();
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
        final long totalRecords = tagManager.getTotalCount(orgId, dropdownCriteria);
        final CrmRequest.ClientType clientType = request.getClientType();
        final OptionContainer optionContainer = CrmRequest.ClientType.ZAPIER == clientType || CrmRequest.ClientType.REST_API == clientType
                ? new OptionContainer(OptionKeys.ID, OptionKeys.NAME, totalRecords)
                : new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<Tag> tags = tagManager.getTagList(orgId, dropdownLimit, dropdownCriteria);

            for (final Tag tag : tags) {
                final JsonObject option = Json.object();

                option.put(OptionKeys.VALUE, tag.id());
                option.put(OptionKeys.NAME, tag.getName());
                option.put(OptionKeys.COLOR, tag.getColor().getName());
                optionContainer.addOption(option);
            }
        }

        return optionContainer;
    }
}
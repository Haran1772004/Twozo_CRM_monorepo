package com.tlc.crm.core.action.dropdown.segment;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.segment.fields.SegmentFields;
import com.tlc.crm.core.segment.manager.SegmentFolderManager;
import com.tlc.crm.core.segment.models.SegmentFilterGroup;
import com.tlc.crm.core.segment.models.SegmentFolder;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.fields.option.OptionKeys;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * <p>
 * Manages {@link SegmentFolder} related dropdown actions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@StaticDropdown(name = "segmentFolderList")
public class SegmentFolderList extends AbstractDropdownAction {

    private final SegmentFolderManager segmentFolderManager;

    public SegmentFolderList() {
        segmentFolderManager = SegmentFolderManager.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final JsonObject data = dropdownRequest.data();
        final Long orgId = dropdownRequest.getOrgId();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final Short groupId = data.containsKey(SegmentFields.GROUP_ID.getName()) ? data.getShort(SegmentFields.GROUP_ID.getName()) : null;
        final SegmentFilterGroup segmentFilterGroup = Objects.isNull(groupId) ? null : SegmentFilterGroup.get(groupId);
        final long totalRecords = segmentFolderManager.getDropdownCount(orgId, segmentFilterGroup, dropdownCriteria);

        final Collection<SegmentFolder> folders = totalRecords > 0 && dropdownLimit.start() <= totalRecords
                ? segmentFolderManager.getDropdownList(orgId, segmentFilterGroup, dropdownLimit, dropdownCriteria)
                : Collections.emptyList();

        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        for (final SegmentFolder folder : folders) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(OptionKeys.VALUE, folder.id());
            jsonObject.put(OptionKeys.NAME, folder.getName());

            optionContainer.addOption(jsonObject);
        }

        return optionContainer;
    }
}
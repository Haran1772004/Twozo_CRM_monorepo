package com.tlc.crm.core.action.dropdown.campaign;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.campaign.fields.CampaignFields;
import com.tlc.crm.core.campaign.manager.CampaignFolderManager;
import com.tlc.crm.core.campaign.model.CampaignFilterGroup;
import com.tlc.crm.core.campaign.model.CampaignFolder;
import com.tlc.crm.core.campaign.model.CampaignCategory;

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
 * Manages {@link CampaignFolder} related dropdown actions.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@StaticDropdown(name = "campaignFolderList")
public class CampaignFolderList extends AbstractDropdownAction {

    private final CampaignFolderManager campaignFolderManager;

    /**
     * <p>
     * Constructs the {@link CampaignFolderList}.
     * </p>
     */
    public CampaignFolderList() {
        campaignFolderManager = CampaignFolderManager.getInstance();
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
        final CampaignCategory category = CampaignCategory.get(data.getShort(CampaignFields.CATEGORY));
        final Short groupId = data.containsKey(CampaignFields.GROUP_ID)
                ? data.getShort(CampaignFields.GROUP_ID) : null;
        final CampaignFilterGroup campaignFilterGroup = Objects.isNull(groupId) ? null : CampaignFilterGroup.get(groupId);
        final long totalRecords = campaignFolderManager.getDropdownCount(orgId, category, campaignFilterGroup, dropdownCriteria);
        final Collection<CampaignFolder> folders = totalRecords > 0 && dropdownLimit.start() <= totalRecords
                ? campaignFolderManager.getDropdownList(orgId, category, campaignFilterGroup, dropdownLimit, dropdownCriteria)
                : Collections.emptyList();
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        for (final CampaignFolder folder : folders) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(OptionKeys.VALUE, folder.id());
            jsonObject.put(OptionKeys.NAME, folder.getName());

            optionContainer.addOption(jsonObject);
        }

        return optionContainer;
    }
}

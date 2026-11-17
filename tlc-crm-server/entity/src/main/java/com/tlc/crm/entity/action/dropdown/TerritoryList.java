package com.tlc.crm.entity.action.dropdown;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;

/**
 * <p>
 * Manages the {@link Territory} related drop-down actions
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@StaticDropdown(name = "territoryList")
public class TerritoryList extends AbstractDropdownAction {

    private final TerritoryManager territoryManager;

    public TerritoryList() {
        territoryManager = TerritoryManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final long orgId = dropdownRequest.getOrgId();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final Criteria criteria = dropdownRequest.getCriteria().build(Table.get(OMCRMTERRITORY.TABLE).getColumn(OMCRMTERRITORY.NAME));
        final long totalRecords = territoryManager.getTotalCount(orgId, criteria);
        final CrmRequest.ClientType clientType = dropdownRequest.getClientType();
        final OptionContainer optionContainer = CrmRequest.ClientType.ZAPIER == clientType || CrmRequest.ClientType.REST_API == clientType
                ? new OptionContainer(OptionKeys.ID, OptionKeys.NAME, totalRecords)
                : new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<Territory> territories = territoryManager.getTerritoryDropDownList(orgId, dropdownLimit.getLimitClause(),
                    criteria);

            for (final Territory territory : territories) {
                optionContainer.addOption(territory.id(), territory.getName());
            }
        }

        return optionContainer;
    }
}

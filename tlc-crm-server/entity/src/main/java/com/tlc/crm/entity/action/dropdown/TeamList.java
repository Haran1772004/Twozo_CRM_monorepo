package com.tlc.crm.entity.action.dropdown;

import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.sql.resource.OMCRMTEAM;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;

/**
 * <p>
 * Manages the {@link Team} related drop-down actions
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@StaticDropdown(name = "teamList")
public class TeamList extends AbstractDropdownAction {

    private final TeamManager teamManager;

    public TeamList() {
        teamManager = TeamManager.getInstance();
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
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final Criteria criteria = dropdownRequest.getCriteria().build(Table.get(OMCRMTEAM.TABLE).getColumn(OMCRMTEAM.NAME));
        final long totalRecords = teamManager.getTotalCount(orgId, criteria);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<Team> teams = teamManager.getTeamDropDownList(orgId, dropdownLimit.getLimitClause(), criteria);

            for (final Team team : teams) {
                optionContainer.addOption(team.id(), team.getName());
            }
        }

        return optionContainer;
    }
}

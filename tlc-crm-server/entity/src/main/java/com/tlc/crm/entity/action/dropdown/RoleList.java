package com.tlc.crm.entity.action.dropdown;

import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.security.sql.resource.OMCRMROLE;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;

/**
 * <p>
 * Manages the {@link CrmRole} related drop-down actions
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@StaticDropdown(name = "roleList")
public class RoleList extends AbstractDropdownAction {

    private final RoleManager roleManager;

    public RoleList() {
        roleManager = RoleManager.getInstance();
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
        final Criteria criteria = dropdownRequest.getCriteria().build(Table.get(OMCRMROLE.TABLE).getColumn(OMCRMROLE.NAME));
        final long totalRecords = roleManager.getTotalCount(orgId, criteria);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<CrmRole> roles = roleManager.getRoleDropDownList(orgId, dropdownLimit.getLimitClause(), criteria);

            for (final CrmRole crmRole : roles) {
                optionContainer.addOption(crmRole.id(), crmRole.getName());
            }
        }

        return optionContainer;
    }
}

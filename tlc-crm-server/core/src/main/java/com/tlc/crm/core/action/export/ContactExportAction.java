package com.tlc.crm.core.action.export;

import com.tlc.crm.common.export.action.DataExporter;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.SubCategory;

import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.export.action.AbstractEntityDataExportAction;

/**
 * Exports the contact information.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@DataExporter(name = "contact")
public class ContactExportAction extends AbstractEntityDataExportAction {

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organisation id.
     * @param roleId Represents the role id.
     * @return Holds the permission details of the data export.
     */
    @Override
    protected RolePermission getPermission(final Long orgId, final Long roleId) {
        return rolePermissionService.getPermission(orgId, roleId, Category.Type.SOURCES, SubCategory.Type.CONTACTS, Action.Type.EXPORT_RECORDS);
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Provider}.
     */
    @Override
    protected Provider getProvider() {
        return Provider.CONTACT_LIST;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link ListType}.
     */
    @Override
    protected ListType getListType() {
        return ListType.CONTACT_LIST;
    }
}
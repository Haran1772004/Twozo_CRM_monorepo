package com.tlc.crm.common.list;

/**
 * <p>
 * Provides the list structure information into the container.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class ListStructureContainer {

    private Long orgId;
    private Long userId;
    private Long providerId;
    private ListStructure listStructure;

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(final Long providerId) {
        this.providerId = providerId;
    }

    public ListStructure getListStructure() {
        return listStructure;
    }

    public void setListStructure(final ListStructure listStructure) {
        this.listStructure = listStructure;
    }
}

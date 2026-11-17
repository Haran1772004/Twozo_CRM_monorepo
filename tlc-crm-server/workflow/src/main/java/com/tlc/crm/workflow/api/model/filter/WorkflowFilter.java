package com.tlc.crm.workflow.api.model.filter;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * A model that represents the {@link WorkflowFilter}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class WorkflowFilter {

    private Long id;
    private long userId;
    private long orgId;
    private Type type;
    private String searchText;
    private Map<FilterGroup, Collection<Long>> criteria;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final long userId) {
        this.userId = userId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(final long orgId) {
        this.orgId = orgId;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(final String searchText) {
        this.searchText = searchText;
    }

    public Map<FilterGroup, Collection<Long>> getCriteria() {
        return criteria;
    }

    public void setCriteria(final Map<FilterGroup, Collection<Long>> criteria) {
        this.criteria = criteria;
    }
}

package com.tlc.crm.common.list.sort.models;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.validator.TlcModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * A model that represents the sort config data.
 * </p>
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public class SortConfig implements TlcModel {

    private Long id;
    private final Long orgId;
    private final Long userId;
    private final Long sourceId;
    private SortViewType sortViewType;
    private Collection<SortCriteria> sortCriteriaList;
    private JsonArray configJson;
    private String config;

    public SortConfig(final Long orgId, final Long userId, final Long sourceId) {
        this.orgId = Objects.requireNonNull(orgId);
        this.userId = Objects.requireNonNull(userId);
        this.sourceId = sourceId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public SortViewType getSortProvider() {
        return sortViewType;
    }

    public void setSortProvider(final SortViewType sortViewType) {
        this.sortViewType = sortViewType;
    }

    public Collection<SortCriteria> getSortCriteriaList() {
        return sortCriteriaList;
    }

    public void setSortCriteriaList(final Collection<SortCriteria> sortCriteriaList) {
        this.sortCriteriaList = sortCriteriaList;
    }

    public void addSortCriteria(final SortCriteria sortCriteria) {
        this.sortCriteriaList = null == this.sortCriteriaList ? new ArrayList<>() : this.sortCriteriaList;

        this.sortCriteriaList.add(sortCriteria);
    }

    public JsonArray getConfigJson() {
        return configJson;
    }

    public void setConfig(final String config) {
        this.config = config;
        this.configJson = Objects.nonNull(config) ? Json.array(config) : Json.array();
    }

    public String getConfig() {
        return config;
    }

    public Long getSourceId() {
        return sourceId;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }
}
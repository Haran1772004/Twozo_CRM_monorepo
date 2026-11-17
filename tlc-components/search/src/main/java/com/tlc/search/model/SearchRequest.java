package com.tlc.search.model;

import java.util.Objects;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * <p>
 * Model depicting the search request related fields.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class SearchRequest implements IndexRequestModel {

    private FilterClause filterClause;
    private List<String> selectedFields;
    private List<String> excludedFields;
    private Collection<SortClause> sortClauses;
    private RangeClause rangeClause;
    private Collection<ScriptInfo> scripts;
    private int totalCount;

    public SearchRequest() {
    }

    /**
     * <p>
     * Includes the field to be retrieved from index.
     * </p>
     *
     * @param field Refers to name of the field.
     */
    public void includeField(final String field) {
        if (Objects.isNull(selectedFields)) {
            selectedFields = new ArrayList<>();
        }
        selectedFields.add(field);
    }

    /**
     * <p>
     * Includes the fields to be retrieved from index.
     * </p>
     *
     * @param fields Refers to name of the fields.
     */
    public void includeFields(final Collection<String> fields) {
        if (Objects.isNull(this.selectedFields)) {
            this.selectedFields = new ArrayList<>();
        }

        if (Objects.nonNull(fields) && !fields.isEmpty()) {
            this.selectedFields.addAll(fields);
        }
    }

    /**
     * <p>
     * Excludes the field from being retrieved from index.
     * </p>
     *
     * @param field Refers to name of the field.
     */
    public void excludeField(final String field) {
        if (Objects.isNull(excludedFields)) {
            excludedFields = new ArrayList<>();
        }
        excludedFields.add(field);
    }

    /**
     * <p>
     * Excludes the fields from being retrieved from index.
     * </p>
     *
     * @param fields Refers to name of the fields.
     */
    public void excludeFields(final Collection<String> fields) {
        if (Objects.isNull(this.excludedFields)) {
            this.excludedFields = new ArrayList<>();
        }

        if (Objects.nonNull(fields) && !fields.isEmpty()) {
            this.excludedFields.addAll(fields);
        }
    }

    /**
     * <p>
     * Adds {@link SortClause}.
     * </p>
     *
     * @param sortClause {@link SortClause}.
     */
    public void addSortClause(final SortClause sortClause) {
        if (Objects.isNull(sortClauses)) {
            sortClauses = new HashSet<>();
        }
        sortClauses.add(sortClause);
    }

    /**
     * <p>
     * Sets collection {@link SortClause}.
     * </p>
     *
     * @param sortClauses collection {@link SortClause}.
     */
    public void setSortClauses(final Collection<SortClause> sortClauses) {
        if (Objects.isNull(this.sortClauses)) {
            this.sortClauses = new HashSet<>();
        }
        this.sortClauses.addAll(sortClauses);
    }

    /**
     * <p>
     * Sets {@link RangeClause}.
     * </p>
     *
     * @param rangeClause {@link RangeClause}.
     */
    public void setRangeClause(final RangeClause rangeClause) {
        this.rangeClause = rangeClause;
    }

    /**
     * <p>
     * Gets the fields that are to be retrieved from index.
     * </p>
     *
     * @return the selected fields that are to be retrieved from index.
     */
    public List<String> getSelectFields() {
        return selectedFields;
    }

    /**
     * <p>
     * Gets the fields that are to be excluded when retrieving from index.
     * </p>
     *
     * @return the fields that are to be excluded when retrieving from index.
     */
    public List<String> getExcludedFields() {
        return excludedFields;
    }

    /**
     * <p>
     * Gets collection {@link SortClause}.
     * </p>
     *
     * @return collection {@link SortClause}.
     */
    public Collection<SortClause> getSortClauses() {
        return sortClauses;
    }

    /**
     * <p>
     * Gets {@link RangeClause}.
     * </p>
     *
     * @return Gets {@link RangeClause}.
     */
    public RangeClause getRangeClause() {
        return rangeClause;
    }

    /**
     * <p>
     * Sets collection {@link ScriptInfo}.
     * </p>
     *
     * @param scripts collection {@link ScriptInfo}.
     */
    public void setScripts(final Collection<ScriptInfo> scripts) {
        this.scripts = scripts;
    }

    /**
     * <p>
     * Adds {@link ScriptInfo}.
     * </p>
     *
     * @param scriptInfo {@link ScriptInfo}.
     */
    public void addScript(final ScriptInfo scriptInfo) {

        if (Objects.isNull(scripts)) {
            scripts = new ArrayList<>();
        }
        scripts.add(scriptInfo);
    }

    /**
     * <p>
     * Gets collection {@link ScriptInfo}.
     * </p>
     *
     * @return collection {@link ScriptInfo}.
     */
    public Collection<ScriptInfo> getScripts() {
        return Objects.isNull(scripts) ? Collections.emptyList() : Collections.unmodifiableCollection(scripts);
    }

    /**
     * <p>
     * Gets the total doc count.
     * </p>
     *
     * @return total doc count
     */
    public int totalDocCount() {
        return totalCount;
    }

    /**
     * <p>
     * Sets the total doc count.
     * </p>
     *
     * @param totalCount the total doc count.
     */
    public void setTotalDocCount(final int totalCount) {
        this.totalCount = totalCount;
    }

    /**
     * <p>
     * Sets {@link FilterClause}.
     * </p>
     *
     * @param filterClause{@link FilterClause}.
     */
    public void setFilter(final FilterClause filterClause) {
        this.filterClause = filterClause;
    }

    /**
     * <p>
     * Gets {@link FilterClause}.
     * </p>
     *
     * @return Gets {@link FilterClause}.
     */
    public FilterClause getFilter() {
        return filterClause;
    }
}
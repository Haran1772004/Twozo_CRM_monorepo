package com.tlc.crm.entity.features.api.tag.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.color.Color;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.features.internal.status.TagErrorCodes;
import com.tlc.crm.entity.sql.resource.OMTTAG;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.OrderByClause.OrderType;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the {@link Tag} related DB actions
 *
 * @author EswariNivethaVU
 * @version 1.0
 */
public class TagManager extends AbstractConfigManager<Tag> {

    private UserService userService;

    private TagManager() {
        userService = UserService.getInstance();
    }

    private static class Instance {
        private static final TagManager INSTANCE = new TagManager();
    }

    /**
     * <p>
     * Gets instance of Tag Manager.
     * </p>
     *
     * @return {@link Instance of TagManager}
     */
    public static TagManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Tag} Table
     */
    @Override
    public Table getTable() {
        return Table.get(OMTTAG.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the Organization id
     * @param ids   Represents the {@link Collection} of tag id
     * @return Collection of {@link Tag}
     */
    @Override
    public Collection<Tag> get(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();
        final Stream<Row> rows = orgDataStore(orgId).get(table, ids).getRows(table);

        if (Objects.isNull(rows)) {
            throw ErrorCode.get(TagErrorCodes.TAG_NOT_FOUND, "i18n.tag.not.found");
        } else {
            return rows.map(this::convertRowToModel).toList();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param tag An entity that holds the tag attributes which have to be saved into database
     * @return {@link Tag}
     */
    @Override
    public Tag create(final Tag tag) {
        final DataContainer targetContainer = DataContainer.create();

        loadTagIntoContainer(true, tag, null, targetContainer);
        orgDataStore(tag.orgId()).commitChanges(targetContainer);

        return tag;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the Organization id
     * @param tags  Collection of tag that have to be updated
     * @return Updated status
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<Tag> tags) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer entityContainer = orgDataStore.get(getTable(), tags.stream().map(Tag::id)
                .filter(Objects::nonNull).toList());

        tags.forEach(tag -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadTagIntoContainer(false, tag, entityContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(tag.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Tag update failed Org ID : {} ID : {} reason : {}", orgId, tag.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), CommonErrorCodes.UPDATE_ACTION_FAILED.name(), tag.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        return processOutcome;
    }

    /**
     * <p>
     * Loads the tag details into the data container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param tag             An entity that holds the tag attributes
     * @param entityContainer Holds the user defined and system field values of existing tag
     * @param dataContainer   Holds the updated information of user defined and system field values
     */
    public void loadTagIntoContainer(final boolean isCreateEvent, final Tag tag, final DataContainer entityContainer,
                                     final DataContainer dataContainer) {
        final Table tagTable = getTable();
        final Long tagId = tag.id();
        final String tagName = (String) tag.getName();
        final Row tagRow = isCreateEvent ? new Row(tagTable) : entityContainer.getRow(tagTable, tagId);

        if (!isCreateEvent && Objects.isNull(tagRow)) {
            throw ErrorCode.get(TagErrorCodes.TAG_NOT_FOUND, "i18n.tag.not.found");
        }

        final long currentTime = Instant.now().toEpochMilli();

        if (isCreateEvent) {
            tagRow.setOrgId(tag.orgId());
            tag.setId(tagRow.getPKValue());
            tagRow.set(OMTTAG.CREATED_TIME, currentTime);
            tagRow.set(OMTTAG.CREATED_BY, tag.getCreatedBy().id());
            tagRow.set(OMTTAG.COLOR, Objects.requireNonNullElse(tag.getColor(), Tag.getDefaultColor()).id());
            tagRow.set(OMTTAG.NAME, tagName);
        } else {
            if (Objects.nonNull(tag.getColor())) {
                tagRow.set(OMTTAG.COLOR, tag.getColor().id());
            }

            if (Objects.nonNull(tagName)) {
                tagRow.set(OMTTAG.NAME, tagName);
            }
        }
        tagRow.set(OMTTAG.UPDATED_TIME, currentTime);
        dataContainer.updateRow(tagRow);
    }

    /**
     * <p>
     * Fetches the {@link Tag} list for the dropdown actions.
     * </p>
     *
     * @param orgId    Represents the Organization id.
     * @param limit    Which holds the limit configuration.
     * @param criteria Which configure the criteria.
     * @return {@link Collection} of tags.
     */
    public Collection<Tag> getTagList(final Long orgId, final DropdownLimit limit,
                                      final DropdownCriteria criteria) {
        final Table tagTable = getTable();
        final Column tagNameColumn = tagTable.getColumn(OMTTAG.NAME);
        final Column colorColumn = tagTable.getColumn(OMTTAG.COLOR);
        final SelectQuery selectQuery = SelectQuery.get(tagTable);

        selectQuery.addSelectClause(tagNameColumn);
        selectQuery.addSelectClause(colorColumn);
        selectQuery.setWhereClause(new WhereClause(criteria.build(tagNameColumn)));
        selectQuery.setLimitClause(limit.getLimitClause());
        selectQuery.addOrderByClause(new OrderByClause(tagTable.getPKColumn()));

        final Stream<Row> rows = orgDataStore(orgId).get(selectQuery).getRows(tagTable);

        return rows.filter(Objects::nonNull).map(this::convertRowToModel).toList();
    }

    /**
     * <p>
     * Fetches the tag counts
     * </p>
     *
     * @param orgId Represents an organization id
     * @return The tag count
     */
    public Long getTagCount(final Long orgId, final String searchText) {
        final CountQuery countQuery = CountQuery.get(getTable());

        if (Objects.nonNull(searchText) && !searchText.isBlank()) {
            countQuery.setWhereClause(new WhereClause(Criteria.contains(getTable().getColumn(OMTTAG.NAME), searchText)));
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <>
     * Fetches the tag list for the given organization
     * </>
     *
     * @param orgId    Represents an organization id
     * @param start    Represents the start value
     * @param limit    Represents the limit value
     * @param sortType Represents the sort type
     * @param field    Represents the field to sort by
     * @return The {@link Collection} of tag
     */
    public Collection<Tag> getTagList(final Long orgId, final int start, final int limit, final String field,
                                      final SortType sortType, final String searchText) {
        final Table tagTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(tagTable);
        final Column nameColumn = tagTable.getColumn(OMTTAG.NAME);
        final Column createdByColumn = tagTable.getColumn(OMTTAG.CREATED_BY);
        final Column createdTimeColumn = tagTable.getColumn(OMTTAG.CREATED_TIME);
        final Column lastUsedColumn = tagTable.getColumn(OMTTAG.LAST_USED);
        final OrderType orderType = SortType.ASCENDING == sortType ? OrderType.ASCENDING : OrderType.DESCENDING;

        selectQuery.addSelectClause(List.of(nameColumn, createdByColumn, createdTimeColumn, lastUsedColumn,
                tagTable.getColumn(OMTTAG.COLOR), tagTable.getColumn(OMTTAG.ORG_ID)));
        selectQuery.setLimitClause(new LimitClause(start, limit));

        if (Objects.nonNull(searchText) && !searchText.isBlank()) {
            selectQuery.setWhereClause(new WhereClause(Criteria.contains(nameColumn, searchText)));
        }

        switch (field) {
            case "name" -> selectQuery.addOrderByClause(new OrderByClause(nameColumn, orderType));
            case "createdBy" -> selectQuery.addOrderByClause(new OrderByClause(createdByColumn, orderType));
            case "lastUsed" -> {
                final OrderByClause orderByClause = new OrderByClause(lastUsedColumn, orderType);

                if (SortType.ASCENDING == sortType) {
                    orderByClause.setNullFirst(false);
                }
                selectQuery.addOrderByClause(orderByClause);
            }
            default -> selectQuery.addOrderByClause(new OrderByClause(createdTimeColumn, orderType));
        }
        selectQuery.addOrderByClause(new OrderByClause(tagTable.getPKColumn(), OrderType.DESCENDING));
        final Stream<Row> rows = orgDataStore(orgId).get(selectQuery).getRows(tagTable);

        return rows.map(this::convertRowToModel).toList();
    }

    /**
     * Converts the given row into a {@link Tag} and sets the specified order.
     *
     * @param row Tag row to be converted
     * @param order Order of the tag
     * @return {@link Tag}
     */
    public Tag convertRowToModel(final Row row, final Short order) {
        final Tag tag = convertRowToModel(row);

        tag.setOrder(order);

        return tag;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Tag> partialGet(final Long orgId, final Collection<Long> ids) {

        if (ids.isEmpty()) {
            return List.of();
        } else {
            final Table table = getTable();
            final SelectQuery selectQuery = SelectQuery.get(table);

            selectQuery.addSelectClause(table.getColumn(OMTTAG.NAME));
            selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), ids)));

            return orgDataStore(orgId).get(selectQuery).getRows(table).map(row -> {
                final Tag tag = new Tag();

                tag.setId(row.getPKValue());
                tag.setName(row.get(OMTTAG.NAME));
                return tag;
            }).toList();
        }
    }

    /**
     * <p>
     * Checks whether the given tag name exists in the organization
     * </p>
     *
     * @param orgId   Represents the organisation id
     * @param tagName Represents the tag name
     * @return True if the given tag name already exists, false otherwise
     */
    public boolean isTagNameExists(final Long orgId, final String tagName) {
        final Table tagTable = getTable();
        final CountQuery countQuery = CountQuery.get(tagTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(tagTable.getColumn(OMTTAG.NAME), tagName)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * Get all tags of the specified organization
     *
     * @param orgId Represents the organization ID
     * @return {@link Collection} of all tags of the specified organization
     */
    public Collection<Tag> get(final long orgId) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.setWhereClause(new WhereClause(Criteria.notEq(table.getPKColumn(), null)));
        return orgDataStore(orgId).get(selectQuery).getRows(table).map(this::convertRowToModel).toList();
    }

    /**
     * Gets the tag by the provided name
     *
     * @param orgId   Represents the organization ID
     * @param tagName Represents the tag name
     * @return the {@link Tag}
     */
    public Optional<Tag> get(final long orgId, final String tagName) {
        final Table tagTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(tagTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(tagTable.getColumn(OMTTAG.NAME), tagName)));
        selectQuery.addOrderByClause(new OrderByClause(tagTable.getPKColumn()));
        selectQuery.setLimitClause(new LimitClause(1));

        final Row row = orgDataStore(orgId).get(selectQuery).getRow(tagTable);

        if (Objects.isNull(row)) {
            return Optional.empty();
        }
        return Optional.of(convertRowToModel(row));
    }

    /**
     * <p>
     * Retrieves the total tag count
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param dropdownCriteria Represents a dropdown criteria
     * @return The count of entities
     */
    public long getTotalCount(final Long orgId, final DropdownCriteria dropdownCriteria) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        if (Objects.nonNull(dropdownCriteria)) {
            countQuery.setWhereClause(new WhereClause(dropdownCriteria.build(table.getColumn(OMTTAG.NAME))));
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Get tag names mapped by their ids
     *
     * @param orgId Represents the Organization id
     * @param ids   Represents the {@link Collection} of tag id
     * @return Map of tag id to tag name
     */
    public Map<Long, String> getNamesByIds(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();

        return orgDataStore(orgId).get(table, ids, List.of(table.getPKColumn(), table.getColumn(OMTTAG.NAME))).getRows(table)
                .filter(Objects::nonNull).collect(Collectors.toMap(Row::getPKValue, row -> (String) row.get(OMTTAG.NAME)));
    }

    /**
     * <p>
     * Converts row into {@link Tag}
     * </p>
     *
     * @param row Tag row to be converted
     * @return {@link Tag}
     */
    private Tag convertRowToModel(final Row row) {
        final Tag tag = new Tag();
        final Long createdBy = row.get(OMTTAG.CREATED_BY);
        final Long orgId = row.getOrgId();
        final Short colorId = row.get(OMTTAG.COLOR);

        tag.setId(row.getPKValue());
        tag.setName(row.get(OMTTAG.NAME));
        tag.setOrgId(orgId);
        tag.setCreatedTime(row.get(OMTTAG.CREATED_TIME));
        tag.setUpdatedTime(row.get(OMTTAG.UPDATED_TIME));
        tag.setLastUsed(row.get(OMTTAG.LAST_USED));
        tag.setColor(Color.get(colorId.shortValue()));

        if (Objects.nonNull(createdBy)) {
            tag.setCreatedBy(userService.getUser(orgId, createdBy));
        }

        return tag;
    }

}
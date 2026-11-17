package com.tlc.crm.core.notification.email.template.api;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.config.manager.AbstractConfigManager;

import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.core.notification.email.template.api.model.EntityMailTemplate;
import com.tlc.crm.core.notification.email.template.api.model.TemplateTag;
import com.tlc.crm.core.notification.email.template.status.TemplateTagErrorCodes;
import com.tlc.crm.core.sql.resource.OMEMTAG;
import com.tlc.crm.core.sql.resource.OMEMTEMPLATETAG;

import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.dml.OrderByClause.OrderType;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the {@link TemplateTag} related DB actions
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class TemplateTagManager extends AbstractConfigManager<TemplateTag> {

    private static class Instance {
        private static final TemplateTagManager INSTANCE = new TemplateTagManager();
    }

    /**
     * <p>
     * Gets the {@link TemplateTagManager} Instance
     * </p>
     *
     * @return {@link TemplateTagManager}
     */
    public static TemplateTagManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Table}
     */
    @Override
    public Table getTable() {
        return Table.get(OMEMTAG.TABLE);
    }

    /**
     * <p>
     * Gets the template tag table
     * </p>
     *
     * @return the {@link Table} of template tag
     */
    private Table getTemplateTagTable() {
        return Table.get(OMEMTEMPLATETAG.TABLE);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the {@link Collection} of {@link TemplateTag} ids
     * @return The collection of {@link TemplateTag}
     */
    @Override
    public Collection<TemplateTag> get(final Long orgId, final Collection<Long> ids) {
        final DataContainer dataContainer = getDataContainer(orgId, ids);
        final Stream<Row> rows = dataContainer.getRows(getTable());

        return rows.map(this::convertRowToModel).toList();
    }

    /**
     * <p>
     * Converts row into {@link TemplateTag}
     * </p>
     *
     * @param row Represents the {@link TemplateTag} row
     * @return The {@link TemplateTag}
     */
    public TemplateTag convertRowToModel(final Row row) {
        final TemplateTag templateTag = new TemplateTag();

        templateTag.setId(row.getPKValue());
        templateTag.setOrgId(row.getOrgId());
        templateTag.setName(row.get(OMEMTAG.NAME));
        templateTag.setLastUsedTime(row.get(OMEMTAG.LAST_USED_TIME));
        templateTag.setCreatedTime(row.get(OMEMTAG.CREATED_TIME));
        templateTag.setOwnerId(row.get(OMEMTAG.OWNER_ID));
        templateTag.setUpdatedTime(row.get(OMEMTAG.UPDATED_TIME));

        return templateTag;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents the organization id
     * @param ids   Represents the {@link Collection} of {@link TemplateTag} ids
     * @return The {@link Collection} of {@link TemplateTag}
     */
    @Override
    public Collection<TemplateTag> partialGet(final Long orgId, final Collection<Long> ids) {
        return null;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param templateTag Represents the {@link TemplateTag} that holds the attributes
     * @return The {@link TemplateTag}
     */
    @Override
    public TemplateTag create(final TemplateTag templateTag) {
        final DataContainer targetContainer = DataContainer.create();

        loadTemplateTagIntoContainer(true, templateTag, null, targetContainer, false);
        orgDataStore(templateTag.orgId()).commitChanges(targetContainer);

        return templateTag;
    }

    /**
     * <p>
     * Loads the template tag details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param templateTag     Represents the {@link TemplateTag} attributes
     * @param targetContainer Holds the updated information of template tag attributes
     */
    private void loadTemplateTagIntoContainer(final boolean isCreateEvent, final TemplateTag templateTag, final DataContainer dataContainer, final DataContainer targetContainer) {
        loadTemplateTagIntoContainer(isCreateEvent, templateTag, dataContainer, targetContainer, false);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param templateTags Represents the {@link Collection} of {@link TemplateTag} that holds the attributes have to be updated
     * @return A {@link Map} which keys are status message and values are update operation details
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<TemplateTag> templateTags) {
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Collection<Long> ids = templateTags.stream().map(TemplateTag::id).filter(Objects::nonNull).toList();
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final DataContainer dataContainer = getDataContainer(orgId, ids);

        templateTags.forEach(templateTag -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadTemplateTagIntoContainer(false, templateTag, dataContainer, targetContainer);
                orgDataStore.commitChanges(targetContainer);
                processOutcome.addProcessedRecord(templateTag.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Template tag update failed Org ID : {} ID : {} reason : {}", orgId, templateTag.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, templateTag.id().toString()));
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
     * Deletes the unmapped template tags of the template in the container
     * </p>
     *
     * @param template        Represents the {@link EntityMailTemplate} that holds the attributes details
     * @param sourceContainer Holds the attributes of existing template
     * @param targetContainer Holds the updated information of template
     */
    public void deleteUnmappedTags(final EntityMailTemplate template, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Collection<TemplateTag> templateTags = template.getTemplateTags();
        final Table templateTagTable = getTemplateTagTable();
        final Collection<Long> tagIds = Objects.nonNull(templateTags) && !templateTags.isEmpty()
                ? templateTags.stream().map(TemplateTag::id).collect(Collectors.toUnmodifiableSet()) : null;

        final WhereClause templateIdWhereclause = new WhereClause(Criteria.eq(templateTagTable
                .getColumn(OMEMTEMPLATETAG.TEMPLATE_ID), template.id()));
        final WhereClause whereClause = Objects.nonNull(tagIds) && !tagIds.isEmpty() ? templateIdWhereclause.and(Criteria
                .notIn(templateTagTable.getColumn(OMEMTEMPLATETAG.TAG_ID), tagIds)) : templateIdWhereclause;

        final Stream<Row> rows = sourceContainer.getRows(templateTagTable, whereClause);

        rows.forEach(targetContainer::deleteRow);
    }

    /**
     * <p>
     * Loads the template tag data into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is create or an update event
     * @param orgId           Represents an organization id
     * @param templateId      Represents the template id
     * @param templateTags    Represents the {@link Collection} of {@link TemplateTag}
     * @param sourceContainer Holds the attributes of existing template
     * @param dataContainer   Holds the updated information of template
     */
    public void loadTemplateTagIntoContainer(final boolean isCreateEvent, final Long orgId, final Long templateId,
                                             final Collection<TemplateTag> templateTags, final DataContainer sourceContainer,
                                             final DataContainer dataContainer) {
        if (Objects.nonNull(templateTags) && !templateTags.isEmpty()) {
            final Table templateTagTable = getTemplateTagTable();
            final List<TemplateTag> newlyMappedTags = new ArrayList<>();

            for (final TemplateTag templateTag : templateTags) {
                final boolean isNewTag = Objects.isNull(templateTag.id());

                if (isNewTag) {
                    loadTemplateTagIntoContainer(true, templateTag, null, dataContainer, true);
                }
                final Long tagId = templateTag.id();
                final Row existingRow = isCreateEvent ? null : sourceContainer.getRow(templateTagTable, new WhereClause(
                        Criteria.eq(templateTagTable.getColumn(OMEMTEMPLATETAG.TEMPLATE_ID), templateId))
                        .and(Criteria.eq(templateTagTable.getColumn(OMEMTEMPLATETAG.TAG_ID), tagId)));

                if (Objects.isNull(existingRow)) {
                    final Row row = new Row(templateTagTable);

                    row.setOrgId(orgId);
                    row.set(OMEMTEMPLATETAG.TEMPLATE_ID, templateId);
                    row.set(OMEMTEMPLATETAG.TAG_ID, tagId);

                    dataContainer.updateRow(row);
                    if (!isNewTag) {
                        newlyMappedTags.add(templateTag);
                    }
                }
            }
            if (!newlyMappedTags.isEmpty()) {
                loadTemplateTagRowIntoContainer(orgId, newlyMappedTags, dataContainer);
            }
        }
    }

    /**
     * <p>
     * Checks whether the template tag names exists or not
     * </p>
     *
     * @param orgId Represents an organization id
     * @param names Represents the collection of {@link TemplateTagManager} names
     * @return True if {@link TemplateTag} names are exists, otherwise returns false
     */
    public boolean checkNamesExistence(final Long orgId, final Collection<String> names) {
        final Table tagTable = getTable();
        final CountQuery countQuery = CountQuery.get(tagTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(tagTable.getColumn(OMEMTAG.NAME), names)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Fetches the template tag count
     * </p>
     *
     * @param orgId Represents an organization id
     * @return the tag count.
     */
    public Long getTemplateTagCount(final long orgId, final String searchText) {
        final Table tagTable = getTable();
        final CountQuery countQuery = CountQuery.get(tagTable);

        if (Objects.nonNull(searchText) && !searchText.isBlank()) {
            countQuery.setWhereClause(new WhereClause(Criteria.contains(tagTable.getColumn(OMEMTAG.NAME), searchText)));
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Fetches the {@link TemplateTag} list for the dropdown actions.
     * </p>
     *
     * @param orgId    Represents an Organization id.
     * @param start    Represents the start value
     * @param limit    Which holds the limit configuration.
     * @param field    Represents the field to sort by
     * @param sortType Represents the sort type
     * @return {@link Collection} of tags.
     */
    public Collection<TemplateTag> getTagList(final Long orgId, final int start, final int limit, final String field,
                                              final SortType sortType, final String searchText) {
        final Table tagTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(tagTable);
        final Column createdTimeColumn = tagTable.getColumn(OMEMTAG.CREATED_TIME);
        final Column lastUsedTimeColumn = tagTable.getColumn(OMEMTAG.LAST_USED_TIME);
        final Column nameColumn = tagTable.getColumn(OMEMTAG.NAME);
        final OrderType orderType = SortType.ASCENDING == sortType ? OrderType.ASCENDING : OrderType.DESCENDING;

        if (Objects.nonNull(searchText) && !searchText.isBlank()) {
            selectQuery.setWhereClause(new WhereClause(Criteria.contains(nameColumn, searchText)));
        }

        switch (field) {
            case "name" -> selectQuery.addOrderByClause(new OrderByClause(nameColumn, orderType));
            case "createdBy" -> selectQuery.addOrderByClause(new OrderByClause(tagTable.getColumn(OMEMTAG.OWNER_ID), orderType));
            case "lastUsed" -> {
                final OrderByClause orderByClause = new OrderByClause(lastUsedTimeColumn, orderType);

                if (SortType.ASCENDING == sortType) {
                    orderByClause.setNullFirst(false);
                }
                selectQuery.addOrderByClause(orderByClause);
            }
            default -> selectQuery.addOrderByClause(new OrderByClause(createdTimeColumn, orderType));
        }
        selectQuery.setLimitClause(new LimitClause(start, limit));
        final Stream<Row> rows = orgDataStore(orgId).get(selectQuery).getRows(tagTable);

        return rows.map(this::convertRowToModel).toList();
    }

    /**
     * <p>
     * Fetches the {@link TemplateTag} list for the dropdown actions.
     * </p>
     *
     * @param orgId    Represents an Organization id.
     * @param limit    Which holds the limit configuration.
     * @param criteria Which configure the criteria.
     * @return {@link Collection} of tags.
     */
    public Collection<TemplateTag> getTagDropDownList(final Long orgId, final DropdownLimit limit,
                                                      final DropdownCriteria criteria) {
        final Table tagTable = getTable();
        final Column tagNameColumn = tagTable.getColumn(OMEMTAG.NAME);
        final SelectQuery selectQuery = SelectQuery.get(tagTable);

        selectQuery.addSelectClause(tagNameColumn);
        selectQuery.setWhereClause(new WhereClause(criteria.build(tagNameColumn)));
        selectQuery.addOrderByClause(new OrderByClause(tagTable.getPKColumn()));
        selectQuery.setLimitClause(limit.getLimitClause());
        final Stream<Row> rows = orgDataStore(orgId).get(selectQuery).getRows(tagTable);

        return rows.map(this::convertRowToModel).toList();
    }

    /**
     * <p>
     * Checks whether the template tags exists or not, even if a single tag is not found it returns false
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the collection of {@link TemplateTag} ids
     * @return If true means {@link TemplateTag} names are exists
     */
    public boolean exists(final Long orgId, final Collection<Long> ids) {
        final Table tagTable = getTable();
        final CountQuery countQuery = CountQuery.get(tagTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(tagTable.getPKColumn(), ids)));

        return orgDataStore(orgId).get(countQuery) == ids.size();
    }

    /**
     * <p>
     * Loads the template tag details into container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param templateTag     Represents the {@link TemplateTag} attributes
     * @param dataContainer   Holds the user defined and system field values of existing {@link TemplateTag}
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void loadTemplateTagIntoContainer(final boolean isCreateEvent, final TemplateTag templateTag,
                                              final DataContainer dataContainer, final DataContainer targetContainer,
                                              final boolean assignedToTemplate) {
        final Table templateTagTable = getTable();
        final Long templateTagId = templateTag.id();
        final Row templateTagRow = isCreateEvent ? new Row(templateTagTable) : dataContainer.getRow(templateTagTable, templateTagId);

        if (!isCreateEvent && Objects.isNull(templateTagRow)) {
            throw ErrorCode.get(TemplateTagErrorCodes.TEMPLATE_TAG_NOT_FOUND, String.format("Tag Id : %s", templateTagId));
        }
        final Long currentTime = Instant.now().toEpochMilli();

        if (templateTagRow.isNewRow()) {
            templateTagRow.setOrgId(templateTag.orgId());
            templateTagRow.set(OMEMTAG.CREATED_TIME, currentTime);
            templateTag.setId(templateTagRow.getPKValue());
            templateTagRow.set(OMEMTAG.OWNER_ID, templateTag.getOwnerId());

            if (assignedToTemplate) {
                templateTagRow.set(OMEMTAG.LAST_USED_TIME, currentTime);
            }
        }
        templateTagRow.set(OMEMTAG.NAME, templateTag.getName());
        templateTagRow.set(OMEMTAG.UPDATED_TIME, currentTime);

        targetContainer.updateRow(templateTagRow);
    }

    /**
     * <p>
     * Loads the template row into container
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param templateTags  Represents the collection of template tag ids
     * @param dataContainer Holds the updated information of field values provided by user
     */
    private void loadTemplateTagRowIntoContainer(final Long orgId, final Collection<TemplateTag> templateTags,
                                                 final DataContainer dataContainer) {
        final Table tagTable = Table.get(OMEMTAG.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(tagTable);
        final Collection<Long> ids = templateTags.stream().map(TemplateTag::id).collect(Collectors.toSet());

        selectQuery.setWhereClause(new WhereClause(Criteria.in(tagTable.getPKColumn(), ids)));
        final DataContainer existingDataContainer = orgDataStore(orgId).get(selectQuery);
        final Stream<Row> rows = existingDataContainer.getRows(tagTable);

        rows.forEach(row -> {
            row.set(OMEMTAG.LAST_USED_TIME, Instant.now().toEpochMilli());

            dataContainer.updateRow(row);
        });
    }

    /**
     * <p>
     * Gets the template tag data container
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Represents the {@link Collection} of {@link TemplateTag} ids
     * @return The {@link DataContainer}
     */
    private DataContainer getDataContainer(final Long orgId, final Collection<Long> ids) {
        final Table tagTable = getTable();

        return orgDataStore(orgId).get(tagTable, new WhereClause(Criteria.in(tagTable.getPKColumn(), ids)));
    }

    /**
     * <p>
     * Retrieves the template tags count
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param dropdownCriteria Represent a dropdown criteria
     * @return The count of entities
     */
    public long getTotalCount(final Long orgId, final DropdownCriteria dropdownCriteria) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        if (Objects.nonNull(dropdownCriteria)) {
            countQuery.setWhereClause(new WhereClause(dropdownCriteria.build(table.getColumn(OMEMTAG.NAME))));
        }

        return orgDataStore(orgId).get(countQuery);
    }
}
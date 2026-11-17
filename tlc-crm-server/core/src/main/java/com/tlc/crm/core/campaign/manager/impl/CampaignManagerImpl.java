package com.tlc.crm.core.campaign.manager.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.CommonFields;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.common.models.SortType;

import com.tlc.crm.core.campaign.fields.CampaignFields;
import com.tlc.crm.core.campaign.manager.CampaignFolderManager;
import com.tlc.crm.core.campaign.manager.CampaignManager;
import com.tlc.crm.core.campaign.model.Campaign;
import com.tlc.crm.core.campaign.model.CampaignCategory;
import com.tlc.crm.core.sql.resource.OMKCPFOLDER;
import com.tlc.crm.entity.campaign.model.CampaignRecipient;
import com.tlc.crm.core.campaign.model.CampaignDefaultFilter;
import com.tlc.crm.core.campaign.model.CampaignFilterGroup;
import com.tlc.crm.core.campaign.model.CampaignFilterUserPreference;
import com.tlc.crm.core.campaign.model.CampaignFolder;
import com.tlc.crm.core.campaign.model.CampaignStatus;
import com.tlc.crm.core.segment.manager.SegmentManager;
import com.tlc.crm.core.segment.models.Segment;
import com.tlc.crm.core.sql.resource.OMKSSEGMENT;
import com.tlc.crm.entity.marketinglist.manager.MarketingListManager;
import com.tlc.crm.entity.marketinglist.model.MarketingList;
import com.tlc.crm.core.sql.resource.OMKCPCAMPAIGN;
import com.tlc.crm.core.sql.resource.OMKCPCAMPAIGNRECIPIENT;
import com.tlc.crm.core.sql.resource.OMKSSEGMENTCONTACT;
import com.tlc.crm.entity.sql.resource.OMKLMARKETINGLIST;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.validator.ProcessOutcome;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the {@link Campaign} related DB actions.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class CampaignManagerImpl extends AbstractConfigManager<Campaign> implements CampaignManager {

    private final UserService userService;
    private final MarketingListManager marketingListManager;
    private final SegmentManager segmentManager;
    private final CampaignFolderManager campaignFolderManager;

    private static class Instance {
        private static final CampaignManagerImpl INSTANCE = new CampaignManagerImpl();
    }

    private CampaignManagerImpl() {
        userService = UserService.getInstance();
        marketingListManager = MarketingListManager.getInstance();
        segmentManager = SegmentManager.getInstance();
        campaignFolderManager = CampaignFolderManager.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link CampaignManagerImpl}.
     * </p>
     *
     * @return {@link CampaignManagerImpl}.
     */
    public static CampaignManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(OMKCPCAMPAIGN.TABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getCampaignRecipientTable() {
        return Table.get(OMKCPCAMPAIGNRECIPIENT.TABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Campaign> get(final Long orgId, final Collection<Long> ids) {
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION, "i18n.campaign.unsupported.action");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Campaign> partialGet(final Long orgId, final Collection<Long> ids) {
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION, "i18n.campaign.unsupported.action");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Campaign create(final Campaign campaign) {
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION, "i18n.campaign.unsupported.action");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<Campaign> campaigns) {
        throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_ACTION, "i18n.campaign.unsupported.action");
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organization id.
     * @param category   Represents the campaign category.
     * @param campaignId Represents the campaign id.
     * @param name       Represents the name.
     * @return true if exists, false otherwise
     */
    @Override
    public boolean existsByName(final Long orgId, final CampaignCategory category, final Long campaignId, final String name) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMKCPCAMPAIGN.NAME), name))
                .and(Criteria.eq(table.getColumn(OMKCPCAMPAIGN.CATEGORY), category.getId()));

        countQuery.setWhereClause(Objects.nonNull(campaignId)
                ? whereClause.and(Criteria.notEq(table.getPKColumn(), campaignId)) : whereClause);

        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents the organization id.
     * @param campaignIds Represents the campaign ids.
     * @param archived    Represents the archived state.
     */
    @Override
    public void updateArchiveState(final Long orgId, final Collection<Long> campaignIds, final boolean archived) {
        if (Objects.isNull(campaignIds) || campaignIds.isEmpty()) {
            return;
        }
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMKCPCAMPAIGN.IS_ARCHIVED), archived);
        updateQuery.setWhereClause(new WhereClause(Criteria.in(table.getPKColumn(), campaignIds)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents the organization id.
     * @param userId       Represents the user id.
     * @param campaignId   Represents the campaign id.
     * @param campaignName Represents the updated campaign name.
     */
    @Override
    public void rename(final Long orgId, final Long userId, final Long campaignId, final String campaignName) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMKCPCAMPAIGN.NAME), campaignName);
        updateQuery.addUpdateColumn(table.getColumn(OMKCPCAMPAIGN.UPDATED_BY), userId);
        updateQuery.addUpdateColumn(table.getColumn(OMKCPCAMPAIGN.UPDATED_AT), Instant.now().toEpochMilli());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), campaignId)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organization id.
     * @param userId     Represents the user id.
     * @param campaignId Represents the campaign id.
     */
    @Override
    public void stopSending(final Long orgId, final Long userId, final Long campaignId) {
        updateStatus(orgId, userId, campaignId, CampaignStatus.STOPPED);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organization id.
     * @param userId     Represents the user id.
     * @param campaignId Represents the campaign id.
     */
    @Override
    public void pause(final Long orgId, final Long userId, final Long campaignId) {
        updateStatus(orgId, userId, campaignId, CampaignStatus.PAUSED);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organization id.
     * @param userId     Represents the user id.
     * @param campaignId Represents the campaign id.
     * @param folderId   Represents the folder id.
     */
    @Override
    public void addToFolder(final Long orgId, final Long userId, final Long campaignId, final Long folderId) {
        updateFolderId(orgId, userId, campaignId, folderId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organization id.
     * @param userId     Represents the user id.
     * @param campaignId Represents the campaign id.
     */
    @Override
    public void removeFromFolder(final Long orgId, final Long userId, final Long campaignId) {
        updateFolderId(orgId, userId, campaignId, null);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents the organization id.
     * @param userId        Represents the user id.
     * @param campaignId    Represents the campaign id.
     * @param scheduledTime Represents the scheduled time.
     * @param timeZoneId    Represents the time zone id.
     */
    @Override
    public void reschedule(final Long orgId, final Long userId, final Long campaignId, final long scheduledTime, final int timeZoneId) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMKCPCAMPAIGN.SCHEDULED_TIME), scheduledTime);
        updateQuery.addUpdateColumn(table.getColumn(OMKCPCAMPAIGN.TIME_ZONE_ID), timeZoneId);
        updateQuery.addUpdateColumn(table.getColumn(OMKCPCAMPAIGN.UPDATED_AT), Instant.now().toEpochMilli());
        updateQuery.addUpdateColumn(table.getColumn(OMKCPCAMPAIGN.UPDATED_BY), userId);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), campaignId)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organization id.
     * @param campaignId Represents the campaign id.
     * @param isArchived Represents the archived state.
     */
    @Override
    public void updateArchiveState(final Long orgId, final Long campaignId, final boolean isArchived) {
        updateArchiveState(orgId, List.of(campaignId), isArchived);
    }

    /**
     * <p>
     * Updates the folder id for the given campaign.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param campaignId Represents the campaign id.
     * @param folderId   Represents the folder id, or {@code null} to remove.
     */
    private void updateFolderId(final Long orgId, final Long userId, final Long campaignId, final Long folderId) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMKCPCAMPAIGN.FOLDER_ID), folderId);
        updateQuery.addUpdateColumn(table.getColumn(OMKCPCAMPAIGN.UPDATED_AT), Instant.now().toEpochMilli());
        updateQuery.addUpdateColumn(table.getColumn(OMKCPCAMPAIGN.UPDATED_BY), userId);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), campaignId)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organization id.
     * @param userId     Represents the user id.
     * @param campaignId Represents the campaign id.
     * @param status     Represents the campaign status.
     */
    @Override
    public void updateStatus(final Long orgId, final Long userId, final Long campaignId, final CampaignStatus status) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMKCPCAMPAIGN.STATUS), status.getId());
        updateQuery.addUpdateColumn(table.getColumn(OMKCPCAMPAIGN.UPDATED_AT), Instant.now().toEpochMilli());
        updateQuery.addUpdateColumn(table.getColumn(OMKCPCAMPAIGN.UPDATED_BY), userId);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), campaignId)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Returns the {@link OrderByClause} based on the given sorting configuration.
     * </p>
     *
     * @param preference Represents the campaign filter preference.
     * @return The {@link OrderByClause} to be applied for the query.
     */
    private OrderByClause getOrderByClause(final CampaignFilterUserPreference preference) {
        final Table campaignTable = getTable();
        final OrderByClause defaultOrderByClause = new OrderByClause(campaignTable.getColumn(OMKCPCAMPAIGN.CREATED_AT), OrderByClause.OrderType.DESCENDING);

        if (Objects.isNull(preference)) {
            return defaultOrderByClause;
        }
        final JsonObject sortJson = preference.getSortJson();

        if (Objects.nonNull(sortJson)) {
            final String sortFieldName = sortJson.getString(CommonFields.FIELD_NAME);
            final String sortType = sortJson.getString(CommonFields.TYPE);

            if (Objects.isNull(sortFieldName) || Objects.isNull(sortType)) {
                return defaultOrderByClause;
            }

            return switch (sortFieldName) {
                case "name" -> new OrderByClause(campaignTable.getColumn(OMKCPCAMPAIGN.NAME), SortType.getSortType(sortType).getOrderType());
                default -> defaultOrderByClause;
            };
        }

        return defaultOrderByClause;
    }

    /**
     * <p>
     * Loads the campaign details into the target container.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create event or update event.
     * @param campaign        An entity that holds the {@link Campaign} attributes.
     * @param sourceContainer Holds the old information of campaign.
     * @param targetContainer Holds the updated information of campaign.
     */
    @Override
    public void loadCampaignIntoContainer(final boolean isCreateEvent, final Campaign campaign,
                                           final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table table = getTable();
        final Long currentTime = Instant.now().toEpochMilli();
        final Row row = isCreateEvent ? new Row(table) : sourceContainer.getRow(table, campaign.id());
        final CampaignFolder folder = campaign.getFolder();
        final TimezoneInfo timezoneInfo = campaign.getTimezoneInfo();

        if (row.isNewRow()) {
            campaign.setId(row.getPKValue());
            row.setOrgId(campaign.orgId());
            row.set(OMKCPCAMPAIGN.CREATED_BY, campaign.getCreatedById());
            row.set(OMKCPCAMPAIGN.CREATED_AT, currentTime);
            row.set(OMKCPCAMPAIGN.CATEGORY, campaign.getCategory().getId());
        }
        row.set(OMKCPCAMPAIGN.NAME, campaign.getName());
        row.set(OMKCPCAMPAIGN.STATUS, campaign.getStatus().getId());
        row.set(OMKCPCAMPAIGN.IS_ARCHIVED, campaign.isArchived());
        row.set(OMKCPCAMPAIGN.FOLDER_ID, Objects.isNull(folder) ? null : folder.id());
        row.set(OMKCPCAMPAIGN.UPDATED_BY, campaign.getUpdatedById());
        row.set(OMKCPCAMPAIGN.UPDATED_AT, currentTime);
        row.set(OMKCPCAMPAIGN.SCHEDULED_TIME, Objects.isNull(campaign.getScheduledTime()) ? null : campaign.getScheduledTime().getTimeStamp());
        row.set(OMKCPCAMPAIGN.TIME_ZONE_ID, Objects.isNull(timezoneInfo) ? null : (long) timezoneInfo.getId());
        row.set(OMKCPCAMPAIGN.TOTAL_RECIPIENT_COUNT, campaign.getTotalRecipientCount());

        targetContainer.updateRow(row);
        loadRecipientsIntoContainer(isCreateEvent, campaign, sourceContainer, targetContainer);
    }


    /**
     * <p>
     * Loads the recipients (marketing lists and segments) into the target data container.
     * </p>
     *
     * @param isCreateEvent   Represents whether the operation is a create event or not.
     * @param campaign      Represents the WhatsApp campaign.
     * @param sourceContainer Represents the source data container.
     * @param targetContainer Represents the target data container where the recipients will be loaded.
     */
    private void loadRecipientsIntoContainer(final boolean isCreateEvent, final Campaign campaign, final DataContainer sourceContainer,
                                             final DataContainer targetContainer) {
        final Table campaignRecipientTable = getCampaignRecipientTable();
        final Collection<CampaignRecipient> recipients = campaign.getRecipients();
        final Long orgId = campaign.orgId();
        final Long campaignId = campaign.id();
        final Collection<Long> marketingListIds = new HashSet<>();
        final Collection<Long> segmentIds = new HashSet<>();

        if (Objects.nonNull(recipients) && !recipients.isEmpty()) {
            recipients.forEach(recipient -> {
                final CampaignRecipient.RecipientType recipientType = recipient.getRecipientType();

                if (CampaignRecipient.RecipientType.MARKETING_LIST == recipientType) {
                    final Long marketingListId = recipient.id();
                    final Row row = isCreateEvent ? new Row(campaignRecipientTable) : sourceContainer.getOrInitRow(campaignRecipientTable,
                            new WhereClause(Criteria.eq(campaignRecipientTable.getColumn(OMKCPCAMPAIGNRECIPIENT.CAMPAIGN_ID), campaignId))
                                    .and(Criteria.eq(campaignRecipientTable.getColumn(OMKCPCAMPAIGNRECIPIENT.MARKETING_LIST_ID), marketingListId)));

                    if (row.isNewRow()) {
                        row.setOrgId(orgId);
                        row.set(OMKCPCAMPAIGNRECIPIENT.CAMPAIGN_ID, campaignId);
                        row.set(OMKCPCAMPAIGNRECIPIENT.MARKETING_LIST_ID, marketingListId);
                        targetContainer.updateRow(row);
                    }
                    marketingListIds.add(marketingListId);
                } else if (CampaignRecipient.RecipientType.SEGMENT == recipientType) {
                    final Long segmentId = recipient.id();
                    final Row row = isCreateEvent ? new Row(campaignRecipientTable) : sourceContainer.getOrInitRow(campaignRecipientTable,
                            new WhereClause(Criteria.eq(campaignRecipientTable.getColumn(OMKCPCAMPAIGNRECIPIENT.CAMPAIGN_ID), campaignId))
                                    .and(Criteria.eq(campaignRecipientTable.getColumn(OMKCPCAMPAIGNRECIPIENT.SEGMENT_ID), segmentId)));

                    if (row.isNewRow()) {
                        row.setOrgId(orgId);
                        row.set(OMKCPCAMPAIGNRECIPIENT.CAMPAIGN_ID, campaignId);
                        row.set(OMKCPCAMPAIGNRECIPIENT.SEGMENT_ID, segmentId);
                        targetContainer.updateRow(row);
                    }
                    segmentIds.add(segmentId);
                }
            });
        }

        if (!isCreateEvent) {
            sourceContainer.getRows(campaignRecipientTable, new WhereClause(Criteria.eq(campaignRecipientTable.getColumn(OMKCPCAMPAIGNRECIPIENT.CAMPAIGN_ID), campaignId)))
                    .filter(recipientRow -> {
                        final Long marketingListId = recipientRow.get(OMKCPCAMPAIGNRECIPIENT.MARKETING_LIST_ID);
                        final Long segmentId = recipientRow.get(OMKCPCAMPAIGNRECIPIENT.SEGMENT_ID);

                        if (Objects.nonNull(marketingListId)) {
                            return !marketingListIds.contains(marketingListId);
                        }
                        if (Objects.nonNull(segmentId)) {
                            return !segmentIds.contains(segmentId);
                        }

                        return false;
                    }).forEach(targetContainer::deleteRow);
        }
    }

    /**
     * <p>
     * Converts the campaign {@link Row} into {@link Campaign} instance.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param row   Which contains the values of campaign.
     * @return {@link Campaign} that holds the values of campaign.
     */
    private Campaign convertRowToCampaign(final Long orgId, final Row row, final DataContainer sourceContainer) {
        final Campaign campaign = new Campaign();

        loadRowDetailsIntoCampaign(orgId, campaign, row, sourceContainer);

        return campaign;
    }

    /**
     * <p>
     * Loads database row details into campaign
     * </p>
     *
     * @param orgId           Represents the org id
     * @param campaign        Represents the {@link Campaign}
     * @param row             Represents the database row
     * @param sourceContainer Represents the source container
     */
    @Override
    public void loadRowDetailsIntoCampaign(final Long orgId, final Campaign campaign, final Row row, final DataContainer sourceContainer) {
        final Long folderId = row.get(OMKCPCAMPAIGN.FOLDER_ID);
        final Short timezoneId = row.get(OMKCPCAMPAIGN.TIME_ZONE_ID);
        final Table marketingListTable = marketingListManager.getTable();
        final Table segmentTable = segmentManager.getTable();
        final Long campaignId = row.getPKValue();

        campaign.setId(campaignId);
        campaign.setOrgId(orgId);
        campaign.setName(row.get(OMKCPCAMPAIGN.NAME));
        campaign.setCategory(CampaignCategory.get(row.get(OMKCPCAMPAIGN.CATEGORY)));
        campaign.setStatus(CampaignStatus.get(row.get(OMKCPCAMPAIGN.STATUS)));
        campaign.setArchived(row.get(OMKCPCAMPAIGN.IS_ARCHIVED));
        campaign.setCreatedTime(row.get(OMKCPCAMPAIGN.CREATED_AT));
        campaign.setUpdatedTime(row.get(OMKCPCAMPAIGN.UPDATED_AT));
        campaign.setScheduledTime(row.get(OMKCPCAMPAIGN.SCHEDULED_TIME));
        campaign.setTotalRecipientCount(row.get(OMKCPCAMPAIGN.TOTAL_RECIPIENT_COUNT));
        campaign.setReadyToSend(row.get(OMKCPCAMPAIGN.READY_TO_SEND));
        campaign.setCreatedById(row.get(OMKCPCAMPAIGN.CREATED_BY));
        campaign.setUpdatedById(row.get(OMKCPCAMPAIGN.UPDATED_BY));

        if (Objects.nonNull(timezoneId)) {
            campaign.setTimezoneInfo(TimezoneInfo.getTimezoneById(timezoneId));
        }
        if (Objects.nonNull(folderId)) {
            final CampaignFolder folder = new CampaignFolder(folderId);

            if (Objects.nonNull(sourceContainer)) {
                final Row folderRow = sourceContainer.getRow(campaignFolderManager.getTable(), folderId);

                if (Objects.nonNull(folderRow)) {
                    folder.setName(folderRow.get(OMKCPFOLDER.NAME));
                    folder.setStarred(folderRow.get(OMKCPFOLDER.IS_STARRED));
                }
            }

            folder.setOrgId(orgId);
            campaign.setFolder(folder);
        }

        if (Objects.nonNull(sourceContainer)) {
            final Collection<CampaignRecipient> recipients = new ArrayList<>();

            final Table campaignRecipientTable = getCampaignRecipientTable();
            sourceContainer.getRows(campaignRecipientTable, new WhereClause(Criteria.eq(campaignRecipientTable.getColumn(OMKCPCAMPAIGNRECIPIENT.CAMPAIGN_ID), campaignId)))
                    .sorted(Comparator.comparing(recipientRow -> (Long) recipientRow.get(OMKCPCAMPAIGNRECIPIENT.ID)))
                    .forEach(recipientRow -> {
                        final Long marketingListId = recipientRow.get(OMKCPCAMPAIGNRECIPIENT.MARKETING_LIST_ID);
                        final Long segmentId = recipientRow.get(OMKCPCAMPAIGNRECIPIENT.SEGMENT_ID);
                        final Long contactCount = recipientRow.get(OMKCPCAMPAIGNRECIPIENT.CONTACT_COUNT);

                        if (Objects.nonNull(marketingListId)) {
                            final MarketingList marketingList = new MarketingList();
                            final Row marketingListRow = sourceContainer.getRow(marketingListTable, marketingListId);

                            marketingList.setOrgId(orgId);
                            marketingList.setId(marketingListId);
                            marketingList.setContactCount(Objects.nonNull(contactCount) ? contactCount : 0L);
                            marketingList.setName(Objects.nonNull(marketingListRow) ? marketingListRow.get(OMKLMARKETINGLIST.NAME) : null);
                            recipients.add(marketingList);
                        } else if (Objects.nonNull(segmentId)) {
                            final Segment segment = new Segment();
                            final Row segmentRow = sourceContainer.getRow(segmentTable, segmentId);

                            segment.setOrgId(orgId);
                            segment.setId(segmentId);
                            segment.setContactCount(Objects.nonNull(contactCount) ? contactCount : 0L);
                            segment.setName(Objects.nonNull(segmentRow) ? segmentRow.get(OMKSSEGMENT.NAME) : null);
                            recipients.add(segment);
                        }
                    });

            if (!recipients.isEmpty()) {
                campaign.setRecipients(recipients);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organization id.
     * @param campaignId Represents the campaign id.
     * @return {@link CampaignStatus}.
     */
    @Override
    public Optional<CampaignStatus> getStatus(final Long orgId, final Long campaignId) {
        final Table campaignTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(campaignTable);

        selectQuery.addSelectClause(campaignTable.getColumn(OMKCPCAMPAIGN.STATUS));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(campaignTable.getPKColumn(), campaignId)));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(campaignTable);

        return Objects.nonNull(row) ? Optional.of(CampaignStatus.get(row.get(OMKCPCAMPAIGN.STATUS))) : Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the org id
     * @param preference Represents the {@link CampaignFilterUserPreference}
     */
    @Override
    public long getTotalCount(final Long orgId, final CampaignFilterUserPreference preference) {
        final Table campaignTable = getTable();
        final CountQuery countQuery = CountQuery.get(campaignTable);

        countQuery.setWhereClause(buildListFilterWhereClause(preference));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the org id
     * @param preference Represents the {@link CampaignFilterUserPreference}
     */
    @Override
    public Collection<Campaign> get(final Long orgId, final CampaignFilterUserPreference preference) {
        final Table campaignTable = getTable();
        final Table campaignFolderTable = campaignFolderManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(campaignTable);

        selectQuery.addSelectClause(campaignTable);
        selectQuery.addSelectClause(List.of(campaignFolderTable.getColumn(OMKCPFOLDER.NAME), campaignFolderTable.getColumn(OMKCPFOLDER.CATEGORY),
                campaignFolderTable.getColumn(OMKCPFOLDER.IS_STARRED)));
        selectQuery.addJoinClause(new JoinClause(campaignTable.getColumn(OMKCPCAMPAIGN.FOLDER_ID), campaignFolderTable.getPKColumn(), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(buildListFilterWhereClause(preference));
        selectQuery.addOrderByClause(getOrderByClause(preference));
        selectQuery.setLimitClause(new LimitClause(preference.getStart(), preference.getLimit()));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(campaignTable).map(row -> convertRowToCampaign(orgId, row, dataContainer)).toList();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param preference    Represents the campaign filter preference.
     * @return The {@link WhereClause}, or {@code null} when no filters apply.
     */
    @Override
    public WhereClause buildListFilterWhereClause(final CampaignFilterUserPreference preference) {
        if (Objects.isNull(preference)) {
            return null;
        }

        final Table campaignTable = getTable();
        WhereClause whereClause = new WhereClause(Criteria.eq(campaignTable.getColumn(OMKCPCAMPAIGN.CATEGORY), preference.getCategory().getId()));

        if (Objects.equals(CampaignFilterGroup.DEFAULT_FILTERS, preference.getCampaignFilterGroup())) {
            final Column archiveColumn = campaignTable.getColumn(OMKCPCAMPAIGN.IS_ARCHIVED);

            switch (preference.getCampaignDefaultFilter()) {
                case ALL_CAMPAIGNS ->
                        whereClause = WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.eq(archiveColumn, false)), Connector.AND);
                case ARCHIVED_CAMPAIGNS ->
                        whereClause = WhereClauseUtil.connect(whereClause, new WhereClause(Criteria.eq(archiveColumn, true)), Connector.AND);
            }
        }
        final CampaignFolder folder = preference.getCampaignFolder();

        if (Objects.nonNull(folder) && Objects.nonNull(folder.id())) {
            whereClause = WhereClauseUtil.connect(whereClause,
                    new WhereClause(Criteria.eq(campaignTable.getColumn(OMKCPCAMPAIGN.FOLDER_ID), folder.id())), Connector.AND);
        }
        final String searchText = preference.getSearchText();

        if (Objects.nonNull(searchText) && !searchText.isBlank()) {
            whereClause = WhereClauseUtil.connect(whereClause,
                    new WhereClause(Criteria.contains(campaignTable.getColumn(OMKCPCAMPAIGN.NAME), searchText)), Connector.AND);
        }

        if (preference.hasStatusFilter()) {
            whereClause = WhereClauseUtil.connect(whereClause,
                    new WhereClause(Criteria.in(campaignTable.getColumn(OMKCPCAMPAIGN.STATUS), preference.getSelectedStatusIds())),
                    Connector.AND);
        }

        return whereClause;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param folderIds Represents the folder ids.
     * @return A map of folder id to campaign count.
     */
    @Override
    public Map<Long, Long> getCampaignCounts(final Long orgId, final Collection<Long> folderIds) {
        final Table table = getTable();
        final Column folderIdColumn = table.getColumn(OMKCPCAMPAIGN.FOLDER_ID);
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(List.of(folderIdColumn,
                table.getColumn(OMKCPCAMPAIGN.ID, CampaignFields.CAMPAIGN_COUNT).count()));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(folderIdColumn, folderIds)));
        selectQuery.addGroupByClause(new GroupByClause(folderIdColumn));

        return orgDataStore(orgId).getData(selectQuery).stream()
                .collect(Collectors.toMap(row -> (Long) row.get(OMKCPCAMPAIGN.FOLDER_ID), row -> {
                    final Long campaignCount = (Long) row.get(CampaignFields.CAMPAIGN_COUNT);

                    return Objects.isNull(campaignCount) ? 0L : campaignCount;
                }));
    }
}
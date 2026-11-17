package com.tlc.crm.messenger.campaign.manager.impl;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.campaign.manager.CampaignManager;
import com.tlc.crm.core.campaign.model.CampaignRecipientStatus;
import com.tlc.crm.core.segment.manager.SegmentManager;
import com.tlc.crm.core.sql.resource.OMKCPCAMPAIGNRECIPIENT;

import com.tlc.crm.core.sql.resource.OMKSSEGMENT;
import com.tlc.crm.entity.marketinglist.manager.MarketingListManager;
import com.tlc.crm.entity.sql.resource.OMKLMARKETINGLIST;
import com.tlc.crm.messenger.api.channel.model.CrmWAPhoneNumber;
import com.tlc.crm.messenger.campaign.model.WACampaign;
import com.tlc.crm.messenger.campaign.model.WATemplateMetrics;
import com.tlc.crm.messenger.campaign.manager.WACampaignManager;
import com.tlc.crm.messenger.sql.resource.OMKCPWACAMPAIGN;
import com.tlc.crm.messenger.sql.resource.OMKCPWACAMPAIGNCONTACT;

import com.tlc.social.common.whatsapp.model.template.WATemplate;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides WhatsApp campaign and metrics related DB actions.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WACampaignManagerImpl extends AbstractConfigManager<WACampaign> implements WACampaignManager {

    private final CampaignManager campaignManager;
    private final MarketingListManager marketingListManager;
    private final SegmentManager segmentManager;

    private static class Instance {
        private static final WACampaignManagerImpl INSTANCE = new WACampaignManagerImpl();
    }

    private WACampaignManagerImpl() {
        campaignManager = CampaignManager.getInstance();
        marketingListManager = MarketingListManager.getInstance();
        segmentManager = SegmentManager.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link WACampaignManagerImpl}.
     * </p>
     *
     * @return {@link WACampaignManagerImpl}.
     */
    public static WACampaignManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable() {
        return Table.get(OMKCPWACAMPAIGN.TABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getCampaignContactTable() {
        return Table.get(OMKCPWACAMPAIGNCONTACT.TABLE);
    }


    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents the organisation Id
     * @param campaignIds Represents the campaign ids
     * @return the campaigns
     */
    @Override
    public Collection<WACampaign> get(final Long orgId, final Collection<Long> campaignIds) {
        final DataContainer sourceContainer = getCampaignContainer(orgId, campaignIds);
        final Row campaignRow = sourceContainer.getRow(campaignManager.getTable());
        final WACampaign campaign = new WACampaign();

        campaignManager.loadRowDetailsIntoCampaign(orgId, campaign, campaignRow, sourceContainer);
        final Row waCampaignRow = sourceContainer.getRow(getTable());
        final WATemplate waTemplate = new WATemplate();
        final CrmWAPhoneNumber waPhoneNumber = new CrmWAPhoneNumber();

        waTemplate.setId(waCampaignRow.get(OMKCPWACAMPAIGN.TEMPLATE_ID));
        waPhoneNumber.setCrmPhoneNumberId(waCampaignRow.get(OMKCPWACAMPAIGN.WA_PHONE_NUMBER_ID));
        waPhoneNumber.setOrgId(orgId);
        campaign.setTemplate(waTemplate);
        campaign.setBusinessPhoneNumber(waPhoneNumber);
        campaign.setTemplateConfig(waCampaignRow.get(OMKCPWACAMPAIGN.TEMPLATE_CONFIG));

        return List.of(campaign);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organisation Id
     * @param ids   Represents the campaign ids
     * @return the campaigns
     */
    @Override
    public Collection<WACampaign> partialGet(Long orgId, Collection<Long> ids) {
        return get(orgId, ids);
    }

    /**
     * {@inheritDoc}
     * 
     * @param waCampaign Represents the {@link WACampaign}
     * @return the campaign
     */
    @Override
    public WACampaign create(final WACampaign waCampaign) {
        final DataContainer targetContainer = DataContainer.create();

        loadWACampaignIntoContainer(true, waCampaign, null, targetContainer);
        orgDataStore(waCampaign.orgId()).commitChanges(targetContainer);

        return waCampaign;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organisation id.
     * @param campaigns Holds the details of the campaigns.
     * @return {@link ProcessOutcome}
     */
    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<WACampaign> campaigns) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final Collection<Long> campaignIds = campaigns.stream().map(WACampaign::id).filter(Objects::nonNull).toList();
        final DataContainer sourceContainer = getCampaignContainer(orgId, campaignIds);
        final Collection<ErrorDescriptor> failedEntities = new ArrayList<>();

        campaigns.forEach(waCampaign -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadWACampaignIntoContainer(false, waCampaign, sourceContainer, targetContainer);
                orgDataStore(orgId).commitChanges(targetContainer);
                processOutcome.addProcessedRecord(waCampaign.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Whatsapp Campaign update failed Org ID : {} ID : {} reason : {}",
                        orgId, waCampaign.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, waCampaign.id().toString()));
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
     * Loads whatsapp campaign into the container
     * </p>
     *
     * @param isCreateEvent   Represents whether the operation is a create event or not.
     * @param waCampaign      Represents the WhatsApp campaign.
     * @param sourceContainer Represents the source data container.
     * @param targetContainer Represents the target data container where the recipients will be loaded.
     */
    private void loadWACampaignIntoContainer(final boolean isCreateEvent, final WACampaign waCampaign, final DataContainer sourceContainer,
                                             final DataContainer targetContainer) {
        final Table table = getTable();

        campaignManager.loadCampaignIntoContainer(isCreateEvent, waCampaign, sourceContainer, targetContainer);
        final Long campaignId = waCampaign.id();
        final Row row = isCreateEvent ? new Row(table) : sourceContainer.getRow(table,
                new WhereClause(Criteria.eq(table.getColumn(OMKCPWACAMPAIGN.CAMPAIGN_ID), campaignId)));
        final Long currentTime = Instant.now().toEpochMilli();

        if (row.isNewRow()) {
            row.setOrgId(waCampaign.orgId());
            row.set(OMKCPWACAMPAIGN.CAMPAIGN_ID, campaignId);
            row.set(OMKCPWACAMPAIGN.TEMPLATE_ID, waCampaign.getTemplateId());
            row.set(OMKCPWACAMPAIGN.WA_PHONE_NUMBER_ID, waCampaign.getBusinessPhoneNumberId());
            row.set(OMKCPWACAMPAIGN.CREATED_BY, waCampaign.getCreatedById());
            row.set(OMKCPWACAMPAIGN.CREATED_AT, currentTime);
        }
        row.set(OMKCPWACAMPAIGN.TEMPLATE_CONFIG, waCampaign.getTemplateConfig());
        row.set(OMKCPWACAMPAIGN.UPDATED_BY, waCampaign.getUpdatedById());
        row.set(OMKCPWACAMPAIGN.UPDATED_AT, currentTime);
        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Gets the campaign container
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param campaignIds Represents the campaign ids.
     * @return The {@link DataContainer} containing existing campaign data.
     */
    private DataContainer getCampaignContainer(final Long orgId, final Collection<Long> campaignIds) {
        final Table campaignTable = campaignManager.getTable();
        final Table waCampaignTable = getTable();
        final Table campaignRecipientTable = campaignManager.getCampaignRecipientTable();
        final Column campaignPkColumn = campaignTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final Table marketingListTable = marketingListManager.getTable();
        final Table segmentTable = segmentManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(campaignTable);

        selectQuery.addSelectClause(campaignTable, waCampaignTable, campaignRecipientTable);
        selectQuery.addSelectClause(List.of(marketingListTable.getColumn(OMKLMARKETINGLIST.NAME), segmentTable.getColumn(OMKSSEGMENT.NAME)));
        selectQuery.addJoinClause(List.of(
                new JoinClause(campaignPkColumn, waCampaignTable.getColumn(OMKCPWACAMPAIGN.CAMPAIGN_ID)),
                new JoinClause(campaignPkColumn, campaignRecipientTable.getColumn(OMKCPCAMPAIGNRECIPIENT.CAMPAIGN_ID), leftJoin),
                new JoinClause(campaignRecipientTable.getColumn(OMKCPCAMPAIGNRECIPIENT.MARKETING_LIST_ID), marketingListTable.getPKColumn(), leftJoin),
                new JoinClause(campaignRecipientTable.getColumn(OMKCPCAMPAIGNRECIPIENT.SEGMENT_ID), segmentTable.getPKColumn(), leftJoin)
        ));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(campaignPkColumn, campaignIds)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param waCampaign Represents the WhatsApp campaign with template and phone number to validate.
     * @return {@code true} when both ids match the stored values; {@code false} otherwise.
     */
    @Override
    public boolean matchesExistingTemplateAndPhoneNumber(final Long orgId, final WACampaign waCampaign) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMKCPWACAMPAIGN.CAMPAIGN_ID), waCampaign.id())));

        final Row row = orgDataStore(orgId).get(selectQuery).getRows(table).findFirst().orElse(null);

        if (Objects.isNull(row)) {
            return false;
        }

        return Objects.equals(row.get(OMKCPWACAMPAIGN.TEMPLATE_ID), waCampaign.getTemplateId())
                && Objects.equals(row.get(OMKCPWACAMPAIGN.WA_PHONE_NUMBER_ID), waCampaign.getBusinessPhoneNumberId());
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param campaignIds Represents the campaign ids.
     * @return A map of campaign id to sent count.
     */
    @Override
    public Map<Long, Integer> getSentCounts(final Long orgId, final Collection<Long> campaignIds) {
        final Table table = getCampaignContactTable();
        final Column statusColumn = table.getColumn(OMKCPWACAMPAIGNCONTACT.STATUS);

        return getMetricCounts(orgId, campaignIds, new WhereClause(Criteria.eq(statusColumn, CampaignRecipientStatus.SENT.getId())));
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param campaignIds Represents the campaign ids.
     * @return A map of campaign id to delivered count.
     */
    @Override
    public Map<Long, Integer> getDeliveredCounts(final Long orgId, final Collection<Long> campaignIds) {
        final Table table = getCampaignContactTable();
        final Column deliveredColumn = table.getColumn(OMKCPWACAMPAIGNCONTACT.IS_DELIVERED);

        return getMetricCounts(orgId, campaignIds, new WhereClause(Criteria.eq(deliveredColumn, true)));
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param campaignIds Represents the campaign ids.
     * @return A map of campaign id to read count.
     */
    @Override
    public Map<Long, Integer> getReadCounts(final Long orgId, final Collection<Long> campaignIds) {
        final Table table = getCampaignContactTable();
        final Column readColumn = table.getColumn(OMKCPWACAMPAIGNCONTACT.IS_READ);

        return getMetricCounts(orgId, campaignIds, new WhereClause(Criteria.eq(readColumn, true)));
    }

    /**
     * <p>
     * Gets metric counts grouped by campaign id.
     * </p>
     *
     * @param orgId            Represents the organization id.
     * @param campaignIds      Represents the campaign ids.
     * @param additionalCriteria Represents the additional filter criteria.
     * @return A map of campaign id to count.
     */
    private Map<Long, Integer> getMetricCounts(final Long orgId, final Collection<Long> campaignIds, final WhereClause additionalCriteria) {
        if (Objects.isNull(campaignIds) || campaignIds.isEmpty()) {
            return Collections.emptyMap();
        }
        final Table campaignContactTable = getCampaignContactTable();
        final Column campaignIdColumn = campaignContactTable.getColumn(OMKCPWACAMPAIGNCONTACT.CAMPAIGN_ID);
        final SelectQuery selectQuery = SelectQuery.get(campaignContactTable);

        selectQuery.addSelectClause(List.of(campaignIdColumn, campaignContactTable.getPKColumn().count()));
        selectQuery.setWhereClause(WhereClauseUtil.connect(new WhereClause(Criteria.in(campaignIdColumn, campaignIds)), additionalCriteria, Connector.AND));
        selectQuery.addGroupByClause(new GroupByClause(campaignIdColumn));

        return orgDataStore(orgId).getData(selectQuery).stream()
                .collect(Collectors.toMap(row -> (Long) row.get(OMKCPWACAMPAIGNCONTACT.CAMPAIGN_ID),
                        row -> (Integer) row.get(OMKCPWACAMPAIGNCONTACT.ID)));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents the org id
     * @param templateIds Represents the template ids
     */
    @Override
    public Map<Long, WATemplateMetrics> getTemplateMetrics(final Long orgId, final Collection<Long> templateIds) {
        final Table campaignContactTable = getCampaignContactTable();
        final Map<Long, Integer> sentCounts = getTemplateMetricCounts(orgId, templateIds,
                new WhereClause(Criteria.eq(campaignContactTable.getColumn(OMKCPWACAMPAIGNCONTACT.STATUS), CampaignRecipientStatus.SENT.getId())));
        final Map<Long, Integer> deliveredCounts = getTemplateMetricCounts(orgId, templateIds,
                new WhereClause(Criteria.eq(campaignContactTable.getColumn(OMKCPWACAMPAIGNCONTACT.IS_DELIVERED), true)));
        final Map<Long, Integer> readCounts = getTemplateMetricCounts(orgId, templateIds,
                new WhereClause(Criteria.eq(campaignContactTable.getColumn(OMKCPWACAMPAIGNCONTACT.IS_READ), true)));
        final Map<Long, WATemplateMetrics> metricsMap = new HashMap<>();

        for (final Long templateId : templateIds) {
            final int sent = sentCounts.getOrDefault(templateId, 0);
            final int deliveredPercentage = sent == 0 ? 0 : (deliveredCounts.getOrDefault(templateId, 0) * 100) / sent;
            final int readPercentage = sent == 0 ? 0 : (readCounts.getOrDefault(templateId, 0) * 100) / sent;

            metricsMap.put(templateId, new WATemplateMetrics(sent, deliveredPercentage, readPercentage));
        }

        return metricsMap;
    }

    /**
     * <p>
     * Gets metric counts grouped by template id.
     * </p>
     *
     * @param orgId              Represents the org Id
     * @param templateIds        Represents the template ids
     * @param additionalCriteria Represents the additional criteria
     * @return the metric counts
     */
    private Map<Long, Integer> getTemplateMetricCounts(final Long orgId, final Collection<Long> templateIds, final WhereClause additionalCriteria) {
        final Table waCampaignTable = getTable();
        final Table campaignContactTable = getCampaignContactTable();
        final Column templateIdColumn = waCampaignTable.getColumn(OMKCPWACAMPAIGN.TEMPLATE_ID);
        final SelectQuery selectQuery = SelectQuery.get(waCampaignTable);

        selectQuery.addJoinClause(new JoinClause(waCampaignTable.getColumn(OMKCPWACAMPAIGN.CAMPAIGN_ID),
                campaignContactTable.getColumn(OMKCPWACAMPAIGNCONTACT.CAMPAIGN_ID)));
        selectQuery.addSelectClause(List.of(templateIdColumn, campaignContactTable.getPKColumn().count()));
        selectQuery.setWhereClause(WhereClauseUtil.connect(new WhereClause(Criteria.in(templateIdColumn, templateIds)), additionalCriteria, Connector.AND));
        selectQuery.addGroupByClause(new GroupByClause(templateIdColumn));

        return orgDataStore(orgId).getData(selectQuery).stream().collect(Collectors.toMap(
                row -> (Long) row.get(OMKCPWACAMPAIGN.TEMPLATE_ID),
                row -> (Integer) row.get(OMKCPWACAMPAIGNCONTACT.ID)));
    }
}

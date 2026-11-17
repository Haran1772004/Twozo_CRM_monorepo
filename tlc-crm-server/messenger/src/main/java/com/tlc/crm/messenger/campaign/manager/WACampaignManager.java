package com.tlc.crm.messenger.campaign.manager;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.messenger.campaign.model.WACampaign;
import com.tlc.crm.messenger.campaign.model.WATemplateMetrics;
import com.tlc.crm.messenger.campaign.manager.impl.WACampaignManagerImpl;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides WhatsApp campaign and metrics related DB actions.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface WACampaignManager extends ConfigManager<WACampaign> {

    /**
     * <p>
     * Gets the instance of {@link WACampaignManager}.
     * </p>
     *
     * @return {@link WACampaignManager}.
     */
    static WACampaignManager getInstance() {
        return WACampaignManagerImpl.getInstance();
    }

    /**
     * <p>
     * Gets the campaign contact table.
     * </p>
     *
     * @return The {@link Table}.
     */
    Table getCampaignContactTable();

    /**
     * <p>
     * Checks whether the given template id and business phone number id match the
     * persisted values for the campaign.
     * </p>
     *
     * @param orgId                 Represents the organization id.
     * @param waCampaign            Represents the campaign.
     * @return {@code true} when both ids match the stored values.
     */
    boolean matchesExistingTemplateAndPhoneNumber(final Long orgId, final WACampaign waCampaign);

    /**
     * <p>
     * Gets sent recipient counts for the given campaign ids.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param campaignIds Represents the campaign ids.
     * @return A map of campaign id to sent count.
     */
    Map<Long, Integer> getSentCounts(final Long orgId, final Collection<Long> campaignIds);

    /**
     * <p>
     * Gets delivered recipient counts for the given campaign ids.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param campaignIds Represents the campaign ids.
     * @return A map of campaign id to delivered count.
     */
    Map<Long, Integer> getDeliveredCounts(final Long orgId, final Collection<Long> campaignIds);

    /**
     * <p>
     * Gets read recipient counts for the given campaign ids.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param campaignIds Represents the campaign ids.
     * @return A map of campaign id to read count.
     */
    Map<Long, Integer> getReadCounts(final Long orgId, final Collection<Long> campaignIds);

    /**
     * <p>
     * Gets aggregated template metrics for the given template ids.
     * </p>
     *
     * @param orgId            Represents the organization id.
     * @param templateIds      Represents the template ids.
     * @return A map of template id to {@link WATemplateMetrics}.
     */
    Map<Long, WATemplateMetrics> getTemplateMetrics(final Long orgId, final Collection<Long> templateIds);
}

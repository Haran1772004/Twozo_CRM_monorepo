package com.tlc.crm.core.api.summaryInfo.model;

import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealStatistics;
import com.tlc.crm.core.api.deal.model.EmailSummary;
import com.tlc.crm.core.features.api.activity.model.ActivitySummary;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.Size;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

/**
 * Holds the additional fields info required for summary.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class SummaryInfo {

    private ActivitySummary activitySummary;
    private DealStatistics dealStatistics;

    @Size(max = 10, groups = {Group.Create.class, Group.Update.class}, message = "i18n.tag.limit.exceeded")
    private Collection<Tag> tags;

    private String webFormName;
    private EmailSummary emailSummary;
    private Map<Deal.State, Collection<Deal>> dealsByState;


    public ActivitySummary getActivitySummary() {
        return activitySummary;
    }

    public void setActivitySummary(final ActivitySummary activitySummary) {
        this.activitySummary = activitySummary;
    }

    public DealStatistics getDealStatistics() {
        return dealStatistics;
    }

    public void setDealStatistics(final DealStatistics dealStatistics) {
        this.dealStatistics = dealStatistics;
    }

    public Collection<Tag> getTags() {
        return tags;
    }

    public void setTags(final Collection<Tag> tags) {
        this.tags = tags;
    }

    public String getWebFormName() {
        return webFormName;
    }

    public void setWebFormName(final String webFormName) {
        this.webFormName = webFormName;
    }

    public EmailSummary getEmailSummary() {
        return emailSummary;
    }

    public void setEmailSummary(final EmailSummary emailSummary) {
        this.emailSummary = emailSummary;
    }

    public Map<Deal.State, Collection<Deal>> getDealsByState() {
        return dealsByState;
    }

    public void setDealsByState(final Map<Deal.State, Collection<Deal>> dealsByState) {
        this.dealsByState = dealsByState;
    }

    /**
     * <p>
     * Gets the {@link Collection} of {@link Deal} by states.
     * </p>
     *
     * @param states Represents the {@link Collection} of {@link Deal.State}
     * @return {@link Collection} of {@link Deal}
     */
    public Collection<Deal> getDealsByStates(final Collection<Deal.State> states) {
        final Collection<Deal> deals = new HashSet<>();

        for (final Deal.State state : states) {
            final Optional<Collection<Deal>> optionalDeals = Optional.ofNullable(dealsByState.get(state));

            optionalDeals.ifPresent(deals::addAll);
        }

        return deals;
    }
}

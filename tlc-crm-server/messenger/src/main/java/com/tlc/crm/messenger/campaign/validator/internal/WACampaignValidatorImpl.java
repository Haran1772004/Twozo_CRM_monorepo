package com.tlc.crm.messenger.campaign.validator.internal;

import com.tlc.crm.core.campaign.manager.CampaignFolderManager;
import com.tlc.crm.core.campaign.model.CampaignCategory;
import com.tlc.crm.core.campaign.model.CampaignFolder;
import com.tlc.crm.core.campaign.model.CampaignStatus;
import com.tlc.crm.core.segment.manager.SegmentManager;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.crm.entity.campaign.model.CampaignRecipient;
import com.tlc.crm.entity.marketinglist.manager.MarketingListManager;
import com.tlc.crm.entity.validator.ValidatorContext;

import com.tlc.crm.messenger.api.channel.manager.WABusinessAccountManager;
import com.tlc.crm.messenger.campaign.model.WACampaign;
import com.tlc.crm.messenger.campaign.validator.annotation.WACampaignValidator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;

/**
 * Validates the {@link WACampaign}
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WACampaignValidatorImpl implements ConstraintValidator<WACampaignValidator, WACampaign> {

    private final WABusinessAccountManager waBusinessAccountManager;
    private final MarketingListManager marketingListManager;
    private final SegmentManager segmentManager;
    private final CampaignFolderManager campaignFolderManager;

    public WACampaignValidatorImpl() {
        waBusinessAccountManager = WABusinessAccountManager.getInstance();
        marketingListManager = MarketingListManager.getInstance();
        segmentManager = SegmentManager.getInstance();
        campaignFolderManager = CampaignFolderManager.getInstance();
    }

    @Override
    public boolean isValid(final WACampaign waCampaign, final ConstraintValidatorContext constraintValidatorContext) {
        final ValidatorContext validatorContext = new ValidatorContext(constraintValidatorContext);
        final Long orgId = waCampaign.orgId();
        final CampaignStatus status = waCampaign.getStatus();
        final DateTime scheduledTime = waCampaign.getScheduledTime();
        final TimezoneInfo timezoneInfo = waCampaign.getTimezoneInfo();
        final Long businessPhoneNumberId = waCampaign.getBusinessPhoneNumberId();
        final Collection<CampaignRecipient> recipients = waCampaign.getRecipients();
        final Set<Long> marketingListIds = new HashSet<>();
        final Set<Long> segmentIds = new HashSet<>();

        if (Objects.nonNull(recipients)) {
            recipients.forEach(recipient -> {
                final CampaignRecipient.RecipientType recipientType = recipient.getRecipientType();

                if (CampaignRecipient.RecipientType.MARKETING_LIST == recipientType) {
                    marketingListIds.add(recipient.id());
                } else if (CampaignRecipient.RecipientType.SEGMENT == recipientType) {
                    segmentIds.add(recipient.id());
                }
            });
        }
        final boolean isMarketingListEmpty = marketingListIds.isEmpty();
        final boolean isSegmentsEmpty = segmentIds.isEmpty();
        final CampaignFolder folder = waCampaign.getFolder();

        if (Objects.isNull(businessPhoneNumberId) || !waBusinessAccountManager.exists(orgId, businessPhoneNumberId)) {
            addConstraintViolationMessage(validatorContext, "i18n.whatsapp.business.phone.number.not.found");
        }

        if (!status.isDraft() && isMarketingListEmpty && isSegmentsEmpty) {
            addConstraintViolationMessage(validatorContext, "i18n.campaign.recipients.required");
        }

        if (!isMarketingListEmpty && !marketingListManager.exists(orgId, marketingListIds)) {
            addConstraintViolationMessage(validatorContext, "i18n.marketing.list.not.found");
        }

        if (!isSegmentsEmpty && !segmentManager.exists(orgId, segmentIds)) {
            addConstraintViolationMessage(validatorContext, "i18n.segment.not.found");
        }

        if (Objects.nonNull(folder) && !campaignFolderManager.exists(orgId, CampaignCategory.WHATSAPP, folder.id())) {
            addConstraintViolationMessage(validatorContext, "i18n.campaign.folder.not.found");
        }

        if (status.isScheduled()) {
            if (Objects.isNull(timezoneInfo)) {
                addConstraintViolationMessage(validatorContext, "i18n.campaign.invalid.timezone");
            }
            if (Objects.isNull(scheduledTime) || scheduledTime.getUtcTimeStamp() <= System.currentTimeMillis()) {
                addConstraintViolationMessage(validatorContext, "i18n.campaign.invalid.scheduled.time");
            }
        }

        return !validatorContext.isErrorsFound();
    }

    /**
     * <p>
     * Adds constraint violation message to the validation context
     * </p>
     *
     * @param message Violation message
     */
    private void addConstraintViolationMessage(final ValidatorContext validatorContext, final String message) {
        validatorContext.setErrorsFound(true);
        validatorContext.getContext().buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
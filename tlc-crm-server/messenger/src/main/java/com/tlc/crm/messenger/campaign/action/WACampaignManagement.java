package com.tlc.crm.messenger.campaign.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.page.Page;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.internal.handler.impl.SocialEventRequestHandler;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.models.ViewType;
import com.tlc.crm.common.service.EventRequestService;
import com.tlc.crm.core.api.marketinglist.manager.MarketingListContactManager;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.core.campaign.converter.CampaignFilterPreferenceConverter;
import com.tlc.crm.core.campaign.fields.CampaignFields;
import com.tlc.crm.core.campaign.manager.CampaignManager;
import com.tlc.crm.core.campaign.model.Campaign;
import com.tlc.crm.core.campaign.model.CampaignCategory;
import com.tlc.crm.core.campaign.model.CampaignFilterUserPreference;
import com.tlc.crm.core.campaign.service.CampaignFilterPreferenceService;
import com.tlc.crm.core.segment.manager.SegmentContactManager;
import com.tlc.crm.entity.campaign.model.CampaignRecipient;
import com.tlc.crm.entity.action.AsyncBulkConfigAction;
import com.tlc.crm.entity.campaign.status.CampaignErrorCodes;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.messenger.api.channel.manager.WABusinessAccountManager;
import com.tlc.crm.messenger.api.whatsapp.converter.WATemplateConverter;
import com.tlc.crm.messenger.campaign.converter.WACampaignConverter;
import com.tlc.crm.messenger.campaign.converter.WATemplateFilterPreferenceConverter;
import com.tlc.crm.messenger.campaign.manager.WACampaignManager;
import com.tlc.crm.messenger.campaign.manager.WATemplateFilterPreferenceManager;
import com.tlc.crm.messenger.campaign.model.WACampaign;
import com.tlc.crm.messenger.campaign.model.WACampaignTemplateStatus;
import com.tlc.crm.messenger.campaign.model.WATemplateMetrics;
import com.tlc.crm.messenger.campaign.model.WATemplateFilterPreference;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.crm.messenger.internal.status.whatsapp.WAErrorCodes;
import com.tlc.crm.security.permission.annotation.authorizer.MarketingCampaignAuthorizer;
import com.tlc.social.client.service.whatsapp.WABusinessAccountService;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.model.WABusinessPhoneNumber;
import com.tlc.social.common.whatsapp.model.template.WATemplate;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Manages WhatsApp campaign web actions.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@MarketingCampaignAuthorizer
@WebAction(path = "/campaign/whatsapp/mgmt")
public class WACampaignManagement extends AsyncBulkConfigAction<WACampaign> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WACampaignManagement.class);

    private static final String CONFIG_ACTION_NAME = "waCampaign";

    private final WACampaignConverter waCampaignConverter;
    private final WACampaignManager waCampaignManager;
    private final WABusinessAccountService waBusinessAccountService;
    private final WABusinessAccountManager waBusinessAccountManager;
    private final CampaignManager campaignManager;
    private final MarketingListContactManager marketingListContactManager;
    private final SegmentContactManager segmentContactManager;
    private final WATemplateConverter waTemplateConverter;
    private final SocialEventRequestHandler socialEventRequestHandler;
    private final WATemplateFilterPreferenceConverter waTemplateFilterPreferenceConverter;
    private final WATemplateFilterPreferenceManager waTemplateFilterPreferenceManager;
    private final ConverterConfigBuilder converterConfigBuilder;
    private final CampaignFilterPreferenceConverter campaignFilterPreferenceConverter;
    private final CampaignFilterPreferenceService campaignFilterPreferenceService;

    /**
     * <p>
     * Constructs the {@link WACampaignManagement}.
     * </p>
     */
    public WACampaignManagement() {
        waCampaignConverter = WACampaignConverter.getInstance();
        waCampaignManager = WACampaignManager.getInstance();
        waBusinessAccountService = WABusinessAccountService.getInstance();
        waBusinessAccountManager = WABusinessAccountManager.getInstance();
        campaignManager = CampaignManager.getInstance();
        marketingListContactManager = MarketingListContactManager.getInstance();
        segmentContactManager = SegmentContactManager.getInstance();
        waTemplateConverter = WATemplateConverter.getInstance();
        socialEventRequestHandler = EventRequestService.get().getSocialEventRequestHandler();
        waTemplateFilterPreferenceConverter = WATemplateFilterPreferenceConverter.getInstance();
        waTemplateFilterPreferenceManager = WATemplateFilterPreferenceManager.getInstance();
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        campaignFilterPreferenceConverter = CampaignFilterPreferenceConverter.getInstance();
        campaignFilterPreferenceService = CampaignFilterPreferenceService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request Represents the {@link CrmRequest}
     * @param response Represents the {@link CrmResponse}
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(ActionFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(CampaignFields.DATA);
        final Long orgId = request.orgId();

        switch (type) {
            case "list" -> list(request, response);
            case "templateList" -> templateList(request, orgId, data, response);
            case "getTemplateFilter" -> getTemplateFilter(orgId, request, response);
        }
    }

    /**
     * <p>
     * Lists WhatsApp campaigns matching the filter criteria.
     * </p>
     *
     * @param request  Represents the {@link CrmRequest}.
     * @param response Represents the {@link CrmResponse}.
     */
    private void list(final CrmRequest request, final CrmResponse response) {
        final Long orgId = request.orgId();
        final JsonObject data = request.getRequestJson().getJsonObject(CampaignFields.DATA);
        final JsonObject criteriaJson = data.optJsonObject(CampaignFields.CRITERIA);
        final CampaignFilterUserPreference preference = campaignFilterPreferenceConverter.convert(converterConfigBuilder.build(request,
                false), criteriaJson);

        preference.setSearchText(data.optString(CampaignFields.SEARCH_TEXT, null));
        preference.setCategory(CampaignCategory.WHATSAPP);
        preference.setSortJson(data.optJsonObject(ListActionFields.SORT));
        preference.setStart(data.optInt(ListActionFields.START, 0));
        preference.setLimit(data.optInt(ListActionFields.LIMIT, 0));

        campaignFilterPreferenceService.upsert(preference);
        final long totalRecords = campaignManager.getTotalCount(orgId, preference);
        final Collection<Campaign> campaigns = totalRecords == 0 ? Collections.emptyList() : campaignManager.get(orgId, preference);

        response.put(CampaignFields.LIST, waCampaignConverter.convertListToJson(orgId, campaigns));
        response.put(CampaignFields.TOTAL_RECORDS, totalRecords);
        response.put(CampaignFields.HAS_MORE, preference.getStart() + campaigns.size() <= totalRecords);
    }

    /**
     * {@inheritDoc}
     * 
     * @return the config action name
     */
    @Override
    protected String getConfigActionName() {
        return CONFIG_ACTION_NAME;
    }

    /**
     * <p>
     * Loads the WhatsApp template filter preference for the current user.
     * </p>
     *
     * @param orgId    Represents the org Id
     * @param request  Represents the {@link CrmRequest}
     * @param response Represents the {@link CrmResponse}
     */
    private void getTemplateFilter(final Long orgId, final CrmRequest request, final CrmResponse response) {
        final WATemplateFilterPreference preference = waTemplateFilterPreferenceManager.get(orgId, request.userId());
        final JsonObject data = waTemplateFilterPreferenceConverter.convert(converterConfigBuilder.build(request, false), preference);
        final Long crmBusinessPhoneNumberId = Objects.nonNull(preference) ? preference.getBusinessPhoneNumberId() : null;

        if (Objects.isNull(crmBusinessPhoneNumberId)) {
            response.put(CampaignFields.FILTER, data);

            return;
        }

        waBusinessAccountManager.getBusinessAccountId(orgId, crmBusinessPhoneNumberId).ifPresentOrElse(businessAccountId -> {
            response.convertIntoAsync();

            final EventCallback<Long> eventCallback = new EventCallback<>() {

                @Override
                public void onSuccess(final Long templateCount) {
                    try {
                        data.put(WAFields.TEMPLATE_COUNT, templateCount);
                        response.put(CampaignFields.FILTER, data);
                    } catch (final Throwable throwable) {
                        final String errorDetail = throwable.getMessage();

                        LoggerUtil.logError(LOGGER, throwable, "Whatsapp campaign: Template count retrieved but error during conversion. OrgId: {}, Business phone number Id:{}, Reason: {}",
                                orgId, crmBusinessPhoneNumberId, errorDetail);
                        setErrorDescriptor(response, ErrorCode.get(WAErrorCodes.TEMPLATE_COUNT_RETRIEVAL_FAILED,
                                "i18n.whatsapp.template.count.retrieval.failed"), errorDetail);
                    } finally {
                        response.send();
                    }
                }

                @Override
                public void onFailure(final Throwable cause) {
                    final String errorDetail = cause.getMessage();

                    LoggerUtil.logError(LOGGER, cause, "Whatsapp campaign: Failed to retrieve template count. OrgId: {}, Business phone number Id:{}, Reason: {}",
                            orgId, crmBusinessPhoneNumberId, errorDetail);
                    setErrorDescriptor(response, ErrorCode.get(WAErrorCodes.TEMPLATE_COUNT_RETRIEVAL_FAILED,
                            "i18n.whatsapp.template.count.retrieval.failed"), errorDetail);
                    response.send();
                }
            };

            waBusinessAccountService.fetchTemplateCount(businessAccountId,
                    Arrays.stream(WACampaignTemplateStatus.values()).map(WACampaignTemplateStatus::toWATemplateStatus).toList(), eventCallback);
        }, () -> {
            throw ErrorCode.get(WAErrorCodes.BUSINESS_ACCOUNT_NOT_FOUND, "i18n.social.whatsapp.business.account.not.found");
        });
    }

    /**
     * <p>
     * Fetches the WhatsApp template list for campaigns.
     * </p>
     *
     * @param request  Represents the {@link CrmRequest}
     * @param orgId    Represents the organisation Id
     * @param data     Represents the data
     * @param response Represents the {@link CrmResponse}
     */
    private void templateList(final CrmRequest request, final Long orgId, final JsonObject data, final CrmResponse response) {
        final long businessPhoneNumberId = data.getLong(CampaignFields.BUSINESS_PHONE_NUMBER_ID);

        if (!waBusinessAccountManager.exists(orgId, businessPhoneNumberId)) {
            throw ErrorCode.get(WAErrorCodes.BUSINESS_PHONE_NUMBER_NOT_FOUND, "i18n.whatsapp.business.phone.number.not.found");
        }
        final WATemplateFilterPreference preference = waTemplateFilterPreferenceConverter.convert(converterConfigBuilder.build(request, false), data);

        waTemplateFilterPreferenceManager.update(preference);
        final ViewType view = preference.getViewType();
        final int start = data.optInt(MessengerFields.START, NumericalConstant.ONE);
        final int limit = data.optInt(MessengerFields.LIMIT, NumericalConstant.TWENTY);
        final String searchText = data.optString(CampaignFields.SEARCH_TEXT, null);
        final Collection<WACampaignTemplateStatus> statuses = preference.getStatuses();
        final Collection<Short> statusIds = Objects.isNull(statuses) || statuses.isEmpty()
                ? Arrays.stream(WACampaignTemplateStatus.values()).map(status -> status.toWATemplateStatus().getId()).toList()
                : statuses.stream().map(status -> status.toWATemplateStatus().getId()).toList();

        waBusinessAccountManager.getBusinessAccountId(orgId, businessPhoneNumberId).ifPresentOrElse(businessAccountId -> {
            response.convertIntoAsync();

            final EventCallback<Page<WATemplate>> eventCallback = new EventCallback<>() {

                @Override
                public void onSuccess(final Page<WATemplate> templatePage) {

                    socialEventRequestHandler.handle(() -> {
                        try {
                            final Collection<WATemplate> templates = templatePage.getData();
                            final JsonArray listArray = Json.array();
                            final int totalRecords = templatePage.getNumber();
                            final int size = templates.size();

                            if (ViewType.LIST == view) {
                                final List<Long> templateIds = templates.stream().map(WATemplate::id).toList();
                                final Map<Long, WATemplateMetrics> metricsMap = waCampaignManager.getTemplateMetrics(orgId, templateIds);

                                for (final WATemplate template : templates) {
                                    final WATemplateMetrics metrics = metricsMap.getOrDefault(template.id(),
                                            new WATemplateMetrics(0, 0, 0));
                                    final WACampaignTemplateStatus templateStatus = WACampaignTemplateStatus.fromWATemplateStatus(template.getStatus());
                                    final JsonObject templateNameJson = Json.object()
                                            .put(CampaignFields.NAME, template.getName())
                                            .put(WAFields.LANGUAGE, template.getLanguage().getDisplayLanguage())
                                            .put(CampaignFields.CATEGORY, template.getCategory().getId());

                                    listArray.put(Json.object()
                                            .put(CampaignFields.ID, template.id())
                                            .put(CampaignFields.TEMPLATE_NAME, templateNameJson)
                                            .put(CampaignFields.STATUS, templateStatus.getId())
                                            .put(CampaignFields.SENT, metrics.getSent())
                                            .put(CampaignFields.DELIVERED, Json.object().put(CampaignFields.PERCENTAGE, metrics.getDeliveredPercentage()))
                                            .put(CampaignFields.READ, Json.object().put(CampaignFields.PERCENTAGE, metrics.getReadPercentage())));
                                }
                            } else {
                                for (final WATemplate template : templates) {
                                    final WACampaignTemplateStatus templateStatus = WACampaignTemplateStatus.fromWATemplateStatus(template.getStatus());
                                    final JsonObject previewJson = waTemplateConverter.convert(template);

                                    listArray.put(Json.object()
                                            .put(CampaignFields.NAME, template.getName())
                                            .put(CampaignFields.ID, template.id())
                                            .put(WAFields.LANGUAGE, template.getLanguage().getDisplayLanguage())
                                            .put(CampaignFields.STATUS, templateStatus.getId())
                                            .put(CampaignFields.CATEGORY, template.getCategory().getId())
                                            .put(CampaignFields.PREVIEW, previewJson));
                                }
                            }

                            response.put(CampaignFields.TOTAL_RECORDS, totalRecords);
                            response.put(MessengerFields.SIZE, size);
                            response.put(CampaignFields.HAS_MORE, (start + size) < totalRecords);
                            response.put(CampaignFields.LIST, listArray);
                        } catch (final Throwable throwable) {
                            final String errorDetail = throwable.getMessage();

                            LoggerUtil.logError(LOGGER, throwable, "Whatsapp campaign: Templates retrieved but error during conversion. OrgId: {}, Reason: {}", orgId, errorDetail);
                            setErrorDescriptor(response, ErrorCode.get(WAErrorCodes.TEMPLATE_LIST_RETRIEVAL_FAILED, "i18n.whatsapp.templates.retrieval.failed"), errorDetail);
                        } finally {
                            response.send();
                        }
                    });
                }

                @Override
                public void onFailure(final Throwable cause) {
                    final String errorDetail = cause.getMessage();

                    LoggerUtil.logError(LOGGER, cause, "Whatsapp campaign: Failed to retrieve templates. OrgId: {}, Reason: {}", orgId, errorDetail);
                    setErrorDescriptor(response,
                            ErrorCode.get(WAErrorCodes.TEMPLATE_LIST_RETRIEVAL_FAILED, "i18n.whatsapp.templates.retrieval.failed"),
                            errorDetail);
                    response.send();
                }
            };

            waBusinessAccountService.fetchTemplatesWithComponents(businessAccountId, start, limit, searchText, preference.getCategoryIds(),
                    statusIds, preference.getLanguageIds(), ViewType.GRID == view, eventCallback);
        }, () -> {
            throw ErrorCode.get(WAErrorCodes.BUSINESS_ACCOUNT_NOT_FOUND, "i18n.social.whatsapp.business.account.not.found");
        });
    }

    /**
     * {@inheritDoc}
     * 
     * @return the config manager
     */
    @Override
    public ConfigManager<WACampaign> getConfigManager() {
        return waCampaignManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Represents the {@link CrmRequest}
     * @param jsonObject Represents the json object
     * @return the campaign
     */
    @Override
    public WACampaign convert(final CrmRequest request, final JsonObject jsonObject) {
        return waCampaignConverter.convertToModel(new CrmConverterData<>(request.getUser(), request.locale(), jsonObject));
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Represents the {@link CrmRequest}
     * @param jsonObject Represents the json object
     * @return the campaign
     */
    @Override
    public WACampaign convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        return waCampaignConverter.convertToModel(new CrmConverterData<>(request.getUser(), request.locale(),
                true, true, jsonObject));
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Represents the {@link CrmRequest}
     * @param waCampaign Represents the {@link WACampaign}
     * @return the json object
     */
    @Override
    protected JsonObject convert(final CrmRequest request, final WACampaign waCampaign) {
        return waCampaignConverter.convertToJson(new CrmConverterData<>(request.getUser(), request.locale(), waCampaign));
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Represents the {@link CrmRequest}
     * @param campaign    Represents the {@link WACampaign}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    protected void fetchAndDispatchUser(final CrmRequest request, final WACampaign campaign, final CrmResponse crmResponse) {
        final Long orgId = campaign.orgId();

        if (!campaign.getStatus().isInProgressOrCompleted()) {
            loadContactCounts(orgId, campaign);
        }
        final JsonObject campaignJson = convert(request, campaign);
        final Long crmBusinessPhoneNumberId = campaign.getBusinessPhoneNumberId();
        final Long templateId = campaign.getTemplateId();
        final JsonObject templateJson = campaignJson.getJsonObject(MessengerFields.TEMPLATE);

        if (Objects.isNull(crmBusinessPhoneNumberId) || Objects.isNull(templateId)) {
            campaignJson.forEach(crmResponse::put);

            return;
        }

        crmResponse.convertIntoAsync();
        final Map<Long, Long> phoneNumberIdMap = waBusinessAccountManager.getPhoneNumberIds(orgId, List.of(crmBusinessPhoneNumberId));
        final Long businessPhoneNumberId = phoneNumberIdMap.get(crmBusinessPhoneNumberId);
        final EventCallback<Map<Long, WABusinessPhoneNumber>> phoneCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final Map<Long, WABusinessPhoneNumber> phoneNumberMap) {
                final EventCallback<WATemplate> templateCallback = new EventCallback<>() {

                    @Override
                    public void onSuccess(final WATemplate template) {
                        try {
                            final WABusinessPhoneNumber businessPhoneNumber = phoneNumberMap.get(businessPhoneNumberId);
                            final JsonObject phoneNumberJson = Json.object()
                                    .put(MessengerFields.ID, crmBusinessPhoneNumberId)
                                    .put(MessengerFields.NAME, Objects.nonNull(businessPhoneNumber) ? businessPhoneNumber.getDisplayPhoneNumber() : null);

                            campaignJson.put(CampaignFields.BUSINESS_PHONE_NUMBER, phoneNumberJson);
                            waTemplateConverter.convert(template).forEach(templateJson::put);
                            campaignJson.forEach(crmResponse::put);
                        } catch (final Throwable throwable) {
                            final String errorDetail = throwable.getMessage();

                            LoggerUtil.logError(LOGGER, throwable, "Whatsapp campaign: Template retrieved but error during conversion. OrgId: {}, Reason: {}", orgId, errorDetail);
                            setErrorDescriptor(crmResponse,
                                    ErrorCode.get(WAErrorCodes.TEMPLATE_RETRIEVAL_FAILED, "i18n.whatsapp.template.retrieval.failed"),
                                    errorDetail);
                        } finally {
                            crmResponse.send();
                        }
                    }

                    @Override
                    public void onFailure(final Throwable cause) {
                        final String errorDetail = cause.getMessage();

                        LoggerUtil.logError(LOGGER, cause, "Whatsapp campaign: Failed to retrieve template. OrgId: {}, Reason: {}", orgId, errorDetail);
                        setErrorDescriptor(crmResponse,
                                ErrorCode.get(WAErrorCodes.TEMPLATE_RETRIEVAL_FAILED, "i18n.whatsapp.template.retrieval.failed"),
                                errorDetail);
                        crmResponse.send();
                    }
                };

                waBusinessAccountService.fetchTemplate(templateId, templateCallback);
            }

            @Override
            public void onFailure(final Throwable cause) {
                final String errorDetail = cause.getMessage();

                LoggerUtil.logError(LOGGER, cause, "Whatsapp campaign: Failed to fetch whatsapp phone numbers, OrgId: {}, Business phone number Id: {}, Message: {}",
                        orgId, crmBusinessPhoneNumberId, errorDetail);
                setErrorDescriptor(crmResponse,
                        ErrorCode.get(WAErrorCodes.FAILED_TO_FETCH_BUSINESS_PHONE_NUMBERS, "i18n.whatsapp.business.phone.numbers.retrieval.failed"),
                        errorDetail);
                crmResponse.send();
            }
        };

        waBusinessAccountService.fetchBusinessPhoneNumbers(List.of(businessPhoneNumberId), phoneCallback);
    }

    /**
     * <p>
     * Loads the contact counts to the recipients.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param campaign Represents the WhatsApp campaign.
     */
    private void loadContactCounts(final Long orgId, final WACampaign campaign) {
        final Collection<CampaignRecipient> recipients = campaign.getRecipients();

        if (Objects.isNull(recipients) || recipients.isEmpty()) {
            return;
        }
        final Set<Long> marketingListIds = new HashSet<>();
        final Set<Long> segmentIds = new HashSet<>();

        recipients.forEach(recipient -> {
            final CampaignRecipient.RecipientType recipientType = recipient.getRecipientType();

            if (CampaignRecipient.RecipientType.MARKETING_LIST == recipientType) {
                marketingListIds.add(recipient.id());
            } else if (CampaignRecipient.RecipientType.SEGMENT == recipientType) {
                segmentIds.add(recipient.id());
            }
        });
        final Map<Long, Long> marketingContactCounts = marketingListIds.isEmpty()
                ? Collections.emptyMap() : marketingListContactManager.getContactCountMap(orgId, marketingListIds);
        final Map<Long, Long> segmentContactCounts = segmentIds.isEmpty()
                ? Collections.emptyMap() : segmentContactManager.getContactCountMap(orgId, segmentIds);

        recipients.forEach(recipient -> {
            final CampaignRecipient.RecipientType recipientType = recipient.getRecipientType();

            if (CampaignRecipient.RecipientType.MARKETING_LIST == recipientType) {
                recipient.setContactCount(marketingContactCounts.getOrDefault(recipient.id(), recipient.getContactCount()));
            } else if (CampaignRecipient.RecipientType.SEGMENT == recipientType) {
                recipient.setContactCount(segmentContactCounts.getOrDefault(recipient.id(), recipient.getContactCount()));
            }
        });
    }

    /**
     * <p>
     * Sets {@link ErrorDescriptor} on the response.
     * </p>
     *
     * @param crmResponse Represents the {@link CrmResponse}.
     * @param errorCode   Represents the {@link ErrorCode}.
     * @param detail      Represents the error detail.
     */
    private void setErrorDescriptor(final CrmResponse crmResponse, final ErrorCode errorCode, final String detail) {
        crmResponse.setErrorCode(errorCode);
        crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, detail, null)));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organisation Id
     * @param userId    Represents the user Id
     * @param action    Represents the {@link Action}
     * @param campaigns Represents the {@link Collection<WACampaign>}
     * @return true if the details are valid, false otherwise
     */
    @Override
    protected boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<WACampaign> campaigns) {
        if (action.isCreateOrUpdate() && campaigns != null && !campaigns.isEmpty()) {
            final WACampaign campaign = campaigns.stream().findFirst().get();
            final CampaignCategory category = campaign.getCategory();
            final String campaignName = campaign.getName();

            if (action.isCreate()) {
                if (campaignManager.existsByName(orgId, category, null, campaignName)) {
                    throw ErrorCode.get(CampaignErrorCodes.CAMPAIGN_NAME_ALREADY_EXISTS, "i18n.campaign.name.already.exists");
                }
            }

            if (action.isUpdate()) {
                if (campaignManager.existsByName(orgId, category, campaign.id(), campaignName)) {
                    throw ErrorCode.get(CampaignErrorCodes.CAMPAIGN_NAME_ALREADY_EXISTS, "i18n.campaign.name.already.exists");
                }

                if (!waCampaignManager.matchesExistingTemplateAndPhoneNumber(orgId, campaign)) {
                    throw ErrorCode.get(CampaignErrorCodes.CAMPAIGN_TEMPLATE_OR_PHONE_NUMBER_UPDATE_RESTRICTED, "i18n.campaign.template.or.phone.number.update.restricted");
                }
            }
        }

        return true;
    }
}
package com.tlc.crm.core.action.deal;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmEventRequest;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.internal.CrmUserEntity;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.deal.model.converter.mobile.DealConverterMobileClient;
import com.tlc.crm.core.internal.event.CoreEvent;
import com.tlc.crm.core.validator.internal.DealDataValidator;
import com.tlc.crm.core.zapier.event.ZapEventType;
import com.tlc.crm.entity.forecastcategory.fields.ForecastCategoryFields;
import com.tlc.crm.entity.forecastcategory.manager.ForecastCategoryManager;
import com.tlc.crm.entity.forecastcategory.model.ForecastCategory;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.common.list.sort.SortConverter;
import com.tlc.crm.common.list.sort.api.manager.SortManager;
import com.tlc.crm.common.list.sort.models.KanbanSortConfig;
import com.tlc.crm.common.list.sort.models.SortConfig;
import com.tlc.crm.common.list.sort.models.SortCriteria;
import com.tlc.crm.common.list.sort.models.SortViewType;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.CompanyConverter;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.Deal.State;
import com.tlc.crm.core.api.deal.model.DealProgress;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.api.deal.model.DealKanbanSortFields;
import com.tlc.crm.core.api.deal.model.Participant;
import com.tlc.crm.core.api.deal.model.Pipeline;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.deal.model.StageProgress;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.api.deal.model.converter.PipelineStageConverter;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.field.deal.PipelineFields;
import com.tlc.crm.core.field.deal.PipelineStageFields;
import com.tlc.crm.core.internal.contact.handler.ContactStageUpdateHandler;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.core.internal.event.index.publisher.IndexDataPublisher;
import com.tlc.crm.core.page.DealMobilePageHandler;
import com.tlc.crm.core.workflow.handler.TriggerModelHandler;
import com.tlc.crm.core.workflow.model.ModelSource;
import com.tlc.crm.core.workflow.model.TriggerInfo;
import com.tlc.crm.core.workflow.publish.TriggerDataPublisher;
import com.tlc.crm.entity.action.EntityConfigAction;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.fields.type.FieldSubTypeKeys;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.list.filter.FilterCriteriaBuilder;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.SummaryOverview;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.models.type.converter.EmailConverter;
import com.tlc.crm.entity.models.type.converter.MonetaryConverter;
import com.tlc.crm.entity.models.type.converter.PhoneConverter;
import com.tlc.crm.entity.service.ActivityStatusIconService;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.entity.service.registry.ActivityStatusFetchServiceRegistry;
import com.tlc.crm.fields.cache.FieldSourceCache;
import com.tlc.crm.notification.app.Event;
import com.tlc.crm.security.permission.annotation.authorizer.FollowerAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.QuotaAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.iam.common.models.User;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.resolver.DateUtil;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages the {@link Deal} related web actions
 *
 * @author Sundar
 * @version 1.0
 */
@QuotaAuthorizer(subCategory = SubCategory.Type.COMMIT_DEAL)
@FollowerAuthorizer(subCategory = SubCategory.Type.DEAL_FOLLOWERS_RECORDS)
@SourceAuthorizer(subCategory = SubCategory.Type.DEALS, validateFieldPermission = true)
@WebAction(path = "/deal/mgmt")
public class DealManagement extends EntityConfigAction<Deal> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DealManagement.class);

    public static final String CONFIG_ACTION_NAME = "deal";
    private static final String DEAL_NOT_FOUND_I18N_KEY= "i18n.deal.not.found";

    private final static int DEFAULT_START_VALUE = 1;
    private final static int DEFAULT_LIMIT = 20;

    private final ConverterConfigBuilder converterConfigBuilder;
    private final DealConverter dealConverter;
    private final DealManager dealManager;
    private final PipelineManager pipelineManager;
    private final CompanyManager companyManager;
    private final ContactManager contactManager;
    private final PipelineStageManager pipelineStageManager;
    private final PipelineStageConverter pipelineStageConverter;
    private final ProductManager productManager;
    private final FieldService fieldService;
    private final CompanyConverter companyConverter;
    private final ContactConverter contactConverter;
    private final EmailConverter emailConverter;
    private final MonetaryConverter monetaryConverter;
    private final CrmUserConverter crmUserConverter;
    private final com.tlc.iam.client.UserService userService;
    private final UserService crmUserService;
    private final OrganizationService organizationService;
    private final OptionService optionService;
    private final FilterCriteriaBuilder filterCriteriaBuilder;
    private final IndexDataPublisher indexDataPublisher;
    private final TriggerDataPublisher triggerDataPublisher;
    private final TriggerModelHandler triggerModelHandler;
    private final DateUtil dateUtil;
    private final PhoneConverter phoneConverter;
    private final ContactStageUpdateHandler contactStageUpdateHandler;
    private final SortManager sortManager;
    private final SortConverter sortConverter;
    private final FieldSourceCache fieldSourceCache;
    private final DealConverterMobileClient dealConverterMobileClient;
    private final DealDataValidator dealDataValidator;
    private final ForecastCategoryManager forecastCategoryManager;

    public DealManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        dealConverter = DealConverter.getInstance();
        dealManager = DealManager.getInstance();
        companyManager = CompanyManager.getInstance();
        contactManager = ContactManager.getInstance();
        pipelineManager = PipelineManager.getInstance();
        pipelineStageManager = PipelineStageManager.getInstance();
        pipelineStageConverter = PipelineStageConverter.getInstance();
        productManager = ProductManager.getInstance();
        fieldService = FieldService.getSecureInstance();
        companyConverter = CompanyConverter.getInstance();
        contactConverter = ContactConverter.getInstance();
        emailConverter = EmailConverter.getInstance();
        monetaryConverter = MonetaryConverter.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
        userService = com.tlc.iam.client.UserService.getInstance();
        crmUserService = UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        optionService = OptionService.getInstance();
        indexDataPublisher = IndexDataPublisher.getInstance();
        filterCriteriaBuilder = FilterCriteriaBuilder.getInstance();
        triggerDataPublisher = TriggerDataPublisher.getInstance();
        triggerModelHandler = TriggerModelHandler.getInstance();
        dateUtil = DateUtil.getInstance();
        phoneConverter = PhoneConverter.getInstance();
        contactStageUpdateHandler = ContactStageUpdateHandler.getInstance();
        sortManager = SortManager.getInstance();
        sortConverter = SortConverter.getInstance();
        fieldSourceCache = FieldSourceCache.getInstance();
        dealConverterMobileClient = DealConverterMobileClient.getInstance();
        dealDataValidator = DealDataValidator.getInstance();
        forecastCategoryManager =  ForecastCategoryManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  the received request
     * @param response the response to be sent
     */
    @Override
    public void handleRequest(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString("type");
        final JsonObject data = requestJson.getJsonObject("data");
        final Long orgId = request.orgId();
        final TimezoneInfo timeZone = request.timeZone();
        final Long userId = request.userId();
        final CrmEventRequest crmEventRequest = (CrmEventRequest) request;
        final EventData eventData = crmEventRequest.getEventData();

        switch (type) {
            case "getDealsByStage" -> response.put("data", getDealsByPipelineStage(orgId, userId, data, timeZone, response));
            case "relatedEntities" -> response.put(DealFields.LIST, getRelatedEntities(orgId, data));
            case "getDealSummary" -> dealSummary(orgId, data, response);
            case "updateDealState" -> updateDealState(orgId, userId, data, response, eventData);
            case "getDealsByProduct" ->
                    response.put(DealFields.LIST, getDealsByProduct(orgId, data, response));
            case "summary" -> summary(orgId, data, request, response);
            case "summaryV2" -> summaryV2(orgId, data, request, response);
            case "getDealProgress" -> getDealProgress(orgId, data, response);
            case "updateDealStage" -> updateDealStage(orgId, userId, data, request.locale(), response, eventData);
            case "getContactsByDeals" -> response.put(DealFields.LIST, getDealParticipants(orgId, data));
            case "updatePipeline" -> changePipeline(orgId, userId, data, response, eventData);
            case "updateExpectedCloseDate" -> updateExpectedCloseDate(orgId, userId, data, response, eventData);
            case "getPrimaryEmail" -> response.put(DealFields.EMAIL, getPrimaryEmail(orgId, data.getLong(DealFields.ID)));
            case "getPrimaryPhone" -> response.put(DealFields.PHONE, getPrimaryPhone(orgId, data.getLong(DealFields.ID)));
            case "profile" -> profile(orgId, data, request, response);
            case "getDealsBySource" -> response.put(DealFields.LIST, getDealsBySource(orgId, data, response));
            case "updatePrimaryContact" -> updatePrimaryContact(orgId, data.getLong(DealFields.ID), userId, data.getLong(DealFields.CONTACT_ID));
            case "bUpdateArchiveState", "bulkCommit" -> bUpdate(crmEventRequest, response, eventData, false);
            case "updateArchiveState" -> updateArchiveState(orgId, userId, data.getLong(DealFields.ID), data.optBoolean(DealFields.IS_ARCHIVED));
            case "commitDeal" -> commitDeal(request, response, data, eventData);
            case "revokeDealCommit" -> revokeDealCommit(request, response, data, eventData);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    protected void handleMobileRequest(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString("type");
        final JsonObject data = requestJson.getJsonObject("data");
        final Long orgId = crmRequest.orgId();

        switch (type) {
            case "getDealsByContact" -> crmResponse.put(DealFields.LIST, getDealsByContact(orgId, crmResponse, data));
            case "getDealsByCompany" -> crmResponse.put(DealFields.LIST, getDealsByCompany(orgId, crmResponse, data));
            case "getDealsByProduct" -> crmResponse.put(DealFields.LIST, getDealsByProduct(orgId, crmResponse, data));
            case "summary" -> summary(data, crmRequest, crmResponse);
            default -> authorizedProcess(crmRequest, crmResponse);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link AbstractConfigManager} of Deal
     */
    @Override
    public ConfigManager<Deal> getConfigManager() {
        return dealManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Deal convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return dealConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link JsonObject} filled with the deal data
     */
    @Override
    public JsonObject convert(final CrmRequest request, final Deal deal) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, null);

        return dealConverter.convertModelToJson(deal, converterConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Deal} filled with the data
     */
    @Override
    public Deal convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return dealConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param crmResponse Represents the {@link CrmResponse}
     * @param orgId       Represents the organization id
     * @param userId      Represents the user id
     * @param deals       Represents the collection of {@link Deal}
     * @param eventData   Represents the {@link EventData}
     */
    @Override
    public void create(final CrmResponse crmResponse, final Long orgId, final Long userId, final Collection<Deal> deals, final EventData eventData) {
        final Collection<ErrorDescriptor> errors = validate(deals, Group.Create.class);
        final Action action = Action.CREATE;

        if (isValidDetails(orgId, userId, action, deals)) {
            setDealState(orgId, deals);
            validateDealState(deals, errors);

            if (errors.isEmpty()) {
                final ProcessOutcome processOutcome = getConfigManager().create(orgId, deals);

                loadResponseAndInvokePostAction(action, crmResponse, processOutcome, deals, eventData);
            } else {
                loadValidationErrors(action, crmResponse, errors);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param crmResponse Represents the {@link CrmResponse}
     * @param orgId       Represents the organization id
     * @param deals       An entity that holds the attributes of {@link Deal}
     * @param eventData   Represents the {@link EventData}
     */
    @Override
    public void update(final CrmResponse crmResponse, final Long orgId, final Long userId, final Collection<Deal> deals, final EventData eventData) {
        final Collection<ErrorDescriptor> errors = validate(deals, Group.Update.class);
        final Action action = Action.UPDATE;

        if (isValidDetails(orgId, userId, Action.UPDATE, deals)) {
            setDealState(orgId, deals);
            validateDealState(deals, errors);

            if (errors.isEmpty()) {
                preAction(action, deals, eventData);
                final ProcessOutcome processOutcome = getConfigManager().bulkUpdate(orgId, deals);

                loadResponseAndInvokePostAction(action, crmResponse, processOutcome, deals, eventData);
            } else {
                loadValidationErrors(action, crmResponse, errors);
            }
        }
    }

    /**
     * <p>
     * Updates the expected close date of the given deal
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param userId      Represents the user id
     * @param data        Holds the deal details
     * @param crmResponse Represents the {@link CrmResponse} details
     */
    private void updateExpectedCloseDate(final Long orgId, final long userId, final JsonObject data,
                                         final CrmResponse crmResponse, final EventData eventData) {
        final Long dealId = data.getLong(DealFields.ID);
        final Long expectedCloseDate = data.getLong(DealFields.EXPECTED_CLOSE_DATE);
        final Source source = Source.DEAL;
        final Deal deal = new Deal();

        deal.setOrgId(orgId);
        deal.setId(dealId);
        deal.setExpectedCloseDate(new Date(expectedCloseDate));
        final CrmUser crmuser = new CrmUserEntity();

        crmuser.setId(userId);
        deal.setUpdatedBy(crmuser);

        if (!dealManager.exists(orgId, dealId)) {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, DEAL_NOT_FOUND_I18N_KEY);
        }

        dealManager.updateExpectedCloseDate(orgId, userId, deal, expectedCloseDate);
        crmResponse.put(DealFields.UPDATED_RECORD, dealId);

        eventData.setValues(List.of(deal));
        eventData.setAction(Action.PARTIAL_UPDATE);
        triggerEvent(CoreEvent.QUOTA_DEAL_EVENT, eventData, Event.Type.POST);
        indexDataPublisher.publishIndexData(orgId, List.of(deal), Action.PARTIAL_UPDATE);
        triggerDataPublisher.publish(orgId, userId, List.of(deal), new TriggerInfo(source, Action.UPDATE, ModelSource.WEB_REQUEST,
                List.of(triggerModelHandler.create(orgId, source, eventData.getPreviousValues().get(dealId))), false));
    }

    /**
     * <p>
     * Updates the pipeline based on given deal id and sets the default stage for the given deal id
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param data        Holds the deal details
     * @param crmResponse Represents the {@link CrmResponse} details
     */
    private void changePipeline(final Long orgId, final Long userId, final JsonObject data,
                                final CrmResponse crmResponse, final EventData eventData) {
        final Long dealId = data.getLong(DealFields.ID);
        final Long pipelineId = data.getLong(DealFields.PIPELINE_ID);
        final Source source = Source.DEAL;
        final Deal deal = new Deal();

        deal.setId(dealId);
        deal.setOrgId(orgId);

        if (!dealManager.exists(orgId, dealId)) {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, DEAL_NOT_FOUND_I18N_KEY);
        }

        if (!pipelineManager.exists(orgId, pipelineId)) {
            throw ErrorCode.get(DealErrorCodes.PIPELINE_NOT_FOUND, "i18n.pipeline.not.found");
        }

        dealManager.changePipeline(orgId, userId, dealId, pipelineId, deal);
        crmResponse.put(DealFields.UPDATED_RECORD, dealId);

        eventData.setAction(Action.PARTIAL_UPDATE);
        eventData.setValues(List.of(deal));
        triggerEvent(CoreEvent.QUOTA_DEAL_EVENT, eventData, Event.Type.POST);
        indexDataPublisher.publishIndexData(orgId, List.of(deal), Action.PARTIAL_UPDATE);
        triggerDataPublisher.publish(orgId, userId, List.of(deal), new TriggerInfo(source, Action.UPDATE, ModelSource.WEB_REQUEST,
                List.of(triggerModelHandler.create(orgId, source, eventData.getPreviousValues().get(dealId))), false));
    }

    /**
     * Fetches the summary details of a deal.
     *
     * @param orgId    Represents an organization id.
     * @param data     Represents the request json data.
     * @param request  Represents the entity of {@link CrmRequest}.
     * @param response Represents the entity of {@link CrmResponse}.
     */
    private void summary(final Long orgId, final JsonObject data, final CrmRequest request, final CrmResponse response) {
        final Deal deal = get(orgId, data.getLong(DealFields.ID));

        dealManager.loadSummaryDetails(orgId, deal);
        fetchAndDispatchSummary(request, deal, data, response);
    }

    /**
     * Fetches the summary details of a deal.
     *
     * @param orgId    Represents an organization id.
     * @param data     Represents the request json data.
     * @param request  Represents the entity of {@link CrmRequest}.
     * @param response Represents the entity of {@link CrmResponse}.
     */
    private void summaryV2(final Long orgId, final JsonObject data, final CrmRequest request, final CrmResponse response) {
        final Deal deal = get(orgId, data.getLong(DealFields.ID));

        dealManager.loadSummaryInfo(orgId, deal);
        fetchAndDispatchSummaryV2(request, deal, data, response);
    }

    /**
     * Fetches the summary details of a deal.
     *
     * @param orgId    Represents an organization id.
     * @param data     Represents the request json data.
     * @param request  Represents the entity of {@link CrmRequest}.
     * @param response Represents the entity of {@link CrmResponse}.
     */
    private void profile(final Long orgId, final JsonObject data, final CrmRequest request, final CrmResponse response) {
        final long dealId = data.getLong(DealFields.ID);

        if (dealManager.exists(orgId, dealId)) {
            final Deal deal = dealManager.getProfile(orgId, dealId);

            dealManager.loadSummaryPreviewDetails(orgId, deal, true);
            fetchAndDispatchProfile(request, deal, data, response);
        } else {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found");
        }
    }

    /**
     * Fetches the summary details of a deal for mobile view
     *
     * @param jsonObject Represents the request json data.
     * @param crmRequest Represents the entity of {@link CrmRequest}.
     * @param response   Represents the entity of {@link CrmResponse}.
     */
    private void summary(final JsonObject jsonObject, final CrmRequest crmRequest, final CrmResponse response) {
        final Long orgId = crmRequest.orgId();
        final Deal deal = get(orgId, jsonObject.getLong(DealFields.ID));

        dealManager.loadSummaryDetails(orgId, deal);
        fetchAndDispatchSummary(crmRequest, jsonObject, deal, response);
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param deal       Represents the entity model
     * @param jsonObject Which contains the request data
     * @param users      Contains the collection of users
     * @return The {@link JsonObject} details
     */
    protected JsonObject convertSummary(final CrmRequest crmRequest, final JsonObject jsonObject, final Deal deal, final Collection<User> users) {
        if (!users.isEmpty()) {
            setUser(deal, users);
        }

        return dealConverter.convertSummaryIntoJson(deal, converterConfigBuilder.build(crmRequest, true, jsonObject, true), jsonObject.optBoolean(FieldKeys.IS_HIDDEN.getName(), false));
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param deal       Represents the {@link Deal}
     * @param jsonObject Which contains the request data
     * @param users      Contains the collection of users
     * @return The {@link JsonObject} details
     */
    @Override
    protected JsonObject convertSummaryV2(final CrmRequest crmRequest, final Deal deal, final JsonObject jsonObject, final Collection<User> users) {
        return dealConverter.summaryV2(crmRequest.orgId(), crmRequest.userId(), deal, converterConfigBuilder.build(crmRequest, true, jsonObject, true));
    }

    /**
     * Gets the pipeline stage history of the given deal id
     *
     * @param orgId    Represents an organisation id
     * @param data     Which contains the request data
     * @param response The response to be sent
     */
    private void getDealProgress(final Long orgId, final JsonObject data, final CrmResponse response) {
        final Long dealId = data.getLong(DealFields.ID);

        if (!dealManager.exists(orgId, dealId)) {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, DEAL_NOT_FOUND_I18N_KEY);
        }
        final DealProgress dealProgress = dealManager.getDealProgress(orgId, dealId);

        setDealProgress(dealProgress, response);
    }

    /**
     * Sets the {@link DealProgress}
     *
     * @param dealProgress An entity that holds the deal progress attributes
     * @param response     The response to be sent
     */
    private void setDealProgress(final DealProgress dealProgress, final CrmResponse response) {
        final JsonArray jsonArray = Json.array();
        final Optional<FieldInfo> optionalPipelineFieldInfo = fieldService.getDefaultFieldByAliasName(Source.DEAL, DealFields.PIPELINE);

        addStageProgressToJsonArray(dealProgress, jsonArray);
        response.put(DealFields.ID, dealProgress.getDealId());
        response.put(DealFields.PROGRESS, jsonArray);
        response.put(PipelineFields.PIPELINE_ID.getName(), dealProgress.getPipelineId());
        response.put(PipelineFields.NAME.getName(), dealProgress.getPipelineName());
        response.put(DealFields.EXPECTED_CLOSE_DATE, dateUtil.getTimeStamp(dealProgress.getExpectedCloseDate()));
        response.put(DealFields.PROBABILITY, dealProgress.getProbability());
        optionalPipelineFieldInfo.ifPresent(fieldInfo -> response.put(FieldKeys.FIELD_ID.getName(), fieldInfo.id()));
    }

    /**
     * Adds the deal stage progress into {@link JsonArray}
     *
     * @param dealProgress An entity that holds deal progress attributes
     * @param jsonArray    Holds the stage progress values
     */
    private void addStageProgressToJsonArray(final DealProgress dealProgress, final JsonArray jsonArray) {
        final Collection<StageProgress> stageProgresses = dealProgress.getStageProgresses();
        final TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        final long currentTime = Instant.now().toEpochMilli();
        final Map<Short, List<StageProgress>> stageProgressMap = stageProgresses.stream()
                .collect(Collectors.groupingBy(stageProgress -> stageProgress.getPipelineStage().getOrder()));
        final Collection<List<StageProgress>> stageProgressesList = stageProgressMap.values();
        String status = DealProgress.Status.COMPLETED.name();

        for (final List<StageProgress> stageProgressList : stageProgressesList) {
            final JsonObject jsonObject = Json.object();
            final PipelineStage pipelineStage = stageProgressList.get(0).getPipelineStage();
            final Double probability = pipelineStage.getProbability();
            long currentDay = 0;

            for (final StageProgress stageProgress : stageProgressList) {
                final DateTime startTime = stageProgress.getStartTime();
                final DateTime endTime = stageProgress.getEndTime();
                long differenceTime = 0;

                if (Objects.nonNull(startTime) && Objects.nonNull(endTime)) {
                    differenceTime = endTime.getTimeStamp() - startTime.getTimeStamp();
                } else if (Objects.nonNull(startTime)) {
                    differenceTime = currentTime - startTime.getTimeStamp();
                    status = DealProgress.Status.ACTIVE.name();

                    dealProgress.setProbability(probability);
                }
                currentDay += timeUnit.toDays(differenceTime);
            }
            jsonObject.put(PipelineStageFields.ID.getName(), pipelineStage.id());
            jsonObject.put(PipelineStageFields.NAME.getName(), pipelineStage.getName());
            jsonObject.put(DealFields.STATUS, status);
            jsonObject.put(PipelineStageFields.PROBABILITY.getName(), probability);

            if (!Objects.equals(DealProgress.Status.PENDING.name(), status)) {
                jsonObject.put(DealFields.DAYS, currentDay);
            }

            if (Objects.equals(DealProgress.Status.ACTIVE.name(), status)) {
                status = DealProgress.Status.PENDING.name();
            }
            jsonArray.put(jsonObject);
        }
    }

    /**
     * Fetches the deals by pipeline stage
     *
     * @param orgId    Represents an organisation ID
     * @param userId   Represents the user id
     * @param data     Which contains the request data
     * @param timeZone Represents the {@link TimezoneInfo}
     * @return {@link JsonObject} which contains the pipeline stage with deals
     */
    private JsonObject getDealsByPipelineStage(final long orgId, final long userId, final JsonObject data,
                                               final TimezoneInfo timeZone, final CrmResponse crmResponse) {
        final long time = System.currentTimeMillis();
        final long pipelineStageId = data.getLong(DealFields.PIPELINE_STAGE_ID);
        final PipelineStage pipelineStage = pipelineStageManager.get(orgId, pipelineStageId);
        final boolean needSummary = data.optBoolean(DealFields.NEED_SUMMARY);
        final int start = Math.max(data.optInt(DealFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(DealFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final JsonObject sortDealJson = data.optJsonObject(DealFields.SORT);
        final KanbanSortConfig kanbanSortConfig = new KanbanSortConfig(orgId, userId, fieldSourceCache.getSourceId(Source.DEAL.getName()));
        final Collection<FilterCriteria> filterCriteriaList = new ArrayList<>();
        final Counter counter = new Counter();

        kanbanSortConfig.setSortProvider(SortViewType.KANBAN_SORT);
        kanbanSortConfig.setStageId(pipelineStageId);

        if (Objects.nonNull(sortDealJson)) {
            final SortCriteria sortCriteria = new SortCriteria(orgId);
            final long fieldId = DealKanbanSortFields.getByName(sortDealJson.getString(DealFields.FIELD)).getId();

            sortCriteria.setFieldId(fieldId);
            sortCriteria.setSortType(SortType.getSortType(sortDealJson.getString(DealFields.TYPE)));
            sortCriteria.setOrder(counter.incrementAndGet());

            kanbanSortConfig.addSortCriteria(sortCriteria);
            kanbanSortConfig.setConfig(sortConverter.convertModelsToJson(kanbanSortConfig.getSortCriteriaList()).toString());
            sortManager.update(kanbanSortConfig);
        } else {
            final SortCriteria sortCriteria = new SortCriteria(orgId);
            final long fieldId = DealKanbanSortFields.getByName(DealFields.CREATED_AT).getId();

            sortCriteria.setFieldId(fieldId);
            sortCriteria.setSortType(SortType.DESCENDING);
            sortCriteria.setOrder(counter.incrementAndGet());

            kanbanSortConfig.addSortCriteria(sortCriteria);
            sortManager.remove(kanbanSortConfig);
        }

        if (data.containsKey(DealFields.FILTER_ID) || data.containsKey(DealFields.CRITERIA)
                || data.containsKey(DealFields.OWNER_ID)) {
            filterCriteriaList.addAll(filterCriteriaBuilder.constructFilterCriteria(orgId, userId, Provider.DEAL_LIST, data));
        }

        final JsonObject sortedDealsByPipelineStage = getSortedDealsByPipelineStage(pipelineStage, needSummary, start, limit, timeZone,
                filterCriteriaList, kanbanSortConfig);

        LOGGER.debug("Deal Kanban: Total time taken to process the kanban view took {}ms, Org ID: {}, User ID: {}, Stage ID: {}", System.currentTimeMillis() - time, orgId, userId, pipelineStageId);
        return sortedDealsByPipelineStage;
    }

    /**
     * <p>
     * Converts the collection of deals into json for the kanban view
     * </p>
     *
     * @param deals             Represents the collection of deals
     * @param pipelineStageName Represents the pipeline stage name
     * @param pipelineStageJson Represents the pipeline stage json object
     * @param dealActivitiesMap Represents the deals mapped by activity
     * @param timeZone          Represents the {@link TimezoneInfo}
     * @return {@link JsonObject} which contains the deals
     */
    private JsonObject convertDealsForKanbanView(final Collection<Deal> deals, final String pipelineStageName, final JsonObject pipelineStageJson,
                                                 final Map<Deal, Collection<Activity>> dealActivitiesMap, final TimezoneInfo timeZone) {
        final JsonArray dealsArray = Json.array();
        final long time = System.currentTimeMillis();

        if (!deals.isEmpty()) {
            final Optional<ActivityStatusIconService> optionalActivityStatusIconService =
                    ActivityStatusFetchServiceRegistry.getInstance().opt(FieldKeys.ACTIVITY_STATUS);
            final NumberFormat numberFormat = NumberFormat.getCompactNumberInstance();
            final String isCommitedKey = DealFields.IS_COMMITED;
            final String iconKey = ActivityFields.ICON.getName();

            for (final Deal deal : deals) {
                final JsonObject dealObject = dealConverter.convertDealForKanbanView(deal, numberFormat);
                final ForecastCategory forecastCategory = deal.getForecastCategory();

                loadPreviewDetailsIntoJson(deal.getPrimaryContact(), dealObject, false);
                dealObject.put(isCommitedKey, Objects.nonNull(forecastCategory) && forecastCategory.isCommittedCategory());
                loadPreviewDetailsIntoJson(deal.getPrimaryContact(), dealObject, false);
                optionalActivityStatusIconService.ifPresent(activityStatusIconService -> dealObject.put(
                        iconKey, activityStatusIconService.getIconTypeName(dealActivitiesMap.get(deal), timeZone)));
                dealsArray.put(dealObject);
            }
        }

        if (Objects.nonNull(pipelineStageName) && (State.WON.getName().equalsIgnoreCase(pipelineStageName) || State.LOST.getName().equalsIgnoreCase(pipelineStageName))) {
            pipelineStageJson.put(DealFields.STATUS, pipelineStageName);
        }
        pipelineStageJson.put(PipelineStageFields.DEALS.getName(), dealsArray);

        LOGGER.debug("Deal Kanban: Convert the deal data to JSON took {}ms", System.currentTimeMillis() - time);
        return pipelineStageJson;
    }

    /**
     * <p>
     * Sets the state of each {@link Deal} in the given collection based on the corresponding pipeline stage name
     * </p>
     *
     * @param orgId Represents the organization id
     * @param deals Represents the collection of {@link Deal}
     */
    private void setDealState(final long orgId, final Collection<Deal> deals) {
        final Map<Long, String> pipelineStageNames = pipelineStageManager.getPipelineStageNames(orgId,
                deals.stream().map(Deal::getPipelineStage).filter(Objects::nonNull).map(PipelineStage::id).collect(Collectors.toSet()));

        deals.forEach(deal -> {
            final PipelineStage pipelineStage = deal.getPipelineStage();

            if (Objects.nonNull(pipelineStage)) {
                final String pipelineStageName = pipelineStageNames.get(pipelineStage.id());

                if (Objects.nonNull(pipelineStageName)) {
                    deal.setState(Deal.State.getStateByName(pipelineStageName));
                }
            }
        });
    }

    /**
     * Gets the {@link JsonObject} which contains the pipeline stage data
     *
     * @param pipelineStage      Represents the {@link PipelineStage}
     * @param needSummary        Flag that represents the summary of pipeline and stages is need or not
     * @param start              Represents the start value
     * @param limit              Represents the limit value
     * @param timeZone           Represents the {@link TimezoneInfo}
     * @param filterCriteriaList Which contains the {@link Collection} of {@link FilterCriteria}
     * @return the {@link JsonObject} of pipeline stage
     */
    private JsonObject getSortedDealsByPipelineStage(final PipelineStage pipelineStage, final boolean needSummary, final int start,
                                                     final int limit, final TimezoneInfo timeZone, final Collection<FilterCriteria> filterCriteriaList,
                                                     final SortConfig sortConfig) {
        final String pipelineStageName = pipelineStage.getName();
        final JsonObject pipelineStageJson = pipelineStageConverter.convert(pipelineStage);

        final Map<Deal, Collection<Activity>> dealActivitiesMap = dealManager.getDealsAndLinkedActivitiesByPipelineAndStage(pipelineStage,
                needSummary, pipelineStageJson, start, limit, filterCriteriaList, sortConfig);

        if (Objects.nonNull(pipelineStageName) && (State.WON.getName().equalsIgnoreCase(pipelineStageName) || State.LOST.getName().equalsIgnoreCase(pipelineStageName))) {
            pipelineStageJson.put(DealFields.STATUS, pipelineStageName);
        }
        final Set<Deal> deals = dealActivitiesMap.keySet();

        if (Objects.nonNull(deals) && !deals.isEmpty()) {
            return convertDealsForKanbanView(deals, pipelineStageName, pipelineStageJson, dealActivitiesMap, timeZone);
        }
        return pipelineStageJson;
    }

    /**
     * Gets the related contact and company by deal
     *
     * @param orgId It represents organization id
     * @param data  Holds the field values
     * @return {@link JsonObject} Holds the field values of json
     */
    private JsonObject getRelatedEntities(final Long orgId, final JsonObject data) {
        final Long dealId = data.getLong(DealFields.ID);
        final JsonObject response = Json.object();

        if (!dealManager.exists(orgId, dealId)) {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, DEAL_NOT_FOUND_I18N_KEY);
        }
        final Deal deal = dealManager.getRelatedEntitiesByDealId(orgId, dealId);

        if (Objects.nonNull(deal)) {
            final Company company = deal.getCompany();
            final Contact primaryContact = deal.getPrimaryContact();

            if (Objects.nonNull(primaryContact)) {
                response.put(DealFields.PRIMARY_CONTACT, contactConverter.convertPartialData(primaryContact));
            }

            if (Objects.nonNull(company)) {
                response.put(DealFields.COMPANY, companyConverter.convertPartialData(company));
            }
        }

        return response;
    }

    /**
     * Dispatches the response by retrieving the user details using callback.
     *
     * @param deal        Refers the {@link Deal} details.
     * @param crmResponse Refers the {@link CrmResponse} to be sent.
     */
    private void fetchUsersForDealSummary(final Deal deal, final CrmResponse crmResponse) {
        final Collection<Long> userIds = deal.userIds();
        final Long orgId = deal.orgId();

        if (userIds.isEmpty()) {
            crmResponse.put(DealFields.DEAL_SUMMARY, convertDealToJsonForSummary(deal));
        } else {
            crmResponse.convertIntoAsync();

            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    final Map<Long, User> crmUsers = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                    final CrmUser salesOwner = deal.getSalesOwner();

                    salesOwner.setUser(crmUsers.get(salesOwner.getIamId()));
                    crmResponse.put(DealFields.DEAL_SUMMARY, convertDealToJsonForSummary(deal));
                    crmResponse.send();
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    sendError(crmResponse, null, ErrorCode.get(DealErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                }
            };

            userService.fetchUsers(organizationService.getIamOrgId(orgId), crmUserService.getUsers(orgId, userIds).stream()
                    .map(CrmUser::getIamId).collect(Collectors.toSet()), usersFetchCallBack);
        }
    }

    /**
     * <p>
     * Gets deal summary data by deal id
     * </p>
     *
     * @param orgId       It represents organization id
     * @param data        Which contains the request data
     * @param crmResponse Refers the response to be sent
     */
    private void dealSummary(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final Long dealId = data.getLong(DealFields.ID);

        if (!dealManager.exists(orgId, dealId)) {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, DEAL_NOT_FOUND_I18N_KEY);
        }
        final Deal deal = dealManager.getDealSummary(orgId, dealId);

        if (Objects.nonNull(deal)) {
            fetchUsersForDealSummary(deal, crmResponse);
        }
    }

    /**
     * Converts the {@link Deal} into {@link JsonObject} to provide a summary in a kanban view.
     *
     * @param deal Refers the {@link Deal}.
     * @return The {@link JsonObject} of converted deal details.
     */
    private JsonObject convertDealToJsonForSummary(final Deal deal) {
        final JsonObject response = Json.object();
        final JsonObject pipelineJson = Json.object();
        final Pipeline pipeline = deal.getPipeline();
        final Contact contact = deal.getPrimaryContact();
        final Company dealCompany = deal.getCompany();

        if (Objects.nonNull(contact)) {
            final JsonObject contactJson = Json.object();
            final Company contactCompany = contact.getCompany();
            final Collection<Email> contactEmails = contact.getEmails();
            final Collection<Phone> contactPhones = contact.getPhones();
            final Email contactPrimaryEmail = contact.getPrimaryEmail();
            final Phone primaryPhone = contact.getPrimaryPhone();

            pipelineJson.put(PipelineFields.ID.getName(), pipeline.id());
            pipelineJson.put(PipelineFields.NAME.getName(), pipeline.getName());
            contactJson.put(ContactFields.ID, contact.id());
            contactJson.put(ContactFields.FULL_NAME, contact.getFullName());
            contactJson.put(ContactFields.FACEBOOK, contact.getFacebook());
            contactJson.put(ContactFields.LINKEDIN, contact.getLinkedIn());
            contactJson.put(ContactFields.TWITTER, contact.getTwitter());

            if (Objects.nonNull(contactCompany)) {
                contactJson.put(ContactFields.COMPANY, companyConverter.convertPartialData(contactCompany));
            }

            if (Objects.nonNull(contactPrimaryEmail)) {
                final JsonObject primaryEmailJson = Json.object();

                primaryEmailJson.put(ContactFields.ID, contactPrimaryEmail.id());
                primaryEmailJson.put(ContactFields.TYPE, contactPrimaryEmail.getType());
                primaryEmailJson.put(ContactFields.VALUE, contactPrimaryEmail.getValue());

                contactJson.put(ContactFields.PRIMARY_EMAIL, primaryEmailJson);
            }

            if (Objects.nonNull(primaryPhone)) {
                final JsonObject primaryPhoneJson = Json.object();

                primaryPhoneJson.put(ContactFields.ID, primaryPhone.id());
                primaryPhoneJson.put(ContactFields.NUMBER, primaryPhone.getNumberWithCode());

                contactJson.put(ContactFields.PRIMARY_PHONE, primaryPhoneJson);
            }

            if (Objects.nonNull(contactEmails) && !contactEmails.isEmpty()) {
                final JsonArray emails = Json.array();
                final String idKey = FieldSubTypeKeys.ID.getName();
                final String valueKey = FieldSubTypeKeys.VALUE.getName();

                for (final Email email : contactEmails) {
                    final JsonObject emailJson = Json.object();

                    emailJson.put(idKey, email.id());
                    emailJson.put(valueKey, email.getValue());
                    emails.put(emailJson);
                }
                contactJson.put(ContactFields.EMAILS, emails);
            }

            if (Objects.nonNull(contactPhones) && !contactPhones.isEmpty()) {
                final JsonArray phones = Json.array();
                final String idKey = FieldSubTypeKeys.ID.getName();
                final String numberKey = FieldSubTypeKeys.NUMBER.getName();

                for (final Phone phone : contactPhones) {
                    final JsonObject phoneJson = Json.object();

                    phoneJson.put(idKey, phone.id());
                    phoneJson.put(numberKey, phone.getNumber());
                    phones.put(phoneJson);
                }
                contactJson.put(ContactFields.PHONES, phones);
            }
            response.put(DealFields.PRIMARY_CONTACT, contactJson);
        }
        response.put(DealFields.ID, deal.id());
        response.put(DealFields.TITLE, deal.getTitle());
        response.put(DealFields.SALES_OWNER, crmUserConverter.convert(deal.getSalesOwner(), true, false));
        response.put(DealFields.EXPECTED_CLOSE_DATE, dateUtil.getTimeStamp(deal.getExpectedCloseDate()));
        response.put(DealFields.DEAL_VALUE, monetaryConverter.convert(new ConverterConfig(false, false), deal.getDealValue()));
        response.put(DealFields.PIPELINE, pipelineJson);
        response.put(DealFields.PIPELINE_STAGE, pipelineStageConverter.convert(deal.getPipelineStage()));

        if (Objects.nonNull(dealCompany)) {
            response.put(DealFields.COMPANY, companyConverter.convertPartialData(dealCompany));
        }

        return response;
    }

    /**
     * Sets the response with deal overview details such as activity type and active users etc.
     *
     * @param orgId    Refers the organization id.
     * @param data     Refers the request json.
     * @param response Refers the {@link CrmResponse} to be sent.
     */
    @Override
    public void overview(final long orgId, final JsonObject data, final CrmResponse response) {
        final long dealId = data.getLong(DealFields.ID);

        if (dealManager.exists(orgId, dealId)) {
            final SummaryOverview summaryOverview = dealManager.getSummaryOverview(orgId, dealId, Source.DEAL);

            fetchUsersForOverview(orgId, summaryOverview, response);
        } else {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, DEAL_NOT_FOUND_I18N_KEY);
        }
    }

    /**
     * Gets the {@link JsonArray} of deal data by product
     *
     * @param orgId    Represents organization id
     * @param data     Represents the request data
     * @param response Represents the response data
     * @return {@link JsonArray} which contains the product with deals
     */
    private JsonArray getDealsByProduct(final Long orgId, final JsonObject data, final CrmResponse response) {
        final long productId = data.getLong(DealFields.PRODUCT_ID);
        final int start = Math.max(data.optInt(DealFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(DealFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);

        if (productManager.exists(orgId, productId)) {
            final JsonArray dealArray = Json.array();
            final String stateIdsKey = DealFields.STATE_IDS;
            final Collection<Long> stateIds = new HashSet<>();

            if (data.containsKey(stateIdsKey)) {
                data.getJsonArray(stateIdsKey).forEach(stateId -> stateIds.add((Long) stateId));
            }
            final long dealCount = dealManager.getDealsCountByProduct(orgId, productId, stateIds);

            if (dealCount > 0) {
                final Map<String, SortType> fieldSortMap = fetchSortFields(data);

                if (fieldSortMap.isEmpty()) {
                    fieldSortMap.put(DealFields.TITLE, SortType.ASCENDING);
                }
                final Collection<Deal> deals = dealManager.getDealsByProduct(orgId, productId, start, limit, stateIds, fieldSortMap);

                if (!deals.isEmpty()) {

                    for (final Deal deal : deals) {
                        dealArray.put(convertDealForProduct(deal));
                    }
                }
            }
            response.put(DealFields.TOTAL_RECORDS, dealCount);
            response.put(DealFields.HAS_MORE, (start + limit) <= dealCount);

            return dealArray;
        } else {
            throw ErrorCode.get(DealErrorCodes.PRODUCT_NOT_FOUND, "i18n.product.not.found");
        }
    }

    /**
     * Gets the {@link JsonArray} of deal data by source
     *
     * @param orgId    Represents organization id
     * @param data     Represents the request data
     * @param response Represents the response data
     * @return {@link JsonArray} which contains the deals
     */
    private JsonArray getDealsBySource(final Long orgId, final JsonObject data, final CrmResponse response) {
        final long entityId = data.getLong(FieldKeys.ENTITY_ID.getName());
        final Source source = Source.get(data.getString(FieldKeys.SOURCE.getName()));
        final int start = Math.max(data.optInt(DealFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(DealFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);

        if (isEntityExists(orgId, source, entityId)) {
            final JsonArray dealArray = Json.array();
            final String stateIdsKey = DealFields.STATE_IDS;
            final Collection<Long> stateIds = new HashSet<>();

            if (data.containsKey(stateIdsKey)) {
                data.getJsonArray(stateIdsKey).forEach(stateId -> stateIds.add((Long) stateId));
            }
            final long dealCount = dealManager.getDealsCountBySource(orgId, entityId, stateIds, source);

            if (dealCount > 0) {
                final Collection<Deal> deals = dealManager.getDealsBySource(orgId, entityId, start, limit, stateIds, source);

                if (!deals.isEmpty()) {

                    for (final Deal deal : deals) {
                        dealArray.put(dealConverter.convertDealIntoJson(deal));
                    }
                }
            }
            response.put(DealFields.TOTAL_RECORDS, dealCount);
            response.put(DealFields.HAS_MORE, (start + limit) <= dealCount);

            return dealArray;
        } else {
            throw ErrorCode.get(CommonErrorCodes.ENTITY_NOT_FOUND, "i18n.entity.not.found");
        }
    }

    /**
     * <p>
     * Checks whether the entity exists
     * </p>
     *
     * @param orgId     Represents the organisation Id
     * @param source    Represents the {@link Source}
     * @param entityId  Represents the entity Id
     * @return True if the entity exists, false otherwise
     */
    private boolean isEntityExists(final Long orgId, final Source source, final Long entityId) {
        return switch (source) {
            case CONTACT -> contactManager.exists(orgId, entityId);
            case COMPANY -> companyManager.exists(orgId, entityId);
            case PRODUCT -> productManager.exists(orgId, entityId);
            default -> false;
        };
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  It represents organization id
     * @param userId Represents the user id.
     * @param action which contains data
     * @param deals  Collection of {@link Deal}
     * @return boolean Return true means is valid details
     */
    @Override
    protected final boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<Deal> deals) {
        if (Objects.nonNull(deals) && !deals.isEmpty()) {
            switch (action) {
                case CREATE, UPDATE -> dealDataValidator.validate(orgId, deals);
            }
        }
        return true;
    }

    /**
     * <p>
     * Updates the given deal state fields
     * </p>
     *
     * @param orgId       Organization id
     * @param userId
     * @param data        Holds the field values
     * @param crmResponse the response to be sent
     */
    private void updateDealState(final Long orgId, final Long userId, final JsonObject data,
                                 final CrmResponse crmResponse, final EventData eventData) {
        final Deal deal = new Deal();
        final Long dealId = data.getLong(DealFields.ID);

        deal.setOrgId(orgId);
        deal.setId(dealId);

        if (!dealManager.exists(orgId, dealId)) {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, DEAL_NOT_FOUND_I18N_KEY);
        }
        final String stateKey = DealFields.STATE;

        if (data.containsKey(stateKey)) {
            final int stateValue = data.optInt(stateKey, State.OPEN.getId());
            final State dealState = State.getStateById(stateValue);
            deal.setState(dealState);
            final Pipeline pipeline = new Pipeline();

            pipeline.setOrgId(orgId);
            pipeline.setId(dealManager.getPipelineId(orgId, dealId));
            deal.setPipeline(pipeline);
            final PipelineStage pipelineStage = new PipelineStage();

            pipelineStage.setOrgId(orgId);
            pipelineStage.setId(pipelineStageManager.getPipelineStageId(orgId, deal.getState(), dealId));
            deal.setPipelineStage(pipelineStage);
        }
        updateDealState(orgId, userId, data, deal, crmResponse, eventData);
    }

    /**
     * Checks whether the deal state is won, lost or open if its won need to set won reason and won time
     * if its lost need to set lost reason and lost time.
     *
     * @param deals Represents the {@link Collection} of deals
     */
    private void validateDealState(final Collection<Deal> deals, final Collection<ErrorDescriptor> errors) {
        for (final Deal deal : deals) {
            final State state = deal.getState();
            final Option lostReason = deal.getLostReason();
            final Option wonReason = deal.getWonReason();
            final DateTime closedTime = deal.getClosedTime();

            switch (state) {
                case WON -> {
                    if (Objects.isNull(closedTime)) {
                        errors.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.DATA_VALIDATION_FAILED, "i18n.deal.closed.time.not.found"),
                                null, DealFields.DEAL_CLOSED_ON));
                    }

                    if (Objects.isNull(wonReason)) {
                        errors.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.DATA_VALIDATION_FAILED, "i18n.deal.won.reason.not.found"),
                                null, DealFields.WON_REASON));
                    }

                    if (Objects.nonNull(lostReason)) {
                        deal.setLostReason(null);
                    }
                }
                case LOST -> {
                    if (Objects.isNull(lostReason)) {
                        errors.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.DATA_VALIDATION_FAILED, "i18n.deal.lost.reason.not.found"),
                                null, DealFields.LOST_REASON));
                    }

                    if (Objects.isNull(closedTime)) {
                        errors.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.DATA_VALIDATION_FAILED, "i18n.deal.closed.time.not.found"),
                                null, DealFields.DEAL_CLOSED_ON));
                    }

                    if (Objects.nonNull(wonReason)) {
                        deal.setWonReason(null);
                    }
                }
                default -> {
                    if (Objects.nonNull(closedTime) || Objects.nonNull(wonReason) || Objects.nonNull(lostReason)) {
                        deal.setClosedTime(null);
                        deal.setWonReason(null);
                        deal.setLostReason(null);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Updates the pipeline stage of the given deal.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param data        Entity of the {@link JsonObject} which contains the request data.
     * @param locale      Refers the {@code Locale}.
     * @param crmResponse Entity of {@link CrmResponse}
     */
    private void updateDealStage(final Long orgId, final Long userId, final JsonObject data, final Locale locale,
                                 final CrmResponse crmResponse, final EventData eventData) {
        final Long dealId = data.getLong(DealFields.ID);
        final Long pipelineStageId = data.getLong(DealFields.PIPELINE_STAGE_ID);
        final Long pipelineId = data.getLong(DealFields.PIPELINE_ID);
        final Deal deal = dealManager.getPartialDealById(orgId, dealId);
        final PipelineStage pipelineStage = pipelineStageManager.getPartialPipelineStageById(orgId, pipelineStageId);

        if (Objects.isNull(deal)) {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, DEAL_NOT_FOUND_I18N_KEY);
        }

        if (Objects.isNull(pipelineStage) || !pipelineStage.getPipelineId().equals(pipelineId)) {
            throw ErrorCode.get(DealErrorCodes.INVALID_PIPELINE_OR_STAGE, "i18n.pipeline.invalid.id");
        }

        final String pipelineStageName = pipelineStage.getName();
        final Collection<Deal> deals;

        if (pipelineStageName.equalsIgnoreCase(State.LOST.getName()) || pipelineStageName.equalsIgnoreCase(State.WON.getName())) {
            final State state = State.getStateByName(pipelineStageName);

            deal.setState(state);
            deal.setPipelineStage(pipelineStage);
            updateDealState(orgId, userId, data, deal, crmResponse, eventData);
            deals = List.of(deal);
        } else {
            final Source source = Source.DEAL;

            dealManager.updatePipelineStage(orgId, userId, pipelineId, pipelineStageId, dealId);
            deal.setPipelineStage(pipelineStage);
            deal.setState(State.OPEN);
            deals = List.of(deal);
            crmResponse.put(DealFields.UPDATED_RECORD, dealId);
            triggerDataPublisher.publish(orgId, userId, deals, new TriggerInfo(source, Action.UPDATE, ModelSource.WEB_REQUEST,
                    List.of(triggerModelHandler.create(orgId, source, eventData.getPreviousValues().get(dealId))), false));

            eventData.setLocale(locale);
            eventData.setValues(deals);
            eventData.setAction(Action.PARTIAL_UPDATE);
            eventData.setApiType("updateDealStage");
            triggerEvent(CoreEvent.QUOTA_DEAL_EVENT, eventData, Event.Type.POST);
            triggerEvent(ZapEventType.TRIGGER, eventData, Event.Type.POST);
        }
        indexDataPublisher.publishIndexData(orgId, deals, Action.PARTIAL_UPDATE);
    }

    /**
     * <p>
     * Updates the given deal state fields
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param userId      Represents the user id
     * @param data        Entity of the {@link JsonObject} which contains the request data.
     * @param deal        Entity of {@link Deal}
     * @param crmResponse Entity of {@link CrmResponse}
     */
    private void updateDealState(final Long orgId, final Long userId, final JsonObject data, final Deal deal,
                                 final CrmResponse crmResponse, final EventData eventData) {
        final String lostReasonKey = DealFields.LOST_REASON;
        final String wonReasonKey = DealFields.WON_REASON;
        final String dealClosedOnKey = DealFields.DEAL_CLOSED_ON;
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, lostReasonKey, wonReasonKey);

        deal.setOrgId(orgId);
        deal.setLostReason(data.containsKey(lostReasonKey) && fields.containsKey(lostReasonKey)
                ? dealConverter.buildOption(orgId, fields.get(lostReasonKey).id(), data.getLong(lostReasonKey), false) : deal.getLostReason());
        deal.setWonReason(data.containsKey(wonReasonKey) && fields.containsKey(wonReasonKey)
                ? dealConverter.buildOption(orgId, fields.get(wonReasonKey).id(), data.getLong(wonReasonKey), false) : deal.getWonReason());

        if (data.containsKey(dealClosedOnKey)) {
            deal.setClosedTime(new DateTime(data.getLong(dealClosedOnKey)));
        }
        final Collection<ErrorDescriptor> errors = new ArrayList<>();
        final Collection<Deal> dealList = List.of(deal);

        validateDealState(dealList, errors);

        if (errors.isEmpty()) {
            final Long dealId = deal.id();
            final Option lostReasonOption = deal.getLostReason();
            final Option wonReasonOption = deal.getWonReason();

            if (Objects.nonNull(lostReasonOption) && !optionService.exists(orgId, lostReasonOption.getFieldId(), lostReasonOption.id())) {
                throw ErrorCode.get(DealErrorCodes.INVALID_OPTION, "i18n.deal.fields.state.lost.reason.option.invalid");
            }

            if (Objects.nonNull(wonReasonOption) && !optionService.exists(orgId, wonReasonOption.getFieldId(), wonReasonOption.id())) {
                throw ErrorCode.get(DealErrorCodes.INVALID_OPTION, "i18n.deal.fields.state.won.reason.option.invalid");
            }
            final Source source = Source.DEAL;
            final Action updateAction = Action.UPDATE;
            final Action partialUpdateAction = Action.PARTIAL_UPDATE;

            deal.setUpdatedBy(crmUserService.getUser(orgId, userId));
            dealManager.updateStateAndStage(orgId, userId, deal, pipelineStageManager.getPipelineStageId(orgId, deal.getState(), dealId));
            crmResponse.put(DealFields.UPDATED_RECORD, dealId);
            eventData.setAction(Action.PARTIAL_UPDATE);

            eventData.setValues(dealList);
            triggerEvent(CoreEvent.QUOTA_DEAL_EVENT, eventData, Event.Type.POST);
            indexDataPublisher.publishIndexData(orgId, dealList, partialUpdateAction);
            triggerDataPublisher.publish(orgId, userId, dealList, new TriggerInfo(source, updateAction, ModelSource.WEB_REQUEST,
                    List.of(triggerModelHandler.create(orgId, source, eventData.getPreviousValues().get(dealId))), false));
            contactStageUpdateHandler.handle(orgId, partialUpdateAction, dealList);
        } else {
            final ErrorCode errorCode = ErrorCode.get(DealErrorCodes.INVALID_DEAL_STATE, "i18n.deal.state.invalid");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode), errors));
        }
    }

    /**
     * Retrieves a {@link JsonArray} of participants associated with a deals in a particular organization and linked to a specific user.
     *
     * @param orgId Represents an organization id
     * @param data  Represents the {@link JsonObject} which holds the request data
     * @return {@link JsonArray} of related deal contacts
     */
    private JsonArray getDealParticipants(final long orgId, final JsonObject data) {
        final Set<Long> dealIds = data.getJsonArray(DealFields.IDS).toList().stream()
                .map(id -> (Long) id).collect(Collectors.toUnmodifiableSet());

        if (!dealManager.exists(orgId, dealIds)) {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, DEAL_NOT_FOUND_I18N_KEY);
        }
        final Collection<Participant> participants = dealManager.getParticipantsByDeals(orgId, dealIds);
        final JsonArray participantArray = Json.array();
        final String contactIdKey = DealFields.CONTACT_ID;
        final String nameKey = DealFields.NAME;
        final String dealIdKey = DealFields.DEAL_ID;

        for (final Participant participant : participants) {
            final JsonObject jsonObject = Json.object();
            final Contact contact = participant.getContact();

            jsonObject.put(contactIdKey, participant.getContactId());
            jsonObject.put(nameKey, contact.getFullName());
            jsonObject.put(dealIdKey, participant.getDealId());
            final Email primaryEmail = contact.getPrimaryEmail();

            if (Objects.nonNull(primaryEmail)) {
                jsonObject.put(DealFields.EMAIL, emailConverter.convert(new ConverterConfig(false, false), primaryEmail));
            }
            participantArray.put(jsonObject);
        }

        return participantArray;
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param deal       Represents the entity model
     * @param data       Holds the entity details
     * @param users      Contains the collection of users
     * @return The {@link JsonObject} details
     */
    @Override
    protected JsonObject convertSummary(final CrmRequest crmRequest, final Deal deal, final JsonObject data, final Collection<User> users) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(crmRequest, true, data, true);
        final JsonObject summaryJson = CrmRequest.ClientType.WEB == crmRequest.getClientType() ?
                dealConverter.summary(deal, converterConfig) : dealConverterMobileClient.convertSummary(deal, converterConfig);
        final JsonObject profileJson = dealConverter.convertProfileToJsonObject(deal, converterConfig);

        summaryJson.put(DealFields.ID, deal.id());
        loadPreviewDetailsIntoJson(deal.getPrimaryContact(), profileJson, false);
        summaryJson.put(DealFields.PROFILE, profileJson);

        if (converterConfig.isAccessConfigRequired()) {
            summaryJson.put(FieldKeys.IS_EDITABLE.getName(), deal.isEditable());
            summaryJson.put(FieldKeys.IS_DELETABLE.getName(), deal.isDeletable());
        }

        return summaryJson;
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param deal       Represents the {@link Deal}
     * @param data       Holds the entity details
     * @param users      Contains the collection of users
     * @return The {@link JsonObject} details
     */
    @Override
    protected JsonObject convertProfile(final CrmRequest crmRequest, final Deal deal, final JsonObject data, final Collection<User> users) {
        final JsonObject profileJson = dealConverter.profile(deal, converterConfigBuilder.build(crmRequest, true, data, true));

        loadPreviewDetailsIntoJson(deal.getPrimaryContact(), profileJson, true);

        return profileJson;
    }

    /**
     * Sets the {@link CrmUser} details into {@link Deal}.
     *
     * @param deal  Represents the {@link Deal} details.
     * @param users Refers the collection of {@link User}.
     */
    @Override
    protected void setUser(final Deal deal, final Collection<User> users) {
        final Map<Long, User> crmUsers = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
        final ValueHolder valueHolder = deal.getValueHolder();
        final CrmUser salesOwner = deal.getSalesOwner();
        final CrmUser createdBy = deal.getCreatedBy();
        final CrmUser updatedBy = deal.getUpdatedBy();

        salesOwner.setUser(crmUsers.get(salesOwner.getIamId()));
        createdBy.setUser(crmUsers.get(createdBy.getIamId()));
        updatedBy.setUser(crmUsers.get(updatedBy.getIamId()));

        deal.setSalesOwner(salesOwner);
        deal.setCreatedBy(createdBy);
        deal.setUpdatedBy(updatedBy);

        if (Objects.nonNull(valueHolder)) {

            for (final CrmUser crmUser : valueHolder.getUserValues()) {
                crmUser.setUser(crmUsers.get(crmUser.getIamId()));
            }
        }
    }

    /**
     * Converts the {@link Deal} to {@link JsonObject}
     *
     * @param deal Represents the {@link Deal}
     * @return {@link JsonObject}
     */
    private JsonObject convertDealForProduct(final Deal deal) {
        final JsonObject json = Json.object();
        final JsonObject monetaryJson = Json.object();
        final DealValue dealValue = deal.getDealValue();
        final ProductDetailsHolder productDetailsHolder = deal.getProductDetailsHolder();
        final Contact primaryContact = deal.getPrimaryContact();
        final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

        if (Objects.nonNull(primaryContact)) {
            json.put(DealFields.PRIMARY_CONTACT, contactConverter.convertPartialData(primaryContact));
        }
        json.put(DealFields.ID, deal.id());
        json.put(DealFields.ORG_ID, deal.orgId());
        json.put(DealFields.TITLE, deal.getTitle());
        json.put(DealFields.EXPECTED_CLOSE_DATE, dateUtil.getTimeStamp(deal.getExpectedCloseDate()));
        json.put(DealFields.PRODUCT_QUANTITY, Objects.isNull(productDetails) ? null : productDetails
                .stream().map(ProductDetail::getQuantity).findFirst().get());

        monetaryJson.put(DealFields.VALUE, dealValue.getValue());
        monetaryJson.put(DealFields.CURRENCY_ID, dealValue.getCurrency().getId());
        json.put(DealFields.DEAL_VALUE, monetaryJson);

        return json;
    }

    /**
     * <p>
     * Fetches and maps sort fields from the provided {@link JsonObject}
     * </p>
     *
     * @param data Represents the request {@link JsonObject}
     * @return The {@link Map} contains key as field name value as {@link SortType}
     */
    private Map<String, SortType> fetchSortFields(final JsonObject data) {
        final String sortKey = DealFields.SORT;
        final Map<String, SortType> fieldSortMap = new HashMap<>();

        if (data.containsKey(sortKey)) {
            final String typeKey = DealFields.TYPE;
            final String fieldKey = DealFields.FIELD;
            final JsonArray sortedFields = data.getJsonArray(sortKey);

            for (int index = 0; index < sortedFields.size(); index++) {
                final JsonObject sortedFieldsJson = sortedFields.getJsonObject(index);
                final String fieldName = sortedFieldsJson.getString(fieldKey);
                final String type = sortedFieldsJson.getString(typeKey).toLowerCase();

                fieldSortMap.put(fieldName, SortType.getSortType(type));
            }
        }

        return fieldSortMap;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    protected Collection<String> stateRequiredTypes() {
        final Collection<String> stateRequiredTypes = new HashSet<>(super.stateRequiredTypes());

        stateRequiredTypes.addAll(Set.of("updateDealState", "updatePipeline", "updateExpectedCloseDate", "updateDealStage", "revokeDealCommit", "commitDeal"));
        return stateRequiredTypes;
    }

    /**
     * {@inheritDoc}
     *
     * @param request Represents the {@link CrmRequest}
     * @return The collection of models.
     */
    @Override
    public Collection<TlcModel> getCurrentModels(final CrmRequest request) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(ActionFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(ActionFields.DATA);
        final long orgId = request.orgId();

        return switch (type) {
            case "update", "pUpdate", "updateDealState", "updatePipeline", "updateExpectedCloseDate", "updateDealStage", "delete" -> {
                if (data.containsKey(ActionFields.ID)) {
                    final Deal deal = dealManager.get(orgId, data.getLong(ActionFields.ID));

                    dealManager.loadSummaryDetails(orgId, deal);
                    yield List.of(deal);
                }
                yield Collections.emptyList();
            }
            default ->  Collections.emptyList();
        };
    }


    /**
     * <p>
     * Gets the primary email mapped for the given organization and deal id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param dealId Represents the deal id
     * @return the {@link JsonObject} containing of primary email values
     */
    private JsonObject getPrimaryEmail(final long orgId, final Long dealId) {
        if (dealManager.exists(orgId, dealId)) {
            final Optional<Email> primaryEmail = dealManager.getPrimaryEmail(orgId, dealId);

            return primaryEmail.isPresent() ? emailConverter.convert(new ConverterConfig(false, false), primaryEmail.get()) : Json.object();
        } else {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, DEAL_NOT_FOUND_I18N_KEY);
        }
    }

    /**
     * <p>
     * Gets the primary phone mapped for the given organization and deal id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param dealId Represents the deal id
     * @return the {@link JsonObject} containing of primary phone values
     */
    private JsonObject getPrimaryPhone(final long orgId, final Long dealId) {
        if (dealManager.exists(orgId, dealId)) {
            final Optional<Phone> primaryPhone = dealManager.getPrimaryPhone(orgId, dealId);

            return primaryPhone.isPresent() ? phoneConverter.convert(new ConverterConfig(false, false), primaryPhone.get()) : Json.object();
        } else {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, DEAL_NOT_FOUND_I18N_KEY);
        }
    }

    /**
     * <p>
     * Gets the {@link JsonArray} of deal data mapped for the given contact
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param crmResponse Represents the {@link CrmResponse}
     * @param jsonObject  Represents the {@link JsonObject}
     * @return the {@link JsonArray} which holds the deals by contact
     */
    private JsonArray getDealsByContact(final long orgId, final CrmResponse crmResponse, final JsonObject jsonObject) {
        final Long contactId = jsonObject.getLong(DealFields.CONTACT_ID);

        if (contactManager.exists(orgId, contactId)) {
            return getDealsBySource(orgId, contactId, Source.CONTACT, jsonObject, crmResponse);
        } else {
            throw ErrorCode.get(DealErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        }
    }

    /**
     * <p>
     * Gets the {@link JsonArray} of deal data mapped for the given company.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param crmResponse Represents the {@link CrmResponse}
     * @param jsonObject  Represents the {@link JsonObject}
     * @return the {@link JsonArray} which holds the deals by company.
     */
    private JsonArray getDealsByCompany(final long orgId, final CrmResponse crmResponse, final JsonObject jsonObject) {
        final Long companyId = jsonObject.getLong(DealFields.COMPANY_ID);

        if (companyManager.exists(orgId, companyId)) {
            return getDealsBySource(orgId, companyId, Source.COMPANY, jsonObject, crmResponse);
        } else {
            throw ErrorCode.get(DealErrorCodes.COMPANY_NOT_FOUND, "i18n.company.not.found");
        }
    }

    /**
     * <p>
     * Gets the {@link JsonArray} of deal data mapped for the given product
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param crmResponse Represents the {@link CrmResponse}
     * @param jsonObject  Represents the request data
     * @return {@link JsonArray} which holds the deals by product
     */
    private JsonArray getDealsByProduct(final long orgId, final CrmResponse crmResponse, final JsonObject jsonObject) {
        final long productId = jsonObject.getLong(DealFields.PRODUCT_ID);

        if (productManager.exists(orgId, productId)) {
            return getDealsBySource(orgId, productId, Source.PRODUCT, jsonObject, crmResponse);
        } else {
            throw ErrorCode.get(DealErrorCodes.PRODUCT_NOT_FOUND, "i18n.product.not.found");
        }
    }

    /**
     * <p>
     * Gets the {@link JsonArray} of deal data mapped for the given entity id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param entityId    Represents an entity id
     * @param source      Represents the {@link Source}
     * @param jsonObject  Represents the {@link JsonObject}
     * @param crmResponse Represents the {@link CrmResponse}
     * @return the {@link JsonArray}
     */
    private JsonArray getDealsBySource(final long orgId, final Long entityId, final Source source, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final JsonArray jsonArray = Json.array();
        final DealMobilePageHandler dealMobilePageHandler = buildDealMobilePageHandler(orgId, entityId, source, jsonObject);
        final long totalRecords = dealMobilePageHandler.totalRecordsCount();

        if (totalRecords > 0) {
            final Collection<Deal> deals = dealMobilePageHandler.fetchPageData().getData();

            for (final Deal deal : deals) {
                jsonArray.put(dealConverter.convertDealIntoJson(deal));
            }
        }

        crmResponse.put(DealFields.TOTAL_RECORDS, totalRecords);
        crmResponse.put(DealFields.HAS_MORE, dealMobilePageHandler.hasMore());

        return jsonArray;
    }

    /**
     * <p>
     * Gets the {@link DealMobilePageHandler} for the given organization and entity
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param entityId   Represents an entity id
     * @param source     Represents the {@link Source}
     * @param jsonObject Represents the {@link JsonObject}
     * @return the {@link DealMobilePageHandler}
     */
    private DealMobilePageHandler buildDealMobilePageHandler(final long orgId, final Long entityId, final Source source, final JsonObject jsonObject) {
        final int start = jsonObject.optInt(ActivityFields.START.getName(), DEFAULT_START_VALUE);
        final int limit = jsonObject.optInt(ActivityFields.LIMIT.getName(), DEFAULT_LIMIT);
        final String stateIdKey = DealFields.STATE_ID;
        final DealMobilePageHandler dealMobilePageHandler = new DealMobilePageHandler(start, limit);

        dealMobilePageHandler.setOrgId(orgId);
        dealMobilePageHandler.setEntityId(entityId);
        dealMobilePageHandler.addState(jsonObject.containsKey(stateIdKey) ? jsonObject.getLong(stateIdKey) : null);
        dealMobilePageHandler.setSource(source);

        return dealMobilePageHandler;
    }

    /**
     * <p>
     * Loads the preview details into the given data json
     * </p>
     *
     * @param primaryContact       Represents the primary {@link Contact}
     * @param dataJson             Represents the {@link JsonObject}
     * @param needHoverCardDetails Refers whether the card details are required or not.
     */
    private void loadPreviewDetailsIntoJson(final Contact primaryContact, final JsonObject dataJson, final boolean needHoverCardDetails) {

        if (Objects.nonNull(primaryContact)) {
            final String idKey = DealFields.ID;
            final String nameKey = DealFields.NAME;
            final JsonObject previewJson = Json.object();
            final Long contactId = primaryContact.id();

            if (needHoverCardDetails) {
                final Company company = primaryContact.getCompany();
                final Collection<Email> emails = primaryContact.getEmails();
                final Collection<Phone> phones = primaryContact.getPhones();
                final DisplayPicture thumbnail = primaryContact.getThumbnail();

                if (Objects.nonNull(company)) {
                    final JsonObject companyJson = Json.object();

                    companyJson.put(idKey, company.id());
                    companyJson.put(nameKey, company.getName());
                    previewJson.put(ContactFields.COMPANY, companyJson);
                }
                previewJson.put(ContactFields.TWITTER, primaryContact.getTwitter());
                previewJson.put(ContactFields.FACEBOOK, primaryContact.getFacebook());
                previewJson.put(ContactFields.LINKEDIN, primaryContact.getLinkedIn());

                if (Objects.nonNull(thumbnail)) {
                    previewJson.put(ContactFields.THUMBNAIL, contactConverter.convertThumbnailToJson(contactId, Contact.ENTITY_TYPE, thumbnail));
                }

                if (Objects.nonNull(emails) && !emails.isEmpty()) {
                    final JsonArray emailJsonArray = Json.array();
                    final String valueKey = ContactFields.VALUE;

                    for (final Email email : emails) {
                        final JsonObject emailJson = Json.object();

                        emailJson.put(idKey, email.id());
                        emailJson.put(valueKey, email.getValue());
                        emailJsonArray.put(emailJson);
                    }
                    previewJson.put(ContactFields.EMAILS, emailJsonArray);
                }

                if (Objects.nonNull(phones) && !phones.isEmpty()) {
                    final JsonArray phoneJsonArray = Json.array();
                    final String numberKey = ContactFields.NUMBER;

                    for (final Phone phone : phones) {
                        final JsonObject phoneJson = Json.object();

                        phoneJson.put(idKey, phone.id());
                        phoneJson.put(numberKey, phone.getNumber());
                        phoneJsonArray.put(phoneJson);
                    }
                    previewJson.put(ContactFields.PHONES, phoneJsonArray);
                }
                previewJson.put(ContactFields.FULL_NAME, primaryContact.getFullName());
            } else {
                previewJson.put(nameKey, primaryContact.getFullName());
            }
            previewJson.put(idKey, contactId);
            final Email primaryEmail = primaryContact.getPrimaryEmail();
            final Phone primaryPhone = primaryContact.getPrimaryPhone();

            if (Objects.nonNull(primaryPhone)) {
                final JsonObject phoneJson = Json.object();

                phoneJson.put(idKey, primaryPhone.id());
                phoneJson.put(ContactFields.NUMBER, primaryPhone.getNumberWithCode());

                previewJson.put(ContactFields.PRIMARY_PHONE, phoneJson);
            }

            if (Objects.nonNull(primaryEmail)) {
                final JsonObject emailJson = Json.object();

                emailJson.put(idKey, primaryEmail.id());
                emailJson.put(ContactFields.TYPE, primaryEmail.getType());
                emailJson.put(ContactFields.VALUE, primaryEmail.getValue());

                previewJson.put(ContactFields.PRIMARY_EMAIL, emailJson);
            }
            dataJson.put(DealFields.PARTIAL_DATA, previewJson);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The config action name
     */
    @Override
    protected String getConfigActionName() {
        return CONFIG_ACTION_NAME;
    }

    /**
     * <p>
     * Updates the primary contact of the deal
     * </p>
     *
     * @param orgId             Represents the organisation id
     * @param dealId            Represents the deal id
     * @param primaryContactId  Represents the primary contact id of the deal
     */
    private void updatePrimaryContact(final Long orgId, final Long dealId, final Long userId, final Long primaryContactId) {

        if (dealManager.exists(orgId, dealId) && contactManager.exists(orgId, primaryContactId)) {
            dealManager.updatePrimaryContact(orgId, dealId, userId, primaryContactId);
        } else {
            throw ErrorCode.get(EntityErrorCodes.ENTITY_NOT_FOUND, "i18n.entity.not.found");
        }
    }

    /**
     * <p>
     * Updates the deal archive state
     * </p>
     *
     * @param orgId      Represents the organisation Id
     * @param userId     Represents the user Id
     * @param dealId     Represents the deal Id
     * @param isArchived Represents the archive state
     */
    private void updateArchiveState(final Long orgId, final long userId, final Long dealId, final boolean isArchived) {

        if (dealManager.exists(orgId, dealId)) {
            dealManager.updateArchiveState(orgId, userId, dealId, isArchived);
        } else {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found");
        }
    }

    /**
     * <p>
     * Updates the deal with commited forecast category
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     * @param data            Represents the data {@link JsonObject}
     * @param eventData       Represents the  {@link EventData}
     */
    private void commitDeal(final CrmRequest crmRequest, final CrmResponse crmResponse,final JsonObject data, final EventData eventData) {
        final Deal deal = new Deal();
        final String expectedCloseDateKey = DealFields.EXPECTED_CLOSE_DATE;

        deal.setId(data.getLong(DealFields.ID));
        deal.setOrgId(crmRequest.orgId());
        deal.setUpdatedBy(crmRequest.getUser());

        if (data.containsKey(expectedCloseDateKey)) {
            deal.setExpectedCloseDate(new Date(data.getLong(expectedCloseDateKey)));
        }
        dealManager.updateForecastCategory(deal);
        eventData.setValues(List.of(deal));
        eventData.setAction(Action.UPDATE);
        eventData.setApiType("commitDeal");
        triggerEvent(CoreEvent.QUOTA_DEAL_EVENT, eventData, Event.Type.POST);
        crmResponse.put(ForecastCategoryFields.MESSAGE, crmResponse.resolveI18nString("i18n.quota.deal.committed"));
    }

    /**
     * <p>
     * Removes the committed category of the given deal by clearing its forecast category and related commitment details.
     * </p>
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     * @param data        Represents the data {@link JsonObject}
     * @param eventData   Represents the  {@link EventData}
     */
    private void revokeDealCommit(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data, final EventData eventData) {
        final Deal deal = new Deal();
        final Long orgId = crmRequest.orgId();

        deal.setOrgId(crmRequest.orgId());
        deal.setUpdatedBy(crmRequest.getUser());
        deal.setId(data.getLong(DealFields.ID));
        deal.setForecastCategory(forecastCategoryManager.getDefaultCategory(orgId));

        dealManager.updateForecastCategory(deal);
        eventData.setValues(List.of(deal));
        eventData.setAction(Action.UPDATE);
        eventData.setApiType("revokeDealCommit");
        triggerEvent(CoreEvent.QUOTA_DEAL_EVENT, eventData, Event.Type.POST);
        crmResponse.put(ForecastCategoryFields.MESSAGE, crmResponse.resolveI18nString("i18n.quota.deal.commit.removed"));
    }
}
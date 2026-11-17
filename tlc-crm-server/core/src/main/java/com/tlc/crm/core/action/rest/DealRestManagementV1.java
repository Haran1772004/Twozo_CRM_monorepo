package com.tlc.crm.core.action.rest;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.rest.CrmSecureRestAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.formatter.DealRestReqFormatter;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.core.validator.internal.DealDataValidator;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Option;

import com.tlc.iam.common.models.User;
import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.type.Group;

import com.tlc.web.HttpMethod;
import com.tlc.web.undertow.rest.annotation.ApiRoute;
import com.tlc.web.undertow.rest.annotation.RestApiHandler;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link Deal} related REST web actions.
 * </p>
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@RestApiHandler(path = "/v1/deal")
public class DealRestManagementV1 extends CrmSecureRestAction<Deal> {

    private final DealRestReqFormatter dealRestReqFormatter;
    private final DealConverter dealConverter;
    private final DealManager dealManager;
    private final ConverterConfigBuilder converterConfigBuilder;
    private final DealDataValidator dealDataValidator;
    private final PipelineStageManager pipelineStageManager;

    public DealRestManagementV1() {
        this.dealRestReqFormatter = DealRestReqFormatter.getInstance();
        this.dealConverter = DealConverter.getInstance();
        this.dealManager = DealManager.getInstance();
        this.converterConfigBuilder = ConverterConfigBuilder.getInstance();
        this.dealDataValidator = DealDataValidator.getInstance();
        this.pipelineStageManager = PipelineStageManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing path parameters, query parameters,
     *                 headers, and contextual metadata.
     * @param response The CRM response used to write the result or error payload.
     */
    @ApiRoute(path = "/{id}", method = HttpMethod.GET)
    @Override
    public void get(final CrmRequest request, final CrmResponse response) {
        final long orgId = request.orgId();
        final long id = Long.parseLong(request.getParameter("id"));

        if (dealManager.exists(orgId, id)) {
            fetchAndDispatchUser(request, dealManager.get(request.orgId(), id), response);
        } else {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing the payload and request metadata.
     * @param response The CRM response used to send the updated resource or status result.
     */
    @ApiRoute(path = "/{id}", method = HttpMethod.PUT)
    @Override
    public void put(final CrmRequest request, final CrmResponse response) {
        final String idKey = FieldKeys.ID.getName();
        final JsonObject requestJson = request.getRequestJson();
        final EventData eventData = new SecureEventData(request.orgId(), request.userId());

        eventData.setLocale(request.locale());
        final Collection<TlcModel> currentModels = getCurrentModels(request);

        if (!currentModels.isEmpty()) {
            eventData.setPreviousValues(currentModels);
        }

        requestJson.put(idKey, Long.parseLong(request.getParameter(idKey)));
        final Deal deal = convert(request, request.getRequestJson());

        update(response, request.orgId(), request.userId(), eventData, List.of(deal));
    }

    @Override
    public void getAll(CrmRequest request, CrmResponse response) {

    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing the payload of the resource to be created.
     * @param response The CRM response used to send the created resource or creation status.
     */
    @ApiRoute(method = HttpMethod.POST)
    @Override
    public void post(CrmRequest request, CrmResponse response) {
        final long orgId = request.orgId();
        final long userId = request.userId();
        final Deal model = convert(request, request.getRequestJson());
        final EventData eventData = new SecureEventData(orgId, userId);

        eventData.setLocale(request.locale());
        create(response, orgId, userId, eventData, List.of(model));
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing the partial update payload.
     * @param response The CRM response used to return the modified resource or status details.
     */
    @ApiRoute(path = "/{id}", method = HttpMethod.PATCH)
    @Override
    public void patch(CrmRequest request, CrmResponse response) {
        final String idKey = FieldKeys.ID.getName();
        final JsonObject requestJson = request.getRequestJson();
        final EventData eventData = new SecureEventData(request.orgId(), request.userId());

        eventData.setLocale(request.locale());
        final Collection<TlcModel> currentModels = getCurrentModels(request);

        if (!currentModels.isEmpty()) {
            eventData.setPreviousValues(currentModels);
        }

        requestJson.put(idKey, Long.parseLong(request.getParameter(idKey)));
        update(response, request.orgId(), request.userId(), eventData, List.of(convertPartialData(request, requestJson)));
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing identifiers or parameters required to locate the resource.
     * @param response The CRM response used to send deletion confirmation or error details.
     */
    @ApiRoute(path = "/{id}", method = HttpMethod.DELETE)
    @Override
    public void delete(CrmRequest request, CrmResponse response) {
        final EventData eventData = new SecureEventData(request.orgId(), request.userId());

        eventData.setLocale(request.locale());
        final Collection<TlcModel> currentModels = getCurrentModels(request);

        if (!currentModels.isEmpty()) {
            eventData.setPreviousValues(currentModels);
        }
        delete(response, request.orgId(), request.userId(), eventData, List.of(dealManager.get(request.orgId(), Long.parseLong(request.getParameter("id")))));
    }

    @Override
    public void options(CrmRequest request, CrmResponse response) {

    }

    @Override
    protected Deal convert(final CrmRequest request, final JsonObject jsonObject) {
        final JsonObject dealJson = dealRestReqFormatter.format(request.orgId(), jsonObject, request.i18nResolver());
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, dealJson);

        return dealConverter.convert(converterConfig, dealJson);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    protected ConfigManager<Deal> getConfigManager() {
        return dealManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    The request data.
     * @param jsonObject The request json.
     */
    @Override
    protected Deal convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final JsonObject dealJson = dealRestReqFormatter.format(request.orgId(), jsonObject, request.i18nResolver());
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, dealJson);

        return dealConverter.convert(converterConfig, dealJson);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link JsonObject} filled with the deal data
     */
    @Override
    public JsonObject convert(final CrmRequest request, final Deal deal) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, null);

        return dealConverter.convertToRestJson(deal, converterConfig);
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
     * {@inheritDoc}
     *
     * @param request Represents the {@link CrmRequest}
     * @return The collection of models.
     */
    @Override
    public Collection<TlcModel> getCurrentModels(final CrmRequest request) {
        final long id = Long.parseLong(request.getParameter("id"));
        final long orgId = request.orgId();
        final Deal deal = dealManager.get(orgId, id);

        dealManager.loadSummaryDetails(orgId, deal);
        return List.of(deal);
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
    public void update(final CrmResponse crmResponse, final Long orgId, final Long userId, final EventData eventData, final Collection<Deal> deals) {
        final Collection<ErrorDescriptor> errors = validate(deals, Group.Update.class);
        final Action action = Action.UPDATE;

        if (isValidDetails(orgId, userId, Action.UPDATE, deals)) {
            setDealState(orgId, deals);
            validateDealState(deals, errors);

            if (errors.isEmpty()) {
                preAction(action, deals, eventData);
                final ProcessOutcome processOutcome = getConfigManager().bulkUpdate(orgId, deals);

                loadResponseAndInvokePostAction(action, crmResponse, processOutcome, eventData, deals);
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
     * @param userId      Represents the user id
     * @param deals       Represents the collection of {@link Deal}
     * @param eventData   Represents the {@link EventData}
     */
    @Override
    public void create(final CrmResponse crmResponse, final Long orgId, final Long userId, final EventData eventData, final Collection<Deal> deals) {
        final Collection<ErrorDescriptor> errors = validate(deals, Group.Create.class);
        final Action action = Action.CREATE;

        if (isValidDetails(orgId, userId, action, deals)) {
            setDealState(orgId, deals);
            validateDealState(deals, errors);

            if (errors.isEmpty()) {
                final ProcessOutcome processOutcome = getConfigManager().create(orgId, deals);

                loadResponseAndInvokePostAction(action, crmResponse, processOutcome, eventData, deals);
            } else {
                loadValidationErrors(action, crmResponse, errors);
            }
        }
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
     * Checks whether the deal state is won, lost or open if its won need to set won reason and won time
     * if its lost need to set lost reason and lost time.
     *
     * @param deals Represents the {@link Collection} of deals
     */
    private void validateDealState(final Collection<Deal> deals, final Collection<ErrorDescriptor> errors) {
        for (final Deal deal : deals) {
            final Deal.State state = deal.getState();
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
}

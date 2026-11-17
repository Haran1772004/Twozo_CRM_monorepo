package com.tlc.crm.plan.and.billing.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.converter.model.ConverterData;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmSecureAction;
import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.common.models.SortConfig;
import com.tlc.crm.common.models.SortType;

import com.tlc.crm.entity.currency.BaseCurrencyService;

import com.tlc.crm.plan.and.billing.api.manager.OrgPlanManager;
import com.tlc.crm.plan.and.billing.api.models.BillingDetails;
import com.tlc.crm.plan.and.billing.api.models.Invoice;
import com.tlc.crm.plan.and.billing.api.models.InvoiceSummary;
import com.tlc.crm.plan.and.billing.api.models.Invoices;
import com.tlc.crm.plan.and.billing.api.models.OrgSubscriptionPlan;
import com.tlc.crm.plan.and.billing.api.models.OrgSubscriptions;
import com.tlc.crm.plan.and.billing.api.models.PlanCancellationData;
import com.tlc.crm.plan.and.billing.api.models.PlanContractState;
import com.tlc.crm.plan.and.billing.api.models.PlanStoryType;
import com.tlc.crm.plan.and.billing.api.models.SubscriptionResponse;
import com.tlc.crm.plan.and.billing.api.models.SubscriptionStatus;
import com.tlc.crm.plan.and.billing.api.models.converter.OrgSubscriptionPlanConverter;
import com.tlc.crm.plan.and.billing.calculator.PlanExpirationCalculator;
import com.tlc.crm.plan.and.billing.fields.PaymentFields;
import com.tlc.crm.plan.and.billing.fields.SubscriptionPlanFields;
import com.tlc.crm.plan.and.billing.handler.PlanSubscriptionHandler;
import com.tlc.crm.plan.and.billing.internal.status.SubscriptionErrorCodes;

import com.tlc.crm.security.iam.LoggedInUserInfo;

import com.tlc.i18n.I18nKey;

import com.tlc.iam.client.OrganizationResolver;
import com.tlc.iam.common.models.Organization;

import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.payment.gateway.models.Address;
import com.tlc.payment.gateway.models.BillingAmount;
import com.tlc.payment.gateway.models.Payer;

import com.tlc.plans.and.billings.manager.SubscriptionPlanManager;
import com.tlc.plans.and.billings.models.BillingCycle;
import com.tlc.plans.and.billings.models.CancellationFeedback;
import com.tlc.plans.and.billings.models.CancellationFeedbacks;
import com.tlc.plans.and.billings.models.CancellationReason;
import com.tlc.plans.and.billings.models.Discount;
import com.tlc.plans.and.billings.models.Discounts;
import com.tlc.plans.and.billings.models.FeatureGroup;
import com.tlc.plans.and.billings.models.PlanType;
import com.tlc.plans.and.billings.models.PricingScheme;
import com.tlc.plans.and.billings.models.SubscriptionPlan;
import com.tlc.plans.and.billings.models.converter.SubscriptionPlanConverter;

import com.tlc.validator.ModelValidator;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;

import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import java.math.BigDecimal;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Action class for managing subscription plan operations for organizations, including retrieving,
 * subscribing, updating, and (future) cancelling of plans.
 *
 * @author Sathish
 * @version 1.0
 */
@WebAction(path = "/crm/subscription/plan/mgmt")
public class SubscriptionPlanManagement extends CrmSecureAction {

    private static final String ESTIMATED_INVOICE_DESCRIPTION = "i18n.plan.invoice.estimated.invoice.description";
    private static final String INVOICE_SUMMARY_DATE_FORMAT = "MMM dd, yyyy";
    private static final int MAX_EMAIL_LENGTH = 356;
    private static final int MAX_PHONE_LENGTH = 25;
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PHONE_PATTERN = "^\\+?[0-9]{7,15}$";

    private final SubscriptionPlanManager planManager;
    private final OrgPlanManager orgPlanManager;
    private final SubscriptionPlanConverter planConverter;
    private final OrgSubscriptionPlanConverter orgSubscriptionPlanConverter;
    private final PlanSubscriptionHandler subscriptionHandler;
    private final ModelValidator modelValidator;
    private final OrganizationService organizationService;
    private final BaseCurrencyService baseCurrencyService;
    private final RoleManager roleManager;

    public SubscriptionPlanManagement() {
        planManager = SubscriptionPlanManager.getInstance();
        orgPlanManager = OrgPlanManager.getInstance();
        planConverter = SubscriptionPlanConverter.getInstance();
        orgSubscriptionPlanConverter = OrgSubscriptionPlanConverter.getInstance();
        subscriptionHandler = PlanSubscriptionHandler.getInstance();
        modelValidator = ValidatorServiceProvider.get().modelValidator();
        organizationService = OrganizationService.getInstance();
        baseCurrencyService = BaseCurrencyService.getInstance();
        roleManager = RoleManager.getInstance();
    }

    /**
     * Processes all supported plan management actions, routing requests to the corresponding handler methods.
     * For each type, performs validation, invokes business logic, and sets response objects.
     *
     * @param crmRequest  The CRM request context and payload.
     * @param crmResponse Mutable response object for outputting results and errors.
     */
    @Override
    public void sProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final long orgId = crmRequest.orgId();
        final long userId = crmRequest.userId();
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString(SubscriptionPlanFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(SubscriptionPlanFields.DATA);

        switch (type) {
            case "getPlans" -> getPlans(orgId, userId, crmRequest, crmResponse);
            case "getAddOns" -> getAddOns(orgId, userId, crmRequest, crmResponse);
            case "getCurrentPlans" -> getCurrentPlans(crmRequest, crmResponse);
            case "subscribe" -> subscribe(crmRequest, crmResponse, data);
            case "updatePlan" -> updatePlan(crmRequest, crmResponse, data);
            case "addBillingDetails" -> addBillingDetails(orgId, userId, data, crmResponse);
            case "updateBillingDetails" -> updateBillingDetails(orgId, userId, data, crmResponse);
            case "getBillingDetails" -> getBillingDetails(orgId, crmResponse);
            case "getInvoiceSummary" -> getInvoiceSummary(orgId, crmRequest.locale(), crmResponse);
            case "getInvoiceList" -> getInvoiceList(orgId, data, crmResponse);
            case "getCancellationReasonList" -> getCancellationReasonList(crmResponse);
            case "getCancellationDiscounts" -> getCancellationDiscounts(crmRequest, crmResponse);
            case "activateCancellationDiscount" -> activateCancellationDiscount(orgId, data, crmResponse);
            case "featuresToBeLost" -> featuresToBeLost(data, crmResponse);
            case "cancelPlan" -> cancelPlan(orgId, userId, data, crmResponse);
            case "reactivatePlan" -> reactivatePlan(orgId, data, crmResponse);
            case "getPlanStatusNotification" -> getPlanStatusNotificationDetails(orgId, userId, crmResponse);
        }
    }

    /**
     * Populates the response with details required for subscription-related UI notifications.
     * <p>
     * This method evaluates the organization's current ordinary subscription plan to determine
     * if a status notification is necessary. Logic is applied as follows:
     * <ul>
     * <li><b>Trial and Canceled Plans:</b> Status details are always included in the response.</li>
     * <li><b>Paid Plans:</b> Details are only included when the plan is exactly 3 days away
     * from expiration (the "About to Expire" threshold).</li>
     * </ul>
     * </p>
     *
     * @param orgId       The unique identifier of the organization.
     * @param userId      The unique identifier of the user.
     * @param crmResponse The response object to be populated with plan ID, expiration date, remaining days, and notification type.
     */
    private void getPlanStatusNotificationDetails(final long orgId, final long userId, final CrmResponse crmResponse) {
        final Optional<OrgSubscriptionPlan> optionalCurrentPlan = orgPlanManager.getCurrentPlans(orgId, PlanType.ORDINARY).stream().findAny();

        optionalCurrentPlan.ifPresent(currentPlan -> {
            final SubscriptionStatus status = currentPlan.getStatus();
            final DateTime expirationTime = currentPlan.getExpirationTime();
            final int remainingDays = PlanExpirationCalculator.calculateRemainingDays(expirationTime);

            if (status != SubscriptionStatus.PAID || remainingDays <= NumericalConstant.FIVE) {
                crmResponse.put(SubscriptionPlanFields.PLAN_ID, currentPlan.id());
                crmResponse.put(SubscriptionPlanFields.LAST_DATE, expirationTime.getTimeStamp());
                crmResponse.put(SubscriptionPlanFields.REMAINING_DAYS, remainingDays);
                crmResponse.put(SubscriptionPlanFields.IS_ADMIN_USER, roleManager.getRoleByUser(orgId, userId).getConfig().isAdmin());
                crmResponse.put(SubscriptionPlanFields.SUBSCRIPTION_STATUS, status.getId());
            }
        });
    }

    /**
     * Fetches all active ordinary plans for the organization,
     * applies relevant organization discounts, marks which plans are currently active,
     * separates plans into monthly and yearly billing, and sets the arrays and feature groups in the response.
     *
     * @param orgId       Organization ID whose plans and discounts to fetch.
     * @param userId      User ID for context, possibly used for personalized features or permissions.
     * @param crmRequest  CRM request containing locale and additional context.
     * @param crmResponse CRM response to populate with plan arrays and features.
     */
    private void getPlans(final long orgId, final long userId, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Collection<SubscriptionPlan> subscriptionPlans = planManager.getActivePlans(PlanType.ORDINARY);
        final Collection<FeatureGroup> featureGroups = planManager.getPlanFeatures();
        final Map<Long, Discounts> orgDiscounts = orgPlanManager.getOrgDiscounts(orgId, PlanType.ORDINARY,
                Discount.Type.CANCELLATION_DISCOUNT, Discount.Type.CUSTOM_DISCOUNT);
        final Map<Long, OrgSubscriptionPlan> currentPlans = orgPlanManager.getCurrentPlans(orgId, PlanType.ORDINARY)
                .stream().collect(Collectors.toMap(orgSubscriptionPlan -> orgSubscriptionPlan.getPlan().getId(), Function.identity()));
        final JsonArray monthlyPlanJson = Json.array();
        final JsonArray yearlyPlanJson = Json.array();
        final Currency baseCurrency = baseCurrencyService.getBaseCurrency(orgId).getDefaultCurrency();

        for (final SubscriptionPlan subscriptionPlan : subscriptionPlans) {
            final PricingScheme pricingScheme = subscriptionPlan.getPricingScheme();

            subscriptionPlan.getPricingScheme().setBaseCurrency(baseCurrency);
            loadDiscountIntoPricingScheme(pricingScheme, orgDiscounts);
            final ConverterData<SubscriptionPlan> converterData = new ConverterData<>(orgId, userId, crmRequest.locale(), subscriptionPlan);
            final JsonObject planJson = planConverter.convertToJson(converterData);
            final Long planId = subscriptionPlan.getId();

            if (currentPlans.containsKey(planId)) {
                final OrgSubscriptionPlan currentPlan = currentPlans.get(planId);

                if (Objects.equals(currentPlan.getPlan().getPricingScheme().getId(), pricingScheme.getId())) {
                    planJson.put(SubscriptionPlanFields.PLAN_SUBSCRIPTION_ID, currentPlan.id());
                    planJson.put(SubscriptionPlanFields.IS_CURRENTLY_ACTIVE, true);
                    planJson.put(SubscriptionPlanFields.IN_TRIAL, SubscriptionStatus.TRIAL == currentPlan.getStatus());
                }
            }

            if (BillingCycle.MONTHLY == pricingScheme.getBillingCycle()) {
                monthlyPlanJson.put(planJson);
            } else {
                yearlyPlanJson.put(planJson);
            }
        }
        crmResponse.put(SubscriptionPlanFields.MONTHLY_PLANS, monthlyPlanJson);
        crmResponse.put(SubscriptionPlanFields.YEARLY_PLANS, yearlyPlanJson);
        crmResponse.put(SubscriptionPlanFields.FEATURE_GROUPS, planConverter.convertFeatureGroupIntoJson(featureGroups, crmRequest.i18nResolver()));
    }

    /**
     * Loads the discount into {@link PricingScheme}.
     *
     * @param pricingScheme Holds the pricing scheme details.
     * @param orgDiscounts  Holds the org discounts
     */
    private void loadDiscountIntoPricingScheme(final PricingScheme pricingScheme, final Map<Long, Discounts> orgDiscounts) {
        final Long pricingSchemeId = pricingScheme.getId();

        if (orgDiscounts.containsKey(pricingSchemeId)) {
            pricingScheme.addDiscounts(orgDiscounts.get(pricingSchemeId).getAll());
        }
    }

    /**
     * Fetches all active add-on plans, applies discounts, marks active add-ons,
     * and stores the resulting array of plans in the response.
     *
     * @param orgId       Organization ID whose add-on plans and discounts are fetched.
     * @param userId      User ID for context.
     * @param crmRequest  CRM request containing locale and additional context.
     * @param crmResponse CRM response to populate with add-on plan data.
     */
    private void getAddOns(final long orgId, final long userId, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Collection<SubscriptionPlan> subscriptionPlans = planManager.getActivePlans(PlanType.ADD_ON);
        final Map<Long, Discounts> orgDiscounts = orgPlanManager.getOrgDiscounts(orgId, PlanType.ADD_ON,
                Discount.Type.CANCELLATION_DISCOUNT, Discount.Type.CUSTOM_DISCOUNT);
        final Map<Long, OrgSubscriptionPlan> currentPlans = orgPlanManager.getCurrentPlans(orgId, PlanType.ADD_ON)
                .stream().collect(Collectors.toMap(orgSubscriptionPlan -> orgSubscriptionPlan.getPlan().getId(), Function.identity()));
        final JsonArray plansJson = Json.array();
        final Currency baseCurrency = baseCurrencyService.getBaseCurrency(orgId).getDefaultCurrency();

        for (final SubscriptionPlan subscriptionPlan : subscriptionPlans) {
            final PricingScheme pricingScheme = subscriptionPlan.getPricingScheme();

            subscriptionPlan.getPricingScheme().setBaseCurrency(baseCurrency);
            loadDiscountIntoPricingScheme(pricingScheme, orgDiscounts);
            final ConverterData<SubscriptionPlan> converterData = new ConverterData<>(orgId, userId, crmRequest.locale(), subscriptionPlan);
            final JsonObject planJson = planConverter.convertToJson(converterData);
            final Long planId = subscriptionPlan.getId();

            if (currentPlans.containsKey(planId)) {
                final OrgSubscriptionPlan orgSubscriptionPlan = currentPlans.get(planId);

                if (Objects.equals(orgSubscriptionPlan.getPlan().getPricingScheme().getId(), pricingScheme.getId())) {
                    planJson.put(SubscriptionPlanFields.PLAN_SUBSCRIPTION_ID, orgSubscriptionPlan.id());
                    planJson.put(SubscriptionPlanFields.IS_CURRENTLY_ACTIVE, true);
                }
            }
            plansJson.put(planJson);
        }
        crmResponse.put(SubscriptionPlanFields.PLANS, plansJson);
    }

    /**
     * Loads details of the organization's current ordinary plan and all current add-ons into the response,
     * including plan status and organization name.
     *
     * @param crmRequest  CRM request providing localization and context.
     * @param crmResponse CRM response object to populate with plans, add-ons, status, and organization name.
     */
    private void getCurrentPlans(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final long orgId = crmRequest.orgId();
        final OrgSubscriptions orgSubscriptions = orgPlanManager.get(orgId, PlanContractState.PRESENT);
        final Currency baseCurrency = baseCurrencyService.getBaseCurrency(orgId).getDefaultCurrency();
        final JsonArray addOnArray = Json.array();

        for (final OrgSubscriptionPlan orgSubscriptionPlan : orgSubscriptions.getOrgPlans()) {
            orgSubscriptionPlan.getPlan().getPricingScheme().setBaseCurrency(baseCurrency);
            final ConverterData<OrgSubscriptionPlan> converterData = new CrmConverterData<>(crmRequest.getUser(), crmRequest.locale(), orgSubscriptionPlan);
            final JsonObject planJson = orgSubscriptionPlanConverter.convertToJson(converterData);

            if (PlanType.ORDINARY == orgSubscriptionPlan.getPlan().getPlanType()) {
                crmResponse.put(SubscriptionPlanFields.PLAN, planJson);
                crmResponse.put(SubscriptionPlanFields.PLAN_STATUS, getPlanStatus(orgSubscriptionPlan).getId());
            } else {
                addOnArray.put(planJson);
            }
        }
        crmResponse.put(SubscriptionPlanFields.ADD_ONS, addOnArray);
        loadOrgNameIntoResponse(orgId, crmResponse);
    }

    /**
     * Creates a new subscription for the organization, validates input, and returns a payment/checkout URL or error message.
     *
     * @param crmRequest  CRM request providing organizational and plan data.
     * @param crmResponse CRM response to populate with status message and, on success, a checkout URL.
     * @param data        JSON object containing subscription details to process.
     */
    private void subscribe(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final OrgSubscriptions orgSubscriptions = buildSubscription(crmRequest, data, false);
        final Collection<ErrorDescriptor> errorDescriptors = modelValidator.validate(List.of(orgSubscriptions), Group.Create.class);

        if (errorDescriptors.isEmpty()) {
            if (isValidDetails(orgSubscriptions, crmResponse)) {
                final SubscriptionResponse subscriptionResponse = subscriptionHandler.subscribe(orgSubscriptions);

                crmResponse.put(SubscriptionPlanFields.STATUS, subscriptionResponse.getStatus().name());
                crmResponse.put(PaymentFields.CHECKOUT_URL, subscriptionResponse.getCheckoutUrl());
            }
        } else {
            loadErrors(ErrorCode.get(SubscriptionErrorCodes.SUBSCRIPTION_FAILED), errorDescriptors, crmResponse);
        }
    }

    /**
     * Validates the billing contact details of the given subscription.
     *
     * @param orgSubscriptions The subscription containing billing details to validate.
     * @param crmResponse Holds the {@link CrmResponse}
     * @return {@code true} if all provided contact details are valid, {@code false} otherwise.
     */
    private boolean isValidDetails(final OrgSubscriptions orgSubscriptions, final CrmResponse crmResponse) {
        final Payer contact = orgSubscriptions.getBillingDetails().getContact();

        if (!isValidEmail(contact.getEmail())) {
            final ErrorCode errorCode = ErrorCode.get(SubscriptionErrorCodes.INVALID_EMAIL, "i18n.payment.gateway.payer.email.size");

            loadErrors(errorCode, List.of(new ErrorDescriptor(errorCode)), crmResponse);
            return false;
        } else if (!isValidPhone(contact.getPhoneWithCountryCode())) {
            final ErrorCode errorCode = ErrorCode.get(SubscriptionErrorCodes.INVALID_PHONE, "i18n.payment.gateway.payer.phone.size");

            loadErrors(errorCode, List.of(new ErrorDescriptor(errorCode)), crmResponse);
            return false;
        }

        return true;
    }

    /**
     * Validates the given email address against a standard pattern and maximum length.
     * A null or empty email is considered valid since the field is optional.
     *
     * @param email The email address to validate.
     * @return {@code true} if the email is absent or valid, {@code false} otherwise.
     */
    private boolean isValidEmail(final String email) {
        if (Objects.isNull(email)) {
            return true;
        }
        return email.length() <= MAX_EMAIL_LENGTH && email.matches(EMAIL_PATTERN);
    }

    /**
     * Validates the given phone number against a standard international pattern and maximum length.
     * A null or empty phone is considered valid since the field is optional.
     *
     * @param phone The phone number to validate.
     * @return {@code true} if the phone is absent or valid, {@code false} otherwise.
     */
    private boolean isValidPhone(final String phone) {
        if (Objects.isNull(phone)) {
            return true;
        }
        return phone.length() <= MAX_PHONE_LENGTH && phone.matches(PHONE_PATTERN);
    }

    /**
     * Updates an existing subscription for the organization, validates input, and returns a payment/checkout URL or error message.
     *
     * @param crmRequest  CRM request with context for the update.
     * @param crmResponse CRM response to populate with status update and checkout URL.
     * @param data        JSON object with plan/subscription update details.
     */
    private void updatePlan(final CrmRequest crmRequest, final CrmResponse crmResponse, final JsonObject data) {
        final OrgSubscriptions orgSubscriptions = buildSubscription(crmRequest, data, true);
        final Collection<ErrorDescriptor> errorDescriptors = modelValidator.validate(List.of(orgSubscriptions), Group.Update.class);

        if (errorDescriptors.isEmpty()) {
            if (isValidDetails(orgSubscriptions, crmResponse)) {
                final SubscriptionResponse subscriptionResponse = subscriptionHandler.subscribe(orgSubscriptions);

                crmResponse.put(SubscriptionPlanFields.STATUS, subscriptionResponse.getStatus().name());
                crmResponse.put(PaymentFields.CHECKOUT_URL, subscriptionResponse.getCheckoutUrl());
            }
        } else {
            loadErrors(ErrorCode.get(SubscriptionErrorCodes.SUBSCRIPTION_UPDATE_FAILED), errorDescriptors, crmResponse);
        }
    }

    /**
     * Adds new billing contact details for the organization, unless already set.
     *
     * @param orgId       Organization ID for which details are added.
     * @param userId      User ID who is creating or updating the billing details.
     * @param data        JSON object containing billing contact info.
     * @param crmResponse CRM response for status and result reporting.
     */
    private void addBillingDetails(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {
        final Payer billingContact = buildBillingContact(orgId, data);

        if (orgPlanManager.billingDetailsExists(orgId)) {
            final ErrorCode errorCode = ErrorCode.get(SubscriptionErrorCodes.BILLING_DETAILS_ALREADY_EXISTS, "i18n.plan.billing.details.already.exists");

            loadErrors(errorCode, Collections.emptyList(), crmResponse);
        } else {
            final Collection<ErrorDescriptor> errorDescriptors = modelValidator.validate(List.of(billingContact), Group.Create.class);

            if (errorDescriptors.isEmpty()) {
                orgPlanManager.addBillingContact(userId, billingContact);
                crmResponse.put(SubscriptionPlanFields.MESSAGE, crmResponse.resolveI18nString("i18n.plan.billing.details.saved.successfully"));
            } else {
                final ErrorCode errorCode = ErrorCode.get(SubscriptionErrorCodes.INVALID_BILLING_DETAILS);

                loadErrors(errorCode, errorDescriptors, crmResponse);
            }
        }
    }

    /**
     * Updates billing contact details for the organization and sets a success message in the response.
     *
     * @param orgId       Organization ID whose billing details to update.
     * @param userId      User ID performing the update.
     * @param data        JSON object with updated billing contact.
     * @param crmResponse CRM response for confirmation message.
     */
    private void updateBillingDetails(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {
        final Payer billingContact = buildBillingContact(orgId, data);
        final Collection<ErrorDescriptor> errorDescriptors = modelValidator.validate(List.of(billingContact), Group.Update.class);

        if (errorDescriptors.isEmpty()) {
            orgPlanManager.updateBillingContact(userId, billingContact);
            crmResponse.put(SubscriptionPlanFields.MESSAGE, crmResponse.resolveI18nString("i18n.plan.billing.details.saved.successfully"));
        } else {
            final ErrorCode errorCode = ErrorCode.get(SubscriptionErrorCodes.INVALID_BILLING_DETAILS);

            loadErrors(errorCode, errorDescriptors, crmResponse);
        }
    }

    /**
     * Retrieves the organization's billing contact details, if present, and puts them in the response.
     *
     * @param orgId       Organization ID to query.
     * @param crmResponse CRM response to hold billing contact results.
     */
    private void getBillingDetails(final long orgId, final CrmResponse crmResponse) {
        final Payer billingContact = orgPlanManager.getBillingContact(orgId);

        if (Objects.nonNull(billingContact)) {
            crmResponse.put(SubscriptionPlanFields.BILLING_DETAILS, convertBillingContactIntoJson(billingContact));
        }
    }

    /**
     * Retrieves invoice summary including next estimated invoice and last invoice along with discounts and descriptions.
     *
     * @param orgId       Organization ID
     * @param locale      Represents the originated locale of the request.
     * @param crmResponse Response populated with invoice summary JSON objects
     */
    private void getInvoiceSummary(final long orgId, final Locale locale, final CrmResponse crmResponse) {
        final InvoiceSummary invoiceSummary = orgPlanManager.getInvoiceSummary(orgId);

        if (Objects.nonNull(invoiceSummary)) {
            final Invoice nextEstimatedInvoice = invoiceSummary.getNextEstimatedInvoice();

            if (Objects.nonNull(nextEstimatedInvoice)) {
                final Currency defaultCurrency = baseCurrencyService.getDefaultCurrency(orgId);
                final BigDecimal exchangeRate = baseCurrencyService.getExchangeRate(nextEstimatedInvoice.getCurrency(), defaultCurrency);

                nextEstimatedInvoice.setCurrency(defaultCurrency);
                nextEstimatedInvoice.setAmount(BigDecimal.valueOf(nextEstimatedInvoice.getAmount()).multiply(exchangeRate).doubleValue());
                final JsonObject nextEstimatedInvoiceJson = convertInvoiceIntoJson(nextEstimatedInvoice);
                final Optional<OrgSubscriptionPlan> optionalPlan = orgPlanManager.getCurrentPlans(orgId, PlanType.ORDINARY).stream().findFirst();

                optionalPlan.ifPresent(currentPlan -> {
                    final Map<Long, Discounts> orgDiscounts = orgPlanManager.getOrgDiscounts(orgId, PlanType.ORDINARY, Discount.Type.CANCELLATION_DISCOUNT);
                    final Long pricingSchemeId = currentPlan.getPlan().getPricingScheme().getId();

                    if (orgDiscounts.containsKey(pricingSchemeId)) {
                        final Discounts discounts = orgDiscounts.get(pricingSchemeId);
                        final double discountedValue = discounts.getDiscountedValue(nextEstimatedInvoice.getAmount());
                        final JsonArray descriptionsArray = Json.array();
                        final CrmUser currentUser = LoggedInUserInfo.getUser();
                        final UserRegionalPreference userPreference = currentUser.getPreferenceHolder().get(UserPreferenceType.USER_REGIONAL_PREFERENCE);
                        final TimezoneInfo userTimezone = userPreference.getTimezoneInfo();

                        for (final Discount discount : discounts.getAll()) {
                            final I18nKey simplifiedDescription = discount.getSimplifiedDescription();
                            final String discountDescription = crmResponse.resolveI18nString(Objects.isNull(simplifiedDescription)
                                    ? discount.getName() : simplifiedDescription);
                            final DateTime expirationTime = discount.getExpirationTime();

                            if (Objects.isNull(expirationTime)) {
                                descriptionsArray.put(discountDescription);
                            } else {
                                final DateTimeFormatter simpleDateFormat = DateTimeFormatter.ofPattern(INVOICE_SUMMARY_DATE_FORMAT, locale);
                                final String date = Instant.ofEpochMilli(expirationTime.getUtcTimeStamp()).atZone(userTimezone.getTimeZone().toZoneId()).format(simpleDateFormat);

                                descriptionsArray.put(StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE, discountDescription, SubscriptionPlanFields.UNTIL, date));
                            }
                        }
                        nextEstimatedInvoiceJson.put(SubscriptionPlanFields.DISCOUNT_DESCRIPTIONS, descriptionsArray);
                        nextEstimatedInvoiceJson.put(SubscriptionPlanFields.DISCOUNTED_AMOUNT,
                                StringUtil.join(StringUtil.Delimiter.EMPTY_STRING, defaultCurrency.getSymbol(), Double.toString(discountedValue)));
                    }
                });
                nextEstimatedInvoiceJson.put(SubscriptionPlanFields.DESCRIPTION, crmResponse.resolveI18nString(ESTIMATED_INVOICE_DESCRIPTION));
                crmResponse.put(SubscriptionPlanFields.NEXT_ESTIMATED_INVOICE, nextEstimatedInvoiceJson);
            }
            crmResponse.put(SubscriptionPlanFields.LAST_INVOICE, convertInvoiceIntoJson(invoiceSummary.getLastInvoice()));
        }
    }

    /**
     * Retrieves a paginated list of invoices for the organization.
     *
     * @param orgId       Organization ID
     * @param data        JSON containing pagination parameters (start and limit)
     * @param crmResponse Response populated with invoice array and metadata
     */
    private void getInvoiceList(final long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final JsonArray invoiceArray = Json.array();
        final int start = data.getInt(SubscriptionPlanFields.START);
        final int limit = data.getInt(SubscriptionPlanFields.LIMIT);
        final long totalInvoiceCount = orgPlanManager.getTotalInvoiceCount(orgId);

        if (0 < totalInvoiceCount) {
            final Invoices invoices = orgPlanManager.getInvoiceList(orgId, buildSortConfig(data), new LimitConfig(start, limit));

            for (final Invoice invoice : invoices) {
                invoiceArray.put(convertInvoiceIntoJson(invoice));
            }
        }
        crmResponse.put(SubscriptionPlanFields.INVOICES, invoiceArray);
        crmResponse.put(SubscriptionPlanFields.TOTAL_COUNT, totalInvoiceCount);
        crmResponse.put(SubscriptionPlanFields.HAS_MORE, totalInvoiceCount >= start + limit);
    }

    /**
     * Retrieves the list of cancellation reasons and adds JSON to the response.
     *
     * @param crmResponse Response to populate with cancellation reasons list
     */
    private void getCancellationReasonList(final CrmResponse crmResponse) {
        final Collection<CancellationReason> cancellationReasonList = planManager.getCancellationReasonList();

        crmResponse.put(SubscriptionPlanFields.REASONS, convertCancellationReasonsIntoJson(cancellationReasonList, crmResponse));
    }

    /**
     * Retrieves cancellation discounts and adds them as JSON array to the response.
     *
     * @param crmRequest  CRM request offering i18n resolver context
     * @param crmResponse Response to populate with cancellation discounts
     */
    private void getCancellationDiscounts(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Collection<Discount> cancellationDiscounts = planManager.getCancellationDiscounts();
        final Collection<Long> discountIds = cancellationDiscounts.stream().map(Discount::getId).collect(Collectors.toSet());
        final Map<Long, Boolean> cancellationDiscountsActivationStatus = discountIds.isEmpty() ? Collections.emptyMap()
                : orgPlanManager.getDiscountsActivationStatus(crmRequest.orgId(), discountIds);
        final JsonArray discountArray = Json.array();

        for (final Discount cancellationDiscount : cancellationDiscounts) {
            final boolean activationStatus = cancellationDiscountsActivationStatus.getOrDefault(cancellationDiscount.getId(), false);

            if (!activationStatus) {
                discountArray.put(planConverter.convertDiscountIntoJson(cancellationDiscount, crmRequest.i18nResolver()));
            }
        }
        crmResponse.put(SubscriptionPlanFields.DISCOUNTS, discountArray);
    }

    /**
     * Activates a cancellation discount for the organization if not already activated.
     *
     * @param orgId       Organization ID
     * @param data        JSON containing discount ID to activate
     * @param crmResponse Response populated with success or failure messages
     */
    private void activateCancellationDiscount(final long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final long discountId = data.getLong(SubscriptionPlanFields.DISCOUNT_ID);

        if (orgPlanManager.isDiscountAlreadyActivated(orgId, discountId)) {
            final ErrorCode errorCode = ErrorCode.get(SubscriptionErrorCodes.CANCELLATION_DISCOUNT_ACTIVATION_FAILED, "i18n.plan.cancellation.discount.already.exists");

            loadErrors(errorCode, Collections.emptyList(), crmResponse);
        } else {
            orgPlanManager.activateDiscount(orgId, discountId);
            crmResponse.put(SubscriptionPlanFields.MESSAGE, crmResponse.resolveI18nString("i18n.plan.cancellation.discount.activation.success"));
        }
    }


    /**
     * Retrieves the feature groups that would be lost upon plan change and adds them as strings to the response.
     *
     * @param data        JSON containing plan ID
     * @param crmResponse Response to populate with feature group names
     */
    private void featuresToBeLost(final JsonObject data, final CrmResponse crmResponse) {
        final Collection<FeatureGroup> featureGroups = planManager.getPlanFeatureGroups(data.getLong(SubscriptionPlanFields.PLAN_ID))
                .stream().limit(6).toList();
        final JsonArray featuresArray = Json.array();

        for (final FeatureGroup featureGroup : featureGroups) {
            featuresArray.put(crmResponse.resolveI18nString(featureGroup.getName()));
        }
        crmResponse.put(SubscriptionPlanFields.FEATURES, featuresArray);
    }

    /**
     * Cancels a current plan for the organization after verifying plan validity,
     * collecting cancellation feedback and reasons, and setting response message.
     *
     * @param orgId       Organization ID
     * @param userId      User ID performing cancellation
     * @param data        JSON containing plan ID, cancellation reasons, and feedback
     * @param crmResponse Response to populate with success or error message
     */
    private void cancelPlan(final long orgId, final long userId, final JsonObject data, final CrmResponse crmResponse) {
        final long planId = data.getLong(SubscriptionPlanFields.PLAN_ID);

        if (orgPlanManager.isCurrentPlan(orgId, planId)) {
            final PlanCancellationData planCancellationData = new PlanCancellationData();
            final JsonArray feedbacksArray = data.getJsonArray(SubscriptionPlanFields.FEEDBACKS);
            final JsonArray reasonsArray = data.getJsonArray(SubscriptionPlanFields.REASONS);

            planCancellationData.setId(planId);
            planCancellationData.setOrgId(orgId);
            planCancellationData.setUserId(userId);
            planCancellationData.setCancellationFeedbacks(buildCancellationFeedbacks(feedbacksArray));
            planCancellationData.setCancellationReasons(buildCancellationReasons(reasonsArray));
            final Collection<ErrorDescriptor> errorDescriptors = modelValidator.validate(List.of(planCancellationData), Group.Create.class);

            if (errorDescriptors.isEmpty()) {
                subscriptionHandler.cancelSubscription(planCancellationData);
                crmResponse.put(SubscriptionPlanFields.MESSAGE, crmResponse.resolveI18nString("i18n.plan.cancellation.success"));
            } else {
                final ErrorCode errorCode = ErrorCode.get(SubscriptionErrorCodes.INVALID_PLAN_CANCELLATION_DATA);

                loadErrors(errorCode, errorDescriptors, crmResponse);
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(SubscriptionErrorCodes.INVALID_CURRENT_PLAN, "i18n.plan.current.plan.invalid");

            loadErrors(errorCode, Collections.emptyList(), crmResponse);
        }
    }

    /**
     * Reactivates a previously cancelled plan for the organization and sets a success message.
     *
     * @param orgId       Organization ID
     * @param data        JSON containing plan ID to reactivate
     * @param crmResponse Response to populate with success confirmation
     */
    private void reactivatePlan(final long orgId, final JsonObject data, final CrmResponse crmResponse) {
        subscriptionHandler.reactivatePlan(orgId, data.getLong(SubscriptionPlanFields.PLAN_ID));
        crmResponse.put(SubscriptionPlanFields.MESSAGE, crmResponse.resolveI18nString("i18n.plan.reactivation.success"));
    }

    /**
     * Fetches the {@link PlanStoryType} of the given plan.
     *
     * @param orgSubscriptionPlan Holds the details of the plan.
     * @return {@link PlanStoryType}.
     */
    private PlanStoryType getPlanStatus(final OrgSubscriptionPlan orgSubscriptionPlan) {
        final BillingCycle billingCycle = orgSubscriptionPlan.getPlan().getPricingScheme().getBillingCycle();
        final SubscriptionStatus status = orgSubscriptionPlan.getStatus();

        return PlanStoryType.getBySubscriptionStatus(status, billingCycle);
    }

    /**
     * Converts a {@link JsonArray} of reason IDs into a Collection of {@link CancellationReason} objects.
     * Each {@link CancellationReason} is instantiated and its ID is set from the JSON array.
     *
     * @param reasonsArray {@link JsonArray} containing cancellation reason IDs
     * @return Collection of {@link CancellationReason} objects
     */
    private Collection<CancellationReason> buildCancellationReasons(final JsonArray reasonsArray) {
        final Collection<CancellationReason> cancellationReasons = new HashSet<>();
        final int size = reasonsArray.size();

        for (int index = 0; index < size; index++) {
            final CancellationReason cancellationReason = new CancellationReason();

            cancellationReason.setId(reasonsArray.getLong(index));
            cancellationReasons.add(cancellationReason);
        }
        return cancellationReasons;
    }

    /**
     * Converts a {@link JsonArray} of feedback JSON objects into a {@link CancellationFeedbacks} collection.
     * Each {@link CancellationFeedback} is populated with question and response strings.
     *
     * @param feedbacksArray {@link JsonArray} containing feedback objects with question and response fields
     * @return {@link CancellationFeedbacks} collection
     */
    private CancellationFeedbacks buildCancellationFeedbacks(final JsonArray feedbacksArray) {
        final CancellationFeedbacks cancellationFeedbacks = new CancellationFeedbacks();
        final int size = feedbacksArray.size();

        for (int index = 0; index < size; index++) {
            final CancellationFeedback cancellationFeedback = new CancellationFeedback();
            final JsonObject feedbackJson = feedbacksArray.getJsonObject(index);

            cancellationFeedback.setQuestion(feedbackJson.getString(SubscriptionPlanFields.QUESTION));
            cancellationFeedback.setResponse(feedbackJson.getString(SubscriptionPlanFields.RESPONSE));
            cancellationFeedbacks.add(cancellationFeedback);
        }
        return cancellationFeedbacks;
    }

    /**
     * Converts a {@link Collection} of {@link CancellationReason} objects into a {@link JsonArray} of JSON objects.
     * Each JSON object contains the reason ID and a localized reason string resolved from the CRM response.
     *
     * @param cancellationReasonList {@link Collection} of {@link CancellationReason} objects
     * @param crmResponse            {@link CrmResponse} used to resolve localized reason strings
     * @return {@link JsonArray} of JSON objects representing cancellation reasons
     */
    private JsonArray convertCancellationReasonsIntoJson(final Collection<CancellationReason> cancellationReasonList,
                                                         final CrmResponse crmResponse) {
        final JsonArray jsonArray = Json.array();

        for (final CancellationReason cancellationReason : cancellationReasonList) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(SubscriptionPlanFields.ID, cancellationReason.getId());
            jsonObject.put(SubscriptionPlanFields.REASON, crmResponse.resolveI18nString(cancellationReason.getReason()));
            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

    /**
     * Loads the organization name into {@link CrmResponse}.
     *
     * @param orgId       The unique identifier of an organization.
     * @param crmResponse Mutable response object for outputting results and errors.
     */
    private void loadOrgNameIntoResponse(final Long orgId, final CrmResponse crmResponse) {
        crmResponse.convertIntoAsync();

        final EventCallback<Organization> organizationFetchCallback = new EventCallback<>() {

            /**
             * This will be called upon the successful completion of the event consume operation
             *
             * @param organization Holds the organization details.
             */
            @Override
            public void onSuccess(final Organization organization) {
                crmResponse.put(SubscriptionPlanFields.COMPANY, organization.getName());
                crmResponse.send();
            }

            /**
             * This will be called upon the failure of the event consume operation
             *
             * @param cause Cause of the failure
             */
            @Override
            public void onFailure(final Throwable cause) {
                final ErrorCode errorCode = ErrorCode.get(SubscriptionErrorCodes.ORGANIZATION_RETRIEVAL_FAILED, "i18n.common.organization.retrieval.failed", cause);

                loadErrors(errorCode, Collections.emptyList(), crmResponse);
                crmResponse.send();
            }
        };
        new OrganizationResolver(organizationService.getIamOrgId(orgId)).resolve(organizationFetchCallback);
    }

    /**
     * Builds a SortConfig object from the given JSON input.
     * If the input contains a SORT_CONFIG object, parses sorting field and type from it.
     * Otherwise, defaults to sorting by DATE in ascending order.
     *
     * @param jsonObject the source JSON containing optional sorting preferences
     * @return a SortConfig instance reflecting requested or default sort parameters
     */
    private SortConfig buildSortConfig(final JsonObject jsonObject) {
        if (jsonObject.containsKey(SubscriptionPlanFields.SORT_CONFIG)) {
            final JsonObject sortJson = jsonObject.getJsonObject(SubscriptionPlanFields.SORT_CONFIG);

            return new SortConfig(sortJson.getString(SubscriptionPlanFields.FIELD), SortType.getSortType(sortJson.getString(SubscriptionPlanFields.TYPE)));
        }
        return new SortConfig(SubscriptionPlanFields.DATE, SortType.ASCENDING);
    }

    /**
     * Converts an Invoice object into a JsonObject with relevant invoice details.
     *
     * @param invoice the Invoice to convert
     * @return a JsonObject containing the invoice's number, date, amount, PDF URL, and status
     */
    private JsonObject convertInvoiceIntoJson(final Invoice invoice) {
        final JsonObject invoiceJson = Json.object();
        final Invoice.Status status = invoice.getStatus();
        final String invoiceAmount = String.format("%s%s", invoice.getCurrency().getSymbol(), invoice.getAmount());

        invoiceJson.put(SubscriptionPlanFields.INVOICE_NUMBER, invoice.getInvoiceNumber());
        invoiceJson.put(SubscriptionPlanFields.DATE, invoice.getBillingDate().getTimeStamp());
        invoiceJson.put(SubscriptionPlanFields.AMOUNT, invoiceAmount);
        invoiceJson.put(SubscriptionPlanFields.PDF_URL, invoice.getPdfUrl());

        if (Objects.nonNull(status)) {
            invoiceJson.put(SubscriptionPlanFields.STATUS, status.getId());

            if (Invoice.Status.FAILED == status) {
                invoiceJson.put(SubscriptionPlanFields.AMOUNT, String.format("-%s", invoiceAmount));
            }
        }

        return invoiceJson;
    }

    /**
     * Converts address information into a {@link JsonObject}.
     *
     * @param address the Address entity with location details
     * @return a {@link JsonObject} with company and address mapped for invoice details
     */
    private JsonObject convertAddressIntoJson(final Address address) {
        final JsonObject addressJson = Json.object();
        final Country country = address.getCountry();

        addressJson.put(SubscriptionPlanFields.ID, address.id())
                .put(SubscriptionPlanFields.ADDRESS_LINE_1, address.getAddressLine1())
                .put(SubscriptionPlanFields.ADDRESS_LINE_2, address.getAddressLine2())
                .put(SubscriptionPlanFields.CITY, address.getCity())
                .put(SubscriptionPlanFields.STATE, address.getState())
                .put(SubscriptionPlanFields.ZIPCODE, address.getZip());

        if (Objects.nonNull(country)) {
            final JsonObject countryJson = Json.object();

            countryJson.put(SubscriptionPlanFields.ID, country.getId())
                    .put(SubscriptionPlanFields.NAME, country.getName());
            addressJson.put(SubscriptionPlanFields.COUNTRY, countryJson);
        }

        return addressJson;
    }

    /**
     * Converts a billing contact (Payer) object into its JSON representation,
     * including full name, email, and phone with country code.
     *
     * @param billingContact the Payer entity to convert
     * @return a JsonObject containing the basic billing contact fields
     */
    private JsonObject convertBillingContactIntoJson(final Payer billingContact) {
        final JsonObject contactJson = Json.object();
        final Address address = billingContact.getAddress();

        contactJson.put(SubscriptionPlanFields.ID, billingContact.id())
                .put(SubscriptionPlanFields.FIRST_NAME, billingContact.getFirstName())
                .put(SubscriptionPlanFields.LAST_NAME, billingContact.getLastName())
                .put(SubscriptionPlanFields.EMAIL, billingContact.getEmail())
                .put(SubscriptionPlanFields.PHONE, billingContact.getPhoneWithCountryCode())
                .put(SubscriptionPlanFields.COMPANY, billingContact.getCompany());

        if (Objects.nonNull(address)) {
            contactJson.put(SubscriptionPlanFields.ADDRESS, convertAddressIntoJson(address));
        }

        return contactJson;
    }

    /**
     * Builds an {@link OrgSubscriptions} model from the incoming {@link CrmRequest} and associated JSON payload.
     *
     * <p>
     * This method extracts organization context (orgId, userId, locale) from the request and processes the JSON to
     * convert plan data and add-on subscription plans into domain model objects using the
     * {@link OrgSubscriptionPlanConverter}. It supports handling update and create flows via the {@code isUpdateAction} flag.
     * </p>
     *
     * <p>
     * The method looks for the main plan block under the {@link SubscriptionPlanFields#PLAN} key and for any add-ons
     * under the {@link SubscriptionPlanFields#ADD_ONS} array, deserializing each into {@link OrgSubscriptionPlan} instances.
     * All parsed plans are aggregated into a single {@link OrgSubscriptions} object that can then be used downstream.
     * </p>
     *
     * @param crmRequest     the CRM request containing organization, user, and locale context
     * @param jsonObject     the JSON payload containing plan and add-on subscription data to be converted
     * @param isUpdateAction indicates if the operation is an update action (true) or a create action (false);
     *                       influences conversion semantics and validation considerations
     * @return a fully assembled {@link OrgSubscriptions} object containing all parsed plan and add-on subscriptions
     */
    private OrgSubscriptions buildSubscription(final CrmRequest crmRequest, final JsonObject jsonObject, final boolean isUpdateAction) {
        final Long orgId = crmRequest.orgId();
        final Locale locale = crmRequest.locale();
        final OrgSubscriptions orgSubscriptions = new OrgSubscriptions();

        final BiFunction<JsonObject, PlanType, OrgSubscriptionPlan> orgPlanBuilder = (planJson, planType) -> {
            final CrmConverterData<JsonObject> converterData = new CrmConverterData<>(crmRequest.getUser(), locale, isUpdateAction, false, planJson);
            final OrgSubscriptionPlan orgSubscriptionPlan = orgSubscriptionPlanConverter.convertToModel(converterData);

            orgSubscriptionPlan.getPlan().setPlanType(planType);
            return orgSubscriptionPlan;
        };

        orgSubscriptions.setOrgId(orgId);
        orgSubscriptions.setUserId(crmRequest.userId());

        if (jsonObject.containsKey(SubscriptionPlanFields.PLAN)) {
            final JsonObject planJson = jsonObject.getJsonObject(SubscriptionPlanFields.PLAN);
            final OrgSubscriptionPlan orgSubscriptionPlan = orgPlanBuilder.apply(planJson, PlanType.ORDINARY);

            orgSubscriptions.addOrgPlans(orgSubscriptionPlan);
        } else {
            final Optional<OrgSubscriptionPlan> currentPlan = orgPlanManager.getCurrentPlans(orgId, PlanType.ORDINARY).stream().findAny();

            currentPlan.ifPresent(orgSubscriptions::addOrgPlans);
        }

        if (jsonObject.containsKey(SubscriptionPlanFields.ADD_ONS)) {
            final Map<Long, OrgSubscriptionPlan> addOnPlans = orgPlanManager.getCurrentPlans(orgId, PlanType.ADD_ON)
                    .stream().collect(Collectors.toMap(orgPlan -> orgPlan.getPlan().getId(), Function.identity()));
            final JsonArray addOnsJson = jsonObject.getJsonArray(SubscriptionPlanFields.ADD_ONS);
            final int addOnSize = addOnsJson.size();

            for (int index = 0; index < addOnSize; index++) {
                final JsonObject addOnJson = addOnsJson.getJsonObject(index);
                final OrgSubscriptionPlan orgSubscriptionPlan = orgPlanBuilder.apply(addOnJson, PlanType.ADD_ON);
                final Long planId = orgSubscriptionPlan.getPlan().getId();

                if (addOnPlans.containsKey(planId)) {
                    final OrgSubscriptionPlan orgPlan = addOnPlans.get(planId);

                    orgSubscriptionPlan.setId(orgPlan.id());
                    orgSubscriptionPlan.setStatus(orgPlan.getStatus());
                }
                orgSubscriptions.addOrgPlans(orgSubscriptionPlan);
            }
        }

        if (jsonObject.containsKey(SubscriptionPlanFields.BILLING_DETAILS)) {
            final JsonObject billingDetailsJson = jsonObject.getJsonObject(SubscriptionPlanFields.BILLING_DETAILS);

            orgSubscriptions.setBillingDetails(buildBillingDetails(orgId, billingDetailsJson));
        }
        return orgSubscriptions;
    }

    /**
     * Builds and returns a BillingDetails object using values parsed from
     * the provided JSON object, including contact and price details.
     *
     * @param orgId              Organization ID.
     * @param billingDetailsJson The JSON object containing billing information.
     * @return BillingDetails instance populated with contact and price data.
     */
    private BillingDetails buildBillingDetails(final long orgId, final JsonObject billingDetailsJson) {
        final BillingDetails billingDetails = new BillingDetails();

        billingDetails.setContact(buildBillingContact(orgId, billingDetailsJson.getJsonObject(SubscriptionPlanFields.CONTACT)));
        billingDetails.setPrice(buildBillingAmount(orgId, billingDetailsJson.getJsonObject(SubscriptionPlanFields.PRICE)));
        return billingDetails;
    }

    /**
     * Constructs and returns a BillingAmount object with all price fields populated
     * from the given JSON structure (amount, currency, tax, convenience fee, discount).
     * Optional fields default to 0 if not present.
     *
     * @param priceJson The JSON containing price details.
     * @return BillingAmount object containing amount, currency, tax, convenience fee, and discount.
     */
    private BillingAmount buildBillingAmount(final long orgId, final JsonObject priceJson) {
        final BillingAmount billingAmount = new BillingAmount();
        final String currencyCode = priceJson.optString(SubscriptionPlanFields.CURRENCY, StringUtil.Delimiter.EMPTY_STRING.getDelimiter());
        final Currency currency = Currency.getCurrencyByCode(currencyCode)
                .orElseGet(() -> baseCurrencyService.getBaseCurrency(orgId).getDefaultCurrency());

        billingAmount.setAmount(priceJson.getDouble(SubscriptionPlanFields.AMOUNT));
        billingAmount.setCurrency(currency);
        billingAmount.setTax(priceJson.optDouble(SubscriptionPlanFields.TAX, 0));
        billingAmount.setConvenienceFee(priceJson.optDouble(SubscriptionPlanFields.CONVENIENCE_FEE, 0));
        billingAmount.setDiscount(priceJson.optDouble(SubscriptionPlanFields.DISCOUNT, 0));

        return billingAmount;
    }

    /**
     * Creates and returns a Payer object by extracting and setting the contact
     * information from the provided JSON, including address field parsing.
     *
     * @param orgId       Organization ID.
     * @param contactJson The JSON object containing payer's contact details.
     * @return Payer instance populated with name, phone, email, and address.
     */
    private Payer buildBillingContact(final long orgId, final JsonObject contactJson) {
        final Payer payer = new Payer();

        if (contactJson.containsKey(SubscriptionPlanFields.ID)) {
            payer.setId(contactJson.getLong(SubscriptionPlanFields.ID));
        }
        payer.setOrgId(orgId);
        payer.setFirstName(contactJson.getNullableString(SubscriptionPlanFields.FIRST_NAME));
        payer.setLastName(contactJson.getNullableString(SubscriptionPlanFields.LAST_NAME));
        payer.setCompany(contactJson.getNullableString(SubscriptionPlanFields.COMPANY));
        payer.setPhoneWithCountryCode(contactJson.getNullableString(SubscriptionPlanFields.PHONE));
        payer.setEmail(contactJson.getNullableString(SubscriptionPlanFields.EMAIL));

        if (contactJson.containsKey(SubscriptionPlanFields.ADDRESS)) {
            payer.setAddress(buildAddress(orgId, contactJson.getJsonObject(SubscriptionPlanFields.ADDRESS)));
        }

        return payer;
    }

    /**
     * Builds and returns an Address object from the address-related fields in the JSON object.
     *
     * @param jsonObject The JSON object containing address fields.
     * @return An Address populated with line1, line2, city, state, country, and zip code.
     */
    private Address buildAddress(final long orgId, final JsonObject jsonObject) {
        final Address address = new Address();

        if (jsonObject.containsKey(SubscriptionPlanFields.ID)) {
            address.setId(jsonObject.getLong(SubscriptionPlanFields.ID));
        }
        address.setOrgId(orgId);
        address.setAddressLine1(jsonObject.getNullableString(SubscriptionPlanFields.ADDRESS_LINE_1));
        address.setAddressLine2(jsonObject.getNullableString(SubscriptionPlanFields.ADDRESS_LINE_2));
        address.setCity(jsonObject.getNullableString(SubscriptionPlanFields.CITY));
        address.setState(jsonObject.getNullableString(SubscriptionPlanFields.STATE));
        address.setZip(jsonObject.getNullableString(SubscriptionPlanFields.ZIPCODE));

        if (jsonObject.containsKey(SubscriptionPlanFields.COUNTRY)) {
            address.setCountry(Country.getCountryById(jsonObject.getInt(SubscriptionPlanFields.COUNTRY)));
        }

        return address;
    }

    /**
     * Populates crmResponse with error code and summary when data validation or subscription fails.
     *
     * @param errorCode        The error code for the operation.
     * @param errorDescriptors A collection of error descriptors to summarize.
     * @param crmResponse      The mutable response to populate.
     */
    private void loadErrors(final ErrorCode errorCode, final Collection<ErrorDescriptor> errorDescriptors, final CrmResponse crmResponse) {
        crmResponse.setErrorCode(errorCode);
        crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode), errorDescriptors));
    }
}
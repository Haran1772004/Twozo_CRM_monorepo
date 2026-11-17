package com.tlc.crm.messenger.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.language.Language;
import com.tlc.commons.page.Page;
import com.tlc.commons.pair.Pair;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.SecureEventAction;

import com.tlc.crm.common.handler.EventRequestHandler;
import com.tlc.crm.common.service.EventRequestService;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.models.type.Phone;

import com.tlc.crm.messenger.api.channel.manager.WABusinessAccountManager;
import com.tlc.crm.messenger.api.channel.model.ChannelState;
import com.tlc.crm.messenger.api.channel.model.CrmWAPhoneNumber;
import com.tlc.crm.messenger.campaign.model.WACampaignTemplateStatus;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.crm.messenger.internal.status.whatsapp.WAErrorCodes;
import com.tlc.crm.messenger.page.WAChannelPageHandler;
import com.tlc.social.client.service.whatsapp.WABusinessAccountService;
import com.tlc.social.common.whatsapp.model.WABusinessPhoneNumber;
import com.tlc.social.common.whatsapp.fields.WAFields;
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
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages whatsapp related web actions
 * </p>
 *
 * @author Maharaja S
 * @version 1.0.
 */
@WebAction(path = "/messenger/whatsapp/mgmt")
public class WAManagement extends SecureEventAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(WAManagement.class);
    private static final String COUNTRY_CODE_PREFIX = "+";

    private final WABusinessAccountService waBusinessAccountService;
    private final WABusinessAccountManager waBusinessAccountManager;
    private final ContactManager contactManager;
    private final EventRequestHandler socialEventRequestHandler;

    public WAManagement() {
        waBusinessAccountService = WABusinessAccountService.getInstance();
        waBusinessAccountManager = WABusinessAccountManager.getInstance();
        contactManager = ContactManager.getInstance();
        socialEventRequestHandler = EventRequestService.get().getSocialEventRequestHandler();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Represents the{@link CrmRequest}.
     * @param response Represents the {@link CrmResponse}.
     */
    @Override
    protected void secureProcess(final CrmRequest request, final CrmResponse response) {
        final Long orgId = request.orgId();
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(FieldKeys.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());

        switch (type) {
            case "getWAPhoneNumberList" -> getWAPhoneNumberList(orgId, data, response);
            case "checkSyncStatus" -> checkSyncStatus(orgId, response);
            case "getNumbersWithTemplateCount" -> getNumbersWithTemplateCount(orgId, data, response);
            case "checkTemplateSyncStatus" -> checkTemplateSyncStatus(orgId, data, response);
            case "getTemplateLanguageList" -> getTemplateLanguageList(orgId, data, response);
            case "syncTemplate" -> syncTemplate(orgId, data, response);
        }
    }

    /**
     * <p>
     * Gets the whatsapp phone number list along with the chat id
     * </p>
     *
     * @param orgId       Represents the org id
     * @param data        Represents the request data
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void getWAPhoneNumberList(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final boolean isSyncEnabled = waBusinessAccountManager.arePhoneNumbersActive(orgId);

        if (isSyncEnabled) {
            final Optional<Phone> optionalPrimaryPhone = contactManager.getPrimaryPhone(orgId, data.getLong(MessengerFields.CONTACT_ID));

            if (optionalPrimaryPhone.isPresent()) {
                final String primaryPhone = optionalPrimaryPhone.get().getNumber();

                if (!primaryPhone.startsWith(COUNTRY_CODE_PREFIX)) {
                    setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.INVALID_PHONE_NUMBER, "i18n.whatsapp.phone.country.code.not.found"), null, null);
                    return;
                }
                final JsonObject rangeJson = data.optJsonObject(MessengerFields.RANGE);
                final int start;
                final int count;

                if (Objects.isNull(rangeJson)) {
                    start = NumericalConstant.ONE;
                    count = NumericalConstant.TEN;
                } else {
                    start = rangeJson.optInt(MessengerFields.START, NumericalConstant.ONE);
                    count = rangeJson.optInt(MessengerFields.COUNT, NumericalConstant.TEN);
                }
                final WAChannelPageHandler waChannelPageHandler = new WAChannelPageHandler(start, count, orgId, List.of(ChannelState.ENABLED.getId()));
                final long totalRecords = waChannelPageHandler.getTotalRecordsCount();

                if (NumericalConstant.ZERO < totalRecords) {
                    final Collection<CrmWAPhoneNumber> crmBusinessPhoneNumbers = waChannelPageHandler.fetchPageData().getData();

                    crmResponse.put(MessengerFields.TOTAL_COUNT, totalRecords);
                    crmResponse.put(MessengerFields.HAS_MORE, (start + count) <= totalRecords);
                    crmResponse.convertIntoAsync();
                    final Collection<Long> businessPhoneNumberIds = new HashSet<>(20);
                    final Collection<Long> crmBusinessPhoneNumberIds = new HashSet<>(20);

                    for (final CrmWAPhoneNumber crmBusinessPhoneNumber : crmBusinessPhoneNumbers) {
                        businessPhoneNumberIds.add(crmBusinessPhoneNumber.id());
                        crmBusinessPhoneNumberIds.add(crmBusinessPhoneNumber.getCrmPhoneNumberId());
                    }
                    final JsonObject eventJson = Json.object();

                    eventJson.put(MessengerFields.BUSINESS_PHONE_NUMBER_IDS, businessPhoneNumberIds);
                    eventJson.put(MessengerFields.RECIPIENT_PHONE_NUMBER, primaryPhone);
                    final EventCallback<Map<Long, Pair<WABusinessPhoneNumber, Long>>> callback = new EventCallback<>() {

                        @Override
                        public void onSuccess(final Map<Long, Pair<WABusinessPhoneNumber, Long>> phoneNumberMap) {
                            socialEventRequestHandler.handle(() -> {
                                try {
                                    if (phoneNumberMap.isEmpty()) {
                                        final ErrorCode errorCode = ErrorCode.get(WAErrorCodes.BUSINESS_PHONE_NUMBER_IS_EMPTY, "i18n.whatsapp.business.phone.numbers.empty");

                                        LoggerUtil.logError(LOGGER, errorCode, "Whatsapp: Business phone numbers not found, OrgId: {}, Phone number Ids: {}", orgId, businessPhoneNumberIds);
                                        setErrorDescriptor(crmResponse, errorCode, null, null);
                                    } else {
                                        final Collection<Long> chatIds = phoneNumberMap.values().stream().map(Pair::getValue).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
                                        final Map<Long, Long> chatIdMap = chatIds.isEmpty() ? Collections.emptyMap() : waBusinessAccountManager.getChatIdMap(orgId, crmBusinessPhoneNumberIds, chatIds);
                                        final JsonArray phoneArray = Json.array();

                                        for (final CrmWAPhoneNumber crmBusinessPhoneNumber : crmBusinessPhoneNumbers) {
                                            final long crmBusinessPhoneNumberId = crmBusinessPhoneNumber.getCrmPhoneNumberId();
                                            final Pair<WABusinessPhoneNumber, Long> phoneNumberPair = phoneNumberMap.get(crmBusinessPhoneNumber.id());
                                            final WABusinessPhoneNumber businessPhoneNumber = phoneNumberPair.getKey();
                                            final JsonObject businessPhoneNumberJson = Json.object();

                                            businessPhoneNumberJson.put(MessengerFields.ID, crmBusinessPhoneNumberId);
                                            businessPhoneNumberJson.put(MessengerFields.NAME, businessPhoneNumber.getDisplayPhoneNumber());
                                            final Long chatId = chatIdMap.get(crmBusinessPhoneNumberId);

                                            if (Objects.nonNull(chatId)) {
                                                businessPhoneNumberJson.put(MessengerFields.IS_CHAT_EXIST, true);
                                                businessPhoneNumberJson.put(MessengerFields.CHAT_ID, chatId);
                                            } else {
                                                businessPhoneNumberJson.put(MessengerFields.IS_CHAT_EXIST, false);
                                            }
                                            phoneArray.put(businessPhoneNumberJson);
                                        }
                                        crmResponse.put(MessengerFields.LIST, phoneArray);
                                    }
                                } catch (Throwable throwable) {
                                    setErrorDescriptor(orgId, businessPhoneNumberIds, throwable, crmResponse);
                                } finally {
                                    crmResponse.send();
                                }
                            });
                        }

                        @Override
                        public void onFailure(final Throwable cause) {
                            setErrorDescriptor(orgId, businessPhoneNumberIds, cause, crmResponse);
                        }
                    };
                    waBusinessAccountService.fetchBusinessPhoneNumbersWithChat(eventJson, callback);
                }
            } else {
                setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.PRIMARY_PHONE_NOT_FOUND, "i18n.whatsapp.contact.phone.not.found"), null, null);
            }
        } else {
            crmResponse.put(MessengerFields.TOTAL_COUNT, NumericalConstant.ZERO);
            crmResponse.put(MessengerFields.HAS_MORE, false);
            crmResponse.put(MessengerFields.LIST, Json.array());
        }
        crmResponse.put(MessengerFields.SYNC_ENABLED, isSyncEnabled);
    }

    /**
     * <p>
     * Checks the WhatsApp business account sync status
     * </p>
     *
     * @param orgId       Represents the org id
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void checkSyncStatus(final Long orgId, final CrmResponse crmResponse) {
        crmResponse.put(MessengerFields.SYNC_ENABLED, waBusinessAccountManager.arePhoneNumbersActive(orgId));
    }

    /**
     * <p>
     * Gets the WhatsApp phone numbers with template count
     * </p>
     *
     * @param orgId       Represents the org id
     * @param data        Represents the request data
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void getNumbersWithTemplateCount(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final int start = data.optInt(MessengerFields.START, NumericalConstant.ONE);
        final int limit = data.optInt(MessengerFields.LIMIT, NumericalConstant.TWENTY);
        final String searchText = data.optString(MessengerFields.SEARCH_TEXT, null);
        final Collection<CrmWAPhoneNumber> crmBusinessPhoneNumbers = waBusinessAccountManager.getPhoneNumbers(orgId, List.of(ChannelState.ENABLED.getId()));

        if (crmBusinessPhoneNumbers.isEmpty()) {
            crmResponse.put(MessengerFields.TOTAL_COUNT, NumericalConstant.ZERO);
            crmResponse.put(MessengerFields.HAS_MORE, false);
            crmResponse.put(MessengerFields.LIST, Json.array());

            return;
        }
        final Map<Long, Long> phoneNumberIdMap = crmBusinessPhoneNumbers.stream()
                .collect(Collectors.toMap(CrmWAPhoneNumber::id, CrmWAPhoneNumber::getCrmPhoneNumberId));
        final Collection<Long> businessPhoneNumberIds = phoneNumberIdMap.keySet();
        final JsonObject eventJson = Json.object();

        eventJson.put(MessengerFields.PHONE_NUMBER_IDS, businessPhoneNumberIds);
        eventJson.put(MessengerFields.INCLUDE_TEMPLATE_COUNT, true);
        eventJson.put(MessengerFields.START, start);
        eventJson.put(MessengerFields.LIMIT, limit);
        eventJson.put(WAFields.STATUS, Arrays.stream(WACampaignTemplateStatus.values())
                .map(status -> status.toWATemplateStatus().getId())
                .collect(Collectors.toUnmodifiableSet()));

        if (Objects.nonNull(searchText) && !searchText.isBlank()) {
            eventJson.put(MessengerFields.SEARCH_TEXT, searchText);
        }
        crmResponse.convertIntoAsync();

        final EventCallback<Page<WABusinessPhoneNumber>> callback = new EventCallback<>() {

            @Override
            public void onSuccess(final Page<WABusinessPhoneNumber> phoneNumberPage) {
                try {
                    final Collection<WABusinessPhoneNumber> phoneNumbers = phoneNumberPage.getData();
                    final int totalRecords = phoneNumberPage.getNumber();
                    final JsonArray phoneArray = Json.array();

                    crmResponse.put(MessengerFields.TOTAL_COUNT, totalRecords);
                    crmResponse.put(MessengerFields.HAS_MORE, (start + phoneNumbers.size()) < totalRecords);

                    for (final WABusinessPhoneNumber businessPhoneNumber : phoneNumbers) {
                        final Long crmBusinessPhoneNumberId = phoneNumberIdMap.get(businessPhoneNumber.id());
                        final Long templateCount = businessPhoneNumber.getTemplateCount();

                        if (Objects.nonNull(crmBusinessPhoneNumberId)) {
                            final JsonObject businessPhoneNumberJson = Json.object();

                            businessPhoneNumberJson.put(MessengerFields.ID, crmBusinessPhoneNumberId);
                            businessPhoneNumberJson.put(MessengerFields.NAME, businessPhoneNumber.getDisplayPhoneNumber());
                            businessPhoneNumberJson.put(MessengerFields.TEMPLATE_COUNT, Objects.nonNull(templateCount) ? templateCount : 0L);
                            phoneArray.put(businessPhoneNumberJson);
                        }
                    }
                    crmResponse.put(MessengerFields.LIST, phoneArray);
                } catch (Throwable throwable) {
                    LOGGER.error("Whatsapp: Phone numbers retrieved but failed during process. Org Id:{}", orgId);
                    setErrorDescriptor(orgId, businessPhoneNumberIds, throwable, crmResponse);
                } finally {
                    crmResponse.send();
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                setErrorDescriptor(orgId, businessPhoneNumberIds, cause, crmResponse);
                crmResponse.send();
            }
        };

        waBusinessAccountService.fetchNumbersWithTemplateCount(eventJson, callback);
    }

    /**
     * <p>
     * Checks the WhatsApp template sync status
     * </p>
     *
     * @param orgId       Represents the org id
     * @param data        Represents the request data
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void checkTemplateSyncStatus(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final long businessPhoneNumberId = data.getLong(MessengerFields.BUSINESS_PHONE_NUMBER_ID);

        waBusinessAccountManager.getBusinessAccountId(orgId, businessPhoneNumberId).ifPresentOrElse(businessAccountId -> {
            crmResponse.convertIntoAsync();
            waBusinessAccountService.checkTemplateSyncStatus(businessAccountId, new EventCallback<>() {

                @Override
                public void onSuccess(final JsonObject responseJson) {
                    try {
                        if (responseJson.containsKey(WAFields.ERRORS)) {
                            setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.BUSINESS_ACCOUNT_NOT_FOUND, crmResponse.resolveI18nString(responseJson.getString(WAFields.ERRORS))), null, null);
                        } else {
                            crmResponse.put(MessengerFields.STATUS, responseJson.getShort(WAFields.STATUS));

                            if (responseJson.containsKey(WAFields.LAST_SYNCED_TIME)) {
                                crmResponse.put(WAFields.LAST_SYNCED_TIME, responseJson.getLong(WAFields.LAST_SYNCED_TIME));
                            }
                        }
                    } catch (Throwable throwable) {
                        LOGGER.error("Whatsapp: Sync status retrieved but failed during process. Org Id:{}, business phone number Id:{}", orgId, businessPhoneNumberId);
                        setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.FAILED_TO_CHECK_TEMPLATE_SYNC_STATUS), null, null);
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable cause) {
                    LOGGER.error("Whatsapp: Failed to check the template sync status. Org Id:{}, business phone number Id:{}", orgId, businessPhoneNumberId);
                    setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.FAILED_TO_CHECK_TEMPLATE_SYNC_STATUS), null, null);
                    crmResponse.send();
                }
            });
        }, () -> {
            throw ErrorCode.get(WAErrorCodes.BUSINESS_PHONE_NUMBER_NOT_FOUND, "i18n.whatsapp.business.phone.number.not.found");
        });
    }

    /**
     * <p>
     * Initiates the WhatsApp template sync process
     * </p>
     *
     * @param orgId       Represents the org id
     * @param data        Represents the request data
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void syncTemplate(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final long businessPhoneNumberId = data.getLong(MessengerFields.BUSINESS_PHONE_NUMBER_ID);

        waBusinessAccountManager.getBusinessAccountId(orgId, businessPhoneNumberId).ifPresentOrElse(businessAccountId -> {
            crmResponse.convertIntoAsync();

            final EventCallback<JsonObject> templateSyncCallback = new EventCallback<>() {

                @Override
                public void onSuccess(final JsonObject responseJson) {
                    try {
                        if (responseJson.containsKey(WAFields.ERRORS)) {
                            setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.BUSINESS_ACCOUNT_NOT_FOUND, responseJson.getString(WAFields.ERRORS)), null, null);
                        } else {
                            crmResponse.put(MessengerFields.MESSAGE, crmResponse.resolveI18nString(responseJson.getString(WAFields.STATUS)));
                        }
                    } catch (Throwable throwable) {
                        LOGGER.error("Whatsapp: Template sync initiated but failed during process. Org Id:{}, business phone number Id:{}", orgId, businessPhoneNumberId);
                        setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.TEMPLATE_SYNC_INITIATION_FAILED, "i18n.whatsapp.template.sync.initiation.failed"), null, null);
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable cause) {
                    LOGGER.error("Whatsapp: Failed to initiate template sync. Org Id:{}, business phone number Id:{}", orgId, businessPhoneNumberId);
                    setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.TEMPLATE_SYNC_INITIATION_FAILED, "i18n.whatsapp.template.sync.initiation.failed"), null, null);
                    crmResponse.send();
                }
            };

            waBusinessAccountService.syncTemplates(businessAccountId, templateSyncCallback);
        }, () -> {
            throw ErrorCode.get(WAErrorCodes.BUSINESS_PHONE_NUMBER_NOT_FOUND, "i18n.whatsapp.business.phone.number.not.found");
        });
    }

    /**
     * <p>
     * Gets the template language list for the WhatsApp business account
     * </p>
     *
     * @param orgId       Represents the org id
     * @param data        Represents the request data
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void getTemplateLanguageList(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final long businessPhoneNumberId = data.getLong(MessengerFields.BUSINESS_PHONE_NUMBER_ID);

        waBusinessAccountManager.getBusinessAccountId(orgId, businessPhoneNumberId).ifPresentOrElse(businessAccountId -> {
            crmResponse.convertIntoAsync();

            final EventCallback<Collection<Language>> callback = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<Language> languages) {
                    try {
                        final JsonArray languageArray = Json.array();

                        languages.forEach(language -> {
                            final JsonObject languageJson = Json.object();

                            languageJson.put(MessengerFields.ID, language.getId());
                            languageJson.put(MessengerFields.NAME, language.getDisplayLanguage());
                            languageArray.put(languageJson);
                        });

                        crmResponse.put(MessengerFields.LIST, languageArray);
                    } catch (Throwable throwable) {
                        LOGGER.error("Whatsapp: Templates are fetched but failed during process. Org Id:{}, Business phone number Id:{}", orgId, businessAccountId);
                        setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.TEMPLATE_LANGUAGE_RETRIEVAL_FAILED, "i18n.whatsapp.template.languages.retrieval.failed"), null, null);
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable cause) {
                    LOGGER.error("Whatsapp: Failed to fetch the template languages. Org Id:{}, Business phone number Id:{}", orgId, businessAccountId);
                    setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.TEMPLATE_LANGUAGE_RETRIEVAL_FAILED, "i18n.whatsapp.template.languages.retrieval.failed"), null, null);
                    crmResponse.send();
                }
            };

            waBusinessAccountService.fetchTemplateLanguageList(businessAccountId,
                    Arrays.stream(WACampaignTemplateStatus.values()).map(WACampaignTemplateStatus::toWATemplateStatus).toList(), callback);
        }, () -> {
            throw ErrorCode.get(WAErrorCodes.BUSINESS_ACCOUNT_NOT_FOUND, "i18n.social.whatsapp.business.account.not.found");
        });
    }

    /**
     * <p>
     * Sets the error descriptor
     * </p>
     *
     * @param orgId          Represents the org id
     * @param phoneNumberIds Represents the business phone number ids
     * @param crmResponse    Represents the {@link CrmResponse}.
     * @param cause          Represents the {@link Throwable}.
     */
    private void setErrorDescriptor(final Long orgId, final Collection<Long> phoneNumberIds, final Throwable cause, final CrmResponse crmResponse) {
        final String errorDetail = cause.getMessage();

        LoggerUtil.logError(LOGGER, cause, "Whatsapp: Failed to fetch whatsapp phone number list, OrgId: {}, Phone number Ids: {}, Message: {}", orgId, phoneNumberIds, errorDetail);
        setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.FAILED_TO_FETCH_BUSINESS_PHONE_NUMBERS, "i18n.whatsapp.business.phone.numbers.retrieval.failed"), errorDetail, null);
    }
}
package com.tlc.social.client.service.whatsapp.internal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.page.Page;
import com.tlc.commons.pair.Pair;
import com.tlc.social.client.service.whatsapp.WABusinessAccountService;
import com.tlc.social.common.fields.ChatFields;
import com.tlc.commons.language.Language;
import com.tlc.social.common.whatsapp.converter.WATemplateConverter;
import com.tlc.social.common.whatsapp.events.WAEvents;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.model.WABusinessPhoneNumber;

import com.tlc.social.common.whatsapp.model.template.WATemplate;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An implementation of the {@link WABusinessAccountService} which provides client services related to the WhatsApp business account
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WABusinessAccountServiceImpl implements WABusinessAccountService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WABusinessAccountServiceImpl.class);

    private final WATemplateConverter waTemplateConverter;

    private WABusinessAccountServiceImpl() {
        waTemplateConverter = WATemplateConverter.getInstance();
    }

    /**
     * Holds the instance of {@link WABusinessAccountServiceImpl}
     */
    private static final class InstanceHolder {
        private static final WABusinessAccountServiceImpl INSTANCE = new WABusinessAccountServiceImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link WABusinessAccountServiceImpl}
     * </p>
     *
     * @return the instance of {@link WABusinessAccountServiceImpl}
     */
    public static WABusinessAccountServiceImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param data                         Represents the data required to fetch and store WhatsApp business account
     * @param businessAccountStoreCallback Represents the callback to store Whatsapp business account
     */
    @Override
    public void fetchAndStoreWABusinessAccount(final JsonObject data, final EventCallback<JsonObject> businessAccountStoreCallback) {
        final Event<byte[]> businessAccountFetchEvent = new Event<>(WAEvents.FETCH_WA_BUSINESS_ACCOUNT, data.getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                businessAccountStoreCallback.onSuccess(Json.object(bytes));
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.debug("Whatsapp: Failed to fetch and store the business account", throwable);
                businessAccountStoreCallback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(businessAccountFetchEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param phoneNumberIds           Represents the collection of phone number ids
     * @param phoneNumberFetchCallback Represents phone number fetch callback function
     */
    @Override
    public void fetchBusinessPhoneNumbers(final Collection<Long> phoneNumberIds, final EventCallback<Map<Long, WABusinessPhoneNumber>> phoneNumberFetchCallback) {
        final Event<byte[]> phoneNumberFetchEvent = new Event<>(WAEvents.FETCH_WA_BUSINESS_PHONE_NUMBERS, Json.object().put(WAFields.PHONE_NUMBER_IDS, phoneNumberIds).getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                final Map<Long, WABusinessPhoneNumber> phoneNumberMap = new HashMap<>();
                final JsonObject data = Json.object(bytes);
                final JsonArray phoneNumberArray = data.getJsonArray(WAFields.BUSINESS_PHONE_NUMBERS);
                final int phoneNumberSize = phoneNumberArray.size();

                for (int index = 0; index < phoneNumberSize; index++) {
                    final JsonObject phoneNumberJson = phoneNumberArray.getJsonObject(index);
                    final WABusinessPhoneNumber waBusinessPhoneNumber = new WABusinessPhoneNumber();
                    final Long phoneNumberId = phoneNumberJson.getLong(WAFields.ID);

                    waBusinessPhoneNumber.setId(phoneNumberId);
                    waBusinessPhoneNumber.setRegistered(phoneNumberJson.getBoolean(WAFields.IS_REGISTERED));
                    waBusinessPhoneNumber.setDisplayPhoneNumber(phoneNumberJson.getString(WAFields.DISPLAY_NUMBER));

                    phoneNumberMap.put(phoneNumberId, waBusinessPhoneNumber);
                }
                phoneNumberFetchCallback.onSuccess(phoneNumberMap);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.debug("Whatsapp: Failed to fetch business phone numbers, Phone number Ids: {}", phoneNumberIds, throwable);
                phoneNumberFetchCallback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(phoneNumberFetchEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param businessAccountId          Represents the business account id
     * @param start                      Represents the start of the list
     * @param limit                      Represents the limit of the list
     * @param searchText                 Represents the search text to filter the templates based on template name
     * @param templateFetchEventCallback Represents template fetch callback function
     */
    @Override
    public void fetchTemplates(final Long businessAccountId, final int start, final int limit, final String searchText, final EventCallback<Page<WATemplate>> templateFetchEventCallback) {
        final JsonObject eventJson = Json.object();

        eventJson.put(WAFields.BUSINESS_ACCOUNT_ID, businessAccountId);
        eventJson.put(WAFields.START, start);
        eventJson.put(WAFields.LIMIT, limit);

        if (Objects.nonNull(searchText) && !searchText.isBlank()) {
            eventJson.put(WAFields.SEARCH_TEXT, searchText);
        }
        final Event<byte[]> templateFetchEvent = new Event<>(WAEvents.FETCH_WA_TEMPLATES, eventJson.getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                try {
                    final Page<WATemplate> templatePage = new Page<>();
                    final Collection<WATemplate> templates = new ArrayList<>();
                    final JsonObject data = Json.object(bytes);
                    final JsonArray templatesArray = data.getJsonArray(WAFields.TEMPLATES);
                    final int templateSize = templatesArray.size();

                    for (int index = 0; index < templateSize; index++) {
                        final JsonObject templateJson = templatesArray.getJsonObject(index);
                        final WATemplate waTemplate = new WATemplate();

                        waTemplate.setId(templateJson.getLong(WAFields.ID));
                        waTemplate.setName(templateJson.getString(WAFields.NAME));
                        waTemplate.setLanguage(Language.getFromCode(templateJson.getString(WAFields.LANGUAGE)));

                        templates.add(waTemplate);
                    }
                    templatePage.setNumber(data.getInt(WAFields.TOTAL_RECORDS));
                    templatePage.setData(templates);

                    templateFetchEventCallback.onSuccess(templatePage);
                } catch (Throwable throwable) {
                    onFailure(throwable);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.debug("Whatsapp: Failed to fetch templates, Business account Id: {}", businessAccountId, throwable);
                templateFetchEventCallback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(templateFetchEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param businessAccountId          Represents the business account id
     * @param start                      Represents the start of the list
     * @param limit                      Represents the limit of the list
     * @param searchText                 Represents the search text to filter templates by name
     * @param categories                 Represents the category ids
     * @param statuses                   Represents the status ids
     * @param languages                  Represents the language ids
     * @param previewNeeded              Represents whether template preview is needed
     * @param templateFetchEventCallback Represents template fetch callback function
     */
    @Override
    public void fetchTemplatesWithComponents(final Long businessAccountId, final int start, final int limit, final String searchText,
                                             final Collection<Short> categories, final Collection<Short> statuses,
                                             final Collection<Short> languages, final boolean previewNeeded,
                                             final EventCallback<Page<WATemplate>> templateFetchEventCallback) {
        final JsonObject eventJson = Json.object()
                .put(WAFields.BUSINESS_ACCOUNT_ID, businessAccountId)
                .put(WAFields.START, start)
                .put(WAFields.LIMIT, limit)
                .put(WAFields.PREVIEW_NEEDED, previewNeeded);

        if (Objects.nonNull(searchText) && !searchText.isBlank()) {
            eventJson.put(WAFields.SEARCH_TEXT, searchText);
        }

        if (Objects.nonNull(categories) && !categories.isEmpty()) {
            eventJson.put(WAFields.CATEGORY, categories);
        }

        if (Objects.nonNull(statuses) && !statuses.isEmpty()) {
            eventJson.put(WAFields.STATUS, statuses);
        }

        if (Objects.nonNull(languages) && !languages.isEmpty()) {
            eventJson.put(WAFields.LANGUAGE, languages);
        }

        final Event<byte[]> templateFetchEvent = new Event<>(WAEvents.FETCH_WA_TEMPLATES_WITH_COMPONENTS, eventJson.getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                try {
                    final Page<WATemplate> templatePage = new Page<>();
                    final JsonObject data = Json.object(bytes);
                    final JsonArray templatesArray = data.getJsonArray(WAFields.TEMPLATES);
                    final int templateSize = templatesArray.size();
                    final Collection<WATemplate> templates = new ArrayList<>(templateSize);

                    for (int index = 0; index < templateSize; index++) {
                        final JsonObject templateJson = templatesArray.getJsonObject(index);

                        if (previewNeeded) {
                            templates.add(waTemplateConverter.convert(templateJson));
                        } else {
                            final WATemplate waTemplate = new WATemplate();

                            waTemplate.setId(templateJson.getLong(WAFields.ID));
                            waTemplate.setName(templateJson.getString(WAFields.NAME));
                            waTemplate.setLanguage(Language.getFromCode(templateJson.getString(WAFields.LANGUAGE)));
                            waTemplate.setCategory(WATemplate.Category.getById(templateJson.getShort(WAFields.CATEGORY)));
                            waTemplate.setStatus(WATemplate.Status.getById(templateJson.getShort(WAFields.STATUS)));

                            templates.add(waTemplate);
                        }
                    }
                    templatePage.setNumber(data.getInt(WAFields.TOTAL_RECORDS));
                    templatePage.setData(templates);

                    templateFetchEventCallback.onSuccess(templatePage);
                } catch (Throwable throwable) {
                    onFailure(throwable);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.debug("Whatsapp: Failed to fetch templates with components, Business Account Id: {}", businessAccountId, throwable);
                templateFetchEventCallback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(templateFetchEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param data                     Represents the json data to fetch the business phone numbers
     * @param phoneNumberFetchCallback Represents phone number fetch callback function. It returns the pair of {@link WABusinessPhoneNumber} and the chat id via callback
     */
    @Override
    public void fetchBusinessPhoneNumbersWithChat(final JsonObject data, final EventCallback<Map<Long, Pair<WABusinessPhoneNumber, Long>>> phoneNumberFetchCallback) {
        final Event<byte[]> phoneNumberFetchEvent = new Event<>(WAEvents.FETCH_WA_BUSINESS_PHONE_NUMBERS_WITH_CHAT, data.getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                try {
                    final Map<Long, Pair<WABusinessPhoneNumber, Long>> businessPhoneNumberMap = new HashMap<>();
                    final JsonObject data = Json.object(bytes);
                    final JsonArray phoneNumberArray = data.getJsonArray(WAFields.BUSINESS_PHONE_NUMBERS);
                    final int phoneNumberSize = phoneNumberArray.size();

                    for (int index = 0; index < phoneNumberSize; index++) {
                        final JsonObject phoneNumberJson = phoneNumberArray.getJsonObject(index);
                        final Long phoneNumberId = phoneNumberJson.getLong(WAFields.ID);
                        final Long chatId = phoneNumberJson.getNullableValue(ChatFields.CHAT_ID);
                        final WABusinessPhoneNumber waBusinessPhoneNumber = new WABusinessPhoneNumber();

                        waBusinessPhoneNumber.setId(phoneNumberId);
                        waBusinessPhoneNumber.setDisplayPhoneNumber(phoneNumberJson.getString(WAFields.DISPLAY_NUMBER));

                        businessPhoneNumberMap.put(phoneNumberId, Pair.of(waBusinessPhoneNumber, chatId));
                    }
                    phoneNumberFetchCallback.onSuccess(businessPhoneNumberMap);
                } catch (Throwable throwable) {
                    onFailure(throwable);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.debug("Whatsapp: Failed to fetch business phone numbers with chat", throwable);
                phoneNumberFetchCallback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(phoneNumberFetchEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param templateId            Represents the template id
     * @param templateFetchCallback Represents template fetch callback function
     */
    @Override
    public void fetchTemplate(final Long templateId, final EventCallback<WATemplate> templateFetchCallback) {
        final Event<byte[]> phoneNumberFetchEvent = new Event<>(WAEvents.FETCH_WA_TEMPLATE, Json.object().put(WAFields.TEMPLATE_ID, templateId).getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                try {
                    final JsonObject templateJson = Json.object(bytes).getJsonObject(WAFields.TEMPLATE);

                    templateFetchCallback.onSuccess(waTemplateConverter.convert(templateJson));
                } catch (Throwable throwable) {
                    onFailure(throwable);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.debug("Whatsapp: Failed to fetch template, Template Id: {}", templateId, throwable);
                templateFetchCallback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(phoneNumberFetchEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param templateIds           Represents the collection of template ids
     * @param templateFetchCallback Represents the template fetch callback function
     */
    @Override
    public void fetchTemplateNames(final Collection<Long> templateIds, final EventCallback<Map<Long, String>> templateFetchCallback) {
        final Event<byte[]> templateFetchEvent = new Event<>(WAEvents.FETCH_WA_TEMPLATE_NAMES, Json.object().put(WAFields.TEMPLATE_IDS, templateIds).getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                try {
                    final JsonObject templateNames = Json.object(bytes);
                    final Map<Long, String> templateNameMap = new HashMap<>(templateIds.size());

                    templateNames.forEach((key, value) -> templateNameMap.put(Long.valueOf(key), value.toString()));
                    templateFetchCallback.onSuccess(templateNameMap);
                } catch (Throwable throwable) {
                    onFailure(throwable);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.debug("Whatsapp: Failed to fetch template names, Template Ids: {}", templateIds, throwable);
                templateFetchCallback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(templateFetchEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param businessAccountId Represents the business account id
     * @param callback          Represents the template sync status check callback
     */
    @Override
    public void checkTemplateSyncStatus(final Long businessAccountId, final EventCallback<JsonObject> callback) {
        final Event<byte[]> statusEvent = new Event<>(WAEvents.CHECK_WA_TEMPLATE_SYNC_STATUS, Json.object().put(WAFields.BUSINESS_ACCOUNT_ID, businessAccountId).getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                callback.onSuccess(Json.object(bytes));
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.debug("Whatsapp: Failed to check template sync status, Business Account Id: {}", businessAccountId, throwable);
                callback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(statusEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param data                     Represents the json data to fetch the business phone numbers
     * @param phoneNumberFetchCallback Represents phone number fetch callback function. It returns the pair of {@link WABusinessPhoneNumber} and the template count via callback
     */
    @Override
    public void fetchNumbersWithTemplateCount(final JsonObject data, final EventCallback<Page<WABusinessPhoneNumber>> phoneNumberFetchCallback) {
        final Event<byte[]> phoneNumberFetchEvent = new Event<>(WAEvents.FETCH_WA_BUSINESS_PHONE_NUMBERS, data.getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                try {
                    final Page<WABusinessPhoneNumber> phoneNumberPage = new Page<>();
                    final JsonObject responseData = Json.object(bytes);
                    final JsonArray phoneNumberArray = responseData.getJsonArray(WAFields.BUSINESS_PHONE_NUMBERS);
                    final int phoneNumberSize = phoneNumberArray.size();
                    final Collection<WABusinessPhoneNumber> businessPhoneNumbers = new ArrayList<>(phoneNumberSize);

                    for (int index = 0; index < phoneNumberSize; index++) {
                        final JsonObject phoneNumberJson = phoneNumberArray.getJsonObject(index);
                        final WABusinessPhoneNumber waBusinessPhoneNumber = new WABusinessPhoneNumber();

                        waBusinessPhoneNumber.setId(phoneNumberJson.getLong(WAFields.ID));
                        waBusinessPhoneNumber.setDisplayPhoneNumber(phoneNumberJson.getString(WAFields.DISPLAY_NUMBER));
                        waBusinessPhoneNumber.setTemplateCount(phoneNumberJson.optLong(WAFields.TEMPLATE_COUNT, 0L));
                        businessPhoneNumbers.add(waBusinessPhoneNumber);
                    }
                    phoneNumberPage.setNumber(responseData.getInt(WAFields.TOTAL_RECORDS));
                    phoneNumberPage.setData(businessPhoneNumbers);

                    phoneNumberFetchCallback.onSuccess(phoneNumberPage);
                } catch (Throwable throwable) {
                    onFailure(throwable);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.debug("Whatsapp: Failed to fetch numbers with template count", throwable);
                phoneNumberFetchCallback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(phoneNumberFetchEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param businessAccountId Represents the business account id
     * @param callback          Represents the fetch callback
     */
    @Override
    public void fetchTemplateLanguageList(final Long businessAccountId, final Collection<WATemplate.Status> statuses, final EventCallback<Collection<Language>> callback) {
        final JsonObject eventData = Json.object().put(WAFields.BUSINESS_ACCOUNT_ID, businessAccountId).put(WAFields.STATUS,
                statuses.stream().map(WATemplate.Status::getId).collect(Collectors.toUnmodifiableSet()));
        final Event<byte[]> event = new Event<>(WAEvents.FETCH_WA_TEMPLATE_LANGUAGES, eventData.getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                final JsonObject data = Json.object(bytes);
                final JsonArray languageArray = data.getJsonArray(WAFields.LANGUAGE_IDS);
                final Collection<Language> languages = new HashSet<>(languageArray.size());

                languageArray.forEach(languageId -> languages.add(Language.getById(((Number)languageId).shortValue())));

                callback.onSuccess(languages);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.debug("Whatsapp: Failed to fetch template language list, Business Account Id: {}", businessAccountId, throwable);
                callback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(event, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param businessAccountId Represents the business account id
     * @param callback          Represents the sync initiation callback
     */
    @Override
    public void syncTemplates(final Long businessAccountId, final EventCallback<JsonObject> callback) {
        final Event<byte[]> syncEvent = new Event<>(WAEvents.SYNC_WA_TEMPLATES, Json.object().put(WAFields.BUSINESS_ACCOUNT_ID, businessAccountId).getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                callback.onSuccess(Json.object(bytes));
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.debug("Whatsapp: Failed to sync templates, Business Account Id: {}", businessAccountId, throwable);
                callback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(syncEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param businessAccountId Represents the business account id
     * @param statuses          Represents the template statuses
     * @param callback          Represents the fetch callback
     */
    @Override
    public void fetchTemplateCount(final Long businessAccountId, final Collection<WATemplate.Status> statuses,
                                   final EventCallback<Long> callback) {
        final JsonObject eventData = Json.object().put(WAFields.BUSINESS_ACCOUNT_ID, businessAccountId).put(WAFields.STATUS,
                statuses.stream().map(WATemplate.Status::getId).toList());
        final Event<byte[]> event = new Event<>(WAEvents.FETCH_WA_TEMPLATE_COUNT, eventData.getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                try {
                    callback.onSuccess(Json.object(bytes).getLong(WAFields.TEMPLATE_COUNT));
                } catch (Throwable throwable) {
                    onFailure(throwable);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.debug("Whatsapp: Failed to fetch template count, Business Account Id: {}", businessAccountId, throwable);
                callback.onFailure(throwable);
            }
        };

        EventNotifier.get().send(event, eventCallback);
    }
}
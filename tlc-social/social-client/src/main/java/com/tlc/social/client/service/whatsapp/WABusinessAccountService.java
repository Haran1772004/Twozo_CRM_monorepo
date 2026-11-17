package com.tlc.social.client.service.whatsapp;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.language.Language;
import com.tlc.commons.page.Page;
import com.tlc.commons.pair.Pair;
import com.tlc.social.common.whatsapp.model.WABusinessPhoneNumber;
import com.tlc.social.client.service.whatsapp.internal.WABusinessAccountServiceImpl;
import com.tlc.social.common.whatsapp.model.template.WATemplate;
import com.tlc.web.client.event.EventCallback;

import java.util.Collection;
import java.util.Map;

/**
 * Provides the client services related to the WhatsApp Business account
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface WABusinessAccountService {

    /**
     * <p>
     * Gets the instance of {@link WABusinessAccountService}
     * </p>
     *
     * @return the instance of {@link WABusinessAccountService}
     */
    static WABusinessAccountService getInstance() {
        return WABusinessAccountServiceImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the WhatsApp business account along with its phone numbers and store it into database
     * </p>
     *
     * @param data                 Represents the json data required to fetch and store WhatsApp business account
     * @param accountStoreCallback Represents the callback to store Whatsapp business account
     */
    void fetchAndStoreWABusinessAccount(final JsonObject data, final EventCallback<JsonObject> accountStoreCallback);

    /**
     * <p>
     * Fetches the Whatsapp business phone numbers
     * </p>
     *
     * @param phoneNumberIds           Represents the collection of phone number ids
     * @param phoneNumberFetchCallback Represents phone number fetch callback function
     */
    void fetchBusinessPhoneNumbers(final Collection<Long> phoneNumberIds, final EventCallback<Map<Long, WABusinessPhoneNumber>> phoneNumberFetchCallback);

    /**
     * <p>
     * Fetches the Whatsapp message templates
     * </p>
     *
     * @param businessAccountId          Represents the business account id
     * @param start                      Represents the start of the list
     * @param limit                      Represents the limit of the list
     * @param searchText                 Represents the search text to filter the templates based on template name
     * @param templateFetchEventCallback Represents template fetch callback function
     */
    void fetchTemplates(final Long businessAccountId, final int start, final int limit,  final String searchText, final EventCallback<Page<WATemplate>> templateFetchEventCallback);

    /**
     * <p>
     * Fetches the Whatsapp message templates with components
     * </p>
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
    void fetchTemplatesWithComponents(final Long businessAccountId, final int start, final int limit, final String searchText,
                                      final Collection<Short> categories, final Collection<Short> statuses, final Collection<Short> languages,
                                      final boolean previewNeeded, final EventCallback<Page<WATemplate>> templateFetchEventCallback);

    /**
     * <p>
     * Fetches the Whatsapp business phone numbers along with the chat ID (if it exists) corresponding to the chat between the business phone number and the recipient phone number
     * </p>
     *
     * @param data                     Represents the json data to fetch the business phone numbers
     * @param phoneNumberFetchCallback Represents phone number fetch callback function. It returns the pair of {@link WABusinessPhoneNumber} and the chat id via callback
     */
    void fetchBusinessPhoneNumbersWithChat(final JsonObject data, final EventCallback<Map<Long, Pair<WABusinessPhoneNumber, Long>>> phoneNumberFetchCallback);

    /**
     * <p>
     * Fetches the {@link WATemplate}
     * </p>
     *
     * @param templateId Represents the template id
     * @param callback   Represents template fetch callback function
     */
    void fetchTemplate(final Long templateId, final EventCallback<WATemplate> callback);

    /**
     * <p>
     * Fetches the {@link WATemplate} names
     * </p>
     *
     * @param templateIds Represents the collection of template ids
     * @param callback   Represents template name fetch callback function
     */
    void fetchTemplateNames(final Collection<Long> templateIds, final EventCallback<Map<Long, String>> callback);

    /**
     * <p>
     * Fetches the WhatsApp business phone numbers with template count
     * </p>
     *
     * @param data                     Represents the json data to fetch the business phone numbers
     * @param phoneNumberFetchCallback Represents phone number fetch callback function
     */
    void fetchNumbersWithTemplateCount(final JsonObject data, final EventCallback<Page<WABusinessPhoneNumber>> phoneNumberFetchCallback);

    /**
     * <p>
     * Checks the template sync status of the WhatsApp business account
     * </p>
     *
     * @param businessAccountId Represents the business account id
     * @param callback          Represents the template sync status check callback
     */
    void checkTemplateSyncStatus(final Long businessAccountId, final EventCallback<JsonObject> callback);

    /**
     * <p>
     * Fetches the distinct template languages for the WhatsApp business account
     * </p>
     *
     * @param businessAccountId Represents the business account id
     * @param statuses          Represents the template statuses
     * @param callback          Represents the fetch callback
     */
    void fetchTemplateLanguageList(final Long businessAccountId, final Collection<WATemplate.Status> statuses, final EventCallback<Collection<Language>> callback);

    /**
     * <p>
     * Initiates the WhatsApp template sync process for the business account
     * </p>
     *
     * @param businessAccountId Represents the business account id
     * @param callback          Represents the sync initiation callback
     */
    void syncTemplates(final Long businessAccountId, final EventCallback<JsonObject> callback);

    /**
     * <p>
     * Fetches the template count for the WhatsApp business account
     * </p>
     *
     * @param businessAccountId Represents the business account id
     * @param statuses          Represents the template statuses
     * @param callback          Represents the fetch callback
     */
    void fetchTemplateCount(final Long businessAccountId, final Collection<WATemplate.Status> statuses, final EventCallback<Long> callback);
}
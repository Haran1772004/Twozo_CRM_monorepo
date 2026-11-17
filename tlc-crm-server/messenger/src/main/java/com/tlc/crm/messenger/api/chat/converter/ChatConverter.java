package com.tlc.crm.messenger.api.chat.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.messenger.api.chat.model.CrmChat;
import com.tlc.crm.messenger.converter.MessengerConverter;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.social.common.model.Chat;
import com.tlc.social.common.model.Platform;
import com.tlc.social.common.model.RecipientProfile;
import com.tlc.social.common.model.Channel;
import com.tlc.social.common.model.Message;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Converts chat related models.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class ChatConverter implements MessengerConverter<Map<Long, Chat>, Collection<CrmChat>> {

    private static final int MESSAGE_PREVIEW_SIZE = 30;

    private final FieldService fieldService;
    private final FieldTypeService fieldTypeService;

    private ChatConverter() {
        fieldService = FieldService.getInstance();
        fieldTypeService = FieldTypeService.getInstance();
    }

    private static final class Instance {
        private static final ChatConverter CRM_CHAT_CONVERTER = new ChatConverter();
    }

    /**
     * <p>
     * Gets {@link ChatConverter}.
     * </p>
     *
     * @return {@link ChatConverter}.
     */
    public static ChatConverter getInstance() {
        return Instance.CRM_CHAT_CONVERTER;
    }

    /**
     * <p>
     * Converts chat info to {@link JsonObject}.
     * </p>
     *
     * @param chatMap  {@link Chat}.
     * @param crmChats {@link CrmChat}.
     * @return {@link JsonObject}.
     */
    @Override
    public JsonObject convert(final Map<Long, Chat> chatMap, final Collection<CrmChat> crmChats) {
        final JsonArray chatArray = Json.array();
        final String firstNameKey = ContactFields.FIRST_NAME;
        final String phonesKay = ContactFields.PHONES;
        final Map<String, FieldInfo> requiredContactFields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, firstNameKey, phonesKay);
        final Map<StoreSubType, FieldSubType> phonesSubtypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.PHONES);

        for (final CrmChat crmChat : crmChats) {
            final JsonObject chatJson = Json.object();
            final Chat chat = chatMap.get(crmChat.id());

            if (Objects.nonNull(chat)) {
                final Channel channel = chat.getChannel();
                final RecipientProfile recipientProfile = chat.getProfile();
                final RecipientProfile crmRecipientProfile = crmChat.getProfile();
                final Message message = chat.getLastMessage();
                final Platform platform = chat.getPlatform();
                final boolean isWhatsApp = Platform.WHATSAPP == platform;
                final CrmUser owner = crmChat.getOwner();
                final FieldInfo firstNameField = requiredContactFields.get(firstNameKey);

                chatJson.put(MessengerFields.ID, crmChat.getCrmChatId());
                chatJson.put(MessengerFields.PLATFORM, platform.getId());

                if (Objects.nonNull(firstNameField)) {
                    chatJson.put(MessengerFields.FIRST_NAME_FIELD_ID, firstNameField.id());
                }

                if (isWhatsApp) {
                    final Contact contact = crmChat.getContact();
                    final FieldInfo phonesField = requiredContactFields.get(phonesKay);

                    if (Objects.nonNull(phonesField)) {
                        chatJson.put(MessengerFields.PHONES_FIELD_ID, phonesField.id());

                        if (!phonesSubtypes.isEmpty()) {
                            final JsonObject phoneSubFieldJson = Json.object();

                            phoneSubFieldJson.put(MessengerFields.PHONE_ID, phonesSubtypes.get(StoreSubType.PHONE_NUMBER).id());
                            phoneSubFieldJson.put(MessengerFields.PHONE_TYPE_ID, phonesSubtypes.get(StoreSubType.PHONE_TYPE).id());
                            phoneSubFieldJson.put(MessengerFields.COUNTRY_CODE_ID, phonesSubtypes.get(StoreSubType.COUNTRY_CODE).id());

                            chatJson.put(MessengerFields.PHONES_SUB_FIELD, phoneSubFieldJson);
                        }
                    }

                    if (Objects.nonNull(contact)) {
                        chatJson.put(MessengerFields.ASSIGNED_CONTACT_ID, contact.id());
                    }
                }

                if (Objects.nonNull(channel)) {
                    final JsonObject pageInfoJson = Json.object();

                    pageInfoJson.put(MessengerFields.ID, channel.id());
                    pageInfoJson.put(MessengerFields.NAME, channel.getName());
                    chatJson.put(MessengerFields.PAGE_INFO, pageInfoJson);
                }

                if (Objects.nonNull(recipientProfile) && Objects.nonNull(crmRecipientProfile)) {
                    final JsonObject recipientJson = Json.object();
                    final String name = crmRecipientProfile.getName();

                    recipientJson.put(MessengerFields.ID, recipientProfile.id());

                    if (isWhatsApp) {
                        final int delimiterIndex = name.lastIndexOf("|");

                        if (delimiterIndex != -1) {
                            recipientJson.put(MessengerFields.NAME, name.substring(0, delimiterIndex));
                            recipientJson.put(MessengerFields.PHONE_NUMBER, name.substring(delimiterIndex + 1));
                        } else {
                            recipientJson.put(MessengerFields.NAME, name);

                            if (name.startsWith(MessengerFields.COUNTRY_CODE_PREFIX)) {
                                recipientJson.put(MessengerFields.PHONE_NUMBER, name);
                            }
                        }
                    } else {
                        recipientJson.put(MessengerFields.NAME, name);
                    }
                    chatJson.put(MessengerFields.RECIPIENT, recipientJson);
                }

                if (Objects.nonNull(message)) {
                    chatJson.put(MessengerFields.LAST_MESSAGE, crmChat.getRecentMessagePreview(MESSAGE_PREVIEW_SIZE));
                    chatJson.put(MessengerFields.MESSAGE_TYPE, message.getMessageFlow());
                    chatJson.put(MessengerFields.LAST_MESSAGE_TIME, message.getSentTime());
                }
                chatJson.put(MessengerFields.UNREAD_COUNT, crmChat.getUnreadCount());

                if (Objects.nonNull(owner)) {
                    final JsonObject userJson = Json.object();

                    userJson.put(MessengerFields.ID, owner.id());
                    userJson.put(MessengerFields.NAME, owner.getFullName());

                    if (userJson.size() > NumericalConstant.ZERO) {
                        chatJson.put(MessengerFields.SALES_OWNER, userJson);
                    }
                }
            }
            chatArray.put(chatJson);
        }

        return Json.object().put(MessengerFields.LIST, chatArray);
    }
}

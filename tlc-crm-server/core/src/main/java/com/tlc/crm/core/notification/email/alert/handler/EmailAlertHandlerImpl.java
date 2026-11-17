package com.tlc.crm.core.notification.email.alert.handler;

import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.common.email.alert.annotation.EmailAlertField;
import com.tlc.crm.common.email.alert.annotation.EmailAlertTemplate;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.email.service.NoReplyMailService;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.handler.EventRequestHandler;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.service.EventRequestService;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.service.FieldPropertiesService;
import com.tlc.crm.common.service.PropertyDescriptorService;
import com.tlc.crm.common.service.UserPreferenceHolderService;

import com.tlc.crm.core.api.iam.manager.UserNotificationSettingsManager;
import com.tlc.crm.core.notification.email.alert.container.EmailAlertFieldValueExtractorRegistry;
import com.tlc.crm.core.notification.email.alert.model.EmailAlertFieldValueExtractionData;
import com.tlc.crm.core.notification.email.alert.model.EmailAlertInfo;

import com.tlc.crm.common.util.NumberUtils;

import com.tlc.commons.util.TypeChecker;

import com.tlc.crm.notification.app.model.EmailNotificationSettings;
import com.tlc.crm.notification.app.model.MentionAlertNotificationSettings;
import com.tlc.crm.notification.app.model.UserNotificationSettings;
import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;

import com.tlc.notification.model.alert.EmailNotifiableModel;

import com.tlc.email.client.template.mail.EmailNotificationBuilder;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;

import com.tlc.notification.service.template.EmailTemplateBuildRequest;

import com.tlc.validator.TlcModel;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Implements the {@link EmailAlertHandler} and provides functionality for handling email alert template types
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public final class EmailAlertHandlerImpl implements EmailAlertHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailAlertHandlerImpl.class);
    private static final String MODEL = "model";
    private static final String TRANSFER_USER_RECORDS = "transferUserRecords";
    private static final String USER_ID = "userId";
    private static final String NOTIFICATION = "notification";
    private static final String FROM_KEY = "from";
    private static final String MAIL_SERVICE = "mailService";
    private static final String USERS = "users";
    private static final String COMMA = ", ";
    private static final String RECIPIENT = "recipient";

    private final FieldPropertiesService fieldPropertiesService;
    private final PropertyDescriptorService propertyDescriptorService;
    private final UserService userService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;
    private final EmailNotificationBuilder emailNotificationBuilder;
    private final UserNotificationSettingsManager userNotificationSettingsManager;
    private final String from;
    private final EventRequestHandler eventRequestHandler;
    private final EmailAlertFieldValueExtractorRegistry emailAlertFieldValueExtractorRegistry;
    private final NoReplyMailService noReplyMailService;
    private final OrganizationService organizationService;
    private final UserPreferenceHolderService userPreferenceHolderService;

    private EmailAlertHandlerImpl() {
        final Configurations configurations = ConfHolder.getInstance().getProperties(NOTIFICATION).getNestedConfigs(MAIL_SERVICE);

        fieldPropertiesService = FieldPropertiesService.getInstance();
        propertyDescriptorService = PropertyDescriptorService.getInstance();
        userService = UserService.getInstance();
        crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
        emailNotificationBuilder = EmailNotificationBuilder.getInstance();
        userNotificationSettingsManager = UserNotificationSettingsManager.getInstance();

        from = configurations.getConfig(FROM_KEY);
        eventRequestHandler = EventRequestService.get().getEventHandler();
        emailAlertFieldValueExtractorRegistry = EmailAlertFieldValueExtractorRegistry.getInstance();
        noReplyMailService = NoReplyMailService.get();
        organizationService = OrganizationService.getInstance();
        userPreferenceHolderService = UserPreferenceHolderService.get();
    }

    private static class InstanceHolder {
        private static final EmailAlertHandlerImpl INSTANCE = new EmailAlertHandlerImpl();
    }

    /**
     * Gets the {@link EmailAlertHandlerImpl} instance
     *
     * @return The  {@link EmailAlertHandlerImpl}
     */
    public static EmailAlertHandlerImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents the organization ID.
     * @param userId         Represents the user ID.
     * @param models         Represents the collection of models which triggered the event.
     * @param previousValues Represents the map of previous values of the models with model ID as key.
     * @param action         Represents the action performed (CREATE or UPDATE).
     * @param template       Represents the template of email alert
     */
    @Override
    public void sendEmailAlerts(final long orgId, final long userId, final Collection<? extends TlcModel> models,
                                final Map<Long, TlcModel> previousValues, final Action action, final String template) {

        if (!models.isEmpty()) {
            models.stream().findAny().ifPresent(model -> {
                final Class<? extends TlcModel> tlcModelClass = model.getClass();

                if (model instanceof EmailNotifiableModel &&
                        tlcModelClass.isAnnotationPresent(EmailAlertTemplate.class)) {
                    final Map<Long, TlcModel> newModelValues = models.stream()
                            .collect(Collectors.toMap(TlcModel::id, Function.identity(),
                                    (existing, replacement) -> replacement));
                    final Collection<EmailAlertInfo> emailAlerts = new ArrayList<>();

                    for (final TlcModel tlcModel : models) {
                        final long id = tlcModel.id();
                        final Map<EmailNotificationTemplate, Long> templateUserIds = new EnumMap<>(EmailNotificationTemplate.class);

                        extractAndMapTemplateUserIds(tlcModel, templateUserIds);
                        final Collection<EmailNotificationTemplate> userEnabledTemplates = fetchUserEnabledTemplates(orgId,
                                userId, action, template, tlcModelClass.getAnnotation(EmailAlertTemplate.class).templates(), templateUserIds);

                        logTemplateNames(orgId, userId, "EmailAlert: User enabled templates for org ID {}, user ID {}, with template names: {}", userEnabledTemplates);

                        if (!userEnabledTemplates.isEmpty()) {
                            final Map<EmailNotificationTemplate, Map<String, Collection<Object>>> templateFieldValuesMap = new EnumMap<>(EmailNotificationTemplate.class);
                            final TlcModel newModel = newModelValues.get(id);
                            final EmailAlertInfo emailAlertInfo = new EmailAlertInfo();

                            switch (action) {
                                case CREATE -> filterCreateActionTemplate(userId, newModel, userEnabledTemplates);
                                case UPDATE ->
                                        filterUpdateActionTemplates(newModel, previousValues.get(id), userEnabledTemplates, templateFieldValuesMap);
                            }
                            emailAlertInfo.setOrgId(orgId);
                            emailAlertInfo.setTlcModel(tlcModel);
                            emailAlertInfo.setUserEnabledEmailTemplates(userEnabledTemplates);
                            logTemplateNames(orgId, userId, "EmailAlert: Filtered user enabled templates for org ID {}, user ID {}, with template names: {}", userEnabledTemplates);
                            loadEmailAlertField(emailAlertInfo);
                            emailAlertInfo.setTlcModel(tlcModel);

                            if (Action.UPDATE == action && !templateFieldValuesMap.isEmpty()) {
                                loadRecipientsToEmailAlertInfo(emailAlertInfo, templateFieldValuesMap);
                            }
                            emailAlerts.add(emailAlertInfo);
                        }
                    }

                    if (!emailAlerts.isEmpty()) {
                        fetchUsersAndDispatchTemplates(orgId, emailAlerts);
                    }
                }
            });
        }
    }

    /**
     * <p>
     * Loads the recipients details into {@link EmailAlertInfo}
     * </p>
     *
     * @param emailAlertInfo         Represents the  {@link EmailAlertInfo}
     * @param templateFieldValuesMap The {@link Map} to store field values grouped by templates
     */
    private void loadRecipientsToEmailAlertInfo(final EmailAlertInfo emailAlertInfo, final Map<EmailNotificationTemplate, Map<String, Collection<Object>>> templateFieldValuesMap) {

        for (final Map.Entry<EmailNotificationTemplate, Map<String, Collection<Object>>> templateFieldValuesEntry : templateFieldValuesMap.entrySet()) {
            final EmailNotificationTemplate template = templateFieldValuesEntry.getKey();
            final Map<String, Collection<Object>> fieldValues = templateFieldValuesEntry.getValue();
            final Map<String, Collection<Long>> userIdMap = new HashMap<>();
            final Map<String, Collection<String>> currentAlertRecipients = emailAlertInfo.getRecipients(template);

            for (final Map.Entry<String, Collection<Object>> fieldEntry : fieldValues.entrySet()) {
                boolean hasRecipientRemoved = true;
                final String fieldName = fieldEntry.getKey();

                for (final Object value : fieldEntry.getValue()) {

                    if (TypeChecker.isString(value)) {

                        if (Objects.nonNull(currentAlertRecipients) && !currentAlertRecipients.isEmpty()
                                && (currentAlertRecipients.containsKey(fieldName))) {

                            if (hasRecipientRemoved) {
                                currentAlertRecipients.remove(fieldName);
                                hasRecipientRemoved = false;
                            }
                            emailAlertInfo.addRecipient(template, fieldName, value.toString());
                        }
                    } else if (value instanceof final Long userId) {
                        userIdMap.computeIfAbsent(fieldName, userIds -> new HashSet<>()).add(userId);
                    }
                }
            }

            if (!userIdMap.isEmpty()) {
                final Map<EmailNotificationTemplate, Map<String, Collection<Long>>> emailAlertInfoTemplateRecipientUserIds = emailAlertInfo.getTemplateRecipientUserIds();

                if (Objects.nonNull(emailAlertInfoTemplateRecipientUserIds) && !emailAlertInfoTemplateRecipientUserIds.isEmpty()) {
                    final Map<String, Collection<Long>> userIdsByField = emailAlertInfoTemplateRecipientUserIds.get(template);

                    if (Objects.nonNull(userIdsByField)) {

                        userIdMap.forEach((fieldName, userIds) -> {

                            if (userIdsByField.containsKey(fieldName)) {
                                userIdsByField.remove(fieldName);
                                emailAlertInfo.addTemplateRecipientUserIds(template, fieldName, userIds);
                            }
                        });
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Filters out create action template types based on the user is and {@link TlcModel}
     * </p>
     *
     * @param userId               Represents the user id
     * @param tlcModel             Represents the {@link TlcModel}
     * @param userEnabledTemplates Which contains the collection of {@link EmailNotificationTemplate} to filter
     */
    private void filterCreateActionTemplate(final long userId, final TlcModel tlcModel, final Collection<EmailNotificationTemplate> userEnabledTemplates) {
        final Map<EmailNotificationTemplate, Map<String, Object>> fieldValuesByTemplate = new EnumMap<>(EmailNotificationTemplate.class);

        extractTemplateFieldValues(tlcModel, fieldValuesByTemplate, userEnabledTemplates);

        for (final Map.Entry<EmailNotificationTemplate, Map<String, Object>> fieldValuesByTemplateEntry : fieldValuesByTemplate.entrySet()) {
            final EmailNotificationTemplate emailNotificationTemplate = fieldValuesByTemplateEntry.getKey();

            if (userEnabledTemplates.contains(emailNotificationTemplate) && emailNotificationTemplate.isAssignedTemplate()) {
                final Map<String, Object> fieldValues = fieldValuesByTemplateEntry.getValue();

                if (Objects.nonNull(fieldValues)) {
                    final Object assignedUserId = fieldValues.get(USER_ID);

                    if (Objects.equals(assignedUserId, userId)) {
                        userEnabledTemplates.remove(emailNotificationTemplate);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Filters out update action template types based on the differences between the new and old {@link TlcModel}
     * </p>
     *
     * @param newModel               Represents the new {@link TlcModel}
     * @param oldModel               Represents the old {@link TlcModel}
     * @param userEnabledTemplates   Which contains the collection of {@link EmailNotificationTemplate} to filter
     * @param templateFieldValuesMap The {@link Map} containing store extracted field values by each {@link EmailNotificationTemplate}
     */
    private void filterUpdateActionTemplates(final TlcModel newModel, final TlcModel oldModel, final Collection<EmailNotificationTemplate> userEnabledTemplates,
                                             final Map<EmailNotificationTemplate, Map<String, Collection<Object>>> templateFieldValuesMap) {
        final Map<EmailNotificationTemplate, Map<String, Object>> newModelFieldValuesMap = new EnumMap<>(EmailNotificationTemplate.class);
        final Map<EmailNotificationTemplate, Map<String, Object>> oldModelFieldValuesMap = new EnumMap<>(EmailNotificationTemplate.class);

        extractTemplateFieldValues(newModel, newModelFieldValuesMap, userEnabledTemplates);
        extractTemplateFieldValues(oldModel, oldModelFieldValuesMap, userEnabledTemplates);

        for (final Map.Entry<EmailNotificationTemplate, Map<String, Object>> newModelFieldValuesEntry : newModelFieldValuesMap.entrySet()) {
            final Map<String, Object> newFieldValues = newModelFieldValuesEntry.getValue();
            final EmailNotificationTemplate template = newModelFieldValuesEntry.getKey();

            if (userEnabledTemplates.contains(template)) {
                final Map<String, Object> oldFieldValues = oldModelFieldValuesMap.get(template);
                boolean isFieldChanged = false;

                for (final Map.Entry<String, Object> newFieldValueEntry : newFieldValues.entrySet()) {
                    final String fieldName = newFieldValueEntry.getKey();
                    final Object oldValue = oldFieldValues.get(fieldName);
                    final Object newValue = newFieldValueEntry.getValue();

                    if (Objects.nonNull(newValue) && Objects.nonNull(oldValue)) {

                        if (isNumberOrString(newValue) && isNumberOrString(oldValue)) {

                            if (!Objects.equals(newValue, oldValue)) {
                                templateFieldValuesMap.computeIfAbsent(template, templateRecipient -> new HashMap<>())
                                        .computeIfAbsent(fieldName, values -> new HashSet<>()).add(newValue);
                                isFieldChanged = true;
                            }
                        } else if (newValue instanceof final Collection<?> newCollection && oldValue instanceof final Collection<?> oldCollection) {
                            final Map<EmailNotificationTemplate, Map<String, Collection<Object>>> newCollectionValuesMap = new EnumMap<>(EmailNotificationTemplate.class);
                            final Map<EmailNotificationTemplate, Map<String, Collection<Object>>> oldCollectionValuesMap = new EnumMap<>(EmailNotificationTemplate.class);

                            for (final Object fieldValue : newCollection) {

                                if (isNumberOrString(fieldValue) && !oldCollection.contains(fieldValue)) {
                                    templateFieldValuesMap.computeIfAbsent(template, templateRecipient -> new HashMap<>())
                                            .computeIfAbsent(fieldName, values -> new HashSet<>()).add(fieldValue);
                                    isFieldChanged = true;
                                }

                                if (fieldValue instanceof final TlcModel model) {
                                    populateTemplateFieldValues(model, newCollectionValuesMap, userEnabledTemplates);
                                }
                            }

                            for (final Object fieldValue : oldCollection) {

                                if (fieldValue instanceof final TlcModel model) {
                                    populateTemplateFieldValues(model, oldCollectionValuesMap, userEnabledTemplates);
                                }
                            }
                            final Map<String, Collection<Object>> newFieldsMap = newCollectionValuesMap.get(template);
                            final Map<String, Collection<Object>> oldFieldsMap = oldCollectionValuesMap.get(template);

                            if (Objects.nonNull(newFieldsMap) && Objects.nonNull(oldFieldsMap)) {

                                for (final Map.Entry<String, Collection<Object>> newFieldsEntry : newFieldsMap.entrySet()) {
                                    final String name = newFieldsEntry.getKey();
                                    final Collection<Object> newCollectionValues = newFieldsEntry.getValue();
                                    final Collection<Object> oldCollectionValues = oldFieldsMap.get(name);

                                    for (final Object value : newCollectionValues) {

                                        if (!oldCollectionValues.contains(value)) {
                                            templateFieldValuesMap.computeIfAbsent(template, templateRecipient -> new HashMap<>())
                                                    .computeIfAbsent(name, values -> new HashSet<>()).add(value);
                                            isFieldChanged = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isFieldChanged) {
                    userEnabledTemplates.add(template);
                } else {
                    userEnabledTemplates.remove(template);
                }
            }
        }
    }

    /**
     * <p>
     * Extracts field values from the provided {@link TlcModel}
     * </p>
     *
     * @param model                 Represents the {@link TlcModel}
     * @param fieldValuesByTemplate A {@link Map} containing extracted field values by each {@link EmailNotificationTemplate}
     * @param templates             Which contains the collection of {@link EmailNotificationTemplate} to filter
     */
    private void extractTemplateFieldValues(final TlcModel model, final Map<EmailNotificationTemplate, Map<String, Object>> fieldValuesByTemplate,
                                            final Collection<EmailNotificationTemplate> templates) {
        for (final java.lang.reflect.Field field : fieldPropertiesService.getAnnotatedFields(model.getClass(), EmailAlertField.class)) {
            final EmailAlertField emailAlertField = field.getAnnotation(EmailAlertField.class);
            final EmailAlertFieldType emailAlertFieldType = emailAlertField.fieldType();

            emailAlertFieldValueExtractorRegistry.get(emailAlertFieldType).ifPresent(emailAlertFieldValueExtractor ->
                    emailAlertFieldValueExtractor.extractTemplateFieldValues(new EmailAlertFieldValueExtractionData(model, fieldValuesByTemplate, templates)));
        }
    }

    /**
     * <p>
     * Extracts the values of annotated fields from the given model and populates them into a map
     * *grouped by email notification templates
     * </p>
     *
     * @param model                 Represents the {@link TlcModel} from which to extract field values
     * @param fieldValuesByTemplate The {@link Map} to store field values grouped by templates
     * @param templates             Which contains the collection of email notification templates to consider
     */
    private void populateTemplateFieldValues(final TlcModel model, final Map<EmailNotificationTemplate, Map<String, Collection<Object>>> fieldValuesByTemplate,
                                             final Collection<EmailNotificationTemplate> templates) {
        for (final java.lang.reflect.Field field : fieldPropertiesService.getAnnotatedFields(model.getClass(), EmailAlertField.class)) {
            final EmailAlertField emailAlertField = field.getAnnotation(EmailAlertField.class);

            getValueFromField(field, model).ifPresent(value -> {
                final String fieldName = emailAlertField.name();

                if (isNumberOrString(value) && emailAlertField.requirePreviousValue()) {
                    final EmailNotificationTemplate[] placeholderTemplates = emailAlertField.placeholderTemplates();

                    for (final EmailNotificationTemplate emailNotificationTemplate : placeholderTemplates) {

                        if (templates.contains(emailNotificationTemplate)) {
                            fieldValuesByTemplate.computeIfAbsent(emailNotificationTemplate, templateFieldValues
                                    -> new HashMap<>()).computeIfAbsent(fieldName, values -> new HashSet<>()).add(value);
                        }
                    }
                } else {
                    if (value instanceof final TlcModel tlcModel) {
                        populateTemplateFieldValues(tlcModel, fieldValuesByTemplate, templates);
                    } else if (value instanceof final Collection<?> values) {

                        for (final Object fieldValue : values) {

                            if (fieldValue instanceof final TlcModel tlcModel) {
                                populateTemplateFieldValues(tlcModel, fieldValuesByTemplate, templates);
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * <p>
     * Loads email alert fields from the given {@link TlcModel}
     * </p>
     *
     * @param emailAlertInfo Represents {@link EmailAlertInfo}
     */
    private void loadEmailAlertField(final EmailAlertInfo emailAlertInfo) {
        final TlcModel model = emailAlertInfo.getTlcModel();
        final Collection<EmailNotificationTemplate> userEnabledEmailTemplates = emailAlertInfo.getUserEnabledEmailTemplates();

        for (final java.lang.reflect.Field field : fieldPropertiesService.getAnnotatedFields(model.getClass(), EmailAlertField.class)) {
            final EmailAlertField emailAlertField = field.getAnnotation(EmailAlertField.class);

            getValueFromField(field, model).ifPresent(value -> {
                final String fieldName = emailAlertField.name();

                if (emailAlertField.isUserField() && TypeChecker.isNonDecimalNumber(value)) {
                    final long userId = NumberUtils.parseNumber(value).longValue();
                    final EmailNotificationTemplate[] placeholderTemplates = emailAlertField.placeholderTemplates();
                    final EmailNotificationTemplate[] recipientTemplates = emailAlertField.recipientTemplates();

                    for (final EmailNotificationTemplate template : recipientTemplates) {

                        if (userEnabledEmailTemplates.contains(template)) {
                            emailAlertInfo.addTemplateRecipientUserIds(template, fieldName, Set.of(userId));
                        }
                    }

                    for (final EmailNotificationTemplate template : placeholderTemplates) {

                        if (userEnabledEmailTemplates.contains(template)) {
                            emailAlertInfo.addTemplatesUserIds(template, List.of(userId));
                        }
                    }
                } else if (TypeChecker.isString(value)) {
                    final EmailNotificationTemplate[] recipientTemplatesTypes = emailAlertField.recipientTemplates();

                    for (final EmailNotificationTemplate template : recipientTemplatesTypes) {

                        if (userEnabledEmailTemplates.contains(template)) {
                            emailAlertInfo.addRecipient(template, fieldName, (String) value);
                        }
                    }
                } else {
                    loadFieldFromModel(value, emailAlertField, emailAlertInfo, userEnabledEmailTemplates);
                }
            });
        }
    }

    /**
     * <p>
     * Loads field information from the provided value into the {@link EmailAlertInfo}.
     * </p>
     *
     * @param value           Which can be a {@link TlcModel} or a collection of {@link TlcModel}
     * @param emailAlertField Represents the {@link EmailAlertField} containing template information
     * @param emailAlertInfo  Represents the {@link EmailAlertInfo}
     * @param templates       Which contains the collection of {@link EmailNotificationTemplate}
     */
    private void loadFieldFromModel(final Object value, final EmailAlertField emailAlertField, final EmailAlertInfo emailAlertInfo,
                                    final Collection<EmailNotificationTemplate> templates) {

        if (value instanceof final TlcModel tlcModel) {
            emailAlertInfo.setTlcModel(tlcModel);
            loadEmailAlertField(emailAlertInfo);
        } else if (value instanceof final Collection<?> models) {
            final String fieldName = emailAlertField.name();

            if (emailAlertField.isUserField()) {
                final EmailNotificationTemplate[] placeholderTemplates = emailAlertField.placeholderTemplates();
                final EmailNotificationTemplate[] recipientTemplates = emailAlertField.recipientTemplates();
                final Collection<Long> userIds = (Collection<Long>) value;

                for (final EmailNotificationTemplate template : placeholderTemplates) {

                    if (templates.contains(template)) {
                        emailAlertInfo.addTemplateRecipientUserIds(template, fieldName, userIds);
                    }
                }

                for (final EmailNotificationTemplate template : recipientTemplates) {

                    if (templates.contains(template)) {
                        emailAlertInfo.addTemplatesUserIds(template, userIds);
                    }
                }
            } else {
                for (final Object model : models) {

                    if (model instanceof final TlcModel tlcModel) {
                        emailAlertInfo.setTlcModel(tlcModel);
                        loadEmailAlertField(emailAlertInfo);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Fetches users based on the provided template user IDs, retrieves their corresponding IAM IDs,
     * dispatches templates accordingly for email alerts
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param emailAlerts Which contains collection of {@link EmailAlertInfo}
     */
    private void fetchUsersAndDispatchTemplates(final long orgId, final Collection<EmailAlertInfo> emailAlerts) {
        final Collection<Long> userIds = new HashSet<>();

        for (final EmailAlertInfo emailAlertInfo : emailAlerts) {
            final Map<EmailNotificationTemplate, Map<String, Collection<Long>>> templateRecipientUserIds = emailAlertInfo.getTemplateRecipientUserIds();
            final Map<EmailNotificationTemplate, Collection<Long>> templateToMultipleUserIdsMap = emailAlertInfo.getTemplateUserIds();

            if (Objects.nonNull(templateRecipientUserIds) && !templateRecipientUserIds.isEmpty()) {
                templateRecipientUserIds.entrySet().stream().filter(entry -> Objects.nonNull(entry.getValue()))
                        .flatMap(entry -> entry.getValue().entrySet().stream()).filter(Objects::nonNull)
                        .forEach(ids -> userIds.addAll(ids.getValue()));
            }

            if (Objects.nonNull(templateToMultipleUserIdsMap) && !templateToMultipleUserIdsMap.isEmpty()) {
                templateToMultipleUserIdsMap.values().stream().flatMap(Collection::stream).forEach(userIds::add);
            }
        }

        if (!userIds.isEmpty()) {
            final Collection<Long> iamUserIds = crmUserService.getUsers(orgId, userIds).stream().map(CrmUser::getIamId).collect(Collectors.toSet());

            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                /**
                 * {@inheritDoc}
                 *
                 * @param users Response from the consumer
                 */
                @Override
                public void onSuccess(final Collection<User> users) {
                    LOGGER.debug("EmailAlert: Successfully retrieved user information for org ID {}", orgId);
                    final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                    eventRequestHandler.handle(() -> {
                        for (final EmailAlertInfo emailAlert : emailAlerts) {
                            final Map<String, EmailNotifiableModel> notifiableModelMap = new HashMap<>();
                            final Map<EmailNotificationTemplate, Map<String, Collection<Long>>> templateRecipientUserIds = emailAlert.getTemplateRecipientUserIds();

                            if (Objects.nonNull(templateRecipientUserIds)) {
                                templateRecipientUserIds.forEach((emailAlertTemplate, userIds) -> {

                                    if (Objects.nonNull(userIds) && !userIds.isEmpty()) {

                                        for (final Map.Entry<String, Collection<Long>> userIdEntry : userIds.entrySet()) {
                                            final Collection<CrmUser> crmUsers = crmUserService.getUsers(orgId, userIdEntry.getValue());

                                            crmUsers.forEach(crmUser -> emailAlert.addRecipient(emailAlertTemplate, userIdEntry.getKey(), userMap.get(crmUser.getIamId()).getEmail()));
                                        }
                                    }
                                });
                            }
                            notifiableModelMap.put(MODEL, (EmailNotifiableModel) emailAlert.getTlcModel());
                            final Collection<EmailNotificationTemplate> userEnabledEmailTemplates = emailAlert.getUserEnabledEmailTemplates();

                            for (final EmailNotificationTemplate template : userEnabledEmailTemplates) {
                                putRecipient(orgId, emailAlert, template, userMap, notifiableModelMap);
                                dispatchEmailAlerts(template, buildEmailTemplateDataHolder(emailAlert, userMap,
                                        notifiableModelMap, template), emailAlert);
                            }
                        }
                    });
                }

                /**
                 *{@inheritDoc}
                 * @param cause Cause of the failure
                 */
                @Override
                public void onFailure(final Throwable cause) {
                    LOGGER.error("User fetch action failed, Reason : {}", cause.getMessage());
                    LOGGER.debug("Stack: ", cause);
                }
            };
            userService.fetchUsersWithOrg(organizationService.getIamOrgId(orgId), iamUserIds, usersFetchCallBack);
        }
    }

    /**
     * <p>
     * Builds the {@link EmailTemplateBuildRequest}
     * </p>
     *
     * @param emailAlertInfo     Represents the {@link EmailAlertInfo}
     * @param userMap            A {@link Map} containing key is the user id and value is the {@link User}
     * @param notifiableModelMap A {@link Map} containing key is model name value is the {@link EmailNotifiableModel}
     * @param template           Represents the {@link EmailNotificationTemplate}
     * @return The {@link EmailTemplateBuildRequest}
     */
    private EmailTemplateBuildRequest buildEmailTemplateDataHolder(final EmailAlertInfo emailAlertInfo, final Map<Long, User> userMap,
                                                                   final Map<String, EmailNotifiableModel> notifiableModelMap,
                                                                   final EmailNotificationTemplate template) {
        final long orgId = emailAlertInfo.getOrgId();
        final EmailTemplateBuildRequest emailNotificationData = new EmailTemplateBuildRequest(notifiableModelMap);
        final Map<EmailNotificationTemplate, Collection<Long>> templateUserIds = emailAlertInfo.getTemplateUserIds();

        if (Objects.nonNull(templateUserIds)) {
            final Collection<Long> userIds = templateUserIds.get(template);

            if (Objects.nonNull(userIds)) {
                final Collection<CrmUser> crmUsers = crmUserService.getUsers(orgId, userIds);
                final Collection<EmailNotifiableModel> notifiableModels = crmUsers.stream().peek(crmUser -> crmUser.setUser(userMap.get(crmUser.getIamId()))).collect(Collectors.toList());
                final Map<String, Collection<EmailNotifiableModel>> notifiableModelsMap = new HashMap<>();

                notifiableModelsMap.put(USERS, notifiableModels);
                emailNotificationData.setTemplateModels(notifiableModelsMap);
            }
        }
        return emailNotificationData;
    }

    /**
     * <p>
     * Dispatches email alerts based on the provided email notification template
     * </p>
     *
     * @param emailNotificationTemplate Represents the {@link EmailNotificationTemplate}
     * @param emailTemplateDataHolder   Represents the {@link EmailTemplateBuildRequest}
     * @param emailAlertInfo            Represents {@link EmailAlertInfo}
     */
    private void dispatchEmailAlerts(final EmailNotificationTemplate emailNotificationTemplate, final EmailTemplateBuildRequest emailTemplateDataHolder,
                                     final EmailAlertInfo emailAlertInfo) {
        final Map<String, Collection<String>> emailAlertInfoRecipients = emailAlertInfo.getRecipients(emailNotificationTemplate);
        final String emailNotificationTemplateName = emailNotificationTemplate.getName();

        if (Objects.nonNull(emailAlertInfoRecipients) && !emailAlertInfoRecipients.isEmpty()) {
            final Collection<String> recipients = emailAlertInfoRecipients.entrySet().stream().filter(Objects::nonNull)
                    .flatMap(recipient -> recipient.getValue().stream()).collect(Collectors.toUnmodifiableSet());

            if (!recipients.isEmpty()) {
                final Optional<Email> email = emailNotificationBuilder.build(emailNotificationTemplateName, emailTemplateDataHolder);

                email.ifPresent(notification -> {
                    final long orgId = emailAlertInfo.getOrgId();

                    LOGGER.debug("EmailAlert: Model values bound to template for org ID {}, template name {}", orgId, emailNotificationTemplateName);
                    final EmailRecipients emailRecipients = new EmailRecipients();
                    final Collection<EmailAddress> emailAddresses = recipients.stream().map(EmailAddress::new)
                            .collect(Collectors.toUnmodifiableSet());

                    emailRecipients.addRecipients(AddressType.TO, emailAddresses);
                    emailRecipients.addRecipients(AddressType.FROM, Set.of(new EmailAddress(from)));
                    notification.setRecipients(List.of(emailRecipients));

                    noReplyMailService.send(notification);
                    LOGGER.debug("EmailAlert: Sent email alert with template for org ID {} and template name {}",
                            orgId, emailNotificationTemplateName);
                });
            }
        }
    }

    /**
     * <p>
     * Fetches a collection of {@link EmailNotificationTemplate} objects based on user notification settings
     * and email alert configuration
     * </p>
     *
     * @param orgId             Represents the organization id
     * @param userId            Represents the user ID
     * @param action            Represents the {@link Action}
     * @param emailTemplate     Represents the emailTemplate of the alert
     * @param existingTemplates Which contains the collection of {@link EmailNotificationTemplate}
     * @param templateUserIds   A {@link Map} contains key as {@link EmailNotificationTemplate} value as user id
     * @return The collection of {@link EmailNotificationTemplate}
     */
    private Collection<EmailNotificationTemplate> fetchUserEnabledTemplates(final long orgId, final long userId,
                                                                            final Action action, final String emailTemplate,
                                                                            final EmailNotificationTemplate[] existingTemplates,
                                                                            final Map<EmailNotificationTemplate, Long> templateUserIds) {
        final Collection<EmailNotificationTemplate> templates = new HashSet<>();
        final boolean isTemplatePresent = Objects.nonNull(emailTemplate);
        final EmailNotificationTemplate currentTemplate = isTemplatePresent ?
                EmailNotificationTemplate.getByName(emailTemplate) : null;
        final boolean isCurrentTemplateUndefined = Objects.isNull(currentTemplate);

        for (final EmailNotificationTemplate template : existingTemplates) {
            final UserNotificationSettings userNotificationSettings =
                    userNotificationSettingsManager.retrieveUserNotificationSettings(orgId, templateUserIds.getOrDefault(template, userId));
            final EmailNotificationSettings emailNotificationSettings = userNotificationSettings.getEmailNotificationSettings();
            final MentionAlertNotificationSettings mentionAlertNotificationSettings = userNotificationSettings.getMentionsAlertNotificationSettings();

            if (template.getTypes().contains(action)) {
                switch (template) {
                    case ACTIVITY_CREATED, ACTIVITY_MODIFIED -> {
                        if (isCurrentTemplateUndefined &&
                                emailNotificationSettings.isModifiedActivity()) {
                            templates.add(template);
                        }
                    }
                    case ACTIVITY_COMPLETED -> {
                        if (emailNotificationSettings.isActivityCompleted()
                                && Objects.equals(template, currentTemplate)) {
                            templates.add(template);
                        }
                    }
                    case ACTIVITY_ASSIGNED -> {
                        if (emailNotificationSettings.isActivityAssigned()) {
                            templates.add(template);
                        }
                    }
                    case CONTACT_ASSIGNED, COMPANY_ASSIGNED, DEAL_ASSIGNED, PRODUCT_ASSIGNED -> {
                        if (emailNotificationSettings.isSourceAssigned()) {

                            if (isCurrentTemplateUndefined || template.isTransferredTemplate()) {
                                templates.add(template);
                            }
                        }
                    }
                    case CONTACT_TRANSFERRED, COMPANY_TRANSFERRED, DEAL_TRANSFERRED, PRODUCT_TRANSFERRED -> {
                        if (emailNotificationSettings.isSourceTransferred()) {

                            if (isTemplatePresent && emailTemplate.equals(TRANSFER_USER_RECORDS)
                                    && template.isTransferredTemplate()) {
                                templates.add(template);
                            }
                        }
                    }
                    case USER_INVITATION, FORGOT_PASSWORD -> {
                        if (template.equals(currentTemplate)) {
                            templates.add(template);
                        }
                    }
                    case CONTACT_NOTE_MENTION, CONTACT_NOTE_COMMENT_MENTION, DEAL_NOTE_MENTION,
                         DEAL_NOTE_COMMENT_MENTION, COMPANY_NOTE_MENTION, COMPANY_NOTE_COMMENT_MENTION -> {
                        if (mentionAlertNotificationSettings.isEntityEmailEnabled()
                                && Objects.equals(template, currentTemplate)) {
                            templates.add(template);
                        }
                    }
                    case ACTIVITY_NOTE_MENTION -> {
                        if (mentionAlertNotificationSettings.isActivityPrivateNoteEmailEnabled()
                                && Objects.equals(template, currentTemplate)) {
                            templates.add(template);
                        }
                    }
                    case ORGANIZATION_EMAIL, USER_DEACTIVATION,
                         DATA_EXPORT, ACTIVITY_GUEST_INVITATION -> templates.add(template);
                }
            }
        }

        return templates;
    }

    /**
     * Extracts and maps user IDs for email notification templates based on fields annotated with {@link EmailAlertField}.
     *
     * @param model           Represents the {@link TlcModel}
     * @param templateUserIds A {@link Map} containing key as {@link EmailNotificationTemplate} value as user id
     */
    private void extractAndMapTemplateUserIds(final TlcModel model, final Map<EmailNotificationTemplate, Long> templateUserIds) {
        for (final java.lang.reflect.Field field : fieldPropertiesService.getAnnotatedFields(model.getClass(), EmailAlertField.class)) {
            final EmailAlertField emailAlertField = field.getAnnotation(EmailAlertField.class);

            getValueFromField(field, model).ifPresent(value -> {

                if (emailAlertField.isUserField() && TypeChecker.isNonDecimalNumber(value)) {
                    final EmailNotificationTemplate[] emailNotificationTemplates = emailAlertField.recipientTemplates();

                    for (final EmailNotificationTemplate emailNotificationTemplate : emailNotificationTemplates) {

                        if (emailNotificationTemplate.isEmailAlertTemplate()) {
                            templateUserIds.put(emailNotificationTemplate, (Long) value);
                        }
                    }
                } else if (value instanceof final TlcModel tlcModel) {
                    extractAndMapTemplateUserIds(tlcModel, templateUserIds);
                }
            });
        }
    }

    /**
     * <p>
     * Fetches the value of the specified field from the provided {@link TlcModel}
     * </p>
     *
     * @param field    The field whose value needs to be retrieved
     * @param tlcModel Represents the {@link TlcModel}
     * @return An Optional containing the value of the field if found, or an empty Optional if not found or an error occurred
     */
    private <T> Optional<T> getValueFromField(final Field field, final TlcModel tlcModel) {
        final PropertyDescriptor propertyDescriptor = propertyDescriptorService.get(field.getName(), tlcModel.getClass());

        try {
            return (Optional<T>) Optional.ofNullable(propertyDescriptor.getReadMethod().invoke(tlcModel));
        } catch (final IllegalAccessException | InvocationTargetException exception) {
            LOGGER.debug("Error while getting value from field {}", field.getName(), exception);
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Checks whether the given value is a decimal number, a non-decimal number, or a string, as determined by the TypeChecker utility.
     * </p>
     *
     * @param value the value to be checked
     * @return true if the value is a decimal number, a non-decimal number, or a string, false otherwise
     */
    private boolean isNumberOrString(final Object value) {
        return TypeChecker.isDecimalNumber(value) || TypeChecker.isNonDecimalNumber(value) || TypeChecker.isString(value);
    }

    /**
     * <p>
     * Logs the names of the provided email notification templates
     * </p>
     *
     * @param orgId     Represents the org id
     * @param userId    Represents the user id
     * @param message   The log message template with placeholders for the org ID, user id, and template names
     * @param templates A {@link Collection} of email notification templates whose names will be logged.
     */
    private void logTemplateNames(final long orgId, final long userId, final String message, final Collection<EmailNotificationTemplate> templates) {
        final StringBuilder templateNames = new StringBuilder();

        for (final EmailNotificationTemplate template : templates) {
            templateNames.append(template.getName()).append(COMMA);
        }
        LOGGER.debug(message, orgId, userId, templateNames);
    }

    /**
     * Adds the recipient user to the notifiable model map for the given email notification template.
     *
     * @param orgId                Represents the organization ID
     * @param emailAlert           Represents the email alert information
     * @param template             Represents the email notification template
     * @param userMap              Represents the map of users keyed by IAM user ID
     * @param notifiableModelMap   Represents the map of notifiable models to populate
     */
    private void putRecipient(final long orgId, final EmailAlertInfo emailAlert, final EmailNotificationTemplate template,
                              final Map<Long, User> userMap, final Map<String, EmailNotifiableModel> notifiableModelMap) {
        notifiableModelMap.remove(RECIPIENT);
        final Map<EmailNotificationTemplate, Map<String, Collection<Long>>> byTemplate = emailAlert.getTemplateRecipientUserIds();
        final Map<String, Collection<Long>> byField = Objects.nonNull(byTemplate) ? byTemplate.get(template) : null;

        if (Objects.isNull(byField) || byField.isEmpty()) {
            return;
        }
        final Collection<Long> recipientIds = byField.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());

        crmUserService.getUsers(orgId, recipientIds).stream().findFirst().ifPresent(crmUser -> {
            crmUser.setUser(userMap.get(crmUser.getIamId()));
            crmUser.setPreferenceHolder(userPreferenceHolderService.get(orgId, crmUser.id()));
            notifiableModelMap.put(RECIPIENT, crmUser);
        });
    }
}

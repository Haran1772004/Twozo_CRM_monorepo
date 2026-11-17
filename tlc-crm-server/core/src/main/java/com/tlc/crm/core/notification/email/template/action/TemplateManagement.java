package com.tlc.crm.core.notification.email.template.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.product.model.Product;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.crm.core.notification.email.template.api.extractor.MailPlaceHolderExtractor;
import com.tlc.crm.core.template.PlaceHolderValueInterpreter;
import com.tlc.crm.core.notification.email.template.api.model.EntityMailTemplate;
import com.tlc.crm.core.notification.email.template.api.model.TemplateTag;
import com.tlc.crm.core.notification.email.template.api.model.converter.TemplateConverter;
import com.tlc.crm.core.notification.email.template.api.model.fields.TemplateCompanyFields;
import com.tlc.crm.core.notification.email.template.api.model.fields.TemplateContactFields;
import com.tlc.crm.core.notification.email.template.api.model.fields.TemplateDealFields;
import com.tlc.crm.core.notification.email.template.fields.TemplateFields;
import com.tlc.crm.core.notification.email.template.fields.TemplateFolderFields;
import com.tlc.crm.core.notification.email.template.status.TemplateErrorCodes;
import com.tlc.crm.core.notification.email.template.status.TemplateFolderErrorCodes;
import com.tlc.crm.core.notification.email.template.status.TemplateTagErrorCodes;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.fields.settings.SettingsFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.fields.service.settings.SourceSettingsService;
import com.tlc.crm.core.notification.email.template.api.TemplateFolderManager;
import com.tlc.crm.core.notification.email.template.api.TemplateManager;
import com.tlc.crm.core.notification.email.template.api.TemplateTagManager;
import com.tlc.crm.core.notification.email.internal.OutboundEmailService.Action;
import com.tlc.crm.core.notification.email.template.api.model.placeholder.MailPlaceHolder;

import com.tlc.crm.core.notification.email.fields.CoreEmailFields;

import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.RolePermissions;
import com.tlc.crm.security.permission.model.Action.Type;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.crm.core.notification.email.template.service.MailPlaceHolderResolver;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.file.common.models.Attachment;

import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.User;

import com.tlc.notification.model.template.PlaceHolder;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link EntityMailTemplate} related web actions
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
@WebAction(path = "/email/template/mgmt")
public class TemplateManagement extends AuthorizationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateManagement.class);

    private static final int DEFAULT_START = 1;
    private static final int DEFAULT_LIMIT = 50;
    private static final int TEMPLATE_FOLDER_LIMIT = 40;
    private static final byte MAXIMUM_TAG_LIMIT = 5;
    private static final int CONTACT_FULL_NAME_FIELD_ID = NumericalConstant.NEGATIVE_ONE;

    private final FieldService fieldService;
    private final TemplateManager templateManager;
    private final TemplateConverter templateConverter;
    private final TemplateTagManager templateTagManager;
    private final TemplateFolderManager templateFolderManager;
    private final ConverterConfigBuilder converterConfigBuilder;
    private final UserService userService;
    private final OrganizationService organizationService;
    private final SourceSettingsService sourceSettingsService;
    private final FieldSourceService fieldSourceService;
    private final MailPlaceHolderResolver placeHolderResolver;
    private final MailPlaceHolderExtractor placeHolderExtractor;
    private final PlaceHolderValueInterpreter placeHolderValueInterpreter;
    private final AttachmentService attachmentService;
    private final RolePermissionService rolePermissionService;

    public TemplateManagement() {
        fieldService = FieldService.getSecureInstance();
        templateManager = TemplateManager.getInstance();
        templateConverter = TemplateConverter.getInstance();
        templateTagManager = TemplateTagManager.getInstance();
        templateFolderManager = TemplateFolderManager.getInstance();
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        userService = UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        sourceSettingsService = SourceSettingsService.getInstance();
        fieldSourceService = FieldSourceService.getInstance();
        placeHolderResolver = MailPlaceHolderResolver.getInstance();
        placeHolderExtractor = MailPlaceHolderExtractor.getInstance();
        placeHolderValueInterpreter = PlaceHolderValueInterpreter.getInstance();
        this.attachmentService = AttachmentService.getInstance();
        this.rolePermissionService = RolePermissionService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     The received request
     * @param crmResponse The response to be sent
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(TemplateFields.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(TemplateFields.DATA.getName());
        final Long orgId = request.orgId();

        switch (type) {
            case "create" -> constructTemplate(orgId, List.of(convert(request, data)), crmResponse);
            case "update" -> updateTemplate(orgId, List.of(convert(request, data)), crmResponse);
            case "pUpdate" -> updateTemplate(orgId, List.of(convertPartialData(request, data)), crmResponse);
            case "delete" -> delete(crmResponse, orgId, List.of(data.getLong(CoreEmailFields.ID.getName())));
            case "bDelete" ->
                    delete(crmResponse, orgId, data.getJsonArray(CoreEmailFields.IDS.getName()).toList().stream().
                            map(id -> (Long) id).collect(Collectors.toUnmodifiableSet()));
            case "list" -> crmResponse.put(TemplateFields.LIST.getName(), getTemplateList(orgId, request, crmResponse));
            case "get" -> fetchAndDispatchTemplate(request, data, crmResponse, false);
            case "resolve" -> fetchAndDispatchTemplate(request, data, crmResponse, true);
            case "changeFolder" -> switchFolder(orgId, data, crmResponse);
            case "statistics" ->
                    crmResponse.put(TemplateFields.STATISTICS.getName(), getTemplateStatistics(orgId, request, crmResponse));
            case "getPlaceHolderFields" -> fetchAndDispatchPlaceHolderFields(orgId, request, crmResponse);
            case "clone" -> cloneTemplate(request, data, crmResponse);
            case "getPlaceHolderSources" ->
                    crmResponse.put(TemplateFields.SOURCE.getName(), getPlaceHolderSources(orgId));
            case "getPlaceHolderFieldValues" -> fetchAndDispatchFieldValues(orgId, request, crmResponse);
        }
    }

    /**
     * <p>
     * Clones the template in the folder for the provided template id in the request
     * </p>
     *
     * @param request     Represents the crm request
     * @param data        Represents the request data
     * @param crmResponse Represents the crm response
     */
    private void cloneTemplate(final CrmRequest request, final JsonObject data, final CrmResponse crmResponse) {
        final String idKey = TemplateFields.ID.getName();

        if (data.containsKey(idKey)) {
            final Long orgId = request.orgId();
            final Long templateId = data.getLong(idKey);
            final EntityMailTemplate template = new EntityMailTemplate();

            template.setOrgId(orgId);
            template.setId(templateId);

            templateManager.populateTemplateFromSource(template);
            template.setName(generateUniqueName(orgId, template.getName()));

            if (template.getName().length() >= 70) {
                crmResponse.put(TemplateFields.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.email.template.invalid.cloningName"));
            }
            constructTemplate(orgId, List.of(template), crmResponse);
        } else {
            throw ErrorCode.get(TemplateErrorCodes.TEMPLATE_ID_NOT_FOUND, crmResponse.resolveI18nString("i18n.email.template.id.invalid"));
        }
    }

    /**
     * <p>
     * Generates the unique template name from the existing template name provided
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param existingName Represents the existing template name
     * @return {@link String} newly generated template name
     */
    private String generateUniqueName(final Long orgId, final String existingName) {
        int index = 1;
        String generatedName = existingName;

        while (templateManager.exists(orgId, generatedName)) {
            generatedName = String.format("%s(%d)", existingName, index);
            index++;
        }

        return generatedName;
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into a {@link EntityMailTemplate}
     * </p>
     *
     * @param request    Represents the crm request
     * @param jsonObject Represents the request data
     * @return the {@link EntityMailTemplate} that holds the template details
     */
    private EntityMailTemplate convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return templateConverter.convert(converterConfig, jsonObject);
    }

    /**
     * Converts the {@link EntityMailTemplate} into the {@link JsonObject}
     *
     * @param template Represents the {@link EntityMailTemplate} that holds the template details
     * @return {@link JsonObject}
     */
    private JsonObject convert(final EntityMailTemplate template) {
        return templateConverter.convert(template);
    }

    /**
     * <p>
     * Converts partial data into {@link EntityMailTemplate}
     * </p>
     *
     * @param request    Represents the crm request
     * @param jsonObject Represents the request data
     * @return the {@link EntityMailTemplate} that holds the template details
     */
    private EntityMailTemplate convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return templateConverter.convert(converterConfig, jsonObject);
    }

    /**
     * <p>
     * Constructs a template for a specified organization and manages the completion of uploads and error handling
     * for attachments using callback methods
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param templates   which contains {@link Collection} of {@link EntityMailTemplate}
     * @param crmResponse Represents the crm response
     */
    private void constructTemplate(final Long orgId, final Collection<EntityMailTemplate> templates, final CrmResponse crmResponse) {
        final Collection<ErrorDescriptor> errors = ValidatorServiceProvider.get().modelValidator().validate(templates, Group.Create.class);

        if (errors.isEmpty() && isValidDetails(orgId, Action.CREATE, templates)) {
            final ProcessOutcome processOutcome = templateManager.create(orgId, templates);

            crmResponse.put(CoreEmailFields.CREATED_RECORDS.getName(), processOutcome.getProcessedRecords());
            final ErrorSummary errorSummary = processOutcome.getErrorSummary();

            if (Objects.nonNull(errorSummary)) {
                crmResponse.setErrorSummary(errorSummary);
                crmResponse.setErrorCode(ErrorCode.get(EmailErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed"));
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(EmailErrorCodes.CREATE_ACTION_FAILED, "i18n.common.create.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode,
                    ErrorKeys.DATA_VALIDATION_FAILED), errors));
        }
    }

    /**
     * <p>
     * Updates a template for a specified organization and manages the completion of uploads and error handling
     * for attachments using callback methods
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param templates   which contains {@link Collection} of {@link EntityMailTemplate}
     * @param crmResponse Represents the crm response
     */
    private void updateTemplate(final Long orgId, final Collection<EntityMailTemplate> templates, final CrmResponse crmResponse) {
        final Collection<ErrorDescriptor> errors = ValidatorServiceProvider.get().modelValidator().validate(templates, Group.Update.class);

        if (errors.isEmpty() && isValidDetails(orgId, Action.UPDATE, templates) && templateManager.exists(orgId,
                templates.stream().map(EntityMailTemplate::id).collect(Collectors.toUnmodifiableSet()))) {
            final ProcessOutcome processOutcome = templateManager.update(orgId, templates);

            crmResponse.put(CoreEmailFields.UPDATED_RECORDS.getName(), processOutcome.getProcessedRecords());
            final ErrorSummary errorSummary = processOutcome.getErrorSummary();

            if (Objects.nonNull(errorSummary)) {
                crmResponse.setErrorCode(ErrorCode.get(EmailErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed"));
                crmResponse.setErrorSummary(errorSummary);
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(EmailErrorCodes.UPDATE_ACTION_FAILED, "i18n.common.update.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, CommonErrorCodes.DATA_VALIDATION_FAILED.name()), errors));
        }
    }

    /**
     * <p>
     * Checks whether the template details are valid or not
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param action    Represents the {@link Action} for given details
     * @param templates Represent the {@link Collection} of {@link EntityMailTemplate}
     * @return True if all the details are valid, otherwise returns false
     */
    private boolean isValidDetails(final Long orgId, final Action action, final Collection<EntityMailTemplate> templates) {
        if (Objects.nonNull(templates) && !templates.isEmpty()) {

            switch (action) {
                case CREATE, UPDATE -> {
                    for (final EntityMailTemplate template : templates) {
                        final Collection<TemplateTag> templateTags = template.getTemplateTags();
                        final Set<Long> attachmentIds = template.getAttachmentIds();

                        if (templateManager.exists(orgId, template.id(), template.getName())) {
                            throw ErrorCode.get(TemplateErrorCodes.TEMPLATE_NAME_ALREADY_EXISTS, "i18n.template.name.already.exists");
                        }

                        if (!templateFolderManager.exists(orgId, template.getFolderId())) {
                            throw ErrorCode.get(TemplateFolderErrorCodes.TEMPLATE_FOLDER_NOT_FOUND, "i18n.email.template.folder.not.found");
                        }

                        if (templateManager.getTemplatesCountInFolder(orgId, template.getFolderId()) >= TEMPLATE_FOLDER_LIMIT) {
                            throw ErrorCode.get(TemplateErrorCodes.TEMPLATE_FOLDER_LIMIT_EXCEEDED, "i18n.email.template.folder.limit.exceeded");
                        }

                        if (Objects.nonNull(attachmentIds) && !attachmentIds.isEmpty()) {

                            if (attachmentService.exists(attachmentIds)) {
                                final Collection<Attachment> attachments = attachmentService.getAttachments(attachmentIds);

                                template.setAttachments(attachments);
                            } else {
                                throw ErrorCode.get(TemplateErrorCodes.ATTACHMENT_NOT_FOUND, "i18n.email.template.attachment.not.found");
                            }
                        }

                        if (Objects.nonNull(templateTags) && !templateTags.isEmpty()) {
                            final Collection<Long> templateTagIds = new HashSet<>();
                            final Collection<String> templateTagNames = new HashSet<>();

                            if (MAXIMUM_TAG_LIMIT < templateTags.size()) {
                                throw ErrorCode.get(TemplateTagErrorCodes.TEMPLATE_TAG_LIMIT_EXCEEDED, "i18n.email.template.tag.limit.exceeded");
                            }

                            for (final TemplateTag templateTag : templateTags) {
                                final Long id = templateTag.id();

                                if (Objects.nonNull(id)) {
                                    templateTagIds.add(id);
                                } else {
                                    templateTagNames.add(templateTag.getName());
                                }
                            }

                            if (!templateTagIds.isEmpty() && !templateTagManager.exists(orgId, templateTagIds)) {
                                throw ErrorCode.get(TemplateTagErrorCodes.TEMPLATE_TAG_NOT_FOUND, "i18n.email.template.tag.not.found");
                            }

                            if (!templateTagNames.isEmpty() && templateTagManager.checkNamesExistence(orgId, templateTagNames)) {
                                throw ErrorCode.get(TemplateTagErrorCodes.TEMPLATE_TAG_NAME_ALREADY_EXISTS, "i18n.email.template.tag.name.already.exists");
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * <p>
     * Moves the selected templates to another folder
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param data     An object that holds request data
     * @param response Represents the crm response
     */
    private void switchFolder(final Long orgId, final JsonObject data, final CrmResponse response) {
        final Long folderId = data.getLong(TemplateFolderFields.ID.getName());
        final Long targetFolderId = data.getLong(TemplateFolderFields.TARGET_FOLDER.getName());

        if (!templateFolderManager.exists(orgId, List.of(folderId, targetFolderId))) {
            throw ErrorCode.get(TemplateFolderErrorCodes.TEMPLATE_FOLDER_NOT_FOUND, "i18n.email.template.folder.not.found");
        }
        final JsonArray jsonArray = data.getJsonArray(TemplateFolderFields.TEMPLATES.getName());

        if (templateManager.getTemplatesCountInFolder(orgId, targetFolderId) + jsonArray.size() > 40) {
            throw ErrorCode.get(TemplateErrorCodes.TEMPLATE_FOLDER_LIMIT_EXCEEDED, "i18n.email.template.folder.limit.exceeded");
        }
        final Collection<Long> templateIds = new HashSet<>();

        for (int index = 0; index < jsonArray.size(); index++) {
            templateIds.add(jsonArray.getLong(index));
        }

        if (!templateManager.exists(orgId, templateIds)) {
            throw ErrorCode.get(TemplateErrorCodes.TEMPLATE_NOT_FOUND, "i18n.email.template.not.found");
        }

        if (!templateManager.isFolderAlreadyMapped(orgId, folderId, templateIds)) {
            throw ErrorCode.get(TemplateErrorCodes.TEMPLATE_FOLDER_NOT_MAPPED, "i18n.email.template.folder.not.mapped");
        }
        templateManager.switchFolder(orgId, folderId, targetFolderId, templateIds);
        response.put("message", response.resolveI18nString("i18n.email.template.folder.changed.successfully"));
    }

    /**
     * <p>
     * Fetches the template list
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param request  Represents the crm request
     * @param response Represents the crm response
     * @return {@link JsonArray} of templates
     */
    private JsonArray getTemplateList(final Long orgId, final CrmRequest request, final CrmResponse response) {
        final JsonArray jsonArray = Json.array();
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(TemplateFields.DATA.getName());
        final Long userId = request.userId();
        final int start = Math.max(data.optInt(TemplateFields.START.getName(), DEFAULT_START), DEFAULT_START);
        final int limit = Math.min(data.optInt(TemplateFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final Long totalRecords = templateManager.fetchAllFolderTemplatesCount(orgId, userId);

        if (totalRecords > 0 && start <= totalRecords) {
            final Collection<EntityMailTemplate> templates = templateManager.getTemplateList(orgId, userId, start, limit);

            if (Objects.nonNull(templates)) {
                final Collection<Long> attachmentIds = new HashSet<>();

                for (final EntityMailTemplate template : templates) {
                    final Set<Long> templateAttachmentIds = template.getAttachmentIds();

                    if (Objects.nonNull(templateAttachmentIds)) {
                        attachmentIds.addAll(template.getAttachmentIds());
                    }
                }
                if (!attachmentIds.isEmpty()) {
                    final Map<Long, Attachment> attachmentMap = attachmentService.getAttachments(attachmentIds).stream().collect(Collectors.toMap(Attachment::id, Function.identity()));

                    for (final EntityMailTemplate template : templates) {
                        final Set<Long> templateAttachmentIds = template.getAttachmentIds();

                        if (Objects.nonNull(templateAttachmentIds)) {

                            for (final Long attachmentId : templateAttachmentIds) {
                                template.addAttachment(attachmentMap.get(attachmentId));
                            }
                        }
                    }
                }
                templates.forEach(template -> jsonArray.put(convert(template)));
            }
        }
        response.put(TemplateFields.TOTAL_RECORDS.getName(), totalRecords);
        response.put(TemplateFields.HAS_MORE.getName(), (start + limit) <= totalRecords);

        return jsonArray;
    }

    /**
     * <p>
     * Fetches the dispatches the template filled with required fields
     * </p>
     *
     * @param request         Represents the received request
     * @param data            Represents the json object
     * @param response        Represents the {@link CrmResponse}
     * @param requiresResolve Represents a flag indicates whether to resolve placeholders or not.
     */
    private void fetchAndDispatchTemplate(final CrmRequest request, final JsonObject data, final CrmResponse response,
                                          final boolean requiresResolve) {
        final String idKey = TemplateFields.ID.getName();
        final EntityMailTemplate template = convert(request, data);

        if (data.containsKey(idKey)) {

            if (templateManager.exists(request.orgId(), data.getLong(idKey))) {
                templateManager.populateTemplateFromSource(template);
                final Collection<Long> attachmentIds = template.getAttachmentIds();

                if (!attachmentIds.isEmpty()) {
                    template.setAttachments(attachmentService.getAttachments(attachmentIds));
                }
                dispatchTemplate(response, template, requiresResolve);
            } else {
                response.setErrorCode(ErrorCode.get(TemplateErrorCodes.TEMPLATE_NOT_FOUND, "i18n.email.template.not.found"));
                response.send();
            }
        } else {
            if (Objects.isNull(template.getEntityId())) {
                template.setSource(null);
            }
            dispatchTemplate(response, template, requiresResolve);
        }
    }

    /**
     * <p>
     * Dispatches the template with or without resolving the placeholders
     * </p>
     *
     * @param response        Represents the {@link CrmResponse}
     * @param template        Represents the {@link EntityMailTemplate}
     * @param requiresResolve Represents a flag indicates whether to resolve placeholders or not.
     */
    private void dispatchTemplate(final CrmResponse response, final EntityMailTemplate template, final boolean requiresResolve) {
        final Collection<MailPlaceHolder> placeHolders = requiresResolve ? placeHolderExtractor.getPlaceHolders(template) : Collections.emptyList();

        if (Objects.nonNull(placeHolders) && !placeHolders.isEmpty()) {
            resolveOwnerPlaceHoldersToDeliver(template, response, placeHolders);
        } else {
            try {
                response.put(TemplateFields.TEMPLATE.getName(), convert(template));
            } finally {
                response.send();
            }
        }
    }

    /**
     * <p>
     * Handles the owner field placeHolders present in template either to replace or fetch owner details
     * and then dispatch it
     * </p>
     *
     * @param template     Represents the {@link EntityMailTemplate}
     * @param response     Represents the {@link CrmResponse}
     * @param placeHolders Represents the {@link Map} of {@link MailPlaceHolder}
     */
    private void resolveOwnerPlaceHoldersToDeliver(final EntityMailTemplate template, final CrmResponse response,
                                                   final Collection<MailPlaceHolder> placeHolders) {
        final Collection<Long> userIds = new HashSet<>();
        final Collection<PlaceHolder> placeHolderList = new HashSet<>();

        if (Objects.nonNull(placeHolders) && !placeHolders.isEmpty()) {

            for (final MailPlaceHolder placeHolder : placeHolders) {

                if (placeHolder.isOwnerField()) {
                    final Long userId = placeHolder.getLong();

                    if (Objects.nonNull(userId)) {
                        userIds.add(userId);
                    }
                }
                placeHolderList.add(placeHolder);
            }
        }

        if (userIds.isEmpty()) {

            try {
                template.setPlaceHolders(placeHolderList);
                formatAndDispatchTemplate(template, response);
            } catch (final Throwable throwable) {
                response.setErrorCode(ErrorCode.get(TemplateErrorCodes.TEMPLATE_DISPATCH_FAILED, "i18n.email.template.dispatch.failed", throwable));
            } finally {
                response.send();
            }
        } else {
            fetchOwnersAndFillPlaceHolders(template, userIds, response, placeHolders);
        }
    }

    /**
     * <p>
     * Fetch owners and fills the appropriate PlaceHolder field with its values
     * </p>
     *
     * @param template     Represents the {@link EntityMailTemplate}
     * @param userIds      Represents the {@link Collection} of user ids
     * @param response     Represents the {@link CrmResponse}
     * @param placeHolders Represents the  {@link Collection} of {@link MailPlaceHolder}
     */
    private void fetchOwnersAndFillPlaceHolders(final EntityMailTemplate template, final Collection<Long> userIds,
                                                final CrmResponse response, final Collection<MailPlaceHolder> placeHolders) {
        response.convertIntoAsync();
        final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Collection<User> users) {
                try {
                    final Map<Long, User> crmUsers = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                    template.setPlaceHolders(placeHolderExtractor.resolvePlaceHoldersWithUser(placeHolders, crmUsers));
                    formatAndDispatchTemplate(template, response);
                } catch (final Throwable throwable) {
                    response.setErrorCode(ErrorCode.get(TemplateErrorCodes.TEMPLATE_DISPATCH_FAILED, "i18n.email.template.dispatch.failed", throwable));
                } finally {
                    response.send();
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                response.setErrorCode(ErrorCode.get(TemplateErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                response.send();
            }
        };
        userService.fetchUsers(organizationService.getIamOrgId(template.orgId()), userIds, usersFetchCallBack);
    }

    /**
     * <p>
     * Formats and dispatches the template attributes with placeholders
     * </p>
     *
     * @param template An entity that holds {@link EntityMailTemplate} attributes
     * @param response Represents the {@link CrmResponse}
     */
    private void formatAndDispatchTemplate(final EntityMailTemplate template, final CrmResponse response) {
        if (template.hasPlaceHolders()) {
            final Collection<PlaceHolder> placeHolders = template.placeHolders();
            final String subject = template.getSubject();
            final String content = template.getContent();

            if (template.isSubjectHasPlaceHolders() && Objects.nonNull(subject)) {
                template.setSubject(placeHolderResolver.resolve(subject,
                        placeHolderExtractor.getSubjectPlaceHolders(placeHolders)));
            }

            if (template.isContentHasPlaceHolders() && Objects.nonNull(content)) {
                template.setContent(placeHolderResolver.resolve(content,
                        placeHolderExtractor.getContentPlaceHolders(placeHolders)));
            }
        }
        response.put(TemplateFields.TEMPLATE.getName(), convert(template));
    }

    /**
     * <p>
     * Fetches the supported sources for template placeHolders
     * </p>
     *
     * @param orgId Represents an organization id
     * @return {@link JsonArray}
     */
    private JsonArray getPlaceHolderSources(final Long orgId) {
        final Collection<SourceSettings> models = sourceSettingsService.getCommonSourceSettings(orgId);
        final JsonArray jsonArray = Json.array();
        final RolePermissions rolePermissions = rolePermissionService.getRolePermissions(orgId, LoggedInUserInfo.getUser().getRoleId());
        final Map<Source, Boolean> rolePermissionsMap = new EnumMap<>(Source.class);
        final Category.Type sourcesCategory = Category.Type.SOURCES;

        rolePermissions.getPermissions().stream().filter(rolePermission -> rolePermission.getCategory().getType().equals(sourcesCategory))
                .filter(rolePermission -> Type.VIEW.equals(rolePermission.getAction().getType()))
                .forEach(rolePermission -> rolePermissionsMap.putIfAbsent(Source.get(rolePermission.getSubCategory().getType().getAliasName()), rolePermission.isGranted()));

        models.forEach(model -> {
            final String sourceName = fieldSourceService.getSource(model.getSourceId()).getName();
            final boolean sourceAccess = rolePermissionsMap.get(Source.get(sourceName));

            if (!Product.ENTITY_TYPE.equals(sourceName) && sourceAccess) {
                final JsonObject jsonObject = Json.object();

                jsonObject.put(SettingsFields.SOURCE_ID.getName(), model.getSourceId());
                jsonObject.put(SettingsFields.SOURCE_NAME.getName(), sourceName);
                jsonArray.put(jsonObject);
            }
        });

        return jsonArray;
    }

    /**
     * <p>
     * Fetches the template statistics
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param request  Represents the crm request
     * @param response Represents the crm response
     * @return {@link JsonArray} of template statistics
     */
    private JsonArray getTemplateStatistics(final Long orgId, final CrmRequest request, final CrmResponse response) {
        final JsonArray jsonArray = Json.array();
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(TemplateFields.DATA.getName());
        final int start = Math.max(data.optInt(TemplateFields.START.getName(), DEFAULT_START), DEFAULT_START);
        final int limit = Math.min(data.optInt(TemplateFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final Long folderId = data.getLong(TemplateFields.FOLDER_ID.getName());
        final Long userId = request.userId();

        if (templateFolderManager.exists(orgId, folderId)) {
            final Long totalRecords = templateManager.fetchAllFolderTemplatesCount(orgId, userId);

            if (totalRecords > 0 && start <= totalRecords) {
                final Collection<EntityMailTemplate> templates = templateManager.getMetrics(folderId, userId, orgId, start, limit);

                if (Objects.nonNull(templates)) {
                    templates.forEach(template -> jsonArray.put(convert(template)));
                }
            }
            response.put(TemplateFields.TOTAL_RECORDS.getName(), totalRecords);
            response.put(TemplateFields.HAS_MORE.getName(), (start + limit) <= totalRecords);
        } else {
            throw ErrorCode.get(TemplateFolderErrorCodes.TEMPLATE_FOLDER_NOT_FOUND, "i18n.email.template.folder.not.found");
        }

        return jsonArray;
    }

    /**
     * <p>
     * Retrieves and displays the placeholder fields
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param request  Represents the crm request
     * @param response Refers to {@link CrmResponse}
     */
    private void fetchAndDispatchPlaceHolderFields(final Long orgId, final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(TemplateFields.DATA.getName());
        final Source source = Source.get(data.getString(TemplateFields.SOURCE.getName()));

        switch (source) {
            case DEAL -> processFieldsIntoJsonArray(getOrgPersistentFieldsBySource(orgId, Deal.ENTITY_TYPE),
                    fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE, TemplateDealFields.getDefaultFields()), false, response);
            case CONTACT -> processFieldsIntoJsonArray(getOrgPersistentFieldsBySource(orgId, Contact.ENTITY_TYPE),
                    fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, TemplateContactFields.getDefaultFields()), true, response);
            case COMPANY -> processFieldsIntoJsonArray(getOrgPersistentFieldsBySource(orgId, Company.ENTITY_TYPE),
                    fieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE, TemplateCompanyFields.getDefaultFields()), false, response);
            default -> throw ErrorCode.get(TemplateErrorCodes.SOURCE_NOT_FOUND, "i18n.email.template.source.not.found");
        }
    }

    /**
     * <p>
     * Fetches the place holder field values for the provided contact data in request.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param request  Represents the {@link CrmRequest}
     * @param response Represents the {@link CrmResponse}
     */
    private void fetchAndDispatchFieldValues(final Long orgId, final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(TemplateFields.DATA.getName());
        final Source source = Source.get(data.getString(TemplateFields.SOURCE.getName()));
        final long contactId = data.getLong(TemplateFields.CONTACT_ID.getName());
        final long startTime = System.currentTimeMillis();
        LOGGER.debug("Email Template: Placeholder resolution started at: {}", startTime);

        switch (source) {
            case DEAL -> {
                final Collection<FieldInfo> persistentFields =
                        getOrgPersistentFieldsBySource(orgId, Deal.ENTITY_TYPE);
                final Map<String, FieldInfo> defaultFields =
                        fieldService.groupDefaultFieldsByAliasName(
                                Deal.ENTITY_TYPE, TemplateDealFields.getDefaultFields());

                resolveFieldsWithEntity(persistentFields, defaultFields,
                        templateManager.getRelatedDeal(orgId, contactId, mergeFields(persistentFields, defaultFields)), response, false);
            }
            case CONTACT -> {
                final Collection<FieldInfo> persistentFields =
                        getOrgPersistentFieldsBySource(orgId, Contact.ENTITY_TYPE);
                final Map<String, FieldInfo> defaultFields =
                        fieldService.groupDefaultFieldsByAliasName(
                                Contact.ENTITY_TYPE, TemplateContactFields.getDefaultFields());

                resolveFieldsWithEntity(persistentFields, defaultFields,
                    templateManager.getContact(orgId, contactId, mergeFields(persistentFields, defaultFields)), response, true);
            }
            case COMPANY -> resolveFieldsWithEntity(getOrgPersistentFieldsBySource(orgId, Company.ENTITY_TYPE),
                    fieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE, TemplateCompanyFields.getDefaultFields()),
                    templateManager.getRelatedCompany(orgId, contactId), response, false);
            default -> throw ErrorCode.get(TemplateErrorCodes.SOURCE_NOT_FOUND, "i18n.email.template.source.not.found");
        }
        LOGGER.debug("Email Template: Placeholder resolution completed in {} ms", (System.currentTimeMillis() - startTime));
    }

    /**
     * <p>
     * Retrieves a collection of source persistent fields with an organization
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param sourceName Represents the source name
     * @return the {@link Collection} of {@link FieldInfo}
     */
    private Collection<FieldInfo> getOrgPersistentFieldsBySource(final Long orgId, final String sourceName) {
        return fieldService.getPersistentFieldsBySource(orgId, sourceName);
    }

    /**
     * <p>
     * Converts the collection of custom fields and source placeholder fields into {@link JsonObject}
     * </p>
     *
     * @param persistentFields  Represents the persistent fields
     * @param placeHolderFields Represents the {@link FieldInfo} with corresponding source in {@link Map}
     * @param isContact         A flag to indicate the type of placeholder field is for contact
     * @param response          Refers to {@link CrmResponse}
     */
    private void processFieldsIntoJsonArray(final Collection<FieldInfo> persistentFields, final Map<String, FieldInfo> placeHolderFields,
                                            final boolean isContact, final CrmResponse response) {
        final JsonArray jsonArray = Json.array();
        final String idKey = TemplateFields.ID.getName();
        final String displayNameKey = TemplateFields.DISPLAY_NAME.getName();

        if (isContact) {
            addFullName(jsonArray, null, response);
        }

        if (!placeHolderFields.isEmpty()) {
            response.put(TemplateFields.SOURCE_ID.getName(), placeHolderFields.entrySet().stream().findFirst().get().getValue().sourceId());
        }

        for (final Map.Entry<String, FieldInfo> entry : placeHolderFields.entrySet()) {
            final JsonObject jsonObject = Json.object();
            final FieldInfo fieldInfo = entry.getValue();

            jsonObject.put(idKey, fieldInfo.id());
            jsonObject.put(displayNameKey, response.resolveI18nPlaceholder(fieldInfo.i18nName()));
            jsonArray.put(jsonObject);
        }

        if (Objects.nonNull(persistentFields) && !persistentFields.isEmpty()) {

            for (final FieldInfo fieldInfo : persistentFields) {
                final JsonObject jsonObject = Json.object();

                jsonObject.put(idKey, fieldInfo.id());
                jsonObject.put(displayNameKey, response.resolveI18nPlaceholder(fieldInfo.i18nName()));

                jsonArray.put(jsonObject);
            }
        }
        response.put(TemplateFields.FIELDS.getName(), jsonArray);
        response.send();
    }

    /**
     * <p>
     * Resolves the collection of placeholder fields along with its corresponding value from the entity
     * </p>
     *
     * @param persistentFields  Represents the persistent fields
     * @param placeHolderFields Represents the {@link FieldInfo} with corresponding source in {@link Map}
     * @param entity            Represents the {@link Entity}
     * @param response          Represents the {@link CrmResponse}
     * @param isContact         A flag to indicate the type of placeholder field is for contact
     */
    private void resolveFieldsWithEntity(final Collection<FieldInfo> persistentFields, final Map<String, FieldInfo> placeHolderFields,
                                         final Entity entity, final CrmResponse response, final boolean isContact) {
        try {
            if (Objects.isNull(entity)) {
                throw ErrorCode.get(EntityErrorCodes.ENTITY_NOT_FOUND, response.resolveI18nString("i18n.entity.not.found"));
            }
            final JsonArray jsonArray = Json.array();
            final Set<Long> userIds = new HashSet<>();

            if (isContact) {
                addFullName(jsonArray, (Contact) entity, response);
            }
            final CrmUser user = LoggedInUserInfo.getUser();
            final UserRegionalPreference regionalPreference = user.getPreferenceHolder().get(UserRegionalPreference.TYPE);
            final TimeZone timeZone = regionalPreference.getTimezoneInfo().getTimeZone();
            final long startTime = System.currentTimeMillis();
            LOGGER.debug("Email Template: PlaceHolder value resolution started at: {}", startTime);

            placeHolderFields.values().forEach(fieldInfo -> handleFieldValue(response, fieldInfo, jsonArray, entity, userIds, timeZone));
            persistentFields.forEach(fieldInfo -> handleFieldValue(response, fieldInfo, jsonArray, entity, userIds, timeZone));
            LOGGER.debug("Email Template: Placeholder value resolution completed in {} ms", (System.currentTimeMillis() - startTime));

            if (userIds.isEmpty()) {
                response.put(TemplateFields.FIELDS.getName(), jsonArray);
                response.send();
            } else {
                fetchOwnerFields(userIds, entity.orgId(), jsonArray, response);
            }
        } catch (final Throwable throwable) {
            response.send();
        }
    }

    /**
     * <p>
     * Combines persistent fields with default fields into a single collection.
     * </p>
     *
     * @param persistentFields  Represents the organisation persistent fields.
     * @param defaultFields     Represents the default fields map
     * @return Merged {@link FieldInfo} collection of all fields
     */
    private Collection<FieldInfo> mergeFields(final Collection<FieldInfo> persistentFields, final Map<String, FieldInfo> defaultFields) {
        final Collection<FieldInfo> mergedFields = new ArrayList<>();

        mergedFields.addAll(persistentFields);
        mergedFields.addAll(defaultFields.values());
        return mergedFields;
    }

    /**
     * <p>
     * Handles the field value to be placed in placeholder for the field and entity provided
     * </p>
     *
     * @param fieldInfo     Refers to the {@link FieldInfo}
     * @param jsonArray     Refers to the {@link JsonArray}
     * @param entity        Represents the entity from which values are processed
     * @param userIds       Represents the {@link Collection} of user ids
     * @param timeZone      Represents the {@link TimeZone} of logged-in user
     */
    private void handleFieldValue(final CrmResponse response, final FieldInfo fieldInfo, final JsonArray jsonArray, Entity entity,
                                  final Set<Long> userIds, final TimeZone timeZone) {
        final JsonObject jsonObject = Json.object();
        final MailPlaceHolder placeHolder = new MailPlaceHolder();

        placeHolderValueInterpreter.resolve(placeHolder, fieldInfo, entity, timeZone);
        placeHolder.setOwnerField(fieldInfo.storeType().isOwnerType());
        final Object value = placeHolder.getValue();

        jsonObject.put(TemplateFields.ID.getName(), fieldInfo.id());
        jsonObject.put(TemplateFields.DISPLAY_NAME.getName(), response.resolveI18nPlaceholder(fieldInfo.i18nName()));
        jsonObject.put(TemplateFields.VALUE.getName(), value);

        if (Objects.nonNull(value) && placeHolder.isOwnerField()) {
            final Long userId = placeHolder.getLong();

            if (Objects.nonNull(userId)) {
                userIds.add(userId);
            }
            jsonObject.put(TemplateFields.OWNER.getName(), value);
        }
        jsonArray.put(jsonObject);
    }

    /**
     * <p>
     * Adds the full name field into {@link JsonArray} to display in placeholder list
     * </p>
     *
     * @param jsonArray Refers to {@link JsonArray}
     * @param entity    An entity that holds contact attributes
     * @param response  Refers to {@link CrmResponse}
     */
    private void addFullName(final JsonArray jsonArray, final Contact entity, final CrmResponse response) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(TemplateFields.DISPLAY_NAME.getName(), response.resolveI18nString("i18n.contact.field.fullName"));
        jsonObject.put(TemplateFields.ID.getName(), CONTACT_FULL_NAME_FIELD_ID);

        if (Objects.nonNull(entity)) {
            jsonObject.put(TemplateFields.VALUE.getName(), entity.getFullName());
        }
        jsonArray.put(jsonObject);
    }

    /**
     * <p>
     * Deletes the templates
     * </p>
     *
     * @param crmResponse Represents the crm response
     * @param orgId       Represents an organization id
     * @param templateIds Represents the {@link Collection} of {@link EntityMailTemplate}
     */
    public void delete(final CrmResponse crmResponse, final Long orgId, final Collection<Long> templateIds) {
        if (templateManager.exists(orgId, templateIds)) {
            final Collection<EntityMailTemplate> templates = templateManager.get(orgId, templateIds);

            templateManager.delete(orgId, templates);
            crmResponse.put(CoreEmailFields.ERRORS.getName(), Collections.emptyList());
        } else {
            throw ErrorCode.get(TemplateErrorCodes.TEMPLATE_NOT_FOUND, "i18n.email.template.not.found");
        }
    }

    /**
     * <p>
     * Fetches the owner field values for the provided user ids.
     * </p>
     *
     * @param userIds   Represents the {@link Collection} of user ids
     * @param orgId     Represents an organization id
     * @param jsonArray Represents the {@link JsonArray}
     * @param response  Represents the {@link CrmResponse}
     */
    private void fetchOwnerFields(final Collection<Long> userIds, final Long orgId, final JsonArray jsonArray,
                                  final CrmResponse response) {
        final String ownerKey = TemplateFields.OWNER.getName();
        final String valueKey = TemplateFields.VALUE.getName();

        response.convertIntoAsync();
        final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final Collection<User> users) {
                try {
                    final Map<Long, User> crmUsers = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                    for (int index = 0; index < jsonArray.size(); index++) {
                        final JsonObject jsonObject = jsonArray.getJsonObject(index);

                        if (jsonObject.containsKey(ownerKey)) {
                            jsonObject.put(valueKey, crmUsers.get(jsonObject.getLong(ownerKey)).getFullName());
                            jsonObject.remove(ownerKey);
                        }
                    }
                    response.put(TemplateFields.FIELDS.getName(), jsonArray);
                } catch (final Throwable throwable) {
                    response.setErrorCode(ErrorCode.get(TemplateErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                } finally {
                    response.send();
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                response.setErrorCode(ErrorCode.get(TemplateErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", throwable));
                response.send();
            }
        };
        userService.fetchUsers(organizationService.getIamOrgId(orgId), userIds, usersFetchCallBack);
    }
}
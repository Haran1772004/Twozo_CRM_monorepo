package com.tlc.crm.core.action.contact;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmEventRequest;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.ContactDealInfo;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.ParticipantManager;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.core.field.marketinglist.MarketingListFields;
import com.tlc.crm.core.field.product.ProductFields;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.core.internal.contact.sync.ContactSyncService;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;
import com.tlc.crm.core.internal.field.BulkActionFields;
import com.tlc.crm.core.page.ContactDealPageHandler;
import com.tlc.crm.core.validator.internal.ContactDataValidator;
import com.tlc.crm.entity.action.EntityConfigAction;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.SummaryOverview;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.models.type.converter.EmailConverter;
import com.tlc.crm.entity.models.type.converter.PhoneConverter;
import com.tlc.crm.security.permission.annotation.authorizer.FollowerAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.iam.common.models.User;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.web.WebAction;
import com.tlc.crm.core.workflow.handler.TriggerModelHandler;
import com.tlc.crm.core.workflow.model.ModelSource;
import com.tlc.crm.core.workflow.model.TriggerInfo;
import com.tlc.crm.core.workflow.publish.TriggerDataPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Abishek
 * @version 1.0
 */
@FollowerAuthorizer(subCategory = SubCategory.Type.CONTACT_FOLLOWERS_RECORDS)
@SourceAuthorizer(subCategory = SubCategory.Type.CONTACTS, validateFieldPermission = true)
@WebAction(path = "/contact/mgmt")
public class ContactManagement extends EntityConfigAction<Contact> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactManagement.class);
    public static final String CONFIG_ACTION_NAME = "contact";
    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 50;
    private static final int LIFECYCLE_STAGE_DEFAULT_LIMIT = 100;

    protected final ContactConverter contactConverter;

    private final ConverterConfigBuilder converterConfigBuilder;
    private final ContactManager contactManager;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final UserService userService;
    private final ContactSyncService contactSyncService;
    private final EmailConverter emailConverter;
    private final PhoneConverter phoneConverter;
    private final DealManager dealManager;
    private final ParticipantManager participantManager;
    private final ContactDataValidator contactDataValidator;
    private final TriggerDataPublisher triggerDataPublisher;
    private final TriggerModelHandler triggerModelHandler;

    public ContactManagement() {
        contactManager = ContactManager.getInstance();
        contactConverter = ContactConverter.getInstance();
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
        lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        userService = UserService.getInstance();
        contactSyncService = ContactSyncService.getInstance();
        emailConverter = EmailConverter.getInstance();
        phoneConverter = PhoneConverter.getInstance();
        dealManager = DealManager.getInstance();
        participantManager = ParticipantManager.getInstance();
        contactDataValidator = ContactDataValidator.getInstance();
        triggerDataPublisher = TriggerDataPublisher.getInstance();
        triggerModelHandler = TriggerModelHandler.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRequest(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(ContactFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(ContactFields.DATA);
        final Long orgId = request.orgId();
        final CrmEventRequest crmEventRequest = (CrmEventRequest) request;
        final EventData eventData = crmEventRequest.getEventData();

        switch (type) {
            case "summary" -> summary(orgId, data, request, response);
            case "summaryV2" -> summaryV2(orgId, data, request, response);
            case "relatedCompany" -> response.put(ContactFields.COMPANY, getRelatedCompany(orgId, data));
            case "getPrimaryEmail" ->
                    response.put(ContactFields.EMAIL, getPrimaryEmail(orgId, data.getLong(ContactFields.ID)));
            case "getPrimaryPhone" ->
                    response.put(ContactFields.PHONE, getPrimaryPhone(orgId, data.getLong(ContactFields.ID)));
            case "updateLifecycleStage" -> {
                updateLifeCycleStage(orgId, request.userId(), data, eventData);
                response.put(FieldKeys.MESSAGE.getName(), response.resolveI18nString("i18n.contact.stage.update.success"));
            }
            case "updateLifecycleStatus" -> {
                updateLifeCycleStatus(orgId, request.userId(), data, eventData);
                response.put(FieldKeys.MESSAGE.getName(), response.resolveI18nString("i18n.contact.status.update.success"));
            }
            case "getLifeCycleStageProgress" -> lifeCycleStageProgress(orgId, data, response);
            case "profile" -> profile(orgId, data, request, response);
            case "bCreateActivity" -> bUpdate(request, response, eventData, false);
            case "bAddToMarketingList" -> bAddContactToMarketingList(request, response);
        }
    }

    /**
     * <p>
     * Handles the progress of a contact lifecycle stage within an org id
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param data     Represent the {@link JsonObject}
     * @param response Represents the {@link CrmResponse}
     */
    private void lifeCycleStageProgress(final long orgId, final JsonObject data, final CrmResponse response) {
        final long contactId = data.getLong(ContactFields.CONTACT_ID);
        final int start = Math.max(data.optInt(ContactFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(ContactFields.LIMIT, LIFECYCLE_STAGE_DEFAULT_LIMIT), LIFECYCLE_STAGE_DEFAULT_LIMIT);

        if (!contactManager.exists(orgId, contactId)) {
            throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        }
        final Collection<LifeCycleStage> lifeCycleStages = lifeCycleStageManager.getLifeCycleStages(orgId, start, limit);
        final Optional<FieldInfo> optionalLifeCycleFieldInfo = fieldService.getDefaultFieldByAliasName(Source.CONTACT, ContactFields.LIFECYCLE_STAGE);
        final List<LifeCycleStatus> lifeCycleStatuses = lifeCycleStages.stream().map(LifeCycleStage::getStatuses).flatMap(Collection::stream).toList();
        final int totalRecords = lifeCycleStatuses.size();

        response.put(ContactFields.TOTAL_RECORDS, totalRecords);
        response.put(ContactFields.HAS_MORE, (start + limit) <= totalRecords);
        final Optional<LifeCycleStatus> contactLifeCycleStatus = contactManager.getContactLifeCycleStatus(orgId, contactId);

        optionalLifeCycleFieldInfo.ifPresent(fieldInfo -> response.put(FieldKeys.FIELD_ID.getName(), fieldInfo.id()));
        response.put(ContactFields.STATUS, contactLifeCycleStatus.isPresent()
                ? buildLifeCycleStageProgress(contactLifeCycleStatus.get(), lifeCycleStatuses, lifeCycleStages) : Collections.emptyList());
    }

    /**
     * <p>
     * Builds the lifecycle stage progress array for a given  lifecycle statuses.
     * </p>
     *
     * @param contactLifeCycleStatus The lifecycle status associated with the contact
     * @param lifeCycleStatuses      Which contains the collection of {@link LifeCycleStatus}
     * @param lifeCycleStages        Which contains the collection of {@link LifeCycleStage}
     * @return A {@link JsonArray} representing the lifecycle stage progress,
     */
    private JsonArray buildLifeCycleStageProgress(final LifeCycleStatus contactLifeCycleStatus, final List<LifeCycleStatus> lifeCycleStatuses,
                                                  final Collection<LifeCycleStage> lifeCycleStages) {
        final JsonArray lifeCycleStatusArray = Json.array();
        final String lifeCycleStageIdKey = ContactFields.LIFECYCLE_STAGE_ID;
        final String lifeCycleStageNameKey = ContactFields.LIFE_CYCLE_STAGE_NAME;
        final String nameKey = ContactFields.NAME;
        final String idKey = ContactFields.ID;
        final String statusKey = ContactFields.STATUS;
        final String labelKey = ContactFields.LABEL;
        final String lost = ContactFields.LOST;
        final String completed = ContactFields.COMPLETED;
        final String dropdownKey = ContactFields.DROPDOWN;
        final String pending = ContactFields.PENDING;
        final Long currentLifeCycleStatusId = contactLifeCycleStatus.id();
        final LifeCycleStatus.Label label = contactLifeCycleStatus.getLabel();
        final short closedLostId = LifeCycleStatus.Label.CLOSED_LOST.getId();
        final String status = Objects.nonNull(label) && label.getId() == closedLostId ? lost : completed;
        final Map<Long, List<LifeCycleStatus>> recentLifeCycleStatusesByStage = lifeCycleStatuses.stream()
                .collect(Collectors.groupingBy(LifeCycleStatus::getStageId, Collectors.collectingAndThen(Collectors.toList(),
                        list -> {
                            if (list.size() <= 1) {
                                return Collections.emptyList();
                            }
                            final boolean containsClosedLost = list.stream().anyMatch(lifeCycleStatus -> {
                                final LifeCycleStatus.Label lifeCycleStatusLabel = lifeCycleStatus.getLabel();

                                return Objects.nonNull(lifeCycleStatusLabel) && Objects.equals(closedLostId, lifeCycleStatusLabel.getId());
                            });
                            return containsClosedLost ? list.subList(Math.max(0, list.size() - 2), list.size())
                                    : Collections.emptyList();
                        })));

        final Long previousStatusId = (Objects.nonNull(label) && Objects.equals(label.getId(), closedLostId)) ?
                IntStream.range(1, lifeCycleStatuses.size()).filter(index -> Objects.equals(lifeCycleStatuses.get(index).id(),
                        currentLifeCycleStatusId)).mapToObj(index -> lifeCycleStatuses.get(index - 1).id()).findFirst().orElse(null) : null;

        boolean isNewStatusRequired = true;
        boolean isClosedLostStatusRequired = false;
        boolean isCurrentContactLifeCycleStatus = true;
        boolean isCurrentStatusClosedLost = true;

        for (final LifeCycleStage lifeCycleStage : lifeCycleStages) {
            final String lifeCycleStageName = lifeCycleStage.getName();
            final Collection<LifeCycleStatus> lifeCycleStatusList = lifeCycleStage.getStatuses();
            final boolean hasMultipleStatus = lifeCycleStatusList.size() > 1;

            for (final LifeCycleStatus lifeCycleStatus : lifeCycleStatusList) {
                final long lifeCycleStatusId = lifeCycleStatus.id();

                if (hasMultipleStatus && Objects.equals(previousStatusId, lifeCycleStatusId)) {
                    isCurrentStatusClosedLost = false;
                    continue;
                }

                if (hasMultipleStatus && isClosedLostStatusRequired) {
                    isClosedLostStatusRequired = false;
                    continue;
                }
                final String statusValue = isNewStatusRequired ? status : pending;

                if (isCurrentContactLifeCycleStatus && Objects.equals(lifeCycleStatusId, currentLifeCycleStatusId)) {
                    isNewStatusRequired = false;
                    isCurrentContactLifeCycleStatus = false;
                }
                final JsonObject lifeCycleStatusJson = buildLifeCycleStatusJson(lifeCycleStatus, lifeCycleStageName,
                        lifeCycleStageNameKey, lifeCycleStageIdKey, nameKey, idKey, labelKey);
                final Collection<LifeCycleStatus> recentLifeCycleStatuses = recentLifeCycleStatusesByStage.get(lifeCycleStatus.getStageId());

                if (Objects.nonNull(recentLifeCycleStatuses) && recentLifeCycleStatuses.contains(lifeCycleStatus)) {
                    final JsonArray dropdownStatusArray = Json.array();

                    for (final LifeCycleStatus cycleStatus : recentLifeCycleStatuses) {
                        final LifeCycleStatus.Label cycleStatusLabel = cycleStatus.getLabel();

                        dropdownStatusArray.put(buildLifeCycleStatusJson(cycleStatus, lifeCycleStageName, lifeCycleStageNameKey,
                                lifeCycleStageIdKey, nameKey, idKey, labelKey));

                        if (Objects.nonNull(cycleStatusLabel) && closedLostId == cycleStatusLabel.getId()) {

                            if (isCurrentStatusClosedLost) {
                                isClosedLostStatusRequired = true;
                            } else {
                                isCurrentStatusClosedLost = true;
                            }
                        }
                    }
                    lifeCycleStatusJson.put(dropdownKey, dropdownStatusArray);
                }
                lifeCycleStatusJson.put(statusKey, statusValue);
                lifeCycleStatusArray.put(lifeCycleStatusJson);
            }
        }

        return lifeCycleStatusArray;
    }

    /**
     * <p>
     * Builds a {@link JsonObject} representation of a {@link LifeCycleStatus}
     * </p>
     *
     * @param lifeCycleStatus       Represents the {@link LifeCycleStatus}
     * @param stageName             Represents the stage name
     * @param lifecycleStageNameKey Represents the lifecycle stage name
     * @param lifecycleStageIdKey   The key for the lifecycle stage ID
     * @param nameKey               The key for the name field
     * @param idKey                 The key for the ID field
     * @param labelKey              The key for the label field
     * @return A {@link JsonObject} representing the {@link LifeCycleStatus}
     */
    private JsonObject buildLifeCycleStatusJson(final LifeCycleStatus lifeCycleStatus, final String stageName,
                                                final String lifecycleStageNameKey, final String lifecycleStageIdKey,
                                                final String nameKey, final String idKey, final String labelKey) {
        final JsonObject lifeCycleStatusJson = Json.object();
        final LifeCycleStatus.Label label = lifeCycleStatus.getLabel();

        lifeCycleStatusJson.put(lifecycleStageNameKey, stageName);
        lifeCycleStatusJson.put(lifecycleStageIdKey, lifeCycleStatus.getStageId());
        lifeCycleStatusJson.put(nameKey, lifeCycleStatus.getName());
        lifeCycleStatusJson.put(idKey, lifeCycleStatus.id());

        if (Objects.nonNull(label)) {
            lifeCycleStatusJson.put(labelKey, label.getId());
        }

        return lifeCycleStatusJson;
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
     * {@inheritDoc}
     *
     * @param crmRequest  Handles the received request
     * @param crmResponse Generates the CRM response
     * @throws Exception if an error occurs during the request processing
     */
    @Override
    public void handleMobileRequest(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString("type");
        final JsonObject data = requestJson.getJsonObject("data");
        final long orgId = crmRequest.orgId();

        switch (type) {
            case "getContactList" ->
                    crmResponse.put(ContactFields.CONTACTS, getContacts(orgId, data, crmResponse));
            case "summary" -> summary(data, crmRequest, crmResponse);
            case "summaryV2" -> summaryV2(orgId, data, crmRequest, crmResponse);
            case "socialProfile" -> socialProfile(orgId, data, crmRequest, crmResponse);
            default -> authorizedProcess(crmRequest, crmResponse);
        }
    }

    /**
     * <p>
     * Gets the contacts mapped for the given organization and deal
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param jsonObject  Represents the {@link JsonObject}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private JsonArray getContacts(final long orgId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final Long dealId = jsonObject.getLong(DealFields.DEAL_ID);

        if (dealManager.exists(orgId, dealId)) {
            final String criteriaKey = DealFields.CRITERIA;
            final DropdownCriteria dropdownCriteria = jsonObject.containsKey(criteriaKey) ? new DropdownCriteria(jsonObject.getJsonObject(criteriaKey)) : new DropdownCriteria();
            final ContactDealPageHandler contactDealPageHandler = new ContactDealPageHandler(jsonObject.optInt(ActivityFields.START.getName(), DEFAULT_START_VALUE),
                    jsonObject.optInt(ActivityFields.LIMIT.getName(), DEFAULT_LIMIT), dropdownCriteria);
            final JsonArray contacts = Json.array();

            contactDealPageHandler.setOrgId(orgId);
            contactDealPageHandler.setEntityId(dealId);

            final long totalRecords = contactDealPageHandler.totalRecordsCount();

            if (totalRecords > 0) {
                final Collection<ContactDealInfo> contactDealInfoList = contactDealPageHandler.fetchPageData().getData();

                for (final ContactDealInfo contactDealInfo : contactDealInfoList) {
                    final JsonObject contactJson = contactConverter.convertContactWithThumbnail(contactDealInfo.getContact());
                    final Collection<Long> dealIds = contactDealInfo.getDealIds();

                    if (Objects.nonNull(dealIds) && dealIds.contains(dealId)) {
                        contactJson.put(ProductFields.IS_ADDED_WITH_DEAL, true);
                    }
                    contacts.put(contactJson);
                }

                crmResponse.put(ContactFields.RELATED_CONTACTS_COUNT, participantManager.getDealParticipantCount(orgId, dealId));
            }

            crmResponse.put(ContactFields.TOTAL_RECORDS, totalRecords);
            crmResponse.put(ContactFields.HAS_MORE, contactDealPageHandler.hasMore());

            return contacts;
        } else {
            throw ErrorCode.get(DealErrorCodes.DEAL_NOT_FOUND, "i18n.deal.not.found");
        }
    }

    /**
     * <p>
     * Gets the primary phone for the given contact
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param contactId Represents the contact id
     * @return {@link JsonObject}
     */
    private JsonObject getPrimaryPhone(final long orgId, final Long contactId) {
        if (contactManager.exists(orgId, contactId)) {
            final Optional<Phone> primaryPhone = contactManager.getPrimaryPhone(orgId, contactId);

            return primaryPhone.isPresent() ? phoneConverter.convert(new ConverterConfig(false, false), primaryPhone.get()) : Json.object();
        } else {
            throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        }
    }

    /**
     * <p>
     * Gets the primary email for the given contact
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param contactId Represents the contact id
     * @return {@link JsonObject}
     */
    private JsonObject getPrimaryEmail(final long orgId, final Long contactId) {
        if (contactManager.exists(orgId, contactId)) {
            final Optional<Email> primaryEmail = contactManager.getPrimaryEmail(orgId, contactId);

            return primaryEmail.isPresent() ? emailConverter.convert(new ConverterConfig(false, false), primaryEmail.get()) : Json.object();
        } else {
            throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        }
    }

    /**
     * <p>
     * Updates the {@link LifeCycleStage} of the given contact.
     * </p>
     *
     * @param orgId  Represents the organisation id.
     * @param userId Represents the user id.
     * @param data   Holds the details of the requested data.
     */
    private void updateLifeCycleStage(final Long orgId, final Long userId, final JsonObject data, final EventData eventData) {
        final long contactId = data.getLong(ContactFields.ID);

        if (contactManager.exists(orgId, contactId)) {
            final long lifeCycleStageId = data.getLong(ContactFields.LIFECYCLE_STAGE_ID);

            if (lifeCycleStageManager.exists(orgId, lifeCycleStageId)) {
                final LifeCycleStatus defaultStatus = lifeCycleStatusManager.getDefaultStatus(orgId, lifeCycleStageId);
                final String lostReasonKey = ContactFields.CONTACT_LOST_REASON;
                final Optional<FieldInfo> optionalLostReasonField = fieldService.getDefaultFieldByAliasName(Source.CONTACT, lostReasonKey);
                final Contact contact = new Contact();
                final LifeCycleStage lifeCycleStage = new LifeCycleStage();

                lifeCycleStage.setId(lifeCycleStageId);
                contact.setId(contactId);
                contact.setOrgId(orgId);
                contact.setUpdatedBy(userService.getUser(orgId, userId));
                contact.setLifeCycleStage(lifeCycleStage);
                contact.setLifeCycleStatus(defaultStatus);
                optionalLostReasonField.ifPresent(lostReasonField -> contactConverter.loadLostReasonIntoModel(orgId,
                        true, lostReasonField, lostReasonKey, defaultStatus.getLabel(), contact, data));

                contactManager.updateLifeCycle(contact);
                eventData.setValues(List.of(contact));
                eventData.setAction(Action.PARTIAL_UPDATE);

                triggerDataPublisher.publish(orgId, userId, List.of(contact), new TriggerInfo(Source.CONTACT, Action.UPDATE, ModelSource.WEB_REQUEST,
                            List.of(triggerModelHandler.create(orgId, Source.CONTACT, eventData.getPreviousValues().get(contactId))), false));
            } else {
                throw ErrorCode.get(ContactErrorCodes.LIFE_CYCLE_STAGE_NOT_FOUND, "i18n.lifecycle.stage.not.found");
            }
        } else {
            throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        }
    }

    /**
     * <p>
     * Updates the {@link LifeCycleStatus} of the given contact.
     * </p>
     *
     * @param orgId  Represents the organisation id.
     * @param userId Represents the user id.
     * @param data   Holds the details of the requested data.
     */
    private void updateLifeCycleStatus(final Long orgId, final Long userId, final JsonObject data , final EventData eventData) {
        final long contactId = data.getLong(ContactFields.ID);

        if (contactManager.exists(orgId, contactId)) {
            final long lifeCycleStageId = data.getLong(ContactFields.LIFECYCLE_STAGE_ID);
            final long lifeCycleStatusId = data.getLong(ContactFields.LIFECYCLE_STATUS_ID);

            if (lifeCycleStatusManager.exists(orgId, lifeCycleStageId, lifeCycleStatusId)) {
                final Contact contact = new Contact();
                final LifeCycleStatus lifeCycleStatus = lifeCycleStatusManager.get(orgId, lifeCycleStatusId);
                final LifeCycleStage lifeCycleStage = new LifeCycleStage();
                final String lostReasonKey = ContactFields.CONTACT_LOST_REASON;
                final FieldInfo lostReasonField = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, lostReasonKey).get(lostReasonKey);

                contactConverter.loadLostReasonIntoModel(orgId, false, lostReasonField, lostReasonKey, lifeCycleStatus.getLabel(), contact, data);
                lifeCycleStage.setId(lifeCycleStageId);
                contact.setId(contactId);
                contact.setOrgId(orgId);
                contact.setUpdatedBy(userService.getUser(orgId, userId));
                contact.setLifeCycleStage(lifeCycleStage);
                contact.setLifeCycleStatus(lifeCycleStatus);
                contactManager.updateLifeCycle(contact);

                eventData.setValues(List.of(contact));
                eventData.setAction(Action.PARTIAL_UPDATE);

                triggerDataPublisher.publish(orgId, userId, List.of(contact), new TriggerInfo(Source.CONTACT, Action.UPDATE, ModelSource.WEB_REQUEST,
                        List.of(triggerModelHandler.create(orgId, Source.CONTACT, eventData.getPreviousValues().get(contactId))), false));
            } else {
                throw ErrorCode.get(ContactErrorCodes.LIFE_CYCLE_STATUS_NOT_FOUND, "i18n.lifecycle.status.not.found");
            }
        } else {
            throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        }
    }

    /**
     * Fetches the summary details of a contact.
     *
     * @param orgId    Refers the organization id.
     * @param data     Refers the request json data.
     * @param request  Refers the crm request object.
     * @param response Refers the response to be sent.
     */
    private void summary(final Long orgId, final JsonObject data, final CrmRequest request, final CrmResponse response) {
        final Contact contact = get(orgId, data.getLong(ContactFields.ID));

        contactManager.loadSummaryDetails(orgId, contact);
        fetchAndDispatchSummary(request, contact, data, response);
    }

    /**
     * Fetches the summary details of a contact.
     *
     * @param orgId    Refers the organization id.
     * @param data     Refers the request json data.
     * @param request  Refers the crm request object.
     * @param response Refers the response to be sent.
     */
    private void summaryV2(final Long orgId, final JsonObject data, final CrmRequest request, final CrmResponse response) {
        final Contact contact = get(orgId, data.getLong(ContactFields.ID));

        contactManager.loadSummaryInfo(orgId, contact);
        fetchAndDispatchSummaryV2(request, contact, data, response);
    }

    /**
     * <p>
     * Fetches the contact profile details
     * </p>
     *
     * @param orgId    Refers the organization id.
     * @param data     Refers the request json data.
     * @param request  Refers the crm request object.
     * @param response Refers the response to be sent.
     */
    private void profile(final Long orgId, final JsonObject data, final CrmRequest request, final CrmResponse response) {
        final long contactId = data.getLong(ContactFields.ID);

        if (contactManager.exists(orgId, contactId)) {
            final Contact contact = contactManager.getContactProfile(orgId, contactId);

            fetchAndDispatchProfile(request, contact, data, response);
        } else {
            throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigManager<Contact> getConfigManager() {
        return contactManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Contact convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return contactConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param crmResponse Represents the crm response
     * @param orgId       Represents an organization id
     * @param contacts    Represents the {@link Contact}
     * @param eventData   Represents the {@link EventData}
     */
    @Override
    public void create(final CrmResponse crmResponse, final Long orgId, final Long userId, final Collection<Contact> contacts, final EventData eventData) {
        LOGGER.info("CONTACT: Create OrgId: {}, UserId: {}", orgId, userId);
        final Collection<ErrorDescriptor> errors = validate(contacts, Group.Create.class);
        final Action action = Action.CREATE;

        if (errors.isEmpty()) {

            if (isValidDetails(orgId, userId, action, contacts)) {
                final ProcessOutcome processOutcome = getConfigManager().create(orgId, contacts);

                loadResponseAndInvokePostAction(action, crmResponse, processOutcome, contacts, eventData);
            }
        } else {
            loadValidationErrors(action, crmResponse, errors);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param crmResponse Represents the crm response
     * @param orgId       Represents an organization id
     * @param contacts    Represents the {@link Contact}
     * @param eventData   Represents the {@link EventData}
     */
    @Override
    public void update(final CrmResponse crmResponse, final Long orgId, final Long userId, final Collection<Contact> contacts, final EventData eventData) {
        final Collection<ErrorDescriptor> errors = validate(contacts, Group.Update.class);
        final Action action = Action.UPDATE;

        if (errors.isEmpty() && isValidDetails(orgId, userId, action, contacts)) {
            preAction(action, contacts, eventData);
            final ProcessOutcome processOutcome = contactManager.bulkUpdate(orgId, contacts);

            loadResponseAndInvokePostAction(action, crmResponse, processOutcome, contacts, eventData);
        } else {
            loadValidationErrors(action, crmResponse, errors);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param crmResponse Represents the crm response
     * @param orgId       Represents an organization id
     * @param contacts    Represents the {@link Contact}
     * @param eventData   Represents the {@link EventData}
     */
    @Override
    public void delete(final CrmResponse crmResponse, final Long orgId, final Long userId, final Collection<Contact> contacts, final EventData eventData) {
        final Collection<ErrorDescriptor> errors = validate(contacts, Group.Delete.class);
        final Action action = Action.DELETE;

        if (errors.isEmpty()) {

            if (isValidDetails(orgId, userId, action, contacts)) {
                final Map<Long, String> personIds = contactSyncService.fetchExternalPersonIds(orgId, userId,
                        contacts.stream().map(Contact::id).collect(Collectors.toUnmodifiableSet()));

                for (final Contact contact : contacts) {
                    contact.setExternalPersonId(personIds.get(contact.id()));
                    final long linkedDealCount = dealManager.getDealCountByContact(orgId, contact.id());

                    if (NumericalConstant.ZERO < linkedDealCount) {
                        final ErrorCode errorCode = ErrorCode.get(ContactErrorCodes.DELETE_ACTION_RESTRICTED, "i18n.contact.action.delete.restricted");

                        crmResponse.setErrorCode(errorCode);
                        crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode)));
                        return;
                    }
                }

                try {
                    contactManager.bulkDelete(orgId, contacts);
                } catch (ErrorCode errorCode) {
                    crmResponse.setErrorCode(errorCode);
                    crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode)));
                }
            }
        } else {
            final ErrorCode errorCode = ErrorCode.get(ContactErrorCodes.DELETE_ACTION_FAILED, "i18n.contact.action.delete.failed");

            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode,
                    ErrorKeys.DELETE_ACTION_DATA_VALIDATION_FAILED), errors));
        }
        postAction(action, contacts, eventData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject convert(final CrmRequest crmRequest, final Contact contact) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(crmRequest, false, null);

        return contactConverter.convertModelToJson(contact, converterConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Contact convertPartialData(CrmRequest request, JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return contactConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization id
     * @param userId
     * @param action   Holds the action values
     * @param contacts Collection of {@link Contact}
     */
    @Override
    protected final boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<Contact> contacts) {

        if (Objects.nonNull(contacts) && !contacts.isEmpty()) {

            switch (action) {
                case CREATE -> contactDataValidator.validate(orgId, contacts);
                case UPDATE -> {

                    if (!contactManager.exists(orgId, contacts.stream().map(Contact::id).collect(Collectors.toUnmodifiableSet()))) {
                        throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
                    }
                    final String emailsKey = FieldKeys.EMAILS.getName();
                    final String phonesKey = FieldKeys.PHONES.getName();
                    final Map<String, FieldInfo> contactFields = fieldService.groupDefaultFieldsByAliasName(orgId, Source.CONTACT.getName(), List.of(emailsKey, phonesKey));
                    final PresetConfig emailPresetConfig = contactFields.get(emailsKey).presetConfig();
                    final PresetConfig phonePresetConfig = contactFields.get(phonesKey).presetConfig();

                    for (final Contact contact : contacts) {
                        final Collection<Email> emails = contact.getEmails();
                        final Collection<Phone> phones = contact.getPhones();

                        if ((Objects.isNull(emails) || emails.isEmpty()) && emailPresetConfig.required()) {
                            throw ErrorCode.get(ContactErrorCodes.PRIMARY_EMAIL_REQUIRED, "i18n.contact.primary.email.required");
                        }

                        if ((Objects.isNull(phones) || phones.isEmpty()) && phonePresetConfig.required()) {
                            throw ErrorCode.get(ContactErrorCodes.PRIMARY_PHONE_REQUIRED, "i18n.contact.primary.phone.required");
                        }
                    }
                    contactDataValidator.validate(orgId, contacts);
                }
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param contact    Represents the entity model
     * @param data       Holds the entity details
     * @param users      Contains the collection of users
     * @return The {@link JsonObject} details
     */
    @Override
    protected JsonObject convertSummary(final CrmRequest crmRequest, final Contact contact, final JsonObject data, final Collection<User> users) {
        return contactConverter.summary(contact, converterConfigBuilder.build(crmRequest, true, data, true));
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param contact    Represents the {@link Contact}
     * @param data       Holds the entity details
     * @param users      Contains the collection of users
     * @return The {@link JsonObject} containing summary details
     */
    @Override
    protected JsonObject convertSummaryV2(final CrmRequest crmRequest, final Contact contact, final JsonObject data, final Collection<User> users) {
        return contactConverter.summaryV2(crmRequest.orgId(), crmRequest.userId(), contact, converterConfigBuilder.build(crmRequest, true, data, true));
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param contact    Represents the {@link Contact}
     * @param data       Holds the entity details
     * @param users      Contains the collection of users
     * @return The {@link JsonObject} containing profile details
     */
    @Override
    protected JsonObject convertProfile(final CrmRequest crmRequest, final Contact contact, final JsonObject data, final Collection<User> users) {
        return contactConverter.profile(contact, converterConfigBuilder.build(crmRequest, true, data, true));
    }

    /**
     * <p>
     * Sets the {@link CrmUser} details into {@link Contact}
     * </p>
     *
     * @param contact Represents the contact details
     * @param users   Contains the collection of {@link User}
     */
    @Override
    public void setUser(final Contact contact, final Collection<User> users) {
        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
        final ValueHolder valueHolder = contact.getValueHolder();
        final CrmUser salesOwner = contact.getSalesOwner();
        final CrmUser createdBy = contact.getCreatedBy();
        final CrmUser updatedBy = contact.getUpdatedBy();

        salesOwner.setUser(userMap.get(salesOwner.getIamId()));
        contact.setSalesOwner(salesOwner);
        createdBy.setUser(userMap.get(createdBy.getIamId()));
        contact.setCreatedBy(createdBy);
        updatedBy.setUser(userMap.get(updatedBy.getIamId()));
        contact.setUpdatedBy(updatedBy);

        if (Objects.nonNull(valueHolder)) {

            for (final CrmUser crmUser : valueHolder.getUserValues()) {
                crmUser.setUser(userMap.get(crmUser.getIamId()));
            }
        }
    }

    /**
     * <p>
     * Retrieves company details related to a contact within an organization
     * </p>
     *
     * @param orgId Represents an organization id
     * @param data  Holds the contact details
     * @return The related company json
     */
    private JsonObject getRelatedCompany(final long orgId, final JsonObject data) {
        final long contactId = data.getLong(ContactFields.ID);

        if (!contactManager.exists(orgId, contactId)) {
            throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        }
        final Optional<Company> optionalCompany = contactManager.getRelatedCompany(orgId, contactId);

        if (optionalCompany.isPresent()) {
            final Company company = optionalCompany.get();
            final Long companyId = company.id();
            final DisplayPicture thumbnail = company.getThumbnail();
            final JsonObject companyJson = Json.object();

            companyJson.put(ContactFields.VALUE, companyId);
            companyJson.put(ContactFields.NAME, company.getName());

            if (Objects.nonNull(thumbnail)) {
                companyJson.put(FieldKeys.THUMBNAIL.getName(), contactConverter.convertThumbnailToJson(companyId, Company.ENTITY_TYPE, thumbnail));
            }

            return companyJson;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    protected Collection<String> stateRequiredTypes() {
        final Collection<String> stateRequiredTypes = new HashSet<>(super.stateRequiredTypes());

        stateRequiredTypes.addAll(Set.of("updateLifecycleStage", "updateLifecycleStatus"));
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
            case "update", "pUpdate", "delete" , "updateLifecycleStage", "updateLifecycleStatus" -> {
                if (data.containsKey(ActionFields.ID)) {
                    final Contact contact = contactManager.get(orgId, data.getLong(ActionFields.ID));

                    contactManager.loadSummaryDetails(orgId, contact);
                    yield List.of(contact);
                }
                yield Collections.emptyList();
            }
            default -> Collections.emptyList();
        };
    }

    /**
     * <p>
     * Fetches the {@link Company} related summary fields
     * </p>
     *
     * @param jsonObject  Represents the request {@link JsonObject}
     * @param crmRequest  Represents the {@link CrmRequest} data
     * @param crmResponse Represents the {@link CrmResponse} to be sent
     */
    private void summary(final JsonObject jsonObject, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final long orgId = crmRequest.orgId();
        final Contact contact = get(orgId, jsonObject.getLong(ContactFields.ID));

        contactManager.loadSummaryDetails(orgId, contact);
        fetchAndDispatchSummary(crmRequest, jsonObject, contact, crmResponse);
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest Holds the {@link CrmRequest} details
     * @param data       Which contains the request data
     * @param contact    Represents the entity model
     * @param users      Contains the collection of users
     * @return The {@link JsonObject} details
     */
    @Override
    protected JsonObject convertSummary(final CrmRequest crmRequest, final JsonObject data, final Contact contact, final Collection<User> users) {
        if (!users.isEmpty()) {
            setUser(contact, users);
        }

        return contactConverter.convertSummaryIntoJson(contact, converterConfigBuilder.build(crmRequest, true, data, true), data.optBoolean(FieldKeys.IS_HIDDEN.getName(), false));
    }

    /**
     * Sets the response with contact overview details such as activity type and active users etc.
     *
     * @param orgId    Refers the organization id.
     * @param data     Refers the request json.
     * @param response Refers the {@link CrmResponse} to be sent.
     */
    @Override
    public void overview(final long orgId, final JsonObject data, final CrmResponse response) {
        final long contactId = data.getLong(ContactFields.ID);

        if (contactManager.exists(orgId, contactId)) {
            final SummaryOverview summaryOverview = contactManager.getSummaryOverview(orgId, contactId, Source.CONTACT);

            fetchUsersForOverview(orgId, summaryOverview, response);
        } else {
            throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        }
    }

    /**
     * <p>
     * Fetches the contact profile details
     * </p>
     *
     * @param orgId    Refers the organization id.
     * @param data     Refers the request json data.
     * @param request  Refers the crm request object.
     * @param response Refers the response to be sent.
     */
    private void socialProfile(final Long orgId, final JsonObject data, final CrmRequest request, final CrmResponse response) {
        final long contactId = data.getLong(ContactFields.ID);

        if (contactManager.exists(orgId, contactId)) {
            contactConverter.convertSocialProfile(contactManager.getSocialProfile(orgId, contactId),
                    converterConfigBuilder.build(request, true, data)).forEach(response::put);
        } else {
            throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        }
    }

    /**
     * <p>
     * Adds a contact to a marketing list. If the marketing list doesn't exist, it creates a new marketing list and adds the contact to it.
     * </p>
     *
     * @param request  Represents the {@link CrmRequest}
     * @param response Represents the {@link CrmResponse}
     */
    private void bAddContactToMarketingList(final CrmRequest request, final CrmResponse response) {
        final JsonObject data = request.getRequestJson().getJsonObject(ActionFields.DATA);
        final Optional<FieldInfo> marketingListField = fieldService.getDefaultFieldByAliasName(Source.CONTACT, ContactFields.MARKETING_LIST);

        if (marketingListField.isPresent()) {
            final JsonObject updatedItemJson = Json.object();
            updatedItemJson.put(String.valueOf(marketingListField.get().id()), data.getLong(MarketingListFields.MARKETING_LIST_ID));

            data.put(BulkActionFields.UPDATE_ITEM, updatedItemJson);
            bUpdate(request, response, ((CrmEventRequest) request).getEventData(), false);
        }
    }
}
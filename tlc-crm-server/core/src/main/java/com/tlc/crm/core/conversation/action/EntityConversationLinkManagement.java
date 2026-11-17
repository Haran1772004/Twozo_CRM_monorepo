package com.tlc.crm.core.conversation.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.SecureEventAction;
import com.tlc.crm.common.iam.api.manager.CrmUserManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.LifeCycleStage;
import com.tlc.crm.core.api.contact.model.LifeCycleStatus;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.deal.manager.PipelineStageManager;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.DealValue;
import com.tlc.crm.core.api.deal.model.PipelineStage;
import com.tlc.crm.core.api.deal.model.ProductDetail;
import com.tlc.crm.core.api.deal.model.ProductDetailsHolder;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.api.product.manager.VariationManager;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.manager.EntityMailManager;
import com.tlc.crm.core.conversation.api.manager.EntityConversationLinkManager;
import com.tlc.crm.core.conversation.status.EntityConversationLinkErrorCode;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;

import com.tlc.crm.entity.service.FieldService;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;

import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.tlc.crm.entity.fields.source.Source.COMPANY;
import static com.tlc.crm.entity.fields.source.Source.CONTACT;
import static com.tlc.crm.entity.fields.source.Source.DEAL;

/**
 * Manages the entity email linking related web actions
 *
 * @author Kishore J
 * @version 1.0
 */
@WebAction(path = "/entity/conversation/link/mgmt")
public class EntityConversationLinkManagement extends SecureEventAction {

    private final ConverterConfigBuilder converterConfigBuilder;
    private final ModelValidator modelValidator;
    private final DealConverter dealConverter;
    private final ContactManager contactManager;
    private final DealManager dealManager;
    private final CoreMailManager mailManager;
    private final ContactConverter contactConverter;
    private final CrmUserManager crmUserManager;
    private final CompanyManager companyManager;
    private final ProductManager productManager;
    private final PipelineStageManager pipelineStageManager;
    private final VariationManager variationManager;
    private final TerritoryManager territoryManager;
    private final LifeCycleStageManager lifeCycleStageManager;
    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final EntityConversationLinkManager entityConversationLinkManager;
    private final EntityMailManager entityMailManager;
    private final FieldService fieldService;
    private final OptionService optionService;

    public EntityConversationLinkManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        modelValidator = ValidatorServiceProvider.get().modelValidator();
        dealConverter = DealConverter.getInstance();
        contactManager = ContactManager.getInstance();
        dealManager = DealManager.getInstance();
        mailManager = CoreMailManager.getInstance();
        contactConverter = ContactConverter.getInstance();
        crmUserManager = CrmUserManager.getInstance();
        companyManager = CompanyManager.getInstance();
        productManager = ProductManager.getInstance();
        pipelineStageManager = PipelineStageManager.getInstance();
        variationManager = VariationManager.getInstance();
        territoryManager = TerritoryManager.getInstance();
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
        lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        entityConversationLinkManager = EntityConversationLinkManager.getInstance();
        entityMailManager = EntityMailManager.getInstance();
        fieldService = FieldService.getSecureInstance();
        optionService = OptionService.getInstance();
    }

    @Override
    protected void secureProcess(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(CoreEmailFields.TYPE.getName());

        switch (type) {
            case "linkSource" -> linkSource(request, response);
            case "unlinkSource" -> detachEmailFromSource(request, response);
        }
    }

    /**
     * <p>
     * Links an email thread to a source based on the provided request data
     * </p>
     *
     * @param request  Represents the {@link CrmRequest}
     * @param response Represents the  {@link CrmResponse}
     */
    private void linkSource(final CrmRequest request, final CrmResponse response) {
        final JsonObject data = request.getRequestJson().getJsonObject(CoreEmailFields.DATA.getName());
        final Source source = Source.get(data.getString(CoreEmailFields.SOURCE.getName()));

        switch (source) {
            case CONTACT -> linkToContact(request, response);
            case DEAL -> linkToDeal(request, response);
        }
    }

    /**
     * <p>
     * Links an email thread to a deal based on the provided request data
     * </p>
     *
     * @param request  Represents the {@link CrmRequest}
     * @param response Represents the {@link CrmResponse}
     */
    private void linkToDeal(final CrmRequest request, final CrmResponse response) {
        final JsonObject data = request.getRequestJson().getJsonObject(CoreEmailFields.DATA.getName());
        final long threadId = data.getLong(CoreEmailFields.THREAD_ID.getName());
        final long orgId = request.orgId();
        final long userId = request.userId();

        if (mailManager.isThreadExists(orgId, userId, Set.of(threadId), true)) {
            final JsonObject dealJson = data.getJsonObject(CoreEmailFields.DEAL.getName());

            if (!entityMailManager.isThreadLinkedWithDeal(orgId, threadId)) {
                final String dealIdKey = CoreEmailFields.DEAL_ID.getName();

                if (dealJson.containsKey(dealIdKey)) {
                    final Long dealId = dealJson.getLong(dealIdKey);

                    if (dealManager.exists(orgId, dealId)) {
                        entityConversationLinkManager.linkThreadToDeal(orgId, userId, dealId, threadId);
                        response.put(CoreEmailFields.LINKED_TO_DEAL.getName(), dealId);
                    } else {
                        throw ErrorCode.get(EntityConversationLinkErrorCode.DEAL_NOT_FOUND, "i18n.notifications.mail.deal.not.found");
                    }
                } else {
                    final Deal deal = dealConverter.convert(converterConfigBuilder.build(request,
                            false, data), dealJson);
                    final PipelineStage pipelineStage = deal.getPipelineStage();

                    if (Objects.nonNull(pipelineStage)) {
                        final String pipelineStageName = pipelineStageManager.get(orgId, pipelineStage.id()).getName();

                        if (Objects.nonNull(pipelineStageName)) {
                            deal.setState(Deal.State.getStateByName(pipelineStageName));
                        }
                    }
                    final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(deal), Group.Create.class);

                    if (errors.isEmpty()) {
                        validate(deal);
                        entityConversationLinkManager.createNewDealAndLinkConversation(orgId, userId, deal, threadId);
                        postAction(orgId, userId, Action.CREATE, List.of(deal));
                        response.put(CoreEmailFields.LINKED_TO_DEAL.getName(), deal.id());
                    } else {
                        final ErrorCode errorCode = ErrorCode.get(EntityConversationLinkErrorCode.CREATE_ACTION_FAILED, "i18n.common.create.failed");

                        response.setErrorCode(errorCode);
                        response.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DATA_VALIDATION_FAILED), errors));
                    }
                }
            } else {
                throw ErrorCode.get(EntityConversationLinkErrorCode.THREAD_ALREADY_MAPPED_WITH_ANOTHER_DEAL, "i18n.notifications.mail.thread.already.mapped.with.another.deal");
            }
        } else {
            throw ErrorCode.get(EntityConversationLinkErrorCode.THREAD_NOT_FOUND, "i18n.notifications.mail.thread.not.found");
        }
    }

    /**
     * <p>
     * Validates the {@link Deal} details
     * </p>
     *
     * @param deal Represents the {@link Deal}
     */
    private void validate(final Deal deal) {
        final Set<Long> productIds = new HashSet<>();
        final Set<Long> variationIds = new HashSet<>();
        final Set<Long> companyIds = new HashSet<>();
        final Set<Long> contactIds = new HashSet<>();
        final Set<Long> salesOwnerIds = new HashSet<>();
        final Set<Long> territoryIds = new HashSet<>();
        final Map<Long, Collection<Long>> pipelineAndStageIds = new HashMap<>();
        final long orgId = deal.orgId();

        final Contact dealContact = deal.getPrimaryContact();
        final Long contactId = Objects.isNull(dealContact) ? null : dealContact.id();
        final DealValue dealValue = deal.getDealValue();
        final ProductDetailsHolder productDetailsHolder = Objects.isNull(dealValue) ? null : deal.getProductDetailsHolder();
        final Company dealCompany = deal.getCompany();
        final Territory territory = deal.getTerritory();
        final CrmUser salesOwner = deal.getSalesOwner();
        final Long territoryId = Objects.isNull(territory) ? null : territory.id();
        final Long salesOwnerId = Objects.isNull(salesOwner) ? null : salesOwner.id();

        if (Objects.nonNull(contactId)) {
            contactIds.add(contactId);
        }

        if (Objects.nonNull(territoryId)) {
            territoryIds.add(territoryId);
        }

        if (Objects.nonNull(salesOwnerId)) {
            salesOwnerIds.add(salesOwnerId);
        }

        if (Objects.nonNull(dealCompany)) {
            final Long companyId = dealCompany.id();

            if (Objects.nonNull(companyId)) {
                companyIds.add(companyId);
            }
        }

        if (Objects.isNull(deal.getCompany()) && Objects.nonNull(contactId)) {
            final Company contactCompany = dealContact.getCompany();

            if (Objects.isNull(contactCompany)) {
                final Long contactCompanyId = contactManager.getContactCompanyId(contactId, orgId);

                if (Objects.nonNull(contactCompanyId)) {
                    final Company company = new Company();

                    company.setOrgId(orgId);
                    company.setId(contactCompanyId);
                    deal.setCompany(company);
                }
            } else {
                deal.setCompany(contactCompany);
            }
        }

        if (Objects.nonNull(productDetailsHolder)) {
            final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

            if (Objects.nonNull(productDetails)) {

                for (final ProductDetail productDetail : productDetails) {
                    final Long id = productDetail.getProduct().id();

                    if (Objects.nonNull(id)) {
                        productIds.add(id);
                    }

                    if (Objects.nonNull(productDetail.getVariationId())) {
                        variationIds.add(productDetail.getVariationId());
                    }
                }
            }
        }

        pipelineAndStageIds.computeIfAbsent(deal.getPipeline().id(), pipelineIdKey -> new HashSet<>())
                .add(deal.getPipelineStage().id());

        if (!companyIds.isEmpty() && !companyManager.exists(orgId, companyIds)) {
            throw ErrorCode.get(EntityConversationLinkErrorCode.COMPANY_NOT_FOUND);
        }

        if (!contactIds.isEmpty() && !contactManager.exists(orgId, contactIds)) {
            throw ErrorCode.get(EntityConversationLinkErrorCode.CONTACT_NOT_FOUND);
        }

        if (!productIds.isEmpty() && !productManager.exists(orgId, productIds)) {
            throw ErrorCode.get(EntityConversationLinkErrorCode.PRODUCT_NOT_FOUND);
        }

        if (!pipelineAndStageIds.isEmpty() && !pipelineStageManager.exists(orgId, pipelineAndStageIds)) {
            throw ErrorCode.get(EntityConversationLinkErrorCode.INVALID_PIPELINE_OR_STAGE);
        }

        if (!variationIds.isEmpty() && !variationManager.exists(orgId, variationIds)) {
            throw ErrorCode.get(EntityConversationLinkErrorCode.VARIATION_NOT_FOUND);
        }

        if (!salesOwnerIds.isEmpty() && !crmUserManager.exists(orgId, salesOwnerIds)) {
            throw ErrorCode.get(EntityConversationLinkErrorCode.CRM_USER_NOT_FOUND);
        }

        if (!territoryIds.isEmpty() && !territoryManager.exists(orgId, territoryIds)) {
            throw ErrorCode.get(EntityConversationLinkErrorCode.TERRITORY_NOT_FOUND);
        }
    }

    /**
     * <p>
     * Detaches linked mail from the entity
     * </p>
     *
     * @param request  Represents the crm request
     * @param response Represents the crm response
     */
    private void detachEmailFromSource(final CrmRequest request, final CrmResponse response) {
        final long orgId = request.orgId();
        final long userId = request.userId();
        final JsonObject data = request.getRequestJson().getJsonObject(CoreEmailFields.DATA.getName());
        final long threadId = data.getLong(CoreEmailFields.THREAD_ID.getName());
        final long entityId = data.getLong(CoreEmailFields.ENTITY_ID.getName());
        final Optional<Long> optionalThreadMappedUserId = entityMailManager.getThreadMappedUserId(orgId, threadId);

        if (optionalThreadMappedUserId.isPresent() && userId == optionalThreadMappedUserId.get()) {

            if (mailManager.isThreadExists(orgId, userId, Set.of(threadId), true)) {
                final Collection<Long> entityConversationIds = entityConversationLinkManager.getEntityConversationIds(orgId, userId, threadId);

                if (!entityConversationIds.isEmpty()) {

                    switch (Source.get(data.getString(CoreEmailFields.SOURCE.getName()))) {
                        case CONTACT -> {
                            final boolean isRemoveRelatedEntityConversation = entityConversationLinkManager
                                    .isEntityConversationsExists(orgId, entityConversationIds, COMPANY) ||
                                    entityConversationLinkManager.isEntityConversationsExists(orgId, entityConversationIds, DEAL);

                            entityConversationLinkManager.unlinkEntityConversation(orgId, userId, threadId, CONTACT, entityId, entityConversationIds, isRemoveRelatedEntityConversation);
                        }
                        case COMPANY -> {
                            final boolean isRemoveRelatedEntityConversation = entityConversationLinkManager
                                    .isEntityConversationsExists(orgId, entityConversationIds, CONTACT) ||
                                    entityConversationLinkManager.isEntityConversationsExists(orgId, entityConversationIds, DEAL);

                            entityConversationLinkManager.unlinkEntityConversation(orgId, userId, threadId, COMPANY, entityId, entityConversationIds, isRemoveRelatedEntityConversation);
                        }
                        case DEAL -> {
                            final boolean isRemoveRelatedEntityConversation = entityConversationLinkManager
                                    .isEntityConversationsExists(orgId, entityConversationIds, CONTACT) ||
                                    entityConversationLinkManager.isEntityConversationsExists(orgId, entityConversationIds, COMPANY);

                            entityConversationLinkManager.unlinkEntityConversation(orgId, userId, threadId, DEAL, entityId, entityConversationIds, isRemoveRelatedEntityConversation);
                        }
                    }
                    response.put(CoreEmailFields.MESSAGE.getName(), response.resolveI18nString("i18n.notification.mail.conversation.unlinked"));
                }
            } else {
                throw ErrorCode.get(EntityConversationLinkErrorCode.THREAD_NOT_FOUND, "i18n.notifications.mail.thread.not.found");
            }
        } else {
            throw ErrorCode.get(EntityConversationLinkErrorCode.UNLINK_CONVERSATION_ACTION_RESTRICTED, "i18n.notification.mail.conversation.action.restricted");
        }
    }

    /**
     * <p>
     * Links an email thread to a contact based on the provided request data
     * </p>
     *
     * @param request  Represents the {@link CrmRequest}
     * @param response Represents the {@link CrmResponse}
     */
    private void linkToContact(final CrmRequest request, final CrmResponse response) {
        final long orgId = request.orgId();
        final long userId = request.userId();
        final JsonObject data = request.getRequestJson().getJsonObject(CoreEmailFields.DATA.getName());
        final long threadId = data.getLong(CoreEmailFields.THREAD_ID.getName());

        if (mailManager.isThreadExists(orgId, userId, Set.of(threadId), true)) {
            final JsonObject contactJson = data.getJsonObject(CoreEmailFields.CONTACT.getName());
            final String contactIdKey = CoreEmailFields.CONTACT_ID.getName();

            if (contactJson.containsKey(contactIdKey)) {
                final long contactId = contactJson.getLong(contactIdKey);

                if (contactManager.exists(orgId, contactId)) {
                    final String emailValue = contactJson.getString(CoreEmailFields.EMAIL_VALUE.getName());

                    if (!contactManager.isEmailExistsInContact(orgId, contactId, emailValue)) {
                        final String emailField = ContactFields.EMAILS;

                        if (contactManager.getEmailsCount(orgId, contactId) > fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE,
                                emailField).get(emailField).presetConfig().maxLimit()) {
                            throw ErrorCode.get(EntityConversationLinkErrorCode.EMAILS_LIMIT_EXCEEDED, "i18n.contact.emails.limit.exceeded");
                        }
                        entityConversationLinkManager.addEmailToExistingContact(orgId, userId, threadId, contactId, emailValue);
                        response.put(CoreEmailFields.LINKED_TO_CONTACT.getName(), contactId);
                    } else {
                        throw ErrorCode.get(EntityConversationLinkErrorCode.EMAIL_ADDRESS_ALREADY_EXISTS,
                                "i18n.notifications.mail.already.exists");
                    }
                } else {
                    throw ErrorCode.get(EntityConversationLinkErrorCode.CONTACT_NOT_FOUND, "i18n.notifications.mail.contact.not.found");
                }
            } else {
                final Contact contact = contactConverter.convert(converterConfigBuilder.build(request,
                        false, contactJson), contactJson);
                final Collection<ErrorDescriptor> errors = modelValidator.validate(List.of(contact), Group.Create.class);

                if (errors.isEmpty()) {
                    validate(contact);
                    entityConversationLinkManager.createNewContactAndLinkConversation(orgId, userId, threadId, contact);
                    postAction(orgId, userId, Action.CREATE, List.of(contact));
                    response.put(CoreEmailFields.LINKED_TO_CONTACT.getName(), contact.id());
                } else {
                    final ErrorCode errorCode = ErrorCode.get(EntityConversationLinkErrorCode.CREATE_ACTION_FAILED, "i18n.notifications.mail.contact.create.failed");

                    response.setErrorCode(errorCode);
                    response.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, ErrorKeys.DATA_VALIDATION_FAILED), errors));
                }
            }
        } else {
            throw ErrorCode.get(EntityConversationLinkErrorCode.THREAD_NOT_FOUND, "i18n.notifications.mail.thread.not.found");
        }
    }

    /**
     * <p>
     * Validates the {@link Contact} details
     * </p>
     *
     * @param contact Represents the {@link Contact}
     */
    private void validate(final Contact contact) {
        final Set<Long> lifeCycleStageIds = new HashSet<>();
        final Set<Long> lifeCycleStatusIds = new HashSet<>();
        final Collection<Long> lostReasonIds = new HashSet<>();
        final Collection<Long> salesOwnerIds = new HashSet<>();
        final Collection<Long> territoryIds = new HashSet<>();
        final LifeCycleStage lifeCycleStage = contact.getLifeCycleStage();
        final LifeCycleStatus lifeCycleStatus = contact.getLifeCycleStatus();
        final Option lostReason = contact.getLostReason();
        final Collection<Email> emails = contact.getEmails();
        final Collection<Phone> phones = contact.getPhones();
        final Territory territory = contact.getTerritory();
        final long orgId = contact.orgId();

        if (Objects.nonNull(emails) && !emails.isEmpty()) {
            final Collection<String> emailAddress = new HashSet<>();

            for (final Email email : emails) {

                if (!emailAddress.add(email.getValue())) {
                    throw ErrorCode.get(EntityConversationLinkErrorCode.DUPLICATE_EMAIL_ADDRESS, "i18n.contact.duplicate.email.address");
                }
            }
        }

        if (Objects.nonNull(phones) && !phones.isEmpty()) {
            final Collection<String> phoneNumbers = new HashSet<>();

            for (final Phone phone : phones) {

                if (!phoneNumbers.add(phone.getNumber())) {
                    throw ErrorCode.get(EntityConversationLinkErrorCode.DUPLICATE_MOBILE_NUMBER, "i18n.contact.duplicate.phone.number");
                }
            }
        }

        if (Objects.nonNull(lifeCycleStage)) {
            final Long lifecycleStageId = lifeCycleStage.id();

            if (Objects.nonNull(lifecycleStageId)) {
                lifeCycleStageIds.add(lifecycleStageId);
            }
        }

        if (Objects.nonNull(lifeCycleStatus)) {
            final Long lifecycleStatusId = lifeCycleStatus.id();

            if (Objects.nonNull(lifecycleStatusId)) {
                lifeCycleStatusIds.add(lifecycleStatusId);
            }
        }

        if (Objects.nonNull(lostReason)) {
            final Long lostReasonId = lostReason.id();

            if (Objects.nonNull(lostReasonId)) {
                lostReasonIds.add(lostReasonId);
            }
        }

        if (Objects.nonNull(territory)) {
            territoryIds.add(territory.id());
        }
        salesOwnerIds.add(contact.getSalesOwner().id());

        if (!lifeCycleStageIds.isEmpty() && !lifeCycleStageManager.exists(orgId, lifeCycleStageIds)) {
            throw ErrorCode.get(EntityConversationLinkErrorCode.LIFE_CYCLE_STAGE_NOT_FOUND, "i18n.lifecycle.stage.not.found");
        }

        if (!lifeCycleStatusIds.isEmpty() && !lifeCycleStatusManager.exists(orgId, lifeCycleStatusIds)) {
            throw ErrorCode.get(EntityConversationLinkErrorCode.LIFE_CYCLE_STATUS_NOT_FOUND, "i18n.lifecycle.status.not.found");
        }

        if (!lostReasonIds.isEmpty() && !optionService.exists(orgId, lostReason.getFieldId(), lostReasonIds, false)) {
            throw ErrorCode.get(ContactErrorCodes.LOST_REASON_NOT_FOUND, "i18n.contact.lostReason.not.found");
        }

        if (!crmUserManager.exists(orgId, salesOwnerIds)) {
            throw ErrorCode.get(EntityConversationLinkErrorCode.SALES_OWNER_NOT_FOUND, "i18n.contact.salesOwner.not.found");
        }

        if (!territoryIds.isEmpty() && !territoryManager.exists(orgId, territoryIds)) {
            throw ErrorCode.get(EntityConversationLinkErrorCode.TERRITORY_NOT_FOUND, "i18n.contact.territory.not.found");
        }
    }
}

package com.tlc.crm.core.internal.event.onboard.consumer;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.handler.EventDataHandler;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.entity.fields.OrgOnboardFields;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.loader.DefaultFieldValuesLoader;
import com.tlc.crm.entity.manager.field.type.OptionManager;
import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.notification.app.Event;
import com.tlc.iam.client.UserService;
import com.tlc.iam.common.models.Organization;
import com.tlc.iam.common.models.User;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Performs an org's onboard related actions such as Contact/Company creation once sign up is completed.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class OrgOnboardingPerformer implements SecureUserTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrgOnboardingPerformer.class);

    private final UserService userService;
    private final CompanyManager companyManager;
    private final ContactManager contactManager;
    private final EventDataHandler eventDataHandler;

    private final Long targetIamOrgId;
    private final Long targetIamSalesOwnerId;
    private final Long targetCrmAdminId;
    private final Long targetCrmSalesOwnerId;
    private final Long targetCrmOrgId;
    private final Long companySizeFieldId;
    private final Long noOfUsersFieldId;
    private final Long industryTypeFieldId;
    private final Long webSourceOptionId;

    public OrgOnboardingPerformer(final Configurations configurations) {
        userService = UserService.getInstance();
        eventDataHandler = EventDataHandler.getInstance();
        companyManager = CompanyManager.getInstance();
        contactManager = ContactManager.getInstance();
        targetIamOrgId = configurations.getLong(OrgOnboardFields.TARGET_IAM_ORG_ID);
        targetCrmOrgId = configurations.getLong(OrgOnboardFields.TARGET_CRM_ORG_ID);
        targetCrmSalesOwnerId = configurations.getLong(OrgOnboardFields.TARGET_SALES_OWNER_CRM_ID);
        targetCrmAdminId = configurations.getLong(OrgOnboardFields.TARGET_CRM_ADMIN_ID);
        targetIamSalesOwnerId = configurations.getLong(OrgOnboardFields.TARGET_SALES_OWNER_IAM_ID);
        companySizeFieldId = configurations.getLong(OrgOnboardFields.COMPANY_SIZE_FIELD_ID);
        noOfUsersFieldId = configurations.getLong(OrgOnboardFields.NO_OF_USERS_FIELD_ID);
        industryTypeFieldId = configurations.getLong(OrgOnboardFields.INDUSTRY_TYPE_FIELD_ID);
        webSourceOptionId = configurations.getLong(OrgOnboardFields.WEB_SOURCE_OPTION_ID);
    }

    /**
     * <p>
     * Parses the payload and initiates the org onboard action.
     * </p>
     *
     * @param payload Represents the raw Kafka message payload.
     */
    public void perform(final StreamMessage<byte[]> streamMessage, final JsonObject payload) {
        final Long onboardIamOrgId = payload.getLong(OrgOnboardFields.IAM_ORG_ID);
        final Long onboardIamUserId = payload.getLong(OrgOnboardFields.IAM_USER_ID);
        final Long onboardOrgId = payload.getLong(OrgOnboardFields.ORG_ID);
        final JsonObject signupData = payload.getJsonObject(OrgOnboardFields.SIGNUP_DATA);

        userService.checkAllUsersActive(targetIamOrgId, List.of(targetIamSalesOwnerId), new EventCallback<>() {

            @Override
            public void onSuccess(final Boolean areAllUsersActive) {
                final Long entitySalesOwnerId = areAllUsersActive ? targetCrmSalesOwnerId : targetCrmAdminId;

                processOnboardedUser(streamMessage, onboardIamOrgId, onboardIamUserId, onboardOrgId, entitySalesOwnerId, signupData);
            }

            @Override
            public void onFailure(final Throwable cause) {
                LOGGER.error("Org onboard action: User active check failed. IamOrgId: {}, IamUserId: {}", onboardIamOrgId, onboardIamUserId);
                cause.printStackTrace();
            }
        });
    }

    /**
     * <p>
     * Fetches the signed-up user and initiates entity creation under the resolved sales owner's context.
     * </p>
     *
     * @param streamMessage      Represents the stream message.
     * @param iamOrgId           Represents the signed-up user's IAM org id.
     * @param iamUserId          Represents the signed-up user's IAM user id.
     * @param entitySalesOwnerId Represents the resolved CRM sales owner id.
     * @param signupData         Holds the raw signup form data.
     */
    private void processOnboardedUser(final StreamMessage<byte[]> streamMessage, final Long iamOrgId, final Long iamUserId,
                                      final Long orgId, final Long entitySalesOwnerId, final JsonObject signupData) {
        userService.fetchUsers(iamOrgId, List.of(iamUserId), new EventCallback<>() {

            @Override
            public void onSuccess(final Collection<User> users) {

                try {
                    final User signedUpUser = users.iterator().next();
                    final String companyName = signupData.optString(OrgOnboardFields.COMPANY_NAME, null);
                    final Company company = buildCompany(companyName, signupData);
                    final Contact contact = buildContact(signedUpUser, signupData, company);

                    secureProcess(targetCrmOrgId, entitySalesOwnerId, () -> {
                        final CrmUser entitySalesOwner = com.tlc.crm.common.iam.service.UserService.getInstance()
                                .getUser(targetCrmOrgId, entitySalesOwnerId);
                        final EventData contactEventData = new SecureEventData(targetCrmOrgId, entitySalesOwnerId);

                        assignSalesOwner(contact, entitySalesOwner);

                        contactEventData.setValues(List.of(contact));
                        contactEventData.setAction(Action.CREATE);

                        if (Objects.nonNull(company)) {
                            final EventData companyEventData = new SecureEventData(targetCrmOrgId, entitySalesOwnerId);

                            assignSalesOwner(company, entitySalesOwner);
                            companyManager.create(company);
                            companyEventData.setValues(List.of(company));
                            companyEventData.setAction(Action.CREATE);
                            eventDataHandler.handleEvents(companyEventData, Event.Type.POST);
                        }
                        contactManager.create(contact);
                        eventDataHandler.handleEvents(contactEventData, Event.Type.POST);
                        streamMessage.clean();
                    });
                } catch (Throwable throwable) {
                    LOGGER.error("Org onboard action: Failed to fetch signed-up user. IAM Org ID: {}, IAM User ID: {}, Reason: {}", iamOrgId, iamUserId, throwable.getMessage());
                    throwable.printStackTrace();
                    throw ErrorCode.get(EntityErrorCodes.ORG_ONBOARDING_SIGNED_UP_USER_FETCH_FAILED, throwable);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.error("Org onboard action: User callback failed. IAM Org ID: {}, IAM User ID: {}, Reason: {}", iamOrgId, iamUserId, throwable.getMessage());
                throwable.printStackTrace();
            }
        });
    }

    /**
     * <p>
     * Assigns the sales owner, created by and updated by fields on the given entity.
     * </p>
     *
     * @param entity     Represents the target entity.
     * @param salesOwner Represents the sales owner to assign.
     */
    private void assignSalesOwner(final Entity entity, final CrmUser salesOwner) {
        entity.setSalesOwner(salesOwner);
        entity.setCreatedBy(salesOwner);
        entity.setUpdatedBy(salesOwner);
    }


    /**
     * <p>
     * Builds a {@link Company} entity from signup data.
     * </p>
     *
     * @param companyName Represents the resolved company name.
     * @param signupData  Holds the raw signup form data.
     * @return The built {@link Company}.
     */
    private Company buildCompany(final String companyName, final JsonObject signupData) {

        if (Objects.isNull(companyName)) {
            return null;
        }
        final Company company = new Company();
        final ValueHolder valueHolder = new ValueHolder();
        final Short noOfEmployees = signupData.containsKey(OrgOnboardFields.ORG_SIZE) ? signupData.getShort(OrgOnboardFields.ORG_SIZE) : null;
        final Short companySize = signupData.containsKey(OrgOnboardFields.USERS_SIZE) ? signupData.getShort(OrgOnboardFields.USERS_SIZE) : null;
        final Short industryType = signupData.containsKey(OrgOnboardFields.INDUSTRY_TYPE) ? signupData.getShort(OrgOnboardFields.INDUSTRY_TYPE) : null;
        final DefaultFieldValuesLoader defaultFieldValuesLoader = DefaultFieldValuesLoader.getInstance();

        company.setOrgId(targetCrmOrgId);
        company.setName(companyName);

        if (Objects.nonNull(companySize)) {
            valueHolder.put(companySizeFieldId, Organization.OrgSize.valueOf(companySize).getName());
        }

        if (Objects.nonNull(noOfEmployees)) {
            valueHolder.put(noOfUsersFieldId, Organization.UsersSize.valueOf(noOfEmployees).getName());
        }

        if (Objects.nonNull(industryType)) {
            valueHolder.put(industryTypeFieldId, Organization.IndustryType.valueOf(industryType).name());
        }
        company.setValueHolder(valueHolder);
        defaultFieldValuesLoader.load(company);

        return company;
    }

    /**
     * <p>
     * Builds a {@link Contact} entity from signup data and user details.
     * </p>
     *
     * @param signedUpUser Represents the signed-up user.
     * @param signupData   Holds the raw signup form data.
     * @param company      Represents the associated company.
     * @return The built {@link Contact}.
     */
    private Contact buildContact(final User signedUpUser, final JsonObject signupData, final Company company) {
        final Contact contact = new Contact();
        final String emailAddress = signedUpUser.getEmail();
        final String phoneNumber = signedUpUser.getMobile();
        final String jobTitle = signupData.optString(OrgOnboardFields.JOB_TITLE, null);
        final DefaultFieldValuesLoader defaultFieldValuesLoader = DefaultFieldValuesLoader.getInstance();

        contact.setOrgId(targetCrmOrgId);
        contact.setFirstName(signedUpUser.getFirstName());
        contact.setLastName(signedUpUser.getLastName());
        contact.setDesignation(jobTitle);
        contact.setCompany(company);
        contact.setContactSource(OptionManager.getInstance().get(webSourceOptionId));

        if (Objects.nonNull(emailAddress)) {
            final Email email = new Email();

            email.setOrgId(targetCrmOrgId);
            email.setType(Email.Type.WORK);
            email.setValue(emailAddress);
            contact.setEmails(List.of(email));
            contact.setPrimaryEmail(email);
        }

        if (Objects.nonNull(phoneNumber)) {
            final Phone phone = new Phone();

            phone.setOrgId(targetCrmOrgId);
            phone.setType(Phone.Type.WORK);
            phone.setNumber(phoneNumber);
            contact.setPhones(List.of(phone));
            contact.setPrimaryPhone(phone);
        }
        defaultFieldValuesLoader.load(contact);

        return contact;
    }
}
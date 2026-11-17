package com.tlc.crm.core.action.rest;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.rest.CrmSecureRestAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.formatter.ContactRestReqFormatter;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.core.validator.internal.ContactDataValidator;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.iam.common.models.User;

import com.tlc.validator.TlcModel;

import com.tlc.web.HttpMethod;
import com.tlc.web.undertow.rest.annotation.ApiRoute;
import com.tlc.web.undertow.rest.annotation.RestApiHandler;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link Contact} related REST web actions.
 * </p>
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@RestApiHandler(path = "/v1/contact")
public class ContactRestManagementV1 extends CrmSecureRestAction<Contact> {

    private final ContactRestReqFormatter contactRestReqFormatter;
    private final ContactConverter contactConverter;
    private final ContactManager contactManager;
    private final ConverterConfigBuilder converterConfigBuilder;
    private final ContactDataValidator contactDataValidator;
    private final FieldService fieldService;

    public ContactRestManagementV1() {
        this.contactRestReqFormatter = ContactRestReqFormatter.getInstance();
        this.contactConverter = ContactConverter.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.converterConfigBuilder = ConverterConfigBuilder.getInstance();
        this.contactDataValidator = ContactDataValidator.getInstance();
        this.fieldService = FieldService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing path parameters, query parameters,
     *                 headers, and contextual metadata.
     * @param response The CRM response used to write the result or error payload.
     */
    @ApiRoute(path = "/{id}", method = HttpMethod.GET)
    @Override
    public void get(final CrmRequest request, final CrmResponse response) {
        final long orgId = request.orgId();
        final long id = Long.parseLong(request.getParameter("id"));

        if (contactManager.exists(orgId, id)) {
            fetchAndDispatchUser(request, contactManager.get(request.orgId(), id), response);
        } else {
            throw ErrorCode.get(ContactErrorCodes.CONTACT_NOT_FOUND, "i18n.contact.not.found");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing the payload and request metadata.
     * @param response The CRM response used to send the updated resource or status result.
     */
    @ApiRoute(path = "/{id}", method = HttpMethod.PUT)
    @Override
    public void put(final CrmRequest request, final CrmResponse response) {
        final String idKey = FieldKeys.ID.getName();
        final JsonObject requestJson = request.getRequestJson();
        final EventData eventData = new SecureEventData(request.orgId(), request.userId());

        eventData.setLocale(request.locale());
        final Collection<TlcModel> currentModels = getCurrentModels(request);

        if (!currentModels.isEmpty()) {
            eventData.setPreviousValues(currentModels);
        }

        requestJson.put(idKey, Long.parseLong(request.getParameter(idKey)));
        final Contact contact = convert(request, request.getRequestJson());

        update(response, request.orgId(), request.userId(), eventData, List.of(contact));
    }

    @Override
    public void getAll(final CrmRequest request, final CrmResponse response) {

    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing the payload of the resource to be created.
     * @param response The CRM response used to send the created resource or creation status.
     */
    @ApiRoute(method = HttpMethod.POST)
    @Override
    public void post(final CrmRequest request, final CrmResponse response) {
        final long orgId = request.orgId();
        final long userId = request.userId();
        final Contact model = convert(request, request.getRequestJson());
        final EventData eventData = new SecureEventData(orgId, userId);

        eventData.setLocale(request.locale());
        create(response, orgId, userId, eventData, List.of(model));
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing the partial update payload.
     * @param response The CRM response used to return the modified resource or status details.
     */
    @ApiRoute(path = "/{id}", method = HttpMethod.PATCH)
    @Override
    public void patch(final CrmRequest request, final CrmResponse response) {
        final String idKey = FieldKeys.ID.getName();
        final JsonObject requestJson = request.getRequestJson();
        final EventData eventData = new SecureEventData(request.orgId(), request.userId());

        eventData.setLocale(request.locale());
        final Collection<TlcModel> currentModels = getCurrentModels(request);

        if (!currentModels.isEmpty()) {
            eventData.setPreviousValues(currentModels);
        }

        requestJson.put(idKey, Long.parseLong(request.getParameter(idKey)));
        update(response, request.orgId(), request.userId(), eventData, List.of(convertPartialData(request, requestJson)));
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing identifiers or parameters required to locate the resource.
     * @param response The CRM response used to send deletion confirmation or error details.
     */
    @ApiRoute(path = "/{id}", method = HttpMethod.DELETE)
    @Override
    public void delete(final CrmRequest request, final CrmResponse response) {
        final EventData eventData = new SecureEventData(request.orgId(), request.userId());

        eventData.setLocale(request.locale());
        final Collection<TlcModel> currentModels = getCurrentModels(request);

        if (!currentModels.isEmpty()) {
            eventData.setPreviousValues(currentModels);
        }
        delete(response, request.orgId(), request.userId(), eventData, List.of(contactManager.get(request.orgId(), Long.parseLong(request.getParameter("id")))));
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing identifiers or parameters required to locate the resource.
     * @param response The CRM response used to send deletion confirmation or error details.
     */
    @ApiRoute(method = HttpMethod.OPTIONS)
    @Override
    public void options(final CrmRequest request, final CrmResponse response) {

    }

    /**
     * {@inheritDoc}
     *
     * @param request    The CRM request containing contextual information.
     * @param jsonObject The JSON payload to be converted.
     * @return
     */
    @Override
    protected Contact convert(final CrmRequest request, final JsonObject jsonObject) {
        final JsonObject contactJson = contactRestReqFormatter.format(request.orgId(), jsonObject, request.i18nResolver());
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, contactJson);

        return contactConverter.convert(converterConfig, contactJson);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConfigManager<Contact> getConfigManager() {
        return contactManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    The request data.
     * @param jsonObject The request json.
     */
    @Override
    protected Contact convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final JsonObject contactJson = contactRestReqFormatter.format(request.orgId(), jsonObject, request.i18nResolver());
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, contactJson);

        return contactConverter.convert(converterConfig, contactJson);
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
     * {@inheritDoc}
     */
    @Override
    public JsonObject convert(final CrmRequest crmRequest, final Contact contact) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(crmRequest, false, null);

        return contactConverter.convertToRestJson(contact, converterConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param request Represents the {@link CrmRequest}
     * @return The collection of models.
     */
    @Override
    public Collection<TlcModel> getCurrentModels(final CrmRequest request) {
        final long id = Long.parseLong(request.getParameter("id"));
        final long orgId = request.orgId();
        final Contact contact = contactManager.get(orgId, id);

        contactManager.loadSummaryDetails(orgId, contact);
        return List.of(contact);
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

        if (Objects.
                nonNull(contacts) && !contacts.isEmpty()) {

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
                        final Long contactId = contact.id();

                        if ((Objects.isNull(emails) || emails.isEmpty()) && emailPresetConfig.required() && contactManager.hasPrimaryEmail(orgId, userId, contactId)) {
                            throw ErrorCode.get(ContactErrorCodes.PRIMARY_EMAIL_REQUIRED, "i18n.contact.primary.email.required");
                        }

                        if ((Objects.isNull(phones) || phones.isEmpty()) && phonePresetConfig.required() && contactManager.hasPrimaryPhone(orgId, userId, contactId)) {
                            throw ErrorCode.get(ContactErrorCodes.PRIMARY_PHONE_REQUIRED, "i18n.contact.primary.phone.required");
                        }
                    }
                    contactDataValidator.validate(orgId, contacts);
                }
            }
        }

        return true;
    }
}

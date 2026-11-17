package com.tlc.crm.core.action.rest;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.internal.IamErrorCodes;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.list.model.ListType;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.models.ValueHolder;

import com.tlc.iam.common.models.User;

import com.tlc.web.HttpMethod;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.undertow.rest.annotation.ApiRoute;
import com.tlc.web.undertow.rest.annotation.RestApiHandler;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link Contact} list related REST web actions.
 * </p>
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@RestApiHandler(path = "/v1/contact/view")
public class ContactRestListActionV1 extends AbstractSecureRestListActionV1 {

    private final ContactManager contactManager;
    private final ContactConverter contactConverter;

    public ContactRestListActionV1() {
        this.contactManager = ContactManager.getInstance();
        this.contactConverter = ContactConverter.getInstance();
    }

    /**
     * Handles the HTTP GET request for fetching the list of {@code Contact} entities based on the provided request parameters and context.
     * This method processes the request, retrieves the relevant data, and constructs the appropriate response to be sent back to the client.
     *
     * @param request  Refers the incoming {@code CrmRequest}.
     * @param response Refers the {@code CrmResponse} to be sent.
     */
    @ApiRoute(path = "/list", method = HttpMethod.GET)
    public void list(final CrmRequest request, final CrmResponse response) {
        processList(request, response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Provider getProvider() {
        return Provider.CONTACT_LIST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Source getSource() {
        return Source.CONTACT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ListType getListType() {
        return ListType.CONTACT_LIST;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       The ID of the organization for which entities are being fetched; must not be null.
     * @param request     The CRM request containing context and parameters; must not be null.
     * @param entityIds   The collection of entity IDs to retrieve and process; must not be null or empty.
     * @param crmResponse The response object to which the processed entity data should be written; must not be null.
     */
    @Override
    protected void fetchAndDispatchEntities(final Long orgId, final CrmRequest request, final Collection<Long> entityIds, final CrmResponse crmResponse) {
        final Map<Long, Contact> contacts = contactManager.get(orgId, entityIds).stream().collect(Collectors.toMap(Contact::id, Function.identity()));
        final Collection<Long> userIds = new HashSet<>();
        final JsonArray array = Json.array();

        for (final Contact contact : contacts.values()) {
            userIds.addAll(contact.userIds());
        }
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false);

        if (userIds.isEmpty()) {

            for (final Long modelId : entityIds) {
                final Contact contact = contacts.get(modelId);

                array.put(contactConverter.convertToRestJson(contact, converterConfig));
            }
            crmResponse.put(ListActionFields.ROWS, array);
            crmResponse.send();
        } else {
            crmResponse.convertIntoAsync();

            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    try {
                        for (final Long modelId : entityIds) {
                            final Contact contact = contacts.get(modelId);

                            setUser(contact, users);
                            array.put(contactConverter.convertToRestJson(contact, converterConfig));
                        }
                        crmResponse.put(ListActionFields.ROWS, array);
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.user.detail.retrieve.failed", throwable));
                    crmResponse.send();
                }
            };
            userService.fetchUsers(organizationService.getIamOrgId(orgId), crmUserService.getUsers(orgId, userIds).stream().map(CrmUser::getIamId).collect(Collectors.toSet()), usersFetchCallBack);
        }
    }

    /**
     * <p>
     * Sets the {@link CrmUser} details into {@link Contact}.
     * </p>
     *
     * @param contact Represents the {@link Contact} details.
     * @param users   Represents the collection of {@link User}.
     */
    private void setUser(final Contact contact, final Collection<User> users) {
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
}

package com.tlc.crm.notifications.action.call;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.Env;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.common.models.SortConfig;
import com.tlc.crm.common.models.SortType;
import com.tlc.crm.entity.dropdown.CriteriaType;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.fields.FieldKeys;

import com.tlc.crm.notifications.api.call.manager.PhoneNumberManager;
import com.tlc.crm.notifications.api.call.model.PhoneNumber;
import com.tlc.crm.notifications.api.call.model.converter.PhoneNumberConverter;
import com.tlc.crm.notifications.fields.NotificationFields;
import com.tlc.crm.notifications.internal.status.NotificationErrorCodes;

import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.security.permission.annotation.authorizer.PhoneAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.notification.model.telephone.TelephoneProvider;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Handles phone number management operations such as purchasing, updating,
 * deleting, retrieving, and listing phone numbers.
 *
 * @author Sathish
 * @version 1.1
 */
@PhoneAuthorizer(subCategories = SubCategory.Type.ACCESS_TO_PHONE)
@WebAction(path = "/phone/number/mgmt")
public class PhoneNumberManagement extends AuthorizationController {

    private static final String EMPTY_STRING = "";
    private static final String LOCALHOST = "localhost";

    private final PhoneNumberManager phoneNumberManager;
    private final PhoneNumberConverter phoneNumberConverter;

    public PhoneNumberManagement() {
        phoneNumberManager = PhoneNumberManager.getInstance();
        phoneNumberConverter = PhoneNumberConverter.getInstance();
    }

    /**
     * Processes authorized requests for phone number management.
     *
     * @param request     The CRM request object containing request data.
     * @param crmResponse The CRM response object for sending responses.
     * @throws Exception if an error occurs during request processing.
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) throws Exception {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString(FieldKeys.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());
        final Long orgId = request.orgId();

        switch (type) {
            case "buyNumber" -> buyNumber(orgId, data, request, crmResponse);
            case "update" -> updateNumber(orgId, data, crmResponse);
            case "delete" -> deleteNumber(orgId, data, crmResponse);
            case "bDelete" -> bulkDeleteNumbers(orgId, data, crmResponse);
            case "list" -> listNumbers(orgId, data, crmResponse);
            case "get" -> getNumber(orgId, data, crmResponse);
        }
    }

    /**
     * Handles purchasing a new phone number.
     *
     * @param data        The JSON object containing phone number details.
     * @param crmResponse The CRM response object to store the result.
     */
    private void buyNumber(final Long orgId, final JsonObject data, final CrmRequest request, final CrmResponse crmResponse) {
        final PhoneNumber phoneNumber = phoneNumberConverter.convert(orgId, data);

        try {
            if (Env.isDevMode() && (phoneNumberManager.exists() || request.getDomain().contains(LOCALHOST))) {
                setErrorDescriptor(crmResponse, ErrorCode.get(NotificationErrorCodes.COULD_NOT_BUY_NUMBER, "i18n.notifications.can.not.buy.number"), null, FieldKeys.MESSAGE.getName());
            } else {
                final CrmUser crmUser = LoggedInUserInfo.getUser();

                phoneNumber.setDisplayName(crmUser.getFullName());
                phoneNumberManager.buyNumber(phoneNumber);
                crmResponse.put(FieldKeys.CREATED_RECORD.getName(), phoneNumber.id());
                crmResponse.put(FieldKeys.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.notifications.phone.number.purchase.succeed"));
            }
        } catch (final Exception exception) {
            throw ErrorCode.get(NotificationErrorCodes.PHONE_NUMBER_PURCHASE_FAILED,
                    crmResponse.resolveI18nString("i18n.notifications.phone.number.purchase.failed"));
        }
    }

    /**
     * Handles updating an existing phone number's details.
     *
     * @param data        The JSON object containing updated phone number details.
     * @param crmResponse The CRM response object to store the result.
     */
    private void updateNumber(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final PhoneNumber phoneNumber = phoneNumberConverter.convert(orgId, data);

        try {
            if (data.optBoolean("isUnderDevelopment")) {
                phoneNumber.setId(1L);
            } else {
                phoneNumberManager.updateNumberDetails(phoneNumber);
            }
            crmResponse.put(FieldKeys.UPDATED_RECORD.getName(), phoneNumber.id());
            crmResponse.put(FieldKeys.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.notifications.phone.number.update.succeed"));
        } catch (final Exception exception) {
            throw ErrorCode.get(NotificationErrorCodes.PHONE_NUMBER_UPDATE_FAILED,
                    crmResponse.resolveI18nString("i18n.notifications.phone.number.update.failed"));
        }
    }

    /**
     * Handles deleting a phone number by its ID.
     *
     * @param orgId       The organization ID.
     * @param data        The JSON object containing the phone number ID.
     * @param crmResponse The CRM response object to store the result.
     */
    private void deleteNumber(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        try {
            if (!data.optBoolean("isUnderDevelopment")) {
                phoneNumberManager.deleteNumber(orgId, data.getLong(NotificationFields.ID.getName()));
            }
            crmResponse.put(FieldKeys.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.notifications.phone.number.delete.succeed"));
        } catch (final Exception exception) {
            throw ErrorCode.get(NotificationErrorCodes.PHONE_NUMBER_DELETE_FAILED,
                    crmResponse.resolveI18nString("i18n.notifications.phone.number.delete.failed"));
        }
    }

    /**
     * Handles bulk deletion of phone numbers.
     *
     * @param orgId       The organization ID.
     * @param data        The JSON object containing the list of phone number IDs.
     * @param crmResponse The CRM response object to store the result.
     */
    private void bulkDeleteNumbers(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        try {
            final JsonArray numberIds = data.getJsonArray(NotificationFields.IDS.getName());

            if (!data.optBoolean("isUnderDevelopment")) {
                final Collection<Long> ids = new HashSet<>();

                for (int index = 0; index < numberIds.size(); index++) {
                    ids.add(numberIds.getLong(index));
                }
                phoneNumberManager.deleteNumbers(orgId, ids);
            }
            crmResponse.put(FieldKeys.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.notifications.phone.number.delete.succeed"));
        } catch (final Exception exception) {
            throw ErrorCode.get(NotificationErrorCodes.PHONE_NUMBER_DELETE_FAILED,
                    crmResponse.resolveI18nString("i18n.notifications.phone.number.delete.failed"));
        }
    }

    /**
     * Handles listing all available phone numbers with pagination and sorting.
     *
     * @param orgId       The organization ID.
     * @param data        The JSON object containing pagination and sorting details.
     * @param crmResponse The CRM response object to store the list of numbers.
     */
    private void listNumbers(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final String searchText = data.getNullableString(NotificationFields.SEARCH_TEXT.getName());
        final int start = data.getInt(NotificationFields.START.getName());
        final DropdownCriteria dropdownCriteria = new DropdownCriteria();

        dropdownCriteria.setCriteriaType(CriteriaType.getCriteriaType(CriteriaType.CONTAINS));
        dropdownCriteria.setCriteriaValue(Objects.isNull(searchText) ? EMPTY_STRING : searchText);
        final long totalNumbersCount = phoneNumberManager.getTotalNumbersCount(orgId, dropdownCriteria, TelephoneProvider.TWILIO);
        final JsonArray jsonArray = Json.array();

        if (start <= totalNumbersCount) {
            final SortConfig sortConfig = getSortDetails(data);
            final int limit = data.getInt(NotificationFields.LIMIT.getName());
            final Collection<PhoneNumber> numbers = phoneNumberManager.getNumbers(orgId, searchText,
                    new LimitConfig(start, limit), sortConfig);

            for (final PhoneNumber phoneNumber : numbers) {
                jsonArray.put(phoneNumberConverter.convert(phoneNumber));
            }
            crmResponse.put(FieldKeys.HAS_MORE.getName(), start + limit <= totalNumbersCount);
        } else {
            crmResponse.put(FieldKeys.HAS_MORE.getName(), false);
        }
        crmResponse.put(FieldKeys.LIST.getName(), jsonArray);
        crmResponse.put(FieldKeys.TOTAL_RECORDS.getName(), totalNumbersCount);
    }

    /**
     * Handles fetching details of a specific phone number by its ID.
     *
     * @param orgId       The organization ID.
     * @param data        The JSON object containing the phone number ID.
     * @param crmResponse The CRM response object to store the phone number details.
     */
    private void getNumber(final Long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final long id = data.getLong(NotificationFields.ID.getName());

        if (phoneNumberManager.exists(orgId, id)) {
            final PhoneNumber phoneNumber = phoneNumberManager.getNumber(orgId, id);

            crmResponse.put(NotificationFields.NUMBER.getName(), phoneNumberConverter.convert(phoneNumber));
        } else  {
            throw ErrorCode.get(NotificationErrorCodes.PHONE_NUMBER_NOT_FOUND,
                    crmResponse.resolveI18nString("i18n.notifications.phone.number.not.found"));
        }
    }

    /**
     * Extracts sorting details from the request data.
     *
     * @param data The JSON object containing sorting parameters.
     * @return A {@link SortConfig} object representing the sorting field and order.
     */
    private SortConfig getSortDetails(final JsonObject data) {
        final JsonObject sortJson = data.optJsonObject(NotificationFields.SORT.getName());

        if (Objects.isNull(sortJson)) {
            return new SortConfig(NotificationFields.DISPLAY_NAME.getName(), SortType.ASCENDING);
        } else {
            return new SortConfig(
                    sortJson.getString(FieldKeys.FIELD.getName()),
                    SortType.getSortType(sortJson.getString(FieldKeys.TYPE.getName()))
            );
        }
    }
}
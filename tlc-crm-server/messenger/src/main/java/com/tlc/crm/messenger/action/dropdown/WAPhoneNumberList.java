package com.tlc.crm.messenger.action.dropdown;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.entity.dropdown.AsyncDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.AsyncDropdown;

import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.messenger.api.channel.model.ChannelState;
import com.tlc.crm.messenger.api.channel.model.CrmWAPhoneNumber;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.crm.messenger.internal.status.whatsapp.WAErrorCodes;
import com.tlc.crm.messenger.page.WAChannelPageHandler;

import com.tlc.social.client.service.whatsapp.WABusinessAccountService;
import com.tlc.social.common.whatsapp.model.WABusinessPhoneNumber;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Manages WhatsApp phone number list related dropdown actions.
 *
 * @author Maharaja S
 * @version 1.0
 */
@AsyncDropdown(name = "waPhoneNumberList")
public class WAPhoneNumberList implements AsyncDropdownAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(WAPhoneNumberList.class);

    private final WABusinessAccountService waBusinessAccountService;

    public WAPhoneNumberList() {
        waBusinessAccountService = WABusinessAccountService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public void fetchAndDispatchData(final DropdownRequest dropdownRequest, final CrmResponse crmResponse) {
        final Long orgId = dropdownRequest.getOrgId();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final int start = dropdownLimit.start();
        final int limit = dropdownLimit.limit();
        final WAChannelPageHandler waChannelPageHandler = new WAChannelPageHandler(start, limit, orgId, List.of(ChannelState.ENABLED.getId()));
        final long totalRecords = waChannelPageHandler.getTotalRecordsCount();
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (NumericalConstant.ZERO < totalRecords) {
            final Collection<CrmWAPhoneNumber> crmBusinessPhoneNumbers = waChannelPageHandler.fetchPageData().getData();
            final Collection<Long> businessPhoneNumberIds = crmBusinessPhoneNumbers.stream().map(CrmWAPhoneNumber::id).collect(Collectors.toUnmodifiableSet());

            crmResponse.convertIntoAsync();

            final EventCallback<Map<Long, WABusinessPhoneNumber>> callback = new EventCallback<>() {

                @Override
                public void onSuccess(final Map<Long, WABusinessPhoneNumber> phoneNumberMap) {

                    try {
                        for (final CrmWAPhoneNumber crmWAPhoneNumber : crmBusinessPhoneNumbers) {
                            final WABusinessPhoneNumber waBusinessPhoneNumber = phoneNumberMap.get(crmWAPhoneNumber.id());

                            if (Objects.nonNull(waBusinessPhoneNumber)) {
                                final JsonObject phoneNumberJson = Json.object();

                                phoneNumberJson.put(MessengerFields.ID, crmWAPhoneNumber.getCrmPhoneNumberId());
                                phoneNumberJson.put(MessengerFields.NAME, waBusinessPhoneNumber.getDisplayPhoneNumber());

                                optionContainer.addOption(phoneNumberJson);
                            }
                        }
                        crmResponse.put(FieldKeys.TOTAL_RECORDS.getName(), totalRecords);
                        crmResponse.put(FieldKeys.HAS_MORE.getName(), start + limit <= totalRecords);
                        crmResponse.put(FieldKeys.OPTIONS.getName(), optionContainer.getOptions());
                        crmResponse.put(FieldKeys.SIZE.getName(), optionContainer.size());
                    } catch (final Throwable cause) {
                        final String errorDetail = cause.getMessage();

                        LoggerUtil.logError(LOGGER, cause, "Whatsapp: Business phone number list retrieved, but data conversion failed. OrgId: {}, Phone number Ids: {}, Message: {}", orgId, businessPhoneNumberIds, errorDetail);
                        setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.FAILED_TO_FETCH_BUSINESS_PHONE_NUMBERS, "i18n.whatsapp.business.phone.numbers.retrieval.failed"), errorDetail);
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable cause) {
                    final String errorDetail = cause.getMessage();

                    LoggerUtil.logError(LOGGER, cause, "Whatsapp: Failed to fetch whatsapp phone number list, OrgId: {}, Phone number Ids: {}, Message: {}", orgId, businessPhoneNumberIds, errorDetail);
                    setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.FAILED_TO_FETCH_BUSINESS_PHONE_NUMBERS, "i18n.whatsapp.business.phone.numbers.retrieval.failed"), errorDetail);
                    crmResponse.send();
                }
            };

            waBusinessAccountService.fetchBusinessPhoneNumbers(businessPhoneNumberIds, callback);
        } else {
            crmResponse.put(FieldKeys.TOTAL_RECORDS.getName(), totalRecords);
            crmResponse.put(FieldKeys.HAS_MORE.getName(), start + limit <= totalRecords);
            crmResponse.put(FieldKeys.OPTIONS.getName(), optionContainer.getOptions());
            crmResponse.put(FieldKeys.SIZE.getName(), optionContainer.size());
            crmResponse.send();
        }
    }

    /**
     * <p>
     * Sets {@link ErrorDescriptor}.
     * </p>
     *
     * @param crmResponse {@link CrmResponse}.
     * @param errorCode   {@link ErrorCode}.
     * @param detail      Represents the more detailed explanation of the error, often useful for debugging.
     */
    private void setErrorDescriptor(final CrmResponse crmResponse, final ErrorCode errorCode, final String detail) {
        crmResponse.setErrorCode(errorCode);
        crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode, detail, null)));
    }
}
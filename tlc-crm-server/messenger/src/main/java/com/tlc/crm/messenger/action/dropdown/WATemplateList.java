package com.tlc.crm.messenger.action.dropdown;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.page.Page;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.entity.dropdown.AsyncDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.AsyncDropdown;
import com.tlc.crm.entity.fields.FieldKeys;

import com.tlc.crm.messenger.api.channel.manager.WABusinessAccountManager;
import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.crm.messenger.internal.status.whatsapp.WAErrorCodes;

import com.tlc.social.client.service.whatsapp.WABusinessAccountService;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.model.template.WATemplate;

import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

/**
 * Manages WhatsApp template list related dropdown actions.
 *
 * @author Maharaja S
 * @version 1.0
 */
@AsyncDropdown(name = "waTemplateList")
public class WATemplateList implements AsyncDropdownAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(WATemplateList.class);

    private final WABusinessAccountManager waBusinessAccountManager;
    private final WABusinessAccountService waBusinessAccountService;

    public WATemplateList() {
        waBusinessAccountManager = WABusinessAccountManager.getInstance();
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
        final JsonObject requestData = dropdownRequest.data();
        final String searchText = (String) dropdownRequest.getCriteria().getValue();
        final Optional<Long> optionalBusinessAccountId = waBusinessAccountManager.getBusinessAccountId(orgId, requestData.getLong(MessengerFields.BUSINESS_PHONE_NUMBER_ID));

        if (optionalBusinessAccountId.isPresent()) {
            final Long businessAccountId = optionalBusinessAccountId.get();

            crmResponse.convertIntoAsync();
            final EventCallback<Page<WATemplate>> callback = new EventCallback<>() {

                @Override
                public void onSuccess(final Page<WATemplate> templatePage) {
                    try {
                        final int totalRecords = templatePage.getNumber();
                        final Collection<WATemplate> templates = templatePage.getData();
                        final OptionContainer optionContainer = new OptionContainer(totalRecords);

                        for (final WATemplate waTemplate : templates) {
                            final JsonObject templateJson = Json.object();

                            templateJson.put(WAFields.ID, waTemplate.id());
                            templateJson.put(WAFields.NAME, waTemplate.getName());

                            optionContainer.addOption(templateJson);
                        }
                        crmResponse.put(FieldKeys.TOTAL_RECORDS.getName(), totalRecords);
                        crmResponse.put(FieldKeys.HAS_MORE.getName(), start + limit <= totalRecords);
                        crmResponse.put(FieldKeys.OPTIONS.getName(), optionContainer.getOptions());
                        crmResponse.put(FieldKeys.SIZE.getName(), optionContainer.size());
                    } catch (Throwable throwable) {
                        final String errorDetail = throwable.getMessage();

                        LoggerUtil.logError(LOGGER, throwable, "Whatsapp: Templates retrieved but error during conversion. OrgId: {}, Reason: {}", orgId, errorDetail);
                        setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.TEMPLATE_LIST_RETRIEVAL_FAILED, "i18n.whatsapp.templates.retrieval.failed"), errorDetail);
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(Throwable cause) {
                    final String errorDetail = cause.getMessage();

                    LoggerUtil.logError(LOGGER, cause, "Whatsapp: Failed to retrieve templates. OrgId: {}, Reason: {}", orgId, errorDetail);
                    setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.TEMPLATE_LIST_RETRIEVAL_FAILED, "i18n.whatsapp.templates.retrieval.failed"), errorDetail);
                    crmResponse.send();
                }
            };

            waBusinessAccountService.fetchTemplates(businessAccountId, start, limit, searchText, callback);
        } else {
            LOGGER.error("Whatsapp: Business account not found. OrgId: {}", orgId);
            throw ErrorCode.get(WAErrorCodes.FAILED_TO_FETCH_WHATSAPP_BUSINESS_ACCOUNT, "i18n.whatsapp.business.account.retrieval.failed");
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
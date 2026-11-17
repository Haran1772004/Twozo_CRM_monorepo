package com.tlc.crm.notifications.action.call;

import com.tlc.commons.data.DataProcessorProvider;
import com.tlc.commons.data.model.RequestData;
import com.tlc.commons.data.processor.DataProcessor;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.StringUtil;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.pub.CrmPublicAction;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.notifications.fields.NotificationFields;
import com.tlc.crm.notifications.handler.CallStatusHandler;
import com.tlc.notification.model.telephone.TelephoneProvider;
import com.tlc.notification.webhook.model.CallStatusWebhookData;
import com.tlc.web.WebAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.Map;
import java.util.Objects;

/**
 * Entry point for processing call status webhook callbacks from telephony providers.
 * Delegates to {@link CallStatusHandler} for handling web-based calls and click-to-call scenarios.
 *
 * @author Sathish
 * @version 1.0
 */
@WebAction(path = "/{provider}/call/status/mgmt")
public class CallStatusManagement extends CrmPublicAction implements SecureUserTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallStatusManagement.class);

    /**
     * Processes incoming call status callback and routes to the appropriate handler.
     *
     * @param request     the incoming request containing call status updates
     * @param crmResponse the response object for error reporting
     */
    @Override
    public void pProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final String providerName = request.getParameter(NotificationFields.PROVIDER.getName());

        if (Objects.nonNull(providerName)) {
            final String webhookProcessorName = StringUtil.join(StringUtil.Delimiter.UNDER_SCORE, providerName,
                    NotificationFields.CALL_STATUS_MONITOR.getName());

            final DataProcessor<CallStatusWebhookData> webhookProcessor = DataProcessorProvider.getProcessor(webhookProcessorName);
            final JsonObject requestJson = request.getRequestJson();
            final Map<String, Deque<String>> params = request.getParams();
            final RequestData<JsonObject> webhookRequest = new RequestData<>(requestJson, params);

            LOGGER.info("Webhook Request Data: {}, Params: {}", requestJson, params);

            try {
                webhookProcessor.process(webhookRequest, webhookData -> {
                    final CallStatusHandler callStatusHandler = CallStatusHandler.get(TelephoneProvider.get(providerName));

                    if (webhookData.isClickToCall()) {
                        callStatusHandler.handleClickToCall(webhookData, crmResponse);
                    } else {
                        callStatusHandler.handleWebCall(webhookData, crmResponse);
                    }
                });
            } catch (final Exception exception) {
                LoggerUtil.logError(LOGGER, exception, "Call Status: Error occurred while processing {} call status webhook. Reason: {}",
                        providerName, exception.getMessage());
                exception.printStackTrace();
            }
        }
    }
}

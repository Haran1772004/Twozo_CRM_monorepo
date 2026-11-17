package com.tlc.crm.core.internal.event.email;

import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.core.action.parser.DataParser;
import com.tlc.crm.core.action.parser.impl.NativeParser;
import com.tlc.crm.core.notification.email.alert.processor.EmailAlertProcessor;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * <p>
 * Listens the email alert events and processes the email alert data.
 * </p>
 *
 * @author Mohammed Asif J
 * @version 1.0
 */
public class EmailAlertEventListener implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailAlertEventListener.class);

    private final DataParser dataParser;
    private final Decoder decoder;

    public EmailAlertEventListener() {
        this.dataParser = NativeParser.getInstance();
        this.decoder = Base64.getDecoder();
    }

    /**
     * <p>
     * Enables to process the {@link StreamMessage} data.
     * </p>
     *
     * @param streamMessage Represents {@link StreamMessage}.
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final Long[] ids = extractIdsFromMessageId(streamMessage.id());

        if (ids.length != NumericalConstant.THREE) {
            LOGGER.error("Email Alert Listener: Invalid stream message Id, skipping processing. Message Id: {}", streamMessage.id());
            return;
        }
        final Long orgId = ids[NumericalConstant.ZERO];
        final Long userId = ids[NumericalConstant.ONE];
        final Long modelId = ids[NumericalConstant.TWO];
        final Map<String, Object> data = (Map<String, Object>) dataParser.deserialize(streamMessage.data());
        final String template = (String) data.get(ActionFields.TEMPLATE);

        if (Objects.nonNull(template)) {
            final EmailNotificationTemplate emailTemplate = EmailNotificationTemplate.getByName(template);
            final EmailAlertProcessor emailAlertProcessor = EmailAlertProcessor.get(emailTemplate);

            if (Objects.nonNull(emailAlertProcessor)) {
                emailAlertProcessor.process(orgId, userId, emailTemplate, List.of(modelId));
            }
            LOGGER.info("Email Alert Listener: Email alert event data processed, OrgId: {}, User Id: {}, Template: {}, ID:{}", orgId, userId, template, modelId);
        } else {
            LOGGER.info("Email Alert Listener: Template is null, skipping the email alert event processing, OrgId: {}, User Id: {}, ID:{}", orgId, userId, modelId);
        }
    }

    /**
     * <p>
     * Extracts orgId, userId and modelId from the Base64 encoded stream message id.
     * </p>
     *
     * @param encodedMessageId Represents encoded id in org:user:model format.
     * @return Array with orgId, userId and modelId in order.
     */
    private Long[] extractIdsFromMessageId(final String encodedMessageId) {
        final String[] idParts = new String(decoder.decode(encodedMessageId), UTF_8)
                .split(StringUtil.Delimiter.COLON.getDelimiter(), NumericalConstant.THREE);

        return new Long[] {
                Long.parseLong(idParts[NumericalConstant.ZERO]),
                Long.parseLong(idParts[NumericalConstant.ONE]),
                Long.parseLong(idParts[NumericalConstant.TWO])
        };
    }
}
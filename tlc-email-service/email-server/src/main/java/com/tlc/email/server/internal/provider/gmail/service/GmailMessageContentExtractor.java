package com.tlc.email.server.internal.provider.gmail.service;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.tlc.notification.fields.NotificationKeys;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * A service class responsible for parsing Gmail message payloads,
 * including extracting headers and decoding the message body.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class GmailMessageContentExtractor {

    private static class Instance {
        private static final GmailMessageContentExtractor INSTANCE = new GmailMessageContentExtractor();
    }

    private GmailMessageContentExtractor() {
    }

    /**
     * <p>
     * Gets the {@link GmailMessageContentExtractor} Instance
     * </p>
     *
     * @return {@link GmailMessageContentExtractor}
     */
    public static GmailMessageContentExtractor getInstance() {
        return GmailMessageContentExtractor.Instance.INSTANCE;
    }

    /**
     * <p>
     * Extracts headers from the given {@link Message} instance.
     * </p>
     *
     * @param message the {@link Message} object from which headers are extracted
     * @return a {@link Map} containing header names as keys and their corresponding values
     */
    public Map<String, String> extractHeaders(final Message message) {
        final Map<String, String> headers = new HashMap<>();
        final MessagePart messagePart = message.getPayload();
        final Optional<List<MessagePartHeader>> optionalMessagePartHeader = Objects.isNull(messagePart) ? Optional.empty()
                : Optional.ofNullable(messagePart.getHeaders());

        if (optionalMessagePartHeader.isPresent()) {

            for (final MessagePartHeader header : optionalMessagePartHeader.get()) {
                headers.put(header.getName(), header.getValue());
            }
        }

        return headers;
    }

    /**
     * <p>
     * Extracts the complete decoded message body as a string from the given {@link Message}.
     * </p>
     *
     * @param message The {@link Message} object containing the payload to extract
     * @return The decoded message body as a {@link String}; empty string if no payload found
     */
    public String extractMessageBody(final Message message) {
        final StringBuilder body = new StringBuilder();
        final MessagePart messagePart = message.getPayload();
        final String htmlTextKey = NotificationKeys.HTML_CONTENT_TYPE.getKey();

        if (Objects.nonNull(messagePart)) {
            if (htmlTextKey.equalsIgnoreCase(messagePart.getMimeType())) {
                final MessagePartBody messagePartBody = messagePart.getBody();

                Optional.ofNullable(messagePartBody)
                        .map(MessagePartBody::getData)
                        .map(data -> new String(Base64.getUrlDecoder().decode(data), StandardCharsets.UTF_8))
                        .ifPresent(body::append);

                return body.toString();
            }
            extractBodyFromPart(messagePart, body, htmlTextKey);

            if (body.isEmpty()) {
                extractBodyFromPart(messagePart, body, NotificationKeys.PLAIN_TEXT.getKey());
            }
        }

        return body.toString();
    }

    /**
     * <p>
     * Recursively extracts and decodes the body content from the given {@link MessagePart} and appends it to the provided {@link StringBuilder}.
     * </p>
     *
     * @param part    The {@link MessagePart} from which to extract the body content
     * @param builder The {@link StringBuilder} to append the decoded body content to
     */
    private void extractBodyFromPart(final MessagePart part, final StringBuilder builder, final String mimeType) {
        if (Objects.isNull(part)) {
            return;
        }

        Optional.ofNullable(part.getMimeType())
                .filter(type -> type.equalsIgnoreCase(mimeType))
                .flatMap(type -> Optional.ofNullable(part.getBody()))
                .map(MessagePartBody::getData)
                .map(Base64.getUrlDecoder()::decode)
                .map(String::new)
                .ifPresent(builder::append);

        Optional.ofNullable(part.getParts())
                .filter(parts -> !parts.isEmpty())
                .ifPresent(parts -> parts.forEach(subPart -> extractBodyFromPart(subPart, builder, mimeType)));
    }
}
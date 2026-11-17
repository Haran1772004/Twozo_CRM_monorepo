package com.tlc.email.common.codec;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.email.common.model.AbstractEmailEvent;
import com.tlc.email.common.model.EmailCreateEvent;
import com.tlc.email.common.model.EmailDeleteEvent;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.ThreadHolder;
import com.tlc.email.common.model.EmailEvent.Event;
import com.tlc.email.common.model.EmailUpdateEvent;
import com.tlc.email.common.request.EmailEventRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <p>
 * Codec implementation for encoding and decoding {@link EmailEventRequest} instances
 * to and from byte arrays using JSON format.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 * @see EmailEventRequest
 * @see EmailCreateEvent
 * @see EmailUpdateEvent
 * @see EmailDeleteEvent
 * @see ThreadCodec
 */
public class EmailEventRequestCodec extends AbstractEmailCodec<EmailEventRequest> {

    private final ThreadCodec threadCodec;

    private EmailEventRequestCodec() {
        this.threadCodec = ThreadCodec.getInstance();
    }

    private static class Instance {
        private static final EmailEventRequestCodec INSTANCE = new EmailEventRequestCodec();
    }

    public static EmailEventRequestCodec getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Encodes the provided {@link EmailEventRequest} into a JSON-based byte array.
     * </p>
     *
     * @param emailEventRequest The request to encode.
     * @return The encoded byte array.
     */
    @Override
    public byte[] encode(final EmailEventRequest emailEventRequest) {
        final JsonObject jsonObject = Json.object();

        final AbstractEmailEvent createEvent = emailEventRequest.getCreateEvent();
        final AbstractEmailEvent updateEvent = emailEventRequest.getUpdateEvent();
        final EmailDeleteEvent deleteEvent = emailEventRequest.getDeleteEvent();

        if (Objects.nonNull(createEvent)) {
            loadWebhookEvent(jsonObject, createEvent);
        }

        if (Objects.nonNull(updateEvent)) {
            loadWebhookEvent(jsonObject, updateEvent);
        }

        if (Objects.nonNull(deleteEvent)) {
            loadDeleteEvent(jsonObject, deleteEvent);
        }

        return jsonObject.getBytes();
    }

    /**
     * <p>
     * Decodes the given byte array into a {@link EmailEventRequest} instance.
     * </p>
     *
     * @param data The encoded byte array.
     * @return The decoded {@link EmailEventRequest}.
     */
    @Override
    public EmailEventRequest decode(final byte[] data) {
        final JsonObject jsonObject = Json.object(data);
        final EmailEventRequest emailEventRequest = new EmailEventRequest();

        decodeThreadEvent(
                jsonObject,
                Event.CREATE.getName(),
                EmailCreateEvent::new,
                emailEventRequest::setCreateEvent
        );

        decodeThreadEvent(
                jsonObject,
                Event.UPDATE.getName(),
                EmailUpdateEvent::new,
                emailEventRequest::setUpdateEvent
        );

        Optional.ofNullable(jsonObject.optJsonArray(Event.DELETE.getName()))
                .map(this::extractIds)
                .map(EmailDeleteEvent::new)
                .ifPresent(emailEventRequest::setDeleteEvent);

        return emailEventRequest;
    }

    /**
     * <p>
     * Loads thread data for create/update events into the JSON object.
     * </p>
     *
     * @param jsonObject The JSON object to populate.
     * @param event      The event containing thread data.
     */
    private void loadWebhookEvent(final JsonObject jsonObject, final AbstractEmailEvent event) {
        final Event type = event.getEvent();
        final ThreadHolder threadHolder = event.getThreadHolder();

        if (Objects.nonNull(type) && Objects.nonNull(threadHolder)) {
            final Collection<Thread> threads = threadHolder.getThreads();

            if (Objects.nonNull(threads) && !threads.isEmpty()) {
                jsonObject.put(type.getName(), Json.object(threadCodec.encode(threadHolder)));
            }
        }
    }

    /**
     * <p>
     * Loads delete event ID list into the JSON object.
     * </p>
     *
     * @param jsonObject The JSON object to populate.
     * @param event      The delete event containing IDs.
     */
    private void loadDeleteEvent(final JsonObject jsonObject, final EmailDeleteEvent event) {
        final Event type = event.getEvent();
        final Collection<Long> ids = event.ids();

        if (Objects.nonNull(ids) && !ids.isEmpty()) {
            jsonObject.put(type.getName(), getIds(ids));
        }
    }

    /**
     * <p>
     * Converts a collection of IDs into a {@link JsonArray}.
     * </p>
     *
     * @param ids The collection of IDs.
     * @return A JSON array of IDs.
     */
    private JsonArray getIds(final Collection<Long> ids) {
        final JsonArray array = Json.array();

        for (final Long id : ids) {
            array.put(id);
        }
        return array;
    }

    /**
     * <p>
     * Decodes a {@link ThreadHolder} from a {@link JsonObject} using the specified event key.
     * </p>
     *
     * @param <T>          The type of the event to construct.
     * @param jsonObject   The source JSON object.
     * @param eventKey     The key identifying the event in JSON.
     * @param eventBuilder Function to convert a decoded ThreadHolder to an event.
     * @param setter       Consumer to assign the event to a builder or target.
     */
    private <T> void decodeThreadEvent(final JsonObject jsonObject,
                                       final String eventKey,
                                       final Function<ThreadHolder, T> eventBuilder,
                                       final Consumer<T> setter) {
        Optional.ofNullable(jsonObject.optJsonObject(eventKey))
                .map(json -> threadCodec.decode(json.getBytes()))
                .map(eventBuilder)
                .ifPresent(setter);
    }

    /**
     * <p>
     * Extracts a collection of long IDs from a {@link JsonArray}.
     * </p>
     *
     * @param jsonArray The source JSON array.
     * @return A collection of extracted long IDs.
     */
    private Collection<Long> extractIds(final JsonArray jsonArray) {
        final Collection<Long> ids = new ArrayList<>();

        for (int index = 0; index < jsonArray.size(); index++) {
            ids.add(jsonArray.getLong(index));
        }
        return ids;
    }
}
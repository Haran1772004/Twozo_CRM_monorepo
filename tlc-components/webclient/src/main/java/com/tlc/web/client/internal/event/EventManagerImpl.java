package com.tlc.web.client.internal.event;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.executor.context.ContextUtil;
import com.tlc.commons.executor.context.ContextUtil.CapturedContext;

import com.tlc.commons.executor.task.RunnableTask;
import com.tlc.web.client.event.EventListener;
import com.tlc.web.client.event.*;
import com.tlc.web.client.internal.status.WebClientErrorCodes;
import com.tlc.web.client.service.provider.WebClientServiceProvider;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * A manager that manages the publishing and delivering of events
 * and also handles the registration and un-registration of event consumers
 *
 * @author Selvakumar
 * @version 1.0
 */
public class EventManagerImpl implements EventNotifier, EventConsumerRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventManagerImpl.class);

    private final Map<String, List<MessageConsumer>> consumers;

    private static class Instance {
        private static final EventManagerImpl INSTANCE = new EventManagerImpl();
    }

    public static EventManagerImpl getInstance() {
        return EventManagerImpl.Instance.INSTANCE;
    }

    private EventManagerImpl() {
        this.consumers = new HashMap<>();
    }

    /**
     * Resolves the live Vert.x instance so in-JVM cluster reboot can replace the underlying runtime.
     */
    private Vertx vertx() {
        return WebClientServiceProvider.get().getVertx();
    }

    /**
     * Resolves the live Event Bus bound to the current Vert.x instance.
     */
    private EventBus eventBus() {
        return vertx().eventBus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void send(final Event<T> event) {
        this.eventBus().send(event.name(), event.data());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, R> void send(Event<T> event, EventCallback<R> callback) {
        final CapturedContext capturedContext = ContextUtil.captureContext();
        final Consumer<Consumer<Void>> resultPublisher = callbackResult -> ContextUtil.runWithContext(capturedContext, () -> callbackResult.accept(null));
        final String name = event.name();
        final io.vertx.core.Context callerContext = Vertx.currentContext();

        LOGGER.debug("Vertx event flow: sending event {} through {}", name, Thread.currentThread().getName());
        this.eventBus().request(name, event.data()).onSuccess(replyMessage -> {
            LOGGER.debug("Vertx event flow: response received for {} through {}", name, Thread.currentThread().getName());
            final RunnableTask task = () -> {
                LOGGER.debug("Vertx event flow: response processing for {} through {}", name, Thread.currentThread().getName());
                final R result = Objects.isNull(replyMessage) ? null : (R) replyMessage.body();
                resultPublisher.accept(unused -> callback.onSuccess(result));
            };
            dispatchOnCallerContext(callerContext, task);
        }).onFailure(throwable -> {
            final Runnable task = () -> resultPublisher.accept(unused -> callback.onFailure(throwable));
            dispatchOnCallerContext(callerContext, task);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void publish(final Event<T> event) {
        this.eventBus().publish(event.name(), event.data());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(final String group, final Map<EventListener, EventConsumer> consumers) {

        if (!consumers.isEmpty()) {

            final List<MessageConsumer> existingConsumers = this.consumers.remove(group);
            if (Objects.nonNull(existingConsumers)) {
                LOGGER.warn("Replacing existing event consumers for group {} during re-registration", group);
            }

            final List<MessageConsumer> messageConsumers = new ArrayList<>();

            consumers.forEach((metaData, consumer) -> {
                messageConsumers.add(receive(metaData.name(), consumer));
            });

            this.consumers.put(group, messageConsumers);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void unregister(final String group) {
        final List<MessageConsumer> registeredConsumers = this.consumers.remove(group);

        if (Objects.nonNull(registeredConsumers)) {
            final CountDownLatch countDownLatch = new CountDownLatch(registeredConsumers.size());

            for (final MessageConsumer registeredConsumer : registeredConsumers) {
                final MessageConsumer<T> consumer = (MessageConsumer<T>) registeredConsumer;

                consumer.unregister().onComplete(unregisterHandler -> {
                    if (unregisterHandler.succeeded()) {
                        LOGGER.info("Event consumer unregistered successfully, Consumer : {}", registeredConsumer.address());
                    } else {
                        LOGGER.error("Event consumer unregistration failed, Consumer : {}, Error : {}", registeredConsumer.address(), unregisterHandler.cause().getMessage());
                    }
                    countDownLatch.countDown();
                });
            }

            try {
                countDownLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception e) {
                LOGGER.error("Error occurred while waiting for the event handler un-registration to propagate across the cluster. Error : {}", e.getMessage());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterAll() {
        final List<String> groups = new ArrayList<>(consumers.keySet());

        for (final String group : groups) {
            unregister(group);
        }
    }

    /**
     * Drops local consumer bookkeeping without waiting on the Event Bus.
     * Used when Vert.x is already leaving/closed and unregister propagation is unreliable.
     */
    @Override
    public void discardAllConsumers() {
        consumers.clear();
    }


    /**
     * Registers a consumer for the specified event name
     *
     * @param eventName Name of the event that has to be listened
     * @param consumer  Event consumer
     * @param <T>
     * @return Message Consumer
     */
    private <T> MessageConsumer<T> receive(final String eventName, final EventConsumer<T> consumer) {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final MessageConsumer<T> messageConsumer = this.eventBus().consumer(eventName, data -> {
            if ("true".equals(data.headers().get("testEvent"))) {
                LOGGER.info("Test message received for event {}", eventName);
            } else {
                final T t = (T) data.body();

                final Consumer<Throwable> failureHandler = throwable -> {
                    data.fail(0, throwable.getMessage());
                    LOGGER.error("Error occurred while consuming the event {}. Error: {}", eventName, throwable.getMessage(), throwable);
                };

                vertx().executeBlocking(() -> {

                            try {
                                LOGGER.debug("Vertx event flow: response consuming for {} through {}", eventName, Thread.currentThread().getName());
                                return consumer.consume(t);
                            } catch (Exception e) {
                                throw ErrorCode.get(ErrorCodes.UNKNOWN_ERROR, e.getMessage(), e);
                            }
                        })
                        .onSuccess(response -> {
                            LOGGER.debug("Vertx event flow: response sending for {} through {}", eventName, Thread.currentThread().getName());
                            data.reply(response);
                        })
                        .onFailure(failureHandler::accept);
            }
        });

        messageConsumer.completionHandler(completionHandler -> {

            if (completionHandler.succeeded()) {
                LOGGER.info("Event handler registration successfully propagated across the cluster. Event : {}", eventName);
            } else {
                LOGGER.error("Failed to propagate event handler registration across the cluster. Event : {}", eventName);
            }

            countDownLatch.countDown();
        });

        try {
            countDownLatch.await();
        } catch (Exception e) {
            LOGGER.error("Error occurred while waiting for the event handler registration to propagate across the cluster. Event : {}, Error : {}", eventName, e.getMessage());
        }

        sendTestEvent(eventName);

        return messageConsumer;
    }

    /**
     * publishes a test event
     *
     * @param eventName
     */
    private void sendTestEvent(String eventName) {
        this.eventBus().send(eventName, null, new DeliveryOptions().addHeader("testEvent", "true"));
    }

    /**
     * Dispatches the given task back on the caller's Vert.x context.
     * <p>
     * If the caller had a Vert.x context, the task is executed as a blocking operation
     * on that context (to avoid blocking the event loop while preserving the context).
     * If no Vert.x context was available (caller was a non-Vert.x thread), the task
     * is executed on the default worker pool via {@code vertx.executeBlocking}.
     * </p>
     *
     * @param callerContext The Vert.x context captured at the time of the initial request (may be null)
     * @param task          The task to execute
     */
    private void dispatchOnCallerContext(final io.vertx.core.Context callerContext, final Runnable task) {
        if (Objects.nonNull(callerContext)) {
            callerContext.executeBlocking(() -> {
                task.run();
                return null;
            }, false);
        } else {
            vertx().executeBlocking(() -> {
                task.run();
                return null;
            }, false);
        }
    }

    /**
     * An interceptor that intercepts the incoming or outgoing events of event bus
     */
    private void intercept() {
        //TODO handle interceptor using a wrapper
        eventBus().addOutboundInterceptor((interceptor) -> {
            final Message<Object> object = interceptor.message();

            System.out.println("================ OUTGOING ================================ ");

            //Describes whether the message was sent by using send or publish method
            System.out.println(" Is Send " + object.isSend());
            System.out.println(" outbound data " + object.body());
            System.out.println("================ OUTGOING ================================ ");

            interceptor.next(); // Process next
        });

        eventBus().addInboundInterceptor(interceptor -> {
            final Message<Object> message = interceptor.message();

            System.out.println("================ INCOMING ================================ ");
            System.out.println(" Message " + message.isSend());
            System.out.println("Incoming data: " + message.body());
            System.out.println("================ INCOMING ================================ ");

            interceptor.next();//Process next
        });
    }
}

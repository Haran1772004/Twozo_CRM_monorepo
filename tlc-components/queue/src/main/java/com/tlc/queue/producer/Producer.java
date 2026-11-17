package com.tlc.queue.producer;

import com.tlc.queue.consumer.StreamMessage;

/**
 * @author Abishek
 * @version 1.0
 */
public interface Producer<E> extends AutoCloseable
{
	void send(final StreamMessage<E> streamMessage);

	void sendWithoutContext(final StreamMessage<E> streamMessage);

	void close();
}

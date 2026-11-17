package com.tlc.web.client.event;

import com.tlc.commons.listeners.Callback;

/**
 * A callback that will be called after the event is sent to the receiver
 *
 * @param <R> Response type of event.<b>Note : </b>
 *            <span style="color:red">For deserialization purpose, a codec has to be registered this field type,
 *            except primitive types, byte array and string data types.
 *            If the codec is not registered, the success callback will never be called</span>
 * @author Selvakumar
 * @version 1.0
 */
public abstract class EventCallback<R> implements Callback<R> {

    /**
     * This will be called upon the successful completion of the event consume operation
     *
     * @param response Response from the consumer
     */
    public abstract void onSuccess(R response);

    /**
     * This will be called upon the failure of the event consume operation
     *
     * @param cause Cause of the failure
     */
    public abstract void onFailure(final Throwable cause);

}

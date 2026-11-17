package com.tlc.email.common.model.exchange;

import com.tlc.email.common.model.StateData;

/**
 * <p>
 * Serves as an abstract base class for handling both email sending and receiving operations. And provides a common interface
 * and shared functionality that can be extended by both sender and receiver implementations.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public abstract class EmailData {

    protected StateData stateData;

    protected EmailData() {
    }

    public void setStateData(final StateData stateData) {
        this.stateData = stateData;
    }

    public StateData getStateData() {
        return stateData;
    }
}

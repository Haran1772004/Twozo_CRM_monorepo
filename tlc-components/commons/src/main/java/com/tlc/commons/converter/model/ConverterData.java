package com.tlc.commons.converter.model;

import java.util.Locale;

/**
 * Holds the configuration that are used to convert the models.
 *
 * @author Sathish
 * @version 1.0
 */
public class ConverterData<T> {

    private final Long orgId;
    private final Long ownerId;
    private final Locale locale;
    private final boolean isUpdateAction;
    private final boolean isPartialData;
    private final T dataToBeConverted;

    public ConverterData(final Long orgId, final Long ownerId, final Locale locale, final T dataToBeConverted) {
        this(orgId, ownerId, locale, false, false, dataToBeConverted);
    }

    public ConverterData(final Long orgId, final Long ownerId, final Locale locale, final boolean isUpdateAction,
                         final boolean isPartialData, final T dataToBeConverted) {
        this.orgId = orgId;
        this.ownerId = ownerId;
        this.locale = locale;
        this.isUpdateAction = isUpdateAction;
        this.isPartialData = isPartialData;
        this.dataToBeConverted = dataToBeConverted;
    }

    public Long orgId() {
        return orgId;
    }

    public Long ownerId() {
        return ownerId;
    }

    public Locale locale() {
        return locale;
    }

    public boolean isPartialData() {
        return isPartialData;
    }

    public boolean isUpdateAction() {
        return isUpdateAction;
    }

    public T dataToBeConverted() {
        return dataToBeConverted;
    }
}

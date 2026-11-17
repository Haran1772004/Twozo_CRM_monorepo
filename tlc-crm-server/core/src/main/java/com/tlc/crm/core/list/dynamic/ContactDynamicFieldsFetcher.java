package com.tlc.crm.core.list.dynamic;

import com.tlc.crm.entity.list.dynamic.model.DynamicFieldRequest;
import com.tlc.crm.entity.list.dynamic.model.DynamicFieldsData;

/**
 * <p>
 * Implements {@link DynamicFieldsFetcher} the for fetching dynamic field data to contact list
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class ContactDynamicFieldsFetcher extends DynamicFieldsFetcher {

    private final WebFormFieldsFetcher webFormFieldsFetcher;
    private final ActivityDynamicFieldsFetcher activityDynamicFieldsFetcher;
    private final DealStatisticsFieldsFetcher dealStatisticsFieldsFetcher;
    private final EmailFieldsFetcher emailFieldsFetcher;

    private ContactDynamicFieldsFetcher() {
        webFormFieldsFetcher = WebFormFieldsFetcher.getInstance();
        activityDynamicFieldsFetcher = ActivityDynamicFieldsFetcher.getInstance();
        dealStatisticsFieldsFetcher = DealStatisticsFieldsFetcher.getInstance();
        emailFieldsFetcher = EmailFieldsFetcher.getInstance();
    }

    private static class InstanceHolder {
        private static final ContactDynamicFieldsFetcher INSTANCE = new ContactDynamicFieldsFetcher();
    }

    /**
     * <p>
     * Gets the {@link ContactDynamicFieldsFetcher} instance
     * </p>
     *
     * @return The {@link ContactDynamicFieldsFetcher}
     */
    public static ContactDynamicFieldsFetcher getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param dynamicFieldRequest Represents the {@link DynamicFieldRequest}
     * @return The {@link DynamicFieldsData}
     */
    @Override
    public DynamicFieldsData get(final DynamicFieldRequest dynamicFieldRequest) {
        final DynamicFieldsData dynamicFieldsData = new DynamicFieldsData();
        final DynamicFieldsData webFormDynamicFieldsData = webFormFieldsFetcher.get(dynamicFieldRequest);
        final DynamicFieldsData activityDynamicFieldsData = activityDynamicFieldsFetcher.get(dynamicFieldRequest);
        final DynamicFieldsData dealStatisticsDynamicFieldsData = dealStatisticsFieldsFetcher.get(dynamicFieldRequest);
        final DynamicFieldsData emailDynamicFieldsData = emailFieldsFetcher.get(dynamicFieldRequest);

        dynamicFieldsData.append(webFormDynamicFieldsData);
        dynamicFieldsData.append(activityDynamicFieldsData);
        dynamicFieldsData.append(dealStatisticsDynamicFieldsData);
        dynamicFieldsData.append(emailDynamicFieldsData);

        return dynamicFieldsData;
    }
}

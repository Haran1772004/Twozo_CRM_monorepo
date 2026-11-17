package com.tlc.crm.core.list.dynamic;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.list.dynamic.model.ListDynamicFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.list.dynamic.model.DynamicFieldRequest;
import com.tlc.crm.entity.list.dynamic.model.DynamicFieldsData;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Implements {@link DynamicFieldsFetcher} the for fetching dynamic field data to web form fields
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class WebFormFieldsFetcher extends DynamicFieldsFetcher {

    private final ContactManager contactManager;
    private final DealManager dealManager;

    private WebFormFieldsFetcher() {
        contactManager = ContactManager.getInstance();
        dealManager = DealManager.getInstance();
    }

    private static class InstanceHolder {
        private static final WebFormFieldsFetcher INSTANCE = new WebFormFieldsFetcher();
    }

    /**
     * <p>
     * Gets the {@link WebFormFieldsFetcher} instance
     * </p>
     *
     * @return The {@link WebFormFieldsFetcher}
     */
    public static WebFormFieldsFetcher getInstance() {
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
        final long orgId = dynamicFieldRequest.orgId();
        final Source source = dynamicFieldRequest.provider().getSource();
        final Collection<String> dynamicFields = dynamicFieldRequest.dynamicFields();
        final Collection<Long> entityIds = dynamicFieldRequest.entityIds();
        final boolean webFormNameRequired;
        final Map<Long, String> webFormNames;

        switch (source) {
            case CONTACT -> {
                webFormNameRequired = dynamicFields.stream().anyMatch(ListDynamicFields.WEB_FORMS::equals);
                webFormNames = webFormNameRequired ? contactManager.getWebFormNames(orgId, entityIds) : Collections.emptyMap();
            }
            case DEAL -> {
                webFormNameRequired = dynamicFields.stream().anyMatch(ListDynamicFields.WEB_FORM_NAME::equals);
                webFormNames = webFormNameRequired ? dealManager.getWebFormNames(orgId, entityIds) : Collections.emptyMap();
            }
            default -> {
                webFormNameRequired = false;
                webFormNames = Collections.emptyMap();
            }
        }

        if (webFormNameRequired && !webFormNames.isEmpty()) {
            final Map<String, Map<String, Object>> entityRows = new HashMap<>();

            entityIds.forEach(id -> {

                if (webFormNames.containsKey(id)) {
                    final Map<String, Object> entityRow = new HashMap<>();
                    final String webFormName = webFormNames.get(id);

                    switch (source) {
                        case DEAL -> entityRow.put(ListDynamicFields.WEB_FORM_NAME, webFormName);
                        case CONTACT -> entityRow.put(ListDynamicFields.WEB_FORMS, webFormName);
                    }
                    entityRows.put(id.toString(), entityRow);
                }
            });
            dynamicFieldsData.addAll(entityRows);
        }
        return dynamicFieldsData;
    }
}
package com.tlc.crm.workflow.internal.trigger.action.formatter;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Aggregates the data of a company with the existing data
 *
 * @author Maharaja S
 * @version 1.0
 */
public final class CompanyJsonAggregator extends EntityJsonAggregator {

    private static final class InstanceHolder {
        private static final CompanyJsonAggregator INSTANCE = new CompanyJsonAggregator();
    }

    /**
     * Gets the instance of {@link CompanyJsonAggregator}.
     *
     * @return {@link CompanyJsonAggregator} instance.
     */
    public static CompanyJsonAggregator getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents the organization id
     * @param companyJson Represents the json data of a company
     */
    @Override
    public void merge(final long orgId, final JsonObject companyJson) {
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(orgId, ActionAttributes.COMPANY);
        final String tagsFieldKey = fields.get(ActionAttributes.TAGS).idAsString();

        if (companyJson.containsKey(ActionAttributes.ID)) {
            final Long companyId = companyJson.getLong(ActionAttributes.ID);

            if (companyJson.containsKey(tagsFieldKey)) {
                final Collection<Long> mappedTagIds = sourceTagManager.getMappedTagIds(orgId, ActionAttributes.COMPANY, sourceTagManager.getCompanyTagTable(), List.of(companyId));

                if (!mappedTagIds.isEmpty()) {
                    mergeTags(companyJson, mappedTagIds, tagsFieldKey);
                }
            }
        }
    }
}
package com.tlc.crm.workflow.internal.trigger.action.formatter;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Aggregates the data of a deal with the existing data
 *
 * @author Maharaja S
 * @version 1.0
 */
public final class DealJsonAggregator extends EntityJsonAggregator {

    private static final class InstanceHolder {
        private static final DealJsonAggregator INSTANCE = new DealJsonAggregator();
    }

    /**
     * Gets the instance of {@link DealJsonAggregator}.
     *
     * @return {@link DealJsonAggregator} instance.
     */
    public static DealJsonAggregator getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization id
     * @param dealJson Represents the json data of a deal
     */
    @Override
    public void merge(final long orgId, final JsonObject dealJson) {
        final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(orgId, ActionAttributes.DEAL);
        final String tagsFieldKey = fields.get(ActionAttributes.TAGS).idAsString();

        if (dealJson.containsKey(ActionAttributes.ID)) {
            final Long dealId = dealJson.getLong(ActionAttributes.ID);

            if (dealJson.containsKey(tagsFieldKey)) {
                final Collection<Long> mappedTagIds = sourceTagManager.getMappedTagIds(orgId, ActionAttributes.DEAL, sourceTagManager.getDealTagTable(), List.of(dealId));

                if (!mappedTagIds.isEmpty()) {
                    mergeTags(dealJson, mappedTagIds, tagsFieldKey);
                }
            }
        }
    }
}
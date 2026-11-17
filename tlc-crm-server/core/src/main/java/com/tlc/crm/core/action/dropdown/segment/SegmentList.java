package com.tlc.crm.core.action.dropdown.segment;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.segment.fields.SegmentFields;
import com.tlc.crm.core.segment.manager.SegmentContactManager;
import com.tlc.crm.core.segment.models.Segment;
import com.tlc.crm.core.segment.service.SegmentService;
import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;
import com.tlc.crm.entity.fields.option.OptionKeys;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Manages {@link Segment} related dropdown actions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
@StaticDropdown(name = "segmentList")
public class SegmentList extends AbstractDropdownAction {

    private final SegmentService segmentService;
    private final SegmentContactManager segmentContactManager;

    public SegmentList() {
        segmentService = SegmentService.getInstance();
        segmentContactManager = SegmentContactManager.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final Long orgId = dropdownRequest.getOrgId();
        final DropdownCriteria dropdownCriteria = dropdownRequest.getCriteria();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final JsonObject requestData = dropdownRequest.data();
        final boolean includeContactCount = requestData.optBoolean(SegmentFields.INCLUDE_CONTACT_COUNT.getName(), false);
        final long totalRecords = segmentService.getDropdownCount(orgId, dropdownCriteria);
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<Segment> segments = segmentService.getDropdownList(orgId, dropdownLimit, dropdownCriteria);

            if (includeContactCount) {
                final Map<Long, Long> contactCountMap = segmentContactManager.getContactCountMap(orgId, segments.stream().map(Segment::id).toList());

                for (final Segment segment : segments) {
                    final JsonObject jsonObject = Json.object();
                    final Long segmentId = segment.id();

                    jsonObject.put(OptionKeys.VALUE, segmentId);
                    jsonObject.put(OptionKeys.NAME, segment.getName());
                    jsonObject.put(SegmentFields.CONTACT_COUNT.getName(), contactCountMap.getOrDefault(segmentId, 0L));

                    optionContainer.addOption(jsonObject);
                }
            } else {
                for (final Segment segment : segments) {
                    optionContainer.addOption(segment.id(), segment.getName());
                }
            }
        }

        return optionContainer;
    }
}
package com.tlc.attachment.storage.model;

import java.util.List;

/**
 * Response containing object summaries for a list request.
 *
 * @author sundar
 * @version 1.0
 */
public record ObjectListResponse(
        List<ObjectSummary> summaries,
        String nextContinuationToken,
        boolean truncated
) {
}

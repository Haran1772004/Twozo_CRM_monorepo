package com.tlc.crm.core.page;

import com.tlc.crm.common.page.MobilePageHandler;
import com.tlc.crm.entity.fields.source.Source;

/**
 * <p>
 * Represents as page handler {@link Source} for handling mobile context
 * </p>
 *
 * @param <T> the type of entity being handled by this page handler
 * @author Kavinila E
 * @version 1.0
 */
public abstract class SourceMobilePageHandler<T> extends MobilePageHandler<T> {

    private Long entityId;
    private Source source;

    public long getEntityId() {
        return entityId;
    }

    public void setEntityId(final Long entityId) {
        this.entityId = entityId;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(final Source source) {
        this.source = source;
    }
}
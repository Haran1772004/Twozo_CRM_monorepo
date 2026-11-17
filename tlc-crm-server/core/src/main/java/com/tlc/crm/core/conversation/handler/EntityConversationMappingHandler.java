package com.tlc.crm.core.conversation.handler;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.crm.core.sql.resource.OMECCOMPANYCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECCONTACTCONVERSATIONS;
import com.tlc.crm.core.sql.resource.OMECDEALCONVERSATIONS;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Table;

/**
 * <p>
 * A handler class for managing entity-to-conversation mappings.
 * </p>
 *
 * @author kishore J
 * @version 1.0
 */
public final class EntityConversationMappingHandler {

    private EntityConversationMappingHandler() {
    }

    private static class Instance {
        private static final EntityConversationMappingHandler INSTANCE = new EntityConversationMappingHandler();
    }

    /**
     * <p>
     * Gets the {@link EntityConversationMappingHandler} Instance
     * </p>
     *
     * @return {@link EntityConversationMappingHandler}
     */
    public static EntityConversationMappingHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Fetches the {@link SourceConversationMapping} for the given {@link Source}
     * </p>
     *
     * @param source Represents the {@link Source}
     * @return The {@link SourceConversationMapping}
     */
    public SourceConversationMapping getSourceConversationMapping(final Source source) {
        final Table sourceConversationTable;
        final Column entityConversationIdColumn;
        final Column entityIdColumn;

        switch (source) {
            case CONTACT -> {
                sourceConversationTable = Table.get(OMECCONTACTCONVERSATIONS.TABLE);

                entityIdColumn = sourceConversationTable.getColumn(OMECCONTACTCONVERSATIONS.CONTACT_ID);
                entityConversationIdColumn = sourceConversationTable.getColumn(OMECCONTACTCONVERSATIONS.ENTITY_CONVERSATION_ID);
            }
            case COMPANY -> {
                sourceConversationTable = Table.get(OMECCOMPANYCONVERSATIONS.TABLE);

                entityIdColumn = sourceConversationTable.getColumn(OMECCOMPANYCONVERSATIONS.COMPANY_ID);
                entityConversationIdColumn = sourceConversationTable.getColumn(OMECCOMPANYCONVERSATIONS.ENTITY_CONVERSATION_ID);
            }
            case DEAL -> {
                sourceConversationTable = Table.get(OMECDEALCONVERSATIONS.TABLE);

                entityIdColumn = sourceConversationTable.getColumn(OMECDEALCONVERSATIONS.DEAL_ID);
                entityConversationIdColumn = sourceConversationTable.getColumn(OMECDEALCONVERSATIONS.ENTITY_CONVERSATION_ID);
            }
            default -> throw ErrorCode.get(EmailErrorCodes.INVALID_SOURCE, "i18n.notifications.email.invalid.source");
        }
        return new SourceConversationMapping(sourceConversationTable, entityIdColumn, entityConversationIdColumn);
    }

    /**
     * <p>
     * Represents a mapping of source entity conversation with the associated table and columns
     * </p>
     *
     * @param sourceConversationTable    Represents the source conversation table
     * @param entityIdColumn             Represents an entity id column
     * @param entityConversationIdColumn Represents the entity conversation id column
     * @author Kishore J
     * @version 1.0
     */
    public record SourceConversationMapping(Table sourceConversationTable, Column entityIdColumn,
                                            Column entityConversationIdColumn) {
    }
}

package com.tlc.email.common.model.type;

import com.tlc.commons.code.ErrorCode;

import com.tlc.email.common.internal.status.EmailErrorCodes;

import java.util.Objects;

/**
 * <p>
 * A model that represents the draft email.
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class DraftEmail extends OutboundEmail {

    private DraftAction draftAction;

    /**
     * {@inheritDoc}
     *
     * @return true
     */
    @Override
    public boolean isDraft() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link MessageCategory}
     */
    @Override
    public MessageCategory getMessageCategory() {
        return MessageCategory.DRAFT;
    }

    public DraftAction getDraftAction() {
        return Objects.isNull(draftAction) ? DraftAction.CREATE : draftAction;
    }

    public void setDraftAction(final DraftAction draftAction) {
        this.draftAction = draftAction;
    }

    /**
     * <p>
     * To define draft action
     * </p>
     * <p>
     * {@link #CREATE}
     * {@link #UPDATE}
     * {@link #SEND}
     */
    public enum DraftAction {
        CREATE((byte) 1),
        UPDATE((byte) 2),
        SEND((byte) 3);

        private final byte id;

        DraftAction(final byte id) {
            this.id = id;
        }

        public byte getId() {
            return id;
        }

        /**
         * Gets the {@link DraftAction}.
         *
         * @param id Represents the draft action id
         * @return {@link DraftAction}
         */
        public static DraftAction get(final short id) {
            return switch (id) {
                case 1 -> CREATE;
                case 2 -> UPDATE;
                case 3 -> SEND;
                default -> throw ErrorCode.get(EmailErrorCodes.INVALID_DRAFT_ACTION);
            };
        }

        public boolean isCreate() {
            return Objects.equals(this, CREATE);
        }

        public boolean isUpdate() {
            return Objects.equals(this, UPDATE);
        }

        public boolean isSend() {
            return Objects.equals(this, SEND);
        }

        public boolean isCreateOrUpdate() {
            return Objects.equals(this, CREATE) || Objects.equals(this, UPDATE);
        }
    }
}
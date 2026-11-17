package com.tlc.email.common.codec;

/**
 * <p>
 * Provides the services for email related request codec.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface EmailCodec {

    /**
     * <p>
     * Gets the {@link EmailCodec} for the given {@link CodecType}.
     * </p>
     *
     * @param codecType Represents the type of codec
     * @return {@link EmailCodec}
     */
    static EmailCodec get(final CodecType codecType) {
        return switch (codecType) {
            case SEND -> SendRequestCodec.getInstance();
            case RECEIVE -> SyncRequestCodec.getInstance();
            case UPDATE -> UpdateRequestCodec.getInstance();
            case DELETE -> DeleteRequestCodec.getInstance();
            case THREAD_CONFIG -> ThreadCodec.getInstance();
            case TENANT -> TenantRequestCodec.getInstance();
            case TRANSACTIONAL -> TransactionalMailCodec.getInstance();
            case EMAIL_EVENT -> EmailEventRequestCodec.getInstance();
            case DOMAIN -> EmailDomainRequestCodec.getInstance();
        };
    }

    /**
     * Represents the type of codec.
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    enum CodecType {

        SEND,
        RECEIVE,
        UPDATE,
        DELETE,
        THREAD_CONFIG,
        TENANT,
        TRANSACTIONAL,
        EMAIL_EVENT,
        DOMAIN,
        ;
    }

    /**
     * <p>
     * Gets the instance of {@link EmailCodec}.
     * </p>
     *
     * @param <T> which extends {@link AbstractEmailCodec}
     * @return {@link EmailCodec}
     */
    default <T extends EmailCodec> T get() {
        return (T) this;
    }
}
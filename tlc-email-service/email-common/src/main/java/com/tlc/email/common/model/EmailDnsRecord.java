package com.tlc.email.common.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.dns.model.DnsRecord;
import com.tlc.email.common.internal.status.EmailErrorCodes;

import java.util.Objects;

/**
 * <p>
 * Represents an DNS record configuration.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailDnsRecord extends DnsRecord implements EmailModel {

    private Long id;
    private Long domainId;
    private Category category;
    private RecordProvider recordProvider;

    public EmailDnsRecord() {
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setDomainId(final Long domainId) {
        this.domainId = domainId;
    }

    public Long getId() {
        return id;
    }

    public Long getDomainId() {
        return domainId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long id() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(final Category category) {
        this.category = category;
    }

    public RecordProvider getRecordProvider() {
        return recordProvider;
    }

    public void setRecordProvider(final RecordProvider recordProvider) {
        this.recordProvider = recordProvider;
    }

    /**
     * <p>
     * Represents an DNS record provider.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public enum RecordProvider {

        SENDGRID((short) 1),
        INTERNAL((short) 2);

        private final short id;

        RecordProvider(short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        public static RecordProvider get(final short id) {
            return switch (id) {
                case 1 -> SENDGRID;
                case 2 -> INTERNAL;
                default -> throw ErrorCode.get(EmailErrorCodes.INVALID_DNS_RECORD_PROVIDER);
            };
        }

        public boolean isInternal() {
            return Objects.equals(this, INTERNAL);
        }
    }
}
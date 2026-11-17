package com.tlc.commons.dns.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.dns.status.DnsErrorCodes;

/**
 * <p>
 * Represents the type of DNS record.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum RecordType {

    A((short) 1),
    AAAA((short) 2),
    CNAME((short) 3),
    MX((short) 4),
    TXT((short) 5),
    NS((short) 6),
    SRV((short) 7);

    private final short code;

    /**
     * <p>
     * Constructs a {@link RecordType} with the specified database code.
     * </p>
     *
     * @param id code representing the DNS record type.
     */
    RecordType(final short id) {
        this.code = id;
    }

    public short getId() {
        return code;
    }

    /**
     * <p>
     * Returns the {@link RecordType} corresponding to the given code.
     * </p>
     *
     * @param code the DNS record type code.
     * @return the matching RecordType.
     */
    public static RecordType get(final short code) {
        return switch (code) {
            case 1 -> A;
            case 2 -> AAAA;
            case 3 -> CNAME;
            case 4 -> MX;
            case 5 -> TXT;
            case 6 -> NS;
            case 7 -> SRV;
            default -> throw ErrorCode.get(DnsErrorCodes.UNSUPPORTED_RECORD_TYPE);
        };
    }
}
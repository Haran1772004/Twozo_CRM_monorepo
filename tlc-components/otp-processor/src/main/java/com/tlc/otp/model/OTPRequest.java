package com.tlc.otp.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.otp.code.OTPErrorCodes;
import com.tlc.otp.validator.annotation.OTPRequestValidator;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Represents the otp request details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@Setter
@Getter
@OTPRequestValidator(groups = {Group.Create.class, Group.Update.class})
public class OTPRequest implements TlcModel {

    private Long id;
    private Long orgId;
    private Long userId;
    private String otp;
    private String verificationToken;
    private Long expiresAt;
    private Long verifiedAt;
    private int totalAttempts;
    private long addedAt;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.otp.category.invalid")
    private Category category;

    private final String receiver;
    private final Channel channel;

    public OTPRequest(final String receiver, final Channel channel) {
        this.receiver = receiver;
        this.channel = channel;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        throw ErrorCode.get(OTPErrorCodes.UNSUPPORTED_OPERATION);
    }
}

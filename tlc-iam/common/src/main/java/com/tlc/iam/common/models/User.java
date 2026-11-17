package com.tlc.iam.common.models;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.code.ErrorCode;
import com.tlc.iam.common.models.validator.type.PasswordReset;
import com.tlc.iam.common.models.validator.type.Signup;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import com.tlc.validator.type.Phone;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

/**
 * An entity that holds user related information
 *
 * @author Selvakumar
 * @version 1.0
 */
public class User implements TlcModel, Serializable {

    public static final String PASSWORD_PATTERN = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-={}\\[\\]|\\\\:;\"'<>,.?/])\\S{8,16}$";

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.iam.accounts.user.id.required")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.iam.accounts.org.id.required")
    private Long orgId;

    @Size(max = 255, groups = {Group.Create.class, Group.Update.class, Signup.class}, message = "i18n.iam.accounts.user.name.max.length")
    @NotNull(groups = {Group.Create.class, Group.Update.class, Signup.class}, message = "i18n.iam.accounts.user.firstName.required")
    private String firstName;
    private String lastName;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Signup.class}, message = "i18n.iam.accounts.user.email.required")
    private String email;
    private boolean isEmailVerified;
    @Phone(groups = {Group.Create.class, Group.Update.class, Signup.class})
    @NotNull(groups = {Signup.class}, message = "i18n.iam.accounts.user.phone.required")
    private String mobile;
    private boolean isMobileNumberVerified;
    private Locale locale;
    private String localeString;
    private boolean isAdminUser = false;
    private DateTime createdTime;
    private DateTime stateUpdatedTime;
    private String fullName;

    @Size(max = 255, groups = {Group.Create.class, Group.Update.class}, message = "i18n.iam.accounts.max.job.title.length")
    private String jobTitle;

    @NotNull(groups = {Group.Create.class, Signup.class}, message = "i18n.iam.accounts.user.state.required")
    private User.State userState;

    @NotNull(groups = {Group.Create.class, Signup.class}, message = "i18n.iam.accounts.user.type.required")
    private Type userType;

    @NotNull(groups = {Signup.class}, message = "i18n.iam.accounts.user.password.required")
    @Pattern(groups = {PasswordReset.class, Signup.class}, regexp = PASSWORD_PATTERN, message = "i18n.iam.accounts.invalid.password.pattern")
    private String password;

    private String passwordHash;
    private Organization organization;
    private DateTime lastLoginTime;
    private DisplayPicture thumbnail;
    private DisplayPicture originalPicture;
    private String idpUserId;
    private Boolean hasActivePassword;

    public User(final Long id, final Long orgId, final String firstName, final String lastName, final String email,
                final String mobile, final String locale, final String passwordHash, final Type userType) {
        this.id = id;
        this.orgId = orgId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.mobile = mobile;
        this.localeString = locale;
        this.password = passwordHash;
        this.userType = userType;
        this.locale = new Locale(localeString);
    }

    public User() {
    }

    public enum Type {
        ORG_ADMIN((short) 1),
        ORG_USER((short) 2);

        private final short id;

        Type(short id) {
            this.id = id;
        }

        public short id() {
            return id;
        }

        public static Type valueOf(short id) {
            return switch (id) {
                case 1:
                    yield ORG_ADMIN;
                case 2:
                    yield ORG_USER;
                default:
                    throw ErrorCode.get(IamErrorCodes.INVALID_USER_TYPE);
            };
        }
    }

    /**
     * Represents the state of a user
     * ACTIVE: User is in an active state.
     * DEACTIVATED: User is deactivated, and data can be retrieved later.
     * INVITED: User is invited to join the organization.
     */
    public enum State {

        ACTIVE((short) 2, "Active"),
        DEACTIVATED((short) 3, "Deactivated"),
        INVITED((short) 4, "Invited");

        private final short id;
        private final String name;

        State(final short id, final String name) {
            this.id = id;
            this.name = name;
        }

        public short id() {
            return id;
        }

        public String getName() {
            return name;
        }

        public static User.State get(final short id) {
            return switch (id) {
                case 2 -> ACTIVE;
                case 3 -> DEACTIVATED;
                case 4 -> INVITED;
                default -> throw ErrorCode.get(IamErrorCodes.INVALID_USER_STATE);
            };
        }

        public static State get(final String name) {
            return switch (name) {
                case "ACTIVE" -> ACTIVE;
                case "DEACTIVATED" -> DEACTIVATED;
                case "INVITED" -> INVITED;
                default -> throw ErrorCode.get(IamErrorCodes.INVALID_USER_STATE);
            };
        }

        /**
         * Checks whether the user state is active or not.
         */
        public boolean isActive() {
            return switch (this) {
                case ACTIVE -> true;
                default -> false;
            };
        }
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;

        updateFullName();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;

        updateFullName();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Locale getLocale() {
        return new Locale(localeString);
    }

    public String getLocaleString() {
        return localeString;
    }

    public void setLocaleString(String locale) {
        this.localeString = locale;
        this.locale = new Locale(localeString);
    }

    public boolean isAdminUser() {
        return this.userType == Type.ORG_ADMIN;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
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
        return null;
    }

    public boolean isEmailVerified() {
        return isEmailVerified;
    }

    public void setEmailVerified(final boolean isEmailVerified) {
        this.isEmailVerified = isEmailVerified;
    }

    public boolean isMobileNumberVerified() {
        return isMobileNumberVerified;
    }

    public void setMobileNumberVerified(final boolean isMobileNumberVerified) {
        this.isMobileNumberVerified = isMobileNumberVerified;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(final String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Type getType() {
        return userType;
    }

    public void setType(final Type userType) {
        this.userType = userType;
    }

    public State getUserState() {
        return userState;
    }

    public void setOrganization(final Organization organization) {
        this.organization = organization;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setUserState(final State userState) {
        this.userState = userState;
    }

    public DateTime getCreatedTime() {
        return this.createdTime;
    }

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public DateTime getStateUpdatedTime() {
        return stateUpdatedTime;
    }

    public void setStateUpdatedTime(final DateTime stateUpdatedTime) {
        this.stateUpdatedTime = stateUpdatedTime;
    }

    private void updateFullName() {
        fullName = (Objects.isNull(firstName) || firstName.isBlank()) ? lastName
                : (Objects.isNull(lastName) || lastName.isBlank()) ? firstName : firstName + " " + lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(final String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public DateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(final DateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public void setThumbnail(final DisplayPicture thumbnail) {
        this.thumbnail = thumbnail;
    }

    public DisplayPicture getThumbnail() {
        return thumbnail;
    }

    public void setOriginalPicture(final DisplayPicture originalPicture) {
        this.originalPicture = originalPicture;
    }

    public DisplayPicture getOriginalPicture() {
        return originalPicture;
    }

    public String getIdpUserId() {
        return idpUserId;
    }

    public void setIdpUserId(final String idpUserId) {
        this.idpUserId = idpUserId;
    }

    public Boolean hasActivePassword() {
        return hasActivePassword;
    }

    public void setHasActivePassword(final Boolean hasActivePassword) {
        this.hasActivePassword = hasActivePassword;
    }

    /**
     * <p>
     * Overrides the equals method from {@link Object}
     * </p>
     *
     * @param object Represents the object to be compared
     * @return true if given object is equals to the current object
     */
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object instanceof User) {
            return this.hashCode() == object.hashCode();
        }
        return false;
    }

    /**
     * <p>
     * Overrides the hashcode method from {@link Object}
     * </p>
     *
     * @return hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}


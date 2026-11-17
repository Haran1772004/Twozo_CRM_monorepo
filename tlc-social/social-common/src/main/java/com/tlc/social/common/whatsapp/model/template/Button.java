package com.tlc.social.common.whatsapp.model.template;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.social.common.whatsapp.internal.status.WAErrorCodes;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A model which represents the button component of the template
 *
 * @author Maharaja S
 * @version 1.0
 */
public class Button extends Component {

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.social.whatsapp.template.button.type.not.found")
    private final ButtonType buttonType;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.social.whatsapp.template.button.text.not.found")
    @Size(max = 25, groups = {Group.Create.class, Group.Update.class}, message = "i18n.social.whatsapp.template.button.text.length.exceeded")
    private String text;
    private String url;
    private String phoneNumber;
    private boolean hasPlaceholder;
    private int index;

    public Button(final ButtonType buttonType) {
        this.buttonType = buttonType;
    }
    public ButtonType getButtonType() {
        return buttonType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public boolean hasPlaceholder() {
        return hasPlaceholder;
    }

    public void setHasPlaceholder(final boolean hasPlaceholder) {
        this.hasPlaceholder = hasPlaceholder;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(final int index) {
        this.index = index;
    }

    /**
     * {@inheritDoc}
     *
     * @return the type
     */
    @Override
    public Type getType() {
        return Type.BUTTONS;
    }

    /**
     * An enum which represents the button type
     *
     * @author Maharaja S
     * @version 1.0
     */
    public enum ButtonType {

        URL((short) 1),
        QUICK_REPLY((short) 2),
        COPY_CODE((short) 3),
        PHONE_NUMBER((short) 4),
        VOICE_CALL((short) 5);

        private final short id;

        ButtonType(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        /**
         * <p>
         * Gets the {@link ButtonType} based on the provided id}
         * </p>
         *
         * @param id Represents the id of the button type
         * @return the {@link Header.Format}
         */
        public static ButtonType getById(final short id) {
            return switch (id) {
                case 1 -> URL;
                case 2 -> QUICK_REPLY;
                case 3 -> COPY_CODE;
                case 4 -> PHONE_NUMBER;
                case 5 -> VOICE_CALL;
                default -> throw ErrorCode.get(WAErrorCodes.UNSUPPORTED_BUTTON_TYPE, "i18n.social.whatsapp.template.unsupported.button");
            };
        }
    }
}

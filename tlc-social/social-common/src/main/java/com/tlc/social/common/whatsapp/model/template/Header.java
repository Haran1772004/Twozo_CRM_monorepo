package com.tlc.social.common.whatsapp.model.template;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.social.common.whatsapp.internal.status.WAErrorCodes;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

/**
 * A model which represents the header component of the template
 *
 * @author Maharaja S
 * @version 1.0
 */
public class Header extends Component {

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.social.whatsapp.template.header.format.not.found")
    private final Format format;

    private String text;
    private boolean hasPlaceholder;

    public String getText() {
        return text;
    }

    public boolean hasPlaceholder() {
        return hasPlaceholder;
    }

    public void setHasPlaceholder(final boolean hasPlaceholder) {
        this.hasPlaceholder = hasPlaceholder;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public Header(final Format format) {
        this.format = format;
    }

    public Format getFormat() {
        return format;
    }

    /**
     * {@inheritDoc}
     *
     * @return the type
     */
    @Override
    public Type getType() {
        return Type.HEADER;
    }

    /**
     * An enum which represents the format of the header
     *
     * @author Maharaja S
     * @version 1.0
     */
    public enum Format {

        TEXT((short) 1),
        IMAGE((short) 2),
        VIDEO((short) 3),
        DOCUMENT((short) 4),
        LOCATION((short) 5);

        private final short id;

        Format(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        /**
         * <p>
         * Gets the {@link Format} based on the provided id}
         * </p>
         *
         * @param id Represents the id of the category
         * @return the {@link Format}
         */
        public static Format getById(final short id) {
            return switch (id) {
                case 1 -> TEXT;
                case 2 -> IMAGE;
                case 3 -> VIDEO;
                case 4 -> DOCUMENT;
                case 5 -> LOCATION;
                default -> throw ErrorCode.get(WAErrorCodes.UNSUPPORTED_TEMPLATE_HEADER_FORMAT, "i18n.social.whatsapp.template.header.format.not.supported");
            };
        }
    }
}

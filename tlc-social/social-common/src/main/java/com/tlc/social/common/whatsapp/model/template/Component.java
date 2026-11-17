package com.tlc.social.common.whatsapp.model.template;

import com.tlc.commons.code.ErrorCode;
import com.tlc.social.common.whatsapp.internal.status.WAErrorCodes;

/**
 * An abstract class which represents the component of the template
 *
 * @author Maharaja S
 * @version 1.0
 */
public abstract class Component {

    /**
     * <p>
     * Gets the {@link Type} of the template component
     * </p>
     *
     * @return the type
     */
    public abstract Type getType();

    /**
     * An enum which represents the type of the template component
     *
     * @author Maharaja S
     * @version 1.0
     */
    public enum Type {

        HEADER((short) 1),
        BODY((short) 2),
        FOOTER((short) 3),
        BUTTONS((short) 4);

        private final short id;

        Type(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        /**
         * <p>
         * Gets the {@link Type} based on the provided id}
         * </p>
         *
         * @param id Represents the id of the category
         * @return the {@link Type}
         */
        public static Type getById(final short id) {
            return switch (id) {
                case 1 -> HEADER;
                case 2 -> BODY;
                case 3 -> FOOTER;
                case 4 -> BUTTONS;
                default -> throw ErrorCode.get(WAErrorCodes.UNSUPPORTED_TEMPLATE_COMPONENT_TYPE, "i18n.social.whatsapp.template.unsupported.component");
            };
        }
    }
}

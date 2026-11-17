package com.tlc.commons.color;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;

import java.util.Objects;

/**
 * Enum representing the supported colors along with their unique IDs
 * and corresponding display names.
 *
 * @author Breethika Devi
 * @version 1.0
 */
public enum Color {

    BLUE((short) 1, "BLUE"),
    MILD_ORANGE((short) 2, "MILD_ORANGE"),
    DESERT_BROWN((short) 3, "DESERT_BROWN"),
    LEMON_YELLOW((short) 4, "LEMON_YELLOW"),
    SPRING_GREEN((short) 5, "SPRING_GREEN"),
    SUMMER_GREEN((short) 6, "SUMMER_GREEN"),
    AQUA_BLUE((short) 7, "AQUA_BLUE"),
    EVENING_BLUE((short) 8, "EVENING_BLUE");

    private final short id;

    private final String name;

    Color(final short id, final String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Gets the id
     *
     * @return id
     */
    public short id() {
        return id;
    }

    /**
     * Gets the name
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the Color corresponding to the provided id.
     */
    public static Color get(final short id) {
        return switch (id) {
            case 1 -> BLUE;
            case 2 -> MILD_ORANGE;
            case 3 -> DESERT_BROWN;
            case 4 -> LEMON_YELLOW;
            case 5 -> SPRING_GREEN;
            case 6 -> SUMMER_GREEN;
            case 7 -> AQUA_BLUE;
            case 8 -> EVENING_BLUE;
            default -> throw ErrorCode.get(ErrorCodes.INVALID_COLOR, "Invalid color id");
        };
    }

    /**
     * Returns the Color corresponding to the provided name.
     */
    public static Color get(final String name) {
        if (Objects.isNull(name) || name.isBlank()) {
            throw ErrorCode.get(ErrorCodes.INVALID_COLOR, "Invalid color name");
        }
        return switch (name) {
            case "BLUE" -> BLUE;
            case "MILD_ORANGE" -> MILD_ORANGE;
            case "DESERT_BROWN" -> DESERT_BROWN;
            case "LEMON_YELLOW" -> LEMON_YELLOW;
            case "SPRING_GREEN" -> SPRING_GREEN;
            case "SUMMER_GREEN" -> SUMMER_GREEN;
            case "AQUA_BLUE" -> AQUA_BLUE;
            case "EVENING_BLUE" -> EVENING_BLUE;
            default -> throw ErrorCode.get(ErrorCodes.INVALID_COLOR, "Invalid color name");
        };
    }
}
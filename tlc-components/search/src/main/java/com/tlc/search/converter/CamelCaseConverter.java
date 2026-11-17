package com.tlc.search.converter;

import java.util.Objects;

/**
 * <p>
 * Provides the converter to converts the uppercase string to camelcase string.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CamelCaseConverter {

    private static final String UNDERSCORE = "_";
    private static final int ZERO = 0;
    private static final int ONE = 1;

    private static class InstanceHolder {
        private static final CamelCaseConverter CASE_CONVERTER = new CamelCaseConverter();
    }

    private CamelCaseConverter() {

    }

    public static CamelCaseConverter getInstance() {
        return InstanceHolder.CASE_CONVERTER;
    }

    /**
     * <p>
     * Converts the uppercase string to camelcase string.
     * </p>
     *
     * @param name Upper case string value.
     * @return Camel case string value.
     */
    public String convert(final String name) {
        if ((name.contains(UNDERSCORE) && name.equals(name.toLowerCase())) || name.equals(name.toUpperCase())) {
            final String[] names = name.split(UNDERSCORE);
            final StringBuilder nameBuilder = new StringBuilder();

            for (int index = ZERO; index < names.length; index++) {
                final String partOfName = names[index];

                if (Objects.equals(ZERO, index)) {
                    nameBuilder.append(partOfName.toLowerCase());
                } else {
                    nameBuilder.append(partOfName.charAt(ZERO));
                    nameBuilder.append(partOfName.substring(ONE).toLowerCase());
                }
            }

            return nameBuilder.toString();
        }

        return name;
    }
}

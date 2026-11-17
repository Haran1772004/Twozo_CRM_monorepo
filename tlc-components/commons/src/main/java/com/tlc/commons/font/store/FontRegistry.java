package com.tlc.commons.font.store;

import com.tlc.commons.font.Font;
import com.tlc.commons.util.Configurations;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains the registry of available fonts used for document rendering.
 *
 * <p>The registry loads font configurations from the application configuration,
 * resolves font file locations, creates {@link Font} definitions, and stores them
 * grouped by font family for efficient lookup.
 *
 * <p>This class follows the singleton pattern to provide a shared font registry
 * instance across the application.
 *
 * <p>Registered fonts can be retrieved either as a complete collection or by
 * specific font family name.
 *
 * @author Sathish
 * @version 1.0
 */
public final class FontRegistry {

    private static final Collection<Font> EMPTY_COLLECTION = Collections.emptyList();

    private final Map<String, Collection<Font>> fonts;

    private FontRegistry() {
        fonts = new ConcurrentHashMap<>();
    }

    private static final class InstanceHolder {
        private static final FontRegistry FONT_STORE = new FontRegistry();
    }

    public static FontRegistry getInstance() {
        return InstanceHolder.FONT_STORE;
    }

    /**
     * Registers font definitions from the provided configuration.
     *
     * <p>The method reads the configured font base path and font family definitions,
     * creates {@link Font} objects for each configured font file, and stores them
     * internally grouped by their font family.
     *
     * <p>Existing registered fonts are retained, and newly loaded fonts are added
     * to the corresponding font family collection.
     *
     * @param configurations Application configuration containing font registry details.
     */
    public void register(final Configurations configurations) {
        final String basePath = configurations.getConfig(FontFields.BASE_PATH);
        final Configurations fontConfigurations = configurations.getNestedConfigs(FontFields.CONFIG);

        configurations.getValues(FontFields.FONT_FAMILIES).forEach(fontFamily -> {
            final Configurations fontFamilyConfigs = fontConfigurations.getNestedConfigs(fontFamily);
            final String familyName = fontFamilyConfigs.getConfig(FontFields.NAME);
            final Collection<Map<String, String>> fontConfigs = fontFamilyConfigs.groupedConfigArray(FontFields.FONTS);

            for (final Map<String, String> fontConfig : fontConfigs) {
                final String name = fontConfig.get(FontFields.NAME);
                final String fileName = fontConfig.get(FontFields.FILE_NAME);
                final String weight = fontConfig.get(FontFields.WEIGHT);
                final String style = fontConfig.get(FontFields.STYLE);
                final boolean subset = Boolean.parseBoolean(fontConfig.get(FontFields.SUBSET));
                final Font font = new Font(
                        new File(basePath, fileName), familyName, name,
                        Objects.isNull(weight) ? null : Integer.parseInt(weight),
                        Objects.isNull(style) ? Font.Style.NORMAL : Font.Style.valueOf(style), subset);

                fonts.computeIfAbsent(familyName, famName -> new HashSet<>()).add(font);
            }
        });
    }

    /**
     * Unregisters all fonts from the registry, clearing all stored font definitions.
     */
    public void unregister() {
        fonts.clear();
    }

    /**
     * Returns all registered fonts from every configured font family.
     *
     * @return collection containing all registered {@link Font} definitions.
     */
    public Collection<Font> getAllFonts() {
        return fonts.values().stream().flatMap(Collection::stream).toList();
    }

    /**
     * Retrieves all registered fonts for the given font family.
     *
     * <p>If the requested font family is not registered, an empty collection is returned.
     *
     * @param fontFamily Name of the font family to retrieve.
     *
     * @return collection of fonts belonging to the specified family.
     */
    public Collection<Font> getFonts(final String fontFamily) {
        return fonts.getOrDefault(fontFamily, EMPTY_COLLECTION);
    }

    /**
     * Contains configuration keys used for font registry loading.
     */
    private static class FontFields {

        private static final String BASE_PATH = "basePath";
        private static final String FONT_FAMILIES = "fontFamilies";
        private static final String FONTS = "fonts";
        private static final String CONFIG = "config";
        private static final String FILE_NAME = "fileName";
        private static final String WEIGHT = "weight";
        private static final String STYLE = "style";
        private static final String SUBSET = "subset";
        private static final String NAME = "name";

    }
}

package com.tlc.crm.common.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * An enum that represents a {@link Language} data
 * </p>
 *
 * @author eswari
 * @version 1.0
 */
public enum Language {

    ARABIC("عربي", "Arabic", "ar", 1),
    CATALAN("Català", "Catalan", "ca", 2),
    CZECH("Čeština", "Czech", "cs", 3),
    CHINESE_SIMPLIFIED("简体中文 (Simplified)", "Chinese", "zh-Hans", 4),
    CHINESE_TRADITIONAL("繁體中文 (Traditional)", "Chinese", "zh-Hant", 5),
    DANISH("Dansk", "Danish", "da", 6),
    DUTCH("Nederlands", "Dutch", "nl", 7),
    ENGLISH_UNITED_KINGDOM("English (United Kingdom)", "English", "en-GB", 8),
    ENGLISH_UNITED_STATES("English (United States)", "English", "en-US", 9),
    ESTONIAN("Eesti keel", "Estonian", "et", 10),
    FINNISH("Suomi", "Finnish", "fi", 11),
    FRENCH("Français", "French", "fr", 12),
    GERMAN("Deutsch", "German", "de", 13),
    HEBREW("עִברִית", "Hebrew", "he", 14),
    HUNGARIAN("Magyar", "Hungarian", "hu", 15),
    INDONESIAN("Bahasa Indonesia", "Indonesian", "id", 16),
    ITALIAN("Italiano", "Italian", "it", 17),
    JAPANESE("日本語", "Japanese", "ja", 18),
    KOREAN("한국어", "Korean", "ko", 19),
    LATVIAN("Latviešu", "Latvian", "lv", 20),
    NORWEGIAN("Norsk", "Norwegian", "no", 21),
    POLISH("Polski", "Polish", "pl", 22),
    PORTUGUESE_BRAZIL("Português (Brazil)", "Portuguese", "pt-BR", 23),
    PORTUGUESE_PORTUGAL("Português (Portugal)", "Portuguese", "pt-PT", 24),
    ROMANIAN("Română", "Romanian", "ro", 25),
    RUSSIAN("русский", "Russian", "ru", 26),
    SLOVAK("Slovenčina", "Slovak", "sk", 27),
    SLOVENIAN("Slovenčina", "Slovenian", "sl", 28),
    SPANISH_LATIN_AMERICA("Español (Latin America)", "Spanish", "es-419", 29),
    SPANISH_SPAIN("Español (Spain)", "Spanish", "es-ES", 30),
    SWEDISH("Svenska", "Swedish", "sv", 31),
    THAI("ภาษาไทย", "Thai", "th", 32),
    TURKISH("Türkçe", "Turkish", "tr", 33),
    UKRAINIAN("Українська", "Ukrainian", "uk", 34),
    VIETNAMESE("Tiếng Việt", "Vietnamese", "vi", 35);

    private static final Map<String, Language> LANGUAGE_BY_COUNTRY = new HashMap<>();

    private final String name;
    private final String aliasName;
    private final String languageTag;
    private final int id;

    static {

        for (final Language language : Language.values()) {
            LANGUAGE_BY_COUNTRY.put(language.getAliasName(), language);
        }
    }

    Language(final String name, final String aliasName, final String languageTag, final int id) {
        this.name = name;
        this.aliasName = aliasName;
        this.languageTag = languageTag;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getAliasName() {
        return aliasName;
    }

    public int getId() {
        return id;
    }
    
    public String getLanguageTag() {
        return languageTag;
    }

    /**
     * <p>
     * Gets the {@link Language} by country
     * </p>
     *
     * @param country Represents the country name
     * @return {@link Language}
     */
    public static Language get(final String country) {
        final Language language = LANGUAGE_BY_COUNTRY.get(country);

        if (Objects.isNull(language)) {
            throw ErrorCode.get(CommonErrorCodes.INVALID_COUNTRY);
        }

        return language;
    }

    /**
     * Gets the {@link Language} by id
     *
     * @param id represents the id of the language
     * @return {@link Language}
     */
    public static Language getLanguageById(final int id) {
        return switch (id) {
            case 1 -> ARABIC;
            case 2 -> CATALAN;
            case 3 -> CZECH;
            case 4 -> CHINESE_SIMPLIFIED;
            case 5 -> CHINESE_TRADITIONAL;
            case 6 -> DANISH;
            case 7 -> DUTCH;
            case 8 -> ENGLISH_UNITED_KINGDOM;
            case 9 -> ENGLISH_UNITED_STATES;
            case 10 -> ESTONIAN;
            case 11 -> FINNISH;
            case 12 -> FRENCH;
            case 13 -> GERMAN;
            case 14 -> HEBREW;
            case 15 -> HUNGARIAN;
            case 16 -> INDONESIAN;
            case 17 -> ITALIAN;
            case 18 -> JAPANESE;
            case 19 -> KOREAN;
            case 20 -> LATVIAN;
            case 21 -> NORWEGIAN;
            case 22 -> POLISH;
            case 23 -> PORTUGUESE_BRAZIL;
            case 24 -> PORTUGUESE_PORTUGAL;
            case 25 -> ROMANIAN;
            case 26 -> RUSSIAN;
            case 27 -> SLOVAK;
            case 28 -> SLOVENIAN;
            case 29 -> SPANISH_LATIN_AMERICA;
            case 30 -> SPANISH_SPAIN;
            case 31 -> SWEDISH;
            case 32 -> THAI;
            case 33 -> TURKISH;
            case 34 -> UKRAINIAN;
            case 35 -> VIETNAMESE;
            default -> throw ErrorCode.get(CommonErrorCodes.INVALID_LANGUAGE);
        };
    }
}

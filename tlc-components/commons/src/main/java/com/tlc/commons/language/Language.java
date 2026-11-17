package com.tlc.commons.language;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;

/**
 * An enum which represents the languages
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum Language {

    AFRIKAANS((short) 1, "Afrikaans", "af"),
    ALBANIAN((short) 2, "Albanian", "sq"),
    ARABIC((short) 3, "Arabic", "ar"),
    ARABIC_EGY((short) 4, "Arabic (EGY)", "ar_EG"),
    ARABIC_UAE((short) 5, "Arabic (UAE)", "ar_AE"),
    ARABIC_LBN((short) 6, "Arabic (LBN)", "ar_LB"),
    ARABIC_MAR((short) 7, "Arabic (MAR)", "ar_MA"),
    ARABIC_QAT((short) 8, "Arabic (QAT)", "ar_QA"),
    AZERBAIJANI((short) 9, "Azerbaijani", "az"),
    BELARUSIAN((short) 10, "Belarusian", "be_BY"),
    BENGALI((short) 11, "Bengali", "bn"),
    BENGALI_IND((short) 12, "Bengali (IND)", "bn_IN"),
    BULGARIAN((short) 13, "Bulgarian", "bg"),
    CATALAN((short) 14, "Catalan", "ca"),
    CHINESE_CHN((short) 15, "Chinese (CHN)", "zh_CN"),
    CHINESE_HKG((short) 16, "Chinese (HKG)", "zh_HK"),
    CHINESE_TAI((short) 17, "Chinese (TAI)", "zh_TW"),
    CROATIAN((short) 18, "Croatian", "hr"),
    CZECH((short) 19, "Czech", "cs"),
    DANISH((short) 20, "Danish", "da"),
    DARI((short) 21, "Dari", "prs_AF"),
    DUTCH((short) 22, "Dutch", "nl"),
    DUTCH_BEL((short) 23, "Dutch (BEL)", "nl_BE"),
    ENGLISH((short) 24, "English", "en"),
    ENGLISH_UK((short) 25, "English (UK)", "en_GB"),
    ENGLISH_US((short) 26, "English (US)", "en_US"),
    ENGLISH_UAE((short) 27, "English (UAE)", "en_AE"),
    ENGLISH_AUS((short) 28, "English (AUS)", "en_AU"),
    ENGLISH_CAN((short) 29, "English (CAN)", "en_CA"),
    ENGLISH_GHA((short) 30, "English (GHA)", "en_GH"),
    ENGLISH_IRL((short) 31, "English (IRL)", "en_IE"),
    ENGLISH_IND((short) 32, "English (IND)", "en_IN"),
    ENGLISH_JAM((short) 33, "English (JAM)", "en_JM"),
    ENGLISH_MYS((short) 34, "English (MYS)", "en_MY"),
    ENGLISH_NZL((short) 35, "English (NZL)", "en_NZ"),
    ENGLISH_QAT((short) 36, "English (QAT)", "en_QA"),
    ENGLISH_SGP((short) 37, "English (SGP)", "en_SG"),
    ENGLISH_UGA((short) 38, "English (UGA)", "en_UG"),
    ENGLISH_ZAF((short) 39, "English (ZAF)", "en_ZA"),
    ESTONIAN((short) 40, "Estonian", "et"),
    FILIPINO((short) 41, "Filipino", "fil"),
    FINNISH((short) 42, "Finnish", "fi"),
    FRENCH((short) 43, "French", "fr"),
    FRENCH_BEL((short) 44, "French (BEL)", "fr_BE"),
    FRENCH_CAN((short) 45, "French (CAN)", "fr_CA"),
    FRENCH_CHE((short) 46, "French (CHE)", "fr_CH"),
    FRENCH_CIV((short) 47, "French (CIV)", "fr_CI"),
    FRENCH_MAR((short) 48, "French (MAR)", "fr_MA"),
    GEORGIAN((short) 49, "Georgian", "ka"),
    GERMAN((short) 50, "German", "de"),
    GERMAN_AUT((short) 51, "German (AUT)", "de_AT"),
    GERMAN_CHE((short) 52, "German (CHE)", "de_CH"),
    GREEK((short) 53, "Greek", "el"),
    GUJARATI((short) 54, "Gujarati", "gu"),
    HAUSA((short) 55, "Hausa", "ha"),
    HEBREW((short) 56, "Hebrew", "he"),
    HINDI((short) 57, "Hindi", "hi"),
    HUNGARIAN((short) 58, "Hungarian", "hu"),
    INDONESIAN((short) 59, "Indonesian", "id"),
    IRISH((short) 60, "Irish", "ga"),
    ITALIAN((short) 61, "Italian", "it"),
    JAPANESE((short) 62, "Japanese", "ja"),
    KANNADA((short) 63, "Kannada", "kn"),
    KAZAKH((short) 64, "Kazakh", "kk"),
    KINYARWANDA((short) 65, "Kinyarwanda", "rw_RW"),
    KOREAN((short) 66, "Korean", "ko"),
    KYRGYZ((short) 67, "Kyrgyz (Kyrgyzstan)", "ky_KG"),
    LAO((short) 68, "Lao", "lo"),
    LATVIAN((short) 69, "Latvian", "lv"),
    LITHUANIAN((short) 70, "Lithuanian", "lt"),
    MACEDONIAN((short) 71, "Macedonian", "mk"),
    MALAY((short) 72, "Malay", "ms"),
    MALAYALAM((short) 73, "Malayalam", "ml"),
    MARATHI((short) 74, "Marathi", "mr"),
    NORWEGIAN((short) 75, "Norwegian", "nb"),
    PASHTO((short) 76, "Pashto", "ps_AF"),
    PERSIAN((short) 77, "Persian", "fa"),
    POLISH((short) 78, "Polish", "pl"),
    PORTUGUESE_BR((short) 79, "Portuguese (BR)", "pt_BR"),
    PORTUGUESE_POR((short) 80, "Portuguese (POR)", "pt_PT"),
    PUNJABI((short) 81, "Punjabi", "pa"),
    ROMANIAN((short) 82, "Romanian", "ro"),
    RUSSIAN((short) 83, "Russian", "ru"),
    SERBIAN((short) 84, "Serbian", "sr"),
    SINHALA((short) 85, "Sinhala", "si_LK"),
    SLOVAK((short) 86, "Slovak", "sk"),
    SLOVENIAN((short) 87, "Slovenian", "sl"),
    SPANISH((short) 88, "Spanish", "es"),
    SPANISH_ARG((short) 89, "Spanish (ARG)", "es_AR"),
    SPANISH_CHL((short) 90, "Spanish (CHL)", "es_CL"),
    SPANISH_COL((short) 91, "Spanish (COL)", "es_CO"),
    SPANISH_CRI((short) 92, "Spanish (CRI)", "es_CR"),
    SPANISH_DOM((short) 93, "Spanish (DOM)", "es_DO"),
    SPANISH_ECU((short) 94, "Spanish (ECU)", "es_EC"),
    SPANISH_HND((short) 95, "Spanish (HND)", "es_HN"),
    SPANISH_MEX((short) 96, "Spanish (MEX)", "es_MX"),
    SPANISH_PAN((short) 97, "Spanish (PAN)", "es_PA"),
    SPANISH_PER((short) 98, "Spanish (PER)", "es_PE"),
    SPANISH_SPA((short) 99, "Spanish (SPA)", "es_ES"),
    SPANISH_URY((short) 100, "Spanish (URY)", "es_UY"),
    SWAHILI((short) 101, "Swahili", "sw"),
    SWEDISH((short) 102, "Swedish", "sv"),
    TAMIL((short) 103, "Tamil", "ta"),
    TELUGU((short) 104, "Telugu", "te"),
    THAI((short) 105, "Thai", "th"),
    TURKISH((short) 106, "Turkish", "tr"),
    UKRAINIAN((short) 107, "Ukrainian", "uk"),
    URDU((short) 108, "Urdu", "ur"),
    UZBEK((short) 109, "Uzbek", "uz"),
    VIETNAMESE((short) 110, "Vietnamese", "vi"),
    ZULU((short) 111, "Zulu", "zu");

    private final short id;
    private final String displayLanguage;
    private final String code;

    Language(final short id, final String displayLanguage, final String code) {
        this.id = id;
        this.displayLanguage = displayLanguage;
        this.code = code;
    }

    public String getDisplayLanguage() {
        return displayLanguage;
    }

    public String getCode() {
        return code;
    }

    public short getId() {
        return id;
    }

    /**
     * <p>
     * Gets the {@link Language} from the provided code
     * </p>
     *
     * @param code Represents the language code
     * @return the {@link Language}
     */
    public static Language getFromCode(final String code) {
        return switch (code) {
            case "af" -> AFRIKAANS;
            case "sq" -> ALBANIAN;
            case "ar" -> ARABIC;
            case "ar_EG" -> ARABIC_EGY;
            case "ar_AE" -> ARABIC_UAE;
            case "ar_LB" -> ARABIC_LBN;
            case "ar_MA" -> ARABIC_MAR;
            case "ar_QA" -> ARABIC_QAT;
            case "az" -> AZERBAIJANI;
            case "be_BY" -> BELARUSIAN;
            case "bn" -> BENGALI;
            case "bn_IN" -> BENGALI_IND;
            case "bg" -> BULGARIAN;
            case "ca" -> CATALAN;
            case "zh_CN" -> CHINESE_CHN;
            case "zh_HK" -> CHINESE_HKG;
            case "zh_TW" -> CHINESE_TAI;
            case "hr" -> CROATIAN;
            case "cs" -> CZECH;
            case "da" -> DANISH;
            case "prs_AF" -> DARI;
            case "nl" -> DUTCH;
            case "nl_BE" -> DUTCH_BEL;
            case "en" -> ENGLISH;
            case "en_GB" -> ENGLISH_UK;
            case "en_US" -> ENGLISH_US;
            case "en_AE" -> ENGLISH_UAE;
            case "en_AU" -> ENGLISH_AUS;
            case "en_CA" -> ENGLISH_CAN;
            case "en_GH" -> ENGLISH_GHA;
            case "en_IE" -> ENGLISH_IRL;
            case "en_IN" -> ENGLISH_IND;
            case "en_JM" -> ENGLISH_JAM;
            case "en_MY" -> ENGLISH_MYS;
            case "en_NZ" -> ENGLISH_NZL;
            case "en_QA" -> ENGLISH_QAT;
            case "en_SG" -> ENGLISH_SGP;
            case "en_UG" -> ENGLISH_UGA;
            case "en_ZA" -> ENGLISH_ZAF;
            case "et" -> ESTONIAN;
            case "fil" -> FILIPINO;
            case "fi" -> FINNISH;
            case "fr" -> FRENCH;
            case "fr_BE" -> FRENCH_BEL;
            case "fr_CA" -> FRENCH_CAN;
            case "fr_CH" -> FRENCH_CHE;
            case "fr_CI" -> FRENCH_CIV;
            case "fr_MA" -> FRENCH_MAR;
            case "ka" -> GEORGIAN;
            case "de" -> GERMAN;
            case "de_AT" -> GERMAN_AUT;
            case "de_CH" -> GERMAN_CHE;
            case "el" -> GREEK;
            case "gu" -> GUJARATI;
            case "ha" -> HAUSA;
            case "he" -> HEBREW;
            case "hi" -> HINDI;
            case "hu" -> HUNGARIAN;
            case "id" -> INDONESIAN;
            case "ga" -> IRISH;
            case "it" -> ITALIAN;
            case "ja" -> JAPANESE;
            case "kn" -> KANNADA;
            case "kk" -> KAZAKH;
            case "rw_RW" -> KINYARWANDA;
            case "ko" -> KOREAN;
            case "ky_KG" -> KYRGYZ;
            case "lo" -> LAO;
            case "lv" -> LATVIAN;
            case "lt" -> LITHUANIAN;
            case "mk" -> MACEDONIAN;
            case "ms" -> MALAY;
            case "ml" -> MALAYALAM;
            case "mr" -> MARATHI;
            case "nb" -> NORWEGIAN;
            case "ps_AF" -> PASHTO;
            case "fa" -> PERSIAN;
            case "pl" -> POLISH;
            case "pt_BR" -> PORTUGUESE_BR;
            case "pt_PT" -> PORTUGUESE_POR;
            case "pa" -> PUNJABI;
            case "ro" -> ROMANIAN;
            case "ru" -> RUSSIAN;
            case "sr" -> SERBIAN;
            case "si_LK" -> SINHALA;
            case "sk" -> SLOVAK;
            case "sl" -> SLOVENIAN;
            case "es" -> SPANISH;
            case "es_AR" -> SPANISH_ARG;
            case "es_CL" -> SPANISH_CHL;
            case "es_CO" -> SPANISH_COL;
            case "es_CR" -> SPANISH_CRI;
            case "es_DO" -> SPANISH_DOM;
            case "es_EC" -> SPANISH_ECU;
            case "es_HN" -> SPANISH_HND;
            case "es_MX" -> SPANISH_MEX;
            case "es_PA" -> SPANISH_PAN;
            case "es_PE" -> SPANISH_PER;
            case "es_ES" -> SPANISH_SPA;
            case "es_UY" -> SPANISH_URY;
            case "sw" -> SWAHILI;
            case "sv" -> SWEDISH;
            case "ta" -> TAMIL;
            case "te" -> TELUGU;
            case "th" -> THAI;
            case "tr" -> TURKISH;
            case "uk" -> UKRAINIAN;
            case "ur" -> URDU;
            case "uz" -> UZBEK;
            case "vi" -> VIETNAMESE;
            case "zu" -> ZULU;
            default -> throw ErrorCode.get(ErrorCodes.UNSUPPORTED_LANGUAGE, String.format("Unsupported language code: %s", code));
        };
    }

    /**
     * <p>
     * Gets the {@link Language} from the provided id
     * </p>
     *
     * @param id Represents the language id
     * @return the {@link Language}
     */
    public static Language getById(final short id) {
        return switch (id) {
            case 1 -> AFRIKAANS;
            case 2 -> ALBANIAN;
            case 3 -> ARABIC;
            case 4 -> ARABIC_EGY;
            case 5 -> ARABIC_UAE;
            case 6 -> ARABIC_LBN;
            case 7 -> ARABIC_MAR;
            case 8 -> ARABIC_QAT;
            case 9 -> AZERBAIJANI;
            case 10 -> BELARUSIAN;
            case 11 -> BENGALI;
            case 12 -> BENGALI_IND;
            case 13 -> BULGARIAN;
            case 14 -> CATALAN;
            case 15 -> CHINESE_CHN;
            case 16 -> CHINESE_HKG;
            case 17 -> CHINESE_TAI;
            case 18 -> CROATIAN;
            case 19 -> CZECH;
            case 20 -> DANISH;
            case 21 -> DARI;
            case 22 -> DUTCH;
            case 23 -> DUTCH_BEL;
            case 24 -> ENGLISH;
            case 25 -> ENGLISH_UK;
            case 26 -> ENGLISH_US;
            case 27 -> ENGLISH_UAE;
            case 28 -> ENGLISH_AUS;
            case 29 -> ENGLISH_CAN;
            case 30 -> ENGLISH_GHA;
            case 31 -> ENGLISH_IRL;
            case 32 -> ENGLISH_IND;
            case 33 -> ENGLISH_JAM;
            case 34 -> ENGLISH_MYS;
            case 35 -> ENGLISH_NZL;
            case 36 -> ENGLISH_QAT;
            case 37 -> ENGLISH_SGP;
            case 38 -> ENGLISH_UGA;
            case 39 -> ENGLISH_ZAF;
            case 40 -> ESTONIAN;
            case 41 -> FILIPINO;
            case 42 -> FINNISH;
            case 43 -> FRENCH;
            case 44 -> FRENCH_BEL;
            case 45 -> FRENCH_CAN;
            case 46 -> FRENCH_CHE;
            case 47 -> FRENCH_CIV;
            case 48 -> FRENCH_MAR;
            case 49 -> GEORGIAN;
            case 50 -> GERMAN;
            case 51 -> GERMAN_AUT;
            case 52 -> GERMAN_CHE;
            case 53 -> GREEK;
            case 54 -> GUJARATI;
            case 55 -> HAUSA;
            case 56 -> HEBREW;
            case 57 -> HINDI;
            case 58 -> HUNGARIAN;
            case 59 -> INDONESIAN;
            case 60 -> IRISH;
            case 61 -> ITALIAN;
            case 62 -> JAPANESE;
            case 63 -> KANNADA;
            case 64 -> KAZAKH;
            case 65 -> KINYARWANDA;
            case 66 -> KOREAN;
            case 67 -> KYRGYZ;
            case 68 -> LAO;
            case 69 -> LATVIAN;
            case 70 -> LITHUANIAN;
            case 71 -> MACEDONIAN;
            case 72 -> MALAY;
            case 73 -> MALAYALAM;
            case 74 -> MARATHI;
            case 75 -> NORWEGIAN;
            case 76 -> PASHTO;
            case 77 -> PERSIAN;
            case 78 -> POLISH;
            case 79 -> PORTUGUESE_BR;
            case 80 -> PORTUGUESE_POR;
            case 81 -> PUNJABI;
            case 82 -> ROMANIAN;
            case 83 -> RUSSIAN;
            case 84 -> SERBIAN;
            case 85 -> SINHALA;
            case 86 -> SLOVAK;
            case 87 -> SLOVENIAN;
            case 88 -> SPANISH;
            case 89 -> SPANISH_ARG;
            case 90 -> SPANISH_CHL;
            case 91 -> SPANISH_COL;
            case 92 -> SPANISH_CRI;
            case 93 -> SPANISH_DOM;
            case 94 -> SPANISH_ECU;
            case 95 -> SPANISH_HND;
            case 96 -> SPANISH_MEX;
            case 97 -> SPANISH_PAN;
            case 98 -> SPANISH_PER;
            case 99 -> SPANISH_SPA;
            case 100 -> SPANISH_URY;
            case 101 -> SWAHILI;
            case 102 -> SWEDISH;
            case 103 -> TAMIL;
            case 104 -> TELUGU;
            case 105 -> THAI;
            case 106 -> TURKISH;
            case 107 -> UKRAINIAN;
            case 108 -> URDU;
            case 109 -> UZBEK;
            case 110 -> VIETNAMESE;
            case 111 -> ZULU;
            default -> throw ErrorCode.get(ErrorCodes.UNSUPPORTED_LANGUAGE, String.format("Unsupported language. Id: %s", id));
        };
    }
}
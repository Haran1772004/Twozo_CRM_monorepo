package com.tlc.l10n.model.currency;

import com.tlc.commons.code.ErrorCode;

import com.tlc.i18n.I18nKey;

import com.tlc.l10n.internal.status.L10NErrorCode;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * An enum that represents a {@link Currency} data
 * </p>
 *
 * @author eswari
 * @version 1.0
 */
public enum Currency {
    AED("AED", "unitedArabEmiratesDirham", new I18nKey("i18n.l10n.currency.united.arab.emirates.dirham"), (short) 1),
    AFN("AFN", "afghanAfghani", new I18nKey("i18n.l10n.currency.afghan.afghani"), (short) 2),
    ALL("ALL", "albanianLek", new I18nKey("i18n.l10n.currency.albanian.lek"), (short) 3),
    AMD("AMD", "armenianDram", new I18nKey("i18n.l10n.currency.armenian.dram"), (short) 4),
    ANG("ANG", "netherlandsAntilleanGuilder", new I18nKey("i18n.l10n.currency.netherlands.antilles.guilder"), (short) 5),
    AOA("AOA", "angolanKwanza", new I18nKey("i18n.l10n.currency.angolan.kwanza"), (short) 6),
    ARS("ARS", "argentinePeso", new I18nKey("i18n.l10n.currency.argentine.peso"), (short) 7),
    AUD("AUD", "australianDollar", new I18nKey("i18n.l10n.currency.australian.dollar"), (short) 8),
    AWG("AWG", "arubanFlorin", new I18nKey("i18n.l10n.currency.aruban.florin"), (short) 9),
    AZN("AZN", "azerbaijaniManat", new I18nKey("i18n.l10n.currency.azerbaijan.manat"), (short) 10),
    BAM("BAM", "bosniaAndHerzegovinaConvertibleMark", new I18nKey("i18n.l10n.currency.bosnia.and.herzegovina.convertible.marks"), (short) 11),
    BBD("BBD", "barbadianDollar", new I18nKey("i18n.l10n.currency.barbadian.dollar"), (short) 12),
    BDT("BDT", "bangladeshiTaka", new I18nKey("i18n.l10n.currency.bangladesh.taka"), (short) 13),
    BGN("BGN", "bulgarianLev", new I18nKey("i18n.l10n.currency.bulgarian.lev"), (short) 14),
    BHD("BHD", "bahrainiDinar", new I18nKey("i18n.l10n.currency.bahraini.dinar"), (short) 15),
    BIF("BIF", "burundianFranc", new I18nKey("i18n.l10n.currency.burundi.franc"), (short) 16),
    BMD("BMD", "bermudanDollar", new I18nKey("i18n.l10n.currency.bermudian.dollar"), (short) 17),
    BND("BND", "bruneiDollar", new I18nKey("i18n.l10n.currency.brunei.dollar"), (short) 18),
    BOB("BOB", "bolivianBoliviano", new I18nKey("i18n.l10n.currency.bolivian.boliviano"), (short) 19),
    BRL("BRL", "brazilianReal", new I18nKey("i18n.l10n.currency.brazilian.real"), (short) 20),
    BSD("BSD", "bahamianDollar", new I18nKey("i18n.l10n.currency.bahamian.dollar"), (short) 21),
    BTN("BTN", "bhutaneseNgultrum", new I18nKey("i18n.l10n.currency.bhutan.ngultrum"), (short) 22),
    BWP("BWP", "botswanaPula", new I18nKey("i18n.l10n.currency.botswana.pula"), (short) 23),
    BYN("BYN", "belarusianRuble", new I18nKey("i18n.l10n.currency.belarusian.ruble"), (short) 24),//to remove
    BYR("BYR", "newBelarusianRuble", new I18nKey("i18n.l10n.currency.new.belarusian.ruble"), (short) 25),
    BZD("BZD", "belizeDollar", new I18nKey("i18n.l10n.currency.belize.dollar"), (short) 26),
    CAD("CAD", "canadianDollar", new I18nKey("i18n.l10n.currency.canadian.dollar"), (short) 27),
    CDF("CDF", "congoleseFranc", new I18nKey("i18n.l10n.currency.congolese.franc"), (short) 28),
    CHF("CHF", "swissFranc", new I18nKey("i18n.l10n.currency.swiss.franc"), (short) 29),
    CLF("CLF", "chileanUnitOfAccount", new I18nKey("i18n.l10n.currency.chilean.unidad.de.fomento"), (short) 30),
    CLP("CLP", "chileanPeso", new I18nKey("i18n.l10n.currency.chilean.peso"), (short) 31),
    CNY("CNY", "chineseYuan", new I18nKey("i18n.l10n.currency.chinese.yuan.renminbi"), (short) 32),
    COP("COP", "colombianPeso", new I18nKey("i18n.l10n.currency.colombian.peso"), (short) 33),
    CRC("CRC", "costaRicanColón", new I18nKey("i18n.l10n.currency.costa.rican.colon"), (short) 34),
    CUC("CUC", "cubanConvertiblePeso", new I18nKey("i18n.l10n.currency.cuban.convertible.peso"), (short) 35),
    CUP("CUP", "cubanPeso", new I18nKey("i18n.l10n.currency.cuban.peso"), (short) 36),
    CVE("CVE", "capeVerdeanEscudo", new I18nKey("i18n.l10n.currency.cape.verde.escudo"), (short) 37),
    CZK("CZK", "czechKoruna", new I18nKey("i18n.l10n.currency.czech.koruna"), (short) 38),
    DJF("DJF", "djiboutianFranc", new I18nKey("i18n.l10n.currency.djibouti.franc"), (short) 39),
    DKK("DKK", "danishKrone", new I18nKey("i18n.l10n.currency.danish.krone"), (short) 40),
    DOP("DOP", "dominicanPeso", new I18nKey("i18n.l10n.currency.dominican.peso"), (short) 41),
    DZD("DZD", "algerianDinar", new I18nKey("i18n.l10n.currency.algerian.dinar"), (short) 42),
    EGP("EGP", "egyptianPound", new I18nKey("i18n.l10n.currency.egyptian.pound"), (short) 43),
    ERN("ERN", "eritreanNakfa", new I18nKey("i18n.l10n.currency.eritrea.nafka"), (short) 44),
    ETB("ETB", "ethiopianBirr", new I18nKey("i18n.l10n.currency.ethiopian.birr"), (short) 45),
    EUR("EUR", "euro", new I18nKey("i18n.l10n.currency.euro"), (short) 46),
    FJD("FJD", "fijianDollar", new I18nKey("i18n.l10n.currency.fiji.dollar"), (short) 47),
    FKP("FKP", "falklandIslandsPound", new I18nKey("i18n.l10n.currency.falkland.islands.pound"), (short) 48),
    GBP("GBP", "britishPound", new I18nKey("i18n.l10n.currency.british.pound"), (short) 49),
    GEL("GEL", "georgianLari", new I18nKey("i18n.l10n.currency.georgian.lari"), (short) 50),
    GGP("GGP", "guernseyPound", new I18nKey("i18n.l10n.currency.guernsey.pound"), (short) 51),
    GHS("GHS", "ghanaianCedi", new I18nKey("i18n.l10n.currency.ghanaian.cedi"), (short) 52),
    GIP("GIP", "gibraltarPound", new I18nKey("i18n.l10n.currency.gibraltar.pound"), (short) 53),
    GMD("GMD", "gambianDalasi", new I18nKey("i18n.l10n.currency.gambian.dalasi"), (short) 54),
    GNF("GNF", "guineanFranc", new I18nKey("i18n.l10n.currency.guinean.franc"), (short) 55),
    GTQ("GTQ", "guatemalanGuetzal", new I18nKey("i18n.l10n.currency.guatemala.quetzal"), (short) 56),
    GYD("GYD", "guyaneseDollar", new I18nKey("i18n.l10n.currency.guyana.dollar"), (short) 57),
    HKD("HKD", "hongKongDollar", new I18nKey("i18n.l10n.currency.hong.kong.dollar"), (short) 58),
    HNL("HNL", "honduranLempira", new I18nKey("i18n.l10n.currency.honduras.lempira"), (short) 59),
    HRK("HRK", "croatianKuna", new I18nKey("i18n.l10n.currency.croatian.kuna"), (short) 60),
    HTG("HTG", "haitianGourde", new I18nKey("i18n.l10n.currency.haiti.gourde"), (short) 61),
    HUF("HUF", "hungarianForint", new I18nKey("i18n.l10n.currency.hungarian.forint"), (short) 62),
    IDR("IDR", "indonesianRupiah", new I18nKey("i18n.l10n.currency.indonesian.rupiah"), (short) 63),
    ILS("ILS", "israeliShekel", new I18nKey("i18n.l10n.currency.israel.shekel"), (short) 64),
    IMP("IMP", "isleOfManPound", new I18nKey("i18n.l10n.currency.isle.of.man.pound"), (short) 65),
    INR("INR", "indianRupee", new I18nKey("i18n.l10n.currency.indian.rupee"), (short) 66),
    IQD("IQD", "iraqiDinar", new I18nKey("i18n.l10n.currency.iraqi.dinar"), (short) 67),
    IRR("IRR", "iranianRial", new I18nKey("i18n.l10n.currency.iranian.rial"), (short) 68),
    ISK("ISK", "icelandicKróna", new I18nKey("i18n.l10n.currency.iceland.krona"), (short) 69),
    JEP("JEP", "jerseyPound", new I18nKey("i18n.l10n.currency.jersey.pound"), (short) 70),
    JMD("JMD", "jamaicanDollar", new I18nKey("i18n.l10n.currency.jamaican.dollar"), (short) 71),
    JOD("JOD", "jordanianDinar", new I18nKey("i18n.l10n.currency.jordanian.dinar"), (short) 72),
    JPY("JPY", "japaneseYen", new I18nKey("i18n.l10n.currency.japanese.yen"), (short) 73),
    KES("KES", "kenyanShilling", new I18nKey("i18n.l10n.currency.kenyan.shilling"), (short) 74),
    KGS("KGS", "kyrgyzstaniSom", new I18nKey("i18n.l10n.currency.kyrgyzstan.som"), (short) 75),
    KHR("KHR", "cambodianRiel", new I18nKey("i18n.l10n.currency.cambodia.riel"), (short) 76),
    KMF("KMF", "comorianFranc", new I18nKey("i18n.l10n.currency.comorian.franc"), (short) 77),
    KPW("KPW", "northKoreanWon", new I18nKey("i18n.l10n.currency.north.korean.won"), (short) 78),
    KRW("KRW", "southKoreanWon", new I18nKey("i18n.l10n.currency.south.korean.won"), (short) 79),
    KWD("KWD", "kuwaitiDinar", new I18nKey("i18n.l10n.currency.kuwaiti.dinar"), (short) 80),
    KYD("KYD", "caymanIslandsDollar", new I18nKey("i18n.l10n.currency.cayman.islands.dollar"), (short) 81),
    KZT("KZT", "kazakhstaniTenge", new I18nKey("i18n.l10n.currency.kazakhstan.tenge"), (short) 82),
    LAK("LAK", "laoKip", new I18nKey("i18n.l10n.currency.lao.kip"), (short) 83),
    LBP("LBP", "lebanesePound", new I18nKey("i18n.l10n.currency.lebanese.pound"), (short) 84),
    LKR("LKR", "sriLankanRupee", new I18nKey("i18n.l10n.currency.sri.lanka.Rupee"), (short) 85),
    LRD("LRD", "liberianDollar", new I18nKey("i18n.l10n.currency.liberian.dollar"), (short) 86),
    LSL("LSL", "lesothoLoti", new I18nKey("i18n.l10n.currency.lesotho.loti"), (short) 87),
    LTL("LTL", "lithuanianLitas", new I18nKey("i18n.l10n.currency.lithuanian.litas"), (short) 88),
    LVL("LVL", "latvianLats", new I18nKey("i18n.l10n.currency.latvian.lats"), (short) 89),
    LYD("LYD", "libyanDinar", new I18nKey("i18n.l10n.currency.libyan.dinar"), (short) 90),
    MAD("MAD", "moroccanDirham", new I18nKey("i18n.l10n.currency.moroccan.dirham"), (short) 91),
    MDL("MDL", "moldovanLeu", new I18nKey("i18n.l10n.currency.moldovan.leu"), (short) 92),
    MGA("MGA", "malagasyAriary", new I18nKey("i18n.l10n.currency.malagasy.ariary"), (short) 93),
    MKD("MKD", "macedonianDenar", new I18nKey("i18n.l10n.currency.macedonian.denar"), (short) 94),
    MMK("MMK", "burmeseKyat", new I18nKey("i18n.l10n.currency.burmese.kyat"), (short) 95),
    MNT("MNT", "mongolianTughrik", new I18nKey("i18n.l10n.currency.mongolian.tughrik"), (short) 96),
    MOP("MOP", "macanesePataca", new I18nKey("i18n.l10n.currency.macau.pataca"), (short) 97),
    MRO("MRO", "mauritanianOuguiya", new I18nKey("i18n.l10n.currency.mauritania.ouguiya"), (short) 98),
    MUR("MUR", "mauritianRupee", new I18nKey("i18n.l10n.currency.mauritius.rupee"), (short) 99),
    MVR("MVR", "maldivianRufiyaa", new I18nKey("i18n.l10n.currency.maldivies.rufiyaa"), (short) 100),
    MWK("MWK", "malawianKwacha", new I18nKey("i18n.l10n.currency.malawi.kwacha"), (short) 101),
    MXN("MXN", "mexicanPeso", new I18nKey("i18n.l10n.currency.mexican.peso"), (short) 102),
    MYR("MYR", "malaysianRinggit", new I18nKey("i18n.l10n.currency.malaysian.ringgit"), (short) 103),
    MZN("MZN", "mozambicanMetical", new I18nKey("i18n.l10n.currency.mozambican.metical"), (short) 104),
    NAD("NAD", "namibianDollar", new I18nKey("i18n.l10n.currency.namibian.dollar"), (short) 105),
    NGN("NGN", "nigerianNaira", new I18nKey("i18n.l10n.currency.nigeria.naira"), (short) 106),
    NIO("NIO", "nicaraguanCordoba", new I18nKey("i18n.l10n.currency.nicaragua.cordoba.ora"), (short) 107),
    NOK("NOK", "norwegianKrone", new I18nKey("i18n.l10n.currency.norwegian.krone"), (short) 108),
    NPR("NPR", "nepaleseRupee", new I18nKey("i18n.l10n.currency.nepalese.rupee"), (short) 109),
    NZD("NZD", "newZealandDollar", new I18nKey("i18n.l10n.currency.new.zealand.dollar"), (short) 110),
    OMR("OMR", "omaniRial", new I18nKey("i18n.l10n.currency.oman.rial"), (short) 111),
    PAB("PAB", "pnamanianBalboa", new I18nKey("i18n.l10n.currency.panama.balboa"), (short) 112),
    PEN("PEN", "peruvianSol", new I18nKey("i18n.l10n.currency.peruvian.nuevo.sol"), (short) 113),
    PGK("PGK", "papuaNewGuineanKina", new I18nKey("i18n.l10n.currency.papua.new.guinean.kina"), (short) 114),
    PHP("PHP", "philippinePeso", new I18nKey("i18n.l10n.currency.philippine.peso"), (short) 115),
    PKR("PKR", "pakistaniRupee", new I18nKey("i18n.l10n.currency.pakistan.rupee"), (short) 116),
    PLN("PLN", "polishZloty", new I18nKey("i18n.l10n.currency.polish.zloty"), (short) 117),
    PYG("PYG", "paraguayanGuaraní", new I18nKey("i18n.l10n.currency.paraguayan.guarani"), (short) 118),
    QAR("QAR", "qatariRiyal", new I18nKey("i18n.l10n.currency.qatari.rial"), (short) 119),
    RON("RON", "romanianLeu", new I18nKey("i18n.l10n.currency.romanian.leu"), (short) 120),
    RSD("RSD", "serbianDinar", new I18nKey("i18n.l10n.currency.serbian.dinar"), (short) 121),
    RUB("RUB", "russianRuble", new I18nKey("i18n.l10n.currency.russian.ruble"), (short) 122),
    RWF("RWF", "rwandanFranc", new I18nKey("i18n.l10n.currency.rwanda.franc"), (short) 123),
    SAR("SAR", "saudiRiyal", new I18nKey("i18n.l10n.currency.saudi.riyal"), (short) 124),
    SBD("SBD", "solomonIslandsDollar", new I18nKey("i18n.l10n.currency.solomon.islands.dollar"), (short) 125),
    SCR("SCR", "seychelloisRupee", new I18nKey("i18n.l10n.currency.seychelles.rupee"), (short) 126),
    SDG("SDG", "sudanesePound", new I18nKey("i18n.l10n.currency.sudanese.pound"), (short) 127),
    SEK("SEK", "swedishKrona", new I18nKey("i18n.l10n.currency.swedish.krona"), (short) 128),
    SGD("SGD", "singaporedollar", new I18nKey("i18n.l10n.currency.singapore.dollar"), (short) 129),
    SHP("SHP", "saintHelenaPound", new I18nKey("i18n.l10n.currency.saint.helena.pound"), (short) 130),
    SLL("SLL", "sierraLeoneanLeone", new I18nKey("i18n.l10n.currency.sierra.leone.leone"), (short) 131),
    SOS("SOS", "somaliShilling", new I18nKey("i18n.l10n.currency.somali.shilling"), (short) 132),
    SRD("SRD", "surinameseDollar", new I18nKey("i18n.l10n.currency.surinam.dollar"), (short) 133),
    SSP("SSP", "southSudanesePound", new I18nKey("i18n.l10n.currency.south.sudanese.pound"), (short) 134),
    STD("STD", "saoTomeAndPrincipeDobra", new I18nKey("i18n.l10n.currency.sao.tome.and.principe.dobra"), (short) 135),
    SVC("SVC", "salvadoranColón", new I18nKey("i18n.l10n.currency.salvador.colon"), (short) 136),
    SYP("SYP", "syrianPound", new I18nKey("i18n.l10n.currency.syrian.pound"), (short) 137),
    SZL("SZL", "swaziLilangeni", new I18nKey("i18n.l10n.currency.swaziland.lilangeni"), (short) 138),
    THB("THB", "thaiBaht", new I18nKey("i18n.l10n.currency.thai.baht"), (short) 139),
    TJS("TJS", "tajikistaniSomoni", new I18nKey("i18n.l10n.currency.tajik.somoni"), (short) 140),
    TMT("TMT", "turkmenistanManat", new I18nKey("i18n.l10n.currency.turkmenistani.manat"), (short) 141),
    TND("TND", "tunisianDinar", new I18nKey("i18n.l10n.currency.tunisian.dinar"), (short) 142),
    TOP("TOP", "tonganPa'anga", new I18nKey("i18n.l10n.currency.tonga.pa'anga"), (short) 143),
    TRY("TRY", "turkishLira", new I18nKey("i18n.l10n.currency.turkish.lira"), (short) 144),
    TTD("TTD", "trinidadAndTobagoDollar", new I18nKey("i18n.l10n.currency.trinidad.and.tobago.dollar"), (short) 145),
    TWD("TWD", "newTaiwanDollar", new I18nKey("i18n.l10n.currency.new.taiwan.dollar"), (short) 146),
    TZS("TZS", "tanzanianShilling", new I18nKey("i18n.l10n.currency.tanzanian.shilling"), (short) 147),
    UAH("UAH", "ukrainianHryvnia", new I18nKey("i18n.l10n.currency.ukraine.hryvnia"), (short) 148),
    UGX("UGX", "ugandanShilling", new I18nKey("i18n.l10n.currency.uganda.shilling"), (short) 149),
    USD("USD", "unitedStatesDollar", new I18nKey("i18n.l10n.currency.united.states.dollar"), (short) 150),
    UYU("UYU", "uruguayanPeso", new I18nKey("i18n.l10n.currency.uruguayan.peso"), (short) 151),
    UZS("UZS", "uzbekistaniSom", new I18nKey("i18n.l10n.currency.uzbekistan.som"), (short) 152),
    VEF("VEF", "venezuelanBolívar", new I18nKey("i18n.l10n.currency.venezuela.bolivar"), (short) 153),
    VND("VND", "vietnameseDong", new I18nKey("i18n.l10n.currency.vietnamese.dong"), (short) 154),
    VUV("VUV", "vanuatuVatu", new I18nKey("i18n.l10n.currency.vanuatu.vatu"), (short) 155),
    WST("WST", "samoanTala", new I18nKey("i18n.l10n.currency.samoan.tala"), (short) 156),
    XAF("XAF", "centralAfricanCFAFranc", new I18nKey("i18n.l10n.currency.central.african.cfa.franc"), (short) 157),
    XAG("XAG", "silver", new I18nKey("i18n.l10n.currency.silver"), (short) 158),
    XAU("XAU", "gold", new I18nKey("i18n.l10n.currency.gold"), (short) 159),
    XCD("XCD", "eastCaribbeanDollar", new I18nKey("i18n.l10n.currency.east.caribbean.dollar"), (short) 160),
    XDR("XDR", "specialDrawingRight", new I18nKey("i18n.l10n.currency.special.drawing.right"), (short) 161),
    XOF("XOF", "westAfricanCFAFranc", new I18nKey("i18n.l10n.currency.west.african.cfa.franc"), (short) 162),
    XPF("XPF", "CFPFranc", new I18nKey("i18n.l10n.currency.cfp.franc"), (short) 163),
    YER("YER", "yemeniRial", new I18nKey("i18n.l10n.currency.yemeni.rial"), (short) 164),
    ZAR("ZAR", "southAfricanRand", new I18nKey("i18n.l10n.currency.south.african.rand"), (short) 165),
    ZMW("ZMW", "zambianKwacha", new I18nKey("i18n.l10n.currency.zambian.kwacha"), (short) 166),
    ZWL("ZWL", "zimbabweanDollar", new I18nKey("i18n.l10n.currency.zimbabwe.dollar"), (short) 167),
    ;

    private static final Collection<Currency> CURRENCIES;
    private static final Collection<String> CURRENCY_CODES;
    private static final Collection<String> AVAILABLE_CURRENCY_CODES;

    static {
        CURRENCIES = Set.of(Currency.values());
        CURRENCY_CODES = CURRENCIES.stream().map(Currency::getCode).collect(Collectors.toUnmodifiableSet());
        AVAILABLE_CURRENCY_CODES = java.util.Currency.getAvailableCurrencies().stream()
                .map(java.util.Currency::getCurrencyCode).collect(Collectors.toUnmodifiableSet());
    }

    private final I18nKey i18nKey;
    private final String name;
    private final String code;
    private final short id;

    Currency(final String code, final String name, final I18nKey i18nKey, final short id) {
        this.code = code;
        this.name = name;
        this.i18nKey = i18nKey;
        this.id = id;
    }

    public I18nKey getI18nKey() {
        return i18nKey;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public short getId() {
        return id;
    }

    public String getSymbol() {
        return getSymbol(code);
    }

    /**
     * Gets the {@link Currency} by id
     *
     * @param id represents the id of the currency
     * @return {@link Currency}
     */
    public static Currency getCurrencyById(final short id) {
        return switch (id) {
            case 1 -> AED;
            case 2 -> AFN;
            case 3 -> ALL;
            case 4 -> AMD;
            case 5 -> ANG;
            case 6 -> AOA;
            case 7 -> ARS;
            case 8 -> AUD;
            case 9 -> AWG;
            case 10 -> AZN;
            case 11 -> BAM;
            case 12 -> BBD;
            case 13 -> BDT;
            case 14 -> BGN;
            case 15 -> BHD;
            case 16 -> BIF;
            case 17 -> BMD;
            case 18 -> BND;
            case 19 -> BOB;
            case 20 -> BRL;
            case 21 -> BSD;
            case 22 -> BTN;
            case 23 -> BWP;
            case 24 -> BYN;
            case 25 -> BYR;
            case 26 -> BZD;
            case 27 -> CAD;
            case 28 -> CDF;
            case 29 -> CHF;
            case 30 -> CLF;
            case 31 -> CLP;
            case 32 -> CNY;
            case 33 -> COP;
            case 34 -> CRC;
            case 35 -> CUC;
            case 36 -> CUP;
            case 37 -> CVE;
            case 38 -> CZK;
            case 39 -> DJF;
            case 40 -> DKK;
            case 41 -> DOP;
            case 42 -> DZD;
            case 43 -> EGP;
            case 44 -> ERN;
            case 45 -> ETB;
            case 46 -> EUR;
            case 47 -> FJD;
            case 48 -> FKP;
            case 49 -> GBP;
            case 50 -> GEL;
            case 51 -> GGP;
            case 52 -> GHS;
            case 53 -> GIP;
            case 54 -> GMD;
            case 55 -> GNF;
            case 56 -> GTQ;
            case 57 -> GYD;
            case 58 -> HKD;
            case 59 -> HNL;
            case 60 -> HRK;
            case 61 -> HTG;
            case 62 -> HUF;
            case 63 -> IDR;
            case 64 -> ILS;
            case 65 -> IMP;
            case 66 -> INR;
            case 67 -> IQD;
            case 68 -> IRR;
            case 69 -> ISK;
            case 70 -> JEP;
            case 71 -> JMD;
            case 72 -> JOD;
            case 73 -> JPY;
            case 74 -> KES;
            case 75 -> KGS;
            case 76 -> KHR;
            case 77 -> KMF;
            case 78 -> KPW;
            case 79 -> KRW;
            case 80 -> KWD;
            case 81 -> KYD;
            case 82 -> KZT;
            case 83 -> LAK;
            case 84 -> LBP;
            case 85 -> LKR;
            case 86 -> LRD;
            case 87 -> LSL;
            case 88 -> LTL;
            case 89 -> LVL;
            case 90 -> LYD;
            case 91 -> MAD;
            case 92 -> MDL;
            case 93 -> MGA;
            case 94 -> MKD;
            case 95 -> MMK;
            case 96 -> MNT;
            case 97 -> MOP;
            case 98 -> MRO;
            case 99 -> MUR;
            case 100 -> MVR;
            case 101 -> MWK;
            case 102 -> MXN;
            case 103 -> MYR;
            case 104 -> MZN;
            case 105 -> NAD;
            case 106 -> NGN;
            case 107 -> NIO;
            case 108 -> NOK;
            case 109 -> NPR;
            case 110 -> NZD;
            case 111 -> OMR;
            case 112 -> PAB;
            case 113 -> PEN;
            case 114 -> PGK;
            case 115 -> PHP;
            case 116 -> PKR;
            case 117 -> PLN;
            case 118 -> PYG;
            case 119 -> QAR;
            case 120 -> RON;
            case 121 -> RSD;
            case 122 -> RUB;
            case 123 -> RWF;
            case 124 -> SAR;
            case 125 -> SBD;
            case 126 -> SCR;
            case 127 -> SDG;
            case 128 -> SEK;
            case 129 -> SGD;
            case 130 -> SHP;
            case 131 -> SLL;
            case 132 -> SOS;
            case 133 -> SRD;
            case 134 -> SSP;
            case 135 -> STD;
            case 136 -> SVC;
            case 137 -> SYP;
            case 138 -> SZL;
            case 139 -> THB;
            case 140 -> TJS;
            case 141 -> TMT;
            case 142 -> TND;
            case 143 -> TOP;
            case 144 -> TRY;
            case 145 -> TTD;
            case 146 -> TWD;
            case 147 -> TZS;
            case 148 -> UAH;
            case 149 -> UGX;
            case 150 -> USD;
            case 151 -> UYU;
            case 152 -> UZS;
            case 153 -> VEF;
            case 154 -> VND;
            case 155 -> VUV;
            case 156 -> WST;
            case 157 -> XAF;
            case 158 -> XAG;
            case 159 -> XAU;
            case 160 -> XCD;
            case 161 -> XDR;
            case 162 -> XOF;
            case 163 -> XPF;
            case 164 -> YER;
            case 165 -> ZAR;
            case 166 -> ZMW;
            case 167 -> ZWL;
            default -> throw ErrorCode.get(L10NErrorCode.INVALID_CURRENCY, "i18n.l10n.currency.invalid");
        };
    }

    /**
     * <p>
     * Gets the symbol for the provided currency code.
     * </p>
     *
     * @param code Represents the currency code
     * @return the symbol of currency
     */
    public static String getSymbol(final String code) {

        if (AVAILABLE_CURRENCY_CODES.contains(code)) {
            return java.util.Currency.getInstance(code).getSymbol();
        }

        return code;
    }

    /**
     * <p>
     * Gets the {@link Collection} of {@link Currency}.
     * </p>
     *
     * @return the {@link Collection} of {@link Currency}
     */
    public static Collection<Currency> getCurrencies() {
        return CURRENCIES;
    }

    /**
     * <p>
     * Gets the {@link Collection} of {@link Currency} codes.
     * </p>
     *
     * @return the {@link Collection} of {@link Currency} codes
     */
    public static Collection<String> getCodes() {
        return CURRENCY_CODES;
    }

    /**
     * <p>
     * Gets the optional currency based on the provided currency code
     * </p>
     *
     * @param currencyCode Represents the currency code
     * @return the optional of {@link Currency}
     */
    public static Optional<Currency> getCurrencyByCode(final String currencyCode) {
        return Optional.ofNullable(switch (currencyCode) {
            case "AED" -> AED;
            case "AFN" -> AFN;
            case "ALL" -> ALL;
            case "AMD" -> AMD;
            case "ANG" -> ANG;
            case "AOA" -> AOA;
            case "ARS" -> ARS;
            case "AUD" -> AUD;
            case "AWG" -> AWG;
            case "AZN" -> AZN;
            case "BAM" -> BAM;
            case "BBD" -> BBD;
            case "BDT" -> BDT;
            case "BGN" -> BGN;
            case "BHD" -> BHD;
            case "BIF" -> BIF;
            case "BMD" -> BMD;
            case "BND" -> BND;
            case "BOB" -> BOB;
            case "BRL" -> BRL;
            case "BSD" -> BSD;
            case "BTN" -> BTN;
            case "BWP" -> BWP;
            case "BYN" -> BYN;
            case "BYR" -> BYR;
            case "BZD" -> BZD;
            case "CAD" -> CAD;
            case "CDF" -> CDF;
            case "CHF" -> CHF;
            case "CLF" -> CLF;
            case "CLP" -> CLP;
            case "CNY" -> CNY;
            case "COP" -> COP;
            case "CRC" -> CRC;
            case "CUC" -> CUC;
            case "CUP" -> CUP;
            case "CVE" -> CVE;
            case "CZK" -> CZK;
            case "DJF" -> DJF;
            case "DKK" -> DKK;
            case "DOP" -> DOP;
            case "DZD" -> DZD;
            case "EGP" -> EGP;
            case "ERN" -> ERN;
            case "ETB" -> ETB;
            case "EUR" -> EUR;
            case "FJD" -> FJD;
            case "FKP" -> FKP;
            case "GBP" -> GBP;
            case "GEL" -> GEL;
            case "GGP" -> GGP;
            case "GHS" -> GHS;
            case "GIP" -> GIP;
            case "GMD" -> GMD;
            case "GNF" -> GNF;
            case "GTQ" -> GTQ;
            case "GYD" -> GYD;
            case "HKD" -> HKD;
            case "HNL" -> HNL;
            case "HRK" -> HRK;
            case "HTG" -> HTG;
            case "HUF" -> HUF;
            case "IDR" -> IDR;
            case "ILS" -> ILS;
            case "IMP" -> IMP;
            case "INR" -> INR;
            case "IQD" -> IQD;
            case "IRR" -> IRR;
            case "ISK" -> ISK;
            case "JEP" -> JEP;
            case "JMD" -> JMD;
            case "JOD" -> JOD;
            case "JPY" -> JPY;
            case "KES" -> KES;
            case "KGS" -> KGS;
            case "KHR" -> KHR;
            case "KMF" -> KMF;
            case "KPW" -> KPW;
            case "KRW" -> KRW;
            case "KWD" -> KWD;
            case "KYD" -> KYD;
            case "KZT" -> KZT;
            case "LAK" -> LAK;
            case "LBP" -> LBP;
            case "LKR" -> LKR;
            case "LRD" -> LRD;
            case "LSL" -> LSL;
            case "LTL" -> LTL;
            case "LVL" -> LVL;
            case "LYD" -> LYD;
            case "MAD" -> MAD;
            case "MDL" -> MDL;
            case "MGA" -> MGA;
            case "MKD" -> MKD;
            case "MMK" -> MMK;
            case "MNT" -> MNT;
            case "MOP" -> MOP;
            case "MRO" -> MRO;
            case "MUR" -> MUR;
            case "MVR" -> MVR;
            case "MWK" -> MWK;
            case "MXN" -> MXN;
            case "MYR" -> MYR;
            case "MZN" -> MZN;
            case "NAD" -> NAD;
            case "NGN" -> NGN;
            case "NIO" -> NIO;
            case "NOK" -> NOK;
            case "NPR" -> NPR;
            case "NZD" -> NZD;
            case "OMR" -> OMR;
            case "PAB" -> PAB;
            case "PEN" -> PEN;
            case "PGK" -> PGK;
            case "PHP" -> PHP;
            case "PKR" -> PKR;
            case "PLN" -> PLN;
            case "PYG" -> PYG;
            case "QAR" -> QAR;
            case "RON" -> RON;
            case "RSD" -> RSD;
            case "RUB" -> RUB;
            case "RWF" -> RWF;
            case "SAR" -> SAR;
            case "SBD" -> SBD;
            case "SCR" -> SCR;
            case "SDG" -> SDG;
            case "SEK" -> SEK;
            case "SGD" -> SGD;
            case "SHP" -> SHP;
            case "SLL" -> SLL;
            case "SOS" -> SOS;
            case "SRD" -> SRD;
            case "SSP" -> SSP;
            case "STD" -> STD;
            case "SVC" -> SVC;
            case "SYP" -> SYP;
            case "SZL" -> SZL;
            case "THB" -> THB;
            case "TJS" -> TJS;
            case "TMT" -> TMT;
            case "TND" -> TND;
            case "TOP" -> TOP;
            case "TRY" -> TRY;
            case "TTD" -> TTD;
            case "TWD" -> TWD;
            case "TZS" -> TZS;
            case "UAH" -> UAH;
            case "UGX" -> UGX;
            case "USD" -> USD;
            case "UYU" -> UYU;
            case "UZS" -> UZS;
            case "VEF" -> VEF;
            case "VND" -> VND;
            case "VUV" -> VUV;
            case "WST" -> WST;
            case "XAF" -> XAF;
            case "XAG" -> XAG;
            case "XAU" -> XAU;
            case "XCD" -> XCD;
            case "XDR" -> XDR;
            case "XOF" -> XOF;
            case "XPF" -> XPF;
            case "YER" -> YER;
            case "ZAR" -> ZAR;
            case "ZMW" -> ZMW;
            case "ZWL" -> ZWL;
            default -> null;
        });
    }
}
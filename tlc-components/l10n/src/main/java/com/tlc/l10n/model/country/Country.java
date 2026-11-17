package com.tlc.l10n.model.country;

import com.tlc.commons.code.ErrorCode;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.internal.status.L10NErrorCode;

import java.util.Optional;

/**
 * <p>
 * An enum that represents a {@link Country} data
 * </p>
 *
 * @author eswari
 * @version 1.0
 */
public enum Country {
    UNITED_ARAB_EMIRATES("+971", "United Arab Emirates", "unitedArabEmirates", Currency.AED, 1),
    AFGHANISTAN("+93", "Afghanistan", "afghanistan", Currency.AFN, 2),
    ALBANIA("+355", "Albania", "albania", Currency.ALL, 3),
    ARMENIA("+374", "Armenia", "armenia", Currency.AMD, 4),
    ANGOLA("+244", "Angola", "angola", Currency.AOA, 5),
    AUSTRALIA("+61", "Australia", "australia", Currency.AUD, 6),
    KIRIBATI("+686", "Kiribati", "kiribati", Currency.AUD, 7),
    NAURU("+674", "Nauru", "nauru", Currency.AUD, 8),
    TUVALU("+688", "Tuvalu", "tuvalu", Currency.AUD, 9),
    AZERBAIJAN("+994", "Azerbaijan", "azerbaijan", Currency.AZN, 10),
    BOSNIA_AND_HERZEGOVINA("+387", "Bosnia And Herzegovina", "bosniaAndHerzegovina", Currency.BAM, 11),
    BARBADOS("+1", "Barbados", "barbados", Currency.BBD, 12),
    BANGLADESH("+880", "Bangladesh", "bangladesh", Currency.BDT, 13),
    BULGARIA("+359", "Bulgaria", "bulgaria", Currency.BGN, 14),
    BAHRAIN("+973", "Bahrain", "bahrain", Currency.BHD, 15),
    BURUNDI("+257", "Burundi", "burundi", Currency.BIF, 16),
    BRUNEI("+673", "Brunei", "brunei", Currency.BND, 17),
    BOLIVIA("+591", "Bolivia", "bolivia", Currency.BOB, 18),
    BRAZIL("+55", "Brazil", "brazil", Currency.BRL, 19),
    BAHAMAS("+1", "Bahamas", "bahamas", Currency.BSD, 20),
    BHUTAN("+975", "Bhutan", "bhutan", Currency.BTN, 21),
    BOTSWANA("+267", "Botswana", "botswana", Currency.BWP, 22),
    BELARUS("+375", "Belarus", "belarus", Currency.BYN, 23),
    BELIZE("+501", "Belize", "belize", Currency.BZD, 24),
    CANADA("+1", "Canada", "canada", Currency.CAD, 25),
    SWITZERLAND("+41", "Switzerland", "switzerland", Currency.CHF, 26),
    LIECHTENSTEIN("+423", "Liechtenstein", "liechtenstein", Currency.CHF, 27),
    CHILE("+56", "Chile", "chile", Currency.CLP, 28),
    CHINA("+86", "China", "china", Currency.CNY, 29),
    COLOMBIA("+57", "Colombia", "colombia", Currency.COP, 30),
    CRC_COSTA_RICA("+506", "CRC Costa Rica", "crcCostaRica", Currency.CRC, 31),
    CUBA("+53", "Cuba", "cuba", Currency.CUP, 32),
    CAPE_VERDE("+238", "Cape Verde", "capeVerde", Currency.CVE, 33),
    CZECH_REPUBLIC("+420", "Czech Republic", "czechRepublic", Currency.CZK, 34),
    DJIBOUTI("+253", "Djibouti", "djibouti", Currency.DJF, 35),
    DENMARK("+45", "Denmark", "denmark", Currency.DKK, 36),
    DOMINICAN_REPUBLIC("+1", "Dominican Republic", "dominicanRepublic", Currency.DOP, 37),
    ALGERIA("+213", "Algeria", "algeria", Currency.DZD, 38),
    EGYPT("+20", "Egypt", "egypt", Currency.EGP, 39),
    ERITREA("+291", "Eritrea", "eritrea", Currency.ERN, 40),
    ETHIOPIA("+251", "Ethiopia", "ethiopia", Currency.ETB, 41),
    ANDORRA("+376", "Andorra", "andorra", Currency.EUR, 42),
    AUSTRIA("+43", "Austria", "austria", Currency.EUR, 43),
    BELGIUM("+32", "Belgium", "belgium", Currency.EUR, 44),
    CYPRUS("+357", "Cyprus", "cyprus", Currency.EUR, 45),
    ESTONIA("+372", "Estonia", "estonia", Currency.EUR, 46),
    FINLAND("+358", "Finland", "finland", Currency.EUR, 47),
    FRANCE("+33", "France", "france", Currency.EUR, 48),
    GERMANY("+49", "Germany", "germany", Currency.EUR, 49),
    GREECE("+30", "Greece", "greece", Currency.EUR, 50),
    IRELAND("+353", "Ireland", "ireland", Currency.EUR, 51),
    ITALY("+39", "Italy", "italy", Currency.EUR, 52),
    KOSOVO("+383", "Kosovo", "kosovo", Currency.EUR, 53),
    LATVIA("+371", "Latvia", "latvia", Currency.EUR, 54),
    LITHUANIA("+370", "Lithuania", "lithuania", Currency.EUR, 55),
    LUXEMBOURG("+352", "Luxembourg", "luxembourg", Currency.EUR, 56),
    MALTA("+356", "Malta", "malta", Currency.EUR, 57),
    MONACO("+377", "Monaco", "monaco", Currency.EUR, 58),
    MONTENEGRO("+382", "Montenegro", "montenegro", Currency.EUR, 59),
    NETHERLANDS("+31", "Netherlands", "netherlands", Currency.EUR, 60),
    PORTUGAL("+351", "Portugal", "portugal", Currency.EUR, 61),
    SAN_MARINO("+378", "San Marino", "sanMarino", Currency.EUR, 62),
    SLOVAKIA("+421", "Slovakia", "slovakia", Currency.EUR, 63),
    SLOVENIA("+386", "Slovenia", "slovenia", Currency.EUR, 64),
    SPAIN("+34", "Spain", "spain", Currency.EUR, 65),
    VATICAN_CITY("+379", "Vatican City", "vaticanCity", Currency.EUR, 66),
    FIJI("+679", "Fiji", "fiji", Currency.FJD, 67),
    UNITED_KINGDOM("+44", "United Kingdom", "unitedKingdom", Currency.GBP, 68),
    GEORGIA("+995", "Georgia", "georgia", Currency.GEL, 69),
    GHANA("+233", "Ghana", "ghana", Currency.GHS, 70),
    GAMBIA("+220", "Gambia", "gambia", Currency.GMD, 71),
    GUINEA("+224", "Guinea", "guinea", Currency.GNF, 72),
    GUATEMALA("+502", "Guatemala", "guatemala", Currency.GTQ, 73),
    GUYANA("+592", "Guyana", "guyana", Currency.GYD, 74),
    HONDURAS("+504", "Honduras", "honduras", Currency.HNL, 75),
    CROATIA("+385", "Croatia", "croatia", Currency.HRK, 76),
    HAITI("+509", "Haiti", "haiti", Currency.HTG, 77),
    HUNGARY("+36", "Hungary", "hungary", Currency.HUF, 78),
    INDONESIA("+62", "Indonesia", "indonesia", Currency.IDR, 79),
    ISRAEL("+972", "Israel", "israel", Currency.ILS, 80),
    PALESTINE("+970", "Palestine", "palestine", Currency.ILS, 81),
    INDIA("+91", "India", "india", Currency.INR, 82),
    IRAQ("+964", "Iraq", "iraq", Currency.IQD, 83),
    IRAN("+98", "Iran", "iran", Currency.IRR, 84),
    ICELAND("+354", "Iceland", "iceland", Currency.ISK, 85),
    JAMAICA("+1", "Jamaica", "jamaica", Currency.JMD, 86),
    JORDAN("+962", "Jordan", "jordan", Currency.JOD, 87),
    JAPAN("+81", "Japan", "japan", Currency.JPY, 88),
    KENYA("+254", "Kenya", "kenya", Currency.KES, 89),
    KYRGYZSTAN("+996", "Kyrgyzstan", "kyrgyzstan", Currency.KGS, 90),
    CAMBODIA("+855", "Cambodia", "cambodia", Currency.KHR, 91),
    COMOROS("+269", "Comoros", "comoros", Currency.KMF, 92),
    NORTH_KOREA("+850", "North Korea", "northKorea", Currency.KPW, 93),
    SOUTH_KOREA("+82", "South Korea", "southKorea", Currency.KPW, 94),
    KUWAIT("+965", "Kuwait", "kuwait", Currency.KWD, 95),
    KAZAKHSTAN("+7", "Kazakhstan", "kazakhstan", Currency.KZT, 96),
    LAOS("+856", "Laos", "laos", Currency.LAK, 97),
    LEBANON("+961", "Lebanon", "lebanon", Currency.LBP, 98),
    SRI_LANKA("+94", "Sri Lanka", "sriLanka", Currency.LKR, 99),
    LIBERIA("+231", "Liberia", "liberia", Currency.LRD, 100),
    LESOTHO("+266", "Lesotho", "lesotho", Currency.LSL, 101),
    LIBYA("+218", "Libya", "libya", Currency.LYD, 102),
    MOROCCO("+212", "Morocco", "morocco", Currency.MAD, 103),
    MOLDOVA("+373", "Moldova", "moldova", Currency.MDL, 104),
    MADAGASCAR("+261", "Madagascar", "madagascar", Currency.MGA, 105),
    NORTH_MACEDONIA("+389", "North Macedonia", "northMacedonia", Currency.MKD, 106),
    MYANMAR("+95", "Myanmar", "myanmar", Currency.MMK, 107),
    MONGOLIA("+976", "Mongolia", "mongolia", Currency.MNT, 108),
    MAURITANIA("+222", "Mauritania", "mauritania", Currency.MRO, 109),
    MAURITIUS("+230", "Mauritius", "mauritius", Currency.MUR, 110),
    MALDIVES("+960", "Maldives", "maldives", Currency.MVR, 111),
    MALAWI("+265", "Malawi", "malawi", Currency.MWK, 112),
    MEXICO("+52", "Mexico", "mexico", Currency.MXN, 113),
    MALAYSIA("+60", "Malaysia", "malaysia", Currency.MYR, 114),
    MOZAMBIQUE("+258", "Mozambique", "mozambique", Currency.MZN, 115),
    NAMIBIA("+264", "Namibia", "namibia", Currency.NAD, 116),
    NIGERIA("+234", "Nigeria", "nigeria", Currency.NGN, 117),
    NICARAGUA("+505", "Nicaragua", "nicaragua", Currency.NIO, 118),
    NORWAY("+47", "Norway", "norway", Currency.NOK, 119),
    NEPAL("+977", "Nepal", "nepal", Currency.NPR, 120),
    NEW_ZEALAND("+64", "New Zealand", "newZealand", Currency.NZD, 121),
    OMAN("+968", "Oman", "oman", Currency.OMR, 122),
    PANAMA("+507", "Panama", "panama", Currency.PAB, 123),
    PERU("+51", "Peru", "peru", Currency.PEN, 124),
    PAPUA_NEW_GUINEA("+675", "Papua New Guinea", "papuaNewGuinea", Currency.PGK, 125),
    PHILIPPINES("+63", "Philippines", "philippines", Currency.PHP, 126),
    PAKISTAN("+92", "Pakistan", "pakistan", Currency.PKR, 127),
    POLAND("+48", "Poland", "poland", Currency.PLN, 128),
    PARAGUAY("+595", "Paraguay", "paraguay", Currency.PYG, 129),
    QATAR("+974", "Qatar", "qatar", Currency.QAR, 130),
    ROMANIA("+40", "Romania", "romania", Currency.RON, 131),
    SERBIA("+381", "Serbia", "serbia", Currency.RSD, 132),
    RUSSIA("+7", "Russia", "russia", Currency.RUB, 133),
    RWANDA("+250", "Rwanda", "rwanda", Currency.RWF, 134),
    SAUDI_ARABIA("+966", "Saudi Arabia", "saudiArabia", Currency.SAR, 135),
    SOLOMON_ISLANDS("+677", "Solomon Islands", "solomonIslands", Currency.SBD, 136),
    SEYCHELLES("+248", "Seychelles", "seychelles", Currency.SCR, 137),
    SUDAN("+249", "Sudan", "sudan", Currency.SDG, 138),
    SWEDEN("+46", "Sweden", "sweden", Currency.SEK, 139),
    SINGAPORE("+65", "Singapore", "singapore", Currency.SGD, 140),
    SIERRA_LEONE("+232", "Sierra Leone", "sierraLeone", Currency.SLL, 141),
    SOMALIA("+252", "Somalia", "somalia", Currency.SOS, 142),
    SURINAME("+597", "Suriname", "suriname", Currency.SRD, 143),
    SOUTH_SUDAN("+1", "South Sudan", "southSudan", Currency.SSP, 144),
    SAO_TOME_AND_PRINCIPE("+239", "Sao Tome and Principe", "saoTomeAndPrincipe", Currency.STD, 145),
    SYRIA("+963", "Syria", "syria", Currency.SYP, 146),
    ESWATINI("+268", "Eswatini", "eswatini", Currency.SZL, 147),
    THAILAND("+66", "Thailand", "thailand", Currency.THB, 148),
    TAJIKISTAN("+992", "Tajikistan", "tajikistan", Currency.TJS, 149),
    TURKMENISTAN("+993", "Turkmenistan", "turkmenistan", Currency.TMT, 150),
    TUNISIA("+216", "Tunisia", "tunisia", Currency.TND, 151),
    TONGA("+676", "Tonga", "tonga", Currency.TOP, 152),
    TURKEY("+90", "Turkey", "turkey", Currency.TRY, 153),
    TRINIDAD_AND_TOBAGO("+1", "Trinidad and Tobago", "trinidadAndTobago", Currency.TTD, 154),
    TAIWAN("+886", "Taiwan", "taiwan", Currency.TWD, 155),
    TANZANIA("+255", "Tanzania", "tanzania", Currency.TZS, 156),
    UKRAINE("+380", "Ukraine", "ukraine", Currency.UAH, 157),
    UGANDA("+256", "Uganda", "uganda", Currency.UGX, 158),
    EAST_TIMOR("+670", "East Timor", "eastTimor", Currency.USD, 159),
    ECUADOR("+593", "Ecuador", "ecuador", Currency.USD, 160),
    EL_SALVADOR("+503", "El Salvador", "elSalvador", Currency.USD, 161),
    MARSHALL_ISLANDS("+692", "Marshall Islands", "marshallIslands", Currency.USD, 162),
    MICRONESIA("+691", "Micronesia", "micronesia", Currency.USD, 163),
    PALAU("+680", "Palau", "palau", Currency.USD, 164),
    UNITED_STATES("+1", "United States", "unitedStates", Currency.USD, 165),
    ZIMBABWE("+263", "Zimbabwe", "zimbabwe", Currency.USD, 166),
    URUGUAY("+598", "Uruguay", "uruguay", Currency.UYU, 167),
    UZBEKISTAN("+998", "Uzbekistan", "uzbekistan", Currency.UZS, 168),
    VENEZUELA("+58", "Venezuela", "venezuela", Currency.VEF, 169),
    VIETNAM("+84", "Vietnam", "vietnam", Currency.VND, 170),
    VANUATU("+678", "Vanuatu", "vanuatu", Currency.VUV, 171),
    SAMOA("+685", "Samoa", "samoa", Currency.WST, 172),
    CAMEROON("+237", "Cameroon", "cameroon", Currency.XAF, 173),
    CENTRAL_AFRICAN_REPUBLIC("+236", "Central African Republic", "centralAfricanRepublic", Currency.XAF, 174),
    CHAD("+235", "Chad", "chad", Currency.XAF, 175),
    EQUATORIAL_GUINEA("+240", "Equatorial Guinea", "equatorialGuinea", Currency.XAF, 176),
    GABON("+241", "Gabon", "gabon", Currency.XAF, 177),
    REPUBLIC_OF_THE_CONGO("+242", "Republic of the Congo", "republicOfTheCongo", Currency.XAF, 178),
    ANTIGUA_AND_BARBUDA("+1", "Antigua and Barbuda", "antiguaAndBarbuda", Currency.XCD, 179),
    DOMINICA("+1", "Dominica", "dominica", Currency.XCD, 180),
    GRENADA("+1", "Grenada", "grenada", Currency.XCD, 181),
    SAINT_KITTS_AND_NEVIS("+1", "Saint Kitts and Nevis", "saintKittsAndNevis", Currency.XCD, 182),
    SAINT_LUCIA("+1", "Saint Lucia", "saintLucia", Currency.XCD, 183),
    SAINT_VINCENT_AND_THE_GRENADINES("+1", "Saint Vincent and the Grenadines", "saintVincentAndTheGrenadines", Currency.XCD, 184),
    BENIN("+229", "Benin", "benin", Currency.XOF, 185),
    BURKINA_FASO("+226", "Burkina Faso", "burkinaFaso", Currency.XOF, 186),
    GUINEA_BISSAU("+245", "Guinea-Bissau", "guineaBissau", Currency.XOF, 187),
    IVORY_COAST("+225", "Ivory Coast", "ivoryCoast", Currency.XOF, 188),
    MALI("+223", "Mali", "mali", Currency.XOF, 189),
    NIGER("+227", "Niger", "niger", Currency.XOF, 190),
    SENEGAL("+221", "Senegal", "senegal", Currency.XOF, 191),
    TOGO("+228", "Togo", "togo", Currency.XOF, 192),
    YEMEN("+967", "Yemen", "yemen", Currency.YER, 193),
    SOUTH_AFRICA("+27", "South Africa", "southAfrica", Currency.ZAR, 194),
    ZAMBIA("+260", "Zambia", "zambia", Currency.ZMW, 195);

    private final String aliasName;
    private final String code;
    private final Currency currency;
    private final int id;
    private final String name;

    Country(final String code, final String name, final String aliasName, final Currency currency, final int id) {
        this.aliasName = aliasName;
        this.code = code;
        this.currency = currency;
        this.id = id;
        this.name = name;
    }

    public String getAliasName() {
        return aliasName;
    }

    public String getCode() {
        return code;
    }

    public Currency getCurrency() {
        return currency;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Gets the {@link Country} by id
     *
     * @param id represents the id of the country
     * @return {@link Country}
     */
    public static Country getCountryById(final int id) {
        return switch (id) {
            case 1 -> UNITED_ARAB_EMIRATES;
            case 2 -> AFGHANISTAN;
            case 3 -> ALBANIA;
            case 4 -> ARMENIA;
            case 5 -> ANGOLA;
            case 6 -> AUSTRALIA;
            case 7 -> KIRIBATI;
            case 8 -> NAURU;
            case 9 -> TUVALU;
            case 10 -> AZERBAIJAN;
            case 11 -> BOSNIA_AND_HERZEGOVINA;
            case 12 -> BARBADOS;
            case 13 -> BANGLADESH;
            case 14 -> BULGARIA;
            case 15 -> BAHRAIN;
            case 16 -> BURUNDI;
            case 17 -> BRUNEI;
            case 18 -> BOLIVIA;
            case 19 -> BRAZIL;
            case 20 -> BAHAMAS;
            case 21 -> BHUTAN;
            case 22 -> BOTSWANA;
            case 23 -> BELARUS;
            case 24 -> BELIZE;
            case 25 -> CANADA;
            case 26 -> SWITZERLAND;
            case 27 -> LIECHTENSTEIN;
            case 28 -> CHILE;
            case 29 -> CHINA;
            case 30 -> COLOMBIA;
            case 31 -> CRC_COSTA_RICA;
            case 32 -> CUBA;
            case 33 -> CAPE_VERDE;
            case 34 -> CZECH_REPUBLIC;
            case 35 -> DJIBOUTI;
            case 36 -> DENMARK;
            case 37 -> DOMINICAN_REPUBLIC;
            case 38 -> ALGERIA;
            case 39 -> EGYPT;
            case 40 -> ERITREA;
            case 41 -> ETHIOPIA;
            case 42 -> ANDORRA;
            case 43 -> AUSTRIA;
            case 44 -> BELGIUM;
            case 45 -> CYPRUS;
            case 46 -> ESTONIA;
            case 47 -> FINLAND;
            case 48 -> FRANCE;
            case 49 -> GERMANY;
            case 50 -> GREECE;
            case 51 -> IRELAND;
            case 52 -> ITALY;
            case 53 -> KOSOVO;
            case 54 -> LATVIA;
            case 55 -> LITHUANIA;
            case 56 -> LUXEMBOURG;
            case 57 -> MALTA;
            case 58 -> MONACO;
            case 59 -> MONTENEGRO;
            case 60 -> NETHERLANDS;
            case 61 -> PORTUGAL;
            case 62 -> SAN_MARINO;
            case 63 -> SLOVAKIA;
            case 64 -> SLOVENIA;
            case 65 -> SPAIN;
            case 66 -> VATICAN_CITY;
            case 67 -> FIJI;
            case 68 -> UNITED_KINGDOM;
            case 69 -> GEORGIA;
            case 70 -> GHANA;
            case 71 -> GAMBIA;
            case 72 -> GUINEA;
            case 73 -> GUATEMALA;
            case 74 -> GUYANA;
            case 75 -> HONDURAS;
            case 76 -> CROATIA;
            case 77 -> HAITI;
            case 78 -> HUNGARY;
            case 79 -> INDONESIA;
            case 80 -> ISRAEL;
            case 81 -> PALESTINE;
            case 82 -> INDIA;
            case 83 -> IRAQ;
            case 84 -> IRAN;
            case 85 -> ICELAND;
            case 86 -> JAMAICA;
            case 87 -> JORDAN;
            case 88 -> JAPAN;
            case 89 -> KENYA;
            case 90 -> KYRGYZSTAN;
            case 91 -> CAMBODIA;
            case 92 -> COMOROS;
            case 93 -> NORTH_KOREA;
            case 94 -> SOUTH_KOREA;
            case 95 -> KUWAIT;
            case 96 -> KAZAKHSTAN;
            case 97 -> LAOS;
            case 98 -> LEBANON;
            case 99 -> SRI_LANKA;
            case 100 -> LIBERIA;
            case 101 -> LESOTHO;
            case 102 -> LIBYA;
            case 103 -> MOROCCO;
            case 104 -> MOLDOVA;
            case 105 -> MADAGASCAR;
            case 106 -> NORTH_MACEDONIA;
            case 107 -> MYANMAR;
            case 108 -> MONGOLIA;
            case 109 -> MAURITANIA;
            case 110 -> MAURITIUS;
            case 111 -> MALDIVES;
            case 112 -> MALAWI;
            case 113 -> MEXICO;
            case 114 -> MALAYSIA;
            case 115 -> MOZAMBIQUE;
            case 116 -> NAMIBIA;
            case 117 -> NIGERIA;
            case 118 -> NICARAGUA;
            case 119 -> NORWAY;
            case 120 -> NEPAL;
            case 121 -> NEW_ZEALAND;
            case 122 -> OMAN;
            case 123 -> PANAMA;
            case 124 -> PERU;
            case 125 -> PAPUA_NEW_GUINEA;
            case 126 -> PHILIPPINES;
            case 127 -> PAKISTAN;
            case 128 -> POLAND;
            case 129 -> PARAGUAY;
            case 130 -> QATAR;
            case 131 -> ROMANIA;
            case 132 -> SERBIA;
            case 133 -> RUSSIA;
            case 134 -> RWANDA;
            case 135 -> SAUDI_ARABIA;
            case 136 -> SOLOMON_ISLANDS;
            case 137 -> SEYCHELLES;
            case 138 -> SUDAN;
            case 139 -> SWEDEN;
            case 140 -> SINGAPORE;
            case 141 -> SIERRA_LEONE;
            case 142 -> SOMALIA;
            case 143 -> SURINAME;
            case 144 -> SOUTH_SUDAN;
            case 145 -> SAO_TOME_AND_PRINCIPE;
            case 146 -> SYRIA;
            case 147 -> ESWATINI;
            case 148 -> THAILAND;
            case 149 -> TAJIKISTAN;
            case 150 -> TURKMENISTAN;
            case 151 -> TUNISIA;
            case 152 -> TONGA;
            case 153 -> TURKEY;
            case 154 -> TRINIDAD_AND_TOBAGO;
            case 155 -> TAIWAN;
            case 156 -> TANZANIA;
            case 157 -> UKRAINE;
            case 158 -> UGANDA;
            case 159 -> EAST_TIMOR;
            case 160 -> ECUADOR;
            case 161 -> EL_SALVADOR;
            case 162 -> MARSHALL_ISLANDS;
            case 163 -> MICRONESIA;
            case 164 -> PALAU;
            case 165 -> UNITED_STATES;
            case 166 -> ZIMBABWE;
            case 167 -> URUGUAY;
            case 168 -> UZBEKISTAN;
            case 169 -> VENEZUELA;
            case 170 -> VIETNAM;
            case 171 -> VANUATU;
            case 172 -> SAMOA;
            case 173 -> CAMEROON;
            case 174 -> CENTRAL_AFRICAN_REPUBLIC;
            case 175 -> CHAD;
            case 176 -> EQUATORIAL_GUINEA;
            case 177 -> GABON;
            case 178 -> REPUBLIC_OF_THE_CONGO;
            case 179 -> ANTIGUA_AND_BARBUDA;
            case 180 -> DOMINICA;
            case 181 -> GRENADA;
            case 182 -> SAINT_KITTS_AND_NEVIS;
            case 183 -> SAINT_LUCIA;
            case 184 -> SAINT_VINCENT_AND_THE_GRENADINES;
            case 185 -> BENIN;
            case 186 -> BURKINA_FASO;
            case 187 -> GUINEA_BISSAU;
            case 188 -> IVORY_COAST;
            case 189 -> MALI;
            case 190 -> NIGER;
            case 191 -> SENEGAL;
            case 192 -> TOGO;
            case 193 -> YEMEN;
            case 194 -> SOUTH_AFRICA;
            case 195 -> ZAMBIA;
            default -> throw ErrorCode.get(L10NErrorCode.INVALID_COUNTRY, "i18n.l10n.country.invalid");
        };
    }

    /**
     * <p>
     * Gets the {@link Country} by using name/ISO code.
     * </p>
     *
     * @param name represents the name/ISO code of the country.
     * @return {@link Country}.
     */
    public static Optional<Country> getCountryByName(final String name) {
        return Optional.ofNullable(switch (name.toUpperCase()) {
            case "AF", "AFG", "AFGHANISTAN" -> AFGHANISTAN;
            case "AL", "ALB", "ALBANIA" -> ALBANIA;
            case "DZ", "DZA", "ALGERIA" -> ALGERIA;
            case "AD", "AND", "ANDORRA" -> ANDORRA;
            case "AO", "AGO", "ANGOLA" -> ANGOLA;
            case "AG", "ATG", "ANTIGUA_AND_BARBUDA", "ANTIGUAANDBARBUDA" -> ANTIGUA_AND_BARBUDA;
            case "XK", "XKX", "KOSOVO" -> KOSOVO;
            case "AM", "ARM", "ARMENIA" -> ARMENIA;
            case "AU", "AUS", "AUSTRALIA" -> AUSTRALIA;
            case "AT", "AUT", "AUSTRIA" -> AUSTRIA;
            case "AZ", "AZE", "AZERBAIJAN" -> AZERBAIJAN;
            case "BS", "BHS", "BAHAMAS" -> BAHAMAS;
            case "BH", "BHR", "BAHRAIN" -> BAHRAIN;
            case "BD", "BGD", "BANGLADESH" -> BANGLADESH;
            case "BB", "BRB", "BARBADOS" -> BARBADOS;
            case "BY", "BLR", "BELARUS" -> BELARUS;
            case "BE", "BEL", "BELGIUM" -> BELGIUM;
            case "BZ", "BLZ", "BELIZE" -> BELIZE;
            case "BJ", "BEN", "BENIN" -> BENIN;
            case "BT", "BTN", "BHUTAN" -> BHUTAN;
            case "BO", "BOL", "BOLIVIA" -> BOLIVIA;
            case "BA", "BIH", "BOSNIA_AND_HERZEGOVINA", "BOSNIAANDHERZEGOVINA" -> BOSNIA_AND_HERZEGOVINA;
            case "BW", "BWA", "BOTSWANA" -> BOTSWANA;
            case "BR", "BRA", "BRAZIL" -> BRAZIL;
            case "BN", "BRN", "BRUNEI" -> BRUNEI;
            case "BG", "BGR", "BULGARIA" -> BULGARIA;
            case "BF", "BFA", "BURKINA_FASO", "BURKINAFASO" -> BURKINA_FASO;
            case "BI", "BDI", "BURUNDI" -> BURUNDI;
            case "CV", "CPV", "CAPE_VERDE", "CAPEVERDE" -> CAPE_VERDE;
            case "KH", "KHM", "CAMBODIA" -> CAMBODIA;
            case "CM", "CMR", "CAMEROON" -> CAMEROON;
            case "CA", "CAN", "CANADA" -> CANADA;
            case "CF", "CAF", "CENTRAL_AFRICAN_REPUBLIC", "CENTRALAFRICANREPUBLIC" -> CENTRAL_AFRICAN_REPUBLIC;
            case "TD", "TCD", "CHAD" -> CHAD;
            case "CL", "CHL", "CHILE" -> CHILE;
            case "CN", "CHN", "CHINA" -> CHINA;
            case "CO", "COL", "COLOMBIA" -> COLOMBIA;
            case "KM", "COM", "COMOROS" -> COMOROS;
            case "CD", "COD", "REPUBLIC_OF_THE_CONGO", "REPUBLICOFTHECONGO" -> REPUBLIC_OF_THE_CONGO;
            case "CR", "CRI", "CRC_COSTA_RICA", "CRCCOSTARICA" -> CRC_COSTA_RICA;
            case "HR", "HRV", "CROATIA" -> CROATIA;
            case "CU", "CUB", "CUBA" -> CUBA;
            case "CY", "CYP", "CYPRUS" -> CYPRUS;
            case "CZ", "CZE", "CZECH_REPUBLIC", "CZECHREPUBLIC" -> CZECH_REPUBLIC;
            case "TW", "TWN", "TAIWAN" -> TAIWAN;
            case "DK", "DNK", "DENMARK" -> DENMARK;
            case "DJ", "DJI", "DJIBOUTI" -> DJIBOUTI;
            case "DM", "DMA", "DOMINICA" -> DOMINICA;
            case "DO", "DOM", "DOMINICAN_REPUBLIC", "DOMINICANREPUBLIC" -> DOMINICAN_REPUBLIC;
            case "TL", "TLS", "EAST_TIMOR", "EASTTIMOR" -> EAST_TIMOR;
            case "EC", "ECU", "ECUADOR" -> ECUADOR;
            case "EG", "EGY", "EGYPT" -> EGYPT;
            case "SV", "SLV", "EL_SALVADOR", "ELSALVADOR" -> EL_SALVADOR;
            case "GQ", "GNQ", "EQUATORIAL_GUINEA", "EQUATORIALGUINEA" -> EQUATORIAL_GUINEA;
            case "ER", "ERI", "ERITREA" -> ERITREA;
            case "EE", "EST", "ESTONIA" -> ESTONIA;
            case "SZ", "SWZ", "ESWATINI" -> ESWATINI;
            case "ET", "ETH", "ETHIOPIA" -> ETHIOPIA;
            case "FJ", "FJI", "FIJI" -> FIJI;
            case "FI", "FIN", "FINLAND" -> FINLAND;
            case "FR", "FRA", "FRANCE" -> FRANCE;
            case "GA", "GAB", "GABON" -> GABON;
            case "GM", "GMB", "GAMBIA" -> GAMBIA;
            case "GE", "GEO", "GEORGIA" -> GEORGIA;
            case "DE", "DEU", "GERMANY" -> GERMANY;
            case "GH", "GHA", "GHANA" -> GHANA;
            case "GR", "GRC", "GREECE" -> GREECE;
            case "GD", "GRD", "GRENADA" -> GRENADA;
            case "GT", "GTM", "GUATEMALA" -> GUATEMALA;
            case "GN", "GIN", "GUINEA" -> GUINEA;
            case "GW", "GNB", "GUINEA_BISSAU", "GUINEABISSAU" -> GUINEA_BISSAU;
            case "GY", "GUY", "GUYANA" -> GUYANA;
            case "HT", "HTI", "HAITI" -> HAITI;
            case "HN", "HND", "HONDURAS" -> HONDURAS;
            case "HU", "HUN", "HUNGARY" -> HUNGARY;
            case "IS", "ISL", "ICELAND" -> ICELAND;
            case "IN", "IND", "INDIA" -> INDIA;
            case "ID", "IDN", "INDONESIA" -> INDONESIA;
            case "IR", "IRN", "IRAN" -> IRAN;
            case "IQ", "IRQ", "IRAQ" -> IRAQ;
            case "IE", "IRL", "IRELAND" -> IRELAND;
            case "IL", "ISR", "ISRAEL" -> ISRAEL;
            case "IT", "ITA", "ITALY" -> ITALY;
            case "CI", "CIV", "IVORY_COAST", "IVORYCOAST" -> IVORY_COAST;
            case "JM", "JAM", "JAMAICA" -> JAMAICA;
            case "JP", "JPN", "JAPAN" -> JAPAN;
            case "JO", "JOR", "JORDAN" -> JORDAN;
            case "KZ", "KAZ", "KAZAKHSTAN" -> KAZAKHSTAN;
            case "KE", "KEN", "KENYA" -> KENYA;
            case "KI", "KIR", "KIRIBATI" -> KIRIBATI;
            case "KW", "KWT", "KUWAIT" -> KUWAIT;
            case "KG", "KGZ", "KYRGYZSTAN" -> KYRGYZSTAN;
            case "LA", "LAO", "LAOS" -> LAOS;
            case "LV", "LVA", "LATVIA" -> LATVIA;
            case "LB", "LBN", "LEBANON" -> LEBANON;
            case "LS", "LSO", "LESOTHO" -> LESOTHO;
            case "LR", "LBR", "LIBERIA" -> LIBERIA;
            case "LY", "LBY", "LIBYA" -> LIBYA;
            case "LI", "LIE", "LIECHTENSTEIN" -> LIECHTENSTEIN;
            case "LT", "LTU", "LITHUANIA" -> LITHUANIA;
            case "LU", "LUX", "LUXEMBOURG" -> LUXEMBOURG;
            case "MG", "MDG", "MADAGASCAR" -> MADAGASCAR;
            case "MW", "MWI", "MALAWI" -> MALAWI;
            case "MY", "MYS", "MALAYSIA" -> MALAYSIA;
            case "MV", "MDV", "MALDIVES" -> MALDIVES;
            case "ML", "MLI", "MALI" -> MALI;
            case "MT", "MLT", "MALTA" -> MALTA;
            case "MH", "MHL", "MARSHALL_ISLANDS", "MARSHALLISLANDS" -> MARSHALL_ISLANDS;
            case "MR", "MRT", "MAURITANIA" -> MAURITANIA;
            case "MU", "MUS", "MAURITIUS" -> MAURITIUS;
            case "MX", "MEX", "MEXICO" -> MEXICO;
            case "FM", "FSM", "MICRONESIA" -> MICRONESIA;
            case "MD", "MDA", "MOLDOVA" -> MOLDOVA;
            case "MC", "MCO", "MONACO" -> MONACO;
            case "MN", "MNG", "MONGOLIA" -> MONGOLIA;
            case "ME", "MNE", "MONTENEGRO" -> MONTENEGRO;
            case "MA", "MAR", "MOROCCO" -> MOROCCO;
            case "MZ", "MOZ", "MOZAMBIQUE" -> MOZAMBIQUE;
            case "MM", "MMR", "MYANMAR" -> MYANMAR;
            case "NA", "NAM", "NAMIBIA" -> NAMIBIA;
            case "NR", "NRU", "NAURU" -> NAURU;
            case "NP", "NPL", "NEPAL" -> NEPAL;
            case "NL", "NLD", "NETHERLANDS" -> NETHERLANDS;
            case "NZ", "NZL", "NEW_ZEALAND", "NEWZEALAND" -> NEW_ZEALAND;
            case "NI", "NIC", "NICARAGUA" -> NICARAGUA;
            case "NE", "NER", "NIGER" -> NIGER;
            case "NG", "NGA", "NIGERIA" -> NIGERIA;
            case "KP", "PRK", "NORTH_KOREA", "NORTHKOREA" -> NORTH_KOREA;
            case "MK", "MKD", "NORTH_MACEDONIA", "NORTHMACEDONIA" -> NORTH_MACEDONIA;
            case "NO", "NOR", "NORWAY" -> NORWAY;
            case "OM", "OMN", "OMAN" -> OMAN;
            case "PK", "PAK", "PAKISTAN" -> PAKISTAN;
            case "PW", "PLW", "PALAU" -> PALAU;
            case "PS", "PSE", "PALESTINE" -> PALESTINE;
            case "PA", "PAN", "PANAMA" -> PANAMA;
            case "PG", "PNG", "PAPUA_NEW_GUINEA", "PAPUANEWGUINEA" -> PAPUA_NEW_GUINEA;
            case "PY", "PRY", "PARAGUAY" -> PARAGUAY;
            case "PE", "PER", "PERU" -> PERU;
            case "PH", "PHL", "PHILIPPINES" -> PHILIPPINES;
            case "PL", "POL", "POLAND" -> POLAND;
            case "PT", "PRT", "PORTUGAL" -> PORTUGAL;
            case "QA", "QAT", "QATAR" -> QATAR;
            case "RO", "ROU", "ROMANIA" -> ROMANIA;
            case "RU", "RUS", "RUSSIA" -> RUSSIA;
            case "RW", "RWA", "RWANDA" -> RWANDA;
            case "KN", "KNA", "SAINT_KITTS_AND_NEVIS", "SAINTKITTSANDNEVIS" -> SAINT_KITTS_AND_NEVIS;
            case "LC", "LCA", "SAINT_LUCIA", "SAINTLUCIA" -> SAINT_LUCIA;
            case "VC", "VCT", "SAINT_VINCENT_AND_THE_GRENADINES", "SAINTVINCENTANDTHEGRENADINES" -> SAINT_VINCENT_AND_THE_GRENADINES;
            case "WS", "WSM", "SAMOA" -> SAMOA;
            case "SM", "SMR", "SAN_MARINO", "SANMARINO" -> SAN_MARINO;
            case "ST", "STP", "SAO_TOME_AND_PRINCIPE", "SAOTOMEANDPRINCIPE" -> SAO_TOME_AND_PRINCIPE;
            case "SA", "SAU", "SAUDI_ARABIA", "SAUDIARABIA" -> SAUDI_ARABIA;
            case "SN", "SEN", "SENEGAL" -> SENEGAL;
            case "RS", "SRB", "SERBIA" -> SERBIA;
            case "SC", "SYC", "SEYCHELLES" -> SEYCHELLES;
            case "SL", "SLE", "SIERRA_LEONE", "SIERRALEONE" -> SIERRA_LEONE;
            case "SG", "SGP", "SINGAPORE" -> SINGAPORE;
            case "SK", "SVK", "SLOVAKIA" -> SLOVAKIA;
            case "SI", "SVN", "SLOVENIA" -> SLOVENIA;
            case "SB", "SLB", "SOLOMON_ISLANDS", "SOLOMONISLANDS" -> SOLOMON_ISLANDS;
            case "SO", "SOM", "SOMALIA" -> SOMALIA;
            case "ZA", "ZAF", "SOUTH_AFRICA", "SOUTHAFRICA" -> SOUTH_AFRICA;
            case "KR", "KOR", "SOUTH_KOREA", "SOUTHKOREA" -> SOUTH_KOREA;
            case "SS", "SSD", "SOUTH_SUDAN", "SOUTHSUDAN" -> SOUTH_SUDAN;
            case "ES", "ESP", "SPAIN" -> SPAIN;
            case "LK", "LKA", "SRI_LANKA", "SRILANKA" -> SRI_LANKA;
            case "SD", "SDN", "SUDAN" -> SUDAN;
            case "SR", "SUR", "SURINAME" -> SURINAME;
            case "SE", "SWE", "SWEDEN" -> SWEDEN;
            case "CH", "CHE", "SWITZERLAND" -> SWITZERLAND;
            case "SY", "SYR", "SYRIA" -> SYRIA;
            case "TJ", "TJK", "TAJIKISTAN" -> TAJIKISTAN;
            case "TZ", "TZA", "TANZANIA" -> TANZANIA;
            case "TH", "THA", "THAILAND" -> THAILAND;
            case "TG", "TGO", "TOGO" -> TOGO;
            case "TO", "TON", "TONGA" -> TONGA;
            case "TT", "TTO", "TRINIDAD_AND_TOBAGO", "TRINIDADANDTOBAGO" -> TRINIDAD_AND_TOBAGO;
            case "TN", "TUN", "TUNISIA" -> TUNISIA;
            case "TR", "TUR", "TURKEY" -> TURKEY;
            case "TM", "TKM", "TURKMENISTAN" -> TURKMENISTAN;
            case "TV", "TUV", "TUVALU" -> TUVALU;
            case "UG", "UGA", "UGANDA" -> UGANDA;
            case "UA", "UKR", "UKRAINE" -> UKRAINE;
            case "AE", "ARE", "UNITED_ARAB_EMIRATES", "UNITEDARABEMIRATES" -> UNITED_ARAB_EMIRATES;
            case "GB", "GBR", "UNITED_KINGDOM", "UNITEDKINGDOM" -> UNITED_KINGDOM;
            case "US", "USA", "UNITED_STATES", "UNITEDSTATES" -> UNITED_STATES;
            case "UY", "URY", "URUGUAY" -> URUGUAY;
            case "UZ", "UZB", "UZBEKISTAN" -> UZBEKISTAN;
            case "VU", "VUT", "VANUATU" -> VANUATU;
            case "VA", "VAT", "VATICAN_CITY", "VATICANCITY" -> VATICAN_CITY;
            case "VE", "VEN", "VENEZUELA" -> VENEZUELA;
            case "VN", "VNM", "VIETNAM" -> VIETNAM;
            case "YE", "YEM", "YEMEN" -> YEMEN;
            case "ZM", "ZMB", "ZAMBIA" -> ZAMBIA;
            case "ZW", "ZWE", "ZIMBABWE" -> ZIMBABWE;
            default -> null;
        });
    }
}

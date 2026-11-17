package com.tlc.crm.core.dropdown.company;

import com.tlc.crm.entity.dropdown.DefaultOption;

import java.util.Arrays;
import java.util.Collection;

/**
 * An enum contains the various the {@link IndustryType} data
 *
 * @author Kishore J
 * @version 1.0
 */
public enum IndustryType implements DefaultOption {

    ACCOUNTING("Accounting"),
    ADVERTISING("Advertising "),
    AEROSPACE(" Aerospace"),
    AGRICULTURE("Agriculture"),
    AIRCRAFT("Aircraft"),
    AIRLINE("Airline"),
    APPAREL_ACCESSORIES("Apparel & Accessories"),
    AUTOMOTIVE("Automotive"),
    BANKING("Banking"),
    BIOTECHNOLOGY("Biotechnology"),
    BROADCASTING("Broadcasting"),
    BROKERAGE("Brokerage"),
    CALL_CENTERS("Call Centers"),
    CARGO_HANDLING("Cargo Handling"),
    CHEMICALS("Chemicals"),
    COMPUTER("Computer"),
    CONSTRUCTION("Construction"),
    CONSULTING("Consulting"),
    CONSUMER_PRODUCTS("Consumer Products"),
    COSMETICS("Cosmetics"),
    DEFENSE("Defense"),
    DEPARTMENT_STORES("Department Stores"),
    ECOMMERCE("Ecommerce"),
    EDUCATION("Education"),
    ELECTRONICS("Electronics"),
    ENERGY("Energy"),
    ENGINEERING("Engineering"),
    ENTERTAINMENT("Entertainment & Leisure"),
    EXECUTIVE_SEARCH(" Executive Search "),
    FINANCIAL_SERVICES("Financial Services"),
    FOOD_BEVERAGE_TOBACCO("Food, Beverage & Tobacco"),
    GAMING("Gaming"),
    GOVERNMENT("Government"),
    GROCERY("Grocery"),
    HEALTH_CARE("Health Care"),
    HOSPITALITY("Hospitality"),
    INSURANCE("Insurance"),
    INTERNET_PUBLISHING("Internet Publishing"),
    INVESTMENT_BANKING("Investment Banking"),
    LEGAL("Legal"),
    MACHINERY("Machinery"),
    MANUFACTURING("Manufacturing"),
    MEDIA("Media"),
    MOTION_PICTURE_VIDEO("Motion Picture & Video"),
    MUSIC("Music"),
    NEWSPAPER_PUBLISHERS("Newspaper Publishers"),
    NOT_FOR_PROFIT("Not for Profit"),
    ONLINE_AUCTIONS("Online Auctions"),
    OTHER("Other"),
    PENSION_FUNDS("Pension Funds"),
    PHARMACEUTICALS("Pharmaceuticals"),
    PRIVATE_EQUITY("Private Equity"),
    PUBLISHING("Publishing"),
    REAL_ESTATE("Real Estate"),
    RETAIL_WHOLESALE("Retail & Wholesale"),
    SECURITIES_COMMODITY_EXCHANGES("Securities & Commodity Exchanges"),
    SERVICE("Service"),
    SHIPPING("Shipping"),
    SOAP_DETERGENT("Soap & Detergent"),
    SOFTWARE("Software"),
    SPORTS("Sports"),
    TECHNOLOGY("Technology"),
    TELECOMMUNICATION("Telecommunications"),
    TELEVISION("Television"),
    TRANSPORTATION("Transportation"),
    TRUCKING("Trucking"),
    UTILITIES("Utilities"),
    VENTURE_CAPITAL("Venture Capital");

    private final String name;

    IndustryType(final String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     *
     * @return true If the option is editable, otherwise it will return false
     */
    @Override
    public boolean isEditable() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return true If the option is default, otherwise it will return false
     */
    public boolean isDefault() {
        return false;
    }

    /**
     * <p>
     * Retrieves a collection of default options represented by the {@link IndustryType} enum values.
     * </p>
     *
     * @return The collection of {@link DefaultOption}
     */
    public static Collection<DefaultOption> getOptions() {
        return Arrays.asList(IndustryType.values());
    }

    /**
     * {@inheritDoc}
     *
     * @return The option name
     */
    @Override
    public String getName() {
        return name;
    }
}

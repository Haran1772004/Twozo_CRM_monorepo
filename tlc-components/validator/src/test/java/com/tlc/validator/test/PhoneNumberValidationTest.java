package com.tlc.validator.test;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the valid and invalid mobile numbers
 *
 * @author Maharaja S
 * @version 1.0
 */
public class PhoneNumberValidationTest {

    private static final String PHONE_NUMBER_PATTERN = "^(?=(?:\\D*\\d){5,17}$)\\+?(?:\\d{1,4}[ \\-]?)?(?:\\(?\\d{1,4}\\)?[ \\-]?)?\\d{1,4}[ \\-]?\\d{1,4}(?:[ \\-]?\\d{1,4})?$";

    /**
     * <p>
     * Validates the mobile number based on the regex patter
     * </p>
     *
     * @param phoneNumber Represents the phone number string to validate
     * @return true if the phone number is valid, false otherwise
     */
    public boolean isValidNumber(final String phoneNumber) {
        return phoneNumber.matches(PHONE_NUMBER_PATTERN);
    }

    /**
     * <p>
     * Tests the valid mobile numbers
     * </p>
     */
    @Test
    public void testValidMobileNumbers() {
        assertTrue(isValidNumber("12345"));
        assertTrue(isValidNumber("+123-456-7892-1112"));
        assertTrue(isValidNumber("+1234-5678"));
        assertTrue(isValidNumber("1234-5678"));
        assertTrue(isValidNumber("12-34-567"));
        assertTrue(isValidNumber("12-34-56-78"));
        assertTrue(isValidNumber("1234 567 8901"));
        assertTrue(isValidNumber("1234-567-8901"));
        assertTrue(isValidNumber("+1234 5678"));
        assertTrue(isValidNumber("+91-9876567898"));
        assertTrue(isValidNumber("+91(456)876567898"));
        assertTrue(isValidNumber("+91-98-76567898"));
        assertTrue(isValidNumber("+91-987-6567898"));
        assertTrue(isValidNumber("+1 (202) 555-0143"));
        assertTrue(isValidNumber("+1234 567 8901"));
        assertTrue(isValidNumber("+1234-567-8901"));
        assertTrue(isValidNumber("123 (4567) 8901"));
        assertTrue(isValidNumber("123-(4567)-8901"));
        assertTrue(isValidNumber("(123) 4567 8901"));
        assertTrue(isValidNumber("(123)-4567-8901"));
        assertTrue(isValidNumber("+12 (345) 678 901"));
        assertTrue(isValidNumber("+12-345-678-901"));
        assertTrue(isValidNumber("+12 (3456) 7890"));
        assertTrue(isValidNumber("1234 (5678) 9012"));
        assertTrue(isValidNumber("(123) 456-7890"));
        assertTrue(isValidNumber("(1234) 567-890"));
        assertTrue(isValidNumber("(12) 345 6789"));
        assertTrue(isValidNumber("+1-(1)-1-1-3"));
        assertTrue(isValidNumber("+12-(12)-12-12-23"));
        assertTrue(isValidNumber("+122-(122)-122-122-232"));
        assertTrue(isValidNumber("+1224-(1224)-122-122-232"));
        assertTrue(isValidNumber("+122-(122)-1224-122-2323"));
        assertTrue(isValidNumber("+122-(122)-12-12-22"));
        assertTrue(isValidNumber("+1234-(1)-1234-1234"));
        assertTrue(isValidNumber("+1234-(12)-1234-1234"));
        assertTrue(isValidNumber("+1234-(123)-1234-1234"));
        assertTrue(isValidNumber("+1234-(1234)-1234-1234"));
        assertTrue(isValidNumber("+1234-(1234)-1234-12-34"));
        assertTrue(isValidNumber("+1234 (1) 1234 1234"));
        assertTrue(isValidNumber("+1234 (12) 1234 1234"));
        assertTrue(isValidNumber("+1234 (123) 1234 1234"));
        assertTrue(isValidNumber("+1234 (1234) 1234 1234"));
        assertTrue(isValidNumber("+1234 (1234) 1234 12 34"));
        assertTrue(isValidNumber("+12345678912345678"));
        assertTrue(isValidNumber("+12345678-912345678"));
        assertTrue(isValidNumber("+1234(5678)-912345678"));
        assertTrue(isValidNumber("12345678912345678"));
    }

    /**
     * <p>
     * Tests the invalid mobile numbers
     * </p>
     */
    @Test
    public void testInvalidMobileNumbers() {
        assertFalse(isValidNumber("+1"));
        assertFalse(isValidNumber("+9999"));
        assertFalse(isValidNumber("+12345-(12345)-12345-12345"));
        assertFalse(isValidNumber("++123 456"));
        assertFalse(isValidNumber("+1()45 678"));
        assertFalse(isValidNumber("+1234--5678"));
        assertFalse(isValidNumber("abcd-1234"));
        assertFalse(isValidNumber("+1234 (abc) 1234"));
        assertFalse(isValidNumber("123456789123456789"));
        assertFalse(isValidNumber("123-456-789-123-123-44"));
        assertFalse(isValidNumber("123(456)-(567)-234"));
        assertFalse(isValidNumber("+12345-(1234)"));
        assertFalse(isValidNumber("+(34543)-2345"));
        assertFalse(isValidNumber("+9(9)99"));
        assertFalse(isValidNumber("+9(9)-9-9"));
        assertFalse(isValidNumber("+9-(9)-9-9"));
        assertFalse(isValidNumber("+9+9"));
        assertFalse(isValidNumber("+-12345"));
        assertFalse(isValidNumber("+1-1-1-1-1-1-1"));
        assertFalse(isValidNumber("+1234-"));
        assertFalse(isValidNumber("+12345678+"));
        assertFalse(isValidNumber("+12-34 (56) 78"));
        assertFalse(isValidNumber("1234-1234-(345)"));
        assertFalse(isValidNumber("12345-(767)"));
        assertFalse(isValidNumber("-12 34-56-78-90"));
        assertFalse(isValidNumber("2134++1234"));
        assertFalse(isValidNumber("1234(345)21(234)"));
        assertFalse(isValidNumber("123-213-432-1234-123-1234"));
        assertFalse(isValidNumber("1234()1234"));
        assertFalse(isValidNumber("12-(1)=1234"));
        assertFalse(isValidNumber("234235.65756"));
        assertFalse(isValidNumber("+34+34+546-3245"));
        assertFalse(isValidNumber("+123()-3"));
    }
}
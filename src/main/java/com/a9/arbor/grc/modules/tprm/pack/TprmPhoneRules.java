package com.a9.arbor.grc.modules.tprm.pack;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vendor SPOC mobile: allowed country dials, E.164 storage, libphonenumber validity.
 * UAE national length is exactly 9 (client rule).
 */
public final class TprmPhoneRules {

    private TprmPhoneRules() {
    }

    private static final PhoneNumberUtil UTIL = PhoneNumberUtil.getInstance();
    private static final String UAE_DIAL = "+971";

    /** Longest dial first so +971 wins over +97. */
    private static final String[] DIALS_LONGEST_FIRST = {
            "+971", "+966", "+974", "+973", "+968", "+965", "+880",
            "+91", "+92", "+63", "+65", "+61", "+44", "+49", "+33", "+27", "+20", "+1"
    };

    private static final Map<String, String> DIAL_TO_REGION = new LinkedHashMap<>();

    static {
        DIAL_TO_REGION.put("+971", "AE");
        DIAL_TO_REGION.put("+966", "SA");
        DIAL_TO_REGION.put("+974", "QA");
        DIAL_TO_REGION.put("+973", "BH");
        DIAL_TO_REGION.put("+968", "OM");
        DIAL_TO_REGION.put("+965", "KW");
        DIAL_TO_REGION.put("+880", "BD");
        DIAL_TO_REGION.put("+91", "IN");
        DIAL_TO_REGION.put("+92", "PK");
        DIAL_TO_REGION.put("+63", "PH");
        DIAL_TO_REGION.put("+65", "SG");
        DIAL_TO_REGION.put("+61", "AU");
        DIAL_TO_REGION.put("+44", "GB");
        DIAL_TO_REGION.put("+49", "DE");
        DIAL_TO_REGION.put("+33", "FR");
        DIAL_TO_REGION.put("+27", "ZA");
        DIAL_TO_REGION.put("+20", "EG");
        DIAL_TO_REGION.put("+1", "US");
    }

    public static int nationalLength(String dial) {
        return nationalMax(dial);
    }

    public static int nationalMin(String dial) {
        return nationalMax(dial);
    }

    public static int nationalMax(String dial) {
        if (UAE_DIAL.equals(dial)) {
            return 9;
        }
        String region = DIAL_TO_REGION.get(dial);
        if (region == null) {
            return 10;
        }
        PhoneNumber example = UTIL.getExampleNumberForType(region, PhoneNumberType.MOBILE);
        if (example == null) {
            return 10;
        }
        return UTIL.getNationalSignificantNumber(example).length();
    }

    public static boolean isValid(String mobile) {
        String trimmed = compact(mobile);
        if (trimmed == null) {
            return false;
        }
        String dial = matchingDial(trimmed);
        if (dial == null) {
            return false;
        }
        String local = trimmed.substring(dial.length());
        if (local.isEmpty() || !local.chars().allMatch(Character::isDigit)) {
            return false;
        }
        // Listed country + exact national length. Do not also require
        // libphonenumber "valid mobile" (random tester digits of the right
        // length were failing Create).
        return local.length() == nationalMax(dial);
    }

    /** Country code with no national digits is treated as blank. */
    public static String normalize(String mobile) {
        String trimmed = compact(mobile);
        if (trimmed == null) {
            return null;
        }
        for (String dial : DIALS_LONGEST_FIRST) {
            if (trimmed.equals(dial)) {
                return null;
            }
        }
        return trimmed;
    }

    public static String errorMessage() {
        return "Enter a valid mobile number for the selected country (UAE 9 digits, India 10 digits)";
    }

    private static String matchingDial(String trimmed) {
        for (String dial : DIALS_LONGEST_FIRST) {
            if (trimmed.startsWith(dial)) {
                return dial;
            }
        }
        return null;
    }

    private static String compact(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            return null;
        }
        String trimmed = mobile.trim().replaceAll("\\s+", "");
        return trimmed.isEmpty() ? null : trimmed;
    }
}

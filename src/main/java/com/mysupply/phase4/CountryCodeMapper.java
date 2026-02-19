package com.mysupply.phase4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Java conversion of the C# CountryCodeMapper class.
 * Maps country codes based on endpoint types and GLN values.
 */
@Component
public class CountryCodeMapper implements ICountryCodeMapper {

    private static final Logger logger = LoggerFactory.getLogger(CountryCodeMapper.class);

    private final SbdMappingData sbdMappingData;

    /**
     * GLN mapping for ranges 00001 – 00009
     */
    private final Map<Integer, String> mapGlnLong;

    /**
     * GLN mapping for ranges 0001 – 0009
     */
    private final Map<Integer, String> mapGlnMedium;

    /**
     * GLN mapping for ranges 001 – 019, 030 - 039, etc.
     */
    private final Map<Integer, String> mapGlnShort;

    private final Map<String, String> customMappings;

    private final Map<String, String> glnValues;

    /**
     * Constructor for CountryCodeMapper.
     */
    public CountryCodeMapper() {
        this.sbdMappingData = new SbdMappingData();
        this.mapGlnLong = createLongMapping();
        this.mapGlnMedium = createMediumMapping();
        this.mapGlnShort = createShortMapping();
        this.glnValues = createEanValues();

        this.customMappings = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.customMappings.put("556577236401", "DK");
    }

    /**
     * Create EAN/GLN values mapping.
     */
    private Map<String, String> createEanValues() {
        Map<String, String> glnValues = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        glnValues.put("EAN", "");
        glnValues.put("GLN", "");
        glnValues.put("0088", "");
        return glnValues;
    }

    /**
     * Create long GLN mapping (5-digit prefixes).
     */
    private Map<Integer, String> createLongMapping() {
        Map<Integer, String> map = new HashMap<>();
        addRange(map, 1, 9, "US");
        return map;
    }

    /**
     * Create medium GLN mapping (4-digit prefixes).
     */
    private Map<Integer, String> createMediumMapping() {
        Map<Integer, String> map = new HashMap<>();
        addRange(map, 1, 9, "US");
        return map;
    }

    /**
     * Create short GLN mapping (3-digit prefixes).
     */
    private Map<Integer, String> createShortMapping() {
        Map<Integer, String> map = new HashMap<>();

        // https://www.gs1.org/standards/id-keys/company-prefix
        addRange(map, 1, 19, "US");
        addRange(map, 30, 39, "US");
        addRange(map, 60, 139, "US");
        addRange(map, 300, 379, "FR");
        addRange(map, 380, "BG");
        addRange(map, 383, "SI");
        addRange(map, 385, "HR");
        addRange(map, 387, "BA");
        addRange(map, 389, "ME");
        addRange(map, 400, 440, "DE");
        addRange(map, 450, 459, "JP");
        addRange(map, 490, 499, "JP");
        addRange(map, 460, 469, "RU");
        addRange(map, 470, "KG");
        addRange(map, 471, "CN");
        addRange(map, 474, "EE");
        addRange(map, 475, "LV");
        addRange(map, 476, "AZ");
        addRange(map, 477, "LT");
        addRange(map, 478, "UZ");
        addRange(map, 479, "LK");
        addRange(map, 480, "PH");
        addRange(map, 481, "BY");
        addRange(map, 482, "UA");
        addRange(map, 483, "TM");
        addRange(map, 484, "MD");
        addRange(map, 485, "AM");
        addRange(map, 486, "GE");
        addRange(map, 487, "KZ");
        addRange(map, 488, "TJ");
        addRange(map, 489, "HK");
        addRange(map, 500, 509, "GB");
        addRange(map, 520, 521, "GR");
        addRange(map, 528, "LB");
        addRange(map, 529, "CY");
        addRange(map, 530, "AL");
        addRange(map, 531, "MK");
        addRange(map, 535, "MT");
        addRange(map, 539, "IE");
        addRange(map, 540, 549, "BE");
        addRange(map, 560, "PT");
        addRange(map, 569, "IS");
        addRange(map, 570, 579, "DK");
        addRange(map, 590, "PL");
        addRange(map, 594, "RO");
        addRange(map, 599, "HU");
        addRange(map, 600, 601, "ZA");
        addRange(map, 603, "GH");
        addRange(map, 604, "SN");
        addRange(map, 607, "OM");
        addRange(map, 608, "BH");
        addRange(map, 609, "MU");
        addRange(map, 611, "MA");
        addRange(map, 613, "DZ");
        addRange(map, 615, "NG");
        addRange(map, 616, "KE");
        addRange(map, 617, "CM");
        addRange(map, 618, "CI");
        addRange(map, 619, "TN");
        addRange(map, 620, "TZ");
        addRange(map, 621, "SY");
        addRange(map, 622, "EG");
        addRange(map, 624, "LY");
        addRange(map, 625, "JO");
        addRange(map, 626, "IR");
        addRange(map, 627, "KW");
        addRange(map, 628, "SA");
        addRange(map, 629, "AE");
        addRange(map, 630, "QA");
        addRange(map, 631, "NA");
        addRange(map, 640, 649, "FI");
        addRange(map, 680, 681, "CN");
        addRange(map, 690, 699, "CN");
        addRange(map, 700, 709, "NO");
        addRange(map, 729, "IL");
        addRange(map, 730, 739, "SE");
        addRange(map, 740, "GT");
        addRange(map, 741, "SV");
        addRange(map, 742, "HN");
        addRange(map, 743, "NI");
        addRange(map, 744, "CR");
        addRange(map, 745, "PA");
        addRange(map, 746, "DO");
        addRange(map, 750, "MX");
        addRange(map, 754, 755, "CA");
        addRange(map, 759, "VE");
        addRange(map, 760, 769, "CH");
        addRange(map, 770, 771, "CO");
        addRange(map, 773, "UY");
        addRange(map, 775, "PE");
        addRange(map, 777, "BO");
        addRange(map, 778, 779, "AR");
        addRange(map, 780, "CL");
        addRange(map, 784, "PY");
        addRange(map, 786, "EC");
        addRange(map, 789, 790, "BR");
        addRange(map, 800, 839, "IT");
        addRange(map, 840, 849, "ES");
        addRange(map, 850, "CU");
        addRange(map, 858, "SK");
        addRange(map, 859, "CZ");
        addRange(map, 860, "RS");
        addRange(map, 865, "MN");
        addRange(map, 867, "KP");
        addRange(map, 868, 869, "TR");
        addRange(map, 870, 879, "NL");
        addRange(map, 880, 881, "KR");
        addRange(map, 883, "MM");
        addRange(map, 884, "KH");
        addRange(map, 885, "TH");
        addRange(map, 888, "SG");
        addRange(map, 890, "IN");
        addRange(map, 893, "VN");
        addRange(map, 896, "PK");
        addRange(map, 899, "ID");
        addRange(map, 900, 919, "AT");
        addRange(map, 930, 939, "AU");
        addRange(map, 940, 949, "NZ");
        addRange(map, 955, "MY");
        addRange(map, 958, "CN");

        return map;
    }

    /**
     * Add a single value to the mapping.
     */
    private void addRange(Map<Integer, String> map, int value, String countryCode) {
        addRange(map, value, value, countryCode);
    }

    /**
     * Add a range of values to the mapping.
     */
    private void addRange(Map<Integer, String> map, int fromValue, int toValue, String countryCode) {
        for (int index = fromValue; index <= toValue; index++) {
            map.put(index, countryCode);
        }
    }



    /**
     * Map country code based on endpoint type and value.
     * @param endpoint The endpoint, consists of 'endpointType:endpointValue'.
     * @return The mapped country code
     */
    public String mapCountryCode(String endpoint) {
        if (endpoint == null || endpoint.isEmpty()) {
            throw new IllegalArgumentException("Endpoint must not be null or empty");
        }
        String[] parts = endpoint.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid endpoint format: '" + endpoint + "'. Expected format: 'type:value'");
        }
        String endpointType = parts[0];
        String endpointValue = parts[1];
        return mapCountryCode(endpointType, endpointValue);
    }

    /**
     * Map country code based on endpoint type and value.
     * @param endpointType The endpoint type
     * @param endpointValue The endpoint value
     * @return The mapped country code
     */
    public String mapCountryCode(String endpointType, String endpointValue) {
        String countryCode;
        if (this.glnValues.containsKey(endpointType)) {
            countryCode = this.mapGlnToCountryCode(endpointValue);
        } else {
            countryCode = this.mapEndpointTypeToCountryCode(endpointType);
        }
        return countryCode;
    }

    /**
     * Map GLN to country code.
     * @param endpointValue The GLN value
     * @return The mapped country code
     */
    private String mapGlnToCountryCode(String endpointValue) {
        String countryCode;

        if (endpointValue != null && endpointValue.length() >= 5) {
            try {
                int prefix;

                // Try 3-digit prefix first
                if (endpointValue.length() >= 3) {
                    String prefixStr = endpointValue.substring(0, 3);
                    prefix = Integer.parseInt(prefixStr);
                    if (prefix != 0) {
                        countryCode = mapGlnToCountryCode(prefix, this.mapGlnShort);
                        if (!"ZZ".equals(countryCode)) {
                            return countryCode;
                        }
                    }
                }

                // Try 4-digit prefix
                if (endpointValue.length() >= 4) {
                    String prefixStr = endpointValue.substring(0, 4);
                    prefix = Integer.parseInt(prefixStr);
                    if (prefix != 0) {
                        countryCode = mapGlnToCountryCode(prefix, this.mapGlnMedium);
                        if (!"ZZ".equals(countryCode)) {
                            return countryCode;
                        }
                    }
                }

                // Try 5-digit prefix
                if (endpointValue.length() >= 5) {
                    String prefixStr = endpointValue.substring(0, 5);
                    prefix = Integer.parseInt(prefixStr);
                    if (prefix != 0) {
                        countryCode = mapGlnToCountryCode(prefix, this.mapGlnLong);
                        if (!"ZZ".equals(countryCode)) {
                            return countryCode;
                        }
                    }
                }

                // Check custom mappings
                if (this.customMappings.containsKey(endpointValue)) {
                    countryCode = this.customMappings.get(endpointValue);
                } else {
                    countryCode = "ZZ";
                }

            } catch (NumberFormatException ex) {
                // Check custom mappings if numeric parsing fails
                if (this.customMappings.containsKey(endpointValue)) {
                    countryCode = this.customMappings.get(endpointValue);
                } else {
                    countryCode = "ZZ";
                }
            }
        } else {
            countryCode = "ZZ";
        }

        return countryCode;
    }

    /**
     * Map GLN prefix to country code using the specified mapping.
     * @param prefix The GLN prefix
     * @param map The mapping to use
     * @return The mapped country code
     */
    private String mapGlnToCountryCode(int prefix, Map<Integer, String> map) {
        String countryCode = map.get(prefix);
        return countryCode != null ? countryCode : "ZZ";
    }

    /**
     * Map endpoint type to country code using the SBD mappings.
     * @param endpointType The endpoint type
     * @return The mapped country code
     */
    private String mapEndpointTypeToCountryCode(String endpointType) {
        String countryCode = this.sbdMappingData.tryMap(endpointType);
        return countryCode != null ? countryCode : "ZZ";
    }

//    /**
//     * Try map the value. If the key is not found, try the empty string as key to identify the default value.
//     * @param mappings The mapping collection
//     * @param key The key
//     * @return TryMapResult containing success status and mapped value
//     */
//    public TryMapResult tryMap(Map<String, String> mappings, String key) {
//        boolean mapped;
//        String value;
//
//        if (key != null && mappings.containsKey(key)) {
//            mapped = true;
//            value = mappings.get(key);
//        } else if (mappings.containsKey("")) {
//            // Default value
//            mapped = true;
//            value = mappings.get("");
//        } else {
//            mapped = false;
//            value = null;
//        }
//
//        return new TryMapResult(mapped, value);
//    }

    /**
     * Result class for tryMap method.
     */
    public static class TryMapResult {
        private final boolean success;
        private final String value;

        public TryMapResult(boolean success, String value) {
            this.success = success;
            this.value = value;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getValue() {
            return value;
        }
    }
}

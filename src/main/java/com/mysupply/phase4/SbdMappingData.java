package com.mysupply.phase4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This class contains the raw mappings from the XML file and the created named mapped cache.
 * Java conversion of the C# SbdMappingData class with embedded XML data.
 */
public class SbdMappingData {

    /**
     * The collection of the different mappings.
     */
    private final Map<String, String> cacheCollection = new HashMap<>();

    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(SbdMappingData.class);

    /**
     * The lock object for synchronization.
     */
    private final Object lock = new Object();

    /**
     * Constructor for SbdMappingData.
     */
    public SbdMappingData() {
        this.load();
    }

    /**
     * Load the data if not already loaded.
     */
    private void load() {
        synchronized (lock) {
            if (cacheCollection.isEmpty()) {
                initializeEmbeddedMappings();
            }
        }
    }

    /**
     * Initialize mappings with embedded XML data.
     */
    private void initializeEmbeddedMappings() {
        //Map<String, String> cacheCollection = new HashMap<>();

        // Embed the XML data directly as key-value pairs
        // Internationally approved ICDs
        cacheCollection.put("FR:SIRENE", "FR");
        cacheCollection.put("0002", "FR");
        cacheCollection.put("SE:ORGNR", "SE");
        cacheCollection.put("0007", "SE");
        cacheCollection.put("FR:SIRET", "FR");
        cacheCollection.put("0009", "FR");
        cacheCollection.put("FI:OVT", "FI");
        cacheCollection.put("0037", "FI");
        cacheCollection.put("DUNS", "ZZ");
        cacheCollection.put("0060", "ZZ");
        cacheCollection.put("DK:P", "DK");
        cacheCollection.put("0096", "DK");
        cacheCollection.put("IT:FTI", "IT");
        cacheCollection.put("0097", "IT");
        cacheCollection.put("NL:KVK", "NL");
        cacheCollection.put("0106", "NL");
        cacheCollection.put("NAL", "ZZ");
        cacheCollection.put("0130", "ZZ");
        cacheCollection.put("IT:SIA", "IT");
        cacheCollection.put("0135", "IT");
        cacheCollection.put("IT:SECETI", "IT");
        cacheCollection.put("0142", "IT");
        cacheCollection.put("AU:ABN", "AU");
        cacheCollection.put("0151", "AU");
        cacheCollection.put("CH:UIDB", "CH");
        cacheCollection.put("0183", "CH");
        cacheCollection.put("DIGST", "DK");
        cacheCollection.put("0184", "DK");
        cacheCollection.put("JP:SST", "JP");
        cacheCollection.put("0188", "JP");
        cacheCollection.put("NL:OINO", "NL");
        cacheCollection.put("0190", "NL");
        cacheCollection.put("EE:CC", "EE");
        cacheCollection.put("0191", "EE");
        cacheCollection.put("NO:ORG", "NO");
        cacheCollection.put("0192", "NO");
        cacheCollection.put("UBLBE", "BE");
        cacheCollection.put("0193", "BE");
        cacheCollection.put("SG:UEN", "SG");
        cacheCollection.put("0195", "SG");
        cacheCollection.put("IS:KTNR", "IS");
        cacheCollection.put("0196", "IS");
        cacheCollection.put("DK:ERST", "DK");
        cacheCollection.put("0198", "DK");
        cacheCollection.put("LEI", "ZZ");
        cacheCollection.put("0199", "ZZ");
        cacheCollection.put("LT:LEC", "LT");
        cacheCollection.put("0200", "LT");
        cacheCollection.put("IT:CUUO", "IT");
        cacheCollection.put("0201", "IT");
        cacheCollection.put("DE:LWID", "DE");
        cacheCollection.put("0204", "DE");
        cacheCollection.put("IT:COD", "IT");
        cacheCollection.put("0205", "IT");
        cacheCollection.put("BE:EN", "BE");
        cacheCollection.put("0208", "BE");
        cacheCollection.put("GS1", "ZZ");
        cacheCollection.put("0209", "ZZ");
        cacheCollection.put("IT:CFI", "IT");
        cacheCollection.put("0210", "IT");
        cacheCollection.put("IT:IVA", "IT");
        cacheCollection.put("0211", "IT");
        cacheCollection.put("FI:ORG", "FI");
        cacheCollection.put("0212", "FI");
        cacheCollection.put("FI:VAT", "FI");
        cacheCollection.put("0213", "FI");
        cacheCollection.put("FI:NSI", "FI");
        cacheCollection.put("0215", "FI");
        cacheCollection.put("FI:OVT2", "FI");
        cacheCollection.put("0216", "FI");
        cacheCollection.put("JP:IIN", "JP");
        cacheCollection.put("0221", "JP");
        cacheCollection.put("MY:EIF", "MY");
        cacheCollection.put("0230", "MY");

        // ICDs created and maintained by OpenPEPPOL
        cacheCollection.put("DK:CPR", "DK");
        cacheCollection.put("9901", "DK");
        cacheCollection.put("DK:CVR", "DK");
        cacheCollection.put("9902", "DK");
        cacheCollection.put("DK:SE", "DK");
        cacheCollection.put("9904", "DK");
        cacheCollection.put("DK:VANS", "DK");
        cacheCollection.put("9905", "DK");
        cacheCollection.put("IT:VAT", "IT");
        cacheCollection.put("9906", "IT");
        cacheCollection.put("IT:CF", "IT");
        cacheCollection.put("9907", "IT");
        cacheCollection.put("NO:ORGNR", "NO");
        cacheCollection.put("9908", "NO");
        cacheCollection.put("NO:VAT", "NO");
        cacheCollection.put("9909", "NO");
        cacheCollection.put("HU:VAT", "HU");
        cacheCollection.put("9910", "HU");
        cacheCollection.put("EU:REID", "ZZ");
        cacheCollection.put("9913", "ZZ");
        cacheCollection.put("AT:VAT", "AT");
        cacheCollection.put("9914", "AT");
        cacheCollection.put("AT:GOV", "AT");
        cacheCollection.put("9915", "AT");
        cacheCollection.put("AT:CID", "AT");
        cacheCollection.put("9916", "AT");
        cacheCollection.put("IS:KT", "IS");
        cacheCollection.put("9917", "IS");
        cacheCollection.put("IBAN", "ZZ");
        cacheCollection.put("9918", "ZZ");
        cacheCollection.put("AT:KUR", "AT");
        cacheCollection.put("9919", "AT");
        cacheCollection.put("ES:VAT", "ES");
        cacheCollection.put("9920", "ES");
        cacheCollection.put("IT:IPA", "IT");
        cacheCollection.put("9921", "IT");
        cacheCollection.put("AD:VAT", "AD");
        cacheCollection.put("9922", "AD");
        cacheCollection.put("AL:VAT", "AL");
        cacheCollection.put("9923", "AL");
        cacheCollection.put("BA:VAT", "BA");
        cacheCollection.put("9924", "BA");
        cacheCollection.put("BE:VAT", "BE");
        cacheCollection.put("9925", "BE");
        cacheCollection.put("BG:VAT", "BG");
        cacheCollection.put("9926", "BG");
        cacheCollection.put("CH:VAT", "CH");
        cacheCollection.put("9927", "CH");
        cacheCollection.put("CY:VAT", "CY");
        cacheCollection.put("9928", "CY");
        cacheCollection.put("CZ:VAT", "CZ");
        cacheCollection.put("9929", "CZ");
        cacheCollection.put("DE:VAT", "DE");
        cacheCollection.put("9930", "DE");
        cacheCollection.put("EE:VAT", "EE");
        cacheCollection.put("9931", "EE");
        cacheCollection.put("GB:VAT", "GB");
        cacheCollection.put("9932", "GB");
        cacheCollection.put("GR:VAT", "GR");
        cacheCollection.put("9933", "GR");
        cacheCollection.put("HR:VAT", "HR");
        cacheCollection.put("9934", "HR");
        cacheCollection.put("IE:VAT", "IE");
        cacheCollection.put("9935", "IE");
        cacheCollection.put("LI:VAT", "LI");
        cacheCollection.put("9936", "LI");
        cacheCollection.put("LT:VAT", "LT");
        cacheCollection.put("9937", "LT");
        cacheCollection.put("LU:VAT", "LU");
        cacheCollection.put("9938", "LU");
        cacheCollection.put("LV:VAT", "LV");
        cacheCollection.put("9939", "LV");
        cacheCollection.put("MC:VAT", "MC");
        cacheCollection.put("9940", "MC");
        cacheCollection.put("ME:VAT", "ME");
        cacheCollection.put("9941", "ME");
        cacheCollection.put("MK:VAT", "MK");
        cacheCollection.put("9942", "MK");
        cacheCollection.put("MT:VAT", "MT");
        cacheCollection.put("9943", "MT");
        cacheCollection.put("NL:VAT", "NL");
        cacheCollection.put("9944", "NL");
        cacheCollection.put("PL:VAT", "PL");
        cacheCollection.put("9945", "PL");
        cacheCollection.put("PT:VAT", "PT");
        cacheCollection.put("9946", "PT");
        cacheCollection.put("RO:VAT", "RO");
        cacheCollection.put("9947", "RO");
        cacheCollection.put("RS:VAT", "RS");
        cacheCollection.put("9948", "RS");
        cacheCollection.put("SI:VAT", "SI");
        cacheCollection.put("9949", "SI");
        cacheCollection.put("SK:VAT", "SK");
        cacheCollection.put("9950", "SK");
        cacheCollection.put("SM:VAT", "SM");
        cacheCollection.put("9951", "SM");
        cacheCollection.put("TR:VAT", "TR");
        cacheCollection.put("9952", "TR");
        cacheCollection.put("VA:VAT", "VA");
        cacheCollection.put("9953", "VA");
        cacheCollection.put("NL:OIN", "NL");
        cacheCollection.put("9954", "NL");
        cacheCollection.put("SE:VAT", "SE");
        cacheCollection.put("9955", "SE");
        cacheCollection.put("BE:CBE", "BE");
        cacheCollection.put("9956", "BE");
        cacheCollection.put("FR:VAT", "FR");
        cacheCollection.put("9957", "FR");
        cacheCollection.put("DE:LID", "DE");
        cacheCollection.put("9958", "DE");
        cacheCollection.put("US:EIN", "US");
        cacheCollection.put("9959", "US");

        // Set default value for unmapped keys
        cacheCollection.put("", "ZZ");
    }



    /**
     * Try map the value. If the key is not found, try the empty string as key to identify the default value.
     * @param key The key
     * @return The mapped value or null if not found
     */
    public String tryMap(String key) {
        if (key != null && this.cacheCollection.containsKey(key)) {
            return this.cacheCollection.get(key);
        } else if (this.cacheCollection.containsKey("")) {
            // Default value
            return this.cacheCollection.get("");
        } else {
            return null;
        }
    }
}

package com.mysupply.phase4;

public interface ICountryCodeMapper
{
    /// Map the endpoint (endPointType:endpointValue) to a country code.
    String mapCountryCode(String endpoint);

    /// Map the endpoint type and value to a country code.
    String mapCountryCode(String endpointType, String endpointValue);
}
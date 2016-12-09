package com.microsoft.azuretools.authmanage.srvpri.entities;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureErrorArm {

    @JsonProperty
    public Error error;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Error {
        @JsonProperty
        public String code;
        @JsonProperty
        public String message;
    }
}

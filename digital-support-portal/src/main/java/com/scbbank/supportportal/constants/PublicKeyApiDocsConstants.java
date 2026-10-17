package com.scbbank.supportportal.constants;

public final class PublicKeyApiDocsConstants {

    private PublicKeyApiDocsConstants() {
    }

    public static final String SUMMARY =
            "Generate a public key for a login attempt";

    public static final String DESCRIPTION =
            "Generates a fresh RSA-2048 key pair. "
                    + "Returns the public key and stores the "
                    + "AES-encrypted private key in Redis for 300 seconds.";

    public static final String SUCCESS_EXAMPLE = """
            {
              "successful": true,
              "messageType": "SUCCESS",
              "data": {
                "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOC...",
                "expiresIn": 300
              }
            }
            """;

    public static final String RATE_LIMIT_EXAMPLE = """
            {
              "successful": false,
              "messageType": "ERROR",
              "errorCode": "DSP-AUTH-008",
              "errorMessage": "Too many requests. Please try again later."
            }
            """;
}

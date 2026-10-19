package com.scbbank.supportportal.constants;

public final class LoginApiDocsConstants {

    private LoginApiDocsConstants() {}

    public static final String TAG = "Authentication";

    public static final String SUMMARY = "Login with RSA-encrypted credentials";

    public static final String DESCRIPTION = """
            Call POST /api/v1/auth/public-key first to get a one-time RSA public key. \
            Encrypt the username and the password with RSA-OAEP (SHA-256, MGF1-SHA-256), \
            Base64-encode them, and send them with the same requestId. \
            A requestId works only once: after any login attempt, request a new key.""";

    // Response descriptions
    public static final String R200 = "Success. Access token in the body, refresh token in the HttpOnly cookie DSP_RT.";
    public static final String R400 = "DSP-AUTH-013 invalid requestId, DSP-AUTH-003 expired or used requestId, "
            + "DSP-AUTH-005 decryption failed, DSP-AUTH-010 invalid credential format.";
    public static final String R401 = "DSP-AUTH-001 invalid username or password. data.remainingAttempts shows the attempts left.";
    public static final String R423 = "DSP-AUTH-002 account temporarily locked. data.retryAt is an ISO-8601 UTC time.";
    public static final String R429 = "DSP-AUTH-008 too many requests.";
    public static final String R500 = "DSP-GEN-999 unexpected error.";
    public static final String R503 = "DSP-GEN-998 sign-in service unavailable.";

    // Swagger examples (JSON)
    public static final String EXAMPLE_200 = """
            {
              "successful": true,
              "messageType": "SUCCESS",
              "data": {
                "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
                "expiresIn": 900,
                "tokenType": "Bearer"
              }
            }""";

    public static final String EXAMPLE_401 = """
            {
              "successful": false,
              "messageType": "ERROR",
              "errorCode": "DSP-AUTH-001",
              "errorMessage": "Invalid username or password.",
              "data": { "remainingAttempts": 3 }
            }""";

    public static final String EXAMPLE_423 = """
            {
              "successful": false,
              "messageType": "ERROR",
              "errorCode": "DSP-AUTH-002",
              "errorMessage": "Account temporarily locked.",
              "data": { "remainingAttempts": 0, "retryAt": "2026-10-13T12:30:00Z" }
            }""";
}
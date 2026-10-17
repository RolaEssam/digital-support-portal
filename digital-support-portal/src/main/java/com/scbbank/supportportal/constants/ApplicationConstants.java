package com.scbbank.supportportal.constants;

public final class ApplicationConstants {

    private ApplicationConstants() {
    }

    // Redis keys
    public static final String LOGIN_KEY_PREFIX = "auth:login-key:";
    public static final String RATE_LIMIT_KEY_PREFIX = "auth:rl:";

    // Public key
    public static final int RSA_KEY_SIZE = 2048;
    public static final int PUBLIC_KEY_EXPIRY_SECONDS = 300;

    // AES-GCM
    public static final int AES_KEY_SIZE_BYTES = 32;
    public static final int AES_IV_LENGTH_BYTES = 12;
    public static final int AES_TAG_LENGTH_BITS = 128;

    // Global API rate limit
    public static final int RATE_LIMIT_MAX_REQUESTS = 10;
    public static final int RATE_LIMIT_WINDOW_SECONDS = 60;

    // Encryption algorithms
    public static final String RSA_ALGORITHM = "RSA";
    public static final String AES_ALGORITHM = "AES";
    public static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    public static final String RSA_OAEP_TRANSFORMATION =
            "RSA/ECB/OAEPPadding";
}

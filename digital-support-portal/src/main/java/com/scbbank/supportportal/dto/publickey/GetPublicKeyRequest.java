package com.scbbank.supportportal.dto.publickey;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GetPublicKeyRequest(
        @NotNull UUID requestId
) {
}


package com.training.client.model.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraUserResponse {

    private String accountId;
    private String displayName;
    private String emailAddress;
    private String accountType;
    private boolean active;
}

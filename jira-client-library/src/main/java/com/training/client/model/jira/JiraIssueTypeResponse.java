
package com.training.client.model.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssueTypeResponse {

    private String id;
    private String name;
    private String description;
    private boolean subtask;
    private int hierarchyLevel;
}


package com.training.client.model.jira;

import java.util.List;
import java.util.Map;

public class JiraCreateIssueRequest {

    public static Map<String, Object> build(
            String projectKey,
            String issueTypeId,
            String summary,
            String description
    ) {
        return Map.of(
                "fields", Map.of(
                        "project", Map.of("key", projectKey),
                        "issuetype", Map.of("id", issueTypeId),
                        "summary", summary,
                        "description", Map.of(
                                "type", "doc",
                                "version", 1,
                                "content", List.of(
                                        Map.of(
                                                "type", "paragraph",
                                                "content", List.of(
                                                        Map.of(
                                                                "type", "text",
                                                                "text", description
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }
}

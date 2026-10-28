
package com.training.client.core;

public final class ApiConstants {

    private ApiConstants() {
        // Prevent creating instances of this utility class.
    }

    // Jira user information
    public static final String GET_MYSELF =
            "rest/api/3/myself";

    // Jira issue types
    public static final String GET_ISSUE_TYPES =
            "rest/api/3/issuetype";

    // Project-specific issue creation metadata
    public static final String GET_CREATE_META =
            "rest/api/3/issue/createmeta/{project-key}/issuetypes";

    public static final String GET_CREATE_FIELDS =
            "rest/api/3/issue/createmeta/{project-key}/issuetypes/{issue-type-id}";

    // Jira tickets
    public static final String CREATE_ISSUE =
            "rest/api/3/issue";

    public static final String GET_ISSUE =
            "rest/api/3/issue/{issue-key}";

    // Project APIs: keep for future investigation
    public static final String SEARCH_PROJECTS =
            "rest/api/3/project/search";

    public static final String GET_PROJECT =
            "rest/api/3/project/{project-key}";
}


package com.training.client.example;

import com.training.client.model.jira.JiraCreateIssueResponse;
import com.training.client.model.jira.JiraIssueResponse;
import com.training.client.model.jira.JiraIssueTypeResponse;
import com.training.client.model.jira.JiraUserResponse;

import com.training.client.core.exception.BusinessException;
import com.training.client.oauth.AtlassianOAuthClient;
import com.training.client.oauth.InMemoryJiraTokenStore;
import com.training.client.oauth.JiraTokenManager;
import com.training.client.oauth.StoredJiraTokens;
import com.training.client.service.JiraClientService;

import java.time.Instant;
import java.time.Duration;

public class JiraLibraryTestApp {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {

        System.out.println("\n========================================");
        System.out.println(" DIGITAL SUPPORT PORTAL - JIRA LIBRARY");
        System.out.println("========================================");

        String cloudId = requiredEnv("JIRA_CLOUD_ID");
        String accessToken = requiredEnv("JIRA_ACCESS_TOKEN");
        String issueKey = requiredEnv("JIRA_ISSUE_KEY");

        String projectKey = envOrDefault(
                "JIRA_PROJECT_KEY", "KAN"
        );

        String issueTypeId = envOrDefault(
                "JIRA_ISSUE_TYPE_ID", "10003"
        );

        boolean createTicket = Boolean.parseBoolean(
                envOrDefault("JIRA_TEST_CREATE", "false")
        );

        /*
         * Smoke-test token configuration:
         *
         * We use a currently valid access token.
         * We intentionally disable automatic refresh
         * for this demonstration.
         *
         * This expiry is a testing assumption,
         * NOT the actual token expiration.
         */
        StoredJiraTokens tokens = new StoredJiraTokens(
                accessToken,
                "SMOKE_TEST_NO_REFRESH",
                Instant.now().plus(Duration.ofMinutes(10))
        );

        InMemoryJiraTokenStore store =
                new InMemoryJiraTokenStore(tokens);

        JiraTokenManager tokenManager =
                new JiraTokenManager(
                        new AtlassianOAuthClient(),
                        store,
                        "SMOKE_TEST_NO_REFRESH",
                        "SMOKE_TEST_NO_REFRESH"
                );

        JiraClientService jira =
                new JiraClientService(cloudId, tokenManager);

        // TEST 1: Authenticated user
        runTest("Get Jira User", () -> {

            JiraUserResponse user =
                    jira.getCurrentUser();

            require(user != null
                    && user.getAccountId() != null,
                    "Missing Jira account ID");

            System.out.println(
                    "  User: " + user.getDisplayName()
            );
            System.out.println(
                    "  Account ID: " + user.getAccountId()
            );
        });

        // TEST 2: Issue types
        runTest("Get Jira Issue Types", () -> {

            JiraIssueTypeResponse[] types =
                    jira.getIssueTypes();

            require(types != null && types.length > 0,
                    "No issue types returned");

            System.out.println(
                    "  Total issue types: " + types.length
            );

            for (JiraIssueTypeResponse type : types) {
                System.out.println(
                        "  - " + type.getName()
                        + " (" + type.getId() + ")"
                );
            }
        });

        // TEST 3: Get issue by key
        runTest("Get Jira Issue", () -> {

            JiraIssueResponse issue =
                    jira.getIssue(issueKey);

            require(issue != null
                    && issueKey.equals(issue.getKey()),
                    "Unexpected issue key");

            System.out.println(
                    "  Key: " + issue.getKey()
            );

            if (issue.getFields() != null) {
                System.out.println(
                        "  Summary: "
                        + issue.getFields().getSummary()
                );
            }
        });

        // TEST 4: Query parameters
        runTest("Get Jira Issue With Fields", () -> {

            JiraIssueResponse issue =
                    jira.getIssueWithFields(
                            issueKey,
                            "summary,status,project"
                    );

            require(issue != null
                    && issueKey.equals(issue.getKey()),
                    "Unexpected issue response");

            System.out.println(
                    "  Key: " + issue.getKey()
            );

            if (issue.getFields() != null) {
                System.out.println(
                        "  Summary: "
                        + issue.getFields().getSummary()
                );
            }
        });

        // TEST 5: HTTP 404 handling
        runTest("Handle Nonexistent Issue", () -> {

            try {
                jira.getIssue(
                        projectKey + "-999999999"
                );

                throw new AssertionError(
                        "Expected HTTP 404 but request succeeded"
                );

            } catch (BusinessException ex) {

                if (!String.valueOf(ex.getMessage())
                        .contains("404")) {
                    throw ex;
                }

                System.out.println(
                        "  Expected HTTP 404 handled correctly"
                );
            }
        });

        // TEST 6 + 7: Optional ticket creation
        if (createTicket) {

            runTest("Create and Retrieve Jira Ticket", () -> {

                String summary =
                        "Digital Support Portal - Library Test "
                        + Instant.now();

                JiraCreateIssueResponse created =
                        jira.createIssue(
                                projectKey,
                                issueTypeId,
                                summary,
                                "Integration test from the reusable "
                                + "Jira Java Client Library."
                        );

                require(created != null
                        && created.getKey() != null,
                        "No new issue key returned");

                System.out.println(
                        "  Created: " + created.getKey()
                );

                JiraIssueResponse retrieved =
                        jira.getIssue(created.getKey());

                require(retrieved != null
                        && created.getKey().equals(
                                retrieved.getKey()),
                        "Could not retrieve created issue");

                System.out.println(
                        "  Retrieved: " + retrieved.getKey()
                );
                System.out.println(
                        "  Browser URL: "
                        + "https://rolaessam03.atlassian.net/browse/"
                        + created.getKey()
                );
            });

        } else {
            System.out.println(
                    "\n[SKIPPED] Create Ticket "
                    + "(JIRA_TEST_CREATE=false)"
            );
        }

        System.out.println("\n========================================");
        System.out.println(" TEST SUMMARY");
        System.out.println("========================================");
        System.out.println(" PASSED: " + passed);
        System.out.println(" FAILED: " + failed);
        System.out.println("========================================");

        if (failed > 0) {
            throw new IllegalStateException(
                    failed + " Jira library test(s) failed"
            );
        }

        System.out.println("\nALL EXECUTED TESTS PASSED");
    }

    private static void runTest(
            String name,
            Runnable test
    ) {

        System.out.println("\n[TEST] " + name);

        try {
            test.run();
            passed++;
            System.out.println("[PASS] " + name);

        } catch (Exception | AssertionError ex) {
            failed++;
            System.out.println(
                    "[FAIL] " + name
                    + " - " + ex.getClass().getSimpleName()
            );
        }
    }

    private static void require(
            boolean condition,
            String message
    ) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static String requiredEnv(String name) {

        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing environment variable: " + name
            );
        }

        return value;
    }

    private static String envOrDefault(
            String name,
            String defaultValue
    ) {

        String value = System.getenv(name);

        return (value == null || value.isBlank())
                ? defaultValue
                : value;
    }
}

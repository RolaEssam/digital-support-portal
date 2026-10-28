# Jira Cloud Client Library

## Overview

A reusable Java 17 client library for integrating the Digital Support Portal with Jira Cloud through OAuth 2.0.

The library follows a generic outbound request architecture that separates HTTP request construction, execution, authentication, response mapping, and error handling.

## Technology Stack

- Java 17
- Maven
- Java HTTP Client
- Jackson for JSON serialization/deserialization
- Lombok
- Atlassian OAuth 2.0 (3LO)
- Jira Cloud REST API v3

## Supported Operations

| Operation | HTTP Method | API |
|---|---|---|
| Get authenticated user | GET | `/rest/api/3/myself` |
| Get issue types | GET | `/rest/api/3/issuetype` |
| Get ticket | GET | `/rest/api/3/issue/{issueKey}` |
| Get selected ticket fields | GET | `/rest/api/3/issue/{issueKey}?fields=...` |
| Create ticket | POST | `/rest/api/3/issue` |

The library also includes initial support for OAuth authorization-code exchange, refresh-token requests, and token management.

## Architecture

The library contains the following components:

- **Core:** Generic HTTP configuration, request builders, request execution, and exception handling.
- **Jira models:** Request and response objects for Jira API operations.
- **Outbound classes:** Endpoint-specific implementations of the reusable request builder.
- **OAuth:** Token exchange, refresh, and storage abstractions.
- **Service:** `JiraClientService`, which provides simplified methods for consuming Jira operations.

## Configuration

Set these environment variables before running the live integration tests:

| Variable | Description |
|---|---|
| `JIRA_CLOUD_ID` | Atlassian Jira Cloud ID |
| `JIRA_ACCESS_TOKEN` | Valid OAuth access token |
| `JIRA_ISSUE_KEY` | Existing accessible ticket key |
| `JIRA_PROJECT_KEY` | Jira project key, e.g. `KAN` |
| `JIRA_ISSUE_TYPE_ID` | Jira issue type ID |
| `JIRA_TEST_CREATE` | Set `true` to create a real ticket; defaults to `false` |

Never commit OAuth tokens, refresh tokens, client secrets, or credentials to version control.

## Build

```powershell
mvn clean package
```

The resulting JAR is generated under `target/`.

## Run Live Integration Tests

```powershell
mvn test-compile

mvn exec:java "-Dexec.mainClass=com.training.client.example.JiraLibraryTestApp" "-Dexec.classpathScope=test"
```

By default, the smoke-test application runs read-only requests plus a negative test for HTTP 404.

To test ticket creation:

```powershell
$env:JIRA_TEST_CREATE = "true"
```

This creates an actual Jira issue. Run only when intentional and disable the option afterward.

## Example Usage

```java
JiraIssueResponse issue =
    jiraClientService.getIssue("KAN-1");
```

Create a ticket:

```java
JiraCreateIssueResponse created =
    jiraClientService.createIssue(
        "KAN",
        "10003",
        "Customer login issue",
        "Customer cannot access the portal."
    );
```

The calling application must construct `JiraClientService` with a valid Jira Cloud ID and a configured `JiraTokenManager`.

## Verified Integration Results

The library was tested against a Jira Cloud test project.

- Get authenticated user — Passed
- Get Jira issue types — Passed
- Get issue by key — Passed
- Get issue using query parameters — Passed
- Handle HTTP 404 — Passed
- Create and retrieve an issue — Passed

**Result: 6 passed, 0 failed.**

## Important Limitations

- The live smoke-test application uses a supplied access token and does not validate automatic refresh.
- The in-memory token store is intended for development only.
- Production integration requires encrypted, persistent token storage and safe refresh-token rotation.
- Spring Boot security and controller integration are not included yet.
- Automated JUnit tests are planned but not yet implemented.

## Next Phase

Integrate the library into the Digital Support Portal Spring Boot application while preserving the existing JWT authentication and authorization system.
package com.scbbank.supportportal.controller;

import com.scbbank.supportportal.Security.Permission;
import com.scbbank.supportportal.Security.RequiresPermission;
import com.scbbank.supportportal.dto.base.ApiResponseDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class PortalController {

    // =====================================================================
    // Agent only
    // =====================================================================



    @GetMapping("/tickets")
    @RequiresPermission(Permission.TICKET_API)
    public ResponseEntity<ApiResponseDto<List<Object>>> tickets() {
        return ok();
    }

    @PostMapping("/tickets")
    @RequiresPermission(Permission.TICKET_WRITE)
    public ResponseEntity<ApiResponseDto<List<Object>>> createTicket() {
        return ok();
    }

    @PatchMapping("/tickets/{id}")
    @RequiresPermission(Permission.TICKET_WRITE)
    public ResponseEntity<ApiResponseDto<List<Object>>> updateTicket(@PathVariable Long id) {
        return ok();
    }


    @GetMapping("/customers")
    @RequiresPermission(Permission.CUSTOMER_API)
    public ResponseEntity<ApiResponseDto<List<Object>>> customers() {
        return ok();
    }



    @GetMapping("/calls")
    @RequiresPermission(Permission.CALL_API)
    public ResponseEntity<ApiResponseDto<List<Object>>> calls() {
        return ok();
    }

    @PostMapping("/calls")
    @RequiresPermission(Permission.CALL_WRITE)
    public ResponseEntity<ApiResponseDto<List<Object>>> createCall() {
        return ok();
    }

    @PatchMapping("/calls/{id}")
    @RequiresPermission(Permission.CALL_WRITE)
    public ResponseEntity<ApiResponseDto<List<Object>>> updateCall(@PathVariable Long id) {
        return ok();
    }



    @GetMapping("/agent/guides")
    @RequiresPermission(Permission.AGENT_GUIDE_API)
    public ResponseEntity<ApiResponseDto<List<Object>>> agentGuides() {
        return ok();
    }

    // =====================================================================
    // Admin and Reviewer
    // =====================================================================

    // ---- Guides: both can read, only the Admin (author) can write ----

    @GetMapping("/guides")
    @RequiresPermission(Permission.GUIDE_READ)
    public ResponseEntity<ApiResponseDto<List<Object>>> guides() {
        return ok();
    }

    @PostMapping("/guides")
    @RequiresPermission(Permission.GUIDE_WRITE)
    public ResponseEntity<ApiResponseDto<List<Object>>> createGuide() {
        return ok();
    }

    @PutMapping("/guides/{id}")
    @RequiresPermission(Permission.GUIDE_WRITE)
    public ResponseEntity<ApiResponseDto<List<Object>>> updateGuide(@PathVariable Long id) {
        return ok();
    }

    @PostMapping("/guides/{id}/submit")
    @RequiresPermission(Permission.GUIDE_WRITE)
    public ResponseEntity<ApiResponseDto<List<Object>>> submitGuide(@PathVariable Long id) {
        return ok();
    }

    // ---- Review: both can see the queue, only the Reviewer decides ----

    @GetMapping("/review")
    @RequiresPermission(Permission.REVIEW_API)
    public ResponseEntity<ApiResponseDto<List<Object>>> review() {
        return ok();
    }

    @PostMapping("/review/{versionId}/approve")
    @RequiresPermission(Permission.REVIEW_DECIDE)
    public ResponseEntity<ApiResponseDto<List<Object>>> approve(@PathVariable Long versionId) {
        return ok();
    }

    @PostMapping("/review/{versionId}/reject")
    @RequiresPermission(Permission.REVIEW_DECIDE)
    public ResponseEntity<ApiResponseDto<List<Object>>> reject(@PathVariable Long versionId) {
        return ok();
    }

    // =====================================================================
    // Admin only
    // =====================================================================

    // ---- Rules: full CRUD ----

    @GetMapping("/rules")
    @RequiresPermission(Permission.RULES_API)
    public ResponseEntity<ApiResponseDto<List<Object>>> rules() {
        return ok();
    }

    @PostMapping("/rules")
    @RequiresPermission(Permission.RULES_WRITE)
    public ResponseEntity<ApiResponseDto<List<Object>>> createRule() {
        return ok();
    }

    @PutMapping("/rules/{id}")
    @RequiresPermission(Permission.RULES_WRITE)
    public ResponseEntity<ApiResponseDto<List<Object>>> updateRule(@PathVariable Long id) {
        return ok();
    }

    @DeleteMapping("/rules/{id}")
    @RequiresPermission(Permission.RULES_WRITE)
    public ResponseEntity<ApiResponseDto<List<Object>>> deleteRule(@PathVariable Long id) {
        return ok();
    }

    // ---- Triage: full CRUD ----

    @GetMapping("/triage")
    @RequiresPermission(Permission.TRIAGE_API)
    public ResponseEntity<ApiResponseDto<List<Object>>> triage() {
        return ok();
    }

    @PostMapping("/triage")
    @RequiresPermission(Permission.TRIAGE_WRITE)
    public ResponseEntity<ApiResponseDto<List<Object>>> createTriage() {
        return ok();
    }

    @PutMapping("/triage/{id}")
    @RequiresPermission(Permission.TRIAGE_WRITE)
    public ResponseEntity<ApiResponseDto<List<Object>>> updateTriage(@PathVariable Long id) {
        return ok();
    }

    @DeleteMapping("/triage/{id}")
    @RequiresPermission(Permission.TRIAGE_WRITE)
    public ResponseEntity<ApiResponseDto<List<Object>>> deleteTriage(@PathVariable Long id) {
        return ok();
    }

    // ---- Analytics and Audit: read only (the audit log must never be editable) ----

    @GetMapping("/analytics")
    @RequiresPermission(Permission.ANALYTICS_API)
    public ResponseEntity<ApiResponseDto<List<Object>>> analytics() {
        return ok();
    }

    @GetMapping("/audit")
    @RequiresPermission(Permission.AUDIT_API)
    public ResponseEntity<ApiResponseDto<List<Object>>> audit() {
        return ok();
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private static String currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getClaimAsString("role");
        }
        return null;
    }

    /** Placeholder body. Replace with the real data in each story. */
    private static ResponseEntity<ApiResponseDto<List<Object>>> ok() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(ApiResponseDto.success(currentRole(), List.of()));
    }
}
package com.oviro.controller;

import com.oviro.dto.request.SupportMessageRequest;
import com.oviro.dto.request.SupportTicketRequest;
import com.oviro.dto.response.ApiResponse;
import com.oviro.dto.response.SupportTicketResponse;
import com.oviro.enums.SupportTicketStatus;
import com.oviro.service.SupportService;
import com.oviro.util.SecurityContextHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/support")
@RequiredArgsConstructor
@Tag(name = "Support Client", description = "Tickets d'assistance et messagerie support")
@SecurityRequirement(name = "bearerAuth")
public class SupportController {

    private final SupportService supportService;
    private final SecurityContextHelper securityHelper;

    @PostMapping("/tickets")
    @Operation(summary = "Ouvrir un ticket de support")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> createTicket(
            @Valid @RequestBody SupportTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Ticket ouvert", supportService.createTicket(request)));
    }

    @GetMapping("/tickets")
    @Operation(summary = "Mes tickets")
    public ResponseEntity<ApiResponse<Page<SupportTicketResponse>>> myTickets(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(supportService.getMyTickets(pageable)));
    }

    @GetMapping("/tickets/{ticketId}")
    @Operation(summary = "Détails d'un ticket")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> getTicket(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(ApiResponse.ok(supportService.getTicket(ticketId)));
    }

    @PostMapping("/tickets/{ticketId}/messages")
    @Operation(summary = "Ajouter un message à un ticket")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> addMessage(
            @PathVariable UUID ticketId,
            @Valid @RequestBody SupportMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Message envoyé", supportService.addMessage(ticketId, request)));
    }

    // ─── Admin endpoints ──────────────────────────────────────────

    @GetMapping("/admin/tickets")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tous les tickets [Admin]")
    public ResponseEntity<ApiResponse<Page<SupportTicketResponse>>> allTickets(
            @RequestParam(required = false) SupportTicketStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(supportService.getAllTickets(status, pageable)));
    }

    @PostMapping("/admin/tickets/{ticketId}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Répondre à un ticket [Admin]")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> agentReply(
            @PathVariable UUID ticketId,
            @Valid @RequestBody SupportMessageRequest request) {
        UUID agentId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("Réponse envoyée", supportService.agentReply(ticketId, request, agentId)));
    }

    @PatchMapping("/admin/tickets/{ticketId}/close")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fermer un ticket [Admin]")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> closeTicket(@PathVariable UUID ticketId) {
        UUID agentId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("Ticket résolu", supportService.closeTicket(ticketId, agentId)));
    }
}

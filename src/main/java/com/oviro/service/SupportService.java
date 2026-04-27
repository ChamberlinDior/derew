package com.oviro.service;

import com.oviro.dto.request.SupportMessageRequest;
import com.oviro.dto.request.SupportTicketRequest;
import com.oviro.dto.response.SupportTicketResponse;
import com.oviro.enums.NotificationType;
import com.oviro.enums.SupportTicketStatus;
import com.oviro.exception.BusinessException;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.SupportMessage;
import com.oviro.model.SupportTicket;
import com.oviro.model.User;
import com.oviro.repository.SupportMessageRepository;
import com.oviro.repository.SupportTicketRepository;
import com.oviro.repository.UserRepository;
import com.oviro.util.ReferenceGenerator;
import com.oviro.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportService {

    private final SupportTicketRepository ticketRepository;
    private final SupportMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ReferenceGenerator referenceGenerator;
    private final SecurityContextHelper securityHelper;
    private final NotificationService notificationService;

    @Transactional
    public SupportTicketResponse createTicket(SupportTicketRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId.toString()));

        SupportTicket ticket = SupportTicket.builder()
                .reference("TKT-" + referenceGenerator.generateRideReference())
                .userId(userId)
                .category(request.getCategory())
                .subject(request.getSubject())
                .relatedRideId(request.getRelatedRideId())
                .relatedDeliveryId(request.getRelatedDeliveryId())
                .relatedOrderId(request.getRelatedOrderId())
                .status(SupportTicketStatus.OPEN)
                .build();

        ticket = ticketRepository.save(ticket);

        SupportMessage firstMsg = SupportMessage.builder()
                .ticket(ticket)
                .senderId(userId)
                .senderName(user.getFullName())
                .content(request.getFirstMessage())
                .fromAgent(false)
                .build();
        messageRepository.save(firstMsg);

        log.info("Ticket support créé: {} par userId={}", ticket.getReference(), userId);
        return mapResponse(ticket);
    }

    @Transactional
    public SupportTicketResponse addMessage(UUID ticketId, SupportMessageRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId.toString()));

        SupportTicket ticket = findTicketForUser(ticketId, userId);

        if (ticket.getStatus() == SupportTicketStatus.CLOSED) {
            throw new BusinessException("Ce ticket est fermé. Ouvrez-en un nouveau.", "TICKET_CLOSED");
        }

        SupportMessage msg = SupportMessage.builder()
                .ticket(ticket)
                .senderId(userId)
                .senderName(user.getFullName())
                .content(request.getContent())
                .fromAgent(false)
                .build();
        messageRepository.save(msg);

        if (ticket.getStatus() == SupportTicketStatus.WAITING_USER) {
            ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
        }

        return mapResponse(ticket);
    }

    @Transactional
    public SupportTicketResponse agentReply(UUID ticketId, SupportMessageRequest request, UUID agentId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId.toString()));

        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent", agentId.toString()));

        SupportMessage msg = SupportMessage.builder()
                .ticket(ticket)
                .senderId(agentId)
                .senderName(agent.getFullName() + " (Support)")
                .content(request.getContent())
                .fromAgent(true)
                .build();
        messageRepository.save(msg);

        ticket.setStatus(SupportTicketStatus.WAITING_USER);
        ticket.setAssignedAgentId(agentId);
        ticketRepository.save(ticket);

        notificationService.sendToUser(ticket.getUserId(), NotificationType.SUPPORT_TICKET_REPLIED,
                "Réponse à votre ticket",
                "L'équipe OVIRO a répondu à votre demande : " + ticket.getSubject(),
                Map.of("ticketId", ticketId.toString()), 3600L, false);

        return mapResponse(ticket);
    }

    @Transactional
    public SupportTicketResponse closeTicket(UUID ticketId, UUID agentId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId.toString()));
        ticket.setStatus(SupportTicketStatus.RESOLVED);
        ticketRepository.save(ticket);

        notificationService.sendToUser(ticket.getUserId(), NotificationType.SUPPORT_TICKET_RESOLVED,
                "Ticket résolu", "Votre demande " + ticket.getReference() + " a été résolue.",
                Map.of("ticketId", ticketId.toString()), 3600L, false);

        return mapResponse(ticket);
    }

    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getMyTickets(Pageable pageable) {
        UUID userId = securityHelper.getCurrentUserId();
        return ticketRepository.findByUserId(userId, pageable).map(this::mapResponse);
    }

    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getAllTickets(SupportTicketStatus status, Pageable pageable) {
        if (status != null) {
            return ticketRepository.findByStatus(status, pageable).map(this::mapResponse);
        }
        return ticketRepository.findAll(pageable).map(this::mapResponse);
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse getTicket(UUID ticketId) {
        UUID userId = securityHelper.getCurrentUserId();
        return mapResponse(findTicketForUser(ticketId, userId));
    }

    private SupportTicket findTicketForUser(UUID ticketId, UUID userId) {
        return ticketRepository.findByIdAndUserId(ticketId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId.toString()));
    }

    private SupportTicketResponse mapResponse(SupportTicket t) {
        List<SupportMessage> msgs = messageRepository.findByTicketIdOrderByCreatedAtAsc(t.getId());
        return SupportTicketResponse.builder()
                .id(t.getId())
                .reference(t.getReference())
                .userId(t.getUserId())
                .category(t.getCategory())
                .subject(t.getSubject())
                .status(t.getStatus())
                .relatedRideId(t.getRelatedRideId())
                .relatedDeliveryId(t.getRelatedDeliveryId())
                .relatedOrderId(t.getRelatedOrderId())
                .messages(msgs.stream().map(m -> SupportTicketResponse.SupportMessageResponse.builder()
                        .id(m.getId())
                        .senderId(m.getSenderId())
                        .senderName(m.getSenderName())
                        .content(m.getContent())
                        .fromAgent(m.isFromAgent())
                        .createdAt(m.getCreatedAt())
                        .build()).toList())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}

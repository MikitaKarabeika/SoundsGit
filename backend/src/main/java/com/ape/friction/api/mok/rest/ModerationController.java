package com.ape.friction.api.mok.rest;

import com.ape.friction.api.dto.request.CreateModerationReportRequestDTO;
import com.ape.friction.api.dto.request.ModerationActionRequestDTO;
import com.ape.friction.api.dto.request.ModerationDecisionRequestDTO;
import com.ape.friction.api.dto.response.AccountModerationStatusDTO;
import com.ape.friction.api.dto.response.ModerationDecisionResponseDTO;
import com.ape.friction.api.dto.response.ModerationActionResponseDTO;
import com.ape.friction.api.dto.response.ModerationReportResponseDTO;
import com.ape.friction.api.filters.userPrincipal.UserPrincipal;
import com.ape.friction.api.mok.services.ModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/moderation")
public class ModerationController {

    private final ModerationService moderationService;

    @PostMapping("/reports")
    public ResponseEntity<ModerationReportResponseDTO> createReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateModerationReportRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moderationService.createReport(principal.getId(), request));
    }

    @GetMapping("/reports")
    public Page<ModerationReportResponseDTO> getOpenReports(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return moderationService.getOpenReports(pageable);
    }

    @PostMapping("/reports/{reportId}/decision")
    public ModerationDecisionResponseDTO decideReport(
            @PathVariable UUID reportId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ModerationDecisionRequestDTO request
    ) {
        return moderationService.decideReport(reportId, principal.getId(), request);
    }

    @GetMapping("/decisions")
    public Page<ModerationDecisionResponseDTO> getDecisionLog(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return moderationService.getDecisionLog(pageable);
    }

    @GetMapping("/accounts")
    public Page<AccountModerationStatusDTO> getAccountStatuses(
            @PageableDefault(size = 20, sort = "login") Pageable pageable
    ) {
        return moderationService.getAccountStatuses(pageable);
    }

    @GetMapping("/accounts/{accountId}")
    public AccountModerationStatusDTO getAccountStatus(@PathVariable UUID accountId) {
        return moderationService.getAccountStatus(accountId);
    }

    @GetMapping("/accounts/{accountId}/actions")
    public Page<ModerationActionResponseDTO> getAccountActionHistory(
            @PathVariable UUID accountId,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable
    ) {
        return moderationService.getAccountActionHistory(accountId, pageable);
    }

    @PostMapping("/accounts/{accountId}/ban")
    public AccountModerationStatusDTO permanentlyBan(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ModerationActionRequestDTO request
    ) {
        return moderationService.permanentlyBan(accountId, principal.getId(), request);
    }

    @PostMapping("/accounts/{accountId}/lift")
    public AccountModerationStatusDTO liftSanction(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ModerationActionRequestDTO request
    ) {
        return moderationService.liftSanction(accountId, principal.getId(), request);
    }
}

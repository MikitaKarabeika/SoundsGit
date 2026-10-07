package com.ape.friction.api.mok.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ape.friction.api.dto.data.AuditRecordDTO;
import com.ape.friction.api.mok.services.JaversAuditService;

import java.util.List;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
public class JaversAuditController {

    private final JaversAuditService javersAuditService;

    @GetMapping
    @PreAuthorize("hasAuthority('JAVERS_GET_AUDIT')")
    public ResponseEntity<List<AuditRecordDTO>> getAuditHistory(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(required = false, defaultValue = "DATE_DESC") String sortBy
    ) {
        List<AuditRecordDTO> history = javersAuditService.getAuditHistory(limit, skip, sortBy);
        return ResponseEntity.ok(history);
    }

}

package com.ape.friction.api.mok.services;

import lombok.RequiredArgsConstructor;
import org.javers.core.Javers;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.QueryBuilder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ape.friction.api.dto.data.AuditRecordDTO;
import com.ape.friction.api.interceptors.MethodCallLogged;
import com.ape.friction.api.interceptors.TransactionLogged;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@TransactionLogged
@MethodCallLogged
@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, transactionManager = "mokTransactionManager")
public class JaversAuditService {

    private final Javers javers;

    @PreAuthorize("hasAuthority('JAVERS_GET_AUDIT')")
    @Transactional(transactionManager = "mokTransactionManager", readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<AuditRecordDTO> getAuditHistory(int limit, int skip, String sortBy) {

        List<CdoSnapshot> snapshots = javers.findSnapshots(
                QueryBuilder.anyDomainObject()
                        .limit(limit)
                        .skip(skip)
                        .build()
        );

        List<AuditRecordDTO> auditRecords = snapshots.stream()
                .map(snapshot -> new AuditRecordDTO(
                        snapshot.getCommitMetadata().getAuthor(),
                        snapshot.getCommitMetadata().getCommitDateInstant(),
                        snapshot.getGlobalId().getTypeName(),
                        snapshot.getType().name(),
                        snapshot.getChanged()
                ))
                .collect(Collectors.toList());

        if ("ENTITY_TYPE".equalsIgnoreCase(sortBy)) {
            auditRecords.sort(Comparator.comparing(AuditRecordDTO::entityType));
        } else if ("DATE_ASC".equalsIgnoreCase(sortBy)) {
            auditRecords.sort(Comparator.comparing(AuditRecordDTO::commitDate));
        }

        return auditRecords;
    }
}

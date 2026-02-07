package org.pavl.secretsharingapp.db;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.pavl.secretsharingapp.domain.ActionType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    @NotNull
    private LocalDateTime timestamp;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    private String secretAccessTokenHash;

    private String apiKeyHash;
}

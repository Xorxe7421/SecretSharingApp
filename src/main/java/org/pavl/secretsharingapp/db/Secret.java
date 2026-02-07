package org.pavl.secretsharingapp.db;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.pavl.secretsharingapp.validation.CombinedNotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
@CombinedNotNull({"expirationTime", "viewCount"})
public class Secret {

    @Id
    @GeneratedValue
    private UUID id;

    @NotNull
    private String encryptedContent;

    @NotNull
    private String accessTokenHash;

    @Future
    private LocalDateTime expirationTime;

    @Min(1)
    @Max(5)
    private Integer viewCount;

    @ManyToOne
    @JoinColumn(name = "api_key_id")
    private ApiKey apiKey;
}

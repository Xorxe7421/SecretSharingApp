package org.pavl.secretsharingapp.db;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.pavl.secretsharingapp.domain.Tier;

import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class ApiKey {

    @Id
    @GeneratedValue
    private UUID id;

    @NotNull
    private String keyHash;

    @NotNull
    private String ownerName;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Tier tier;

    @NotNull
    private boolean isActive;

    @OneToMany(mappedBy = "apiKey", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Secret> secrets;
}

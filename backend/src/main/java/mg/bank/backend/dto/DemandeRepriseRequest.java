package mg.bank.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeRepriseRequest {

    @Size(max = 1000, message = "Le motif ne peut pas dépasser 1000 caractères")
    private String motif;
}
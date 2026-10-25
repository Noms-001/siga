package mg.bank.backend.model.utilisateur;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "type_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypeToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_type_token")
    private Integer idTypeToken;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "libelle", nullable = false, length = 150)
    private String libelle;

    @Column(name = "description")
    private String description;
}
package mg.bank.backend.model;

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

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "libelle", nullable = false)
    private String libelle;

    @Column(name = "description")
    private String description;
}
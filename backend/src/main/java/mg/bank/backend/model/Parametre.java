package mg.bank.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parametre")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parametre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parametre")
    private Integer idParametre;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "valeur", nullable = false)
    private String valeur;

    @Column(name = "description")
    private String description;

    @Column(name = "actif", nullable = false)
    private Boolean actif;
}
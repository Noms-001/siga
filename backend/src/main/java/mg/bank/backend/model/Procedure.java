package mg.bank.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "procedure")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Procedure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_procedure")
    private Integer idProcedure;

    @Column(name = "designation", nullable = false, length = 255)
    private String designation;

    @Column(name = "description")
    private String description;

    @OneToMany(
            mappedBy = "procedure",
            cascade = CascadeType.ALL,
            orphanRemoval = false
    )
    @OrderBy("niveau ASC")
    @Builder.Default
    private List<EtapeValidation> etapesValidation = new ArrayList<>();
}
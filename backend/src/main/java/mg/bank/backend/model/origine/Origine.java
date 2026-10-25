package mg.bank.backend.model.origine;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.bank.backend.model.utilisateur.Utilisateur;

@Entity
@Table(name = "origine")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Origine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_origine")
    private Integer idOrigine;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Column(name = "type_origine", nullable = false, length = 50)
    private String typeOrigine;

    @Column(name = "description")
    private String description;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;
}
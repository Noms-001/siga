package mg.bank.backend.model;

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

/**
 * Livrable rattache a une sous-activite.
 *
 * <p>Les fichiers eventuellement deposes sur ce livrable sont portes par
 * {@code FichierSousActivite} (table {@code fichier_sous_activite}). Ils ne
 * font pas partie de l'import initial : ils seront ajoutes ulterieurement
 * depuis l'interface GESTIA (cf. cahier des charges).</p>
 */
@Entity
@Table(name = "livrable_sous_activite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivrableSousActivite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_livrable_sous_activite")
    private Integer idLivrableSousActivite;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sous_activite", nullable = false)
    private SousActivite sousActivite;
}
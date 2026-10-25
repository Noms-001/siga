package mg.bank.backend.model.activite;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mg.bank.backend.model.utilisateur.Utilisateur;

@Entity
@Table(name = "fichier_sous_activite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fichier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_fichier_sous_activite")
    private Integer idFichier;

    @Column(name = "nom_fichier", nullable = false)
    private String nomFichier;

    @Column(name = "nom_original", nullable = false)
    private String nomOriginal;

    @Column(name = "chemin_fichier", nullable = false)
    private String cheminFichier;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "extension", length = 20)
    private String extension;

    @Column(name = "type_mime", length = 150)
    private String typeMime;

    @Column(name = "taille")
    private Long taille;

    @Column(name = "date_depot", nullable = false)
    private LocalDateTime dateDepot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_livrable_sous_activite", nullable = false)
    private Livrable livrable;

}

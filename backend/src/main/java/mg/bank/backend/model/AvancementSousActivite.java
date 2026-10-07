package mg.bank.backend.model;

import java.math.BigDecimal;
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


@Entity
@Table(name = "avancement_sous_activite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvancementSousActivite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historique_sous_activite")
    private Integer idHistoriqueSousActivite;

    @Column(name = "valeur_pourcentage", nullable = false, precision = 15, scale = 2)
    private BigDecimal valeurPourcentage;

    @Column(name = "commentaire")
    private String commentaire;

    @Column(name = "date_changement", nullable = false)
    private LocalDateTime dateChangement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sous_activite", nullable = false)
    private SousActivite sousActivite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_statut", nullable = false)
    private Statut statut;
}
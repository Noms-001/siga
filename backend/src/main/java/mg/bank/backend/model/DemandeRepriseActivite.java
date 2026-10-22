package mg.bank.backend.model;

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
@Table(name = "demande_reprise_activite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeRepriseActivite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_demande_reprise_activite")
    private Integer idDemandeRepriseActivite;

    @Column(name = "date_demande", nullable = false)
    private LocalDateTime dateDemande;

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;

    @Column(name = "statut", nullable = false)
    private String statut;

    @Column(name = "motif")
    private String motif;

    @Column(name = "commentaire_decision")
    private String commentaireDecision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_activite", nullable = false)
    private Activite activite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_demandeur", nullable = false)
    private Utilisateur demandeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_responsable_actuel", nullable = false)
    private Utilisateur responsableActuel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_decideur")
    private Utilisateur decideur;
}
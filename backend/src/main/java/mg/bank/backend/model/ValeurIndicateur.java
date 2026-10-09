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

/**
 * Relevé ponctuel d'un indicateur sur une période donnée.
 *
 * La valeur courante d'un indicateur est la ligne dont periode_fin est
 * la plus récente ; c'est cette ligne que le front affiche en tête.
 * Aucune valeur n'est jamais écrasée — chaque saisie ajoute une ligne.
 */
@Entity
@Table(name = "valeur_indicateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValeurIndicateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_valeur_indicateur")
    private Integer idValeurIndicateur;

    @Column(name = "valeur", nullable = false, precision = 15, scale = 2)
    private BigDecimal valeur;

    @Column(name = "periode_debut", nullable = false)
    private LocalDateTime periodeDebut;

    @Column(name = "periode_fin", nullable = false)
    private LocalDateTime periodeFin;

    @Column(name = "commentaire")
    private String commentaire;

    @Column(name = "date_saisie", nullable = false)
    private LocalDateTime dateSaisie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_indicateur", nullable = false)
    private Indicateur indicateur;
}
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
 * Resultat intermediaire d une activite.
 *
 * MEME JUSTIFICATION QUE SousActivite, ET PAS LA MEME CONSQUENCE
 *
 * Cette table n etait lue qu en SQL natif, via une projection
 * (ResultatIntermediaireRow). Aucun mapping JPA n existait, parce que rien ne
 * l ecrivait. Le formulaire de creation et de modification devant pouvoir
 * dcrire ses resultats intermediaires dans la meme transaction que
 * l activite mere, le mapping est ajoute ici plutot qu un INSERT/UPDATE en
 * SQL brut : la transaction et le flush restent ceux de JPA, comme pour
 * Activite et SousActivite.
 *
 * CONTRAIREMENT A SousActivite, LA SUPPRESSION EST LIBRE
 *
 * Aucune table ne pointe vers resultat_intermediaire -- ni livrable, ni
 * affectation, ni avancement, ni validation. Une ligne de resultat n est
 * donc rattachee qu a son activite, ce qui autorise le remplacement de
 * l ensemble a la volee, la ou une sous-activite doit etre synchronisee ligne
 * a ligne pour ne pas casser ses dependances. Verifier ce point avant de
 * transformer un jour cette synchronisation en "DELETE puis INSERT" sur les
 * sous-activites.
 *
 * CE QUI N EST PAS DANS LE MODELE
 *
 * La table ne porte que trois colonnes : un identifiant, une designation et
 * l activite. Ni date, ni valeur, ni responsable : les exposer ici
 * reviendrait a inventer une donnee sans colonne.
 */
@Entity
@Table(name = "resultat_intermediaire")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultatIntermediaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resultat_intermediaire")
    private Integer idResultatIntermediaire;

    @Column(name = "designation", nullable = false)
    private String designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_activite", nullable = false)
    private Activite activite;

}

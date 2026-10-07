package mg.bank.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mg.bank.backend.model.AvancementSousActivite;
import mg.bank.backend.repository.projection.AvancementSousActiviteRow;

public interface AvancementSousActiviteRepository
                extends JpaRepository<AvancementSousActivite, Integer> {

        /**
         * Dernier avancement par sous-activité, pour PLUSIEURS sous-activités.
         *
         * UNE seule requête pour toutes les sous-activités d'une page : c'est
         * la condition pour que le nombre de requêtes ne dépende pas du nombre
         * de sous-activités affichées.
         *
         * Le "dernier" est désigné par NOT EXISTS (date, id) > (date, id) —
         * exactement le même motif que ActiviteRepository.SELECT_BASE, pour que
         * la valeur affichée dans Suivi.vue et celle utilisée dans le calcul
         * d'avancement de l'activité soient rigoureusement identiques.
         *
         * CAST en double précision : la colonne est NUMERIC(15,2), on la
         * convertit une fois pour que le front reçoive un nombre simple, sans
         * se soucier du BigDecimal côté JSON.
         */
        @Query(value = """
                        SELECT av.id_sous_activite          AS idSousActivite,
                               av.valeur_pourcentage::float AS valeur,
                               av.date_changement           AS dateChangement
                        FROM avancement_sous_activite av
                        WHERE av.id_sous_activite IN (:ids)
                          AND NOT EXISTS (
                              SELECT 1 FROM avancement_sous_activite av2
                              WHERE av2.id_sous_activite = av.id_sous_activite
                                AND (av2.date_changement, av2.id_historique_sous_activite)
                                    > (av.date_changement, av.id_historique_sous_activite))
                        """, nativeQuery = true)
        List<AvancementSousActiviteRow> findDerniersPourSousActivites(
                        @Param("ids") List<Integer> ids);

        /**
         * Historique complet d'une sous-activité, du plus récent au plus ancien.
         *
         * JOIN FETCH sur utilisateur et statut : sans lui, chaque ligne lue
         * déclencherait deux requêtes supplémentaires (N+1) au moment où le
         * mapping accède à l'utilisateur ou au statut. Comme l'écran d'historique
         * affiche typiquement ces deux colonnes, on les charge dès la première
         * requête.
         *
         * Le second critère de tri (id_historique_sous_activite DESC) est
         * nécessaire : deux relevés peuvent partager une même date_changement, et
         * sans ce critère, l'ordre serait indéterminé d'un appel à l'autre.
         */
        @Query("""
                        SELECT av FROM AvancementSousActivite av
                        JOIN FETCH av.utilisateur
                        JOIN FETCH av.statut
                        WHERE av.sousActivite.idSousActivite = :id
                        ORDER BY av.dateChangement DESC, av.idHistoriqueSousActivite DESC
                        """)
        List<AvancementSousActivite> findHistorique(@Param("id") Integer idSousActivite);
}
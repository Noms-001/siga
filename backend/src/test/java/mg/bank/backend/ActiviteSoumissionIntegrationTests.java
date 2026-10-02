package mg.bank.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests de la soumission d une activite a la validation.
 *
 * Ces tests lisent et ecrivent la base de developpement, dans une transaction
 * annulee en fin de test. Ils ne simulent donc ni le verrou de ligne, ni la
 * regle "le statut courant est la derniere ligne d historique", ni le
 * departage 404 / 403 : ce sont precisement les comportements qui ne
 * s observent pas sur une base en memoire.
 *
 * AUCUN IDENTIFIANT FIGE, AUCUN STATUT SUPPOSE
 * Le jeu de donnees ne contient aucune activite en brouillon : toutes ont
 * deja parcouru le circuit. Chaque test fabrique donc son etat de depart avec
 * rendreBrouillon, datee apres la derniere entree existante, et lit le
 * resultat avec statutCourant, qui applique la meme regle que le code
 * applicatif. Aucun test ne depend donc du contenu du jeu de donnees, et
 * aucun ne peut passer a vide.
 *
 * L authentification passe par @WithMockUser : le service lit l identite
 * dans le contexte de securite, et c est ce qu il faut reproduire.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActiviteSoumissionIntegrationTests {

    /** Rattache au service 1, auteur des brouillons qu il soumet. */
    private static final String UTILISATEUR_SERVICE_UN =
            "leila.mansouri@bfm.tn";

    /** Rattache au service 2 : ne voit pas les activites du service 1. */
    private static final String UTILISATEUR_SERVICE_DEUX =
            "hatem.trabelsi@bfm.tn";

    /** Niveau departement : voit tous les services du departement. */
    private static final String UTILISATEUR_DEPARTEMENT =
            "karim.bensalah@bfm.tn";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    // ------------------------------------------------------------------
    // Cas nominal
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Soumettre un brouillon le place en attente de validation")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void soumissionReussie() throws Exception {

        Integer id = rendreBrouillon(activiteDuService(1));

        mockMvc.perform(post("/api/activites/{id}/soumettre-validation", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.idActivite").value(id))
                .andExpect(jsonPath("$.data.code").isNotEmpty())
                .andExpect(jsonPath("$.data.statut").value("EN_ATTENTE_VALIDATION"))
                .andExpect(jsonPath("$.data.dateSoumission").isNotEmpty())
                .andExpect(jsonPath("$.error").doesNotExist());

        assertThat(statutCourant(id)).isEqualTo("EN_ATTENTE_VALIDATION");
    }

    @Test
    @DisplayName("La soumission ecrit une entree d historique datee, pas une colonne")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void soumissionEcritDansLEhistorique() throws Exception {

        Integer id = rendreBrouillon(activiteDuService(1));

        long avant = nombreEntrees(id);

        mockMvc.perform(post("/api/activites/{id}/soumettre-validation", id))
                .andExpect(status().isOk());

        // Une seule entree de plus, et c est la derniere : changer le statut
        // d une activite revient a ecrire dans historique_activite, la table
        // activite n en portant aucun.
        assertThat(nombreEntrees(id)).isEqualTo(avant + 1);
        assertThat(statutCourant(id)).isEqualTo("EN_ATTENTE_VALIDATION");
    }

    @Test
    @DisplayName("L historique de la soumission porte l identite de l appelant")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void soumissionAttribueeAuDemandeur() throws Exception {

        Integer id = rendreBrouillon(activiteDuService(1));

        mockMvc.perform(post("/api/activites/{id}/soumettre-validation", id))
                .andExpect(status().isOk());

        String auteur = jdbc.queryForObject("""
                SELECT u.email
                FROM historique_activite h
                JOIN utilisateur u ON u.id_utilisateur = h.id_utilisateur
                WHERE h.id_activite = ?
                ORDER BY h.date_changement DESC, h.id_historique_activite DESC
                LIMIT 1
                """, String.class, id);

        // L auteur vient du contexte de securite et n est pas parametrable :
        // un client ne peut pas soumettre au nom d un autre.
        assertThat(auteur).isEqualTo(UTILISATEUR_SERVICE_UN);
    }

    @Test
    @DisplayName("Un brouillon soumis quitte la liste des brouillons")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void activiteSoumiseQuitteLaListe() throws Exception {

        Integer id = rendreBrouillon(activiteDuService(1));

        mockMvc.perform(post("/api/activites/{id}/soumettre-validation", id))
                .andExpect(status().isOk());

        // EN_ATTENTE_VALIDATION est dans SQL_NON_PUBLIEES : une activite
        // soumise n est plus un brouillon, elle n est pas encore publiee au
        // suivi. Elle ne doit donc plus etre listee comme telle.
        Integer encoreListee = jdbc.queryForObject("""
                SELECT count(*)
                FROM activite act
                JOIN LATERAL (
                    SELECT h.id_statut
                    FROM historique_activite h
                    WHERE h.id_activite = act.id_activite
                    ORDER BY h.date_changement DESC, h.id_historique_activite DESC
                    LIMIT 1
                ) d ON TRUE
                JOIN statut sta ON sta.id_statut = d.id_statut
                WHERE act.id_activite = ?
                  AND sta.code = 'BROUILLON'
                """, Integer.class, id);

        assertThat(encoreListee).isZero();
    }

    // ------------------------------------------------------------------
    // Lot
    // ------------------------------------------------------------------

    /**
     * La soumission multiple de la page des brouillons.
     *
     * CE QUE REPRODUIT CE TEST
     * La page ne possede pas d appel de lot : elle envoie une requete par
     * activite, l une apres l autre. Ce test fait donc exactement cela -- trois
     * activites, trois POST -- et verifie que chacune a bien ecrit sa ligne
     * d historique. C est le seul moyen de savoir que la page fonctionne : un
     * test du service seul ne verrait que la premiere ecriture.
     *
     * CE QUE CELA NE CHANGE PAS
     * Chaque requete est sa propre transaction. Une activite refusee au milieu
     * du lot n annule donc pas les precedentes, et c est voulu : la page
     * conserve les cards refusees et ne retire que celles qui sont passees.
     */
    @Test
    @DisplayName("Une soumission de lot ecrit une ligne par activite")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void soumissionLotEcritUneLigneParActivite() throws Exception {

        List<Integer> lot = activitesDuService(1, 3).stream()
                .map(this::rendreBrouillon)
                .toList();

        Map<Integer, Long> avant = new HashMap<>();

        for (Integer id : lot) {
            avant.put(id, nombreEntrees(id));
        }

        // L une apres l autre, comme la page : les appels ne partiraient pas en
        // parallele, la soumission etant une ecriture d etat.
        for (Integer id : lot) {
            mockMvc.perform(post("/api/activites/{id}/soumettre-validation", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.statut").value("EN_ATTENTE_VALIDATION"));
        }

        for (Integer id : lot) {
            assertThat(nombreEntrees(id))
                    .as("Une seule ligne d historique par activite soumise")
                    .isEqualTo(avant.get(id) + 1);

            assertThat(statutCourant(id)).isEqualTo("EN_ATTENTE_VALIDATION");
        }

        // Aucune des trois n a pu disparaitre du lot : elles existent toutes
        // les trois, chacune avec sa propre transition.
        assertThat(lot).hasSize(3);
    }

    /**
     * Une activite refusee au milieu d un lot n annule pas les precedentes.
     *
     * C est la consequence directe de la transaction par requete, et ce que la
     * page fait de visible : elle retire les cards passees et garde les autres.
     */
    @Test
    @DisplayName("Une activite refusee dans un lot laisse les autres soumises")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void refusDansUnLotNAnnulePasLesPrecedentes() throws Exception {

        Integer premiere = rendreBrouillon(activitesDuService(1, 2).get(0));
        Integer dejaSoumise = activitesDuService(1, 3).get(2);

        mockMvc.perform(post("/api/activites/{id}/soumettre-validation", premiere))
                .andExpect(status().isOk());

        long entreesAvant = nombreEntrees(premiere);

        // Deuxieme activite du lot : elle n est pas en brouillon, la page la
        // garde donc avec son message au lieu de la faire disparaitre.
        mockMvc.perform(post("/api/activites/{id}/soumettre-validation", dejaSoumise))
                .andExpect(status().isConflict());

        assertThat(statutCourant(premiere)).isEqualTo("EN_ATTENTE_VALIDATION");
        assertThat(nombreEntrees(premiere)).isEqualTo(entreesAvant);
    }

    // ------------------------------------------------------------------
    // Perimetre
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Un utilisateur de service ne peut pas soumettre une activite d un autre service")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void soumissionHorsPerimetreRefusee() throws Exception {

        Integer id = rendreBrouillon(activiteDuService(1));

        mockMvc.perform(post("/api/activites/{id}/soumettre-validation", id))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").isNotEmpty());

        // Le refus ne doit rien ecrire : c est aussi ce qui distingue un
        // acces refuse d une transition refusee.
        assertThat(statutCourant(id)).isEqualTo("BROUILLON");
    }

    @Test
    @DisplayName("Un utilisateur de niveau departement peut soumettre dans tout son departement")
    @WithMockUser(username = UTILISATEUR_DEPARTEMENT)
    void niveauDepartementPeutSoumettre() throws Exception {

        Integer id = rendreBrouillon(activiteDuService(2));

        mockMvc.perform(post("/api/activites/{id}/soumettre-validation", id))
                .andExpect(status().isOk());

        assertThat(statutCourant(id)).isEqualTo("EN_ATTENTE_VALIDATION");
    }

    // ------------------------------------------------------------------
    // Erreurs
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Une activite inexistante renvoie 404")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void activiteInexistante() throws Exception {

        Integer inconnu = jdbc.queryForObject(
                "SELECT max(id_activite) + 1000 FROM activite", Integer.class);

        mockMvc.perform(post("/api/activites/{id}/soumettre-validation", inconnu))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Un identifiant hors bornes renvoie 400")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void identifiantInvalide() throws Exception {

        mockMvc.perform(post("/api/activites/{id}/soumettre-validation", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Une activite deja publiee au suivi ne peut pas etre soumise : 409")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void activitePasBrouillon() throws Exception {

        Integer id = activiteDuService(1);

        mockMvc.perform(post("/api/activites/{id}/soumettre-validation", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").isNotEmpty());

        // Aucun 404 : l activite existe et est visible, elle n est simplement
        // pas soumettable. Un 404 ferait croire a une faute de frappe.
        assertThat(statutCourant(id)).isNotEqualTo("BROUILLON");
    }

    @Test
    @DisplayName("Resoumettre une activite deja soumise renvoie 409 et n ajoute rien")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void doubleSoumissionRefusee() throws Exception {

        Integer id = rendreBrouillon(activiteDuService(1));

        mockMvc.perform(post("/api/activites/{id}/soumettre-validation", id))
                .andExpect(status().isOk());

        long apresPremiere = nombreEntrees(id);

        mockMvc.perform(post("/api/activites/{id}/soumettre-validation", id))
                .andExpect(status().isConflict());

        // Rendre l appel idempotent masquerait que l activite a deja quitte
        // les brouillons : le second appel doit le dire.
        assertThat(nombreEntrees(id)).isEqualTo(apresPremiere);
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    /**
     * Fabrique un etat de depart : l activite devient un brouillon.
     *
     * La date est posee apres la derniere entree existante, parce que c est
     * ce triplet -- date puis identifiant -- qui designe le statut courant.
     * Un CURRENT_TIMESTAMP suffirait sur une base recente, pas sur un jeu de
     * donnees planifie en 2027 : l ecriture y resterait dans le passe et
     * l activite conserverait son statut initial.
     */
    private Integer rendreBrouillon(Integer idActivite) {
        jdbc.update("""
                INSERT INTO historique_activite
                    (commentaire, date_changement, id_utilisateur, id_statut, id_activite)
                SELECT 'Mise en brouillon (test)',
                       COALESCE((
                           SELECT max(h.date_changement)
                           FROM historique_activite h
                           WHERE h.id_activite = ?), now())
                           + interval '1 second',
                       (SELECT id_utilisateur FROM utilisateur WHERE email = ?),
                       (SELECT id_statut FROM statut WHERE code = 'BROUILLON'),
                       ?
                """, idActivite, UTILISATEUR_SERVICE_UN, idActivite);

        assertThat(statutCourant(idActivite))
                .as("La mise en brouillon n a pas pris : precondition du test")
                .isEqualTo("BROUILLON");

        return idActivite;
    }

    /**
     * Statut courant, lu comme le lit le code applicatif.
     */
    private String statutCourant(Integer idActivite) {
        return jdbc.queryForObject("""
                SELECT sta.code
                FROM historique_activite h
                JOIN statut sta ON sta.id_statut = h.id_statut
                WHERE h.id_activite = ?
                ORDER BY h.date_changement DESC, h.id_historique_activite DESC
                LIMIT 1
                """, String.class, idActivite);
    }

    private long nombreEntrees(Integer idActivite) {
        Long nombre = jdbc.queryForObject(
                "SELECT count(*) FROM historique_activite WHERE id_activite = ?",
                Long.class, idActivite);

        return nombre == null ? 0L : nombre;
    }

    private Integer activiteDuService(int idService) {
        Integer id = jdbc.queryForObject(
                "SELECT min(id_activite) FROM activite WHERE id_service = ?",
                Integer.class, idService);

        assertThat(id).as(
                "Aucune activite dans le service %d : le jeu de donnees a change",
                idService).isNotNull();

        return id;
    }

    /**
     * Les N activites d un service, par ordre d identifiant.
     *
     * Un lot a besoin d activites distinctes : la transition est refusee sur
     * une activite deja soumise, et le test de lot ci-dessus s en sert pour
     * placer un refus au milieu.
     */
    private List<Integer> activitesDuService(int idService, int nombre) {
        List<Integer> ids = jdbc.queryForList(
                """
                SELECT id_activite
                FROM activite
                WHERE id_service = ?
                ORDER BY id_activite
                LIMIT ?
                """,
                Integer.class, idService, nombre);

        assertThat(ids).as(
                "Le service %d ne contient pas %d activites : le jeu de donnees a change",
                idService, nombre).hasSize(nombre);

        return ids;
    }

}

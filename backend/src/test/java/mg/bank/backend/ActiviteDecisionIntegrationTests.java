package mg.bank.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request
        .SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import mg.bank.backend.dto.ActiviteDecisionRequest;
import mg.bank.backend.service.ActiviteSoumissionService;

/**
 * Tests de la decision de validation d une activite soumise.
 *
 * Meme approche que les tests de soumission : une vraie base, dans une
 * transaction annulee en fin de test, et un etat de depart fabrique par le
 * test lui-meme plutot que suppose.
 *
 * CE QUE CES TESTS PROUVENT ET NE PROUVENT PAS
 * Ils prouvent que la decision ecrit dans les deux tables -- l etat dans
 * historique_activite, l examen dans validation_activite -- et que les trois
 * issues posibles aboutissent chacune a l etat attendu. Ils ne prouvent pas la
 * page : celle-ci est verifiee de son cote, et une page qui n appelerait pas cet
 * endpoint passerait tous ces tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActiviteDecisionIntegrationTests {

    /** Chef de service : voit et decide sur les activites de son service. */
    private static final String CHEF_SERVICE =
            "leila.mansouri@bfm.tn";

    /** Chef de departement : voit tout le departement. */
    private static final String CHEF_DEPARTEMENT =
            "karim.bensalah@bfm.tn";

    /** Rattache a un autre service : ne voit rien de ce qui suit. */
    private static final String AUTRE_SERVICE =
            "hatem.trabelsi@bfm.tn";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * Construit a la main plutot qu'injecte : le contexte de test n'expose pas
     * de bean ObjectMapper, et un corps de deux champs n'a pas besoin de la
     * configuration de l'application.
     */
    private final ObjectMapper json = new ObjectMapper();

    /**
     * Appelee directement, et non par HTTP : c'est elle qui fabrique l'etat de
     * depart des tests, demande de validation comprise. Passer par le
     * controleur rendrait le test dependant d'un autre endpoint.
     */
    @Autowired
    private ActiviteSoumissionService activiteSoumissionService;

    // ------------------------------------------------------------------
    // Valider
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Valider publie l activite au suivi")
    @WithMockUser(username = CHEF_SERVICE)
    void validerPublieLActivite() throws Exception {

        Integer id = activiteEnAttente();

        decider(id, "VALIDE", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decision").value("VALIDE"))
                .andExpect(jsonPath("$.data.statut").value("VALIDEE"));

        assertThat(statutCourant(id)).isEqualTo("VALIDEE");
    }

    @Test
    @DisplayName("Valider n'exige pas de commentaire")
    @WithMockUser(username = CHEF_SERVICE)
    void validerSansCommentaire() throws Exception {

        Integer id = activiteEnAttente();

        decider(id, "VALIDE", "   ").andExpect(status().isOk());

        // Un commentaire blanc n'est pas un motif : il ne doit pas etre ecrit
        // comme s il l avait ete.
        assertThat(commentaireDecision(id)).doesNotContain(":");
    }

    // ------------------------------------------------------------------
    // Rejeter
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Rejeter sort l activite du circuit")
    @WithMockUser(username = CHEF_SERVICE)
    void rejeterSortDuCircuit() throws Exception {

        Integer id = activiteEnAttente();

        decider(id, "REJETE", "Budget incoherent avec le plan")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statut").value("REJETE"));

        assertThat(statutCourant(id)).isEqualTo("REJETE");
        assertThat(decisionEnAttente(id)).isZero();
    }

    @Test
    @DisplayName("Retour en modification rend l activite de nouveau modifiable")
    @WithMockUser(username = CHEF_SERVICE)
    void retourEnModificationRedevientBrouillon() throws Exception {

        Integer id = activiteEnAttente();

        decider(id, "RETOUR_MODIFICATION", "Dates a corriger")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statut").value("BROUILLON"));

        // C est tout l interet de cette issue : seule une activite en brouillon
        // peut etre modifiee puis resoumise, donc c est elle qui rend le circuit
        // possible une seconde fois.
        assertThat(statutCourant(id)).isEqualTo("BROUILLON");
    }

    @Test
    @DisplayName("Un refus sans motif est refuse : 400")
    @WithMockUser(username = CHEF_SERVICE)
    void refusSansMotifRefuse() throws Exception {

        Integer id = activiteEnAttente();
        long entreesAvant = nombreEntrees(id);

        decider(id, "REJETE", "   ").andExpect(status().isBadRequest());

        // Rien n'est ecrit : l'utilisateur doit pouvoir corriger et reessayer.
        assertThat(statutCourant(id)).isEqualTo("EN_ATTENTE_VALIDATION");
        assertThat(nombreEntrees(id)).isEqualTo(entreesAvant);
        assertThat(decisionEnAttente(id)).isEqualTo(1);
    }

    @Test
    @DisplayName("Le motif du refus est conserve dans les deux tables")
    @WithMockUser(username = CHEF_SERVICE)
    void motifConservePartout() throws Exception {

        Integer id = activiteEnAttente();

        decider(id, "REJETE", "Objectif hors perimetre du service")
                .andExpect(status().isOk());

        // L'auteur doit retrouver le motif en ouvrant l'activite, et le
        // responsable en relisant l historique : les deux lectures portent donc
        // le texte, prefixe par leur type de ligne.
        assertThat(commentaireDecision(id))
                .contains("Rejetee a la validation")
                .contains("Objectif hors perimetre du service");

        assertThat(commentaireExamen(id))
                .isEqualTo("Objectif hors perimetre du service");
    }

    // ------------------------------------------------------------------
    // Regles
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Une decision est unique : la seconde renvoie 409 et n'ecrit rien")
    @WithMockUser(username = CHEF_SERVICE)
    void doubleDecisionRefusee() throws Exception {

        Integer id = activiteEnAttente();

        decider(id, "VALIDE", null).andExpect(status().isOk());

        long entreesApres = nombreEntrees(id);

        decider(id, "REJETE", "Finalement non")
                .andExpect(status().isConflict());

        // Une decision deja prise ne se rejoue pas : le responsable qui reclique
        // ne doit pas pouvoir transformer une validation en rejet.
        assertThat(statutCourant(id)).isEqualTo("VALIDEE");
        assertThat(nombreEntrees(id)).isEqualTo(entreesApres);
    }

    @Test
    @DisplayName("Une activite qui n attend pas de decision renvoie 409")
    @WithMockUser(username = CHEF_SERVICE)
    void activitePasEnAttente() throws Exception {

        Integer id = rendreBrouillon(idActivite());

        decider(id, "VALIDE", null).andExpect(status().isConflict());

        assertThat(statutCourant(id)).isEqualTo("BROUILLON");
    }

    @Test
    @DisplayName("Une decision inconnue renvoie 400")
    @WithMockUser(username = CHEF_SERVICE)
    void decisionInconnue() throws Exception {

        Integer id = activiteEnAttente();

        // Un corps vient du client : il peut contenir un statut de suivi, ou
        // n'importe quoi. Ni l'un ni l'autre ne doit passer.
        decider(id, "TERMINEE", null).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Une activite hors perimetre renvoie 403 et n'ecrit rien")
    @WithMockUser(username = CHEF_SERVICE)
    void horsPerimetre() throws Exception {

        // Le chef de service prepare l'activite...
        Integer id = activiteEnAttente(1);

        // ... et c'est un colleague d'un autre service qui tente la decision.
        deciderEnTantQue(id, "VALIDE", null, AUTRE_SERVICE)
                .andExpect(status().isForbidden());

        // Le refus d'acces ne doit rien laisser derriere lui.
        assertThat(statutCourant(id)).isEqualTo("EN_ATTENTE_VALIDATION");
    }

    @Test
    @DisplayName("Le chef de departement decide sur les deux services")
    @WithMockUser(username = CHEF_DEPARTEMENT)
    void chefDepartementDecidePartout() throws Exception {

        Integer id = activiteEnAttente(2);

        decider(id, "VALIDE", null).andExpect(status().isOk());

        assertThat(statutCourant(id)).isEqualTo("VALIDEE");
    }

    // ------------------------------------------------------------------
    // Lot : la page envoie une requete par activite
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Trois decisions en suite valident trois activites")
    @WithMockUser(username = CHEF_SERVICE)
    void lotDeDecisions() throws Exception {

        var lot = jdbc.queryForList("""
                SELECT id_activite
                FROM activite
                WHERE id_service = 1
                ORDER BY id_activite
                LIMIT 3
                """, Integer.class).stream()
                .map(id -> activiteEnAttente(id))
                .toList();

        for (Integer id : lot) {
            decider(id, "VALIDE", null).andExpect(status().isOk());
        }

        for (Integer id : lot) {
            assertThat(statutCourant(id)).isEqualTo("VALIDEE");
            assertThat(decisionEnAttente(id)).isZero();
        }
    }

    @Test
    @DisplayName("Une activite deja decidee dans un lot ne gache pas les autres")
    @WithMockUser(username = CHEF_SERVICE)
    void refusDansUnLotDeDecisions() throws Exception {

        Integer premiere = activiteEnAttente(idActivite());

        // Deuxieme activite : son etat est coherent, mais aucune demande
        // n attends, ce que la page ne peut pas voir.
        /*
            Cette activite est dans le bon etat mais sans demande : c est le cas
            d une activite soumise avant que la soumission n'en enregistre une.
            La page ne peut pas le voir, et la decision doit le dire plutot que
            d'ecrire une decision dans le vide.
        */
        Integer sansDemande = rendreBrouillon(idActiviteSuivant());
        soumettreSansDemande(sansDemande);

        decider(premiere, "VALIDE", null).andExpect(status().isOk());

        decider(sansDemande, "VALIDE", null)
                .andExpect(status().isConflict());

        // La premiere reste validee : chaque requete est sa propre transaction.
        assertThat(statutCourant(premiere)).isEqualTo("VALIDEE");
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    /**
     * L identite vient de @WithMockUser, qui la reapplique a chaque requete.
     * Relire le contexte pour refaire la meme chose serait fragile : la chaine
     * de securite le vide apres un premier appel dans un test donne.
     */
    private ResultActions decider(
            Integer idActivite, String decision, String commentaire) throws Exception {

        return performer(idActivite, decision, commentaire, null);
    }

    /**
     * Utile quand la requete doit etre faite par quelqu'un d'autre que celui
     * qui a fabrique l etat de depart : sans cela, on ne pourrait pas tester le
     * refus d'acces, puisque la preparation elle-meme serait refusee avant
     * d'atteindre le controleur.
     */
    private ResultActions deciderEnTantQue(
            Integer idActivite, String decision, String commentaire, String utilisateur)
            throws Exception {

        return performer(idActivite, decision, commentaire, utilisateur);
    }

    private ResultActions performer(
            Integer idActivite, String decision, String commentaire, String utilisateur)
            throws Exception {

        ActiviteDecisionRequest corps = new ActiviteDecisionRequest();
        corps.setDecision(decision);
        corps.setCommentaire(commentaire);

        var requete = post("/api/activites/{id}/decision", idActivite)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(corps));

        if (utilisateur != null) {
            requete = requete.with(SecurityMockMvcRequestPostProcessors.user(utilisateur));
        }

        return mockMvc.perform(requete);
    }
    /**
     * Activite soumise et en attente de decision.
     *
     * L etat est fabrique par la soumission elle-meme, appelee par le service
     * plutot que par une insertion directe : c est ainsi qu on obtient une
     * demande de validation coherente avec l historique, ce qu une insertion
     * manuelle ne garantirait pas.
     */
    private Integer activiteEnAttente() {
        return activiteEnAttente(idActivite());
    }

    private Integer activiteEnAttente(Integer idActivite) {
        /*
            Le jeu de donnees ne contient aucune activite en brouillon : toutes
            ont parcouru le circuit. Le test la remet donc en brouillon avant de
            la soumettre, comme le font les tests de soumission. Sans cette
            remise a niveau, la soumission serait refusee et le test testerait
            autre chose.
        */
        rendreBrouillon(idActivite);
        activiteSoumissionService.soumettre(idActivite);

        assertThat(statutCourant(idActivite))
                .as("La soumission n a pas pris : precondition du test")
                .isEqualTo("EN_ATTENTE_VALIDATION");

        assertThat(decisionEnAttente(idActivite))
                .as("La soumission doit ouvrir une demande de validation")
                .isEqualTo(1);

        return idActivite;
    }

    /**
     * Pose le statut EN_ATTENTE_VALIDATION sans ouvrir de demande.
     */
    private void soumettreSansDemande(Integer idActivite) {
        jdbc.update("""
                INSERT INTO historique_activite
                    (commentaire, date_changement, id_utilisateur, id_statut, id_activite)
                SELECT 'Soumission sans circuit (test)',
                       COALESCE((SELECT max(h.date_changement)
                                 FROM historique_activite h
                                 WHERE h.id_activite = ?), now())
                           + interval '1 second',
                       (SELECT id_utilisateur FROM utilisateur WHERE email = ?),
                       (SELECT id_statut FROM statut WHERE code = 'EN_ATTENTE_VALIDATION'),
                       ?
                """, idActivite, CHEF_SERVICE, idActivite);

        assertThat(statutCourant(idActivite)).isEqualTo("EN_ATTENTE_VALIDATION");
        assertThat(decisionEnAttente(idActivite)).isZero();
    }

    private Integer rendreBrouillon(Integer idActivite) {
        jdbc.update("""
                INSERT INTO historique_activite
                    (commentaire, date_changement, id_utilisateur, id_statut, id_activite)
                SELECT 'Mise en brouillon (test)',
                       COALESCE((SELECT max(h.date_changement)
                                 FROM historique_activite h
                                 WHERE h.id_activite = ?), now())
                           + interval '1 second',
                       (SELECT id_utilisateur FROM utilisateur WHERE email = ?),
                       (SELECT id_statut FROM statut WHERE code = 'BROUILLON'),
                       ?
                """, idActivite, CHEF_SERVICE, idActivite);

        return idActivite;
    }

    private Integer idActivite() {
        return idActiviteDuService(1);
    }

    private Integer idActiviteSuivant() {
        return jdbc.queryForObject("""
                SELECT min(id_activite)
                FROM activite
                WHERE id_service = 1
                  AND id_activite > (
                      SELECT min(id_activite) FROM activite WHERE id_service = 1)
                """, Integer.class);
    }

    private Integer idActiviteDuService(int idService) {
        Integer id = jdbc.queryForObject(
                "SELECT min(id_activite) FROM activite WHERE id_service = ?",
                Integer.class, idService);

        assertThat(id).as(
                "Aucune activite dans le service %d : le jeu de donnees a change",
                idService).isNotNull();

        return id;
    }

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

    private String commentaireDecision(Integer idActivite) {
        return jdbc.queryForObject("""
                SELECT h.commentaire
                FROM historique_activite h
                WHERE h.id_activite = ?
                ORDER BY h.date_changement DESC, h.id_historique_activite DESC
                LIMIT 1
                """, String.class, idActivite);
    }

    private String commentaireExamen(Integer idActivite) {
        return jdbc.queryForObject("""
                SELECT v.commentaire
                FROM validation_activite v
                WHERE v.id_activite = ?
                ORDER BY v.date_demande DESC, v.id_validation_activite DESC
                LIMIT 1
                """, String.class, idActivite);
    }

    private long nombreEntrees(Integer idActivite) {
        Long nombre = jdbc.queryForObject(
                "SELECT count(*) FROM historique_activite WHERE id_activite = ?",
                Long.class, idActivite);

        return nombre == null ? 0L : nombre;
    }

    private long decisionEnAttente(Integer idActivite) {
        Long nombre = jdbc.queryForObject("""
                SELECT count(*)
                FROM validation_activite
                WHERE id_activite = ?
                  AND decision = 'EN_ATTENTE_VALIDATION'
                """, Long.class, idActivite);

        return nombre == null ? 0L : nombre;
    }

    private long demandesOuvertes(Integer idActivite) {
        return decisionEnAttente(idActivite);
    }
}

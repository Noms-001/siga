package mg.bank.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import mg.bank.backend.enums.DecisionValidation;

/**
 * Tests du detail d une activite, exerces sur la base de developpement.
 *
 * Ces tests ne simulent pas la base : ils la lisent. Le detail repose sur des
 * requetes natives dont le comportement -- dernier historique, enums PostgreSQL,
 * perimetre -- ne peut pas etre verifie sur une base en memoire, qui ne
 * reproduirait ni les types enum ni les contraintes de tableau.
 *
 * Aucun identifiant n est fige dans le code. Les activites utilisees sont
 * recherchees par leurs proprietes, ce qui evite que le test echoue sur un
 * decalage d identifiants et, surtout, qu il passe a vide si le jeu de
 * donnees change : une recherche qui ne trouve rien est explicitement une
 * erreur.
 *
 * L authentification passe par @WithMockUser plutot que par un mot de passe :
 * le service lit l identite dans le contexte de securite, et c est
 * exactement ce que le test doit reproduire. Aucun mot de passe de test n est
 * donc necessaire, et aucun utilisateur de test n est cree en base.
 */
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureMockMvc
@Transactional
class ActiviteDetailIntegrationTests {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Niveau departement : voit tous les services du departement. */
    private static final String UTILISATEUR_DEPARTEMENT =
            "jean.rakoto@bfm-demo.local";

    /** Rattache au service 1 : ne voit que le service 1. */
    private static final String UTILISATEUR_SERVICE_UN =
            "marie.razafindrakoto@bfm-demo.local";

    /** Rattache au service 2 : ne voit que le service 2. */
    private static final String UTILISATEUR_SERVICE_DEUX =
            "tiana.randrianarison@bfm-demo.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private jakarta.persistence.EntityManagerFactory entityManagerFactory;

    // ------------------------------------------------------------------
    // Acces et perimetre
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Une activite du perimetre renvoie son detail complet")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void detailAccessible() throws Exception {

        Integer id = activiteDuService(2);

        mockMvc.perform(get("/api/activites/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.code").isNotEmpty())
                .andExpect(jsonPath("$.data.designation").isNotEmpty())
                .andExpect(jsonPath("$.data.statut.code").isNotEmpty())
                .andExpect(jsonPath("$.data.service.libelle").isNotEmpty())
                .andExpect(jsonPath("$.data.historique").isArray())
                .andExpect(jsonPath("$.data.sousActivites").isArray())
                .andExpect(jsonPath("$.data.validations").isArray());
    }

    @Test
    @DisplayName("Un utilisateur de service ne peut pas lire une activite d un autre service")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void activiteHorsPerimetreRefusee() throws Exception {

        Integer id = activiteDuService(2);

        mockMvc.perform(get("/api/activites/{id}", id))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").isNotEmpty())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("Un utilisateur de niveau departement voit tous les services de son departement")
    @WithMockUser(username = UTILISATEUR_DEPARTEMENT)
    void niveauDepartementVoitLesTousServices() throws Exception {

        // Les deux activites sont dans deux services differents du meme
        // departement : un utilisateur de niveau departement les voit toutes
        // les deux, alors qu un utilisateur de service n en voit qu une.
        Integer serviceUn = activiteDuService(1);
        Integer serviceDeux = activiteDuService(2);

        mockMvc.perform(get("/api/activites/{id}", serviceUn))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/activites/{id}", serviceDeux))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("L activite existe mais est hors perimetre : 403 et non 404")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void horsPerimetreDistingueDeAbsente() throws Exception {

        Integer id = activiteDuService(2);

        // Elle existe reellement : le 403 doit donc porter sur l acces, et non
        // deguise absence. Le cas nominal est couvert par detailAccessible, ou
        // un utilisateur du bon service obtient bien 200 sur une activite du
        // meme service.
        assertThat(activiteExiste(id)).isTrue();

        mockMvc.perform(get("/api/activites/{id}", id))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un identifiant inexistant renvoie 404")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void activiteInexistante() throws Exception {

        Integer inexistant = (int) jdbc.queryForObject(
                "SELECT COALESCE(MAX(id_activite), 0) + 100000 FROM activite",
                Integer.class);

        mockMvc.perform(get("/api/activites/{id}", inexistant))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Sans authentification, l acces est refuse par la couche securite")
    void accesAnonymeRefuse() throws Exception {

        // 403 et non 401, et c est le comportement general du projet : la
        // SecurityFilterChain ne declare aucun point d entree
        // d authentification, formLogin et httpBasic etant desactives. Une
        // requete anonyme se heurte donc a un acces refuse plutot qu a une
        // invitation a se connecter. Corriger cela ferait passer de 401 a 403
        // tous les endpoints existants : c est une decision commune a
        // SecurityConfig, pas une propriete de cet endpoint, et elle est
        // volontairement laissee en l etat ici. Le test fixe le comportement
        // reel pour qu un changement ulterieur soit visible.
        mockMvc.perform(get("/api/activites/{id}", activiteDuService(2)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un identifiant nul ou negatif est refuse par la validation")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void identifiantInvalide() throws Exception {

        mockMvc.perform(get("/api/activites/{id}", 0))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Collections
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Une activite sans indicateur ni resultat expose des listes vides, jamais null")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void collectionsVidesNonNulles() throws Exception {

        Integer id = activiteSansIndicateurNiResultat(2);

        String corps = mockMvc.perform(get("/api/activites/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.indicateurs").isArray())
                .andExpect(jsonPath("$.data.indicateurs").isEmpty())
                .andExpect(jsonPath("$.data.resultatsIntermediaires").isArray())
                .andExpect(jsonPath("$.data.resultatsIntermediaires").isEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(corps).doesNotContain("\"indicateurs\":null");
        assertThat(corps).doesNotContain("\"resultatsIntermediaires\":null");
    }

    // ------------------------------------------------------------------
    // Coherence des donnees derivees
    // ------------------------------------------------------------------

    @Test
    @DisplayName("L historique est decroissant et sa premiere ligne porte le statut courant")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void historiqueDecroissantEtStatutCoherent() throws Exception {

        Integer id = activiteDuService(2);

        String corps = mockMvc.perform(get("/api/activites/{id}", id))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, Object>> historique = lireHistorique(corps);
        assertThat(historique).isNotEmpty();

        // Tri decroissant sur dateChangement.
        List<String> dates = historique.stream()
                .map(ligne -> (String) ligne.get("dateChangement"))
                .toList();
        assertThat(dates).isSortedAccordingTo(
                java.util.Comparator.reverseOrder());

        // Une seule ligne courante, et c est la premiere.
        long courantes = historique.stream()
                .filter(ligne -> Boolean.TRUE.equals(ligne.get("etatActuel")))
                .count();
        assertThat(courantes).isEqualTo(1);
        assertThat(Boolean.TRUE.equals(historique.getFirst().get("etatActuel")))
                .isTrue();

        // Elle porte exactement le statut affiche au niveau de l activite.
        @SuppressWarnings("unchecked")
        Map<String, Object> statutActivite =
                (Map<String, Object>) racine(corps).get("statut");
        @SuppressWarnings("unchecked")
        Map<String, Object> statutCourant =
                (Map<String, Object>) historique.getFirst().get("statut");

        assertThat(statutCourant.get("code")).isEqualTo(statutActivite.get("code"));
    }

    @Test
    @DisplayName("L avancement global est la moyenne des derniers avancements, comme dans la liste")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void avancementGlobalCoherent() throws Exception {

        Integer id = activiteDuService(2);

        String corps = mockMvc.perform(get("/api/activites/{id}", id))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Meme formule que celle de la liste, calculee independamment.
        Double attendu = jdbc.queryForObject("""
                SELECT COALESCE(AVG(dernier.valeur_pourcentage), 0)
                FROM sous_activite sa
                JOIN LATERAL (
                    SELECT av.valeur_pourcentage
                    FROM avancement_sous_activite av
                    WHERE av.id_sous_activite = sa.id_sous_activite
                      AND NOT EXISTS (
                          SELECT 1 FROM avancement_sous_activite av2
                          WHERE av2.id_sous_activite = sa.id_sous_activite
                            AND (av2.date_changement, av2.id_historique_sous_activite)
                                > (av.date_changement, av.id_historique_sous_activite))
                ) dernier ON TRUE
                WHERE sa.id_activite = ?
                """, Double.class, id);

        assertThat(((Number) racine(corps).get("avancement")).doubleValue())
                .isEqualTo(attendu);
    }

    @Test
    @DisplayName("Une sous-activite sans releve expose un avancement courant null")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void sousActiviteSansReleve() throws Exception {

        Integer sousActivite = jdbc.queryForObject("""
                SELECT sa.id_sous_activite
                FROM sous_activite sa
                JOIN activite a ON a.id_activite = sa.id_activite
                WHERE a.id_service = 2
                  AND NOT EXISTS (
                      SELECT 1 FROM avancement_sous_activite av
                      WHERE av.id_sous_activite = sa.id_sous_activite)
                LIMIT 1
                """, Integer.class);

        assertThat(sousActivite).isNotNull();
        Integer activite = jdbc.queryForObject(
                "SELECT id_activite FROM sous_activite WHERE id_sous_activite = ?",
                Integer.class, sousActivite);

        String corps = mockMvc.perform(get("/api/activites/{id}", activite))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sousActivites =
                (List<Map<String, Object>>) racine(corps).get("sousActivites");

        Map<String, Object> cible = sousActivites.stream()
                .filter(ligne -> ((Number) ligne.get("id")).intValue()
                        == sousActivite)
                .findFirst()
                .orElseThrow();

        assertThat(cible).containsEntry("avancementCourant", null);
        assertThat((List<?>) cible.get("historiqueAvancement")).isEmpty();
    }

    @Test
    @DisplayName("Les decisions de validation sont lues comme l enum DecisionValidation")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void decisionsValides() throws Exception {

        String corps = mockMvc.perform(
                get("/api/activites/{id}", activiteDuService(2)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> validations =
                (List<Map<String, Object>>) racine(corps).get("validations");

        assertThat(validations).isNotEmpty();

        for (Map<String, Object> validation : validations) {
            String decision = (String) validation.get("decision");

            // Parse par l enum : une valeur inattendue ferait echouer le test
            // plutot que de passer en silence dans la reponse.
            assertThat(DecisionValidation.valueOf(decision)).isNotNull();

            @SuppressWarnings("unchecked")
            Map<String, Object> etape = (Map<String, Object>) validation.get("etape");
            assertThat(etape.get("designation")).isNotNull();
            assertThat(etape.get("niveau")).isNotNull();
        }
    }

    // ------------------------------------------------------------------
    // Nombre de requetes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Le nombre de requetes ne croit pas avec le nombre de livrables")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void pasDeNPlusUn() throws Exception {

        Integer avecDeuxLivrables = activiteAvecNLivrables(2, 2);
        Integer avecUnLivrable = activiteAvecNLivrables(2, 1);

        assertThat(avecDeuxLivrables).isNotNull();
        assertThat(avecUnLivrable).isNotNull();

        long requetesDetailLourd = compterRequetes(avecDeuxLivrables);
        long requetesDetailLeger = compterRequetes(avecUnLivrable);

        // Deux lectures de plus d un livrable dans la premiere activite, donc
        // une relation un-pour-plusieurs. Si le nombre de requetes suivait, la
        // comparaison le prouverait.
        assertThat(requetesDetailLourd).isEqualTo(requetesDetailLeger);

        // Et le total reste borne : une activity parente, une par type de
        // donnee enfant, plus la lecture de l utilisateur courant.
        assertThat(requetesDetailLourd).isLessThanOrEqualTo(15L);
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    private long compterRequetes(Integer idActivite) throws Exception {

        // getStatistics n existe pas sur l interface EntityManagerFactory :
        // il faut descendre jusqu a la SessionFactory de Hibernate.
        Statistics statistiques = entityManagerFactory
                .unwrap(SessionFactory.class)
                .getStatistics();

        statistiques.clear();

        mockMvc.perform(get("/api/activites/{id}", idActivite))
                .andExpect(status().isOk());

        return statistiques.getQueryExecutionCount();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> racine(String corps) {
        try {
            JsonNode arbre = MAPPER.readTree(corps);
            return MAPPER.convertValue(arbre.get("data"), Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Reponse illisible", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> lireHistorique(String corps) throws Exception {
        return (List<Map<String, Object>>) racine(corps).get("historique");
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

    private Integer activiteSansIndicateurNiResultat(int idService) {
        Integer id = jdbc.queryForObject("""
                SELECT min(a.id_activite)
                FROM activite a
                WHERE a.id_service = ?
                  AND NOT EXISTS (
                      SELECT 1 FROM activite_indicateur i
                      WHERE i.id_activite = a.id_activite)
                  AND NOT EXISTS (
                      SELECT 1 FROM resultat_intermediaire r
                      WHERE r.id_activite = a.id_activite)
                """, Integer.class, idService);

        assertThat(id).as(
                "Aucune activite sans indicateur ni resultat dans le service %d",
                idService).isNotNull();

        return id;
    }

    private Integer activiteAvecNLivrables(int idService, int nombre) {
        return jdbc.queryForObject("""
                SELECT min(a.id_activite)
                FROM activite a
                WHERE a.id_service = ?
                  AND (
                      SELECT count(*)
                      FROM livrable_sous_activite l
                      JOIN sous_activite sa
                            ON sa.id_sous_activite = l.id_sous_activite
                      WHERE sa.id_activite = a.id_activite
                  ) = ?
                """, Integer.class, idService, nombre);
    }

    private boolean activiteExiste(Integer id) {
        Integer compte = jdbc.queryForObject(
                "SELECT count(*) FROM activite WHERE id_activite = ?",
                Integer.class, id);

        return compte != null && compte > 0;
    }

}

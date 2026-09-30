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

/**
 * Tests du detail d une sous-activite, exerces sur la base de developpement.
 *
 * Memes principes que ActiviteDetailIntegrationTests : aucune base en
 * memoire, aucun identifiant fige, l identite est simulee par WithMockUser,
 * et une recherche qui ne trouve rien est une erreur explicite plutot qu un
 * test passe a vide.
 */
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureMockMvc
@Transactional
class SousActiviteDetailIntegrationTests {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
    @DisplayName("Une sous-activite du perimetre renvoie son detail complet")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void detailAccessible() throws Exception {

        Integer sousActivite = sousActiviteDuService(2);
        Integer activite = activiteDe(sousActivite);

        String corps = mockMvc.perform(
                get("/api/activites/{idActivite}/sous-activites/{idSousActivite}",
                        activite, sousActivite))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(sousActivite))
                .andExpect(jsonPath("$.data.code").isNotEmpty())
                .andExpect(jsonPath("$.data.designation").isNotEmpty())
                .andExpect(jsonPath("$.data.affectations").isArray())
                .andExpect(jsonPath("$.data.livrables").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(corps).doesNotContain("\"historiqueAvancement\":null");
        assertThat(corps).doesNotContain("\"affectations\":null");
        assertThat(corps).doesNotContain("\"livrables\":null");
        assertThat(corps).doesNotContain("\"fichiers\":null");
    }

    @Test
    @DisplayName("Une sous-activite d une activite hors perimetre est refusee (403)")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void sousActiviteHorsPerimetreRefusee() throws Exception {

        Integer sousActivite = sousActiviteDuService(2);
        Integer activite = activiteDe(sousActivite);

        // Elle existe reellement : le 403 doit porter sur l acces.
        assertThat(sousActiviteExiste(sousActivite)).isTrue();

        mockMvc.perform(get("/api/activites/{idActivite}/sous-activites/{idSousActivite}",
                        activite, sousActivite))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").isNotEmpty())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("Une sous-activite inexistante renvoie 404")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void sousActiviteInexistante() throws Exception {

        Integer activite = activiteDuService(2);
        Integer inexistant = (int) jdbc.queryForObject(
                "SELECT COALESCE(MAX(id_sous_activite), 0) + 100000 FROM sous_activite",
                Integer.class);

        mockMvc.perform(get("/api/activites/{idActivite}/sous-activites/{idSousActivite}",
                        activite, inexistant))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Une sous-activite d une autre activite renvoie 404")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void sousActiviteDAutreActivite() throws Exception {

        // Deux activites distinctes du meme service : la sous-activite de la
        // premiere demande la deuxieme, dans le perimetre, et aboutit a 404.
        Integer sousActivite = sousActiviteDuService(2);
        Integer activiteUnee = activiteDe(sousActivite);
        Integer activiteAutre = activiteDuService(2);

        if (activiteAutre == activiteUnee) {
            activiteAutre = (int) jdbc.queryForObject("""
                    SELECT id_activite
                    FROM activite
                    WHERE id_service = 2
                      AND id_activite <> ?
                    LIMIT 1
                    """, Integer.class, activiteUnee);

            assertThat(activiteAutre).as(
                    "Aucune seconde activite dans le service 2 : le jeu de donnees a change")
                    .isNotNull();
        }

        mockMvc.perform(get("/api/activites/{idActivite}/sous-activites/{idSousActivite}",
                        activiteAutre, sousActivite))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Une activite inexistante avec une sous-activite existante renvoie 404")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void activiteInexistante() throws Exception {

        Integer sousActivite = sousActiviteDuService(2);
        Integer inexistant = (int) jdbc.queryForObject(
                "SELECT COALESCE(MAX(id_activite), 0) + 100000 FROM activite",
                Integer.class);

        mockMvc.perform(get("/api/activites/{idActivite}/sous-activites/{idSousActivite}",
                        inexistant, sousActivite))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Sans authentification, l acces est refuse par la couche securite")
    void accesAnonymeRefuse() throws Exception {

        mockMvc.perform(get(
                        "/api/activites/{idActivite}/sous-activites/{idSousActivite}",
                        activiteDe(sousActiviteDuService(2)),
                        sousActiviteDuService(2)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un identifiant nul ou negatif est refuse par la validation")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void identifiantInvalide() throws Exception {

        Integer sousActivite = sousActiviteDuService(2);
        Integer activite = activiteDe(sousActivite);

        mockMvc.perform(get(
                        "/api/activites/{idActivite}/sous-activites/{idSousActivite}",
                        0, sousActivite))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(
                        "/api/activites/{idActivite}/sous-activites/{idSousActivite}",
                        activite, 0))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Coherence des donnees derivees
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Une sous-activite sans releve expose un avancement courant null")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void sousActiviteSansReleve() throws Exception {

        Integer sousActivite = sousActiviteSansReleve(2);
        Integer activite = activiteDe(sousActivite);

        String corps = mockMvc.perform(
                get("/api/activites/{idActivite}/sous-activites/{idSousActivite}",
                        activite, sousActivite))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> detail = racine(corps);

        assertThat(detail).containsEntry("avancementCourant", null);
        assertThat((List<?>) detail.get("historiqueAvancement")).isEmpty();
    }

    @Test
    @DisplayName("L historique est decroissant et sa premiere ligne est l avancement courant")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void historiqueDecroissantEtAvancementCourantCoherent() throws Exception {

        Integer sousActivite = sousActiviteAvecReleve(2);
        Integer activite = activiteDe(sousActivite);

        String corps = mockMvc.perform(
                get("/api/activites/{idActivite}/sous-activites/{idSousActivite}",
                        activite, sousActivite))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> detail = racine(corps);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> historique =
                (List<Map<String, Object>>) detail.get("historiqueAvancement");
        assertThat(historique).isNotEmpty();

        List<String> dates = historique.stream()
                .map(ligne -> (String) ligne.get("dateChangement"))
                .toList();
        assertThat(dates).isSortedAccordingTo(
                java.util.Comparator.reverseOrder());

        Long courantes = historique.stream()
                .filter(ligne -> Boolean.TRUE.equals(ligne.get("etatActuel")))
                .count();
        assertThat(courantes).isEqualTo(1);
        assertThat(Boolean.TRUE.equals(historique.getFirst().get("etatActuel")))
                .isTrue();

        // L avancement courant expose est exactement la premiere ligne.
        @SuppressWarnings("unchecked")
        Map<String, Object> courant =
                (Map<String, Object>) detail.get("avancementCourant");
        assertThat(courant.get("id"))
                .isEqualTo(((Number) historique.getFirst().get("id")).intValue());
        assertThat(((Number) courant.get("valeurPourcentage")).doubleValue())
                .isEqualTo(((Number) historique.getFirst().get("valeurPourcentage")).doubleValue());
    }

    @Test
    @DisplayName("L avancement courant d une sous-activite vaut son dernier releve")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void avancementCourantCoherent() throws Exception {

        Integer sousActivite = sousActiviteAvecReleve(2);
        Integer activite = activiteDe(sousActivite);

        Double attendu = jdbc.queryForObject("""
                SELECT av.valeur_pourcentage
                FROM avancement_sous_activite av
                WHERE av.id_sous_activite = ?
                  AND NOT EXISTS (
                      SELECT 1 FROM avancement_sous_activite av2
                      WHERE av2.id_sous_activite = av.id_sous_activite
                        AND (av2.date_changement, av2.id_historique_sous_activite)
                            > (av.date_changement, av.id_historique_sous_activite))
                """, Double.class, sousActivite);

        String corps = mockMvc.perform(
                get("/api/activites/{idActivite}/sous-activites/{idSousActivite}",
                        activite, sousActivite))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> courant =
                (Map<String, Object>) racine(corps).get("avancementCourant");

        assertThat(((Number) courant.get("valeurPourcentage")).doubleValue())
                .isEqualTo(attendu);
    }

    // ------------------------------------------------------------------
    // Nombre de requetes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Le nombre de requetes ne croit pas avec le nombre de livrables")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void pasDeNPlusUn() throws Exception {

        Integer avecDeuxLivrables = sousActiviteAvecNLivrables(2, 2);
        Integer avecUnLivrable = sousActiviteAvecNLivrables(2, 1);

        assertThat(avecDeuxLivrables).isNotNull();
        assertThat(avecUnLivrable).isNotNull();

        long requetesLourd = compterRequetes(avecDeuxLivrables);
        long requetesLeger = compterRequetes(avecUnLivrable);

        assertThat(requetesLourd).isEqualTo(requetesLeger);

        // Une sous-activite, son historique, ses affectations, ses livrables,
        // ses fichiers, plus la lecture de l utilisateur courant : le total
        // reste borne et ne suit pas le nombre de livrables.
        assertThat(requetesLourd).isLessThanOrEqualTo(8L);
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    private long compterRequetes(Integer idSousActivite) throws Exception {

        Statistics statistiques = entityManagerFactory
                .unwrap(SessionFactory.class)
                .getStatistics();

        statistiques.clear();

        mockMvc.perform(get(
                        "/api/activites/{idActivite}/sous-activites/{idSousActivite}",
                        activiteDe(idSousActivite), idSousActivite))
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

    private Integer activiteDuService(int idService) {
        Integer id = jdbc.queryForObject(
                "SELECT min(id_activite) FROM activite WHERE id_service = ?",
                Integer.class, idService);

        assertThat(id).as(
                "Aucune activite dans le service %d : le jeu de donnees a change",
                idService).isNotNull();

        return id;
    }

    private Integer sousActiviteDuService(int idService) {
        Integer id = jdbc.queryForObject("""
                SELECT min(sa.id_sous_activite)
                FROM sous_activite sa
                JOIN activite a
                      ON a.id_activite = sa.id_activite
                WHERE a.id_service = ?
                """, Integer.class, idService);

        assertThat(id).as(
                "Aucune sous-activite dans le service %d : le jeu de donnees a change",
                idService).isNotNull();

        return id;
    }

    private Integer activiteDe(Integer idSousActivite) {
        return jdbc.queryForObject(
                "SELECT id_activite FROM sous_activite WHERE id_sous_activite = ?",
                Integer.class, idSousActivite);
    }

    private Integer sousActiviteSansReleve(int idService) {
        Integer id = jdbc.queryForObject("""
                SELECT min(sa.id_sous_activite)
                FROM sous_activite sa
                JOIN activite a
                      ON a.id_activite = sa.id_activite
                WHERE a.id_service = ?
                  AND NOT EXISTS (
                      SELECT 1 FROM avancement_sous_activite av
                      WHERE av.id_sous_activite = sa.id_sous_activite)
                """, Integer.class, idService);

        assertThat(id).as(
                "Aucune sous-activite sans releve dans le service %d",
                idService).isNotNull();

        return id;
    }

    private Integer sousActiviteAvecReleve(int idService) {
        Integer id = jdbc.queryForObject("""
                SELECT min(sa.id_sous_activite)
                FROM sous_activite sa
                JOIN activite a
                      ON a.id_activite = sa.id_activite
                WHERE a.id_service = ?
                  AND EXISTS (
                      SELECT 1 FROM avancement_sous_activite av
                      WHERE av.id_sous_activite = sa.id_sous_activite)
                """, Integer.class, idService);

        assertThat(id).as(
                "Aucune sous-activite avec releve dans le service %d",
                idService).isNotNull();

        return id;
    }

    private Integer sousActiviteAvecNLivrables(int idService, int nombre) {
        return jdbc.queryForObject("""
                SELECT min(sa.id_sous_activite)
                FROM sous_activite sa
                JOIN activite a
                      ON a.id_activite = sa.id_activite
                WHERE a.id_service = ?
                  AND (
                      SELECT count(*)
                      FROM livrable_sous_activite l
                      WHERE l.id_sous_activite = sa.id_sous_activite
                  ) = ?
                """, Integer.class, idService, nombre);
    }

    private boolean sousActiviteExiste(Integer id) {
        Integer compte = jdbc.queryForObject(
                "SELECT count(*) FROM sous_activite WHERE id_sous_activite = ?",
                Integer.class, id);

        return compte != null && compte > 0;
    }

}
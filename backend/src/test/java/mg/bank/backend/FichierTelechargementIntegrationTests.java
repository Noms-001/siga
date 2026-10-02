package mg.bank.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests du telechargement d un fichier.
 *
 * Les fichiers de data.sql pointent vers /uploads, qui n existe pas sur la
 * machine de developpement : le telechargement ne peut donc pas etre exerce
 * sur la donnee existante. Chaque test cree donc son fichier dans un
 * repertoire temporaire (TempDir) et insere une ligne fichier_sous_activite
 * qui pointe dessus. La transaction du test est annulee a la fin : aucun
 * residu en base.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FichierTelechargementIntegrationTests {

    /** Rattache au service 1 : ne voit que le service 1. */
    private static final String UTILISATEUR_SERVICE_UN =
            "marie.razafindrakoto@bfm-demo.local";

    /** Rattache au service 2 : ne voit que le service 2. */
    private static final String UTILISATEUR_SERVICE_DEUX =
            "tiana.randrianarison@bfm-demo.local";

    @TempDir
    Path repertoireTemporaire;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    // ------------------------------------------------------------------
    // Acces et perimetre
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Un fichier du perimetre est telecharge avec son contenu et son nom")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void fichierAccessible() throws Exception {

        byte[] contenu = { 1, 2, 3, 4, 5 };
        Path fichier = copier("rapport test.pdf", contenu);
        Integer sousActivite = sousActiviteAvecLivrable(2);
        Integer idFichier = insererFichier(
                sousActivite, fichier, "rapport test.pdf", "application/pdf");

        mockMvc.perform(get(
                        "/api/activites/{idActivite}/sous-activites/{idSousActivite}/fichiers/{idFichier}",
                        activiteDe(sousActivite), sousActivite, idFichier))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(contenu))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString(
                                "filename*=UTF-8''rapport%20test.pdf")));
    }

    @Test
    @DisplayName("Un fichier d une activite hors perimetre est refuse (403)")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void fichierHorsPerimetreRefuse() throws Exception {

        Integer sousActivite = sousActiviteAvecLivrable(2);
        Integer idFichier = insererFichier(
                sousActivite, copier("secret.pdf", new byte[] { 9 }),
                "secret.pdf", "application/pdf");

        mockMvc.perform(get(
                        "/api/activites/{idActivite}/sous-activites/{idSousActivite}/fichiers/{idFichier}",
                        activiteDe(sousActivite), sousActivite, idFichier))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType("application/json"));
    }

    // ------------------------------------------------------------------
    // Appartenance et existence
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Un fichier inexistant renvoie 404")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void fichierInexistant() throws Exception {

        Integer sousActivite = sousActiviteAvecLivrable(2);
        Integer inexistant = (int) jdbc.queryForObject(
                "SELECT COALESCE(MAX(id_fichier_sous_activite), 0) + 100000 FROM fichier_sous_activite",
                Integer.class);

        mockMvc.perform(get(
                        "/api/activites/{idActivite}/sous-activites/{idSousActivite}/fichiers/{idFichier}",
                        activiteDe(sousActivite), sousActivite, inexistant))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un fichier d une autre sous-activite renvoie 404")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void fichierDAutreSousActivite() throws Exception {

        Integer sousActiviteUnee = sousActiviteAvecLivrable(2);
        Integer sousActiviteAutre = sousActiviteAvecLivrableAutre(2, sousActiviteUnee);

        Integer idFichier = insererFichier(
                sousActiviteAutre, copier("autre.pdf", new byte[] { 7 }),
                "autre.pdf", "application/pdf");

        mockMvc.perform(get(
                        "/api/activites/{idActivite}/sous-activites/{idSousActivite}/fichiers/{idFichier}",
                        activiteDe(sousActiviteUnee), sousActiviteUnee, idFichier))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un fichier d une activite inexistante renvoie 404")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void activiteInexistante() throws Exception {

        Integer sousActivite = sousActiviteAvecLivrable(2);
        Integer idFichier = insererFichier(
                sousActivite, copier("orphelin.pdf", new byte[] { 3 }),
                "orphelin.pdf", "application/pdf");
        Integer inexistant = (int) jdbc.queryForObject(
                "SELECT COALESCE(MAX(id_activite), 0) + 100000 FROM activite",
                Integer.class);

        mockMvc.perform(get(
                        "/api/activites/{idActivite}/sous-activites/{idSousActivite}/fichiers/{idFichier}",
                        inexistant, sousActivite, idFichier))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un fichier reference en base mais absent du disque renvoie 404")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void fichierAbsentDuDisque() throws Exception {

        Integer sousActivite = sousActiviteAvecLivrable(2);
        Path manquant = repertoireTemporaire.resolve("disparu.pdf");
        Integer idFichier = insererFichier(
                sousActivite, manquant, "disparu.pdf", "application/pdf");

        mockMvc.perform(get(
                        "/api/activites/{idActivite}/sous-activites/{idSousActivite}/fichiers/{idFichier}",
                        activiteDe(sousActivite), sousActivite, idFichier))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // Validation et securite
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Sans authentification, l acces est refuse par la couche securite")
    void accesAnonymeRefuse() throws Exception {

        mockMvc.perform(get(
                        "/api/activites/{idActivite}/sous-activites/{idSousActivite}/fichiers/{idFichier}",
                        1, 1, 1))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un identifiant nul ou negatif est refuse par la validation")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void identifiantInvalide() throws Exception {

        Integer sousActivite = sousActiviteAvecLivrable(2);
        Integer activite = activiteDe(sousActivite);

        mockMvc.perform(get(
                        "/api/activites/{idActivite}/sous-activites/{idSousActivite}/fichiers/{idFichier}",
                        activite, sousActivite, 0))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    private Path copier(String nom, byte[] contenu) throws Exception {
        Path fichier = repertoireTemporaire.resolve(nom);
        Files.write(fichier, contenu);
        return fichier;
    }

    private Integer insererFichier(
            Integer idSousActivite,
            Path fichier,
            String nomOriginal,
            String typeMime) throws Exception {

        Integer idLivrable = jdbc.queryForObject(
                "SELECT min(id_livrable_sous_activite)"
                        + " FROM livrable_sous_activite WHERE id_sous_activite = ?",
                Integer.class, idSousActivite);

        assertThat(idLivrable).as(
                "Aucun livrable pour la sous-activite %d : le jeu de donnees a change",
                idSousActivite).isNotNull();

        Integer idUtilisateur = jdbc.queryForObject(
                "SELECT min(id_utilisateur) FROM utilisateur",
                Integer.class);

        String extension = nomOriginal.substring(nomOriginal.lastIndexOf('.') + 1);

        return jdbc.queryForObject("""
                INSERT INTO fichier_sous_activite
                    (nom_fichier, nom_original, chemin_fichier, version,
                     extension, type_mime, taille, date_depot,
                     id_utilisateur, id_livrable_sous_activite)
                VALUES (?, ?, ?, 1, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?)
                RETURNING id_fichier_sous_activite
                """, Integer.class,
                fichier.getFileName().toString(),
                nomOriginal,
                fichier.toString(),
                extension,
                typeMime,
                Files.isRegularFile(fichier) ? Files.size(fichier) : null,
                idUtilisateur,
                idLivrable);
    }

    private Integer activiteDe(Integer idSousActivite) {
        return jdbc.queryForObject(
                "SELECT id_activite FROM sous_activite WHERE id_sous_activite = ?",
                Integer.class, idSousActivite);
    }

    private Integer sousActiviteAvecLivrable(int idService) {
        Integer id = jdbc.queryForObject("""
                SELECT min(sa.id_sous_activite)
                FROM sous_activite sa
                JOIN activite a
                      ON a.id_activite = sa.id_activite
                WHERE a.id_service = ?
                  AND EXISTS (
                      SELECT 1 FROM livrable_sous_activite l
                      WHERE l.id_sous_activite = sa.id_sous_activite)
                """, Integer.class, idService);

        assertThat(id).as(
                "Aucune sous-activite avec livrable dans le service %d",
                idService).isNotNull();

        return id;
    }

    private Integer sousActiviteAvecLivrableAutre(int idService, Integer aExclure) {
        Integer id = jdbc.queryForObject("""
                SELECT min(sa.id_sous_activite)
                FROM sous_activite sa
                JOIN activite a
                      ON a.id_activite = sa.id_activite
                WHERE a.id_service = ?
                  AND sa.id_sous_activite <> ?
                  AND EXISTS (
                      SELECT 1 FROM livrable_sous_activite l
                      WHERE l.id_sous_activite = sa.id_sous_activite)
                """, Integer.class, idService, aExclure);

        assertThat(id).as(
                "Aucune seconde sous-activite avec livrable dans le service %d",
                idService).isNotNull();

        return id;
    }

}
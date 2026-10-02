package mg.bank.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests de creation et de modification d une activite.
 *
 * Mêmes precautions que pour la soumission.
 *
 * AUCUN IDENTIFIANT FIGE
 * Les referentiels (priorite, type, site, objectif, service, role) sont
 * relus en base a chaque test. Une evolution de data.sql ne doit pas
 * transformer ces tests en echecs arbitraires, et surtout un test qui passe
 * encore doit avoir exerce la regle qu il pretend verifier.
 *
 * LE JEU DE DONNEES NE CONTIENT AUCUN BROUILLON
 * Chaque etat de depart est donc fabrique par le test lui-meme, en passant
 * par le point d entree reel : ce que teste une modification est alors
 * exactement ce que l application produit, historique compris.
 *
 * AUCUN CONTROLE DE PERMISSION, LE PERIMETRE SEUL
 * data.sql n'accorde "Creer une activite" ni au chef de departement, ni aux
 * techniciens. Aucun chemin de code ne lit ces droits : ces deux profils
 * ecrivent donc, et c'est le comportement attendu. Le perimetre, lui, reste
 * verifie, et ses refus sont testes comme tels.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActiviteEcritureIntegrationTests {

    /** Service 1, poste de chef de service : peut creer et modifier. */
    private static final String UTILISATEUR_SERVICE_UN =
            "leila.mansouri@bfm.tn";

    /** Service 2, memes droits mais perimetre disjoint. */
    private static final String UTILISATEUR_SERVICE_DEUX =
            "hatem.trabelsi@bfm.tn";

    /** Administrateur, niveau departement. */
    private static final String ADMINISTRATEUR =
            "tarek.baccouche@bfm.tn";

    /** Chef de departement : aucun droit d'ecriture dans data.sql. */
    private static final String UTILISATEUR_SANS_ECRITURE_DEPARTEMENT =
            "karim.bensalah@bfm.tn";

    /** Technicien : consultation seule dans data.sql. */
    private static final String UTILISATEUR_SANS_ECRITURE =
            "anis.mejri@bfm.tn";

    private static final String BASE = "/api/activites";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    // ------------------------------------------------------------------
    // Creation : cas nominaux
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Creer une activite PTA la dote d un objectif et d un statut brouillon")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void creationPta() throws Exception {

        mockMvc.perform(creer(avec(corpsActifite(),
                        "pta", true,
                        "idObjectifSpecifique", idObjectifSpecifique())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.idActivite").isNumber())
                .andExpect(jsonPath("$.data.code").isNotEmpty())
                .andExpect(jsonPath("$.data.statut").value("BROUILLON"))
                .andExpect(jsonPath("$.data.nombreSousActivites").value(0))
                .andExpect(jsonPath("$.error").doesNotExist());

        Integer id = derniereActivite();

        assertThat(idServiceDe(id)).isEqualTo(serviceDe(UTILISATEUR_SERVICE_UN));

        assertThat(statutCourant(id))
                .as("La creation doit poser la premiere entree d historique")
                .isEqualTo("BROUILLON");
    }

    @Test
    @DisplayName("Creer une activite NON PTA ne l attache a aucun objectif")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void creationNonPta() throws Exception {

        mockMvc.perform(creer(avec(corpsActifite(), "pta", false)))
                .andExpect(status().isCreated());

        // NULL = activite non PTA : c'est la definition retenue par le
        // modele, activite n'ayant pas de colonne statut ni de booleen PTA.
        assertThat(objectifDe(derniereActivite())).isNull();
    }

    @Test
    @DisplayName("Creer une activite avec trois sous-activites les enregistre toutes")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void creationAvecSousActivites() throws Exception {

        mockMvc.perform(creer(avec(corpsActifite(), "sousActivites", List.of(
                                sousActivite("Reprise du dossier", 0, 5)), sousActivite("Controle qualite", 6, 12), sousActivite("Cloture et archivage", 13, 20))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nombreSousActivites").value(3));

        Integer id = derniereActivite();

        assertThat(codesSousActivites(id)).hasSize(3);

        // Chaque sous-activite est rattachee a sa mere : c'est ce qui
        // permet au detail de les retrouver sans porteuse d identifiant.
        assertThat(activiteMereDe(codesSousActivites(id).stream()
                .map(this::idSousActiviteParCode)
                .findFirst()
                .orElseThrow())).isEqualTo(id);
    }

    @Test
    @DisplayName("Le code d une sous-activite est genere quand le formulaire n en fournit pas")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void codeSousActiviteGenere() throws Exception {

        Map<String, Object> sansCode = new LinkedHashMap<>();
        sansCode.put("designation", "Etude preliminaire");
        sansCode.put("dateDebutPrevue", "2026-02-01");
        sansCode.put("dateFinPrevue", "2026-02-10");

        mockMvc.perform(creer(avec(corpsActifite(), "sousActivites", List.of(sansCode))))
                .andExpect(status().isCreated());

        Integer id = derniereActivite();

        // Format de data.sql : SA-<annee>-<rang de l'activite>-<sequence>,
        // les deux derniers nombres etant sur au moins deux chiffres. Le rang
        // est GLOBAL et non remis a zero par objectif, sans quoi deux
        // activites d'objectifs differents porteraient le meme code.
        assertThat(codesSousActivites(id))
                .containsExactly("SA-2026-" + rangActivite(id) + "-01");
    }

    @Test
    @DisplayName("Deux sous-activites sans code recoivent deux sequences distinctes")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void codesSousActivitesGeneresSeSuivent() throws Exception {

        Map<String, Object> premiere = new LinkedHashMap<>();
        premiere.put("designation", "Etude preliminaire");
        premiere.put("dateDebutPrevue", "2026-02-01");
        premiere.put("dateFinPrevue", "2026-02-10");

        Map<String, Object> seconde = new LinkedHashMap<>();
        seconde.put("designation", "Realisation");
        seconde.put("dateDebutPrevue", "2026-02-11");
        seconde.put("dateFinPrevue", "2026-02-20");

        mockMvc.perform(creer(avec(corpsActifite(),
                        "sousActivites", List.of(premiere, seconde))))
                .andExpect(status().isCreated());

        Integer id = derniereActivite();

        // Le zero initial est ce qui distingue ce format de l'ancien
        // SA-<id>-1 : sans lui, "01" et "1" seraient le meme code pour le
        // lecteur, et le format ne correspondrait plus a celui de data.sql.
        assertThat(codesSousActivites(id))
                .containsExactly(
                        "SA-2026-" + rangActivite(id) + "-01",
                        "SA-2026-" + rangActivite(id) + "-02");
    }

    // ------------------------------------------------------------------
    // Creation : regles de coherence
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Une activite PTA sans objectif est refusee : 400")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void ptaSansObjectifRefusee() throws Exception {

        // pta true explicite : c'est lui qui rend l'objectif obligatoire,
        // pas la presence d'un champ vide.
        mockMvc.perform(creer(avec(corpsActifite(),
                        "pta", true,
                        "idObjectifSpecifique", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    @DisplayName("Une activite non PTA rattachee a un objectif est refusee : 400")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void nonPtaAvecObjectifRefusee() throws Exception {

        mockMvc.perform(creer(avec(corpsActifite(),
                        "pta", false,
                        "idObjectifSpecifique", idObjectifSpecifique())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Une activite PTA sans reference est refusee : 400")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void ptaSansReferenceRefusee() throws Exception {

        // Une reference nulle est desormais acceptee en base, puisque les
        // NPTA n en ont pas. Ce test verifie que la regle n'a pas disparu
        // avec le @NotBlank : une PTA sans reference perdrait le seul lien
        // exploitable vers son objectif, et le statut 201 ferait croire
        // l'inverse.
        mockMvc.perform(creer(avec(corpsActifite(),
                        "pta", true,
                        "idObjectifSpecifique", idObjectifSpecifique(),
                        "reference", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    @DisplayName("Une activite non PTA est enregistree sans reference")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void nonPtaEnregistreeSansReference() throws Exception {

        // Le corps par defaut porte une reference, et le formulaire en
        // affiche une. Elle n'a pourtant rien a designer : la ligne doit
        // etre enregistree sans, et non refusee. Un refus ici transforme un
        // detail de saisie en erreur pour un corps par ailleurs valide.
        mockMvc.perform(creer(corpsActifite()))
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject(
                "SELECT reference FROM activite WHERE id_activite = ?",
                String.class, derniereActivite())).isNull();
    }

    @Test
    @DisplayName("Passer une activite PTA en non PTA efface sa reference")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void passagePtaVersNonPtaEffaceLaReference() throws Exception {

        mockMvc.perform(creer(avec(corpsActifite(),
                        "pta", true,
                        "idObjectifSpecifique", idObjectifSpecifique())))
                .andExpect(status().isCreated());

        Integer id = derniereActivite();

        assertThat(jdbc.queryForObject(
                "SELECT reference FROM activite WHERE id_activite = ?",
                String.class, id)).isNotNull();

        // Le retrait de l'objectif et celui de la reference sont le meme
        // changement vu de deux tables. Ne pas les traiter ensemble
        // laisserait une activite sans objectif portant BCT/1_1.
        mockMvc.perform(modifier(id, avec(corpsActifite(),
                        "pta", false,
                        "idObjectifSpecifique", null)))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject(
                "SELECT reference FROM activite WHERE id_activite = ?",
                String.class, id)).isNull();
    }

    @Test
    @DisplayName("Une fin anterieure au debut est refusee : 400")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void datesIncoherentesRefusees() throws Exception {

        // Meme regle que chk_activite_dates_prevues, mais rendue lisible :
        // sans cela la violation remonterait en 500 avec un message SQL.
        mockMvc.perform(creer(avec(corpsActifite(), "dateFinPrevue", "2026-05-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    @DisplayName("Un code d activite deja pris est refuse : 400")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void codeActiviteDupliqueRefuse() throws Exception {

        // UNIQUE(code) est global : sans cette verification, la violation
        // remonterait en 500 au lieu d'etre expliquee.
        String codePris = jdbc.queryForObject(
                "SELECT code FROM activite ORDER BY id_activite LIMIT 1",
                String.class);

        mockMvc.perform(creer(avec(corpsActifite(), "code", codePris)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Perimetre
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Aucun controle de permission : un utilisateur de service ecrit")
    @WithMockUser(username = UTILISATEUR_SANS_ECRITURE)
    void creationSansControleDePermission() throws Exception {

        // data.sql ne donne a cet utilisateur que des droits de
        // consultation. Aucun chemin de code ne les lit : la seule porte
        // reste le perimetre, qu'il respecte. Ce test verrouille ce
        // comportement pour qu'il ne change pas par megarde.
        mockMvc.perform(creer(avec(corpsActifite(), "pta", false)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.statut").value("BROUILLON"));
    }

    @Test
    @DisplayName("Un chef de departement ecrit des lors qu'il respecte le perimetre")
    @WithMockUser(username = UTILISATEUR_SANS_ECRITURE_DEPARTEMENT)
    void creationChefDepartementAcceptee() throws Exception {

        // Symetrique du test precedent, au niveau superieur : avoir le
        // perimetre le plus large ne fait pas non plus obstacle a l'ecriture.
        mockMvc.perform(creer(avec(corpsActifite(), "pta", false)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.statut").value("BROUILLON"));
    }

    @Test
    @DisplayName("Un utilisateur de service ecrit dans son service, meme en en envoyant un autre")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void serviceImposeAuDoublantDePerimetre() throws Exception {

        mockMvc.perform(creer(avec(corpsActifite(), "idService", serviceDe(UTILISATEUR_SERVICE_DEUX))))
                .andExpect(status().isCreated());

        // Le corps ne peut pas elargir le perimetre : c'est le statut
        // d'utilisateur authentifie qui tranche, jamais la valeur envoyee.
        assertThat(idServiceDe(derniereActivite()))
                .isEqualTo(serviceDe(UTILISATEUR_SERVICE_UN));
    }

    @Test
    @DisplayName("Un administrateur de niveau departement choisit parmi les services du departement")
    @WithMockUser(username = ADMINISTRATEUR)
    void niveauDepartementChoisitSonService() throws Exception {

        mockMvc.perform(creer(avec(corpsActifite(), "idService", serviceDe(UTILISATEUR_SERVICE_UN))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.idActivite").isNumber());
    }

    @Test
    @DisplayName("Un administrateur sans service choisi est refuse : 400")
    @WithMockUser(username = ADMINISTRATEUR)
    void niveauDepartementServiceObligatoire() throws Exception {

        // activite.id_service est NOT NULL : un utilisateur de niveau
        // departement doit designer un service, il n'en a pas d'implicite.
        mockMvc.perform(creer(avec(corpsActifite(), "idService", null)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Modification
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Modifier un brouillon met a jour ses champs sans changer son statut")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void modificationBrouillon() throws Exception {

        Integer id = creerBrouillon();

        String nouveauCode = "MOD" + Math.abs(System.nanoTime() % 1000000);

        mockMvc.perform(modifier(id, avec(corpsActifite(),
                        "pta", true,
                        "idObjectifSpecifique", idObjectifSpecifique(),
                        "code", nouveauCode,
                        "designation", "Designation revisee")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(nouveauCode))
                .andExpect(jsonPath("$.data.designation").value("Designation revisee"))
                .andExpect(jsonPath("$.data.statut").value("BROUILLON"));

        assertThat(designationDe(id)).isEqualTo("Designation revisee");

        // Une redaction n'est pas une transition : aucune entree d'historique
        // supplementaire ne doit apparaitre.
        assertThat(statutCourant(id)).isEqualTo("BROUILLON");
    }

    @Test
    @DisplayName("Modifier une activite publiee au suivi est refuse : 409")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void modificationActivitePasBrouillon() throws Exception {

        Integer id = activiteDuService(serviceDe(UTILISATEUR_SERVICE_UN));

        assertThat(statutCourant(id))
                .as("Le jeu de donnees ne contient que des activites publiees")
                .isNotEqualTo("BROUILLON");

        mockMvc.perform(modifier(id, avec(corpsActifite(), "pta", false)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Modifier une activite d un autre service est refuse : 403")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void modificationHorsPerimetre() throws Exception {

        Integer id = creerBrouillon();

        mockMvc.perform(modifier(id, avec(corpsActifite(), "pta", false)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Modifier une activite inexistante renvoie 404")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void modificationActiviteInexistante() throws Exception {

        Integer inconnu = jdbc.queryForObject(
                "SELECT max(id_activite) + 1000 FROM activite", Integer.class);

        mockMvc.perform(modifier(inconnu, avec(corpsActifite(), "pta", false)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un utilisateur sans droit de modification modifie malgre tout")
    @WithMockUser(username = UTILISATEUR_SANS_ECRITURE)
    void modificationSansControleDePermission() throws Exception {

        Integer id = creerBrouillon();

        mockMvc.perform(modifier(id, avec(corpsActifite(), "pta", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statut").value("BROUILLON"));
    }

    // ------------------------------------------------------------------
    // Sous-activites : ce qu une modification ne doit pas faire
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Ajouter une sous-activite conserve les identifiants des precedentes")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void ajoutSousActiviteConserveLesPrecedentes() throws Exception {

        Integer id = creerBrouillon(List.of(
                sousActivite("Premiere", 0, 5),
                sousActivite("Deuxieme", 6, 10)));

        List<Integer> avant = idsSousActivites(id);
        assertThat(avant).hasSize(2);

        List<Map<String, Object>> avecAjout = new ArrayList<>();
        avecAjout.add(sousActiviteExistante(avant.get(0), "Premiere renommee", 0, 5));
        avecAjout.add(sousActiviteExistante(avant.get(1), "Deuxieme", 6, 10));
        avecAjout.add(sousActivite("Troisieme", 11, 15));

        mockMvc.perform(modifier(id, avec(corpsActifite(), "sousActivites", avecAjout)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombreSousActivites").value(3));

        // Une recreation "supprimer puis inserer" aurait reinitialise ces
        // identifiants, et avec eux les affectations et les avancements qui
        // y sont rattaches.
        assertThat(idsSousActivites(id))
                .as("Les identifiants existants doivent survivre a l ajout")
                .containsAll(avant)
                .hasSize(3);

        assertThat(designationSousActivite(avant.get(0)))
                .as("La ligne existante doit avoir ete modifiee, pas recreee")
                .isEqualTo("Premiere renommee");
    }

    @Test
    @DisplayName("Retirer une sous-activite du formulaire la supprime")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void suppressionSousActivite() throws Exception {

        Integer id = creerBrouillon(List.of(
                sousActivite("Conservee", 0, 5),
                sousActivite("Retiree", 6, 10)));

        List<Integer> avant = idsSousActivites(id);
        assertThat(avant).hasSize(2);

        mockMvc.perform(modifier(id, avec(corpsActifite(), "sousActivites", List.of(
                                sousActiviteExistante(avant.get(0), "Conservee", 0, 5)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombreSousActivites").value(1));

        assertThat(idsSousActivites(id)).containsExactly(avant.get(0));
    }

    @Test
    @DisplayName("Une sous-activite qui porte une affectation ne peut pas etre supprimee : 409")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void suppressionSousActiviteLieeRefusee() throws Exception {

        Integer id = creerBrouillon(List.of(sousActivite("Affectee", 0, 5)));

        Integer idSousActivite = idsSousActivites(id).get(0);

        jdbc.update("""
                INSERT INTO affectation_sous_activite
                    (date_affectation, date_desaffectation, id_role,
                     id_sous_activite, id_utilisateur)
                SELECT now(), NULL,
                       (SELECT min(id_role) FROM role),
                       ?,
                       (SELECT id_utilisateur FROM utilisateur WHERE email = ?)
                """, idSousActivite, UTILISATEUR_SERVICE_UN);

        mockMvc.perform(modifier(id, avec(corpsActifite(), "sousActivites", List.of())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").isNotEmpty());

        // La ligne est la, avec son affectation : la supprimer en cascades
        // detruirait une donnee de suivi que personne n'a demandee.
        assertThat(idsSousActivites(id)).containsExactly(idSousActivite);
    }

    @Test
    @DisplayName("Le numero d une sous-activite d une autre activite est refuse : 400")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void sousActiviteDUneAutreActiviteRefusee() throws Exception {

        Integer premiereActivite = creerBrouillon();
        Integer secondeActivite = creerBrouillon();

        Integer sousActiviteEtrangere = idsSousActivites(premiereActivite).get(0);

        mockMvc.perform(modifier(secondeActivite, avec(corpsActifite(), "sousActivites", List.of(
                                sousActiviteExistante(
                                        sousActiviteEtrangere, "Deplacee", 0, 5)))))
                .andExpect(status().isBadRequest());

        // Sans ce controle, un client pourrait deplacer une sous-activite
        // d'une activite a l'autre en envoyant son identifiant.
        assertThat(activiteMereDe(sousActiviteEtrangere))
                .isEqualTo(premiereActivite);
    }

    @Test
    @DisplayName("Une modification ne peut pas effacer les dates reelles d une sous-activite")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void modificationNeReecritPasLesDatesReelles() throws Exception {

        Integer id = creerBrouillon(List.of(sousActivite("Executee", 0, 5)));

        Integer idSousActivite = idsSousActivites(id).get(0);

        jdbc.update("""
                UPDATE sous_activite
                SET date_debut_reelle = DATE '2026-03-02',
                    date_fin_reelle   = DATE '2026-03-04'
                WHERE id_sous_activite = ?
                """, idSousActivite);

        mockMvc.perform(modifier(id, avec(corpsActifite(), "sousActivites", List.of(
                                sousActiviteExistante(
                                        idSousActivite, "Executee renommee", 0, 5)))))
                .andExpect(status().isOk());

        // Les dates reelles appartiennent au suivi : une redaction ne doit
        // pas ecraser une execution.
        assertThat(dateFinReelleDe(idSousActivite))
                .isEqualTo(LocalDate.of(2026, 3, 4));
    }

    // ------------------------------------------------------------------
    // Codes de sous-activite : uniques dans toute la base
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Un code de sous-activite deja porte par une voisine est refuse : 400")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void codeSousActivitePrisParUneVoisineRefuse() throws Exception {

        String codePremier = "SA-COLL-" + Math.abs(System.nanoTime() % 100000);
        String codeSecond = "SA-COLL-" + Math.abs(System.nanoTime() % 100000);

        Integer id = creerBrouillon(List.of(
                sousActivite(codePremier, "Premiere", 0, 5),
                sousActivite(codeSecond, "Seconde", 6, 10)));

        List<Integer> avant = idsSousActivites(id);

        mockMvc.perform(modifier(id, avec(corpsActifite(), "sousActivites", List.of(
                        sousActiviteExistante(avant.get(0), codeSecond, "Premiere renommee", 0, 5),
                        sousActiviteExistante(avant.get(1), codeSecond, "Seconde", 6, 10)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());

        // Les deux lignes gardent leur code : prendre celui d'une voisine
        // doit etre refuse comme une saisie, et non se decouvrir au flush
        // pour remonter en 500 sur une violation d'unicite.
        assertThat(codesSousActivites(id))
                .containsExactly(codePremier, codeSecond);
    }

    @Test
    @DisplayName("Renvoyer le code deja porte par sa propre ligne ne pose pas conflit")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void codeSousActiviteInchangeRenvoyeAccepte() throws Exception {

        Integer id = creerBrouillon(List.of(
                sousActivite("Premiere", 0, 5),
                sousActivite("Seconde", 6, 10)));

        List<Integer> avant = idsSousActivites(id);
        List<String> codes = codesSousActivites(id);

        // Le formulaire renvoie l'etat complet, donc chaque ligne revient
        // avec son code. Confondre "mon code" et "le code d'une autre"
        // rendrait impossible toute modification d'une activite.
        mockMvc.perform(modifier(id, avec(corpsActifite(), "sousActivites", List.of(
                        sousActiviteExistante(avant.get(0), codes.get(0), "Premiere", 0, 5),
                        sousActiviteExistante(avant.get(1), codes.get(1), "Seconde", 6, 10)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombreSousActivites").value(2));

        assertThat(codesSousActivites(id)).isEqualTo(codes);
    }

    @Test
    @DisplayName("La meme sous-activite deux fois dans le formulaire est refusee : 400")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void sousActiviteDupliqueeRefusee() throws Exception {

        Integer id = creerBrouillon(List.of(sousActivite("Premiere", 0, 5)));

        Integer idSousActivite = idsSousActivites(id).get(0);

        mockMvc.perform(modifier(id, avec(corpsActifite(), "sousActivites", List.of(
                        sousActiviteExistante(idSousActivite, "Premiere", 0, 5),
                        sousActiviteExistante(idSousActivite, "Premiere encore", 0, 5)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    // ------------------------------------------------------------------
    // Modification des sous-activites
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Rediger une sous-activite conserve son identifiant")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void redactionSousActiviteConserveSonIdentifiant() throws Exception {

        Integer id = creerBrouillon(List.of(sousActivite("Premiere", 0, 5)));

        List<Integer> avant = idsSousActivites(id);

        // Ce qui compte n'est pas un droit mais l'identite de la ligne :
        // une redaction ne doit pas passer par un DELETE suivi d'un INSERT,
        // qui reinitialiserait l'identifiant et avec lui les affectations,
        // livrables et avancements qui y sont rattaches.
        mockMvc.perform(modifier(id, avec(corpsActifite(), "sousActivites", List.of(
                        sousActiviteExistante(
                                avant.get(0), "Premiere renommee", 0, 5)))))
                .andExpect(status().isOk());

        assertThat(idsSousActivites(id)).containsExactly(avant.get(0));

        assertThat(designationSousActivite(avant.get(0)))
                .isEqualTo("Premiere renommee");
    }

    @Test
    @DisplayName("Renvoyer ses sous-activites inchangees laisse les lignes intactes")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void sousActivitesInchangeesLaissentLesLignesIntactes() throws Exception {

        Integer id = creerBrouillon(List.of(sousActivite("Premiere", 0, 5)));

        List<Integer> avant = idsSousActivites(id);
        List<String> codes = codesSousActivites(id);

        // Le formulaire renvoie l'etat complet : une ligne renvoyee a
        // l'identique ne doit etre ni supprimee puis recreee, ni hissée au
        // statut de ligne ajoutee.
        mockMvc.perform(modifier(id, avec(corpsActifite(),
                        "designation", "Activite renommee",
                        "sousActivites", List.of(sousActiviteExistante(
                                avant.get(0), codes.get(0), "Premiere", 0, 5)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.designation").value("Activite renommee"));

        assertThat(idsSousActivites(id)).containsExactly(avant.get(0));
    }

    // ------------------------------------------------------------------
    // Pre-remplissage du formulaire
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Le formulaire renvoie les champs editables et le statut courant")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void chargementFormulaire() throws Exception {

        // PTA et non le corps par defaut, qui est NON PTA : la reference est
        // ce qui rattache une activite a son objectif, et elle n existe que
        // pour un PTA. La vérifier sur une NON PTA ne testerait rien, et pire,
        // obligerait a conserver une reference sans objet.
        mockMvc.perform(creer(avec(corpsActifite(),
                        "pta", true,
                        "idObjectifSpecifique", idObjectifSpecifique(),
                        "sousActivites", List.of(sousActivite("Premiere", 0, 5)))))
                .andExpect(status().isCreated());

        Integer id = derniereActivite();

        mockMvc.perform(get(BASE + "/{id}/formulaire", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idActivite").value(id))
                .andExpect(jsonPath("$.data.code").isNotEmpty())
                .andExpect(jsonPath("$.data.reference").isNotEmpty())
                .andExpect(jsonPath("$.data.designation").isNotEmpty())
                .andExpect(jsonPath("$.data.idPriorite").isNumber())
                .andExpect(jsonPath("$.data.idService").isNumber())
                .andExpect(jsonPath("$.data.statut").value("BROUILLON"))
                .andExpect(jsonPath("$.data.sousActivites").isArray())
                .andExpect(jsonPath("$.data.sousActivites[0].idSousActivite").isNumber())
                .andExpect(jsonPath("$.data.sousActivites[0].code").isNotEmpty());
    }

    @Test
    @DisplayName("Le formulaire rend l objectif en clair, et non un seul identifiant")
    @WithMockUser(username = UTILISATEUR_SERVICE_UN)
    void chargementFormulaireExposeLeLibelleObjectif() throws Exception {

        mockMvc.perform(creer(avec(corpsActifite(),
                        "pta", true,
                        "idObjectifSpecifique", idObjectifSpecifique())))
                .andExpect(status().isCreated());

        Integer id = derniereActivite();

        // Sans ces libelles, le formulaire ne peut afficher qu'un nombre :
        // il devrait alors refaire une recherche par identifiant, ce qui est
        // une fausse recherche et raterait des que le libelle change.
        mockMvc.perform(get(BASE + "/{id}/formulaire", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idObjectifSpecifique").value(idObjectifSpecifique()))
                .andExpect(jsonPath("$.data.objectifCode").isNotEmpty())
                .andExpect(jsonPath("$.data.objectifDesignation").isNotEmpty())
                .andExpect(jsonPath("$.data.objectifAnnee").isNumber());
    }

    @Test
    @DisplayName("Charger le formulaire d une activite hors perimetre est refuse : 403")
    @WithMockUser(username = UTILISATEUR_SERVICE_DEUX)
    void chargementFormulaireHorsPerimetre() throws Exception {

        Integer id = creerBrouillon();

        mockMvc.perform(get(BASE + "/{id}/formulaire", id))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Charger le formulaire est ouvert : aucun controle de permission")
    @WithMockUser(username = UTILISATEUR_SANS_ECRITURE)
    void chargementFormulaireSansControleDePermission() throws Exception {

        Integer id = creerBrouillon();

        mockMvc.perform(get(BASE + "/{id}/formulaire", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idActivite").value(id));
    }

    // ------------------------------------------------------------------
    // Requetes
    // ------------------------------------------------------------------

    /**
     * Le corps est serialise ici, et non dans une chaine de .put() :
     * Map.put renvoie la valeur, pas la map, et serialiser avant que le
     * test n ait surcharge son corps enverrait toujours le corps par
     * defaut.
     */
    private MockHttpServletRequestBuilder creer(Map<String, Object> corps)
            throws Exception {

        return post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(corps));
    }

    private MockHttpServletRequestBuilder modifier(
            Integer idActivite,
            Map<String, Object> corps) throws Exception {

        return put(BASE + "/{id}", idActivite)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(corps));
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    /**
     * Corps valide par defaut. Chaque test ne surcharge que ce qu'il
     * verifie : un corps valide duplique dans chaque test deriverait du
     * premier et disparaitrait de la plupart.
     *
     * NON PTA par defaut, parce que c'est le seul choix qui n'exige rien
     * d'autre : un PTA sans objectif est refuse, donc un corps PTA par
     * defaut obligerait chaque test a fournir un objectif dont il ne se
     * soucie pas. Les tests PTA surchargent donc les deux champs.
     *
     * La reference reste presente dans le corps par defaut, et c'est
     * volontaire : elle ne dispense d'aucune surcharge, et sa presence ici
     * permet aux tests NON PTA de verifier qu'elle est bien ecartee a
     * l'enregistrement.
     */
    private Map<String, Object> corpsActifite() {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("code", "T" + Math.abs(System.nanoTime() % 1000000));
        corps.put("reference", "R" + Math.abs(System.nanoTime() % 1000000));
        corps.put("designation", "Activite de test");
        corps.put("dateDebutPrevue", "2026-01-05");
        corps.put("dateFinPrevue", "2026-06-30");
        corps.put("pta", false);
        corps.put("idPriorite", prioriteActive());
        corps.put("idTypeActivite", typeActif());
        corps.put("idSite", siteActif());
        return corps;
    }

    /**
     * Complete un corps et le rend.
     *
     * Map.put renvoie V, donc Object : ecrire corps.put(...).put(...) ne
     * compile pas, et son resultat ne serait de toute facon pas un corps.
     * Ce helper rend la surcharge explicite et typée.
     */
    private Map<String, Object> avec(Map<String, Object> corps, Object... entrees) {
        for (int i = 0; i < entrees.length; i += 2) {
            corps.put((String) entrees[i], entrees[i + 1]);
        }
        return corps;
    }

    private Map<String, Object> sousActivite(String designation, int debut, int fin) {
        Map<String, Object> sousActivite = new LinkedHashMap<>();
        sousActivite.put("designation", designation);
        sousActivite.put("dateDebutPrevue", jour(debut));
        sousActivite.put("dateFinPrevue", jour(fin));
        return sousActivite;
    }

    /**
     * Ligne avec un code choisi, plutot que genere.
     *
     * Il faut pouvoir nommer les codes pour eprouver les collisions : deux
     * lignes generees ne se confondraient jamais.
     */
    private Map<String, Object> sousActivite(
            String code,
            String designation,
            int debut,
            int fin) {
        return avec(sousActivite(designation, debut, fin), "code", code);
    }

    private Map<String, Object> sousActiviteExistante(
            Integer idSousActivite,
            String designation,
            int debut,
            int fin) {

        return avec(sousActivite(designation, debut, fin),
                "idSousActivite", idSousActivite);
    }

    private Map<String, Object> sousActiviteExistante(
            Integer idSousActivite,
            String code,
            String designation,
            int debut,
            int fin) {

        return avec(sousActivite(code, designation, debut, fin),
                "idSousActivite", idSousActivite);
    }

        private String jour(int decalage) {
        return LocalDate.of(2026, 1, 5).plusDays(decalage).toString();
    }

    /**
     * Cree une activite par le point d'entree reel, et la renvoie.
     *
     * Passer par POST plutot que par un INSERT SQL garantit que l'etat de
     * depart des tests de modification est exactement celui que
     * l'application produit, historique compris.
     *
     * L'appel est fait avec un chef de service, et non avec l'utilisateur du
     * test : plusieurs tests verifient qu'un utilisateur ne peut pas modifier
     * une activite qu'il n'a pas creee, et leur @WithMockUser n'a justement
     * pas le droit de creer. Sans cet oubli, ces tests echoueraient sur leur
     * propre etat de depart, en 403 a la creation, et non sur la regle qu'ils
     * mesurent. C'est aussi la seule facon d'obtenir une activite dans un
     * perimetre donne, donc hors perimetre de l'appelant.
     */
    private Integer creerBrouillon() throws Exception {
        return creerBrouillon(List.of());
    }

    private Integer creerBrouillon(List<Map<String, Object>> sousActivites)
            throws Exception {

        long avant = nombreActivites();

        mockMvc.perform(post(BASE)
                        .with(user(UTILISATEUR_SERVICE_UN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(avec(corpsActifite(), "sousActivites", sousActivites))))
                .andExpect(status().isCreated());

        assertThat(nombreActivites())
                .as("La creation doit avoir ajoute une activite")
                .isEqualTo(avant + 1);

        return derniereActivite();
    }

    private String json(Object valeur) throws Exception {
        return objectMapper.writeValueAsString(valeur);
    }

    private Integer derniereActivite() {
        return jdbc.queryForObject(
                "SELECT max(id_activite) FROM activite", Integer.class);
    }

    /**
     * Rang global d'une activite, calcule independamment de la production.
     *
     * Le service utilise un COUNT JPQL sur l'entite ; le test repasse par du
     * SQL ecrit ici. Reprendre la methode de production n'aurait rien
     * verifie : les deux se tromperaient ensemble.
     */
    private int rangActivite(Integer idActivite) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM activite WHERE id_activite <= ?",
                Integer.class, idActivite);
    }

    private long nombreActivites() {
        Long nombre = jdbc.queryForObject(
                "SELECT count(*) FROM activite", Long.class);
        return nombre == null ? 0L : nombre;
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

    private List<Integer> idsSousActivites(Integer idActivite) {
        return jdbc.queryForList(
                "SELECT id_sous_activite FROM sous_activite"
                        + " WHERE id_activite = ? ORDER BY id_sous_activite",
                Integer.class, idActivite);
    }

    private List<String> codesSousActivites(Integer idActivite) {
        return jdbc.queryForList(
                "SELECT code FROM sous_activite WHERE id_activite = ?"
                        + " ORDER BY id_sous_activite",
                String.class, idActivite);
    }

    private Integer idSousActiviteParCode(String code) {
        return jdbc.queryForObject(
                "SELECT id_sous_activite FROM sous_activite WHERE code = ?",
                Integer.class, code);
    }

    private Integer activiteMereDe(Integer idSousActivite) {
        return jdbc.queryForObject(
                "SELECT id_activite FROM sous_activite WHERE id_sous_activite = ?",
                Integer.class, idSousActivite);
    }

    private String designationSousActivite(Integer idSousActivite) {
        return jdbc.queryForObject(
                "SELECT designation FROM sous_activite WHERE id_sous_activite = ?",
                String.class, idSousActivite);
    }

    private LocalDate dateFinReelleDe(Integer idSousActivite) {
        return jdbc.queryForObject(
                "SELECT date_fin_reelle FROM sous_activite WHERE id_sous_activite = ?",
                LocalDate.class, idSousActivite);
    }

    private Integer idServiceDe(Integer idActivite) {
        return jdbc.queryForObject(
                "SELECT id_service FROM activite WHERE id_activite = ?",
                Integer.class, idActivite);
    }

    private Integer objectifDe(Integer idActivite) {
        return jdbc.queryForObject(
                "SELECT id_objectif_specifique FROM activite WHERE id_activite = ?",
                Integer.class, idActivite);
    }

    private String designationDe(Integer idActivite) {
        return jdbc.queryForObject(
                "SELECT designation FROM activite WHERE id_activite = ?",
                String.class, idActivite);
    }

    private Integer activiteDuService(Integer idService) {
        Integer id = jdbc.queryForObject(
                "SELECT min(id_activite) FROM activite WHERE id_service = ?",
                Integer.class, idService);

        assertThat(id)
                .as("Aucune activite dans le service %d", idService)
                .isNotNull();

        return id;
    }

    private Integer serviceDe(String email) {
        return jdbc.queryForObject(
                "SELECT id_service FROM utilisateur WHERE email = ?",
                Integer.class, email);
    }

    private Integer idObjectifSpecifique() {
        Integer id = jdbc.queryForObject(
                "SELECT min(id_objectif_specifique) FROM objectif_specifique",
                Integer.class);

        assertThat(id).as("Le referentiel des objectifs est vide").isNotNull();

        return id;
    }

    private Integer prioriteActive() {
        return jdbc.queryForObject(
                "SELECT min(id_priorite) FROM priorite WHERE actif", Integer.class);
    }

    private Integer typeActif() {
        return jdbc.queryForObject(
                "SELECT min(id_type_activite) FROM type_activite WHERE actif",
                Integer.class);
    }

    private Integer siteActif() {
        return jdbc.queryForObject(
                "SELECT min(id_site) FROM site WHERE actif", Integer.class);
    }

}

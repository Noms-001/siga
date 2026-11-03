package mg.bank.backend;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import mg.bank.backend.enums.TypeImport;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.model.referentiel.ObjectifSpecifique;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.referentiel.ObjectifSpecifiqueRepository;
import mg.bank.backend.service.activite.CodeActiviteService;

@SpringBootTest
@Transactional
class CodeActiviteGenerationIntegrationTests {

    @Autowired
    private CodeActiviteService codeService;

    @Autowired
    private ActiviteRepository activiteRepository;

    @Autowired
    private ObjectifSpecifiqueRepository objectifRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deuxActivitesMemeObjectifOntUneSequenceDistincteParAnnee() {
        ObjectifSpecifique objectif = nouvelObjectif(numeroUnique());
                Integer idService = premierService();

        codeService.verrouillerGeneration();
                String premier = codeService.genererCodeActivite(TypeImport.PTA, objectif, 2034, idService);
                String second = codeService.genererCodeActivite(TypeImport.PTA, objectif, 2034, idService);

        assertThat(premier).matches("A-2034-\\d{4}-\\d{2,}");
        assertThat(sequence(second)).isEqualTo(sequence(premier) + 1);
    }

    @Test
    void objectifsDifferentsEtNouvelleAnneeOntDesCompteursIndependants() {
        int numeroObjectif = numeroUnique();
        ObjectifSpecifique premierObjectif = nouvelObjectif(numeroObjectif);
        ObjectifSpecifique secondObjectif = nouvelObjectif(numeroObjectif + 1);
        Integer idService = premierService();

        codeService.verrouillerGeneration();
        String codePremierObjectif = codeService.genererCodeActivite(
                TypeImport.PTA, premierObjectif, 2035, idService);
        String codeSecondObjectif = codeService.genererCodeActivite(
                TypeImport.PTA, secondObjectif, 2035, idService);
        String codeNouvelleAnnee = codeService.genererCodeActivite(
                TypeImport.PTA, premierObjectif, 2036, idService);

        assertThat(codePremierObjectif).isEqualTo("A-2035-" + numeroObjectif + "-01");
        assertThat(codeSecondObjectif).isEqualTo("A-2035-" + (numeroObjectif + 1) + "-01");
        assertThat(codeNouvelleAnnee).isEqualTo("A-2036-" + numeroObjectif + "-01");
    }

    @Test
    void nonPtaResteSansObjectifEtCodeFourniValideEstConserve() {
        Integer idService = premierService();
        codeService.verrouillerGeneration();
        String premier = codeService.genererCodeActivite(TypeImport.NON_PTA, null, 2082, idService);
        String codeFourni = codeService.validerCodeActiviteFourni(
                "A-2082-900", TypeImport.NON_PTA, null, 2082, idService);
        String suivant = codeService.genererCodeActivite(TypeImport.NON_PTA, null, 2082, idService);

        assertThat(premier).matches("A-2082-\\d+");
        assertThat(codeFourni).isEqualTo("A-2082-900");
        assertThat(suivant).isEqualTo("A-2082-901");
        assertThatThrownBy(() -> codeService.validerCodeActiviteFourni(
                "A-2082-01-01", TypeImport.NON_PTA, null, 2082, idService))
                .hasMessageContaining("NON_PTA");
    }

    @Test
    void memeCodeActiviteEstAutoriseDansDeuxServicesEtRefuseDansLeMeme() {
        Integer serviceA = premierService();
        Integer serviceB = jdbc.queryForObject(
                "SELECT max(id_service) FROM service", Integer.class);
        assertThat(serviceB).isNotEqualTo(serviceA);

        String code = "A-2077-777";
        codeService.verrouillerGeneration();
        codeService.validerCodeActiviteFourni(code, TypeImport.NON_PTA, null, 2077, serviceA);
        activiteRepository.saveAndFlush(Activite.builder()
                .code(code)
                .designation("Code réservé au service A")
                .dateDebutPrevue(LocalDate.of(2077, 1, 1))
                .service(mg.bank.backend.model.referentiel.Service.builder()
                        .idService(serviceA).build())
                .build());

        assertThatThrownBy(() -> codeService.validerCodeActiviteFourni(
                code, TypeImport.NON_PTA, null, 2077, serviceA))
                .hasMessageContaining("déjà utilisé");
        assertThat(codeService.validerCodeActiviteFourni(
                code, TypeImport.NON_PTA, null, 2077, serviceB)).isEqualTo(code);

        activiteRepository.saveAndFlush(Activite.builder()
                .code(code)
                .designation("Même code autorisé au service B")
                .dateDebutPrevue(LocalDate.of(2077, 1, 1))
                .service(mg.bank.backend.model.referentiel.Service.builder()
                        .idService(serviceB).build())
                .build());
    }

    @Test
    void sousActivitesReprennentLeCodeDuParentPtaOuNonPta() {
        Activite parentPta = Activite.builder()
                .idActivite(1_900_000_001)
                .code("A-2037-0072-04")
                .build();
        Activite parentNonPta = Activite.builder()
                .idActivite(1_900_000_002)
                .code("A-2037-14")
                .build();

        codeService.verrouillerGeneration();
        String pta1 = codeService.genererCodeSousActivite(parentPta);
        String pta2 = codeService.genererCodeSousActivite(parentPta);
        String nonPta = codeService.genererCodeSousActivite(parentNonPta);

        assertThat(pta1).isEqualTo("SA-2037-0072-04-01");
        assertThat(pta2).isEqualTo("SA-2037-0072-04-02");
        assertThat(nonPta).isEqualTo("SA-2037-14-01");
    }

    @Test
    void compteurNeReutilisePasLeCodeApresSuppression() {
        codeService.verrouillerGeneration();
        Integer idService = premierService();
        String codeSupprime = codeService.genererCodeActivite(TypeImport.NON_PTA, null, 2094, idService);
        Activite activite = activiteRepository.saveAndFlush(Activite.builder()
                .code(codeSupprime)
                .designation("Activité temporaire de test")
                .dateDebutPrevue(LocalDate.of(2094, 1, 1))
                .service(mg.bank.backend.model.referentiel.Service.builder()
                        .idService(idService)
                        .build())
                .build());

        activiteRepository.delete(activite);
        activiteRepository.flush();
        String nouveauCode = codeService.genererCodeActivite(TypeImport.NON_PTA, null, 2094, idService);

        assertThat(nouveauCode).isNotEqualTo(codeSupprime);
        assertThat(sequence(nouveauCode)).isEqualTo(sequence(codeSupprime) + 1);
    }

    private ObjectifSpecifique nouvelObjectif(int numero) {
        String code = "TEST-" + numero;
        jdbc.update("INSERT INTO objectif_specifique (code, designation, annee) VALUES (?, ?, ?)",
                code, "Objectif généré pour test", 2035);
        Integer id = jdbc.queryForObject(
                "SELECT id_objectif_specifique FROM objectif_specifique WHERE code = ?",
                Integer.class, code);
        return objectifRepository.findById(id).orElseThrow();
    }

    private int numeroUnique() {
        return 1_000 + (int) (Math.abs(System.nanoTime()) % 8_000);
    }

        private Integer premierService() {
                return jdbc.queryForObject("SELECT min(id_service) FROM service", Integer.class);
        }

    private int sequence(String code) {
        Matcher matcher = Pattern.compile("-(\\d+)$").matcher(code);
        assertThat(matcher.find()).isTrue();
        return Integer.parseInt(matcher.group(1));
    }
}

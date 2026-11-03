package mg.bank.backend;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import mg.bank.backend.enums.TypeImport;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.model.referentiel.Service;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.service.activite.CodeActiviteService;

@SpringBootTest
class CodeActiviteConcurrencyTests {

    @Autowired
    private CodeActiviteService codeService;

    @Autowired
    private ActiviteRepository activiteRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void deuxCreationsSimultaneesObtiennentDesCodesDifferents() throws Exception {
        int idService = jdbc.queryForObject("SELECT min(id_service) FROM service", Integer.class);
        CountDownLatch depart = new CountDownLatch(1);
        ExecutorService executeur = Executors.newFixedThreadPool(2);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        String code1 = null;
        String code2 = null;

        try {
            Future<String> premiere = executeur.submit(() -> creerSousTransaction(
                    depart, transaction, idService, "Création concurrente 1"));
            Future<String> seconde = executeur.submit(() -> creerSousTransaction(
                    depart, transaction, idService, "Création concurrente 2"));
            depart.countDown();

            code1 = premiere.get(20, TimeUnit.SECONDS);
            code2 = seconde.get(20, TimeUnit.SECONDS);

            assertThat(code1).matches("A-2093-\\d+");
            assertThat(code2).matches("A-2093-\\d+").isNotEqualTo(code1);
        } finally {
            executeur.shutdownNow();
            if (code1 != null) {
                jdbc.update("DELETE FROM activite WHERE code = ? AND id_service = ?", code1, idService);
            }
            if (code2 != null) {
                jdbc.update("DELETE FROM activite WHERE code = ? AND id_service = ?", code2, idService);
            }
        }
    }

    private String creerSousTransaction(
            CountDownLatch depart,
            TransactionTemplate transaction,
            int idService,
            String designation) throws Exception {
        depart.await(10, TimeUnit.SECONDS);
        return transaction.execute(status -> {
            codeService.verrouillerGeneration();
            String code = codeService.genererCodeActivite(TypeImport.NON_PTA, null, 2093, idService);
            activiteRepository.saveAndFlush(Activite.builder()
                    .code(code)
                    .designation(designation)
                    .dateDebutPrevue(LocalDate.of(2093, 1, 1))
                    .service(Service.builder().idService(idService).build())
                    .build());
            return code;
        });
    }
}

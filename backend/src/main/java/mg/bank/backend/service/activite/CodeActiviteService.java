package mg.bank.backend.service.activite;

import java.sql.PreparedStatement;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.enums.TypeImport;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.activite.Activite;
import mg.bank.backend.model.referentiel.ObjectifSpecifique;
import mg.bank.backend.repository.activite.ActiviteRepository;
import mg.bank.backend.repository.activite.SousActiviteRepository;

/**
 * Source unique de génération et de validation des codes d'activités.
 * Appelé dans les transactions d'écriture/import afin que le verrou consultatif
 * couvre l'allocation et l'insertion effective.
 */
@Service
@RequiredArgsConstructor
public class CodeActiviteService {

    private static final long CLE_VERROU_CODES = 92_500_003L;
    private static final Pattern DERNIER_NOMBRE = Pattern.compile("(\\d+)(?!.*\\d)");
    private static final Pattern PTA = Pattern.compile("^A-(\\d{4})-(\\d+)-(\\d{2,})$");
    private static final Pattern NON_PTA = Pattern.compile("^A-(\\d{4})-(\\d+)$");

    private final JdbcTemplate jdbcTemplate;
    private final ActiviteRepository activiteRepository;
    private final SousActiviteRepository sousActiviteRepository;

    public void verrouillerGeneration() {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT pg_advisory_xact_lock(?)")) {
                statement.setLong(1, CLE_VERROU_CODES);
                statement.execute();
            }
            return null;
        });
    }

    public String genererCodeActivite(
            TypeImport type, ObjectifSpecifique objectif, int annee, Integer idService) {
        if (idService == null) {
            throw erreur("Le service de rattachement est requis pour générer un code d'activité.");
        }
        if (type == TypeImport.PTA) {
            int numeroObjectif = numeroObjectif(objectif);
            String segmentObjectif = deuxChiffres(numeroObjectif);
            String cle = "ACT:PTA:" + idService + ":" + annee + ":" + objectif.getIdObjectifSpecifique();
            String expression = "^A-" + annee + "-" + segmentObjectif + "-(\\d+)$";
            return prochainCodeActivite(cle, expression, "id_service", idService,
                    numero -> codeDeLongueurValide(
                        "A-" + annee + "-" + segmentObjectif + "-" + deuxChiffres(numero)));
        }

        if (type != TypeImport.NON_PTA || objectif != null) {
            throw erreur("Le type d'activité et l'objectif spécifique sont incohérents.");
        }
        String cle = "ACT:NON_PTA:" + idService + ":" + annee;
        String expression = "^A-" + annee + "-(\\d+)$";
        return prochainCodeActivite(cle, expression, "id_service", idService,
            numero -> codeDeLongueurValide("A-" + annee + "-" + numero));
    }

    /** Suggestion indicative pour l'ancien endpoint NON_PTA ; la création alloue sous verrou. */
    public String proposerCodeNonPta(int annee, Integer idService) {
        if (idService == null) {
            throw erreur("Le service de rattachement est requis pour proposer un code NON_PTA.");
        }
        String cle = "ACT:NON_PTA:" + idService + ":" + annee;
        String expression = "^A-" + annee + "-(\\d+)$";
        int dernierEnBase = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX((regexp_match(code, ?))[1]::integer), 0) "
                        + "FROM activite WHERE id_service = ? AND code ~ ?",
                Integer.class, expression, idService, expression);
        Integer compteur = jdbcTemplate.query(
                "SELECT dernier_numero FROM code_sequence WHERE cle = ?",
                rs -> rs.next() ? rs.getInt(1) : 0,
                cle);
        int numero = Math.max(dernierEnBase, compteur) + 1;
        String code = "A-" + annee + "-" + numero;
        while (activiteRepository.existsByCodeIgnoreCaseAndService_IdService(code, idService)) {
            code = "A-" + annee + "-" + (++numero);
        }
        return code;
    }

    public String validerCodeActiviteFourni(
            String code, TypeImport type, ObjectifSpecifique objectif, int annee, Integer idService) {
        if (idService == null) {
            throw erreur("Le service de rattachement est requis pour valider un code d'activité.");
        }
        String valeur = code == null ? "" : code.trim();
        Matcher matcher;
        String cle;
        String expression;
        int sequence;

        if (type == TypeImport.PTA) {
            int numeroObjectif = numeroObjectif(objectif);
            matcher = PTA.matcher(valeur);
            if (!matcher.matches()
                    || entier(matcher.group(1), "L'année du code est invalide.") != annee
                    || entier(matcher.group(2), "Le numéro d'objectif du code est invalide.") != numeroObjectif) {
                throw erreur("Le code PTA doit respecter le format A-année-objectif-séquence de l'objectif spécifique.");
            }
            cle = "ACT:PTA:" + idService + ":" + annee + ":" + objectif.getIdObjectifSpecifique();
            expression = "^A-" + annee + "-" + deuxChiffres(numeroObjectif) + "-(\\d+)$";
            sequence = entier(matcher.group(3), "La séquence du code est trop grande.");
        } else {
            matcher = NON_PTA.matcher(valeur);
            if (type != TypeImport.NON_PTA || objectif != null
                    || !matcher.matches()
                    || entier(matcher.group(1), "L'année du code est invalide.") != annee) {
                throw erreur("Le code NON_PTA doit respecter le format A-année-numéro, sans objectif spécifique.");
            }
            cle = "ACT:NON_PTA:" + idService + ":" + annee;
            expression = "^A-" + annee + "-(\\d+)$";
            sequence = entier(matcher.group(2), "La séquence du code est trop grande.");
        }

        verifierCodeActiviteLibre(valeur, idService);
        avancerCompteur(cle, "activite", "id_service", idService, expression, sequence);
        return valeur;
    }

    public String genererCodeSousActivite(Activite activite) {
        PrefixeSousActivite prefixe = prefixeSousActivite(activite);
        String cle = "SA:" + activite.getIdActivite();
        String expression = "^" + prefixe.valeur() + "(\\d+)$";

        for (int tentative = 0; tentative < 1000; tentative++) {
            int numero = prochainNumero(cle, "sous_activite", "id_activite",
                    activite.getIdActivite(), expression);
            String candidat = codeDeLongueurValide(prefixe.valeur() + deuxChiffres(numero));
            if (!sousActiviteRepository.existsByCodeIgnoreCaseAndActivite_IdActivite(
                    candidat, activite.getIdActivite())) {
                return candidat;
            }
            avancerCompteur(cle, "sous_activite", "id_activite", activite.getIdActivite(), expression, numero);
        }
        throw erreur("Impossible de générer un code unique pour cette sous-activité.");
    }

    public String validerCodeSousActiviteFourni(String code, Activite activite) {
        String valeur = code == null ? "" : code.trim();
        PrefixeSousActivite prefixe = prefixeSousActivite(activite);
        Matcher matcher = Pattern.compile("^" + Pattern.quote(prefixe.valeur()) + "(\\d{2,})$")
                .matcher(valeur);
        if (!matcher.matches()) {
            throw erreur("Le code de la sous-activité doit reprendre exactement les références de son activité parente.");
        }
        entier(matcher.group(1), "La séquence du code est trop grande.");
        if (sousActiviteRepository.existsByCodeIgnoreCaseAndActivite_IdActivite(
                valeur, activite.getIdActivite())) {
            throw erreur("Ce code de sous-activité est déjà utilisé.");
        }
        avancerCompteur("SA:" + activite.getIdActivite(), "sous_activite", "id_activite",
                activite.getIdActivite(),
                "^" + prefixe.valeur() + "(\\d+)$",
                Integer.parseInt(matcher.group(1)));
        return valeur;
    }

    public void verifierCodeActiviteLibre(String code, Integer idService) {
        if (activiteRepository.existsByCodeIgnoreCaseAndService_IdService(code, idService)) {
            throw erreur("Ce code d'activité est déjà utilisé.");
        }
    }

    private String prochainCodeActivite(String cle, String expression,
            String colonnePerimetre, Integer idPerimetre,
            java.util.function.IntFunction<String> formateur) {
        for (int tentative = 0; tentative < 1000; tentative++) {
            int numero = prochainNumero(cle, "activite", colonnePerimetre, idPerimetre, expression);
            String candidat = formateur.apply(numero);
            if (!activiteRepository.existsByCodeIgnoreCaseAndService_IdService(candidat, idPerimetre)) {
                return candidat;
            }
            avancerCompteur(cle, "activite", colonnePerimetre, idPerimetre, expression, numero);
        }
        throw erreur("Impossible de générer un code d'activité unique.");
    }

    private int prochainNumero(String cle, String table, String colonnePerimetre,
            Integer idPerimetre, String expression) {
        int maximumExistant = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX((regexp_match(code, ?))[1]::integer), 0) "
                        + "FROM " + table + " WHERE " + colonnePerimetre + " = ? AND code ~ ?",
                Integer.class, expression, idPerimetre, expression);
        return jdbcTemplate.queryForObject(
                "INSERT INTO code_sequence (cle, dernier_numero) VALUES (?, ?) "
                        + "ON CONFLICT (cle) DO UPDATE SET dernier_numero = "
                        + "GREATEST(code_sequence.dernier_numero, EXCLUDED.dernier_numero - 1) + 1 "
                        + "RETURNING dernier_numero",
                Integer.class, cle, maximumExistant + 1);
    }

    private void avancerCompteur(String cle, String table, String colonnePerimetre,
            Integer idPerimetre, String expression, int numero) {
        int maximumExistant = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX((regexp_match(code, ?))[1]::integer), 0) "
                        + "FROM " + table + " WHERE " + colonnePerimetre + " = ? AND code ~ ?",
                Integer.class, expression, idPerimetre, expression);
        int minimum = Math.max(maximumExistant, numero);
        jdbcTemplate.update(
                "INSERT INTO code_sequence (cle, dernier_numero) VALUES (?, ?) "
                        + "ON CONFLICT (cle) DO UPDATE SET dernier_numero = "
                        + "GREATEST(code_sequence.dernier_numero, EXCLUDED.dernier_numero)",
                cle, minimum);
    }

    private PrefixeSousActivite prefixeSousActivite(Activite activite) {
        String code = activite == null || activite.getCode() == null ? "" : activite.getCode().trim();
        Matcher pta = PTA.matcher(code);
        Matcher nonPta = NON_PTA.matcher(code);
        if (pta.matches() || nonPta.matches()) {
            return new PrefixeSousActivite("SA-" + code.substring(2) + "-");
        }
        throw erreur("Le code de l'activité parente ne permet pas de générer une sous-activité cohérente.");
    }

    private int numeroObjectif(ObjectifSpecifique objectif) {
        String code = objectif == null ? null : objectif.getCode();
        if (code == null || code.isBlank()) {
            throw erreur("Le code de l'objectif spécifique est requis pour générer le code PTA.");
        }
        Matcher matcher = DERNIER_NOMBRE.matcher(code);
        if (!matcher.find()) {
            throw erreur("Impossible de dériver le numéro réel depuis le code de l'objectif spécifique « " + code + " ».");
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException exception) {
            throw erreur("Le numéro de l'objectif spécifique est trop grand pour être codifié.");
        }
    }

    private String deuxChiffres(int nombre) {
        return String.format(Locale.ROOT, "%02d", nombre);
    }

    private int entier(String valeur, String message) {
        try {
            return Integer.parseInt(valeur);
        } catch (NumberFormatException exception) {
            throw erreur(message);
        }
    }

    private String codeDeLongueurValide(String code) {
        if (code.length() > 20) {
            throw erreur("Le code généré dépasse la longueur maximale de 20 caractères.");
        }
        return code;
    }

    private ApiException erreur(String message) {
        return new ApiException(message, HttpStatus.BAD_REQUEST);
    }

    private record PrefixeSousActivite(String valeur) {
    }
}
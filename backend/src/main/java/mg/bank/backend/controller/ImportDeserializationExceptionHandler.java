package mg.bank.backend.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import mg.bank.backend.dto.ApiResponse;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ImportDeserializationExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handle(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(extraireDetail(ex)));
    }

    private String extraireDetail(HttpMessageNotReadableException ex) {
        String raw = ex.getMessage();
        if (raw == null || raw.isBlank()) {
            return "Le fichier JSON est invalide.";
        }

        if (raw.contains("Required request body is missing")
                || raw.contains("Required request body")) {
            return "Le corps de la requête est absent : aucun fichier JSON n'a été reçu.";
        }

        if (raw.contains("not one of the values accepted for Enum class")
                || raw.contains("not one of the values accepted for Enum")) {
            String champ = extraireChamp(raw);
            String valeur = extraireValeur(raw);

            if ("typeImport".equals(champ)) {
                return "Le champ « typeImport » doit contenir « PTA » ou « NON_PTA »."
                        + (valeur != null ? " Valeur reçue : " + valeur + "." : "");
            }

            return "La valeur fournie pour le champ « "
                    + (champ != null ? champ : "inconnu")
                    + " » n'est pas autorisée.";
        }

        if (raw.contains("JSON parse error")
                || raw.contains("Unexpected character")
                || raw.contains("Unexpected end-of-input")) {
            return "Le fichier JSON est mal formé. Vérifiez les virgules, "
                    + "guillemets et accolades.";
        }

        if (raw.contains("Cannot deserialize value of type")) {
            String champ = extraireChamp(raw);
            String valeur = extraireValeur(raw);
            StringBuilder sb = new StringBuilder();
            if (champ == null) {
                sb.append("Un champ du fichier JSON a un format invalide");
            } else {
                sb.append("Le champ « ").append(champ).append(" » a un format invalide");
            }
            if (valeur != null) {
                sb.append(" (valeur reçue : ").append(valeur).append(")");
            }
            sb.append(".");
            return sb.toString();
        }

        return "Le fichier JSON est invalide. Vérifiez le format et les valeurs "
                + "de chaque champ.";
    }

    private String extraireChamp(String raw) {
        int idx = raw.indexOf("through reference chain: ");
        if (idx < 0) {
            return null;
        }
        String tail = raw.substring(idx + "through reference chain: ".length());
        int lastOpen = tail.lastIndexOf("[\"");
        if (lastOpen < 0) {
            return null;
        }
        int end = tail.indexOf("\"]", lastOpen);
        if (end < 0) {
            return null;
        }
        return tail.substring(lastOpen + 2, end);
    }

    private String extraireValeur(String raw) {
        int fromIdx = raw.indexOf("from ");
        if (fromIdx < 0) {
            return null;
        }
        int colon = raw.indexOf(':', fromIdx);
        if (colon < 0) {
            return null;
        }
        String candidate = raw.substring(fromIdx + "from ".length(), colon).trim();
        int q1 = candidate.indexOf('"');
        int q2 = candidate.lastIndexOf('"');
        if (q1 >= 0 && q2 > q1) {
            return "« " + candidate.substring(q1 + 1, q2) + " »";
        }
        // Cas d'un type nu (ex. «Integer» pour "Cannot deserialize ... from Integer")
        if (!candidate.isEmpty() && candidate.length() < 50) {
            return "« " + candidate + " »";
        }
        return null;
    }
}
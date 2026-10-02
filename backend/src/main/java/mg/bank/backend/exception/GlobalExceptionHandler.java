package mg.bank.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import mg.bank.backend.dto.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(
            ApiException exception) {
        return ResponseEntity
                .status(exception.getStatus())
                .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .getFirst()
                .getDefaultMessage();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    /**
     * URL inconnue. Doit rester un 404 : sinon une faute de frappe cote
     * front, typiquement un prefixe duplique, renvoie un 500 indistinguable
     * d une vraie panne et masque la cause reelle.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            NoResourceFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Ressource introuvable"));
    }

    /**
     * Un fichier joint depasse la taille autorisee : 400, et non 500.
     *
     * SANS CE CAS, L ERREUR SERAIT INFERNALE POUR L UTILISATEUR
     *
     * Tomcat refuse la requete avant qu elle n atteigne le controleur : le
     * depassement est detecte au moment de lire la partie multipart, donc
     * LivrableEcritureService n est jamais appele et son controle de taille
     * n a rien pu faire. Sans ce handler, l exception tombe dans le cas
     * general et repond "Une erreur interne est survenue" : l utilisateur
     * verrait une panne du serveur pour un fichier trop lourd, et rien dans
     * le message ne lui dirait quoi faire.
     *
     * Aucun detail sur la limite dans le message : il est deja connu du
     * formulaire, qui annonce le maximum avant le depot.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(
            MaxUploadSizeExceededException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        "Un des fichiers joints dépasse la taille maximale autorisée"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception exception) {
        log.error("Erreur non geree", exception);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("Une erreur interne est survenue"));
    }
}

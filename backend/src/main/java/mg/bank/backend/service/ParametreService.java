package mg.bank.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.enums.ParametreEnum;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.Parametre;
import mg.bank.backend.repository.ParametreRepository;

@Service
@RequiredArgsConstructor
public class ParametreService {

    private final ParametreRepository parametreRepository;

    public Parametre getParametre(ParametreEnum parametreEnum) {

        return parametreRepository
                .findByCodeAndActifTrue(parametreEnum.getCode())
                .orElseThrow(() ->
                        new ApiException(
                                "Paramètre introuvable",
                                HttpStatus.INTERNAL_SERVER_ERROR
                        )
                );
    }

    public long getLongValue(ParametreEnum parametreEnum) {

        Parametre parametre = getParametre(parametreEnum);

        try {
            return Long.parseLong(parametre.getValeur());
        } catch (NumberFormatException e) {
            throw new ApiException(
                    "Valeur du paramètre invalide",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
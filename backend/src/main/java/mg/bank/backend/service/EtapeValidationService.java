package mg.bank.backend.service;

import java.util.HashMap;
import java.util.List;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.exception.ApiException;
import mg.bank.backend.model.EtapeValidation;
import mg.bank.backend.repository.EtapeValidationRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

import mg.bank.backend.model.Poste;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EtapeValidationService {

        private final EtapeValidationRepository etapeValidationRepository;

        /**
         * Récupère la première étape active d'une procédure.
         */
        public EtapeValidation getPremiereEtape(Integer idProcedure, Poste poste) {

                if (poste != null) {
                        Optional<EtapeValidation> etapeDuPoste = etapeValidationRepository
                                        .findFirstByProcedure_IdProcedureAndActifTrueAndPostesDecideurs_IdPosteOrderByNiveauAsc(
                                                        idProcedure,
                                                        poste.getIdPoste());

                        if (etapeDuPoste.isPresent()) {
                                Optional<EtapeValidation> suivante = etapeValidationRepository
                                                .findFirstByProcedure_IdProcedureAndActifTrueAndNiveauGreaterThanOrderByNiveauAsc(
                                                                idProcedure,
                                                                etapeDuPoste.get().getNiveau());

                                if (suivante.isPresent()) {
                                        return suivante.get();
                                }
                        }
                        return null;
                }

                return etapeValidationRepository
                                .findFirstByProcedure_IdProcedureAndActifTrueOrderByNiveauAsc(idProcedure)
                                .orElseThrow(() -> new ApiException(
                                                "Aucune étape active trouvée pour la procédure : " + idProcedure,
                                                HttpStatus.INTERNAL_SERVER_ERROR));
        }

        /**
         * Récupère l'étape immédiatement précédente
         * dans la même procédure.
         */
        public Optional<EtapeValidation> getEtapePrecedente(EtapeValidation etapeValidation) {
                if (etapeValidation == null || !etapeValidation.getRetour()) {
                        return Optional.empty();
                }
                return etapeValidationRepository
                                .findFirstByProcedure_IdProcedureAndNiveauLessThanOrderByNiveauDesc(
                                                etapeValidation.getProcedure().getIdProcedure(),
                                                etapeValidation.getNiveau());
        }

        /**
         * Récupère l'étape immédiatement suivante
         * dans la même procédure.
         */
        public Optional<EtapeValidation> getEtapeSuivante(EtapeValidation etapeValidation, Integer idProcedure,
                        Poste poste) {
                if (etapeValidation == null) {
                        return Optional.of(getPremiereEtape(idProcedure, poste));
                }
                return etapeValidationRepository
                                .findFirstByProcedure_IdProcedureAndNiveauGreaterThanOrderByNiveauAsc(
                                                etapeValidation.getProcedure().getIdProcedure(),
                                                etapeValidation.getNiveau());
        }

        public List<EtapeValidation> getEtapesByPoste(Integer idPoste) {
                return etapeValidationRepository.findByPostesDecideurs_IdPosteAndActifTrueOrderByNiveauAsc(idPoste);
        }

        public record EtapesVoisines(Optional<EtapeValidation> precedente,
                        Optional<EtapeValidation> suivante) {
        }

        @Transactional(readOnly = true)
        public Map<Integer, EtapesVoisines> getVoisinesParEtapes(List<EtapeValidation> etapes) {
                Map<Integer, EtapesVoisines> map = new HashMap<>(etapes.size());
                for (EtapeValidation e : etapes) {
                        map.put(e.getIdEtapeValidation(), new EtapesVoisines(
                                        getEtapePrecedente(e),
                                        getEtapeSuivante(e, e.getProcedure().getIdProcedure(), null)));
                }
                return map;
        }
}
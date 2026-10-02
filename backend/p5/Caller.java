import mg.bank.backend.dto.LivrableFormulaireDTO;
public class Caller {
    public Object call() {
        return LivrableFormulaireDTO.builder()
                .idLivrable(1)
                .designation("x")
                .description(null)
                .fichiersDepotes(true)
                .build();
    }
}

package mg.bank.backend.enums;

public enum ParametreEnum {

    DUREE_TOKEN_RESET_MOT_DE_PASSE(
            "DUREE_TOKEN_RESET_MOT_DE_PASSE"
    ),

    DUREE_TOKEN_ACTIVATION_COMPTE(
            "DUREE_TOKEN_ACTIVATION_COMPTE"
    );

    private final String code;

    ParametreEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
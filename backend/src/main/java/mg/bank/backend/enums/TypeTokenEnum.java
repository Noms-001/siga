package mg.bank.backend.enums;

public enum TypeTokenEnum {

    ACTIVATION_COMPTE("ACTIVATION_COMPTE"),
    RESET_MOT_DE_PASSE("RESET_MOT_DE_PASSE");

    private final String code;

    TypeTokenEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
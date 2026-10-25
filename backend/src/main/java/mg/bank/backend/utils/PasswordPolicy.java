package mg.bank.backend.utils;


public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static final int MIN_LENGTH = 8;

    public static final String REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$";

    public static final String MESSAGE_SIZE =
            "Le mot de passe doit contenir au moins 8 caractères";

    public static final String MESSAGE_PATTERN =
            "Le mot de passe doit contenir au moins une majuscule, "
            + "une minuscule, un chiffre et un caractère spécial";
}
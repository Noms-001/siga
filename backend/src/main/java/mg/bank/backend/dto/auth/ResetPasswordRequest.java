package mg.bank.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import mg.bank.backend.utils.PasswordPolicy;

@Getter
@Setter
public class ResetPasswordRequest {

        @NotBlank(message = "Le token est obligatoire")
        private String token;

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = PasswordPolicy.MIN_LENGTH, message = PasswordPolicy.MESSAGE_SIZE)
        @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE_PATTERN)
        private String password;

        @NotBlank(message = "La confirmation du mot de passe est obligatoire")
        private String confirmPassword;
}
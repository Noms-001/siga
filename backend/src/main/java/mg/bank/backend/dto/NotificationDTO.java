package mg.bank.backend.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class NotificationDTO {
    private Integer id;
    private String titre;
    private String message;
    private LocalDateTime dateCreation;
    private LocalDateTime dateLecture;
    private Integer idActivite;
    private String activite;
}

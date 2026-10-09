package mg.bank.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notification")
    private Integer idNotification;
    @Column(nullable = false) private String titre;
    @Column(nullable = false) private String message;
    @Column(name = "date_creation", nullable = false) private LocalDateTime dateCreation;
    @Column(name = "date_lecture") private LocalDateTime dateLecture;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_priorite", nullable = false) private Priorite priorite;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_utilisateur", nullable = false) private Utilisateur utilisateur;
    @Column(name = "id_reference") private Integer idReference;
    @Column(name = "resource", length = 100) private String resource;
}

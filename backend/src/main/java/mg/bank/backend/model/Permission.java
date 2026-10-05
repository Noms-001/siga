package mg.bank.backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permission")
    private Integer idPermission;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Column(name = "description")
    private String description;

    @Column(name = "ressource", nullable = false)
    private String ressource;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "actif", nullable = false)
    private Boolean actif;

    @Column(name = "date_desactivation")
    private LocalDateTime dateDesactivation;
}
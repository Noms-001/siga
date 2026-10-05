package mg.bank.backend.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import mg.bank.backend.enums.PorteePermission;

@Entity
@Table(name = "poste_permission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostePermission {

    @EmbeddedId
    private PostePermissionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idPoste")
    @JoinColumn(name = "id_poste")
    private Poste poste;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idPermission")
    @JoinColumn(name = "id_permission")
    private Permission permission;

    /**
     * La portée n'est pas NOT NULL en base (aucune contrainte dans le DDL) :
     * on la laisse nullable côté Java pour ne pas casser la lecture de
     * lignes existantes qui auraient été créées avant l'ajout de la colonne.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "portee")
    private PorteePermission portee;
}
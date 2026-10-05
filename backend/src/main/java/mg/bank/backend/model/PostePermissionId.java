package mg.bank.backend.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class PostePermissionId implements Serializable {

    @Column(name = "id_poste")
    private Integer idPoste;

    @Column(name = "id_permission")
    private Integer idPermission;

    public PostePermissionId() {
    }

    public PostePermissionId(Integer idPoste, Integer idPermission) {
        this.idPoste = idPoste;
        this.idPermission = idPermission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostePermissionId that)) return false;
        return Objects.equals(idPoste, that.idPoste)
                && Objects.equals(idPermission, that.idPermission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPoste, idPermission);
    }
}
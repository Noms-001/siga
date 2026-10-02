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
@Table(name = "objectif_specifique")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectifSpecifique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_objectif_specifique")
    private Integer idObjectifSpecifique;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Column(name = "annee", nullable = false)
    private Integer annee;

    /**
     * La base est proprietaire de cette colonne : DEFAULT CURRENT_TIMESTAMP.
     *
     * insertable et updatable sont donc desactives. Sans cela, Hibernate
     * ecrit la colonne meme quand le champ est null -- il ignore la valeur par
     * defaut de la base des qu elle fait partie de l INSERT -- et
     * l'insertion echoue sur la contrainte NOT NULL, en 500, pour un objectif
     * parfaitement valide. C'est le comportement observe avant cette regle :
     * le premier POST /objectifs renvoyait une erreur interne.
     *
     * C'est aussi la seule entite de cet objet que l application cree, les
     * autres etant des referentiels charges par le script de donnees. Le
     * defaut existe justement pour cela.
     *
     * La lecture, elle, reste possible : seule l'ecriture est retiree.
     */
    @Column(name = "date_creation", nullable = false, insertable = false, updatable = false)
    private LocalDateTime dateCreation;

}

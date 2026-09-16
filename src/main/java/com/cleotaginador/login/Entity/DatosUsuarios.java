package com.cleotaginador.login.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity 
@Table(name = "\"Datos_Usuario\"")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatosUsuarios {
    @Id
    private Long user_id;
    private String nombre1;

    @Column(name = "nombre2", insertable = false)
    private String nombre2;
    private String apellido1;

    @Column(name = "apellido2", insertable = false)
    private String apellido2;
    private LocalDate fechaNacimiento;
    
    @Column(name = "created_user", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;
    @Column(name = "updated_user", insertable = false, updatable = false)
    private LocalDateTime fechaActualizacion;

    // user_id toma el mismo valor que Usuarios.id (clave primaria compartida)
    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private UsuariosInfo usuariosInfo;
}

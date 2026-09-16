package com.cleotaginador.login.Entity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"Usuarios\"")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuariosInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String nickname;
    private String password;
    private String roles;

    // El id de DatosUsuarios se genera a partir de este id (ver @MapsId en DatosUsuarios.usuariosInfo)
    @OneToOne(mappedBy = "usuariosInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private DatosUsuarios datosUsuarios;
}


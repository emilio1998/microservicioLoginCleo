package com.cleotaginador.login.Repository;

import com.cleotaginador.login.Entity.UsuariosInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;


public interface IUsuarioInfoRepository extends JpaRepository<UsuariosInfo, Long> {
    Optional<UsuariosInfo> findByNickname(String nickname);

    // Trae UsuariosInfo junto con sus DatosUsuarios en una sola consulta (evita el N+1 del OneToOne no propietario)
    @Query("SELECT u FROM UsuariosInfo u LEFT JOIN FETCH u.datosUsuarios WHERE u.nickname = :nickname")
    Optional<UsuariosInfo> findByNicknameConDatos(@Param("nickname") String nickname);
}

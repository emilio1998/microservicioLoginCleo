package com.cleotaginador.login.Repository;

import com.cleotaginador.login.Entity.UsuariosInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface IUsuarioInfoRepository extends JpaRepository<UsuariosInfo, Long> {
    Optional<UsuariosInfo> findByNickname(String nickname);
}

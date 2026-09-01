package com.cleotaginador.login.DTO;
import java.time.Instant;

import com.cleotaginador.login.Entity.UsuariosInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshTokenResponse {
    private UsuariosInfo usuario;
    private String token;
    private Instant timestamp;
}

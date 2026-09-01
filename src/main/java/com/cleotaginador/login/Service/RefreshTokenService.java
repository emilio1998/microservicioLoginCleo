package com.cleotaginador.login.Service;
import com.cleotaginador.login.DTO.RefreshTokenResponse;
import com.cleotaginador.login.Repository.IUsuarioInfoRepository;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;

public class RefreshTokenService {
    private IUsuarioInfoRepository usuarioInfoRepository;

    @Autowired
    public RefreshTokenService(IUsuarioInfoRepository usuarioInfoRepository) {
        this.usuarioInfoRepository = usuarioInfoRepository;
    }

    public RefreshTokenResponse generateRefreshToken(String nickname) {
        RefreshTokenResponse refreshToken = RefreshTokenResponse.builder()
                .usuario(usuarioInfoRepository.findByNickname(nickname).get())
                .token("tokenId")
                .timestamp(Instant.now().plusMillis(600000))
                .build();
        return refreshToken;
    }
}

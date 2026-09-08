package com.cleotaginador.login.Service;
import com.cleotaginador.login.Entity.UsuariosInfo;
import com.cleotaginador.login.Repository.IUsuarioInfoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService implements UserDetailsService {

    private IUsuarioInfoRepository usuarioInfoRepository;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public UsuarioService(IUsuarioInfoRepository usuarioInfoRepository, PasswordEncoder passwordEncoder) {
        this.usuarioInfoRepository = usuarioInfoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String agregarUsuario(UsuariosInfo usuarioInfo) {
        usuarioInfo.setPassword(passwordEncoder.encode(usuarioInfo.getPassword()));
        usuarioInfoRepository.save(usuarioInfo);
        return "Usuario agregado correctamente";
    }

    public boolean nicknameExists(String nickname) {
        return usuarioInfoRepository.findByNickname(nickname).isPresent();
    }

    public UsuariosInfo obtenerUsuarioPorNickname(String nickname) {
        return usuarioInfoRepository.findByNickname(nickname)
            .orElse(null);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UsuariosInfo user = usuarioInfoRepository.findByNickname(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return User.builder()
            .username(user.getNickname())
            .password(user.getPassword())
            .roles(user.getRoles() != null ? user.getRoles() : "USER")
            .build();
    }
}

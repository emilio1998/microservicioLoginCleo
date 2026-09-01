package com.cleotaginador.login.Controllers;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.cleotaginador.login.DTO.AuthRequest;
import com.cleotaginador.login.DTO.AuthResponse;
import com.cleotaginador.login.Entity.UsuariosInfo;
import com.cleotaginador.login.Service.JwtService;
import com.cleotaginador.login.Service.RedisService;
import com.cleotaginador.login.Service.UsuarioService;
import com.cleotaginador.login.exception.GlobalLoginException;

@RestController
@RequestMapping("/auth")
public class AutenticacionController {
    
    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RedisService redisService;

    @Autowired
    public AutenticacionController(@Qualifier("authenticationManager") AuthenticationManager authenticationManager, UsuarioService usuarioService, JwtService jwtService, RedisService redisService) {
        this.authenticationManager = authenticationManager;
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
        this.redisService = redisService;
    }

    @PostMapping("/new")
    public ResponseEntity<Map<String, String>> addNewUser(@RequestBody UsuariosInfo userInfo) {
        boolean nicknameExists = usuarioService.nicknameExists(userInfo.getNickname());
        if (nicknameExists) {
            //return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Nickname already exists")); QUIERO HACER UNA PRUEBA CON UN THROW NUEVO PARA VER SI FUNCIONA EL CATCH DE LA EXCEPCION
            throw new GlobalLoginException(400, "Nickname already exists");
        }
        usuarioService.agregarUsuario(userInfo);
        Map<String, String> response = Map.of("status", "success", "message", "Operation completed");
        return ResponseEntity.ok(response); // Returns HTTP 200 OK with JSON body
    }

    @PostMapping("/autenticar")
    public AuthResponse autenticar(@RequestBody AuthRequest authR) {
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authR.getUsername(), authR.getPassword()));
            if (authentication.isAuthenticated()) {
                AuthResponse authResponse = jwtService.generarToken(authR.getUsername());
                UUID key = UUID.randomUUID();
                redisService.guardarConExpiracion(key.toString(), authResponse.getJwt(), 30, TimeUnit.MINUTES);
                return authResponse;
            } else {
                throw new RuntimeException("Authentication failed");
            }
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Authentication failed", e);
        }
    }
}

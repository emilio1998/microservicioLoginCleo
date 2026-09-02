package com.cleotaginador.login.Controllers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
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
import com.cleotaginador.login.DTO.JwtResponse;
import com.cleotaginador.login.DTO.RefreshTokenRequest;
import com.cleotaginador.login.DTO.RefreshTokenResponse;
import com.cleotaginador.login.Entity.UsuariosInfo;
import com.cleotaginador.login.Service.JwtService;
import com.cleotaginador.login.Service.RedisService;
import com.cleotaginador.login.Service.RefreshTokenService;
import com.cleotaginador.login.Service.UsuarioService;
import com.cleotaginador.login.exception.GlobalLoginException;

@RestController
@RequestMapping("/auth")
public class AutenticacionController {
    
    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RedisService redisService;
    private final RefreshTokenService refreshTokenService;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public AutenticacionController(@Qualifier("authenticationManager") AuthenticationManager authenticationManager, UsuarioService usuarioService, JwtService jwtService, RedisService redisService, RefreshTokenService refreshTokenService, StringRedisTemplate redisTemplate) {
        this.authenticationManager = authenticationManager;
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
        this.redisService = redisService;
        this.refreshTokenService = refreshTokenService;
        this.redisTemplate = redisTemplate;
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
    public JwtResponse autenticar(@RequestBody AuthRequest authR) {
        try {
            eliminarTodosLosTokens(); // Elimina todos los tokens antiguos antes de devolver el nuevo
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authR.getUsername(), authR.getPassword()));
            if (authentication.isAuthenticated()) {
                String authResponse = jwtService.generarToken(authR.getUsername());
                UUID key = UUID.randomUUID();
                redisService.guardarConExpiracion(key.toString() + ":accessToken", authResponse, 30, TimeUnit.MINUTES);

                RefreshTokenResponse refreshTokenResponse = refreshTokenService.generateRefreshToken(authR.getUsername());
                redisService.guardarConExpiracion(key.toString() + ":refreshToken", refreshTokenResponse.getToken(), 3, TimeUnit.DAYS);

                return JwtResponse.builder()
                        .accessToken(key.toString())
                        .refreshToken(refreshTokenResponse.getToken())
                        .build();
            } else {
                throw new RuntimeException("Authentication failed");
            }
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Authentication failed", e);
        }
    }

    @PostMapping("/refreshToken")
    public JwtResponse refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        try{
            borrarPorPatron(refreshTokenRequest.getKeyTokenRedis() + ":accessToken");
            Object storedRefreshToken = redisService.obtener(refreshTokenRequest.getKeyTokenRedis() + ":refreshToken");
            String accessToken = jwtService.generarToken(storedRefreshToken.toString());
            redisService.guardarConExpiracion(refreshTokenRequest.getKeyTokenRedis() + ":accessToken", accessToken, 30, TimeUnit.MINUTES);
            JwtResponse jwtResponse = JwtResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshTokenRequest.getKeyTokenRedis())
                    .build();
            return jwtResponse;
        }
        catch (Exception e){
            throw new GlobalLoginException(401, "El token es inválido");
        }
    }


    private void eliminarTodosLosTokens() {
        borrarPorPatron("*:accessToken");
        borrarPorPatron("*:refreshToken");
    }

    private void borrarPorPatron(String patron) {
        ScanOptions options = ScanOptions.scanOptions().match(patron).count(100).build();
        List<String> keysToDelete = new ArrayList<>();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            
            while (cursor.hasNext()) {
                keysToDelete.add(cursor.next());
                
                // Borra en bloques de 100 para optimizar rendimiento
                if (keysToDelete.size() >= 100) {
                    redisTemplate.delete(keysToDelete);
                    keysToDelete.clear();
                }
            }
            
            // Borra las claves sobrantes
            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error deleting keys by pattern", e);
        }
    }
}

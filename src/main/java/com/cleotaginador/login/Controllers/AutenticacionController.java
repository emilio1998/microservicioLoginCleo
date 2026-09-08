package com.cleotaginador.login.Controllers;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.cleotaginador.login.DTO.AuthRequest;
import com.cleotaginador.login.DTO.EntityResponse;
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
    public ResponseEntity<EntityResponse> autenticar(@RequestBody AuthRequest authR) {
        Map<String, Object> responseMap = new HashMap<>();
        if (authR.getUsername() == null || 
            authR.getUsername().isEmpty() ||
            authR.getPassword() == null ||
            authR.getPassword().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new EntityResponse("Username and password must be provided", 400)
                );
        }
        try {
            eliminarTodosLosTokens(); // Elimina todos los tokens antiguos antes de devolver el nuevo
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authR.getUsername(), authR.getPassword()));
            if (authentication.isAuthenticated()) {
                UsuariosInfo usuario = usuarioService.obtenerUsuarioPorNickname(authR.getUsername());

                Map<String, Object> responseUser = new HashMap<>();

                responseUser.put("id", usuario.getId());
                responseUser.put("nickname", usuario.getNickname());
                responseUser.put("roles", usuario.getRoles());

                String authResponse = jwtService.generarToken(authR.getUsername(), responseUser);
                UUID key = UUID.randomUUID();

                Map<String, Object> responseAccessToken = new HashMap<>();
                responseAccessToken.put("accessToken", authResponse);
                responseAccessToken.put("nickname", usuario.getNickname());
                redisService.guardarConExpiracion(key.toString() + ":accessToken", responseAccessToken, 30, TimeUnit.MINUTES);

                RefreshTokenResponse refreshTokenResponse = refreshTokenService.generateRefreshToken(authR.getUsername());
                Map<String, Object> responseRefreshToken = new HashMap<>();
                responseRefreshToken.put("refreshToken", refreshTokenResponse.getToken());
                responseRefreshToken.put("nickname", usuario.getNickname());
                redisService.guardarConExpiracion(key.toString() + ":refreshToken", responseRefreshToken, 1, TimeUnit.DAYS);

                JwtResponse jwtResponse = JwtResponse.builder()
                        .accessToken(key.toString())
                        .refreshToken(refreshTokenResponse.getToken())
                        .build();

                return ResponseEntity.ok(new EntityResponse(jwtResponse, 200));
            } else {
                return ResponseEntity.status(401).body(new EntityResponse("Credenciales inválidas", 401));
            }
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(new EntityResponse("Credenciales inválidas", 401));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new EntityResponse("Ocurrió un error en la autenticación", 500));
        }
    }

    @PostMapping("/refreshToken")
    public JwtResponse refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest, @RequestHeader("RefreshToken") String refreshToken) {
        try{
            borrarPorPatron(refreshTokenRequest.getKeyTokenRedis() + ":accessToken");

            @SuppressWarnings("unchecked")
            Map<String, Object> storedRefreshToken = (Map<String, Object>) redisService.obtener(refreshTokenRequest.getKeyTokenRedis() + ":refreshToken");

            if (storedRefreshToken == null || !storedRefreshToken.get("refreshToken").toString().equals(refreshToken)) {
                throw new GlobalLoginException(401, "El token es inválido");
            }
            
            UsuariosInfo usuario = usuarioService.obtenerUsuarioPorNickname(storedRefreshToken.get("nickname").toString());
            Map<String, Object> responseUser = new HashMap<>();
            responseUser.put("id", usuario.getId());
            responseUser.put("nickname", usuario.getNickname());
            responseUser.put("roles", usuario.getRoles());


            String accessToken = jwtService.generarToken(storedRefreshToken.get("nickname").toString(), responseUser);

            Map<String, Object> responseAccessToken = new HashMap<>();
            responseAccessToken.put("accessToken", accessToken);
            responseAccessToken.put("nickname", storedRefreshToken.get("nickname").toString());
            redisService.guardarConExpiracion(refreshTokenRequest.getKeyTokenRedis() + ":accessToken", responseAccessToken, 30, TimeUnit.MINUTES);
            JwtResponse jwtResponse = JwtResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
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

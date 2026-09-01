package com.cleotaginador.login.Service;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
public class RedisService {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // Guardar datos de forma permanente
    public void guardar(String llave, Object valor) {
        redisTemplate.opsForValue().set(llave, valor);
    }

    // Guardar datos con un tiempo de expiración (TTL)
    public void guardarConExpiracion(String llave, Object valor, long tiempo, TimeUnit unidad) {
        redisTemplate.opsForValue().set(llave, valor, tiempo, unidad);
    }

    // Obtener datos
    public Object obtener(String llave) {
        return redisTemplate.opsForValue().get(llave);
    }

    // Eliminar datos
    public void eliminar(String llave) {
        redisTemplate.delete(llave);
    }
}

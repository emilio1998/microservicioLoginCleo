package com.cleotaginador.login.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private int codigo;
    private String mensaje;
    private String timestamp;
}

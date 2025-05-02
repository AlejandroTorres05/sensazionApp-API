package co.edu.icesi.sensazionappapi.controller;

import co.edu.icesi.sensazionappapi.dto.UserDto;
import co.edu.icesi.sensazionappapi.entity.User;
import co.edu.icesi.sensazionappapi.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Endpoint para crear o actualizar un usuario
     * Se ejecuta cuando el usuario se registra o actualiza su perfil
     */
    @PostMapping
    public ResponseEntity<User> createUser(
            @RequestBody UserDto userDto,
            @AuthenticationPrincipal Jwt jwt) {

        // Crea el usuario con los datos básicos del JWT
        User user = userService.createUserFromJwt(jwt);

        // Completa con los datos adicionales del DTO
        user.setName(userDto.getName());
        user.setPhone(userDto.getPhone());

        // Guarda y devuelve la respuesta
        User savedUser = userService.saveUser(user);
        return ResponseEntity.ok(savedUser);
    }

    /**
     * Endpoint para obtener el usuario actual
     */
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getCurrentUser(jwt);
        return ResponseEntity.ok(user);
    }
}
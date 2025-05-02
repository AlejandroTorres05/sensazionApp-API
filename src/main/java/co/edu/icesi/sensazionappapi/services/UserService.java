package co.edu.icesi.sensazionappapi.services;

import co.edu.icesi.sensazionappapi.entity.User;
import org.springframework.security.oauth2.jwt.Jwt;

public interface UserService {
    /**
     * Crea un nuevo usuario a partir de la información del JWT de Auth0
     * @param jwt El token JWT de Auth0
     * @return El usuario creado
     */
    User createUserFromJwt(Jwt jwt);

    /**
     * Obtiene el usuario actual basado en el JWT de Auth0
     * @param jwt El token JWT de Auth0
     * @return El usuario encontrado
     */
    User getCurrentUser(Jwt jwt);

    /**
     * Guarda o actualiza un usuario en la base de datos
     * @param user El usuario a guardar
     * @return El usuario guardado
     */
    User saveUser(User user);
}

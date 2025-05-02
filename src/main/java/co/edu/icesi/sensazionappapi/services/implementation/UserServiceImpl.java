package co.edu.icesi.sensazionappapi.services.implementation;

import co.edu.icesi.sensazionappapi.entity.User;
import co.edu.icesi.sensazionappapi.repository.UserRepository;
import co.edu.icesi.sensazionappapi.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public User getCurrentUser(Jwt jwt) {
        String auth0Id = jwt.getSubject(); // ID de Auth0
        return userRepository.findByAuth0Id(auth0Id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    @Override
    public User createUserFromJwt(Jwt jwt) {
        // Verificar si el usuario ya existe
        String auth0Id = jwt.getSubject();
        return userRepository.findByAuth0Id(auth0Id)
                .orElseGet(() -> {
                    User user = new User();
                    user.setAuth0Id(auth0Id);

                    // Extraer el email del claim (si existe)
                    if (jwt.getClaims().containsKey("email")) {
                        user.setEmail(jwt.getClaim("email"));
                    }

                    return user; // No guardamos aún, se completará con los datos del DTO
                });
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}

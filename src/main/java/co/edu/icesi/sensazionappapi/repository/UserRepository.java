package co.edu.icesi.sensazionappapi.repository;

import co.edu.icesi.sensazionappapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByName(String name);
    Optional<User> findByEmail(String email);
    Optional<User> findByAuth0Id(String auth0Id);
}

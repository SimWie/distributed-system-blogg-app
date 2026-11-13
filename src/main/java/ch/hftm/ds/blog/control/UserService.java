package ch.hftm.ds.blog.control;

import java.util.List;

import ch.hftm.ds.blog.entity.User;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class UserService {
    @Inject
    UserRepository userRepository;

    public List<User> getUsers() {
        var users = userRepository.listAll();
        Log.debugf("Returning %d users", users.size());
        return users;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Gibt den Benutzer mit dem übergebenen Benutzernamen zurück und legt ihn
     * an, falls er noch nicht existiert. Wird verwendet, um den in Keycloak
     * angemeldeten Benutzer einem lokalen {@link User} zuzuordnen.
     */
    @Transactional
    public User getOrCreateUser(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            user = new User(username);
            addUser(user);
            Log.infof("Created local user '%s' (id %d) for authenticated Keycloak user", username, user.getId());
        }
        return user;
    }

    @Transactional
    public void addUser(User user) {
        userRepository.persist(user);
        Log.debugf("Persisted user %d '%s'", user.getId(), user.getUsername());
    }

    /**
     * Aktualisiert den Benutzernamen eines bestehenden Benutzers.
     *
     * @return den aktualisierten Benutzer, oder {@code null}, falls kein
     *         Benutzer mit der übergebenen id existiert.
     */
    @Transactional
    public User updateUser(Long id, User updated) {
        User existing = userRepository.findById(id);
        if (existing == null) return null;
        existing.setUsername(updated.getUsername());
        Log.debugf("Updated user %d", id);
        return existing;
    }

    @Transactional
    public boolean deleteUser(Long id) {
        boolean deleted = userRepository.deleteById(id);
        Log.debugf("Delete user %d: %s", id, deleted ? "deleted" : "not found");
        return deleted;
    }
}

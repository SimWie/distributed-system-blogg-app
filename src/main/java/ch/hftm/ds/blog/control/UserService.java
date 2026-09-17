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
        Log.info("Returning " + users.size() + " users");
        return users;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public void addUser(User user) {
        Log.info("Adding user " + user.getUsername());
        userRepository.persist(user);
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
        Log.info("Updating user " + id);
        return existing;
    }

    @Transactional
    public boolean deleteUser(Long id) {
        Log.info("Deleting user " + id);
        return userRepository.deleteById(id);
    }
}

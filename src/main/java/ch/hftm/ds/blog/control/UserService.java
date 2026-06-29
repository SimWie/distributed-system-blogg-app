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

    @Transactional
    public boolean updateUser(Long id, User updated) {
        User existing = userRepository.findById(id);
        if (existing == null) return false;
        existing.setUsername(updated.getUsername());
        Log.info("Updating user " + id);
        return true;
    }

    @Transactional
    public boolean deleteUser(Long id) {
        Log.info("Deleting user " + id);
        return userRepository.deleteById(id);
    }
}

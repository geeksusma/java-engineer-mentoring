package java8.optional;

import java.util.List;
import java.util.Optional;

/**
 * Returns Optional<User> instead of a nullable User — the signature itself
 * tells the caller that "not found" is an expected outcome, not an error.
 */
public final class UserRepository {

    private final List<User> users;

    public UserRepository(List<User> users) {
        this.users = users;
    }

    public Optional<User> findById(long id) {
        return users.stream()
                .filter(u -> u.id() == id)
                .findFirst();
    }

    public Optional<String> findEmail(long id) {
        return findById(id).map(User::email);
    }

    public Optional<Address> findPrimaryAddress(long id) {
        return findById(id).flatMap(User::primaryAddressOptional);
    }

    public Optional<User> findAdminById(long id) {
        return findById(id).filter(User::admin);
    }
}

package java8.optional;

import java.util.Optional;

public record User(long id, String name, String email, Address primaryAddress, boolean admin) {

    public Optional<Address> primaryAddressOptional() {
        return Optional.ofNullable(primaryAddress);
    }
}

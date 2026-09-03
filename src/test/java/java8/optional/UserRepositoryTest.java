package java8.optional;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the idiomatic way to consume Optional: map/flatMap/filter and
 * orElse*, never isPresent() + get().
 */
class UserRepositoryTest {

    private final User ana = new User(1L, "Ana", "ana@example.com",
            new Address("Madrid", "Spain"), false);
    private final User bob = new User(2L, "Bob", "bob@example.com", null, true);

    private final UserRepository repository = new UserRepository(List.of(ana, bob));

    @Test
    void should_returnAPresentOptional_when_theUserExists() {
        assertEquals(Optional.of(ana), repository.findById(1L));
    }

    @Test
    void should_returnAnEmptyOptional_when_theUserDoesNotExist() {
        assertEquals(Optional.empty(), repository.findById(999L));
    }

    @Test
    void should_transformThePresentValue_when_mappingToItsEmail() {
        assertEquals(Optional.of("ana@example.com"), repository.findEmail(1L));
    }

    @Test
    void should_stayEmpty_when_mappingAnAbsentUserToItsEmail() {
        assertEquals(Optional.empty(), repository.findEmail(999L));
    }

    @Test
    void should_flattenNestedOptionals_when_chainingFlatMapToAnOptionalReturningAccessor() {
        assertEquals(Optional.of(new Address("Madrid", "Spain")), repository.findPrimaryAddress(1L));
        assertEquals(Optional.empty(), repository.findPrimaryAddress(2L)); // Bob has no address at all
    }

    @Test
    void should_becomeEmpty_when_filterPredicateDoesNotMatch() {
        assertEquals(Optional.empty(), repository.findAdminById(1L)); // Ana isn't an admin
        assertTrue(repository.findAdminById(2L).isPresent());         // Bob is
    }

    @Test
    void should_throwTheSuppliedException_when_orElseThrowIsUsedOnAnEmptyOptional() {
        assertThrows(NoSuchElementException.class,
                () -> repository.findById(999L)
                        .orElseThrow(() -> new NoSuchElementException("No user with id 999")));
    }
}

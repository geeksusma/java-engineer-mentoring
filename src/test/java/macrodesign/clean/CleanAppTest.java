package macrodesign.clean;

import macrodesign.clean.frameworks.CleanApp;
import macrodesign.clean.interfaceadapters.presenters.RegisterPlayerViewModel;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CleanAppTest {

    private final CleanApp app = new CleanApp();

    @Test
    void should_renderPresenterViewModel_when_requestCrossesAllCircles() {
        assertEquals(new RegisterPlayerViewModel(201, "Welcome, Neo! (P-1)"),
                app.handleRegistration(Map.of("nickname", "Neo", "email", "neo@example.com")));
        assertEquals(new RegisterPlayerViewModel(409, "Sorry, 'neo' is already taken"),
                app.handleRegistration(Map.of("nickname", "neo", "email", "other@example.com")));
        assertEquals(new RegisterPlayerViewModel(422, "Invalid nickname: nickname must be 3-16 letters, digits or underscores"),
                app.handleRegistration(Map.of("nickname", "a", "email", "a@example.com")));

        assertEquals(1, app.table.count());
    }
}

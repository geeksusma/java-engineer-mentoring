package macrodesign.clean.interfaceadapters.controllers;

import macrodesign.clean.usecases.RegisterPlayerInputBoundary;
import macrodesign.clean.usecases.RegisterPlayerRequest;

import java.util.Map;

/**
 * Converts whatever the delivery mechanism hands over (here: submitted form
 * fields) into the request model. It returns nothing — the presenter owns the
 * answer.
 */
public class RegisterPlayerController {

    private final RegisterPlayerInputBoundary registerPlayer;

    public RegisterPlayerController(RegisterPlayerInputBoundary registerPlayer) {
        this.registerPlayer = registerPlayer;
    }

    public void submit(Map<String, String> form) {
        registerPlayer.register(new RegisterPlayerRequest(form.get("nickname"), form.get("email")));
    }
}

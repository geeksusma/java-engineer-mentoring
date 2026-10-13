package macrodesign.clean.interfaceadapters.presenters;

import macrodesign.clean.usecases.RegisterPlayerOutputBoundary;
import macrodesign.clean.usecases.RegisterPlayerResponse;

public class RegisterPlayerPresenter implements RegisterPlayerOutputBoundary {

    private RegisterPlayerViewModel viewModel;

    @Override
    public void presentRegistered(RegisterPlayerResponse response) {
        viewModel = new RegisterPlayerViewModel(201, "Welcome, " + response.nickname() + "! (" + response.playerId() + ")");
    }

    @Override
    public void presentInvalidNickname(String reason) {
        viewModel = new RegisterPlayerViewModel(422, "Invalid nickname: " + reason);
    }

    @Override
    public void presentNicknameTaken(String nickname) {
        viewModel = new RegisterPlayerViewModel(409, "Sorry, '" + nickname + "' is already taken");
    }

    public RegisterPlayerViewModel viewModel() {
        return viewModel;
    }
}

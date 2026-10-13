package macrodesign.clean.frameworks;

import macrodesign.clean.frameworks.database.PlayerTable;
import macrodesign.clean.interfaceadapters.controllers.RegisterPlayerController;
import macrodesign.clean.interfaceadapters.gateways.PlayerRepository;
import macrodesign.clean.interfaceadapters.presenters.RegisterPlayerPresenter;
import macrodesign.clean.interfaceadapters.presenters.RegisterPlayerViewModel;
import macrodesign.clean.usecases.RegisterPlayerInteractor;

import java.util.Map;

/**
 * Main component: the dirtiest part of the system, it builds everything and
 * plays the role of the web framework handing a request to the controller.
 * A presenter holds per-request state, so each request gets its own.
 */
public final class CleanApp {

    public final PlayerTable table = new PlayerTable();

    public RegisterPlayerViewModel handleRegistration(Map<String, String> form) {
        var presenter = new RegisterPlayerPresenter();
        var interactor = new RegisterPlayerInteractor(new PlayerRepository(table), presenter);
        new RegisterPlayerController(interactor).submit(form);
        return presenter.viewModel();
    }
}

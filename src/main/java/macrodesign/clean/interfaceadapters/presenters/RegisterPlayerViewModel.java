package macrodesign.clean.interfaceadapters.presenters;

/**
 * Everything the view needs, already formatted. The view just renders it.
 */
public record RegisterPlayerViewModel(int status, String message) {
}

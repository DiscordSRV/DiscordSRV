package github.scarsz.discordsrv.api;

/**
 * Interface providing cancellation related methods to certain events
 */
public interface Cancellable {

    boolean isCancelled();
    void setCancelled(boolean cancelled);

}

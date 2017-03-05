package github.scarsz.discordsrv.api;

/**
 * Interface providing cancellation related events to certain events
 */
public interface Cancellable {

    boolean isCancelled();
    void setCancelled(boolean cancelled);

}

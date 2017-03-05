package github.scarsz.discordsrv.api;

/**
 * todo
 */
public interface Cancellable {

    boolean isCancelled();
    void setCancelled(boolean cancelled);

}

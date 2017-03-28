package github.scarsz.discordsrv.api.events;

import lombok.Getter;

/**
 * <p>Called directly after a debug report was submitted to GitHub Gists and the requester was informed.</p>
 */
public class DebugReportedEvent extends Event {

    @Getter private final String message;
    @Getter private final String requester;
    @Getter private final String url;

    public DebugReportedEvent(String message, String requester, String url) {
        this.message = message;
        this.requester = requester;
        this.url = url;
    }

}

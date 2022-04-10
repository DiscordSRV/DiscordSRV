package github.scarsz.discordsrv.api.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.bukkit.plugin.Plugin;

import java.util.Set;

/**
 * fired when a result of slash commands registration appears,if there is an error, you shouldn't log the error, we will do it instead.
 */
@RequiredArgsConstructor
public class SlashCommandsPostRegistrationEvent extends Event{
    /**
     * get the result for this registration event
     */
    @Getter
    private final RegistrationResult result;
    /**
     * Get the plugins who added their commands to this, should never be empty.
     */
    @Getter
    private final Set<Plugin> plugins;

    public enum RegistrationResult {
        /**
         * Successfully registered the commands, nothing wrong happened
         */
        SUCCESS,
        /**
         * Rate limited
         */
        RATE_LIMIT,
        /**
         * Missing the required scope to register the commands
         */
        MISSING_SCOPE,
        /**
         * Unknown Error
         */
        UNKNOWN_ERROR;

        public static RegistrationResult getResult(Throwable t) {
            if (t instanceof ErrorResponseException) {
                ErrorResponseException ex = (ErrorResponseException) t;
                if (ex.getErrorResponse() == ErrorResponse.MISSING_ACCESS) return MISSING_SCOPE;
            } else if (t instanceof RateLimitedException) return RATE_LIMIT;
            return UNKNOWN_ERROR;
        }
    }
}

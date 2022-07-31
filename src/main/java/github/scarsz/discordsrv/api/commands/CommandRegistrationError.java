package github.scarsz.discordsrv.api.commands;

import lombok.Getter;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;

public enum CommandRegistrationError {

    /**
     * Missing the required scope to register the commands
     */
    MISSING_SCOPE("missing application.commands scope"),
    /**
     * Unknown error
     */
    UNKNOWN_ERROR("an unknown error");

    @Getter
    private final String friendlyMeaning;

    CommandRegistrationError(String friendlyMeaning) {
        this.friendlyMeaning = friendlyMeaning;
    }

    public static CommandRegistrationError fromThrowable(Throwable t) {
        if (t instanceof ErrorResponseException) {
            ErrorResponseException ex = (ErrorResponseException) t;
            if (ex.getErrorResponse() == ErrorResponse.MISSING_ACCESS) return MISSING_SCOPE;
        }
        return UNKNOWN_ERROR;
    }

}

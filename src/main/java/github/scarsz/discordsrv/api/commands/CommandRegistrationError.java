package github.scarsz.discordsrv.api.commands;

import lombok.Value;
import net.dv8tion.jda.api.entities.Guild;

@Value
public class CommandRegistrationError {

    Guild guild;
    Throwable exception;

}

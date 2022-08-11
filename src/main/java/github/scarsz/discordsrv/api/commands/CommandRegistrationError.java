package github.scarsz.discordsrv.api.commands;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;

@RequiredArgsConstructor
public class CommandRegistrationError {

    @Getter
    private final Guild guild;
    @Getter
    private final Throwable exception;

}

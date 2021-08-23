/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2021 Austin "Scarsz" Shapiro
 * -------------
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * END
 */

package github.scarsz.discordsrv.objects.threads;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.ConsoleMessage;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.util.Deque;
import java.util.concurrent.TimeUnit;

public class ConsoleMessageQueueWorker extends Thread {

    private static final char LINE_WRAP_INDENT = '\t';
    private static final long MIN_SLEEP_TIME_MILLIS = 2000;
    private static final String SLEEP_TIME_SECONDS_KEY = "DiscordConsoleChannelLogRefreshRateInSeconds";

    private final StringBuilder message = new StringBuilder();
    private final Deque<ConsoleMessage> queue = DiscordSRV.getPlugin().getConsoleMessageQueue();

    public ConsoleMessageQueueWorker() {
        super("DiscordSRV - Console Message Queue Worker");
    }

    @Override
    public void run() {
        while (true) {
            try {
                // don't process, if we get disconnected, another measure to prevent UnknownHostException spam
                if (DiscordUtil.getJda().getStatus() != JDA.Status.CONNECTED) {
                    Thread.sleep(3000);
                    continue;
                }
                final String prefix = LangUtil.Message.CONSOLE_CHANNEL_MESSAGE_PREFIX.toString();
                final String suffix = LangUtil.Message.CONSOLE_CHANNEL_MESSAGE_SUFFIX.toString();
                final int wrapperLength = prefix.length() + suffix.length();

                // reuse message builder to avoid garbage - guaranteed to never grow beyond Message.MAX_CONTENT_LENGTH
                message.setLength(0);
                ConsoleMessage consoleMessage;
                // peek to avoid polling a message that we can't process from the queue
                while ((consoleMessage = queue.peek()) != null) {
                    String formattedMessage = consoleMessage.toString();
                    if (formattedMessage == null) {
                        queue.poll();
                        continue;
                    }

                    final int checkLength = formattedMessage.length() + wrapperLength + 1;
                    if (message.length() + checkLength > Message.MAX_CONTENT_LENGTH) {
                        // if the line itself would be too long anyway, chop it down and put parts back in queue
                        if (checkLength > Message.MAX_CONTENT_LENGTH) {
                            chopHead(wrapperLength);
                        }
                        break;
                    }
                    message.append(formattedMessage).append('\n');

                    // finally poll to actually remove the appended message
                    queue.poll();
                }

                final String m = message.toString();
                if (StringUtils.isNotBlank(m)) {
                    TextChannel textChannel = DiscordSRV.getPlugin().getConsoleChannel();
                    if (textChannel != null && textChannel.getGuild().getSelfMember().hasPermission(textChannel, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE)) {
                        textChannel.sendMessage(prefix + m + suffix).queue();
                    }
                }

                // make sure rate isn't less than every MIN_SLEEP_TIME_MILLIS because of rate limitations
                long sleepTimeMS = TimeUnit.SECONDS.toMillis(DiscordSRV.config().getIntElse(SLEEP_TIME_SECONDS_KEY, 0));
                if (sleepTimeMS < MIN_SLEEP_TIME_MILLIS) {
                    sleepTimeMS = MIN_SLEEP_TIME_MILLIS;
                }

                Thread.sleep(sleepTimeMS);
            } catch (InterruptedException e) {
                DiscordSRV.debug("Broke from Console Message Queue Worker thread: sleep interrupted");
                return;
            } catch (Throwable e) {
                DiscordSRV.error("Error in Console Message Queue worker: " + e.getMessage());
                DiscordSRV.debug(e);
            }
        }
    }

    /**
     * Chops down the head {@link ConsoleMessage} in the queue to parts that don't exceed the {@link Message#MAX_CONTENT_LENGTH} after formatting.
     *
     * @param wrapperLength The length of the message wrapper (prefix + suffix)
     */
    private void chopHead(int wrapperLength) {
        ConsoleMessage consoleMessage;
        String formattedMessage;
        while ((consoleMessage = queue.poll()) != null) {
            formattedMessage = consoleMessage.toString();
            if (formattedMessage != null) {
                break;
            }
        }

        if (consoleMessage != null) {
            // length added to the message by the formatting
            int formattingDelta = consoleMessage.toString().length() - consoleMessage.getLine().length();
            // maximum line length, accounting for formatting, prefix/suffix, line break, and LINE_WRAP_INDENT
            int maxLineLength = Message.MAX_CONTENT_LENGTH - wrapperLength - formattingDelta - 2;
            String[] lines = WordUtils.wrap(consoleMessage.getLine(), maxLineLength, "\n", true).split("\n");

            // traverse each line in reverse order, to ensure they can be correctly added back to the head of the queue
            for (int i = lines.length - 1; i >= 1; i--) {
                String line = lines[i].trim();
                if (!line.isEmpty()) {
                    queue.addFirst(new ConsoleMessage(consoleMessage.getEventLevel(), LINE_WRAP_INDENT + line));
                }
            }
            // omit indent on the first message
            queue.addFirst(new ConsoleMessage(consoleMessage.getEventLevel(), lines[0]));
        }
    }
}

package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.bukkit.ChatColor;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>Made by Scarsz</p>
 * <p>German translations by Androkai & GerdSattler</p>
 *
 * @in /dev/hell
 * @on 3/13/2017
 * @at 2:54 PM
 */
public class LangUtil {

    public enum Language {

        EN("en", "English"),
        DE("de", "German");

        @Getter final String code;
        @Getter final String name;

        Language(String code, String name) {
            this.code = code;
            this.name = name;
        }

    }

    /**
     * Messages that are internal to DiscordSRV and are thus not customizable
     */
    public enum InternalMessage {

        ASM_WARNING(new HashMap<Language, String>() {{
            put(Language.EN,
                    "\n" +
                    "\n" +
                    "You're attempting to use DiscordSRV on ASM 4. DiscordSRV requires ASM 5 to function.\n" +
                    "DiscordSRV WILL NOT WORK without ASM 5. Blame your server software's developers for having outdated libraries.\n" +
                    "\n" +
                    "Instructions for updating to ASM 5:\n" +
                    "1. Navigate to the {specialsourcefolder} folder of the server\n" +
                    "2. Delete the SpecialSource-1.7-SNAPSHOT.jar jar file\n" +
                    "3. Download SpecialSource v1.7.4 from http://central.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
                    "4. Copy the jar file to the {specialsourcefolder} folder of the server you navigated to earlier\n" +
                    "5. Rename the jar file you just copied to SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "6. Restart the server\n" +
                    "\n" +
                    "\n");
            put(Language.DE,
                    "\n" +
                    "\n" +
                    "Du versuchst DiscordSRV mit ASM 4 zu nuten. DiscordSRV benötigt ASM 5, um zu funktionieren.\n" +
                    "DiscordSRV wird ohne ASM5 NICHT funktionieren. Beschuldige die Entwickler deiner Serversoftware dafür, veraltete Bibliotheken zu nutzen.\n" +
                    "\n" +
                    "Anleitung zum Nutzen von ASM 5:\n" +
                    "1. Navigiere zum Ordner {specialsourcefolder} deines Servers\n" +
                    "2. Lösche die Datei SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "3. Lade dir die Datei SpecialSource v1.7.4 von hier herunter: http://central.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
                    "4. Kopiere die jar Datei in den zuvor ausgewählten Ordner {specialsourcefolder}\n" +
                    "5. Bennen die kopierte jar Datei in SpecialSource-1.7-SNAPSHOT.jar um\n" +
                    "6. Starte deinen Server neu\n" +
                    "\n" +
                    "\n");
        }}), CONSOLE_FORWARDING_ASSIGNED_TO_CHANNEL(new HashMap<Language, String>() {{
            put(Language.EN, "Console forwarding assigned to channel");
            put(Language.DE, "Konsolenausgabeweiterleitung aktiv");
        }}), DONATOR_THANKS(new HashMap<Language, String>() {{
            put(Language.EN, "Thank you so much to these people for allowing DiscordSRV to grow to what it is");
            put(Language.DE, "Vielen Dank an die folgenden Leute, sie haben DiscordSRV zu dem gemacht, was es heute ist");
        }}), FOUND_SERVER(new HashMap<Language, String>() {{
            put(Language.EN, "Found server");
            put(Language.DE, "Server wurde gefunden");
        }}), NOT_FORWARDING_CONSOLE_OUTPUT(new HashMap<Language, String>() {{
            put(Language.EN, "Console channel ID was blank, not forwarding console output");
            put(Language.DE, "Konsolenkanal-ID ist leer, keine Konsolenausgabeweiterleitung aktiv");
        }}), PLUGIN_CANCELLED_CHAT_EVENT(new HashMap<Language, String>() {{
            put(Language.EN, "Plugin {plugin} cancelled AsyncPlayerChatEvent (author: {author} | message: {message})");
            put(Language.DE, "Plugin {plugin} brach AsyncPlayerChatEvent ab (Author: {author} | Nachricht: {message})");
        }}), COLORS(new HashMap<Language, String>() {{
            put(Language.EN, "Colors:");
            put(Language.DE, "Farben:");
        }}), SHUTDOWN_COMPLETED(new HashMap<Language, String>() {{
            put(Language.EN, "Shutdown completed in {ms}ms");
            put(Language.DE, "Herunterfahren wurde abgeschlossen in {ms}ms");
        }}), LANGUAGE_INITIALIZED(new HashMap<Language, String>() {{
            put(Language.EN, "Language initialized as ");
            put(Language.DE, "Sprache initialisiert als ");
        }}), API_LISTENER_SUBSCRIBED(new HashMap<Language, String>() {{
            put(Language.EN, "API listener {listenername} subscribed ({methodcount} methods)");
            put(Language.DE, "API listener {listenername} Anmeldung ({methodcount} Methoden)");
        }}), API_LISTENER_UNSUBSCRIBED(new HashMap<Language, String>() {{
            put(Language.EN, "API listener {listenername} unsubscribed");
            put(Language.DE, "API listener {listenername} Abmeldung");
        }}), API_LISTENER_THREW_ERROR(new HashMap<Language, String>() {{
            put(Language.EN, "DiscordSRV API Listener {listenername} threw an error");
            put(Language.DE, "DiscordSRV API Listener {listenername} erzeugte einen Fehler");
        }}), API_LISTENER_METHOD_NOT_ACCESSIBLE(new HashMap<Language, String>() {{
            put(Language.EN, "DiscordSRV API Listener {listenername} method {methodname} was inaccessible despite efforts to make it accessible");
            put(Language.DE, "DiscordSRV API Listener {listenername} Methode {methodname} war unzugänglich trotz der Bemühungen, es zugänglich zu machen");
        }}), HTTP_FAILED_TO_FETCH_URL(new HashMap<Language, String>() {{
            put(Language.EN, "Failed to fetch URL");
            put(Language.DE, "Fehler beim Abrufen der URL");
        }}), HTTP_FAILED_TO_DOWNLOAD_URL(new HashMap<Language, String>() {{
            put(Language.EN, "Failed to download URL");
            put(Language.DE, "Fehler beim Download von URL");
        }}), TOWNY_NOT_AUTOMATICALLY_ENABLING_CHANNEL_HOOKING(new HashMap<Language, String>() {{
            put(Language.EN, "Not automatically enabling hooking for TownyChat channels");
            put(Language.DE, "Automatisches Einklinken in TownyChat deaktiviert");
        }}), TOWNY_AUTOMATICALLY_ENABLED_LINKING_FOR_CHANNELS(new HashMap<Language, String>() {{
            put(Language.EN, "Automatically enabled hooking for {amountofchannels} TownyChat channels");
            put(Language.DE, "Automatisches Einklinken in {amountofchannels} TownyChat Chat-Kanäle");
        }}), TOWNY_AUTOMATICALLY_ENABLED_LINKING_FOR_NO_CHANNELS(new HashMap<Language, String>() {{
            put(Language.EN, "No TownyChat channels were automatically hooked. This might cause problems...");
            put(Language.DE, "Es konnte sich in keine TownyChat Chat-Kanäle automatisch eingeklinkt werden. Dies könnte eine Fehlerursache sein...");
        }}), PLUGIN_HOOK_ENABLING(new HashMap<Language, String>() {{
            put(Language.EN, "Enabling {plugin} hook");
            put(Language.DE, "Aktiviere {plugin} Verbindung");
        }}), PLUGIN_HOOKS_NOT_ENABLED(new HashMap<Language, String>() {{
            put(Language.EN, "No chat plugin hooks enabled");
            put(Language.DE, "Keine Pluginverbindungen aktiviert");
        }}), CHAT_CANCELLATION_DETECTOR_ENABLED(new HashMap<Language, String>() {{
            put(Language.EN, "Chat event cancellation detector has been enabled");
            put(Language.DE, "Chatevent-Abbruch-Detektor wurde aktiviert");
        }}), INVALID_CONFIG(new HashMap<Language, String>() {{
            put(Language.EN, "Invalid config.yml");
            put(Language.DE, "Ungültige config.yml");
        }}), FAILED_TO_CONNECT_TO_DISCORD(new HashMap<Language, String>() {{
            put(Language.EN, "DiscordSRV failed to connect to Discord. Reason");
            put(Language.DE, "Fehler beim Verbinden von DiscordSRV mit Discord. Grund");
        }}), BOT_NOT_IN_ANY_SERVERS(new HashMap<Language, String>() {{
            put(Language.EN, "The bot is not a part of any Discord servers. Follow the installation instructions");
            put(Language.DE, "Der Bot ist nicht Bestandteil irgendwelcher Discordserver. Folge den Installationsanweisungen");
        }}), NO_CHANNELS_LINKED(new HashMap<Language, String>() {{
            put(Language.EN, "No channels have been linked");
            put(Language.DE, "Es wurden keine Chat-Kanäle verbunden");
        }}), NO_CHANNELS_LINKED_NOR_CONSOLE(new HashMap<Language, String>() {{
            put(Language.EN, "No channels nor a console channel have been linked. Have you followed the installation instructions?");
            put(Language.DE, "Es wurden weder Chat-Kanäle, noch der Konsolenkanal verbunden. Bitte folge den Installationsanweisungen.");
        }}), CONSOLE_CHANNEL_ASSIGNED_TO_LINKED_CHANNEL(new HashMap<Language, String>() {{
            put(Language.EN, "The console channel was assigned to a channel that's being used for chat. Did you blindly copy/paste an ID into the channel ID config option?");
            put(Language.DE, "Der Konsolenkanal wurde mit einem Kanal verbunden, der auch für den Chat genutzt werden soll. Bitte korrigiere das und folge den Installationsanweisungen!");
        }}), ZPERMISSIONS_VAULT_REQUIRED(new HashMap<Language, String>() {{
            put(Language.EN, "Vault is not installed. It is needed for the group synchronization to work with zPermissions. Install Vault if you want this feature.");
            put(Language.DE, "Vault ist nicht installiert. Es wird gebraucht für Gruppensynchronisation mit zPermissions. Installiere Vault wenn du dieses Feature benötigst.");
        }}), CHAT(new HashMap<Language, String>() {{
            put(Language.EN, "Chat");
            put(Language.DE, "Chat");
        }}), ERROR_LOGGING_CONSOLE_ACTION(new HashMap<Language, String>() {{
            put(Language.EN, "Error logging console action to");
            put(Language.DE, "Fehler beim Loggen einer Konsolenaktion nach");
        }}), SILENT_JOIN(new HashMap<Language, String>() {{
            put(Language.EN, "Player {player} joined with silent joining permission, not sending a join message");
            put(Language.DE, "Spieler {player} hat den Server mit discord.silentjoin Berechtigung betreten, es wird keine Nachricht gesendet");
        }}), SILENT_QUIT(new HashMap<Language, String>() {{
            put(Language.EN, "Player {player} quit with silent quitting permission, not sending a quit message");
            put(Language.DE, "Spieler {player} hat den Server mit discord.silentquit Berechtigung verlassen, es wird keine Nachricht gesendet");
        }}), LINKED_ACCOUNTS_SAVE_SKIPPED(new HashMap<Language, String>() {{
            put(Language.EN, "Skipped saving linked accounts because there were none");
            put(Language.DE, "Überspringe Speicherung von verknüpften Accounts weil kein vorhanden sind");
        }}), LINKED_ACCOUNTS_SAVED(new HashMap<Language, String>() {{
            put(Language.EN, "Saved linked accounts in {ms}ms");
            put(Language.DE, "Speichern von verknüpften Accounts in {ms}ms");
        }}), LINKED_ACCOUNTS_SAVE_FAILED(new HashMap<Language, String>() {{
            put(Language.EN, "Failed saving linked accounts");
            put(Language.DE, "Fehler beim Speichern von verknüpften Accounts");
        }}), METRICS_SAVE_SKIPPED(new HashMap<Language, String>() {{
            put(Language.EN, "Skipped saving metrics because there were none");
            put(Language.DE, "Überspringe Speichern von Statistiken weil keine Vorhanden");
        }}), METRICS_SAVED(new HashMap<Language, String>() {{
            put(Language.EN, "Saved metrics in {ms}ms");
            put(Language.DE, "Speichern von Statistiken in {ms}ms");
        }}), METRICS_SAVE_FAILED(new HashMap<Language, String>() {{
            put(Language.EN, "Failed saving metrics");
            put(Language.DE, "Fehler beim Speichern von Statistiken");
        }}), NO_PERMISSIONS_MANAGEMENT_PLUGIN_DETECTED(new HashMap<Language, String>() {{
            put(Language.EN, "No supported permissions management plugin detected. Group synchronization will not work.");
            put(Language.DE, "Es wurde kein unterstütztes Berechtigungsplugin gefunden. Gruppensynchronisation außer Betrieb.");
        }}), FAILED_LOADING_PLUGIN(new HashMap<Language, String>() {{
            put(Language.EN, "Failed loading plugin");
            put(Language.DE, "Fehler beim Laden des Plugins");
        }}), GROUP_SYNCHRONIZATION_COULD_NOT_FIND_ROLE(new HashMap<Language, String>() {{
            put(Language.EN, "Could not find role with name \"{rolename}\" for use with group synchronization. Is the bot in the server?");
            put(Language.DE, "Es konnte keine Rolle mit dem Namen \"{rolename}\" für die Gruppensynchronisation gefunden werden. Befindet sich der Bot auf dem Server?");
        }}), UNKNOWN_CODE(new HashMap<Language, String>() {{
            put(Language.EN, "I don't know of such a code, try again.");
        }}), INVALID_CODE(new HashMap<Language, String>() {{
            put(Language.EN, "Are you sure that's your code? Link codes are 4 numbers long.");
        }});

        private final Map<Language, String> definitions;
        InternalMessage(Map<Language, String> definitions) {
            this.definitions = definitions;

            // warn about if a definition is missing any translations for messages
            for (Language language : Language.values())
                if (!definitions.containsKey(language))
                    DiscordSRV.debug("Language " + language.getName() + " missing from definitions for " + name());
        }

        @Override
        public String toString() {
            return definitions.get(userLanguage);
        }

    }

    /**
     * Messages external to DiscordSRV and thus can be customized in format.yml
     */
    public enum Message {

        BAN_DISCORD_TO_MINECRAFT("BanSynchronizationDiscordToMinecraftReason"),
        CHAT_CHANNEL_COMMAND_ERROR("DiscordChatChannelConsoleCommandNotifyErrorsFormat"),
        CHAT_CHANNEL_MESSAGE("ChatChannelHookMessageFormat"),
        CHAT_CHANNEL_TOPIC("ChannelTopicUpdaterChatChannelTopicFormat"),
        CHAT_CHANNEL_TOPIC_AT_SERVER_SHUTDOWN("ChannelTopicUpdaterChatChannelTopicAtServerShutdownFormat"),
        CHAT_TO_DISCORD("MinecraftChatToDiscordMessageFormat"),
        CHAT_TO_DISCORD_NO_PRIMARY_GROUP("MinecraftChatToDiscordMessageFormatNoPrimaryGroup"),
        CHAT_TO_MINECRAFT("DiscordToMinecraftChatMessageFormat"),
        CHAT_TO_MINECRAFT_ALL_ROLES_SEPARATOR("DiscordToMinecraftAllRolesSeparator"),
        CHAT_TO_MINECRAFT_NO_ROLE("DiscordToMinecraftChatMessageFormatNoRole"),
        CONSOLE_CHANNEL_LINE("DiscordConsoleChannelFormat"),
        CONSOLE_CHANNEL_TOPIC("ChannelTopicUpdaterConsoleChannelTopicFormat"),
        CONSOLE_CHANNEL_TOPIC_AT_SERVER_SHUTDOWN("ChannelTopicUpdaterConsoleChannelTopicAtServerShutdownFormat"),
        DISCORD_ACCOUNT_LINKED("DiscordAccountLinked"),
        DISCORD_COMMAND("DiscordCommandFormat"),
        MINECRAFT_ACCOUNT_LINKED("MinecraftAccountLinked"),
        ON_SUBSCRIBE("MinecraftSubscriptionMessagesOnSubscribe"),
        ON_UNSUBSCRIBE("MinecraftSubscriptionMessagesOnUnsubscribe"),
        PLAYER_ACHIEVEMENT("MinecraftPlayerAchievementMessagesFormat"),
        PLAYER_DEATH("MinecraftPlayerDeathMessageFormat"),
        PLAYER_JOIN("MinecraftPlayerJoinMessageFormat"),
        PLAYER_JOIN_FIRST_TIME("MinecraftPlayerFirstJoinMessageFormat"),
        PLAYER_LEAVE("MinecraftPlayerLeaveMessageFormat"),
        PLAYER_LIST_COMMAND("DiscordChatChannelListCommandFormatOnlinePlayers"),
        PLAYER_LIST_COMMAND_NO_PLAYERS("DiscordChatChannelListCommandFormatNoOnlinePlayers"),
        SERVER_SHUTDOWN_MESSAGE("DiscordChatChannelServerShutdownMessage"),
        SERVER_STARTUP_MESSAGE("DiscordChatChannelServerStartupMessage"),
        SERVER_WATCHDOG("ServerWatchdogMessage");

        @Getter private final String keyName;

        Message(String keyName) {
            this.keyName = keyName;
        }

        @Override
        public String toString() {
            return ChatColor.translateAlternateColorCodes('&', messages.get(this));
        }

    }

    @Getter private static final Map<Message, String> messages = new HashMap<>();
    @Getter private static final Yaml yaml = new Yaml();
    @Getter private static final Language userLanguage;
    static {
        String languageCode = System.getProperty("user.language").toLowerCase();
        switch (languageCode) {
            case "en": userLanguage = Language.EN; break;
            case "de": userLanguage = Language.DE; break;
            default:
                DiscordSRV.info("Unknown user language " + languageCode.toUpperCase() + ".\nIf you fluently speak " + languageCode.toUpperCase() + " as well as English, see the GitHub repo to translate it!");
                userLanguage = Language.EN;
                break;
        }

        saveConfig();
        saveMessages();
        reloadMessages();

        DiscordSRV.info(InternalMessage.LANGUAGE_INITIALIZED + userLanguage.getName());
    }

    private static void saveResource(String resource, File destination, boolean overwrite) {
        if (destination.exists() && !overwrite) return;

        try {
            FileUtils.copyInputStreamToFile(DiscordSRV.class.getResourceAsStream(resource), destination);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveConfig() {
        saveConfig(false);
    }
    public static void saveConfig(boolean overwrite) {
        File destination = DiscordSRV.getPlugin().getConfigFile();
        String resource = "/config/" + userLanguage.getCode() + ".yml";

        saveResource(resource, destination, overwrite);
    }

    public static void saveMessages() {
        saveMessages(false);
    }
    public static void saveMessages(boolean overwrite) {
        String resource = "/messages/" + userLanguage.getCode() + ".yml";
        File destination = DiscordSRV.getPlugin().getMessagesFile();

        saveResource(resource, destination, overwrite);
    }

    public static void reloadMessages() {
        if (!DiscordSRV.getPlugin().getMessagesFile().exists()) return;

        try {
            for (Map.Entry entry : (Set<Map.Entry>) yaml.loadAs(FileUtils.readFileToString(DiscordSRV.getPlugin().getMessagesFile(), Charset.defaultCharset()), Map.class).entrySet()) {
                //messages.put(Message.valueOf(String.valueOf(entry.getKey())), String.valueOf(entry.getValue()));
                for (Message message : Message.values()) {
                    if (message.getKeyName().equalsIgnoreCase((String) entry.getKey())) {
                        messages.put(message, (String) entry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            DiscordSRV.error("Failed loading " + DiscordSRV.getPlugin().getMessagesFile().getPath() + ": " + e.getMessage());

            File movedToFile = new File(DiscordSRV.getPlugin().getMessagesFile().getParent(), "messages-" + DiscordSRV.getPlugin().getRandom().nextInt(100) + ".yml");
            try { FileUtils.moveFile(DiscordSRV.getPlugin().getMessagesFile(), movedToFile); } catch (IOException ignored) {}
            saveMessages();
            DiscordSRV.error("A new messages.yml has been created and the erroneous one has been moved to " + movedToFile.getPath());
            reloadMessages();
        }
    }

}

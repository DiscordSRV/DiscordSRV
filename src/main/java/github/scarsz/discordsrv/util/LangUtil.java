/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2017 Austin Shapiro AKA Scarsz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
 * <p>Japanese translations by Ucchy</p>
 * <p>French translations by BrinDeNuage</p>
 * <p>Hungarian translations by montlikadani</p>
 */
public class LangUtil {

    public enum Language {

        EN("English"),
        FR("French"),
        DE("German"),
        JA("Japanese");
        HU("Hungarian");

        @Getter final String code;
        @Getter final String name;

        Language(String name) {
            this.code = name().toLowerCase();
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
            put(Language.FR,
                    "\n" +
                    "\n" +
                    "Vous essayez d'utiliser Discord SRV sur ASM 4. DiscordSRV a besoin de ASM 5 pour fonctionner.\n" +
                    "DiscordSRV ne fonctionne pas sans ASM 5. Vos librairies ne sont pas à jour.\n" +
                    "\n" +
                    "Instructions pour mettre à jour ASM 5:\n" +
                    "1. Allez sur le dossier {specialsourcefolder} du serveur\n" +
                    "2. Supprimez le fichier SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "3. Téléchargez le fichier v1.7.4 depuis http://central.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
                    "4. Copiez le fichier jar dans le dossier {specialsourcefolder} \n" +
                    "5. Renommez le fichier de la façon suivante SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "6. Redémarrez le serveur\n" +
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
            put(Language.JA,
                    "\n" +
                    "\n" +
                    "あなたは、DiscordSRV を ASM 4 で使用しようとしています。DiscordSRV を使用するには、ASM 5 が必要です。\n" +
                    "DiscordSRV は ASM 5 でないと正しく動作しません。サーバーソフトウエア開発者に、ライブラリが古くなっていることを教えてあげてください。\n" +
                    "\n" +
                    "ASM 5 へのアップデート手順：\n" +
                    "1. サーバーの {specialsourcefolder} フォルダーに移動します。\n" +
                    "2. SpecialSource-1.7-SNAPSHOT.jar ファイルを削除します。\n" +
                    "3. SpecialSource v1.7.4 を、次のURLからダウンロードします。http://central.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
                    "4. ダウンロードしたjarファイルを、{specialsourcefolder} フォルダーにコピーします。\n" +
                    "5. コピーしたファイルを、SpecialSource-1.7-SNAPSHOT.jar にリネームします。\n" +
                    "6. サーバーを起動します。\n" +
                    "\n" +
                    "\n");
            put(Language.HU,
                    "\n" +
                    "\n" +
                    "Megpróbálja használni a DiscordSRV-t az ASM 4-en. A DiscordSRV-nek az ASM 5 funkcióra van szüksége.\n" +
                    "A DiscordSRV nem fog működni az ASM nélkül. 5. Blokkolja a kiszolgálószoftver fejlesztoit, hogy elavult könyvtárak legyenek.\n" +
                    "\n" +
                    "Az ASM-hez való frissítésre vonatkozó utasítások 5:\n" +
                    "1. Navigáljon a {specialsourcefolder} a szerver mappája\n" +
                    "2. Törölje a SpecialSource-1.7-SNAPSHOT.jar jar fájlt\n" +
                    "3. A SpecialSource v1.7.4 letöltése a http://central.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
                    "4. Másolja a jar fájlt a {specialsourcefolder} a korábban már navigált szerver mappája\n" +
                    "5. Nevezze át az éppen telepített jar fájlt a SpecialSource-1.7-SNAPSHOT.jar fájlba\n" +
                    "6. Indítsa újra a kiszolgálót\n" +
                    "\n" +
                    "\n");
        }}), CONSOLE_FORWARDING_ASSIGNED_TO_CHANNEL(new HashMap<Language, String>() {{
            put(Language.EN, "Console forwarding assigned to channel");
            put(Language.FR, "Réacheminement de la console affecté au canal");
            put(Language.DE, "Konsolenausgabeweiterleitung aktiv");
            put(Language.JA, "コンソールフォワーディングがチャンネルに割り当てられました");
            put(Language.HU, "A konzol továbbítása a csatornához rendelt");
        }}), DONATOR_THANKS(new HashMap<Language, String>() {{
            put(Language.EN, "Thank you so much to these people for allowing DiscordSRV to grow to what it is");
            put(Language.FR, "Merci à toutes ces personnes qui ont fait grandir Discord SRV pour devenir ce qu'il est aujourd'hui");
            put(Language.DE, "Vielen Dank an die folgenden Leute, sie haben DiscordSRV zu dem gemacht, was es heute ist");
            put(Language.JA, "DiscordSRVは、下記の方々に開発のご支援をいただきました。誠にありがとうございます。");
            put(Language.HU, "Nagyon köszönöm ezeket az embereket, hogy lehetővé tette a DiscordSRV számára, hogy növekedjen ahhoz, ami");
        }}), FOUND_SERVER(new HashMap<Language, String>() {{
            put(Language.EN, "Found server");
            put(Language.FR, "Serveur trouvé");
            put(Language.DE, "Server wurde gefunden");
            put(Language.JA, "見つかったサーバー");
            put(Language.HU, "Szerver találva");
        }}), NOT_FORWARDING_CONSOLE_OUTPUT(new HashMap<Language, String>() {{
            put(Language.EN, "Console channel ID was invalid, not forwarding console output");
            put(Language.FR, "L'ID du channel de la console est faux, l'envoie des messages de la console ne sera pas effectué");
            put(Language.DE, "Konsolenkanal-ID ist ungültig, keine Konsolenausgabe Weiterleitung aktiv");
            put(Language.JA, "コンソールチャネルIDは無効であるため、コンソール転送は行われません");
            put(Language.HU, "A konzolcsatorna azonosítója érvénytelen, nem továbbította a konzol kimenetét");
        }}), PLUGIN_CANCELLED_CHAT_EVENT(new HashMap<Language, String>() {{
            put(Language.EN, "Plugin {plugin} cancelled AsyncPlayerChatEvent (author: {author} | message: {message})");
            put(Language.FR, "Plugin {plugin} AsyncPlayerChatEvent annulé (author: {author} | message: {message})");
            put(Language.DE, "Plugin {plugin} brach AsyncPlayerChatEvent ab (Author: {author} | Nachricht: {message})");
            put(Language.JA, "Plugin {plugin} が AsyncPlayerChatEvent を呼び出した (author: {author} | message: {message})");
            put(Language.HU, "A plugin {plugin} törölve AsyncPlayerChatEvent (szerző: {author} | üzenet: {message})");
        }}), SHUTDOWN_COMPLETED(new HashMap<Language, String>() {{
            put(Language.EN, "Shutdown completed in {ms}ms");
            put(Language.FR, "Arrêt effectué en {ms}ms");
            put(Language.DE, "Herunterfahren wurde abgeschlossen in {ms}ms");
            put(Language.JA, "{ms}ミリ秒でシャットダウンしました");
            put(Language.HU, "A leállítás befejeződött {ms}ms");
        }}), LANGUAGE_INITIALIZED(new HashMap<Language, String>() {{
            put(Language.EN, "Language initialized as ");
            put(Language.FR, "Langage initialisé à ");
            put(Language.DE, "Sprache initialisiert als ");
            put(Language.JA, "言語初期化完了: ");
            put(Language.HU, "Nyelv inicializálva ");
        }}), API_LISTENER_SUBSCRIBED(new HashMap<Language, String>() {{
            put(Language.EN, "API listener {listenername} subscribed ({methodcount} methods)");
            put(Language.FR, "API listener {listenername} associé à ({methodcount} methods)");
            put(Language.DE, "API listener {listenername} Anmeldung ({methodcount} Methoden)");
            put(Language.JA, "API listener {listenername} が購読を開始しました (メソッド数: {methodcount} )");
            put(Language.HU, "API listener {listenername} jegyzett ({methodcount} mód)");
        }}), API_LISTENER_UNSUBSCRIBED(new HashMap<Language, String>() {{
            put(Language.EN, "API listener {listenername} unsubscribed");
            put(Language.FR, "API listener {listenername} n'est plus associé");
            put(Language.DE, "API listener {listenername} Abmeldung");
            put(Language.JA, "API listener {listenername} が購読を終了しました");
            put(Language.HU, "API listener {listenername} leiratkozott");
        }}), API_LISTENER_THREW_ERROR(new HashMap<Language, String>() {{
            put(Language.EN, "DiscordSRV API Listener {listenername} threw an error");
            put(Language.FR, "DiscordSRV API Listener {listenername} a causé une erreur");
            put(Language.DE, "DiscordSRV API Listener {listenername} erzeugte einen Fehler");
            put(Language.JA, "DiscordSRV API Listener {listenername} でエラーが発生しました");
            put(Language.HU, "DiscordSRV API Listener {listenername} hibát vétett");
        }}), API_LISTENER_METHOD_NOT_ACCESSIBLE(new HashMap<Language, String>() {{
            put(Language.EN, "DiscordSRV API Listener {listenername} method {methodname} was inaccessible despite efforts to make it accessible");
            put(Language.FR, "DiscordSRV API Listener {listenername} méthode {methodname} est inaccessible malgré les efforts pour la rendre accessible");
            put(Language.DE, "DiscordSRV API Listener {listenername} Methode {methodname} war unzugänglich trotz der Bemühungen, es zugänglich zu machen");
            put(Language.JA, "DiscordSRV API Listener {listenername} の Method {methodname} は、アクセスすることができなくなりました");
            put(Language.HU, "DiscordSRV API Listener {listenername} mód {methodname} az elérhetővé tett erőfeszítések ellenére nem volt elérhető");
        }}), HTTP_FAILED_TO_FETCH_URL(new HashMap<Language, String>() {{
            put(Language.EN, "Failed to fetch URL");
            put(Language.FR, "Impossible de récuperer l'URL");
            put(Language.DE, "Fehler beim Abrufen der URL");
            put(Language.JA, "URLの取得に失敗しました");
            put(Language.HU, "URL lekérése sikertelen");
        }}), HTTP_FAILED_TO_DOWNLOAD_URL(new HashMap<Language, String>() {{
            put(Language.EN, "Failed to download URL");
            put(Language.FR, "Impossible de télécharger l'URL");
            put(Language.DE, "Fehler beim Download von URL");
            put(Language.JA, "URLからのダウンロードに失敗しました");
            put(Language.HU, "URL letöltése nem sikerült");
        }}), TOWNY_NOT_AUTOMATICALLY_ENABLING_CHANNEL_HOOKING(new HashMap<Language, String>() {{
            put(Language.EN, "Not automatically enabling hooking for TownyChat channels");
            put(Language.FR, "La compatibilité avec TownyChat n'est pas automatique");
            put(Language.DE, "Automatisches Einklinken in TownyChat deaktiviert");
            put(Language.JA, "TownyChat チャンネルへの接続を自動的に有効にしません");
            put(Language.HU, "Nem engedélyezi automatikusan a TownyChat csatornák összekapcsolását");
        }}), TOWNY_AUTOMATICALLY_ENABLED_LINKING_FOR_CHANNELS(new HashMap<Language, String>() {{
            put(Language.EN, "Automatically enabled hooking for {amountofchannels} TownyChat channels");
            put(Language.FR, "Accrochage automatique des {amountofchannels} channels de TownyChat");
            put(Language.DE, "Automatisches Einklinken in {amountofchannels} TownyChat Chat-Kanäle");
            put(Language.JA, "TownyChat チャンネル {amountofchannels} への接続を自動的に有効にしました");
            put(Language.HU, "Automatikusan engedélyezett horgolás {amountofchannels} TownyChat csatornák");
        }}), TOWNY_AUTOMATICALLY_ENABLED_LINKING_FOR_NO_CHANNELS(new HashMap<Language, String>() {{
            put(Language.EN, "No TownyChat channels were automatically hooked. This might cause problems...");
            put(Language.FR, "Aucun channels de Towny Chat n'ont été trouvé. Cela peut causer des problèmes...");
            put(Language.DE, "Es konnte sich in keine TownyChat Chat-Kanäle automatisch eingeklinkt werden. Dies könnte eine Fehlerursache sein...");
            put(Language.JA, "TownyChatチャンネルは自動的に接続されませんでした。これは問題を引き起こす可能性があります...");
            put(Language.HU, "A TownyChat csatorna nem volt automatikusan akasztva. Ez problémákat okozhat...");
        }}), PLUGIN_HOOK_ENABLING(new HashMap<Language, String>() {{
            put(Language.EN, "Enabling {plugin} hook");
            put(Language.FR, "Activation de l'accrochage du plugin {plugin}");
            put(Language.DE, "Aktiviere {plugin} Verbindung");
            put(Language.JA, "{plugin} の接続を有効にしました");
            put(Language.HU, "{plugin} Engedélyezés");
        }}), PLUGIN_HOOKS_NOT_ENABLED(new HashMap<Language, String>() {{
            put(Language.EN, "No chat plugin hooks enabled");
            put(Language.FR, "Aucun accrochage de plugin activé");
            put(Language.DE, "Keine Pluginverbindungen aktiviert");
            put(Language.JA, "チャットプラグインへの接続は一つもありません");
            put(Language.HU, "Nincs engedélyezve a csevegő plugin horgok");
        }}), CHAT_CANCELLATION_DETECTOR_ENABLED(new HashMap<Language, String>() {{
            put(Language.EN, "Chat event cancellation detector has been enabled");
            put(Language.FR, "Détecteur d'annulation d'événement de chat vient d'être activé");
            put(Language.DE, "Chatevent-Abbruch-Detektor wurde aktiviert");
            put(Language.JA, "チャットイベントキャンセル検出機能が有効になっています");
            put(Language.HU, "A csevegés esemény-törlési érzékelője engedélyezve volt");
        }}), INVALID_CONFIG(new HashMap<Language, String>() {{
            put(Language.EN, "Invalid config.yml");
            put(Language.FR, "config.yml invalide");
            put(Language.DE, "Ungültige config.yml");
            put(Language.JA, "config.ymlが不正です");
            put(Language.HU, "Érvénytelen config.yml");
        }}), FAILED_TO_CONNECT_TO_DISCORD(new HashMap<Language, String>() {{
            put(Language.EN, "DiscordSRV failed to connect to Discord. Reason");
            put(Language.FR, "DiscordSRV n'a pas réussi à se connecter à Discord. Raison");
            put(Language.DE, "Fehler beim Verbinden von DiscordSRV mit Discord. Grund");
            put(Language.JA, "DiscordSRV は Discord への接続に失敗しました。理由");
            put(Language.HU, "DiscordSRV nem sikerült csatlakozni Discord. Oka");
        }}), BOT_NOT_IN_ANY_SERVERS(new HashMap<Language, String>() {{
            put(Language.EN, "The bot is not a part of any Discord servers. Follow the installation instructions");
            put(Language.FR, "Le bot ne fait partie d'aucun serveur. Suivez les instructions d'installation");
            put(Language.DE, "Der Bot ist nicht Bestandteil irgendwelcher Discordserver. Folge den Installationsanweisungen");
            put(Language.JA, "このBotはどのDiscordサーバーにも所属していません。インストール手順に従ってください");
            put(Language.HU, "A bot nem része a Discord szervereknek. Kövesse a telepítési utasításokat");
        }}), NO_CHANNELS_LINKED(new HashMap<Language, String>() {{
            put(Language.EN, "No channels have been linked");
            put(Language.FR, "Aucun channel n'a été lié");
            put(Language.DE, "Es wurden keine Chat-Kanäle verbunden");
            put(Language.JA, "チャンネルに接続していません");
            put(Language.HU, "Nincs csatolva csatornák");
        }}), NO_CHANNELS_LINKED_NOR_CONSOLE(new HashMap<Language, String>() {{
            put(Language.EN, "No channels nor a console channel have been linked. Have you followed the installation instructions?");
            put(Language.FR, "Aucun channel ou console n'ont été lié. Avez vous suivez les instructions d'installation ?");
            put(Language.DE, "Es wurden weder Chat-Kanäle, noch der Konsolenkanal verbunden. Bitte folge den Installationsanweisungen.");
            put(Language.JA, "チャネルにもコンソールチャネルにも接続されていません。インストール手順に従っていますか？");
            put(Language.HU, "Nincsenek csatornák és konzolcsatornák sem. Követted a telepítési utasításokat?");
        }}), CONSOLE_CHANNEL_ASSIGNED_TO_LINKED_CHANNEL(new HashMap<Language, String>() {{
            put(Language.EN, "The console channel was assigned to a channel that's being used for chat. Did you blindly copy/paste an ID into the channel ID config option?");
            put(Language.FR, "Le channel de la console à été assigné à un channel utilisé pour le tchat. Avez vous copié aveuglement l'ID d'un channel");
            put(Language.DE, "Der Konsolenkanal wurde mit einem Kanal verbunden, der auch für den Chat genutzt werden soll. Bitte korrigiere das und folge den Installationsanweisungen!");
            put(Language.JA, "コンソールチャンネルは、チャットに使用されているチャンネルと同じものが指定されています。IDをチャンネルID設定オプションにそのままコピペしていませんか？");
            put(Language.HU, "A konzolcsatornát egy csatorna csatornájához rendelték. Vakálatlanul másolta/illessze be az azonosítót a csatornaazonosító beállításba?");
        }}), ZPERMISSIONS_VAULT_REQUIRED(new HashMap<Language, String>() {{
            put(Language.EN, "Vault is not installed. It is needed for the group synchronization to work with zPermissions. Install Vault if you want this feature.");
            put(Language.FR, "Vault n'est pas installé. Ce plugin est requis pour la synchronisation des groupes avec zPermissions. Installez Vault si vous souhaitez cette fonctionnalité.");
            put(Language.DE, "Vault ist nicht installiert. Es wird gebraucht für Gruppensynchronisation mit zPermissions. Installiere Vault wenn du dieses Feature benötigst.");
            put(Language.JA, "Vaultがインストールされていません。グループをzPermissionsと同期するために必要です。この機能を使用するには、Vaultをインストールしてください。");
            put(Language.HU, "Vault nincs telepítve. Szükséges a csoportszinkronizáláshoz a zPermissions-kkel való együttműködéshez. Szerkessze a Vault telepítését, ha ezt a funkciót szeretné használni.");
        }}), CHAT(new HashMap<Language, String>() {{
            put(Language.EN, "Chat");
            put(Language.FR, "Tchat");
            put(Language.DE, "Chat");
            put(Language.JA, "チャット");
            put(Language.HU, "Chat");
        }}), ERROR_LOGGING_CONSOLE_ACTION(new HashMap<Language, String>() {{
            put(Language.EN, "Error logging console action to");
            put(Language.FR, "Erreur lors de la journalisation de l'action de la console");
            put(Language.DE, "Fehler beim Loggen einer Konsolenaktion nach");
            put(Language.JA, "動作記録失敗");
            put(Language.HU, "A konzolművelet naplózásának hibája");
        }}), SILENT_JOIN(new HashMap<Language, String>() {{
            put(Language.EN, "Player {player} joined with silent joining permission, not sending a join message");
            put(Language.FR, "Le joueur {player} a rejoint le jeu avec une permission de silence lors de la connexion.");
            put(Language.DE, "Spieler {player} hat den Server mit discord.silentjoin Berechtigung betreten, es wird keine Nachricht gesendet");
            put(Language.JA, "プレイヤー {player} は discord.silentjoin の権限があるので、サーバー参加メッセージが送信されません");
            put(Language.HU, "{player} játékos csendes csatlakozási engedéllyel csatlakozott, és nem küldtek egy csatlakozási üzenetet");
        }}), SILENT_QUIT(new HashMap<Language, String>() {{
            put(Language.EN, "Player {player} quit with silent quitting permission, not sending a quit message");
            put(Language.FR, "Le joueur {player} a quitté le jeu avec une permission de silence lors de le déconnexion.");
            put(Language.DE, "Spieler {player} hat den Server mit discord.silentquit Berechtigung verlassen, es wird keine Nachricht gesendet");
            put(Language.JA, "プレイヤー {player} は discord.silentquit の権限があるので、サーバー退出メッセージが送信されません");
            put(Language.HU, "{player} játékos csendesen kilépett a kilépés engedélyével, és ne küldjön egy kilépési üzenetet");
        }}), LINKED_ACCOUNTS_SAVE_SKIPPED(new HashMap<Language, String>() {{
            put(Language.EN, "Skipped saving linked accounts because there were none");
            put(Language.FR, "Sauvegarde des comptes liés suspendue parce qu'il n'y en avait aucun");
            put(Language.DE, "Überspringe Speicherung von verknüpften Accounts weil kein vorhanden sind");
            put(Language.JA, "リンクされたアカウントが無いので、保存をスキップしました");
            put(Language.HU, "Ugrás a megtakarított kapcsolódó fiókokra, mert nincs");
        }}), LINKED_ACCOUNTS_SAVED(new HashMap<Language, String>() {{
            put(Language.EN, "Saved linked accounts in {ms}ms");
            put(Language.FR, "Sauvegarde des comptes liés en {ms}ms");
            put(Language.DE, "Speichern von verknüpften Accounts in {ms}ms");
            put(Language.JA, "{ms}ミリ秒でリンクされたアカウントを保存しました");
            put(Language.HU, "Mentett kapcsolt fiókok a következőben: {ms}ms");
        }}), LINKED_ACCOUNTS_SAVE_FAILED(new HashMap<Language, String>() {{
            put(Language.EN, "Failed saving linked accounts");
            put(Language.FR, "Erreur lors de la sauvegarde des comptes liés");
            put(Language.DE, "Fehler beim Speichern von verknüpften Accounts");
            put(Language.JA, "リンクされたアカウントの保存に失敗しました");
            put(Language.HU, "Nem sikerült a kapcsolódó fiókok mentése");
        }}), METRICS_SAVE_SKIPPED(new HashMap<Language, String>() {{
            put(Language.EN, "Skipped saving metrics because there were none");
            put(Language.FR, "Sauvegarde de métrics suspendue car il y en a aucun");
            put(Language.DE, "Überspringe Speichern von Statistiken weil keine Vorhanden");
            put(Language.JA, "メトリクスが無いので、保存をスキップしました");
            put(Language.HU, "Hiányzott a mentési metrics, mert nincsenek");
        }}), METRICS_SAVED(new HashMap<Language, String>() {{
            put(Language.EN, "Saved metrics in {ms}ms");
            put(Language.FR, "Sauvegarde de metrics en {ms}ms");
            put(Language.DE, "Speichern von Statistiken in {ms}ms");
            put(Language.JA, "{ms}ミリ秒でメトリクスを保存しました");
            put(Language.HU, "A metrics mentve {ms}ms");
        }}), METRICS_SAVE_FAILED(new HashMap<Language, String>() {{
            put(Language.EN, "Failed saving metrics");
            put(Language.FR, "Erreur lors de la sauvegarde de metrics");
            put(Language.DE, "Fehler beim Speichern von Statistiken");
            put(Language.JA, "メトリクスの保存に失敗しました");
            put(Language.HU, "A metrics mentése sikertelen");
        }}), FAILED_LOADING_PLUGIN(new HashMap<Language, String>() {{
            put(Language.EN, "Failed loading plugin");
            put(Language.FR, "Erreur lors du chargement du plugin");
            put(Language.DE, "Fehler beim Laden des Plugins");
            put(Language.JA, "プラグインの起動に失敗しました");
            put(Language.HU, "A plugin betöltése sikertelen");
        }}), GROUP_SYNCHRONIZATION_COULD_NOT_FIND_ROLE(new HashMap<Language, String>() {{
            put(Language.EN, "Could not find role id {rolename} for use with group synchronization. Is the bot in the server?");
            put(Language.FR, "Impossible de trouver le rôle {rolename} lors de la synchronisation des groupes.Le bot est il sur le serveur ?");
            put(Language.DE, "Konnte keine Rolle mit id {rolename} für gruppensynchronisierung finden. Ist der Bot auf dem Server?");
            put(Language.JA, "グループを同期させるために、ID「{rolename}」のロールを見つけることができませんでした。 Botはサーバ上にありますか？");
            put(Language.HU, "Nem találtam szerepazonosságot {rolename} a csoportszinkronizáláshoz. A bot a kiszolgálón?");
        }}), UNKNOWN_CODE(new HashMap<Language, String>() {{
            put(Language.EN, "I don't know of such a code, try again.");
            put(Language.FR, "Je ne connais pas un tel code, réessayez");
            put(Language.DE, "Diesen Code kenne ich nicht, bitte versuche es erneut.");
            put(Language.JA, "そのようなコードは知りません。もう一度やり直してください。");
            put(Language.HU, "Nem tudok ilyen kódról, próbáld újra.");
        }}), INVALID_CODE(new HashMap<Language, String>() {{
            put(Language.EN, "Are you sure that's your code? Link codes are 4 numbers long.");
            put(Language.FR, "Êtes vous sûr qu'il s'agit du bon code ? Les codes possèdent quatre chiffres.");
            put(Language.DE, "Bist du sicher, dass dies dein Code ist? Link-Codes bestehen aus 4 Zahlen.");
            put(Language.JA, "それがあなたのコードで正しいですか？リンクコードは4文字の数字です。");
            put(Language.HU, "Biztos, hogy ez a kód? A linkek kódja 4 szám.");
        }}), NO_MESSAGE_GIVEN_TO_BROADCAST(new HashMap<Language, String>() {{
            put(Language.EN, "No language given to broadcast");
            put(Language.FR, "Aucune langue donnée à diffuser");
            put(Language.DE, "Keine Sprache für Broadcast angegeben");
            put(Language.JA, "ブロードキャストするメッセージが指定されていません。");
            put(Language.HU, "Nincs olyan nyelv, amelyet közvetített");
        }}), UNABLE_TO_LINK_ACCOUNTS_RIGHT_NOW(new HashMap<Language, String>() {{
            put(Language.EN, "Currently unable to link accounts due to an internal error. Contact your server administration team.");
            put(Language.FR, "Impossible de lier votre compte à cause d'une erreur. Merci de contacter l'équipe de votre serveur.");
            put(Language.DE, "Fehler beim Verbinden der Accounts wegen eines internen Fehlers. Bitte melde dies dem Serverteam.");
            put(Language.JA, "現在、内部エラーのためにアカウントをリンクできません。サーバー管理チームに連絡してください。");
            put(Language.HU, "Jelenleg nem tud összekapcsolni fiókokat belső hiba miatt. Lépjen kapcsolatba a szerver adminisztrációs csapatával.");
        }}), NO_PERMISSION(new HashMap<Language, String>() {{
            put(Language.EN, "You do not have permission to perform this command.");
            put(Language.FR, "Vous n'avez pas accès à cette commande.");
            put(Language.DE, "Du hast keine Berechtigung diesen Befehl auszuführen.");
            put(Language.JA, "あなたはこのコマンドを実行する権限がありません。");
            put(Language.HU, "Nincs jogod ennek a parancsnak a végrehajtására.");
        }}), PLAYER_ONLY_COMMAND(new HashMap<Language, String>() {{
            put(Language.EN, "Only players can execute this command.");
            put(Language.FR, "Seuls les joueurs peuvent effectuer cette commande.");
            put(Language.DE, "Nur Spieler können diesen Befehl ausführen.");
            put(Language.JA, "ゲーム内プレイヤーのみがこのコマンドを実行することができます。");
            put(Language.HU, "Csak a játékosok hajthatják végre ezt a parancsot.");
        }}), COMMAND_DOESNT_EXIST(new HashMap<Language, String>() {{
            put(Language.EN, "That command doesn't exist!");
            put(Language.FR, "Cette commande n'existe pas !");
            put(Language.DE, "Dieser Befehl existiert nicht!");
            put(Language.JA, "指定されたコマンドは存在しません！");
            put(Language.HU, "Ez a parancs nem létezik!");
        }}), RELOADED(new HashMap<Language, String>() {{
            put(Language.EN, "The DiscordSRV config & lang have been reloaded.");
            put(Language.FR, "La configuration et les fichiers de langage de DiscordSRV ont été rechargé.");
            put(Language.DE, "Die DiscordSRV Konfiguration und Sprachdatei wurden neu eingelesen.");
            put(Language.JA, "DiscordSRVの設定と言語が再読込されました。");
            put(Language.HU, "A DiscordSRV config & lang újratöltve.");
        }}), UNLINK_SUCCESS(new HashMap<Language, String>() {{
            put(Language.EN, "Your Minecraft account is no longer associated with {name}.");
            put(Language.FR, "Votre compte Minecraft n'est plus associé à {name}.");
            put(Language.DE, "Dein Minecraft-Account ist nicht länger verbunden mit {name}.");
            put(Language.JA, "あなたのMinecraftアカウントは、{name}とのリンクが解除されました。");
            put(Language.HU, "A Minecraft-fiókodat már nem társították {name}.");
        }}), LINK_FAIL_NOT_ASSOCIATED_WITH_AN_ACCOUNT(new HashMap<Language, String>() {{
            put(Language.EN, "Your Minecraft account isn't associated with a Discord account.");
            put(Language.FR, "Votre compte Minecraft n'est pas associé à un compte Discord.");
            put(Language.DE, "Dein Minecraft-Account ist mit keinem Discord-Account verbunden.");
            put(Language.JA, "あなたのMinecraftアカウントはDiscordアカウントにリンクされていません。");
            put(Language.HU, "A Minecraft-fiókod nem társul a Discord fiókhoz.");
        }}), LINKED_SUCCESS(new HashMap<Language, String>() {{
            put(Language.EN, "Your Minecraft account is associated with {name}.");
            put(Language.FR, "Votre compte Minecraft est associé à {name}.");
            put(Language.DE, "Dein Minecraft-Account ist verbunden mit {name}.");
            put(Language.JA, "あなたのMinecraftアカウントは{name}にリンクされました。");
            put(Language.HU, "Az Minecraft fiókod társítva van {name}.");
        }}), LINKED_NOBODY_FOUND(new HashMap<Language, String>() {{
            put(Language.EN, "Nobody found with Discord ID/Discord name/Minecraft name/Minecraft UUID matching \"{target}\" to look up.");
            put(Language.FR, "Aucune personne ne correspond à l'ID Discord, le nom Minecraft ou un UUID \"{target}\".");
            put(Language.DE, "Niemand gefunden mit der Discord-ID/Discord-Name/Minecraft-Name/Minecraft-UUID \"{target}\".");
            put(Language.JA, "\"{target}\" と一致するものを Discord-ID/Discord-Name/Minecraft-Name/Minecraft-UUID から探しましたが、何も見つかりませんでした。");
            put(Language.HU, "Senki nem találta meg a Disord ID/Discord név/Minecraft név/Minecraft UUID illesztés \"{target}\" felnézni.");
        }}), LINKED_ACCOUNT_REQUIRED(new HashMap<Language, String>() {{
            put(Language.EN, "You attempted to say the following message to the game chat but this server requires that you have your Minecraft account linked to your Discord account. Link it in-game by typing `/discord link`.\n```{message}```");
            put(Language.FR, "Le message suivant n'a pas pu être envoyé sur le jeu car votre compte Minecraft doit être lié à votre compte Discord. Liez votre compte depuis Minecraft en tapant `/discord link`.\n```{message}```");
            put(Language.DE, "Du hast versucht die folgende Nachricht im Spielchat zu senden aber dieser Server verlangt, dass du deinen Minecraft-Account mit deinem Discord-Account verbinden musst. Verbinde sie, indem du im Spiel den Befehl `/discord link` eingibst.\n```{message}```");
            put(Language.JA, "ゲームチャットに以下のメッセージを表示しようとしましたが、このサーバーではあなたのMinecraftアカウントをDiscordアカウントにリンクさせる必要があります。リンクさせるには、ゲーム内で `/discord link` を実行してください。\n```{message}```");
            put(Language.HU, "Megpróbáltad megmondani a következő üzenetet a játékcsatornának, de ez a kiszolgáló megköveteli, hogy a Minecraft fiókod kapcsolódjon a Discord fiókhoz. Csatlakoztassa a játékba a gépeléssel `/discord link`.\n```{message}```");
        }}), ACCOUNT_ALREADY_LINKED(new HashMap<Language, String>() {{
            put(Language.EN, "Your Minecraft account is already associated with a Discord account. Should you have permission to, you can unlink your account with /discord unlink.");
            put(Language.FR, "Votre compte Minecraft est déjà associé à un compte Discord. Si vous avez la permission vous pouvez utiliser la commande /discord unlink pour retirer votre compte Discord.");
            put(Language.DE, "Ihr Minecraft-Konto ist bereits mit einem Discord-Konto verknüpft. Sollten Sie die Erlaubnis haben, können Sie Ihre Konto-Verknüpfung mit /discord unlink aufheben.");
            put(Language.JA, "あなたのMinecraftアカウントはすでにDiscordアカウントに関連付けられています。 もしあなたがパーミッションを持っていれば、/discord unlinkを実行して2つのリンクを解除することができます。");
            put(Language.HU, "A Minecraft-fiókod már társították a Discord fiókkal. Ha engedélyed van, kapcsolódhat a fiókodhoz /discord unlink.");
        }});

        @Getter private final Map<Language, String> definitions;
        InternalMessage(Map<Language, String> definitions) {
            this.definitions = definitions;

            // warn about if a definition is missing any translations for messages
            for (Language language : Language.values())
                if (!definitions.containsKey(language))
                    DiscordSRV.debug("Language " + language.getName() + " missing from definitions for " + name());
        }

        @Override
        public String toString() {
            return definitions.getOrDefault(userLanguage, definitions.get(Language.EN));
        }

    }

    /**
     * Messages external to DiscordSRV and thus can be customized in messages.yml
     */
    public enum Message {

        BAN_DISCORD_TO_MINECRAFT("BanSynchronizationDiscordToMinecraftReason", true),
        CHAT_CHANNEL_COMMAND_ERROR("DiscordChatChannelConsoleCommandNotifyErrorsFormat", false),
        CHAT_CHANNEL_MESSAGE("ChatChannelHookMessageFormat", true),
        CHAT_CHANNEL_TOPIC("ChannelTopicUpdaterChatChannelTopicFormat", false),
        CHAT_CHANNEL_TOPIC_AT_SERVER_SHUTDOWN("ChannelTopicUpdaterChatChannelTopicAtServerShutdownFormat", false),
        CHAT_TO_DISCORD("MinecraftChatToDiscordMessageFormat", false),
        CHAT_TO_DISCORD_NO_PRIMARY_GROUP("MinecraftChatToDiscordMessageFormatNoPrimaryGroup", false),
        CHAT_TO_MINECRAFT("DiscordToMinecraftChatMessageFormat", true),
        CHAT_TO_MINECRAFT_ALL_ROLES_SEPARATOR("DiscordToMinecraftAllRolesSeparator", true),
        CHAT_TO_MINECRAFT_NO_ROLE("DiscordToMinecraftChatMessageFormatNoRole", true),
        CODE_GENERATED("CodeGenerated", true),
        CONSOLE_CHANNEL_LINE("DiscordConsoleChannelFormat", false),
        CONSOLE_CHANNEL_TOPIC("ChannelTopicUpdaterConsoleChannelTopicFormat", false),
        CONSOLE_CHANNEL_TOPIC_AT_SERVER_SHUTDOWN("ChannelTopicUpdaterConsoleChannelTopicAtServerShutdownFormat", false),
        DISCORD_ACCOUNT_LINKED("DiscordAccountLinked", false),
        DISCORD_COMMAND("DiscordCommandFormat", true),
        MINECRAFT_ACCOUNT_LINKED("MinecraftAccountLinked", true),
        PLAYER_ACHIEVEMENT("MinecraftPlayerAchievementMessagesFormat", false),
        PLAYER_DEATH("MinecraftPlayerDeathMessageFormat", false),
        PLAYER_JOIN("MinecraftPlayerJoinMessageFormat", false),
        PLAYER_JOIN_FIRST_TIME("MinecraftPlayerFirstJoinMessageFormat", false),
        PLAYER_LEAVE("MinecraftPlayerLeaveMessageFormat", false),
        PLAYER_LIST_COMMAND("DiscordChatChannelListCommandFormatOnlinePlayers", false),
        PLAYER_LIST_COMMAND_NO_PLAYERS("DiscordChatChannelListCommandFormatNoOnlinePlayers", false),
        SERVER_SHUTDOWN_MESSAGE("DiscordChatChannelServerShutdownMessage", false),
        SERVER_STARTUP_MESSAGE("DiscordChatChannelServerStartupMessage", false),
        SERVER_WATCHDOG("ServerWatchdogMessage", false);

        @Getter private final String keyName;
        @Getter private final boolean translateColors;

        Message(String keyName, boolean translateColors) {
            this.keyName = keyName;
            this.translateColors = translateColors;
        }

        @Override
        public String toString() {
            String message = messages.getOrDefault(this, "");

            return translateColors
                    ? ChatColor.translateAlternateColorCodes('&', message)
                    : message
            ;
        }

    }

    @Getter private static final Map<Message, String> messages = new HashMap<>();
    @Getter private static final Yaml yaml = new Yaml();
    @Getter private static Language userLanguage;
    static {
        String languageCode = System.getProperty("user.language").toUpperCase();

        String forcedLanguage = DiscordSRV.config().getString("ForcedLanguage");
        if (StringUtils.isNotBlank(forcedLanguage) && !forcedLanguage.equalsIgnoreCase("none")) {
            if (forcedLanguage.length() == 2) {
                languageCode = forcedLanguage.toUpperCase();
            } else {
                for (Language language : Language.values()) {
                    if (language.getName().equalsIgnoreCase(forcedLanguage)) {
                        languageCode = language.getCode();
                    }
                }
            }
        }

        try {
            userLanguage = Language.valueOf(languageCode);
        } catch (Exception e) {
            userLanguage = Language.EN;

            DiscordSRV.info("Unknown user language " + languageCode.toUpperCase() + ".");
            DiscordSRV.info("If you fluently speak " + languageCode.toUpperCase() + " as well as English, see the GitHub repo to translate it!");
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
            for (Map.Entry entry : (Set<Map.Entry>) yaml.loadAs(FileUtils.readFileToString(DiscordSRV.getPlugin().getMessagesFile(), Charset.forName("UTF-8")), Map.class).entrySet()) {
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

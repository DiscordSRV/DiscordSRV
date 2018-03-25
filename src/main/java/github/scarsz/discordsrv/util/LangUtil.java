/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2018 Austin "Scarsz" Shapiro
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
 * <p>Korean translations by Alex4386 (with MintNetwork)</p>
 * <p>Dutch translations by Mr Ceasar</p>
 * <p>Spanish translations by ZxFrankxZ</p>
 */
public class LangUtil {

    public enum Language {

        EN("English"),
        FR("French"),
        DE("German"),
        JA("Japanese"),
        KO("Korean"),
        NL("Dutch"),
        ES("Spanish"),
        RU("Russian");

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
           put(Language.KO,
                    "\n" +
                    "\n" +
                    "DiscordSRV를 ASM 4에서 구동 중 입니다.. DiscordSRV는 ASM 5 이상 버전에서 작동합니다.\n" +
                    "DiscordSRV는 ASM 5없이는 작동 할 수 없습니다. 구식 라이브러리를 써서 만든 서버 소프트웨어 개발자 한테 따지세요.\n" +
                    "\n" +
                    "ASM 5로 업데이트 하는 방법:\n" +
                    "1. 서버의 {specialsourcefolder} 폴더로 들어갑니다.\n" +
                    "2. SpecialSource-1.7-SNAPSHOT.jar 파일을 삭제 합니다.\n" +
                    "3. SpecialSource v1.7.4를 http://central.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar 에서 다운로드 받습니다.\n" +
                    "4. {specialsourcefolder}로 3에서 다운로드 받은 파일을 복사합니다." + 
                    "5. 4에서 복사한 파일의 이름을 SpecialSource-1.7-SNAPSHOT.jar로 바꿉니다.\n" +
                    "6. 서버를 재부팅 합니다.\n" +
                    "\n" +
                    "\n");
            put(Language.NL,
                    "\n" +
                    "\n" +
                    "Je probeerd DiscordSRV te gebruiken op ASM 4. DiscordSRV heeft ASM 5 nodig om te functioneren.\n" +
                    "DiscordSRV WERKT NIET zonder ASM 5. Geef je server software's developers de schuld maar voor het hebben van outdated libraries.\n" +
                    "\n" +
                    "Instructies voor het updaten naar ASM 5:\n" +
                    "1. Ga naar de {specialsourcefolder} folder van je server.\n" +
                    "2. Verwijder de SpecialSource-1.7-SNAPSHOT.jar jar file.\n" +
                    "3. Download SpecialSource v1.7.4 vanaf http://central.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
                    "4. Kopieer de jar file naar de {specialsourcefolder} folder van je server waar je mee bezig bent.\n" +
                    "5. Verander de naam van de jar file die je hebt gekopieerd naar SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "6. Herstart je server.\n" +
                    "\n" +
                    "\n");
            put(Language.ES,
                    "\n" +
                    "\n" +
                    "Estas intentando usar DiscordSRV en ASM 4. DiscordSRV necesita ASM 5 para funcionar.\n" +
                    "DiscordSRV NO FUNCIONARÁ sin ASM 5. Informe al desarrollador del software del servidor que la biblioteca no está actualizada.\n" +
                    "\n" +
                    "Instrucciones para actualizar a ASM 5:\n" +
                    "1. Navegue a la carpeta {specialsourcefolder} de tu servidor\n" +
                    "2. Elimina el archivo jar de SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "3. Descarga SpecialSource v1.7.4 desde http://central.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
                    "4. Copia el archivo jar en la carpeta {specialsourcefolder} folder del servidor al que navegaste anteriormente\n" +
                    "5. Renombra el archivo jar que acaba de copiar a: SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "6. Reinicia el servidor\n" +
            put(Language.RU,
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
        }}), CONSOLE_FORWARDING_ASSIGNED_TO_CHANNEL(new HashMap<Language, String>() {{
            put(Language.EN, "Console forwarding assigned to channel");
            put(Language.FR, "Réacheminement de la console affecté au canal");
            put(Language.DE, "Konsolenausgabeweiterleitung aktiv");
            put(Language.JA, "コンソールフォワーディングがチャンネルに割り当てられました");
            put(Language.KO, "콘솔포워딩이 채널에 설정되었습니다");
            put(Language.NL, "Console versturen verbonden aan kanaal");
            put(Language.ES, "Enviar la consola al canal asignado");
            put(Language.RU, "Console forwarding assigned to channel");

        }}), DONATOR_THANKS(new HashMap<Language, String>() {{
            put(Language.EN, "Thank you so much to these people for allowing DiscordSRV to grow to what it is");
            put(Language.FR, "Merci à toutes ces personnes qui ont fait grandir Discord SRV pour devenir ce qu'il est aujourd'hui");
            put(Language.DE, "Vielen Dank an die folgenden Leute, sie haben DiscordSRV zu dem gemacht, was es heute ist");
            put(Language.JA, "DiscordSRVは、下記の方々に開発のご支援をいただきました。誠にありがとうございます。");
            put(Language.KO, "DiscordSRV는 아래의 분들 없이는 성장할 수 없었을 것 입니다. 감사합니다!");
            put(Language.NL, "Ontzettend bedankt aan alle donoren voor het laten groeien van DiscordSRV tot waar het nu is!");
            put(Language.ES, "Muchas gracias a estas personas por permitir que DiscordSRV crezca a lo que es actualmente");
            put(Language.RU, "Thank you so much to these people for allowing DiscordSRV to grow to what it is");

        }}), FOUND_SERVER(new HashMap<Language, String>() {{
            put(Language.EN, "Found server");
            put(Language.FR, "Serveur trouvé");
            put(Language.DE, "Server wurde gefunden");
            put(Language.JA, "見つかったサーバー");
            put(Language.KO, "서버를 찾았습니다");
            put(Language.NL, "Server gevonden");
            put(Language.ES, "Servidor encontrado");
            put(Language.RU, "Found server");

        }}), NOT_FORWARDING_CONSOLE_OUTPUT(new HashMap<Language, String>() {{
            put(Language.EN, "Console channel ID was invalid, not forwarding console output");
            put(Language.FR, "L'ID du channel de la console est faux, l'envoie des messages de la console ne sera pas effectué");
            put(Language.DE, "Konsolenkanal-ID ist ungültig, keine Konsolenausgabe Weiterleitung aktiv");
            put(Language.JA, "コンソールチャネルIDは無効であるため、コンソール転送は行われません");
            put(Language.KO, "콘솔 채널 ID가 올바르지 않습니다. 콘솔 메세지를 채널로 보내지 않습니다.");
            put(Language.NL, "Console kanaal ID is ongeldig, de console wordt niet verzonden");
            put(Language.ES, "La ID del canal de la Consola no es valido, no se enviara ningun mensaje de la consola");
            put(Language.RU, "Console channel ID was invalid, not forwarding console output");

        }}), PLUGIN_CANCELLED_CHAT_EVENT(new HashMap<Language, String>() {{
            put(Language.EN, "Plugin {plugin} cancelled AsyncPlayerChatEvent (author: {author} | message: {message})");
            put(Language.FR, "Plugin {plugin} AsyncPlayerChatEvent annulé (author: {author} | message: {message})");
            put(Language.DE, "Plugin {plugin} brach AsyncPlayerChatEvent ab (Author: {author} | Nachricht: {message})");
            put(Language.JA, "Plugin {plugin} が AsyncPlayerChatEvent を呼び出した (author: {author} | message: {message})");
            put(Language.KO, "Plugin {plugin} 이(가) AsyncPlayerChatEvent를 취소하였습니다. (author: {author} | message: {message})");
            put(Language.NL, "Plugin {plugin} AsyncPlayerChatEvent geannuleerd (author: {author} | message: {message})");
            put(Language.ES, "Plugin {plugin} cancelo AsyncPlayerChatEvent (Author: {author} | Mensaje: {message})");
            put(Language.RU, "Plugin {plugin} cancelled AsyncPlayerChatEvent (author: {author} | message: {message})");

        }}), SHUTDOWN_COMPLETED(new HashMap<Language, String>() {{
            put(Language.EN, "Shutdown completed in {ms}ms");
            put(Language.FR, "Arrêt effectué en {ms}ms");
            put(Language.DE, "Herunterfahren wurde abgeschlossen in {ms}ms");
            put(Language.JA, "{ms}ミリ秒でシャットダウンしました");
            put(Language.KO, "서버가 {ms}ms만에 종료 됨.");
            put(Language.NL, "Shutdown klaar in {ms}ms");
            put(Language.ES, "Cierre completado en {ms}ms");
            put(Language.EN, "Shutdown completed in {ms}ms");

        }}), LANGUAGE_INITIALIZED(new HashMap<Language, String>() {{
            put(Language.EN, "Language initialized as ");
            put(Language.FR, "Langage initialisé à ");
            put(Language.DE, "Sprache initialisiert als ");
            put(Language.JA, "言語初期化完了: ");
            put(Language.KO, "언어가 다음으로 설정 되었습니다:");
            put(Language.NL, "Taal geinitialiseerd als ");
            put(Language.ES, "Idioma iniciado como ");
            put(Language.EN, "Language initialized as ");

        }}), API_LISTENER_SUBSCRIBED(new HashMap<Language, String>() {{
            put(Language.EN, "API listener {listenername} subscribed ({methodcount} methods)");
            put(Language.FR, "API listener {listenername} associé à ({methodcount} methods)");
            put(Language.DE, "API listener {listenername} Anmeldung ({methodcount} Methoden)");
            put(Language.JA, "API listener {listenername} が購読を開始しました (メソッド数: {methodcount} )");
            put(Language.KO, "API listener {listenername} 가 구독을 시작합니다. (Method 수: {methodcount})");
            put(Language.NL, "API listener {listenername} aangemeld ({methodcount} methods)");
            put(Language.ES, "API listener {listenername} suscrito a ({methodcount} métodos)");
            put(Language.EN, "API listener {listenername} subscribed ({methodcount} methods)");

        }}), API_LISTENER_UNSUBSCRIBED(new HashMap<Language, String>() {{
            put(Language.EN, "API listener {listenername} unsubscribed");
            put(Language.FR, "API listener {listenername} n'est plus associé");
            put(Language.DE, "API listener {listenername} Abmeldung");
            put(Language.JA, "API listener {listenername} が購読を終了しました");
            put(Language.KO, "API listener {listenername} 의 구독이 취소 되었습니다.");
            put(Language.NL, "API listener {listenername} afgemeld");
            put(Language.ES, "API listener {listenername} anulado");
            put(Language.EN, "API listener {listenername} unsubscribed");

        }}), API_LISTENER_THREW_ERROR(new HashMap<Language, String>() {{
            put(Language.EN, "DiscordSRV API Listener {listenername} threw an error");
            put(Language.FR, "DiscordSRV API Listener {listenername} a causé une erreur");
            put(Language.DE, "DiscordSRV API Listener {listenername} erzeugte einen Fehler");
            put(Language.JA, "DiscordSRV API Listener {listenername} でエラーが発生しました");
            put(Language.KO, "DiscordSRV API Listener {listenername} 에서 오류가 발생하였습니다.");
            put(Language.NL, "DiscordSRV API Listener {listenername} heeft een error");
            put(Language.ES, "DiscordSRV API Listener {listenername} lanzo un error");
            put(Language.EN, "DiscordSRV API Listener {listenername} threw an error");

        }}), API_LISTENER_METHOD_NOT_ACCESSIBLE(new HashMap<Language, String>() {{
            put(Language.EN, "DiscordSRV API Listener {listenername} method {methodname} was inaccessible despite efforts to make it accessible");
            put(Language.FR, "DiscordSRV API Listener {listenername} méthode {methodname} est inaccessible malgré les efforts pour la rendre accessible");
            put(Language.DE, "DiscordSRV API Listener {listenername} Methode {methodname} war unzugänglich trotz der Bemühungen, es zugänglich zu machen");
            put(Language.JA, "DiscordSRV API Listener {listenername} の Method {methodname} は、アクセスすることができなくなりました");
            put(Language.KO, "DiscordSRV API Listener {listenername} 의 method {methodname} 의 액세스에 실패 하였습니다.");
            put(Language.NL, "DiscordSRV API Listener {listenername} methode {methodname} was onberijkbaar ondanks alle moeite om het berijkbaar te maken");
            put(Language.ES, "DiscordSRV API Listener {listenername} metodo {methodname} era inaccesible a pesar de los esfuerzos para hacerlo accesible");
            put(Language.EN, "DiscordSRV API Listener {listenername} method {methodname} was inaccessible despite efforts to make it accessible");

        }}), HTTP_FAILED_TO_FETCH_URL(new HashMap<Language, String>() {{
            put(Language.EN, "Failed to fetch URL");
            put(Language.FR, "Impossible de récuperer l'URL");
            put(Language.DE, "Fehler beim Abrufen der URL");
            put(Language.JA, "URLの取得に失敗しました");
            put(Language.KO, "URL을 가져오는데 실패 하였습니다.");
            put(Language.NL, "Gefaald om de URL op te halen");
            put(Language.ES, "Fallo al buscar la URL");
            put(Language.EN, "Failed to fetch URL");

        }}), HTTP_FAILED_TO_DOWNLOAD_URL(new HashMap<Language, String>() {{
            put(Language.EN, "Failed to download URL");
            put(Language.FR, "Impossible de télécharger l'URL");
            put(Language.DE, "Fehler beim Download von URL");
            put(Language.JA, "URLからのダウンロードに失敗しました");
            put(Language.KO, "URL 다운로드에 실패 하였습니다.");
            put(Language.NL, "Gefaald om de URL te downloaden");
            put(Language.ES, "Fallo al descargar la URL");
            put(Language.EN, "Failed to download URL");

        }}), TOWNY_NOT_AUTOMATICALLY_ENABLING_CHANNEL_HOOKING(new HashMap<Language, String>() {{
            put(Language.EN, "Not automatically enabling hooking for TownyChat channels");
            put(Language.FR, "La compatibilité avec TownyChat n'est pas automatique");
            put(Language.DE, "Automatisches Einklinken in TownyChat deaktiviert");
            put(Language.JA, "TownyChat チャンネルへの接続を自動的に有効にしません");
            put(Language.KO, "TownyChat과 자동으로 연동하지 않습니다.");
            put(Language.NL, "Het hooken van TownyChat kanalen gaat niet automatisch");
            put(Language.ES, "La compatibilidad con TownyChat no es automática");
            put(Language.EN, "Not automatically enabling hooking for TownyChat channels");

        }}), TOWNY_AUTOMATICALLY_ENABLED_LINKING_FOR_CHANNELS(new HashMap<Language, String>() {{
            put(Language.EN, "Automatically enabled hooking for {amountofchannels} TownyChat channels");
            put(Language.FR, "Accrochage automatique des {amountofchannels} channels de TownyChat");
            put(Language.DE, "Automatisches Einklinken in {amountofchannels} TownyChat Chat-Kanäle");
            put(Language.JA, "TownyChat チャンネル {amountofchannels} への接続を自動的に有効にしました");
            put(Language.KO, "자동으로 TownyChat 채널 {amountofchannels} 개의 연동을 시작합니다");
            put(Language.NL, "Automatisch hooken voor {amountofchannels} TownyChat kanalen");
            put(Language.ES, "Enganche automático de {amountofchannels} canales de TownyChat");
            put(Language.EN, "Automatically enabled hooking for {amountofchannels} TownyChat channels");

        }}), TOWNY_AUTOMATICALLY_ENABLED_LINKING_FOR_NO_CHANNELS(new HashMap<Language, String>() {{
            put(Language.EN, "No TownyChat channels were automatically hooked. This might cause problems...");
            put(Language.FR, "Aucun channels de Towny Chat n'ont été trouvé. Cela peut causer des problèmes...");
            put(Language.DE, "Es konnte sich in keine TownyChat Chat-Kanäle automatisch eingeklinkt werden. Dies könnte eine Fehlerursache sein...");
            put(Language.JA, "TownyChatチャンネルは自動的に接続されませんでした。これは問題を引き起こす可能性があります...");
            put(Language.KO, "TownyChat에 연동된 채널이 없습니다. 연동된 채널이 없어 문제를 일으킬 수도 있습니다...");
            put(Language.NL, "Geen Townychat kanalen zijn automatisch gehooked. Dit kan problemen opleveren...");
            put(Language.ES, "No se encontraron canales de TownyChat. Esto puede causar problemas ...");
            put(Language.EN, "No TownyChat channels were automatically hooked. This might cause problems...");

        }}), PLUGIN_HOOK_ENABLING(new HashMap<Language, String>() {{
            put(Language.EN, "Enabling {plugin} hook");
            put(Language.FR, "Activation de l'accrochage du plugin {plugin}");
            put(Language.DE, "Aktiviere {plugin} Verbindung");
            put(Language.JA, "{plugin} の接続を有効にしました");
            put(Language.KO, "Plugin {plugin} 의 연동을 활성화합니다.");
            put(Language.NL, "Inschakelen {plugin} hook");
            put(Language.ES, "Activando complementos de {plugin}");
            put(Language.EN, "Enabling {plugin} hook");

        }}), PLUGIN_HOOKS_NOT_ENABLED(new HashMap<Language, String>() {{
            put(Language.EN, "No chat plugin hooks enabled");
            put(Language.FR, "Aucun accrochage de plugin activé");
            put(Language.DE, "Keine Pluginverbindungen aktiviert");
            put(Language.JA, "チャットプラグインへの接続は一つもありません");
            put(Language.KO, "활성화된 채팅 플러그인 연동 없음");
            put(Language.NL, "Geen chat plugin hooks ingeschakeld");
            put(Language.ES, "Sin complementos");
            put(Language.EN, "No chat plugin hooks enabled");

        }}), CHAT_CANCELLATION_DETECTOR_ENABLED(new HashMap<Language, String>() {{
            put(Language.EN, "Chat event cancellation detector has been enabled");
            put(Language.FR, "Détecteur d'annulation d'événement de chat vient d'être activé");
            put(Language.DE, "Chatevent-Abbruch-Detektor wurde aktiviert");
            put(Language.JA, "チャットイベントキャンセル検出機能が有効になっています");
            put(Language.KO, "채팅 취소 감지기가 구동되었습니다.");
            put(Language.NL, "Chat gebeurtenis annulering");
            put(Language.ES, "Detector de cancelacion de eventos de chat ha sido activado");
            put(Language.EN, "Chat event cancellation detector has been enabled");

        }}), INVALID_CONFIG(new HashMap<Language, String>() {{
            put(Language.EN, "Invalid config.yml");
            put(Language.FR, "config.yml invalide");
            put(Language.DE, "Ungültige config.yml");
            put(Language.JA, "config.ymlが不正です");
            put(Language.KO, "잘못된 config.yml 파일 입니다.");
            put(Language.NL, "Ongeldige config.yml");
            put(Language.ES, "config.yml Invalido");
            put(Language.EN, "Invalid config.yml");

        }}), FAILED_TO_CONNECT_TO_DISCORD(new HashMap<Language, String>() {{
            put(Language.EN, "DiscordSRV failed to connect to Discord. Reason");
            put(Language.FR, "DiscordSRV n'a pas réussi à se connecter à Discord. Raison");
            put(Language.DE, "Fehler beim Verbinden von DiscordSRV mit Discord. Grund");
            put(Language.JA, "DiscordSRV は Discord への接続に失敗しました。理由");
            put(Language.KO, "DiscordSRV 가 Discord 서버와 연결에 실패 하였습니다. 이유");
            put(Language.NL, "DiscordSRV kon niet met Discord verbinden. Reden");
            put(Language.ES, "DiscordSRV no se pudo conectar a Discord. Razon");
            put(Language.EN, "DiscordSRV failed to connect to Discord. Reason");

        }}), BOT_NOT_IN_ANY_SERVERS(new HashMap<Language, String>() {{
            put(Language.EN, "The bot is not a part of any Discord servers. Follow the installation instructions");
            put(Language.FR, "Le bot ne fait partie d'aucun serveur. Suivez les instructions d'installation");
            put(Language.DE, "Der Bot ist nicht Bestandteil irgendwelcher Discordserver. Folge den Installationsanweisungen");
            put(Language.JA, "このBotはどのDiscordサーバーにも所属していません。インストール手順に従ってください");
            put(Language.KO, "연동된 서버가 없습니다. 설치 방법을 따라 주세요.");
            put(Language.NL, "De bot maakt geen deel uit van een Discord server. Volg de instalatie instructies.");
            put(Language.ES, "El bot no es parte de ningún servidor de Discord. Siga las instrucciones de instalación");
            put(Language.EN, "The bot is not a part of any Discord servers. Follow the installation instructions");

        }}), NO_CHANNELS_LINKED(new HashMap<Language, String>() {{
            put(Language.EN, "No channels have been linked");
            put(Language.FR, "Aucun channel n'a été lié");
            put(Language.DE, "Es wurden keine Chat-Kanäle verbunden");
            put(Language.JA, "チャンネルに接続していません");
            put(Language.KO, "연동된 채널이 없습니다.");
            put(Language.NL, "Geen kanalen zijn gelinked");
            put(Language.ES, "No hay canales vinculados");
            put(Language.EN, "No channels have been linked");

        }}), NO_CHANNELS_LINKED_NOR_CONSOLE(new HashMap<Language, String>() {{
            put(Language.EN, "No channels nor a console channel have been linked. Have you followed the installation instructions?");
            put(Language.FR, "Aucun channel ou console n'ont été lié. Avez vous suivez les instructions d'installation ?");
            put(Language.DE, "Es wurden weder Chat-Kanäle, noch der Konsolenkanal verbunden. Bitte folge den Installationsanweisungen.");
            put(Language.JA, "チャネルにもコンソールチャネルにも接続されていません。インストール手順に従っていますか？");
            put(Language.KO, "연동된 콘솔 채널이 없습니다. 설치 방법을 정확히 따르셨나요?");
            put(Language.NL, "Geen kanaal en geen Console kanaal is gelinked. Heb je de instalatie instructies gevolgd?");
            put(Language.ES, "No se han vinculado canales ni un canal de consola. ¿Has seguido las instrucciones de instalación?");
            put(Language.EN, "No channels nor a console channel have been linked. Have you followed the installation instructions?");

        }}), CONSOLE_CHANNEL_ASSIGNED_TO_LINKED_CHANNEL(new HashMap<Language, String>() {{
            put(Language.EN, "The console channel was assigned to a channel that's being used for chat. Did you blindly copy/paste an ID into the channel ID config option?");
            put(Language.FR, "Le channel de la console à été assigné à un channel utilisé pour le tchat. Avez vous copié aveuglement l'ID d'un channel");
            put(Language.DE, "Der Konsolenkanal wurde mit einem Kanal verbunden, der auch für den Chat genutzt werden soll. Bitte korrigiere das und folge den Installationsanweisungen!");
            put(Language.JA, "コンソールチャンネルは、チャットに使用されているチャンネルと同じものが指定されています。IDをチャンネルID設定オプションにそのままコピペしていませんか？");
            put(Language.KO, "채팅 채널 ID가 콘솔 채널 ID와 같습니다. 정신 차리세요.");
            put(Language.NL, "Het console kanaal is gelinked met een kanaal dat voor chat gebruikt. Heb je het channel ID gekopieerd?? ;P");
            put(Language.ES, "El canal de la consola se asignó a un canal que se está utilizando para el chat. ¿Copió/pegó a ciegas una identificación en la opción de configuración de identificación del canal?");
            put(Language.EN, "The console channel was assigned to a channel that's being used for chat. Did you blindly copy/paste an ID into the channel ID config option?");

        }}), ZPERMISSIONS_VAULT_REQUIRED(new HashMap<Language, String>() {{
            put(Language.EN, "Vault is not installed. It is needed for the group synchronization to work with zPermissions. Install Vault if you want this feature.");
            put(Language.FR, "Vault n'est pas installé. Ce plugin est requis pour la synchronisation des groupes avec zPermissions. Installez Vault si vous souhaitez cette fonctionnalité.");
            put(Language.DE, "Vault ist nicht installiert. Es wird gebraucht für Gruppensynchronisation mit zPermissions. Installiere Vault wenn du dieses Feature benötigst.");
            put(Language.JA, "Vaultがインストールされていません。グループをzPermissionsと同期するために必要です。この機能を使用するには、Vaultをインストールしてください。");
            put(Language.KO, "Vault가 설치 되어있지 않습니다. zPermissions와 연동하려면 Vault플러그인이 필요합니다. 이 기능을 원하시면 Vault를 설치해 주세요.");
            put(Language.NL, "Vault is niet geinstalleerd. Het is nodig om de groep synchronisatie te laten werken met zPermissions. Instaleer Vault als je deze functie wil.");
            put(Language.ES, "Vault no está instalado. Se necesita para que la sincronización de grupo funcione con zPermissions. Instala Vault si quieres esta característica.");
            put(Language.EN, "Vault is not installed. It is needed for the group synchronization to work with zPermissions. Install Vault if you want this feature.");

        }}), CHAT(new HashMap<Language, String>() {{
            put(Language.EN, "Chat");
            put(Language.FR, "Tchat");
            put(Language.DE, "Chat");
            put(Language.JA, "チャット");
            put(Language.KO, "챗");
            put(Language.NL, "Chat");
            put(Language.ES, "Chat");
            put(Language.EN, "Chat");

        }}), ERROR_LOGGING_CONSOLE_ACTION(new HashMap<Language, String>() {{
            put(Language.EN, "Error logging console action to");
            put(Language.FR, "Erreur lors de la journalisation de l'action de la console");
            put(Language.DE, "Fehler beim Loggen einer Konsolenaktion nach");
            put(Language.JA, "動作記録失敗");
            put(Language.KO, "콘솔 로깅중 오류 발생 ");
            put(Language.NL, "Fout opgetreden tijdens het loggen van console acties");
            put(Language.ES, "Error al iniciar sesión en la consola");
            put(Language.EN, "Error logging console action to");

        }}), SILENT_JOIN(new HashMap<Language, String>() {{
            put(Language.EN, "Player {player} joined with silent joining permission, not sending a join message");
            put(Language.FR, "Le joueur {player} a rejoint le jeu avec une permission de silence lors de la connexion.");
            put(Language.DE, "Spieler {player} hat den Server mit Berechtigung zum stillen Betreten betreten, es wird keine Nachricht gesendet");
            put(Language.JA, "プレイヤー {player} は discordsrv.silentjoin の権限があるので、サーバー参加メッセージが送信されません");
            put(Language.KO, "플레이어 {player}가 discordsrv.slientjoin 퍼미션을 가지고 있습니다. 참가메세지를 보내지 않습니다.");
            put(Language.NL, "Speler {speler} joined met toestemming om stil te joinen, geen join bericht wordt verstuurd.");
            put(Language.ES, "Jugador {player} entro con el permiso de entrada silenciosa, no enviando mensaje de entrada");
            put(Language.EN, "Player {player} joined with silent joining permission, not sending a join message");

        }}), SILENT_QUIT(new HashMap<Language, String>() {{
            put(Language.EN, "Player {player} quit with silent quitting permission, not sending a quit message");
            put(Language.FR, "Le joueur {player} a quitté le jeu avec une permission de silence lors de le déconnexion.");
            put(Language.DE, "Spieler {player} hat den Server mit Berechtigung zum stillen Verlassen verlassen, es wird keine Nachricht gesendet");
            put(Language.JA, "プレイヤー {player} は discordsrv.silentquit の権限があるので、サーバー退出メッセージが送信されません");
            put(Language.KO, "플레이어 {player} 가 discordsrv.slientquit 퍼미션을 가지고 있습니다. 퇴장메세지를 보내지 않습니다.");
            put(Language.NL, "Speler {speler} is weg gegaan met toestemming om stil weg te gaan, geen quit bericht wordt verstuurd.");
            put(Language.ES, "Jugador {player} salió con el permiso de salida silenciosa, no enviando un mensaje de salida");
            put(Language.EN, "Player {player} quit with silent quitting permission, not sending a quit message");

        }}), LINKED_ACCOUNTS_SAVE_SKIPPED(new HashMap<Language, String>() {{
            put(Language.EN, "Skipped saving linked accounts because there were none");
            put(Language.FR, "Sauvegarde des comptes liés suspendue parce qu'il n'y en avait aucun");
            put(Language.DE, "Überspringe Speicherung von verknüpften Accounts weil kein vorhanden sind");
            put(Language.JA, "リンクされたアカウントが無いので、保存をスキップしました");
            put(Language.KO, "연동된 Discord 계정이 없어 연동계정 저장을 하지 않습니다.");
            put(Language.NL, "Het opslaan van gekoppelde accounts is overgeslagen omdat er geen waren.");
            put(Language.ES, "Omitido el guardado de cuentas vinculadas porque no habia ninguna");
            put(Language.EN, "Skipped saving linked accounts because there were none");

        }}), LINKED_ACCOUNTS_SAVED(new HashMap<Language, String>() {{
            put(Language.EN, "Saved linked accounts in {ms}ms");
            put(Language.FR, "Sauvegarde des comptes liés en {ms}ms");
            put(Language.DE, "Speichern von verknüpften Accounts in {ms}ms");
            put(Language.JA, "{ms}ミリ秒でリンクされたアカウントを保存しました");
            put(Language.KO, "{ms}ms 만에 연동계정 저장완료");
            put(Language.NL, "Gekoppelde accounts opgeslagen in {ms}ms");
            put(Language.ES, "Cuentas vinculadas guardadas en {ms}ms");
            put(Language.EN, "Saved linked accounts in {ms}ms");

        }}), LINKED_ACCOUNTS_SAVE_FAILED(new HashMap<Language, String>() {{
            put(Language.EN, "Failed saving linked accounts");
            put(Language.FR, "Erreur lors de la sauvegarde des comptes liés");
            put(Language.DE, "Fehler beim Speichern von verknüpften Accounts");
            put(Language.JA, "リンクされたアカウントの保存に失敗しました");
            put(Language.KO, "연동계정 저장 실패");
            put(Language.NL, "Opslaan van gekoppelde accounts is mislukt");
            put(Language.ES, "Fallo al guardar las cuentas vinculadas");
            put(Language.EN, "Failed saving linked accounts");

        }}), METRICS_SAVE_SKIPPED(new HashMap<Language, String>() {{
            put(Language.EN, "Skipped saving metrics because there were none");
            put(Language.FR, "Sauvegarde de métrics suspendue car il y en a aucun");
            put(Language.DE, "Überspringe Speichern von Statistiken weil keine Vorhanden");
            put(Language.JA, "メトリクスが無いので、保存をスキップしました");
            put(Language.KO, "Metrics가 없어, Metrics 저장을 하지 않습니다.");
            put(Language.NL, "Opslaan van instellingen is overgeslagen omdat er het er geen zijn.");
            put(Language.ES, "Omitido el guardado de metricas porque no habia ninguna");
            put(Language.EN, "Skipped saving metrics because there were none");

        }}), METRICS_SAVED(new HashMap<Language, String>() {{
            put(Language.EN, "Saved metrics in {ms}ms");
            put(Language.FR, "Sauvegarde de metrics en {ms}ms");
            put(Language.DE, "Speichern von Statistiken in {ms}ms");
            put(Language.JA, "{ms}ミリ秒でメトリクスを保存しました");
            put(Language.KO, "{ms}ms 만에 Metrics 저장 완료");
            put(Language.NL, "Instellingen opgeslagen in {ms}ms");
            put(Language.ES, "Metricas guardadas en {ms}ms");
            put(Language.EN, "Saved metrics in {ms}ms");

        }}), METRICS_SAVE_FAILED(new HashMap<Language, String>() {{
            put(Language.EN, "Failed saving metrics");
            put(Language.FR, "Erreur lors de la sauvegarde de metrics");
            put(Language.DE, "Fehler beim Speichern von Statistiken");
            put(Language.JA, "メトリクスの保存に失敗しました");
            put(Language.KO, "Metrics 저장 실패");
            put(Language.NL, "Gefaald om instellingen op teslaan.");
            put(Language.ES, "Fallo al guardar las metricas");
            put(Language.EN, "Failed saving metrics");

        }}), FAILED_LOADING_PLUGIN(new HashMap<Language, String>() {{
            put(Language.EN, "Failed loading plugin");
            put(Language.FR, "Erreur lors du chargement du plugin");
            put(Language.DE, "Fehler beim Laden des Plugins");
            put(Language.JA, "プラグインの起動に失敗しました");
            put(Language.KO, "플러그인 로드 실패");
            put(Language.NL, "Gefaald om de plugin te laden.");
            put(Language.ES, "Fallo al cargar el plugin");
            put(Language.EN, "Failed loading plugin");

        }}), GROUP_SYNCHRONIZATION_COULD_NOT_FIND_ROLE(new HashMap<Language, String>() {{
            put(Language.EN, "Could not find role id {rolename} for use with group synchronization. Is the bot in the server?");
            put(Language.FR, "Impossible de trouver le rôle {rolename} lors de la synchronisation des groupes.Le bot est il sur le serveur ?");
            put(Language.DE, "Konnte keine Rolle mit id {rolename} für gruppensynchronisierung finden. Ist der Bot auf dem Server?");
            put(Language.JA, "グループを同期させるために、ID「{rolename}」のロールを見つけることができませんでした。 Botはサーバ上にありますか？");
            put(Language.KO, "그룹 동기화를 할 Role ID를 찾을 수 없습니다. 봇이 디스코드 서버에 있나요?");
            put(Language.NL, "Kon role id {rolename} niet vinden dit word gebruikt voor groep synchronisatie. Is de bot in de server?");
            put(Language.ES, "No se pudo encontrar el rol {rolename} para usar con sincronización de grupo. ¿Esta el bot en el servidor?");
            put(Language.EN, "Could not find role id {rolename} for use with group synchronization. Is the bot in the server?");

        }}), UNKNOWN_CODE(new HashMap<Language, String>() {{
            put(Language.EN, "I don't know of such a code, try again.");
            put(Language.FR, "Je ne connais pas un tel code, réessayez");
            put(Language.DE, "Diesen Code kenne ich nicht, bitte versuche es erneut.");
            put(Language.JA, "そのようなコードは知りません。もう一度やり直してください。");
            put(Language.KO, "그런 코드는 발급한 적 없습니다, 다시 시도 해 주세요.");
            put(Language.NL, "Ik ken die code niet, probeer opnieuw.");
            put(Language.ES, "No conozco ese código, inténtelo de nuevo.");
            put(Language.EN, "I don't know of such a code, try again.");

        }}), INVALID_CODE(new HashMap<Language, String>() {{
            put(Language.EN, "Are you sure that's your code? Link codes are 4 numbers long.");
            put(Language.FR, "Êtes vous sûr qu'il s'agit du bon code ? Les codes possèdent quatre chiffres.");
            put(Language.DE, "Bist du sicher, dass dies dein Code ist? Link-Codes bestehen aus 4 Zahlen.");
            put(Language.JA, "それがあなたのコードで正しいですか？リンクコードは4文字の数字です。");
            put(Language.KO, "코드가 올바르지 않습니다. 코드는 4자리 숫자로 구성 되어 있습니다.");
            put(Language.NL, "Weet je zeker dat dat je code is? De codes zijn 4 cijferss lang.");
            put(Language.ES, "¿Estás seguro de que ese es tu código? Los códigos de enlace tienen 4 números de largo");
            put(Language.EN, "Are you sure that's your code? Link codes are 4 numbers long.");

        }}), NO_MESSAGE_GIVEN_TO_BROADCAST(new HashMap<Language, String>() {{
            put(Language.EN, "No language given to broadcast");
            put(Language.FR, "Aucune langue donnée à diffuser");
            put(Language.DE, "Keine Sprache für Broadcast angegeben");
            put(Language.JA, "ブロードキャストするメッセージが指定されていません。");
            put(Language.KO, "방송할 언어가 주어지지 않았습니다.");
            put(Language.NL, "Geen taal is opgegeven om uit te zenden.");
            put(Language.ES, "Ningún idioma dado para transmitir");
            put(Language.EN, "No language given to broadcast");

        }}), UNABLE_TO_LINK_ACCOUNTS_RIGHT_NOW(new HashMap<Language, String>() {{
            put(Language.EN, "Currently unable to link accounts due to an internal error. Contact your server administration team.");
            put(Language.FR, "Impossible de lier votre compte à cause d'une erreur. Merci de contacter l'équipe de votre serveur.");
            put(Language.DE, "Fehler beim Verbinden der Accounts wegen eines internen Fehlers. Bitte melde dies dem Serverteam.");
            put(Language.JA, "現在、内部エラーのためにアカウントをリンクできません。サーバー管理チームに連絡してください。");
            put(Language.KO, "서버 내부 오류로 인해 계정을 연동할 수 없습니다. 서버 관리팀을 불러주세요!");
            put(Language.NL, "Tijdelijk is het niet mogelijk om accounts te koppelen. Contact het administrator team.");
            put(Language.ES, "Actualmente no puede vincular cuentas debido a un error interno. Póngase en contacto con su equipo de administración del servidor.");
            put(Language.EN, "Currently unable to link accounts due to an internal error. Contact your server administration team.");

        }}), NO_PERMISSION(new HashMap<Language, String>() {{
            put(Language.EN, "You do not have permission to perform this command.");
            put(Language.FR, "Vous n'avez pas accès à cette commande.");
            put(Language.DE, "Du hast keine Berechtigung diesen Befehl auszuführen.");
            put(Language.JA, "あなたはこのコマンドを実行する権限がありません。");
            put(Language.KO, "이 명령어를 실행할 권한이 없습니다.");
            put(Language.NL, "Je moet toegang hebben om dit command te gebruiken.");
            put(Language.ES, "No tienes permisos para usar este comando");
            put(Language.EN, "You do not have permission to perform this command.");

        }}), PLAYER_ONLY_COMMAND(new HashMap<Language, String>() {{
            put(Language.EN, "Only players can execute this command.");
            put(Language.FR, "Seuls les joueurs peuvent effectuer cette commande.");
            put(Language.DE, "Nur Spieler können diesen Befehl ausführen.");
            put(Language.JA, "ゲーム内プレイヤーのみがこのコマンドを実行することができます。");
            put(Language.KO, "플레이어만 이 명령어를 실행 할 수 있습이다.");
            put(Language.NL, "Alleen spelers kunnen dit command gebruiken.");
            put(Language.ES, "Solo los jugadores pueden ejecutar este comando");
            put(Language.EN, "Only players can execute this command.");

        }}), COMMAND_DOESNT_EXIST(new HashMap<Language, String>() {{
            put(Language.EN, "That command doesn't exist!");
            put(Language.FR, "Cette commande n'existe pas !");
            put(Language.DE, "Dieser Befehl existiert nicht!");
            put(Language.JA, "指定されたコマンドは存在しません！");
            put(Language.KO, "그런 명령어는 없습니다!");
            put(Language.NL, "Dat command bestaat niet!");

            put(Language.ES, "Este comando no existe");
        }}), RELOADED(new HashMap<Language, String>() {{

            put(Language.EN, "That command doesn't exist!");
       }}), RELOADED(new HashMap<Language, String>() {{

            put(Language.EN, "The DiscordSRV config & lang have been reloaded.");
            put(Language.FR, "La configuration et les fichiers de langage de DiscordSRV ont été rechargé.");
            put(Language.DE, "Die DiscordSRV Konfiguration und Sprachdatei wurden neu eingelesen.");
            put(Language.JA, "DiscordSRVの設定と言語が再読込されました。");
            put(Language.KO, "DiscordSRV 컨피그 및 언어 설정이 리로드 되었습니다.");
            put(Language.NL, "De DiscordSRV config & lang is herladen.");
            put(Language.ES, "La configuracion y el idioma de DiscordSRV han sido recargadas");
            put(Language.EN, "The DiscordSRV config & lang have been reloaded.");

        }}), UNLINK_SUCCESS(new HashMap<Language, String>() {{
            put(Language.EN, "Your Minecraft account is no longer associated with {name}.");
            put(Language.FR, "Votre compte Minecraft n'est plus associé à {name}.");
            put(Language.DE, "Dein Minecraft-Account ist nicht länger verbunden mit {name}.");
            put(Language.JA, "あなたのMinecraftアカウントは、{name}とのリンクが解除されました。");
            put(Language.KO, "당신의 마인크래프트 계정은 더 이상 {name}과 연동되어있지 않습니다.");
            put(Language.NL, "Je Minecraft account is niet langer gekoppeld met {name}");
            put(Language.ES, "Tu cuenta de Minecraft ya no esta asociada con {name}");
            put(Language.EN, "Your Minecraft account is no longer associated with {name}.");

        }}), LINK_FAIL_NOT_ASSOCIATED_WITH_AN_ACCOUNT(new HashMap<Language, String>() {{
            put(Language.EN, "Your Minecraft account isn't associated with a Discord account.");
            put(Language.FR, "Votre compte Minecraft n'est pas associé à un compte Discord.");
            put(Language.DE, "Dein Minecraft-Account ist mit keinem Discord-Account verbunden.");
            put(Language.JA, "あなたのMinecraftアカウントはDiscordアカウントにリンクされていません。");
            put(Language.KO, "당신의 마인크래프트 계정은 디스코드 계정과 연동되어있지 않습니다.");
            put(Language.NL, "Je Minecraft account is niet gekoppeld met een Discord account.");
            put(Language.ES, "Tu cuenta de Minecraft no esta asociada con una cuenta de Discord");
            put(Language.EN, "Your Minecraft account isn't associated with a Discord account.");

        }}), LINKED_SUCCESS(new HashMap<Language, String>() {{
            put(Language.EN, "Your Minecraft account is associated with {name}.");
            put(Language.FR, "Votre compte Minecraft est associé à {name}.");
            put(Language.DE, "Dein Minecraft-Account ist verbunden mit {name}.");
            put(Language.JA, "あなたのMinecraftアカウントは{name}にリンクされました。");
            put(Language.KO, "당신의 마인크래프트 계정은 디스코드 계정 {name}과 연동되어 있습니다.");
            put(Language.NL, "Je Minecraft is gekoppeld met {name}");
            put(Language.ES, "Tu cuenta de Minecraft esta asociada con {name}");
            put(Language.EN, "Your Minecraft account is associated with {name}.");

        }}), LINKED_NOBODY_FOUND(new HashMap<Language, String>() {{
            put(Language.EN, "Nobody found with Discord ID/Discord name/Minecraft name/Minecraft UUID matching \"{target}\" to look up.");
            put(Language.FR, "Aucune personne ne correspond à l'ID Discord, le nom Minecraft ou un UUID \"{target}\".");
            put(Language.DE, "Niemand gefunden mit der Discord-ID/Discord-Name/Minecraft-Name/Minecraft-UUID \"{target}\".");
            put(Language.JA, "\"{target}\" と一致するものを Discord-ID/Discord-Name/Minecraft-Name/Minecraft-UUID から探しましたが、何も見つかりませんでした。");
            put(Language.KO, "\"{target}\"을 만족하는 Discord ID/Discord name/Minecraft name/Minecraft UUID 가 없습니다.");
            put(Language.NL, "Niemand gevonden met een Discord ID/Discord naam/Minecraft naam/Minecraft UUID daqt het zelfde is \"{target}\" om te zoeken. ");
            put(Language.ES, "No se encontro a nadie con Discord ID/nombre de Discord/nombre de Minecraft/Minecraft UUID \"{target}\" para buscar");
            put(Language.EN, "Nobody found with Discord ID/Discord name/Minecraft name/Minecraft UUID matching \"{target}\" to look up.");

        }}), LINKED_ACCOUNT_REQUIRED(new HashMap<Language, String>() {{
            put(Language.EN, "You attempted to say the following message to the game chat but this server requires that you have your Minecraft account linked to your Discord account. Link it in-game by typing `/discord link`.\n```{message}```");
            put(Language.FR, "Le message suivant n'a pas pu être envoyé sur le jeu car votre compte Minecraft doit être lié à votre compte Discord. Liez votre compte depuis Minecraft en tapant `/discord link`.\n```{message}```");
            put(Language.DE, "Du hast versucht die folgende Nachricht im Spielchat zu senden aber dieser Server verlangt, dass du deinen Minecraft-Account mit deinem Discord-Account verbinden musst. Verbinde sie, indem du im Spiel den Befehl `/discord link` eingibst.\n```{message}```");
            put(Language.JA, "ゲームチャットに以下のメッセージを表示しようとしましたが、このサーバーではあなたのMinecraftアカウントをDiscordアカウントにリンクさせる必要があります。リンクさせるには、ゲーム内で `/discord link` を実行してください。\n```{message}```");
            put(Language.KO, "이 서버는 게임채팅에 말하려면 당신의 마인크래프트 계정을 디스코드에 연동해야 합니다.\n 연동 방법 : `/discord link`.\n\n```{message}```");
            put(Language.NL, "Je hebt geprobeerd het volgende bericht te versturen maar je hebt je Minecraft account niet gekoppeld met je Discord account. koppel het door `/discord link` te typen.\n```{message}```");
            put(Language.ES, "Intentaste decir el siguiente mensaje en el chat del juego, pero este servidor requiere que tenga su cuenta de Minecraft vinculada a su cuenta de Discord. Vinculalo en el juego escribiendo `/discord link`.\n```{message}```");
            put(Language.EN, "You attempted to say the following message to the game chat but this server requires that you have your Minecraft account linked to your Discord account. Link it in-game by typing `/discord link`.\n```{message}```");

        }}), ACCOUNT_ALREADY_LINKED(new HashMap<Language, String>() {{
            put(Language.EN, "Your Minecraft account is already associated with a Discord account. Should you have permission to, you can unlink your account with /discord unlink.");
            put(Language.FR, "Votre compte Minecraft est déjà associé à un compte Discord. Si vous avez la permission vous pouvez utiliser la commande /discord unlink pour retirer votre compte Discord.");
            put(Language.DE, "Ihr Minecraft-Konto ist bereits mit einem Discord-Konto verknüpft. Sollten Sie die Erlaubnis haben, können Sie Ihre Konto-Verknüpfung mit /discord unlink aufheben.");
            put(Language.JA, "あなたのMinecraftアカウントはすでにDiscordアカウントに関連付けられています。 もしあなたがパーミッションを持っていれば、/discord unlinkを実行して2つのリンクを解除することができます。");
            put(Language.KO, "당신의 마인크래프트 계정은 디스코드 계정과 이미 연동되어 있습니다.");
            put(Language.NL, "Je Minecraft account is al gekoppeld met een Discord account. Als je toestemming hebt kan je je account ontkoppelen met /discord unlink.");
            put(Language.ES, "Su cuenta de Minecraft ya está asociada a una cuenta Discord. Si tiene permiso, puede desvincular su cuenta con /discord unlink.");
            put(Language.EN, "Your Minecraft account is already associated with a Discord account. Should you have permission to, you can unlink your account with /discord unlink.");

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
        PLAYER_LIST_COMMAND_PLAYER("DiscordChatChannelListCommandPlayerFormat", true),
        PLAYER_LIST_COMMAND_ALL_PLAYERS_SEPARATOR("DiscordChatChannelListCommandAllPlayersSeparator", false),
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

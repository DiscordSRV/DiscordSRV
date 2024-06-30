/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2024 Austin "Scarsz" Shapiro
 *
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
 */

package github.scarsz.discordsrv.util;

import github.scarsz.configuralize.Language;
import github.scarsz.discordsrv.DiscordSRV;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Made by Scarsz</p>
 * <p>German translations by Androkai & GerdSattler</p>
 * <p>Japanese translations by Ucchy</p>
 * <p>French translations by BrinDeNuage</p>
 * <p>Korean translations by Alex4386 (with MintNetwork)</p>
 * <p>Dutch translations by Mr Ceasar & Zarathos</p>
 * <p>Spanish translations by ZxFrankxZ</p>
 * <p>Russian translations by DmitryRendov</p>
 * <p>Estonian translations by Madis0</p>
 * <p>Chinese translations by Kizajan</p>
 * <p>Polish translations by Zabujca997</p>
 * <p>Danish translations by Tauses</p>
 * <p>Norwegian translations by OPM_Expert</p>
 * <p>Ukrainian translations by FenixInc</p>
 */
public class LangUtil {

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
                    "3. Download SpecialSource v1.7.4 from https://repo1.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
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
                    "3. Téléchargez le fichier v1.7.4 depuis https://repo1.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
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
                    "3. Lade dir die Datei SpecialSource v1.7.4 von hier herunter: https://repo1.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
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
                    "3. SpecialSource v1.7.4 を、次のURLからダウンロードします。https://repo1.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
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
                    "3. SpecialSource v1.7.4를 https://repo1.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar 에서 다운로드 받습니다.\n" +
                    "4. {specialsourcefolder}로 3에서 다운로드 받은 파일을 복사합니다.\n" +
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
                    "3. Download SpecialSource v1.7.4 vanaf https://repo1.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
                    "4. Kopieer de jar file naar de {specialsourcefolder} folder van je server waar je mee bezig bent.\n" +
                    "5. Verander de naam van de jar file die je hebt gekopieerd naar SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "6. Herstart je server.\n" +
                    "\n" +
                    "\n");
            put(Language.ES,
                    "\n" +
                    "\n" +
                    "Estás intentando usar DiscordSRV en ASM 4. DiscordSRV necesita ASM 5 para funcionar.\n" +
                    "DiscordSRV NO FUNCIONARÁ sin ASM 5. Informe al desarrollador del software del servidor de que la biblioteca no está actualizada.\n" +
                    "\n" +
                    "Instrucciones para actualizar a ASM 5:\n" +
                    "1. Navegue a la carpeta {specialsourcefolder} de tu servidor\n" +
                    "2. Elimine el archivo jar de SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "3. Descargue SpecialSource v1.7.4 desde https://repo1.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
                    "4. Copie el archivo jar en la carpeta {specialsourcefolder} del servidor al que navegaste anteriormente\n" +
                    "5. Renombre el archivo jar que acaba de copiar a: SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "6. Reinicie el servidor\n" +
                    "\n" +
                    "\n");
            put(Language.RU,
                    "\n" +
                    "\n" +
                    "Вы пытаетесь использовать DiscordSRV на ASM 4. DiscordSRV требует ASM 5 для работы.\n" +
                    "DiscordSRV НЕ БУДЕТ РАБОТАТЬ без ASM 5. Обратитесь к разработчикам вашей игровой платформы, чтобы получить необходимые библиотеки.\n" +
                    "\n" +
                    "Инструкции для обновления до ASM 5:\n" +
                    "1. Найдите папку {specialsourcefolder} на вашем сервере\n" +
                    "2. Удалите SpecialSource-1.7-SNAPSHOT.jar файл\n" +
                    "3. Скачайте SpecialSource v1.7.4.jar отсюда https://repo1.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
                    "4. Скопируйте jar файл в папку {specialsourcefolder} вашего сервера, которую вы открыли ранее\n" +
                    "5. Переименуйте jar файл, который вы скопировали в SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "6. Перезапустите сервер\n" +
                    "\n" +
                    "\n");
            put(Language.ET,
                    "\n" +
                    "\n" +
                    "Sa proovid DiscordSRV'i kasutada ASM 4 peal. DiscordSRV nõuab töötamiseks ASM 5-te.\n" +
                    "DiscordSRV EI TÖÖTA ilma ASM 5-ta. Süüdista oma serveritarkvara arendajaid vananenud teekide kasutamise eest.\n" +
                    "\n" +
                    "Juhised ASM 5-le täiendamiseks:\n" +
                    "1. Mine serveris kausta {specialsourcefolder}\n" +
                    "2. Kustuta fail SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "3. Laadi SpecialSource v1.7.4 alla saidilt https://repo1.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
                    "4. Kopeeri saadud jar-fail eelnevalt avatud kausta {specialsourcefolder}\n" +
                    "5. Nimeta just kopeeritud jar-fail ümber nimeks SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "6. Taaskäivita server\n" +
                    "\n" +
                    "\n");
            put(Language.ZH,
                    "\n" +
                    "\n" +
                    "您嘗試使用ASM 4來啟動DiscordSRV。 DiscordSRV需要ASM 5來啟動。\n" +
                    "DiscordSRV無法在缺少ASM 5的情況下啟動。 請諮詢您的伺服器軟體開發人員關於舊版函式庫。\n" +
                    "\n" +
                    "ASM 5 升級指南:\n" +
                    "1. 開啟伺服器中的 {specialsourcefolder} 資料夾\n" +
                    "2. 刪除jar檔 SpecialSource-1.7-SNAPSHOT.jar \n" +
                    "3. 下載 SpecialSource v1.7.4 從 https://repo1.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
                    "4. 複製該jar檔至先前在伺服器中開啟的 {specialsourcefolder} 資料夾\n" +
                    "5. 並將檔案重新命名為 SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "6. 重啟伺服器\n" +
                    "\n" +
                    "\n");
            put(Language.PL,
                    "\n" +
                    "\n" +
                    "Próbujesz użyć DiscordSRV na ASM 4. DiscordSRV wymaga ASM 5 do działania.\n" +
                    "DiscordSRV NIE BĘDZIE DZIAŁAŁ bez ASM 5. wincie twórców oprogramowania serwera za posiadanie nieaktualnych bibliotek.\n" +
                    "\n" +
                    "Instrukcje dotyczące aktualizacji do ASM 5:\n" +
                    "1. Przejdź do {specialsourcefolder} folder serwera\n" +
                    "2. Usuń SpecialSource-1.7-SNAPSHOT.jar plik jar\n" +
                    "3. Pobierz SpecialSource v1.7.4 z https://repo1.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
                    "4. Skopiuj plik jar do {specialsourcefolder} folderu serwera, do którego nawigowałeś wcześniej\n" +
                    "5. Zmień nazwę właśnie skopiowanego pliku jar na SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "6. Zrestartuj serwer\n" +
                    "\n" +
                    "\n");
            put(Language.DA,
                    "\n" +
                    "\n" +
                    "Du prøver at bruge DiscordSRV på ASM 4. DiscordSRV kræver ASM 5 for at fungere.\n" +
                    "DiscordSRV VIL IKKE VIRKE uden ASM 5. Skyd skylden på din servers software udviklere for at have uddaterede biblioteketer.\n" +
                    "\n" +
                    "Instruktionen til at opdatere til ASM 5:\n" +
                    "1. Naviger til {specialsourcefolder} folder i serveren\n" +
                    "2. Slet SpcialSource-1.7-SNAPSHOT.jar jar filen\n" +
                    "3. Download SpecialSource v1.7.4 fra https://repo1.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
                    "4. Kopier jar filen til {specialsourcefolder} folderen som nævnt tidligere.\n" +
                    "5. Omdøb jar filen du lige kopierede til SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "6. Genstart serveren\n" +
                    "\n" +
                    "\n");
             put(Language.UK,
                    "\n" +
                    "\n" +
                    "Ви намагаєтесь використовувати DiscordSRV на ASM 4. DiscordSRV вимагає ASM 5 для роботи.\n" +
                    "DiscordSRV не працюватиме без ASM 5. Зверніться до розробників вашої ігрової платформи, щоб отримати необхідні бібліотеки.\n" +
                    "\n" +
                    "Інструкції для оновлення до ASM 5:\n" +
                    "1. Знайдіть папку {special source folder} на вашому сервері\n" +
                    "2. Видаліть SpecialSource-1.7-SNAPSHOT.JAR файл\n" +
                    "3. Скачайте Special Source v1.7. 4.jar звідси https://repo1.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
                    "4. Скопіюйте jar файл в папку {specialsourcefolder} вашого сервера, яку ви відкрили раніше\n" +
                    "5. Перейменуйте jar файл, який ви скопіювали в SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "6. Перезапустіть сервер\n" +
                    "\n" +
                    "\n");
            put(Language.NB,
                    "\n" +
                    "\n" +
                    "Du forsøker å bruke DiscordSRV med ASM 4. DiscordSRV krever ASM 5 for å fungere.\n" +
                    "DiscordSRV VIL IKKE FUNGERE uten ASM 5. Skyld på serverens programvareutvikler for å bruke utdaterte biblioteker\n" +
                    "\n" +
                    "Veiledning for oppdatering til ASM 5:\n" +
                    "1. Naviger til {specialsourcefolder}-mappen på serveren\n" +
                    "2. Slett jar-filen SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "3. Last ned SpecialSource versjon 1.7.4 fra https://repo1.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
                    "4. Kopier jar-filen til {specialsourcefolder}-mappen på serveren du var på\n" +
                    "5. Omkall jar-filen du kopierte til SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "6. Restart the server\n" +
                    "\n" +
                    "\n");
        }}), RESPECT_CHAT_PLUGINS_DISABLED(new HashMap<Language, String>() {{
            put(Language.EN,
                    "\n" +
                    "\n" +
                    "RespectChatPlugins is disabled, this option is for TESTING PURPOSES ONLY\n" +
                    "and should NEVER be disabled on production servers.\n" +
                    "Disabling the option will cause cancelled messages to be forwarded to Discord\n" +
                    "including but not limited to private messages or staff chat messages without /commands\n" +
                    "\n" +
                    "\n");
            put(Language.FR,
                    "\n" +
                    "\n" +
                    "RespectChatPlugins est désactivé, cette option est UNIQUEMENT À DES FINS DE TEST\n" +
                    "et ne doit JAMAIS être désactivé sur les serveurs de production.\n" +
                    "La désactivation de cette option entraînera le transfert des messages annulés vers Discord\n" +
                    "y compris, mais sans s'y limiter, les messages privés ou les messages de discussion du personnel sans commandes\n" +
                    "\n" +
                    "\n");
            put(Language.DE,
                    "\n" +
                    "\n" +
                    "RespectChatPlugins ist deaktiviert. Diese Option dient nur zum Testen von Zwecken\n" +
                    "und sollte NIEMALS auf Produktionsservern deaktiviert werden.\n" +
                    "Durch Deaktivieren der Option werden abgebrochene Nachrichten an Discord weitergeleitet\n" +
                    "einschließlich, aber nicht beschränkt auf private Nachrichten oder Chat-Nachrichten von Mitarbeitern ohne / Befehle\n" +
                    "\n" +
                    "\n");
            put(Language.JA,
                    "\n" +
                    "\n" +
                    "RespectChatPluginsは無効になっています。このオプションは、目的をテストするためだけのものです\n" +
                    "実稼働サーバーでは無効にしないでください。\n" +
                    "オプションを無効にすると、キャンセルされたメッセージがDiscordに転送されます\n" +
                    "/commandsを使用しないプライベートメッセージまたはスタッフチャットメッセージが含まれますが、これらに限定されません\n" +
                    "\n" +
                    "\n");
            put(Language.KO,
                    "\n" +
                    "\n" +
                    "RespectChatPlugins가 비활성화되었습니다.이 옵션은 테스트 목적으로 만 사용됩니다.\n" +
                    "프로덕션 서버에서는 절대 비활성화하지 않아야합니다.\n" +
                    "이 옵션을 비활성화하면 취소 된 메시지가 불일치로 전달됩니다.\n" +
                    "/command가없는 개인 메시지 또는 직원 채팅 메시지를 포함하지만 이에 국한되지는 않습니다.\n" +
                    "\n" +
                    "\n");
            put(Language.NL,
                    "\n" +
                    "\n" +
                    "RespectChatPlugins is uitgeschakeld, deze optie is ALLEEN voor TESTEN VAN DOELEINDEN\n" +
                    "en mag NOOIT worden uitgeschakeld op productieservers.\n" +
                    "Als u deze optie uitschakelt, worden geannuleerde berichten doorgestuurd naar Discord\n" +
                    "inclusief maar niet beperkt tot privéberichten of chatberichten van personeel zonder / commando's\n" +
                    "\n" +
                    "\n");
            put(Language.ES,
                    "\n" +
                    "\n" +
                    "RespectChatPlugins está deshabilitado, esta opción es SOLO PARA PROPÓSITOS\n" +
                    "y NUNCA debe deshabilitarse en los servidores de producción.\n" +
                    "Deshabilitar la opción hará que los mensajes cancelados se reenvíen a Discord\n" +
                    "incluidos, entre otros, mensajes privados o mensajes de chat del personal sin / comandos\n" +
                    "\n" +
                    "\n");
            put(Language.RU,
                    "\n" +
                    "\n" +
                    "RespectChatPlugins отключен, эта опция ТОЛЬКО ДЛЯ ТЕСТИРОВАНИЯ\n" +
                    "и никогда не должен быть отключен на производственных серверах.\n" +
                    "Отключение этой опции приведет к тому, что отмененные сообщения будут отправлены в Discord\n" +
                    "включая, но не ограничиваясь, личные сообщения или сообщения чата персонала без / команд\n" +
                    "\n" +
                    "\n");
            put(Language.ET,
                    "\n" +
                    "\n" +
                    "RespectChatPlugins on keelatud, see suvand on ette nähtud AINULT EESMÄRKIDE TESTIMISEKS\n" +
                    "ja seda ei tohiks KUNAGI tootmisserverites keelata.\n" +
                    "Selle valiku keelamisel edastatakse tühistatud kirjad Discordile\n" +
                    "sealhulgas, kuid mitte ainult, privaatsõnumid või personali vestlussõnumid ilma / käskudeta\n" +
                    "\n" +
                    "\n");
            put(Language.ZH,
                    "\n" +
                    "\n" +
                    "RespectChatPlugins已禁用，此選項僅用於測試目的\n" +
                    "並且永遠不要在生產伺服器上禁用它。\n" +
                    "禁用該選項將導致取消的郵件轉發到Discord\n" +
                    "包括但不限於不帶/ command的私人消息或員工聊天消息\n" +
                    "\n" +
                    "\n");
            put(Language.PL,
                    "\n" +
                    "\n" +
                    "RespectChatPlugins jest wyłączone, ta opcja służy TYLKO DO CELÓW TESTOWYCH\n" +
                    "i NIGDY nie powinno być wyłączane na serwerach produkcyjnych.\n" +
                    "Wyłączenie tej opcji spowoduje, że anulowane wiadomości będą przekazywane do Discord\n" +
                    "w tym między innymi prywatne wiadomości lub wiadomości na czacie graczy bez /komend\n" +
                    "\n" +
                    "\n");
            put(Language.DA,
                    "\n" +
                    "\n" +
                    "RespectChatPlugins er deaktiveret, denne mulighed er KUN til TESTFORMÅL\n" +
                    "og bør ALDRIG deaktiveres på produktionsservere.\n" +
                    "Hvis du deaktiverer indstillingen, vil annullerede beskeder blive videresendt til Discord\n" +
                    "inklusive men ikke begrænset til private beskeder eller personalechatbeskeder uden /kommandoer\n" +
                    "\n" +
                    "\n");
            put(Language.UK,
                    "\n" +
                    "\n" +
                    "Ви намагаєтесь використовувати DiscordSRV на ASM 4. DiscordSRV вимагає ASM 5 для роботи.\n" +
                    "DiscordSRV не працюватиме без ASM 5. Зверніться до розробників вашої ігрової платформи, щоб отримати необхідні бібліотеки.\n" +
                    "\n" +
                    "Інструкції щодо оновлення до ASM 5:\n" +
                    "1. Знайдіть папку {special source folder} на вашому сервері\n" +
                    "2. Respect Chat Plugins вимкнено, ця опція лише для тестування\n" +
                    "3. Скачайте Special Source v1.7. 4.jar звідси https://repo1.maven.org/maven2/net/md-5/SpecialSource/1.7.4/SpecialSource-1.7.4.jar\n" +
                    "4. Скопіюйте jar файл в папку {special source folder} вашого сервера, яку ви відкрили раніше\n" +
                    "5. Перейменуйте jar файл, який ви скопіювали в SpecialSource-1.7-SNAPSHOT.jar\n" +
                    "6. Перезапустіть сервер\n" +
                    "\n" +
                    "\n");
            put(Language.NB,
                    "\n" +
                    "\n" +
                    "RespectChatPlugins er deaktivert, dette alternativet er BARE FOR TESTBRUK\n" +
                    "og skal ALDRI bli deaktivert på produksjonsservere.\n" +
                    "Deaktiveing av dette alternativet vil forårsake at kansellerte meldinger vil bli sendt til Discord\n" +
                    "inkludert, men ikke begrensert til private meldinger eller chat-meldinger fra serverpersonal uten /kommandoer\n" +
                    "\n" +
                    "\n");
        }}), INCOMPATIBLE_CLIENT(new HashMap<Language, String>() {{
            put(Language.EN, "Your user experience is degraded due to using {client}, some commands may not work as expected.");
            put(Language.FR, "Votre expérience utilisateur est dégradée en raison de l'utilisation de {client}, certaines commandes peuvent ne pas fonctionner comme prévu.");
            put(Language.DE, "Ihre Benutzererfahrung ist durch die Verwendung von {client} beeinträchtigt. Einige Befehle funktionieren möglicherweise nicht wie erwartet.");
            put(Language.JA, "{client}を使用しているため、ユーザーエクスペリエンスが低下し、一部のコマンドが期待どおりに機能しない場合があります。 ");
            put(Language.KO, "{client} 사용으로 인해 사용자 경험이 저하되고 일부 명령이 예상대로 작동하지 않을 수 있습니다.");
            put(Language.NL, "Uw gebruikerservaring is verslechterd door het gebruik van {client}. Sommige opdrachten werken mogelijk niet zoals verwacht.");
            put(Language.ES, "Su experiencia de usuario se degrada debido al uso de {cliente}, es posible que algunos comandos no funcionen como se esperaba.");
            put(Language.RU, "Ваше взаимодействие с пользователем ухудшается из-за использования {client}, некоторые команды могут работать не так, как ожидалось.");
            put(Language.ET, "Teie kasutuskogemus on {client} kasutamise tõttu halvenenud, mõned käsud ei pruugi ootuspäraselt töötada.");
            put(Language.ZH, "您的用戶體驗因使用 {client} 而下降，某些命令可能無法按預期工作。");
            put(Language.PL, "Twoje doświadczenie użytkownika jest pogorszone z powodu korzystania z {client}, niektóre polecenia mogą nie działać zgodnie z oczekiwaniami.");
            put(Language.DA, "Din brugeroplevelse er nedgraderet grundet din brug af {client}, nogle kommandoer ville ikke virke som forventet.");
            put(Language.UK, "Ваша взаємодія з користувачем погіршується через використання {client}, деякі команди можуть працювати не так, як очікувалося.");
            put(Language.NB, "Brukeropplevelsen din er degradert grunnet bruk av {client}. Noen kommandoer vil kanskje ikke virke som forventet.");
        }}), CONSOLE_FORWARDING_ASSIGNED_TO_CHANNEL(new HashMap<Language, String>() {{
            put(Language.EN, "Console forwarding assigned to channel");
            put(Language.FR, "Réacheminement de la console affecté au canal");
            put(Language.DE, "Konsolenausgabeweiterleitung aktiv");
            put(Language.JA, "コンソールフォワーディングがチャンネルに割り当てられました");
            put(Language.KO, "콘솔포워딩이 채널에 설정되었습니다");
            put(Language.NL, "Console versturen verbonden aan kanaal");
            put(Language.ES, "Enviar la consola al canal asignado");
            put(Language.RU, "Вывод консоли успешно перенаправлен в канал");
            put(Language.ET, "Konsooliedastus on kanalile määratud");
            put(Language.ZH, "控制台轉送已指派至頻道");
            put(Language.PL, "Przekazywanie konsoli przypisane do kanału");
            put(Language.DA, "Konsol videresendelse tildelt til kanal");
            put(Language.UK, "Вихід консолі успішно перенаправлений на канал");
            put(Language.NB, "Konsollsending er bundet til kanal");
        }}), NOT_FORWARDING_CONSOLE_OUTPUT(new HashMap<Language, String>() {{
            put(Language.EN, "Console channel ID was invalid, not forwarding console output");
            put(Language.FR, "L'ID du channel de la console est faux, l'envoie des messages de la console ne sera pas effectué");
            put(Language.DE, "Konsolenkanal-ID ist ungültig, keine Konsolenausgabe Weiterleitung aktiv");
            put(Language.JA, "コンソールチャネルIDは無効であるため、コンソール転送は行われません");
            put(Language.KO, "콘솔 채널 ID가 올바르지 않습니다. 콘솔 메세지를 채널로 보내지 않습니다.");
            put(Language.NL, "Console kanaal ID is ongeldig, de console wordt niet verzonden");
            put(Language.ES, "El ID del canal de la consola no es válido, no se enviará ningún mensaje de la consola");
            put(Language.RU, "Неверный ID канала для перенаправления вывода консоли, сообщения консоли не будут пересылаться");
            put(Language.ET, "Konsoolikanali ID oli sobimatu, konsooli väljundit ei edastata");
            put(Language.ZH, "錯誤的控制台頻道ID, 並未轉送控制台輸出。");
            put(Language.PL, "Identyfikator kanału konsoli był nieprawidłowy, nie przekazuje danych wyjściowych konsoli");
            put(Language.DA, "Konsol kanal ID var invalidt, videresender ikke konsole beskeder");
            put(Language.UK, " невірний ID каналу для перенаправлення виводу консолі, повідомлення консолі не будуть пересилатися");
            put(Language.NB, "Konsoll-kanalens ID er ugyldig, sender ikke konsollutdata");
        }}), SHUTDOWN_COMPLETED(new HashMap<Language, String>() {{
            put(Language.EN, "Shutdown completed in {ms}ms");
            put(Language.FR, "Arrêt effectué en {ms}ms");
            put(Language.DE, "Herunterfahren wurde abgeschlossen in {ms}ms");
            put(Language.JA, "{ms}ミリ秒でシャットダウンしました");
            put(Language.KO, "서버가 {ms}ms만에 종료 됨.");
            put(Language.NL, "Shutdown klaar in {ms}ms");
            put(Language.ES, "Apagado completado en {ms}ms");
            put(Language.RU, "Отключение завершено за {ms}мс");
            put(Language.ET, "Väljalülitus teostatud {ms}ms jooksul");
            put(Language.ZH, "伺服器已關閉，耗時{ms}ms");
            put(Language.PL, "Wyłączenie zostanie zakończone za {ms}ms");
            put(Language.DA, "Nedlukning gennemført på {ms}ms");
            put(Language.UK, " відключення завершено за {ms}мс");
            put(Language.NB, "Avslutning fullført på {ms} ms");
        }}), API_LISTENER_SUBSCRIBED(new HashMap<Language, String>() {{
            put(Language.EN, "API listener {listenername} subscribed ({methodcount} methods)");
            put(Language.FR, "API listener {listenername} associé à ({methodcount} methods)");
            put(Language.DE, "API listener {listenername} Anmeldung ({methodcount} Methoden)");
            put(Language.JA, "API listener {listenername} が購読を開始しました (メソッド数: {methodcount} )");
            put(Language.KO, "API listener {listenername} 가 구독을 시작합니다. (Method 수: {methodcount})");
            put(Language.NL, "API listener {listenername} aangemeld ({methodcount} methods)");
            put(Language.ES, "API listener {listenername} suscrito a ({methodcount} métodos)");
            put(Language.RU, "API listener {listenername} подписан на ({methodcount} методы)");
            put(Language.ET, "API listener {listenername} on kuulamas ({methodcount} meetodit)");
            put(Language.ZH, "API listener {listenername} 已訂閱 ({methodcount} 種方案)");
            put(Language.PL, "Odbiornik API {listenername} zasubskrybowano ({methodcount} metodą)");
            put(Language.DA, "API listener {listenername} abonneret ({methodcount} metoder)");
            put(Language.UK, "API listener {listenername} підписано на ({methodcount} методи)");
            put(Language.NB, "API-lytter {listername} abonnerte på ({methodcount} måter)");
        }}), API_LISTENER_UNSUBSCRIBED(new HashMap<Language, String>() {{
            put(Language.EN, "API listener {listenername} unsubscribed");
            put(Language.FR, "API listener {listenername} n'est plus associé");
            put(Language.DE, "API listener {listenername} Abmeldung");
            put(Language.JA, "API listener {listenername} が購読を終了しました");
            put(Language.KO, "API listener {listenername} 의 구독이 취소 되었습니다.");
            put(Language.NL, "API listener {listenername} afgemeld");
            put(Language.ES, "API listener {listenername} anulado");
            put(Language.RU, "API listener {listenername} деактивирован");
            put(Language.ET, "API listener {listenername} kuulamine lõpetatud");
            put(Language.ZH, "API listener {listenername} 已取消訂閱");
            put(Language.PL, "Odbiornik API {listenername} odbubskrybowano");
            put(Language.DA, "API listener {listenername} afmeldt abonnement");
            put(Language.UK, "API listener {listenername} деактивовано");
            put(Language.NB, "API-lytter {listenername} avmeldte");
        }}), API_LISTENER_METHOD_NOT_ACCESSIBLE(new HashMap<Language, String>() {{
            put(Language.EN, "DiscordSRV API Listener {listenername} method {methodname} was inaccessible despite efforts to make it accessible");
            put(Language.FR, "DiscordSRV API Listener {listenername} méthode {methodname} est inaccessible malgré les efforts pour la rendre accessible");
            put(Language.DE, "DiscordSRV API Listener {listenername} Methode {methodname} war unzugänglich trotz der Bemühungen, es zugänglich zu machen");
            put(Language.JA, "DiscordSRV API Listener {listenername} の Method {methodname} は、アクセスすることができなくなりました");
            put(Language.KO, "DiscordSRV API Listener {listenername} 의 method {methodname} 의 액세스에 실패 하였습니다.");
            put(Language.NL, "DiscordSRV API Listener {listenername} methode {methodname} was onberijkbaar ondanks alle moeite om het berijkbaar te maken");
            put(Language.ES, "DiscordSRV API Listener {listenername} método {methodname} era inaccesible a pesar de los esfuerzos para hacerlo accesible");
            put(Language.RU, "DiscordSRV API Listener {listenername} метод {methodname} был недоступен, несмотря на все наши усилия сделать его доступным");
            put(Language.ET, "DiscordSRV API Listener {listenername} meetod {methodname} polnud ligipääsetav, kuigi prooviti ligipääsetavaks teha");
            put(Language.ZH, "DiscordSRV API Listener {listenername} 方案 {methodname} 無法存取");
            put(Language.PL, "Odbiornik DiscordSRV API {listenername} metodą {methodname} był niedostępny pomimo starań, aby był dostępny");
            put(Language.DA, "DiscordSRV API Listener {listenername} metode {methodname} var utilgængelig på trods af indsats til at gøre den tilgængelig");
            put(Language.UK, "DiscordSRV API Listener {listener name} метод {methodname} був недоступний, незважаючи на всі наші зусилля, щоб зробити його доступним");
            put(Language.NB, "DiscordSRVs API-lytter {listenername} metode {methodname} var utilgjengelig til tross for forsøk på å gjøre det tilgjengelig");
        }}), HTTP_FAILED_TO_FETCH_URL(new HashMap<Language, String>() {{
            put(Language.EN, "Failed to fetch URL");
            put(Language.FR, "Impossible de récuperer l'URL");
            put(Language.DE, "Fehler beim Abrufen der URL");
            put(Language.JA, "URLの取得に失敗しました");
            put(Language.KO, "URL을 가져오는데 실패 하였습니다.");
            put(Language.NL, "Gefaald om de URL op te halen");
            put(Language.ES, "Fallo al buscar la URL");
            put(Language.RU, "Ошибка получения URL");
            put(Language.ET, "URLi hankimine ebaõnnestus");
            put(Language.ZH, "無法取得URL");
            put(Language.PL, "Nie udało się pobrać adresu URL");
            put(Language.DA, "Kunne ikke hente URL");
            put(Language.UK, "помилка отримання URL");
            put(Language.NB, "Kunne ikke hente URL");
        }}), HTTP_FAILED_TO_DOWNLOAD_URL(new HashMap<Language, String>() {{
            put(Language.EN, "Failed to download URL");
            put(Language.FR, "Impossible de télécharger l'URL");
            put(Language.DE, "Fehler beim Download von URL");
            put(Language.JA, "URLからのダウンロードに失敗しました");
            put(Language.KO, "URL 다운로드에 실패 하였습니다.");
            put(Language.NL, "Gefaald om de URL te downloaden");
            put(Language.ES, "Fallo al descargar la URL");
            put(Language.RU, "Ошибка загрузки URL");
            put(Language.ET, "URLi allalaadimine ebaõnnestus");
            put(Language.ZH, "自URL下載失敗");
            put(Language.PL, "Nie udało się pobrać adresu URL");
            put(Language.DA, "Kunne ikke downloade URL");
            put(Language.UK, "Помилка завантаження URL");
            put(Language.NB, "Kunne ikke laste ned URL");
        }}), PLUGIN_HOOK_ENABLING(new HashMap<Language, String>() {{
            put(Language.EN, "Enabling {plugin} hook");
            put(Language.FR, "Activation de l'accrochage du plugin {plugin}");
            put(Language.DE, "Aktiviere {plugin} Verbindung");
            put(Language.JA, "{plugin} の接続を有効にしました");
            put(Language.KO, "Plugin {plugin} 의 연동을 활성화합니다.");
            put(Language.NL, "Inschakelen {plugin} hook");
            put(Language.ES, "Activando complementos de {plugin}");
            put(Language.RU, "Активация {plugin} подключения");
            put(Language.ET, "{plugin} haakimine lubatud");
            put(Language.ZH, "啟用鉤取 {plugin}");
            put(Language.PL, "Włączono {plugin} haczyk");
            put(Language.DA, "Aktivere {plugin} hook");
            put(Language.UK, "Активація {plugin} підключення");
            put(Language.NB, "Aktiverer {plugin}-tilkopling");
        }}), NO_CHAT_PLUGIN_HOOKED(new HashMap<Language, String>() {{
            put(Language.EN, "No chat plugin hooks enabled");
            put(Language.FR, "Aucun accrochage de plugin activé");
            put(Language.DE, "Keine Pluginverbindungen aktiviert");
            put(Language.JA, "チャットプラグインへの接続は一つもありません");
            put(Language.KO, "활성화된 채팅 플러그인 연동 없음");
            put(Language.NL, "Geen chat plugin hooks ingeschakeld");
            put(Language.ES, "Sin complementos");
            put(Language.RU, "Плагинов для управления игровым чатом не обнаружено");
            put(Language.ET, "Ühegi vestlusplugina haakimine pole lubatud");
            put(Language.ZH, "未啟用鉤取任何聊天插件");
            put(Language.PL, "Żadna wtyczka czatu nie jest włączona");
            put(Language.DA, "Ingen chat plugin hooks aktiveret");
            put(Language.UK, "плагінів для управління ігровим чатом не виявлено");
            put(Language.NB, "Ingen chatutvidelsestilkoplinger er aktivert");
        }}), CHAT_CANCELLATION_DETECTOR_ENABLED(new HashMap<Language, String>() {{
            put(Language.EN, "Chat event cancellation detector has been enabled");
            put(Language.FR, "Détecteur d'annulation d'événement de chat vient d'être activé");
            put(Language.DE, "Chatevent-Abbruch-Detektor wurde aktiviert");
            put(Language.JA, "チャットイベントキャンセル検出機能が有効になっています");
            put(Language.KO, "채팅 취소 감지기가 구동되었습니다.");
            put(Language.NL, "Chat gebeurtenis annulering");
            put(Language.ES, "El detector de cancelación de eventos de chat ha sido activado");
            put(Language.RU, "Включен детектор отмены сообщений чата");
            put(Language.ET, "Vestlussündmuste tühistamise tuvastaja on lubatud");
            put(Language.ZH, "聊天事件撤銷檢測器已啟動");
            put(Language.PL, "Wykrywacz anulowania zdarzeń czatu został włączony");
            put(Language.DA, "Detektor for annullering af events er blevet aktiveret");
            put(Language.UK, "увімкнено детектор скасування повідомлень чату");
            put(Language.NB, "Chat-hendelseskanselleringsdetektor har blitt aktivert");
        }}), BOT_NOT_IN_ANY_SERVERS(new HashMap<Language, String>() {{
            put(Language.EN, "The bot is not a part of any Discord servers. Follow the installation instructions");
            put(Language.FR, "Le bot ne fait partie d'aucun serveur. Suivez les instructions d'installation");
            put(Language.DE, "Der Bot ist nicht Bestandteil irgendwelcher Discordserver. Folge den Installationsanweisungen");
            put(Language.JA, "このBotはどのDiscordサーバーにも所属していません。インストール手順に従ってください");
            put(Language.KO, "연동된 서버가 없습니다. 설치 방법을 따라 주세요.");
            put(Language.NL, "De bot maakt geen deel uit van een Discord server. Volg de instalatie instructies.");
            put(Language.ES, "El bot no es parte de ningún servidor de Discord. Siga las instrucciones de instalación");
            put(Language.RU, "Этот Бот не является частью какого-либо сервера Discord. Подключите его к серверу, следуя инструкциям по установке");
            put(Language.ET, "See bot ei ole ühegi Discordi serveri osa. Järgi paigaldusjuhiseid");
            put(Language.ZH, "這個BOT並不屬於Discord伺服器。 請參照安裝指南。");
            put(Language.PL, "Bot nie jest częścią żadnego serwera Discord. Postępuj zgodnie z instrukcjami instalacji");
            put(Language.DA, "Botten er ikke en del af nogle Discord servere. Følg installations manualen");
            put(Language.UK, "цей Бот не є частиною жодного сервера Discord. Підключіть його до сервера, дотримуючись інструкцій з встановлення");
            put(Language.NB, "Boten er ikke med i noen Discord-servere. Følg installasjonsinstruksjonene");
        }}), CONSOLE_CHANNEL_ASSIGNED_TO_LINKED_CHANNEL(new HashMap<Language, String>() {{
            put(Language.EN, "The console channel was assigned to a channel that's being used for chat. Did you blindly copy/paste an ID into the channel ID config option?");
            put(Language.FR, "Le channel de la console à été assigné à un channel utilisé pour le tchat. Avez vous copié aveuglement l'ID d'un channel");
            put(Language.DE, "Der Konsolenkanal wurde mit einem Kanal verbunden, der auch für den Chat genutzt werden soll. Bitte korrigiere das und folge den Installationsanweisungen!");
            put(Language.JA, "コンソールチャンネルは、チャットに使用されているチャンネルと同じものが指定されています。IDをチャンネルID設定オプションにそのままコピペしていませんか？");
            put(Language.KO, "채팅 채널 ID가 콘솔 채널 ID와 같습니다. 정신 차리세요.");
            put(Language.NL, "Het console kanaal is gelinked met een kanaal dat voor chat gebruikt. Heb je het channel ID gekopieerd?? ;P");
            put(Language.ES, "El canal de la consola se asignó a un canal que se está utilizando para el chat. ¿Copió/pegó a ciegas el ID en la opción de configuración de identificación del canal?");
            put(Language.RU, "Канал для консоли был прикреплен к каналу серверного чата! Слепой копипаст ID канала в файле конфигурации?");
            put(Language.ET, "Konsoolikanal määrati kanalile, mida kasutatakse vestluseks. Kas sa kopeerisid mõne ID pimesi kanali ID seadistusvalikusse?");
            put(Language.ZH, "這個控制台頻道已指派給聊天用頻道。 請確認設定中的頻道ID是否正確。");
            put(Language.PL, "Kanał konsoli został przypisany do kanału używanego do czatu. Czy na ślepo skopiowałeś / wkleiłeś identyfikator do opcji konfiguracji identyfikatora kanału?");
            put(Language.DA, "Konsol kanalen er blevet tildelt til en kanal der bliver brugt til chatten. Har du indsat ID'et i den forkerte kanal i konfigurations filen?");
            put(Language.UK, "Канал для консолі був прикріплений до каналу серверного чату! Сліпий копіпаст ID каналу у файлі конфігурації?");
            put(Language.NB, "Konsollkanalen ble bundet til en kanal som blir brukt som chat. Kopierte og limte du en ID i kanal-ID-alternativet blindt?");
        }}), CHAT(new HashMap<Language, String>() {{
            put(Language.EN, "Chat");
            put(Language.FR, "Tchat");
            put(Language.DE, "Chat");
            put(Language.JA, "チャット");
            put(Language.KO, "챗");
            put(Language.NL, "Chat");
            put(Language.ES, "Chat");
            put(Language.RU, "Чат");
            put(Language.ET, "Vestlus");
            put(Language.ZH, "聊天");
            put(Language.PL, "Czat");
            put(Language.DA, "Chat");
            put(Language.UK, "Чат");
            put(Language.NB, "Chat");
        }}), ERROR_LOGGING_CONSOLE_ACTION(new HashMap<Language, String>() {{
            put(Language.EN, "Error logging console action to");
            put(Language.FR, "Erreur lors de la journalisation de l'action de la console");
            put(Language.DE, "Fehler beim Loggen einer Konsolenaktion nach");
            put(Language.JA, "動作記録失敗");
            put(Language.KO, "콘솔 로깅중 오류 발생 ");
            put(Language.NL, "Fout opgetreden tijdens het loggen van console acties");
            put(Language.ES, "Error al iniciar sesión en la consola");
            put(Language.RU, "Ошибка логирования действий консоли в");
            put(Language.ET, "Esines viga konsoolitegevuse logimisel asukohta");
            put(Language.ZH, "控制台記錄錯誤");
            put(Language.PL, "Błąd podczas rejestrowania akcji konsoli do");
            put(Language.DA, "Fejl under logning af konsolhandling");
            put(Language.UK, "помилка логування дій консолі в");
            put(Language.NB, "Feil ved logging av konsollhandling til");
        }}), SILENT_JOIN(new HashMap<Language, String>() {{
            put(Language.EN, "Player {player} joined with silent joining permission, not sending a join message");
            put(Language.FR, "Le joueur {player} a rejoint le jeu avec une permission de silence lors de la connexion.");
            put(Language.DE, "Spieler {player} hat den Server mit Berechtigung zum stillen Betreten betreten, es wird keine Nachricht gesendet");
            put(Language.JA, "プレイヤー {player} は discordsrv.silentjoin の権限があるので、サーバー参加メッセージが送信されません");
            put(Language.KO, "플레이어 {player}가 discordsrv.slientjoin 퍼미션을 가지고 있습니다. 참가메세지를 보내지 않습니다.");
            put(Language.NL, "Speler {speler} joined met toestemming om stil te joinen, geen join bericht wordt verstuurd.");
            put(Language.ES, "Jugador {player} entró con el permiso de entrada silenciosa, no se ha enviado mensaje de entrada");
            put(Language.RU, "Игрок {player} незаметно присоединился к серверу, безо всяких сообщений в чате");
            put(Language.ET, "Mängija {player} liitus vaikse liitumise õigusega, liitumissõnumit ei saadeta");
            put(Language.ZH, "玩家 {player} 使用靜默登入權限進入了伺服器，並未發送登入訊息。");
            put(Language.PL, "Gracz {player} dołączył z uprawnieniem do cichego dołączania, bez wysyłania wiadomości o dołączeniu");
            put(Language.DA, "Spilleren {player} joinede med stille join tilladelsen, sender ikke join besked");
            put(Language.UK, "гравець {player} непомітно приєднався до сервера, без жодних повідомлень в чаті");
            put(Language.NB, "Spiller {player} koblet til med tillatelsen lydløs tilkobling, sender ikke en tilkoblingsmelding");
        }}), SILENT_QUIT(new HashMap<Language, String>() {{
            put(Language.EN, "Player {player} quit with silent quitting permission, not sending a quit message");
            put(Language.FR, "Le joueur {player} a quitté le jeu avec une permission de silence lors de le déconnexion.");
            put(Language.DE, "Spieler {player} hat den Server mit Berechtigung zum stillen Verlassen verlassen, es wird keine Nachricht gesendet");
            put(Language.JA, "プレイヤー {player} は discordsrv.silentquit の権限があるので、サーバー退出メッセージが送信されません");
            put(Language.KO, "플레이어 {player} 가 discordsrv.slientquit 퍼미션을 가지고 있습니다. 퇴장메세지를 보내지 않습니다.");
            put(Language.NL, "Speler {speler} is weg gegaan met toestemming om stil weg te gaan, geen quit bericht wordt verstuurd.");
            put(Language.ES, "Jugador {player} salió con el permiso de salida silenciosa, no se ha enviado un mensaje de salida");
            put(Language.RU, "Игрок {player} незаметно вышел, не попрощавшись, безо всяких сообщений в чате");
            put(Language.ET, "Mängija {player} lahkus vaikse lahkumise õigusega, lahkumissõnumit ei saadeta");
            put(Language.ZH, "玩家 {player} 使用靜默登出權限離開了伺服器，並未發送登出訊息。");
            put(Language.PL, "Gracz {player} wyszedł z uprawnieniem do cichego wyjścia, bez wysyłania wiadomości o wyjściu");
            put(Language.DA, "Spilleren {player} afsluttede med stille afslutning tilladelse, sender ikke afslutnings besked");
            put(Language.UK, "гравець {player} непомітно вийшов, не попрощавшись, без жодних повідомлень в чаті");
            put(Language.NB, "Spiller {player} frakoblet med tillatelsen lydløs frakobling, sender ikke en frakoblingsmelding");
        }}), LINKED_ACCOUNTS_SAVED(new HashMap<Language, String>() {{
            put(Language.EN, "Saved linked accounts in {ms}ms");
            put(Language.FR, "Sauvegarde des comptes liés en {ms}ms");
            put(Language.DE, "Speichern von verknüpften Accounts in {ms}ms");
            put(Language.JA, "{ms}ミリ秒でリンクされたアカウントを保存しました");
            put(Language.KO, "{ms}ms 만에 연동계정 저장완료");
            put(Language.NL, "Gekoppelde accounts opgeslagen in {ms}ms");
            put(Language.ES, "Cuentas vinculadas guardadas en {ms}ms");
            put(Language.RU, "Привязанные аккаунты успешно сохранены за {ms}мс");
            put(Language.ET, "Ühendatud kontod salvestati {ms}ms jooksul");
            put(Language.ZH, "已儲存已連結帳號，耗時{ms}ms");
            put(Language.PL, "Zapisane połączone konta w {ms}ms");
            put(Language.DA, "Gemte linkede brugere det tog {ms}ms");
            put(Language.UK, "Прив'язані акаунти успішно збережені за {ms}мс");
            put(Language.NB, "Lagret tilkoplede brukere på {ms} ms");
        }}), LINKED_ACCOUNTS_SAVE_FAILED(new HashMap<Language, String>() {{
            put(Language.EN, "Failed saving linked accounts");
            put(Language.FR, "Erreur lors de la sauvegarde des comptes liés");
            put(Language.DE, "Fehler beim Speichern von verknüpften Accounts");
            put(Language.JA, "リンクされたアカウントの保存に失敗しました");
            put(Language.KO, "연동계정 저장 실패");
            put(Language.NL, "Opslaan van gekoppelde accounts is mislukt");
            put(Language.ES, "Fallo al guardar las cuentas vinculadas");
            put(Language.RU, "Произошла ошибка сохранения привязанных аккаунтов");
            put(Language.ET, "Ühendatud kontode salvestamine ebaõnnestus");
            put(Language.ZH, "儲存已連結帳號失敗");
            put(Language.PL, "Nie udało się zapisać połączonych kont");
            put(Language.DA, "Fejlede at gemme linkede brugere");
            put(Language.UK, "Сталася помилка збереження прив'язаних акаунтів");
            put(Language.NB, "Kunne ikke lagre tilkoplede brukere");
        }}), FAILED_LOADING_PLUGIN(new HashMap<Language, String>() {{
            put(Language.EN, "Failed loading plugin");
            put(Language.FR, "Erreur lors du chargement du plugin");
            put(Language.DE, "Fehler beim Laden des Plugins");
            put(Language.JA, "プラグインの起動に失敗しました");
            put(Language.KO, "플러그인 로드 실패");
            put(Language.NL, "Gefaald om de plugin te laden.");
            put(Language.ES, "Fallo al cargar el plugin");
            put(Language.RU, "Ошибка загрузки плагина");
            put(Language.ET, "Plugina laadimine ebaõnnestus");
            put(Language.ZH, "讀取插件失敗");
            put(Language.PL, "Nie udało się załadować wtyczki");
            put(Language.DA, "Fejlede at loade plugin");
            put(Language.UK, "Помилка завантаження плагіна");
            put(Language.NB, "Kunne ikke laste utvidelse");
        }}), GROUP_SYNCHRONIZATION_COULD_NOT_FIND_ROLE(new HashMap<Language, String>() {{
            put(Language.EN, "Could not find role id {rolename} for use with group synchronization. Is the bot in the server?");
            put(Language.FR, "Impossible de trouver le rôle {rolename} lors de la synchronisation des groupes.Le bot est il sur le serveur ?");
            put(Language.DE, "Konnte keine Rolle mit id {rolename} für gruppensynchronisierung finden. Ist der Bot auf dem Server?");
            put(Language.JA, "グループを同期させるために、ID「{rolename}」のロールを見つけることができませんでした。 Botはサーバ上にありますか？");
            put(Language.KO, "그룹 동기화를 할 Role ID를 찾을 수 없습니다. 봇이 디스코드 서버에 있나요?");
            put(Language.NL, "Kon role id {rolename} niet vinden dit word gebruikt voor groep synchronisatie. Is de bot in de server?");
            put(Language.ES, "No se pudo encontrar el rol {rolename} para usar con sincronización de grupo. ¿Está el bot en el servidor?");
            put(Language.RU, "Не могу найти подходящий ID роли {rolename}, чтобы произвести синхронизацию. Бот точно уже подключился к серверу?");
            put(Language.ET, "Gruppide sünkroonimiseks vajalikku rolli ID-d {rolename} ei leitud. Kas bot on serveris?");
            put(Language.ZH, "未能找到身分組 {rolename} 來進行群組同步。 請確認Bot是否有在伺服器中。");
            put(Language.PL, "Nie udało się znaleźć identyfikatora roli {rolename} do użytku z synchronizacją grupową. Czy bot jest na serwerze?");
            put(Language.DA, "Kunne ikke finde rolle id {rolename} til brug af gruppe synkronisation. Er botten i serveren?");
            put(Language.UK, "Не можу знайти відповідний ID ролі {rolename}, щоб зробити синхронізацію. Бот точно вже підключився до сервера?");
            put(Language.NB, "Kunne ikke finne rolle-ID {rolename} for bruk av gruppesynkronisering. Er boten med i serveren?");
        }}), NO_MESSAGE_GIVEN_TO_BROADCAST(new HashMap<Language, String>() {{
            put(Language.EN, "No text given to broadcast");
            put(Language.FR, "Aucune langue donnée à diffuser");
            put(Language.DE, "Keine Sprache für Broadcast angegeben");
            put(Language.JA, "ブロードキャストするメッセージが指定されていません。");
            put(Language.KO, "방송할 언어가 주어지지 않았습니다.");
            put(Language.NL, "Geen taal is opgegeven om uit te zenden.");
            put(Language.ES, "Ningún idioma dado para transmitir");
            put(Language.RU, "Не найден подходящий язык для отправки уведомлений");
            put(Language.ET, "Teadaande saatmiseks ei määratud keelt");
            put(Language.ZH, "未給廣播指定語言");
            put(Language.PL, "Brak tekstu do wysłania");
            put(Language.DA, "Ingen text givet til broadcast");
            put(Language.UK, "не знайдено відповідної мови для надсилання сповіщень");
            put(Language.NB, "Det er ikke gitt noen tekst til sending");
        }}), PLAYER_ONLY_COMMAND(new HashMap<Language, String>() {{
            put(Language.EN, "Only players can execute this command.");
            put(Language.FR, "Seuls les joueurs peuvent effectuer cette commande.");
            put(Language.DE, "Nur Spieler können diesen Befehl ausführen.");
            put(Language.JA, "ゲーム内プレイヤーのみがこのコマンドを実行することができます。");
            put(Language.KO, "플레이어만 이 명령어를 실행 할 수 있습이다.");
            put(Language.NL, "Alleen spelers kunnen dit command gebruiken.");
            put(Language.ES, "Solo los jugadores pueden ejecutar este comando");
            put(Language.RU, "Только игроки могут выполнить такую команду.");
            put(Language.ET, "Ainult mängijad saavad seda käsklust teostada.");
            put(Language.ZH, "只有玩家能執行這個指令");
            put(Language.PL, "Tylko gracze mogą wykonać to polecenie.");
            put(Language.DA, "Kun spillere kan eksekvere denne kommando.");
            put(Language.UK, "Тільки гравці можуть виконати таку команду.");
            put(Language.NB, "Bare spillere kan utføre denne kommandoen.");
        }}), RELOADED(new HashMap<Language, String>() {{
            put(Language.EN, "The DiscordSRV config & lang have been reloaded.");
            put(Language.FR, "La configuration et les fichiers de langage de DiscordSRV ont été rechargé.");
            put(Language.DE, "Die DiscordSRV Konfiguration und Sprachdatei wurden neu eingelesen.");
            put(Language.JA, "DiscordSRVの設定と言語が再読込されました。");
            put(Language.KO, "DiscordSRV 컨피그 및 언어 설정이 리로드 되었습니다.");
            put(Language.NL, "De DiscordSRV config & lang is herladen.");
            put(Language.ES, "La configuración y el idioma de DiscordSRV han sido recargadas");
            put(Language.RU, "DiscordSRV конфигурация и языковые настройки успешно перезагружены.");
            put(Language.ET, "DiscordSRV seadistus ja keel on uuesti laaditud.");
            put(Language.ZH, "DiscordSRV的設定檔與詞條已重新讀取。");
            put(Language.PL, "Konfiguracja i język DiscordSRV zostały ponownie załadowane.");
            put(Language.DA, "DiscordSRV konfigurationen & sprog er blevet genstartet.");
            put(Language.UK, "DiscordSRV конфігурація та налаштування мови успішно перезавантажені.");
            put(Language.NB, "DiscordSRVs konfigurasjons- og språkfil har blitt lastet om på nytt");
        }}), NO_UNLINK_TARGET_SPECIFIED(new HashMap<Language, String>() {{
            put(Language.EN, "No player specified. It can be a player UUID, player name, or Discord ID.");
            put(Language.FR, "Aucune cible spécifiée. Peut être un UUID, un ID Discord ou un nom de joueur.");
            put(Language.DE, "Kein Spieler angegeben. Dies kann eine UUID, ein Spielername oder eine Discord-ID sein.");
            put(Language.JA, "プレーヤーが指定されていません。これは、UUID、プレーヤー名、またはDiscord IDです。");
            put(Language.KO, "대상이 지정되지 않았습니다. 플레이어 UUID, 플레이어 이름 또는 Discord ID 일 수 있습니다.");
            put(Language.NL, "U moet opgeven wie u wilt ontkoppelen. Het kan een UUID, een Discord-ID of een spelersnaam zijn.");
            put(Language.ES, "Ningún objetivo especificado. Puede ser un UUID, una ID de Discord o un nombre de jugador.");
            put(Language.RU, "Ни один игрок не указан. Это может быть UUID, имя игрока или Discord ID.");
            put(Language.ET, "Ühtegi mängijat pole täpsustatud. See võib olla mängija UUID, mängija nimi või Discord ID.");
            put(Language.ZH, "沒有玩家指定。這可能是玩家的UUID，玩家名稱或Discord ID。");
            put(Language.PL, "Nie określono gracza. Może to być identyfikator UUID gracza, nazwa gracza lub identyfikator Discord.");
            put(Language.DA, "Ingen spiller specificeret. Det kan være en spillers UUID, spillernavn, eller Discord ID.");
            put(Language.UK, "Жоден гравець не вказаний. Це може бути UUID, ім'я гравця або Discord ID.");
            put(Language.NB, "Ingen spiller er spesifisert. Det kan være en spiller-UUID, spillernavn eller Discord-ID.");
        }}), COMMAND_EXCEPTION(new HashMap<Language, String>() {{
            put(Language.EN, "An internal error occurred while while processing your command.");
            put(Language.FR, "Une erreur interne š'est produite lors du traitement.");
            put(Language.DE, "Während der Verarbeitung Ihres Befehls ist ein interner Fehler aufgetreten.");
            put(Language.JA, "コマンドの処理中に内部エラーが発生しました。");
            put(Language.KO, "명령을 처리하는 중 내부 오류가 발생했습니다.");
            put(Language.NL, "Een interne fout is opgetreden tijdens het uitvoeren van jouw opdracht.");
            put(Language.ES, "Se produjo un error interno al procesar su comando.");
            put(Language.RU, "Во время обработки вашей команды произошла внутренняя ошибка.");
            put(Language.ET, "Käskluse töötlemisel esines sisemine viga.");
            put(Language.ZH, "處理命令時發生內部錯誤。");
            put(Language.PL, "Podczas przetwarzania polecenia wystąpił błąd wewnętrzny.");
            put(Language.DA, "En intern fejl fandt sted imens den behandlede din kommando.");
            put(Language.UK, "під час обробки вашої команди сталася Внутрішня помилка.");
            put(Language.NB, "En intern feil oppstod under behandlingen av kommandoen din.");
        }}), RESYNC_WHEN_GROUP_SYNC_DISABLED(new HashMap<Language, String>() {{
            put(Language.EN, "Group synchonization requires valid GroupRoleSynchronizationGroupsAndRolesToSync entries in synchronization.yml");
            put(Language.FR, "La synchronisation de groupe nécessite des entrées GroupRoleSynchronizationGroupsAndRolesToSync valides dans synchronization.yml");
            put(Language.DE, "Für die Gruppensynchronisierung sind gültige GroupRoleSynchronizationGroupsAndRolesToSync-Einträge in synchronization.yml erforderlich");
            put(Language.JA, "グループの同期には、synchronization.ymlの有効なGroupRoleSynchronizationGroupsAndRolesToSyncエントリが必要です。");
            put(Language.KO, "그룹 동기화에는 동기화에 유효한 GroupRoleSynchronizationGroupsAndRolesToSync 항목이 synchronization.yml 합니다.");
            put(Language.NL, "Groepsynchronisatie vereist geldige GroupRoleSynchronizationGroupsAndRolesToSync-vermeldingen in synchronization.yml");
            put(Language.ES, "La sincronización de grupo requiere entradas válidas de GroupRoleSynchronizationGroupsAndRolesToSync en synchronization.yml");
            put(Language.RU, "Синхронизация группы требует допустимых записей GroupRoleSynchronizationGroupsAndRolesToSync в synchronization.yml");
            put(Language.ET, "Grupi sünkroonimiseks on vaja kehtivaid GroupRoleSynchronizationGroupsAndRolesToSync kirjeid failis synchronization.yml");
            put(Language.ZH, "群組同步需要在synchronization.yml中有效的GroupRoleSynchronizationGroupsAndRolesToSync條目");
            put(Language.PL, "Synchronizacja grupowa wymaga ważnego GroupRoleSynchronizationGroupsAndRolesToSync wpisu w synchronization.yml");
            put(Language.DA, "Gruppe synkronisation kræver valid GroupRoleSynchronizationGroupsAndRolesToSync entréer i synchronization.yml");
            put(Language.UK, "Синхронізація групи вимагає дійсних записів GroupRoleSynchronizationGroupsAndRolesToSync у synchronization.yml");
            put(Language.NB, "Gruppesynkronisering krever gyldig GroupRoleSynchronizationGroupsAndRolesToSync-oppføringer i synchronization.yml");
        }}), PLUGIN_RELOADED(new HashMap<Language, String>() {{
            put(Language.EN, "DiscordSRV has been reloaded. This is NOT supported, and issues WILL occur! Restart your server before asking for support!");
            put(Language.FR, "DiscordSRV a été rechargé. Ceci n'est PAS pris en charge et des problèmes surviendront! Redémarrez votre serveur avant de demander de l'aide!");
            put(Language.DE, "DiscordSRV wurde neu geladen. Dies wird NICHT unterstützt und es treten Probleme auf! Starten Sie Ihren Server neu, bevor Sie um Unterstützung bitten!");
            put(Language.JA, "DiscordSRVがリロードされました。 これはサポートされておらず、問題が発生します！ サポートを求める前にサーバーを再起動してください！");
            put(Language.KO, "DiscordSRV가 다시로드되었습니다. 이것은 지원되지 않으며 문제가 발생합니다! 지원을 요청하기 전에 서버를 다시 시작하십시오!");
            put(Language.NL, "DiscordSRV is opnieuw geladen. Dit wordt NIET ondersteund en er ZULLEN problemen optreden! Start uw server opnieuw op voordat u om ondersteuning vraagt!");
            put(Language.ES, "DiscordSRV ha sido recargado. ¡Esto NO es compatible, y OCURRIRÁN problemas! ¡Reinicie su servidor antes de solicitar asistencia!");
            put(Language.RU, "DiscordSRV был перезагружен. Это НЕ поддерживается, и проблемы будут происходить! Перезагрузите сервер, прежде чем обращаться за поддержкой!");
            put(Language.ET, "DiscordSRV on taaslaaditud. See EI OLE toetatud ning probleemid ESINEVAD kindlalt! Enne toe küsimist taaskäivita oma server!");
            put(Language.ZH, "DiscordSRV已重新載入。 不支持此功能，並且會發生問題！ 在尋求支持之前，請重新啟動伺服器！");
            put(Language.PL, "DiscordSRV został ponownie załadowany. To NIE jest obsługiwane i pojawią się problemy! Zrestartuj serwer, zanim poprosisz o wsparcie!");
            put(Language.DA, "DiscordSRV er blevet genladet. Dette er IKKE understøttet, og problemer VIL OPSTÅ! Genstart din server før du spørger om hjælp!");
            put(Language.UK, "DiscordSRV було перезавантажено. Це не підтримується, і проблеми будуть відбуватися! Перезавантажте сервер, перш ніж звертатися за підтримкою!");
            put(Language.NB, "DiscordSRV har blitt lastet om. Dette er IKKE støttet og feil VIL oppstå! Restart serveren din før du ber om støtte.");
        }});

        @Getter private final Map<Language, String> definitions;
        InternalMessage(Map<Language, String> definitions) {
            this.definitions = definitions;
        }

        @Override
        public String toString() {
            return definitions.getOrDefault(DiscordSRV.config().getLanguage(), definitions.get(Language.EN));
        }

    }

    /**
     * Messages external to DiscordSRV and thus can be customized in messages.yml
     */
    public enum Message {

        ACCOUNT_ALREADY_LINKED("MinecraftAccountAlreadyLinked", true),
        ALREADY_LINKED("DiscordAccountAlreadyLinked", false),
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
        CHAT_TO_MINECRAFT_REPLY("DiscordToMinecraftMessageReplyFormat", true),
        CODE_GENERATED("CodeGenerated", false), // colors translated with kyori
        CLICK_TO_COPY_CODE("ClickToCopyCode", false), // colors translated with kyori
        COMMAND_DOESNT_EXIST("UnknownCommandMessage", true),
        CONSOLE_CHANNEL_PREFIX("DiscordConsoleChannelPrefix", false),
        CONSOLE_CHANNEL_SUFFIX("DiscordConsoleChannelSuffix", false),
        CONSOLE_CHANNEL_TOPIC("ChannelTopicUpdaterConsoleChannelTopicFormat", false),
        CONSOLE_CHANNEL_TOPIC_AT_SERVER_SHUTDOWN("ChannelTopicUpdaterConsoleChannelTopicAtServerShutdownFormat", false),
        DISCORD_ACCOUNT_LINKED("DiscordAccountLinked", false),
        DISCORD_COMMAND("DiscordCommandFormat", true),
        DYNMAP_CHAT_FORMAT("DynmapChatFormat", true),
        DYNMAP_DISCORD_FORMAT("DynmapDiscordFormat", false),
        DYNMAP_NAME_FORMAT("DynmapNameFormat", true),
        FAILED_TO_CHECK_LINKED_ACCOUNT("DiscordLinkedAccountCheckFailed", false),
        INVALID_CODE("InvalidCode", false),
        LINKED_SUCCESS("LinkedCommandSuccess", true),
        LINKED_ACCOUNT_REQUIRED("DiscordLinkedAccountRequired", false),
        LINKED_NOBODY_FOUND("MinecraftNobodyFound", true),
        LINK_FAIL_NOT_ASSOCIATED_WITH_AN_ACCOUNT("MinecraftNoLinkedAccount", true),
        MINECRAFT_ACCOUNT_LINKED("MinecraftAccountLinked", true),
        NO_PERMISSION("NoPermissionMessage", true),
        PLAYER_LIST_COMMAND("DiscordChatChannelListCommandFormatOnlinePlayers", false),
        PLAYER_LIST_COMMAND_NO_PLAYERS("DiscordChatChannelListCommandFormatNoOnlinePlayers", false),
        PLAYER_LIST_COMMAND_PLAYER("DiscordChatChannelListCommandPlayerFormat", true),
        PLAYER_LIST_COMMAND_ALL_PLAYERS_SEPARATOR("DiscordChatChannelListCommandAllPlayersSeparator", false),
        SERVER_SHUTDOWN_MESSAGE("DiscordChatChannelServerShutdownMessage", false),
        SERVER_STARTUP_MESSAGE("DiscordChatChannelServerStartupMessage", false),
        SERVER_WATCHDOG("ServerWatchdogMessage", false),
        UNABLE_TO_LINK_ACCOUNTS_RIGHT_NOW("LinkingError", true),
        UNKNOWN_CODE("UnknownCode", false),
        UNLINK_SUCCESS("UnlinkCommandSuccess", true);

        @Getter private final String keyName;
        @Getter private final boolean translateColors;

        Message(String keyName, boolean translateColors) {
            this.keyName = keyName;
            this.translateColors = translateColors;
        }

        @Override
        public String toString() {
            return toString(translateColors);
        }

        public String toString(boolean translateColors) {
            String message = DiscordSRV.config().getString(this.keyName);
            return translateColors ? MessageUtil.translateLegacy(message) : message;
        }

    }

}

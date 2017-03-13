package github.scarsz.discordsrv.objects;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.commands.Command;
import github.scarsz.discordsrv.util.GamePermissionUtil;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/12/2017
 * @at 1:02 PM
 */
public class CommandManager {

    @Getter private Map<String, Method> commands = new HashMap<>();

    public CommandManager() {
        Pattern sysLibPattern = Pattern.compile(".*[.](so|dll)", Pattern.CASE_INSENSITIVE);
        ConfigurationBuilder builder = new ConfigurationBuilder().setScanners(new SubTypesScanner(false), new ResourcesScanner()).setUrls(ClasspathHelper.forClassLoader(Arrays.asList(ClasspathHelper.contextClassLoader(), ClasspathHelper.staticClassLoader()).toArray(new ClassLoader[0]))).filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix("github.scarsz.discordsrv.commands")));
        builder.setUrls(builder.getUrls().stream().filter(url -> sysLibPattern.matcher(url.getFile()).matches()).collect(Collectors.toList()));
        Reflections reflections = new Reflections(builder);

        for (String className : reflections.getAllTypes()) {
            Class clazz; try { clazz = Class.forName(className); } catch (ClassNotFoundException ignored) { continue; }

            if (clazz.isAnnotation()) continue;

            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(Command.class)) continue; // make sure method is marked as an annotation

                if (method.getParameters().length != 2) {
                    DiscordSRV.debug("Method " + method.toGenericString().replace("public static void ", "") + " annotated as command but parameters count != 2");
                    continue;
                }
                if (method.getParameters()[0].getType() != CommandSender.class && method.getParameters()[0].getType() != Player.class) {
                    DiscordSRV.debug("Method " + method.toGenericString().replace("public static void ", "") + " annotated as command but parameter 1's type != CommandSender || Player");
                    continue;
                }
                if (method.getParameters()[1].getType() != String[].class) {
                    DiscordSRV.debug("Method " + method.toGenericString().replace("public static void ", "") + " annotated as command but parameter 2's type != String[]");
                    continue;
                }

                Command annotation = method.getAnnotation(Command.class);
                for (String commandName : annotation.commandNames()) commands.put(commandName.toLowerCase(), method);
            }
        }
    }

    public boolean handle(CommandSender sender, String command, String[] args) {
        if (command == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', DiscordSRV.getPlugin().getConfig().getString("DiscordCommandFormat")));
            return true;
        }

        if (commands.containsKey(command.toLowerCase())) {
            try {
                Method commandMethod = commands.get(command.toLowerCase());
                Command commandAnnotation = commandMethod.getAnnotation(Command.class);

                if (!GamePermissionUtil.hasPermission(sender, commandAnnotation.permission())) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to perform this command.");
                    return true;
                }

                if (commandMethod.getParameters()[0].getType() == Player.class && !(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can execute this command.");
                    return true;
                }

                commandMethod.invoke(null, sender, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            sender.sendMessage(ChatColor.AQUA + "That command doesn't exist!");
        }

        return true;
    }

}

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

package github.scarsz.discordsrv.util;

import org.apache.commons.codec.binary.Base64;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NMSUtil {

    private static final Pattern TEXTURE_URL_PATTERN = Pattern.compile("https?://.+(?<texture>\\w{64})\"");

    protected static String versionPrefix = "";
    protected static boolean failed = false;

    protected static Class<?> class_CraftPlayer;
    protected static Class<?> class_GameProfile;
    protected static Class<?> class_GameProfileProperty;
    protected static Class<?> class_EntityPlayer;
    protected static Method method_CraftPlayer_getHandle;
    protected static Method method_EntityPlayer_getGameProfile;
    protected static Method method_GameProfile_getProperties;
    protected static Field field_PropertyMap_properties;
    protected static Field field_GameProfileProperty_value;

    static {
        String className = Bukkit.getServer().getClass().getName();
        String[] packages = className.split("\\.");
        if (packages.length == 5) {
            versionPrefix = packages[3] + ".";
        }

        try {
            class_EntityPlayer = fixBukkitClass("net.minecraft.server.EntityPlayer", "net.minecraft.server.level.EntityPlayer");
            try {
                method_EntityPlayer_getGameProfile = class_EntityPlayer.getMethod("getProfile");
            } catch (NoSuchMethodException e) {
                try {
                    method_EntityPlayer_getGameProfile = class_EntityPlayer.getMethod("getGameProfile");
                } catch (NoSuchMethodException e2) {
                    method_EntityPlayer_getGameProfile = Arrays.stream(class_EntityPlayer.getMethods())
                            .filter(method -> method.getReturnType().getSimpleName().equals("GameProfile"))
                            .findFirst().orElseThrow(() -> new RuntimeException("Couldn't find the GameProfile method"));
                }
            }

            class_CraftPlayer = fixBukkitClass("org.bukkit.craftbukkit.entity.CraftPlayer");
            method_CraftPlayer_getHandle = class_CraftPlayer.getMethod("getHandle");

            class_GameProfile = getClass("com.mojang.authlib.GameProfile");
            class_GameProfileProperty = getClass("com.mojang.authlib.properties.Property");
            if (class_GameProfile == null) {
                class_GameProfile = getClass("net.minecraft.util.com.mojang.authlib.GameProfile");
                class_GameProfileProperty = getClass("net.minecraft.util.com.mojang.authlib.properties.Property");
            }
            method_GameProfile_getProperties = class_GameProfile.getMethod("getProperties");
            field_GameProfileProperty_value = class_GameProfileProperty.getDeclaredField("value");
            field_GameProfileProperty_value.setAccessible(true);
            field_PropertyMap_properties = method_GameProfile_getProperties.getReturnType().getDeclaredField("properties");
            field_PropertyMap_properties.setAccessible(true);
        } catch (Throwable e) {
            e.printStackTrace();
            failed = true;
        }
    }

    public static Class<?> getClass(String className) {
        Class<?> result = null;
        try {
            result = NMSUtil.class.getClassLoader().loadClass(className);
        } catch (Exception ignored) {}
        return result;
    }

    public static Class<?> fixBukkitClass(String className, String... alternateClassNames) throws ClassNotFoundException {
        List<String> classNames = new ArrayList<>();
        classNames.add(className);
        classNames.addAll(Arrays.asList(alternateClassNames));

        for (String name : classNames) {
            try {
                // Try without prefix, Spigot 1.17
                return NMSUtil.class.getClassLoader().loadClass(name);
            } catch (ClassNotFoundException ignored) {}

            if (!versionPrefix.isEmpty()) {
                name = name.replace("org.bukkit.craftbukkit.", "org.bukkit.craftbukkit." + versionPrefix);
                name = name.replace("net.minecraft.server.", "net.minecraft.server." + versionPrefix);
            }

            try {
                return NMSUtil.class.getClassLoader().loadClass(name);
            } catch (ClassNotFoundException ignored) {}
        }
        throw new ClassNotFoundException("Could not find " + className);
    }

    public static Object getHandle(Player player) {
        if (failed) return null;

        try {
            return method_CraftPlayer_getHandle.invoke(player);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getGameProfile(Player player) {
        if (failed) return null;

        Object handle = getHandle(player);
        if (handle != null) {
            try {
                return method_EntityPlayer_getGameProfile.invoke(handle);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static Object getTextureProperty(Object propertyMap) {
        if (failed) return null;

        try {
            Object multi = NMSUtil.field_PropertyMap_properties.get(propertyMap);
            //noinspection rawtypes
            Iterator it = ((Iterable) multi.getClass()
                    .getMethod("get", Object.class)
                    .invoke(multi, "textures")).iterator();
            if (it.hasNext()) {
                return it.next();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getTexture(Player player) {
        if (failed) return null;

        try {
            Object profile = getGameProfile(player);
            if (profile == null) return null;
            Object propertyMap = method_GameProfile_getProperties.invoke(profile);
            Object textureProperty = getTextureProperty(propertyMap);
            if (textureProperty != null) {
                String textureB64 = (String) field_GameProfileProperty_value.get(textureProperty);
                String textureData = new String(Base64.decodeBase64(textureB64));
                Matcher matcher = TEXTURE_URL_PATTERN.matcher(textureData);
                if (matcher.find()) return matcher.group("texture");
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

}

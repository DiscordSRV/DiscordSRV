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

package github.scarsz.discordsrv;

import alexh.weak.Dynamic;
import github.scarsz.configuralize.Language;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class AllConfigsHaveKeysTest {

    private final File resources = new File("src/main/resources");
    private final Yaml yaml = new Yaml();

    //@Test
    public void test() {
        List<File> configs = Arrays.asList(
                new File(resources, "config")
//                new File(resources, "messages")
//                new File(resources, "linking"),
//                new File(resources, "voice")
        );

        for (File parent : configs) {
            try {
                Dynamic english = Dynamic.from(yaml.loadAs(FileUtils.readFileToString(new File(parent, "en.yml"), "UTF-8"), Map.class));
                english.allChildren()
                        .filter(d -> d.getClass().getSimpleName().equals("Child"))
                        .filter(d -> d.children().count() == 0)
                        .map(d -> {
                            String almostClean = d.key().toString()
                                    .substring(6)
                                    .replace("->", ".");
                            do {
                                almostClean = almostClean.substring(0, almostClean.lastIndexOf('.'));
                            } while (almostClean.matches(".+\\.\\d+$"));
                            return almostClean;
                        })
                        .distinct()
                        .filter(StringUtils::isNotBlank)
                        .forEach(System.out::println);

                for (Language language : Language.values()) {
                    if (language == Language.EN) continue;
                    File file = new File(parent, language.getCode().toLowerCase() + ".yml");
                    if (!file.exists()) continue;
                    Map<String, Object> other = yaml.loadAs(FileUtils.readFileToString(file, "UTF-8"), Map.class);

//                    for (String key : english.keySet()) {
//                        Assert.assertTrue(String.format("%s %s config (%s/%s.yml) does not contain key %s",
//                                language.getName(), parent.getName(), parent.getName(), language.getCode().toLowerCase(), key),
//                                other.containsKey(key)
//                        );
//                    }
//
//                    other.keySet().removeAll(english.keySet());
//                    for (String key : other.keySet()) {
//                        Assert.fail(String.format("%s %s config (%s/%s.yml) has unused key %s",
//                                language.getName(), parent.getName(), parent.getName(), language.getCode().toLowerCase(), key)
//                        );
//                    }
                }
            } catch (IOException e) {
                System.out.println("Failed to read file " + parent.getPath() + ": " + e.getMessage());
            }
        }
    }

}

package github.scarsz.discordsrv.objects;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class MessageFormat {

    private String content;

    private String authorName;
    private String authorUrl;
    private String authorImageUrl;
    private String thumbnailUrl;
    private String title;
    private String titleUrl;
    private String description;
    private String imageUrl;
    private Instant timestamp;
    private Color color;
    private List<MessageEmbed.Field> fields;
    private MessageEmbed.Footer footer;

    public boolean isAnyContent() {
        return content != null || authorName != null || authorUrl != null || authorImageUrl != null
                || thumbnailUrl != null || title != null || titleUrl != null || description != null
                || imageUrl != null || fields != null || footer != null;
    }

    public int getTextLength() {
        String contentSum = content + authorName + title + description
                + (footer != null ? footer.getText() != null ? footer.getText() : "" : "")
                + fields.stream().map(field -> (field.getName() != null ? field.getName() : "")
                + (field.getValue() != null ? field.getValue() : "")).collect(Collectors.joining());
        contentSum = contentSum.replaceAll("[^A-z]", "");
        return contentSum.length();
    }
}

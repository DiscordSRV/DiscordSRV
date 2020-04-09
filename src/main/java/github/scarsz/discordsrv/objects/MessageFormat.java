package github.scarsz.discordsrv.objects;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
public class MessageFormat {

    // Regular message
    private String content;

    // Embed contents
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

    // Webhook capabilities
    private boolean useWebhooks;
    private String webhookAvatarUrl;
    private String webhookName;

    public boolean isAnyContent() {
        return content != null || authorName != null || authorUrl != null || authorImageUrl != null
                || thumbnailUrl != null || title != null || titleUrl != null || description != null
                || imageUrl != null || fields != null || footer != null;
    }
}

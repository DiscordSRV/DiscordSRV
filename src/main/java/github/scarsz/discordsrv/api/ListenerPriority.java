package github.scarsz.discordsrv.api;

import lombok.Getter;

/**
 * <p>Completely inspired by the Bukkit API's EventPriority system</p>
 * <p>Event priorities mean the same as Bukkit's; it's in a separate enum to prevent
 * depending on Bukkit classes when they might not be available</p>
 * <p>Defaults to {@link #NORMAL} in {@link Subscribe} annotations where it's not specifically set</p>
 */
public enum ListenerPriority {

    /**
     * Event call is of very low importance and should be ran first, to allow
     * other plugins to further customise the outcome
     */
    LOWEST(0),
    /**
     * Event call is of low importance
     */
    LOW(1),
    /**
     * Event call is neither important nor unimportant, and may be ran
     * normally
     */
    NORMAL(2),
    /**
     * Event call is of high importance
     */
    HIGH(3),
    /**
     * Event call is critical and must have the final say in what happens
     * to the event
     */
    HIGHEST(4),
    /**
     * Event is listened to purely for monitoring the outcome of an event.
     * <p>
     * No modifications to the event should be made under this priority
     */
    MONITOR(5);

    @Getter private final int slot;
    ListenerPriority(int slot) {
        this.slot = slot;
    }

}
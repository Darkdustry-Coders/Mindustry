package mindustry;

import arc.Events;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.game.EventType;

public class MdUtil {
    private MdUtil() {}

    public static float xcapdist(float x, float y, float xorigin, float yorigin, float max) {
        float dst = Math.min(Mathf.dst(x, y, xorigin, yorigin), max);
        float angle = Mathf.atan2(x - xorigin, y - yorigin);
        return Mathf.cos(angle) * dst + xorigin;
    }
    public static float ycapdist(float x, float y, float xorigin, float yorigin, float max) {
        float dst = Math.min(Mathf.dst(x, y, xorigin, yorigin), max);
        float angle = Mathf.atan2(x - xorigin, y - yorigin);
        return Mathf.sin(angle) * dst + yorigin;
    }

    public static float blackBox(float x) { return x; }

    public static long nowNanos = Long.MIN_VALUE;

    public static void init() {
        Events.run(EventType.Trigger.update, () -> nowNanos = Time.nanos());
    }
}

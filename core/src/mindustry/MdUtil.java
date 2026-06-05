package mindustry;

import arc.math.Mathf;

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
}

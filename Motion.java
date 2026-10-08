package dev.lumina.anim;

import dev.lumina.setting.Settings;

/** Global animation switchboard. Everything animated asks this class for the speed factor. */
public final class Motion {
    private Motion() {}

    /** 0 means "no animation, jump straight to the target". */
    public static float speed() {
        if (Settings.REDUCE_MOTION.get() || !Settings.ANIM_ENABLED.get()) return 0f;
        return Settings.ANIM_SPEED.getF();
    }

    public static boolean on() { return speed() > 0f; }

    public static float outCubic(float t) {
        float u = 1f - Math.max(0f, Math.min(1f, t));
        return 1f - u * u * u;
    }

    public static float inCubic(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * t;
    }
}

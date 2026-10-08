package dev.lumina.anim;

/** A value that eases towards a target using frame-rate independent exponential smoothing. */
public final class Anim {
    public float value;
    public float target;
    private final float rate;

    public Anim(float start) { this(start, 14f); }

    public Anim(float start, float rate) {
        this.value = start;
        this.target = start;
        this.rate = rate;
    }

    public void snap(float v) {
        value = v;
        target = v;
    }

    public void update(float dt) {
        float speed = Motion.speed();
        if (speed <= 0f) {
            value = target;
            return;
        }
        value += (target - value) * (1f - (float) Math.exp(-dt * rate * speed));
        if (Math.abs(target - value) < 0.002f) value = target;
    }

    public boolean settled() { return value == target; }
}

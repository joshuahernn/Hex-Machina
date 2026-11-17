/**
 * Every effect sprite sheet: sword slashes, punches, bolts, arrows, impacts, the charge-ups.
 *
 * Most sheets come from pixelfx (https://github.com/patch-gg/pixelfx, MIT), recolored to the
 * game's palette by sprites/snapshot_v8/make_effects.py; the numbers here are copied from the
 * effects.json it writes. Frames run left to right, top to bottom.
 *
 * Every effect points RIGHT and is rotated to the direction of the attack when drawn. The
 * pivot is the point of the frame that sits on the attacker (or on the thing hit, for an
 * impact), measured from the frame's top-left corner.
 */
public enum EffectType {
    // melee: the pivot is raised from the feet to the chest so the arc swings round the hero
    // They play faster than they were exported, so the slash is gone about when the blade stops.
    SLASH_LIGHT("effects/slash_light.png", 136, 104, 4, 16, 50, false, 44, 72),
    SLASH_CROSS("effects/slash_cross.png", 164, 108, 4, 17, 36, false, 44, 72),
    SLASH_HEX("effects/slash_hex.png", 136, 104, 4, 16, 50, false, 44, 72),
    // the iron sword's crossing cuts in hex light, drawn a quarter bigger, with lightning and a wave
    SLASH_HEX_HEAVY("effects/slash_hex_heavy.png", 164, 108, 4, 17, 36, false, 44, 72, 1.25f),
    SLASH_MACHINA("effects/slash_machina.png", 136, 104, 4, 16, 50, false, 44, 72),
    // a flurry of cuts ending in one big one
    SLASH_MACHINA_HEAVY("effects/slash_machina_heavy.png", 148, 104, 4, 20, 30, false, 36, 66, 1.25f),
    // bare hands: small bursts where the fist lands, drawn by make_effects.py; the pivot is the middle
    PUNCH_LIGHT("effects/punch_light.png", 17, 17, 5, 5, 24, false, 8, 8),
    PUNCH_HEAVY("effects/punch_heavy.png", 27, 27, 7, 7, 24, false, 13, 13),
    // impacts
    SPARK_STEEL("effects/spark_steel.png", 136, 80, 3, 11, 20, false, 44, 64),
    BURST_CYAN("effects/burst_cyan.png", 136, 80, 3, 11, 20, false, 44, 64),
    BURST_MAGENTA("effects/burst_magenta.png", 136, 80, 3, 11, 20, false, 44, 64),
    BURST_RED("effects/burst_red.png", 136, 80, 3, 11, 20, false, 44, 64),
    // things in flight: the pivot is the tip
    ARROW("effects/arrow.png", 16, 5, 1, 1, 1, true, 15, 2),
    ARROW_HEX("effects/arrow_hex.png", 16, 5, 1, 1, 1, true, 15, 2),
    ARROW_MACHINA("effects/arrow_machina.png", 16, 5, 1, 1, 1, true, 15, 2),
    // a charged arrow: the same arrow with its head wrapped in flickering light
    ARROW_CHARGED("effects/arrow_charged.png", 24, 9, 4, 4, 20, true, 21, 4),
    ARROW_HEX_CHARGED("effects/arrow_hex_charged.png", 24, 9, 4, 4, 20, true, 21, 4),
    ARROW_MACHINA_CHARGED("effects/arrow_machina_charged.png", 24, 9, 4, 4, 20, true, 21, 4),
    BOLT_CYAN("effects/bolt_cyan.png", 144, 60, 3, 16, 20, true, 108, 30),
    DISC_CYAN("effects/disc_cyan.png", 120, 52, 3, 10, 20, true, 92, 26),
    DART_MAGENTA("effects/dart_magenta.png", 128, 32, 2, 10, 20, true, 108, 16),
    // Every staff shot is its own effect, not another's in a different colour. The crystal and
    // the molten core are drawn by make_effects.py; their pivot is the middle of the crystal or core.
    CRYSTAL_MAGENTA("effects/crystal_magenta.png", 44, 23, 8, 8, 20, true, 32, 11),
    SLUG_RED("effects/slug_red.png", 200, 40, 2, 12, 20, true, 184, 18),
    CORE_RED("effects/core_red.png", 50, 25, 8, 8, 20, true, 37, 12),
    // the streak that follows a charged arrow, a little thicker than the arrow at its head and
    // thinning out behind; drawn by make_effects.py, not pixelfx
    TRAIL_EMBER("effects/trail_ember.png", 38, 7, 6, 6, 20, true, 37, 3),
    TRAIL_CYAN("effects/trail_cyan.png", 38, 7, 6, 6, 20, true, 37, 3),
    TRAIL_RED("effects/trail_red.png", 38, 7, 6, 6, 20, true, 37, 3),
    // charge-ups, one look per kind of weapon. Staff: the pivot is the middle of the gathering orb.
    CHARGE("effects/charge.png", 120, 86, 5, 32, 20, true, 58, 56, 0.4f),
    CHARGE_MAGENTA("effects/charge_magenta.png", 120, 86, 5, 32, 20, true, 58, 56, 0.4f),
    CHARGE_RED("effects/charge_red.png", 120, 86, 5, 32, 20, true, 58, 56, 0.4f),
    // sword: light running up the blade, from the hand (the pivot) to the point, and sparks coming off it
    // Each tier has more of them than the last, and the better two crackle with lightning.
    EDGE_STEEL("effects/edge_steel.png", 30, 15, 12, 12, 20, true, 2, 7),
    EDGE_CYAN("effects/edge_cyan.png", 30, 15, 12, 12, 20, true, 2, 7),
    EDGE_RED("effects/edge_red.png", 30, 15, 12, 12, 20, true, 2, 7),
    // bow: streaks tightening onto the nocked arrow
    DRAW_EMBER("effects/draw_ember.png", 33, 33, 10, 10, 20, true, 16, 16),
    DRAW_CYAN("effects/draw_cyan.png", 33, 33, 10, 10, 20, true, 16, 16),
    DRAW_RED("effects/draw_red.png", 33, 33, 10, 10, 20, true, 16, 16),
    // the ring that pops off a weapon the moment its charge is full
    READY_EMBER("effects/ready_ember.png", 29, 29, 6, 6, 30, false, 14, 14),
    READY_CYAN("effects/ready_cyan.png", 29, 29, 6, 6, 30, false, 14, 14),
    READY_MAGENTA("effects/ready_magenta.png", 29, 29, 6, 6, 30, false, 14, 14),
    READY_RED("effects/ready_red.png", 29, 29, 6, 6, 30, false, 14, 14);

    public final String file;
    public final int frameWidth;
    public final int frameHeight;
    public final int columns;
    public final int frames;
    public final float fps;
    public final boolean loop;
    public final int pivotX;
    public final int pivotY;
    public final float scale; // drawn this many times its sheet size

    EffectType(String file, int frameWidth, int frameHeight, int columns, int frames, float fps,
               boolean loop, int pivotX, int pivotY) {
        this(file, frameWidth, frameHeight, columns, frames, fps, loop, pivotX, pivotY, 1f);
    }

    EffectType(String file, int frameWidth, int frameHeight, int columns, int frames, float fps,
               boolean loop, int pivotX, int pivotY, float scale) {
        this.file = file;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.columns = columns;
        this.frames = frames;
        this.fps = fps;
        this.loop = loop;
        this.pivotX = pivotX;
        this.pivotY = pivotY;
        this.scale = scale;
    }

    /** False for an effect that goes all the way round the attacker, which looks the same whichever way they aim. */
    public boolean turnsWithAim() {
        return this != PUNCH_HEAVY;
    }

    /** Seconds a one-shot effect lasts. */
    public float duration() {
        return frames / fps;
    }
}

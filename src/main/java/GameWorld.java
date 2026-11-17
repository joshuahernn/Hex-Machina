import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;

/**
 * The loose things on the map that are not part of the tile map or a hero: weapons lying on
 * the floor, arrows and bolts in flight, and effects playing (slashes, impacts).
 *
 * It also remembers where melee blows landed this frame (getMeleeHits), so that enemies can
 * take damage from them once enemies exist.
 */
public class GameWorld {
    /**
     * A weapon lying on the floor. (x, y) is its middle, in map pixels, and it points
     * `rotation` degrees (0 = right, counterclockwise): the way it was aimed when dropped.
     */
    public static class DroppedWeapon {
        public final Weapon weapon;
        public final float x;
        public final float y;
        public final float rotation;

        DroppedWeapon(Weapon weapon, float x, float y, float rotation) {
            this.weapon = weapon;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
        }
    }

    /** One effect playing at a spot, turned to face `rotation` degrees (0 = right, counterclockwise). */
    private static class Effect {
        final EffectType type;
        final float x;
        final float y;
        final float rotation;
        final boolean behindHeroes; // on the far side of whoever made it, so drawn before the heroes in its row
        final boolean mirrored; // flipped top to bottom about its pivot, before it is turned
        float time;

        Effect(EffectType type, float x, float y, float rotation, boolean behindHeroes, boolean mirrored) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.behindHeroes = behindHeroes;
            this.mirrored = mirrored;
        }
    }

    /** An arrow or bolt in flight. (x, y) is the spot on the floor underneath it. */
    private static class Projectile {
        final EffectType sprite;
        final EffectType trail; // drawn streaming out behind the sprite; null = none
        final EffectType impact;
        final float damage;
        final float velocityX;
        final float velocityY;
        final float rotation;
        float x;
        float y;
        float distanceLeft;
        float time;
        final float startHeight;
        final float levelDistance;
        final float climbRotation;

        /** How far it has flown, in map pixels. */
        float flown() {
            return PROJECTILE_RANGE - distanceLeft;
        }

        /**
         * How far above the floor it is drawn. It leaves from startHeight and comes down to
         * FLIGHT_HEIGHT evenly over levelDistance, so on screen it flies in one straight line from
         * where it left to where it was aimed.
         */
        float height() {
            if (flown() >= levelDistance) return FLIGHT_HEIGHT;
            return startHeight + (FLIGHT_HEIGHT - startHeight) * flown() / levelDistance;
        }

        /** The way it points on screen: along that straight line, then along the floor. */
        float drawnRotation() {
            return flown() >= levelDistance ? rotation : climbRotation;
        }

        Projectile(EffectType sprite, EffectType trail, EffectType impact, float damage, float x, float y, float rotation, float speed,
                   float startHeight, float levelDistance) {
            this.startHeight = startHeight;
            this.levelDistance = Math.max(MIN_LEVEL_DISTANCE, levelDistance);
            climbRotation = MathUtils.atan2(MathUtils.sinDeg(rotation) * this.levelDistance + FLIGHT_HEIGHT - startHeight,
                    MathUtils.cosDeg(rotation) * this.levelDistance) * MathUtils.radiansToDegrees;
            this.sprite = sprite;
            this.trail = trail;
            this.impact = impact;
            this.damage = damage;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            velocityX = MathUtils.cosDeg(rotation) * speed;
            velocityY = MathUtils.sinDeg(rotation) * speed;
            distanceLeft = PROJECTILE_RANGE;
        }
    }

    /** Where a melee blow landed: a circle, and the damage anything inside it takes. */
    public static class MeleeHit {
        public final float x;
        public final float y;
        public final float radius;
        public final float damage;

        MeleeHit(float x, float y, float radius, float damage) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.damage = damage;
        }
    }

    public static final float FLIGHT_HEIGHT = 18f; // How far above the floor arrows and bolts are drawn.
    private static final float PROJECTILE_RANGE = 14 * TileMap.TILE_SIZE;
    private static final float PROJECTILE_STEP = 4f; // Flight is checked against walls every this many pixels.
    private static final float MIN_LEVEL_DISTANCE = 32f; // A shot never comes down to flight height more sharply than over this far.
    private static final float SHOT_HEAD_ROOM = 5f; // This much of a shot behind its middle always shows: the back of its head.
    private static final float TRAIL_SETBACK = 5f; // A trail's bright head sits this far behind the projectile's tip.

    private final TileMap tileMap;
    private final EnumMap<EffectType, Texture> effectTextures = new EnumMap<>(EffectType.class);
    private final EnumMap<EffectType, TextureRegion[]> effectFrames = new EnumMap<>(EffectType.class);
    private final EnumMap<Weapon, Texture> groundTextures = new EnumMap<>(Weapon.class);
    private final ArrayList<DroppedWeapon> droppedWeapons = new ArrayList<>();
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private final ArrayList<Effect> effects = new ArrayList<>();
    private final ArrayList<MeleeHit> meleeHits = new ArrayList<>();

    public GameWorld(TileMap tileMap) {
        this.tileMap = tileMap;
        for (EffectType type : EffectType.values()) {
            Texture texture = new Texture(Gdx.files.internal(type.file));
            TextureRegion[] frames = new TextureRegion[type.frames];
            for (int i = 0; i < type.frames; i++) {
                frames[i] = new TextureRegion(texture, (i % type.columns) * type.frameWidth,
                        (i / type.columns) * type.frameHeight, type.frameWidth, type.frameHeight);
            }
            effectTextures.put(type, texture);
            effectFrames.put(type, frames);
        }
        for (Weapon weapon : Weapon.values()) {
            groundTextures.put(weapon, new Texture(Gdx.files.internal(weapon.groundFile)));
        }

        // temporary, until chests give weapons: one of each weapon on the floor beside the start
        GridPoint2 spawn = tileMap.getPlayerSpawns().get(0);
        int placed = 0;
        for (int col = spawn.x + 2; placed < Weapon.values().length && col < tileMap.getCols(); col += 2) {
            if (tileMap.isSolid(col, spawn.y)) continue;
            dropWeapon(Weapon.values()[placed++], (col + 0.5f) * TileMap.TILE_SIZE, (spawn.y + 0.5f) * TileMap.TILE_SIZE, 0f);
        }
    }

    /** Leaves a weapon on the floor at (x, y), pointing `rotation` degrees (0 = right, counterclockwise). */
    public void dropWeapon(Weapon weapon, float x, float y, float rotation) {
        droppedWeapons.add(new DroppedWeapon(weapon, x, y, rotation));
    }

    /**
     * Takes the nearest dropped weapon within `range` of (x, y) off the floor and returns it,
     * or returns null if there is none.
     */
    public Weapon takeNearestWeapon(float x, float y, float range) {
        DroppedWeapon nearest = null;
        float nearestDistance = range;
        for (DroppedWeapon dropped : droppedWeapons) {
            float distance = (float) Math.hypot(dropped.x - x, dropped.y - y);
            if (distance <= nearestDistance) {
                nearest = dropped;
                nearestDistance = distance;
            }
        }
        if (nearest == null) return null;
        droppedWeapons.remove(nearest);
        return nearest.weapon;
    }

    public void playEffect(EffectType type, float x, float y, float rotation) {
        playEffect(type, x, y, rotation, false);
    }

    /** For an effect on the far side of the hero who made it, heroes in its row are drawn over it. */
    public void playEffect(EffectType type, float x, float y, float rotation, boolean behindHeroes) {
        playEffect(type, x, y, rotation, behindHeroes, false);
    }

    /**
     * With the effect flipped top to bottom before it is turned to rotation, a slash
     * drawn sweeping downward stays sweeping downward when it is turned round to point left.
     */
    public void playEffect(EffectType type, float x, float y, float rotation, boolean behindHeroes, boolean mirrored) {
        effects.add(new Effect(type, x, y, rotation, behindHeroes, mirrored));
    }

    /**
     * Fires an arrow or bolt from the floor spot (x, y) toward rotation degrees.
     * trail is an effect that streams out behind it as it flies, or null for none.
     */
    public void fireProjectile(EffectType sprite, EffectType trail, EffectType impact, float damage, float x, float y, float rotation, float speed) {
        fireProjectile(sprite, trail, impact, damage, x, y, rotation, speed, FLIGHT_HEIGHT, MIN_LEVEL_DISTANCE);
    }

    /**
     * For a shot that leaves from `startHeight` above the floor, like the head of a
     * staff. `levelDistance` is how far away the thing aimed at is: the shot is drawn flying
     * straight at it, reaching normal flight height as it gets there.
     */
    public void fireProjectile(EffectType sprite, EffectType trail, EffectType impact, float damage, float x, float y, float rotation, float speed,
                               float startHeight, float levelDistance) {
        projectiles.add(new Projectile(sprite, trail, impact, damage, x, y, rotation, speed, startHeight, levelDistance));
    }

    /** Records where a melee blow landed this frame. */
    public void addMeleeHit(float x, float y, float radius, float damage) {
        meleeHits.add(new MeleeHit(x, y, radius, damage));
    }

    /** The melee blows that landed since the last update. Nothing reads these until enemies exist. */
    public ArrayList<MeleeHit> getMeleeHits() {
        return meleeHits;
    }

    /** Call once a frame, before the heroes update. */
    public void update(float deltaSeconds) {
        meleeHits.clear();

        for (Iterator<Effect> it = effects.iterator(); it.hasNext(); ) {
            Effect effect = it.next();
            effect.time += deltaSeconds;
            if (!effect.type.loop && effect.time >= effect.type.duration()) it.remove();
        }

        for (Iterator<Projectile> it = projectiles.iterator(); it.hasNext(); ) {
            Projectile projectile = it.next();
            projectile.time += deltaSeconds;
            float speed = (float) Math.hypot(projectile.velocityX, projectile.velocityY);
            float distance = speed * deltaSeconds;
            boolean stopped = false;
            // Move in short steps so a fast arrow can't skip through a wall.
            for (float moved = 0; moved < distance && !stopped; moved += PROJECTILE_STEP) {
                float step = Math.min(PROJECTILE_STEP, distance - moved);
                float nextX = projectile.x + projectile.velocityX / speed * step;
                float nextY = projectile.y + projectile.velocityY / speed * step;
                if (tileMap.isSolid(TileMap.rowAt(nextX), TileMap.rowAt(nextY))) {
                    stopped = true;
                } else {
                    projectile.x = nextX;
                    projectile.y = nextY;
                    projectile.distanceLeft -= step;
                    if (projectile.distanceLeft <= 0) stopped = true;
                }
            }
            if (stopped) {
                // The impact sprays back the way the shot came from.
                playEffect(projectile.impact, projectile.x, projectile.y + FLIGHT_HEIGHT, projectile.rotation + 180f);
                it.remove();
            }
        }
    }

    private void drawFrame(SpriteBatch batch, EffectType type, float time, float x, float y, float rotation) {
        drawFrame(batch, type, time, x, y, rotation, false);
    }

    private void drawFrame(SpriteBatch batch, EffectType type, float time, float x, float y, float rotation, boolean mirrored) {
        TextureRegion[] frames = effectFrames.get(type);
        int frame = (int) (time * type.fps);
        frame = type.loop ? frame % frames.length : Math.min(frame, frames.length - 1);
        float pivotFromBottom = type.frameHeight - type.pivotY;
        batch.draw(frames[frame], Math.round(x - type.pivotX), Math.round(y - pivotFromBottom),
                type.pivotX, pivotFromBottom, type.frameWidth, type.frameHeight, type.scale, mirrored ? -type.scale : type.scale, rotation);
    }

    /**
     * Draws a trail with its head at (x, y), showing
     * no more than length pixels of it behind the head.
     */
    private void drawTrail(SpriteBatch batch, EffectType type, float time, float x, float y, float rotation, float length) {
        TextureRegion frame = effectFrames.get(type)[(int) (time * type.fps) % type.frames];
        int shown = (int) Math.min(type.pivotX, Math.max(0f, length));
        int hidden = type.pivotX - shown;
        float pivotFromBottom = type.frameHeight - type.pivotY;
        // (x, y) is where the head goes; the part that is shown starts `shown` pixels to its left.
        batch.draw(frame.getTexture(), Math.round(x) - shown, Math.round(y - pivotFromBottom), shown, pivotFromBottom,
                type.frameWidth - hidden, type.frameHeight, 1f, 1f, rotation,
                frame.getRegionX() + hidden, frame.getRegionY(), type.frameWidth - hidden, type.frameHeight, false, false);
    }

    /** Draws one frame of a looping effect that something else is timing, like a hero's charge-up. */
    public void drawEffect(SpriteBatch batch, EffectType type, float time, float x, float y, float rotation) {
        drawFrame(batch, type, time, x, y, rotation);
    }

    /**
     * Draws everything in one row of the map that heroes in that row stand in front of: the
     * dropped weapons on the floor, shots flying away up the screen, and effects on the far
     * side of whoever made them. Call before drawing that row's heroes.
     */
    public void drawRowFloor(SpriteBatch batch, int row) {
        for (DroppedWeapon dropped : droppedWeapons) {
            if (TileMap.rowAt(dropped.y) != row) continue;
            // The ground sprites point right; turn each about its middle to the angle it was dropped at.
            Texture texture = groundTextures.get(dropped.weapon);
            float width = texture.getWidth();
            float height = texture.getHeight();
            batch.draw(texture, Math.round(dropped.x - width / 2f), Math.round(dropped.y - height / 2f),
                    width / 2f, height / 2f, width, height, 1f, 1f, dropped.rotation,
                    0, 0, texture.getWidth(), texture.getHeight(), false, false);
        }
        drawRowFlying(batch, row, true);
    }

    /** Draws the rest of the projectiles and effects in one row of the map. Call after drawing that row's heroes. */
    public void drawRowAir(SpriteBatch batch, int row) {
        drawRowFlying(batch, row, false);
    }

    /** Draws the projectiles and effects of one row that go behind that row's heroes, or the ones that go in front. */
    private void drawRowFlying(SpriteBatch batch, int row, boolean behindHeroes) {
        for (Projectile projectile : projectiles) {
            if (TileMap.rowAt(projectile.y) != row) continue;
            // A shot heading up the screen is going away from the camera, past whoever fired it.
            if ((projectile.velocityY > 0f) != behindHeroes) continue;
            if (projectile.trail != null) {
                // Only as much trail as the shot has flown, so it never reaches back behind the bow.
                drawTrail(batch, projectile.trail, projectile.time,
                        projectile.x - MathUtils.cosDeg(projectile.drawnRotation()) * TRAIL_SETBACK,
                        projectile.y + projectile.height() - MathUtils.sinDeg(projectile.drawnRotation()) * TRAIL_SETBACK,
                        projectile.drawnRotation(), projectile.flown());
            }
            // A shot's own tail grows out behind it as it flies: none of it shows behind where it was fired from.
            drawTrail(batch, projectile.sprite, projectile.time, projectile.x, projectile.y + projectile.height(),
                    projectile.drawnRotation(), projectile.flown() + SHOT_HEAD_ROOM);
        }
        for (Effect effect : effects) {
            // An effect is sorted by the floor under it, not by how high it is drawn.
            if (TileMap.rowAt(effect.y - FLIGHT_HEIGHT) != row || effect.behindHeroes != behindHeroes) continue;
            drawFrame(batch, effect.type, effect.time, effect.x, effect.y, effect.rotation, effect.mirrored);
        }
    }

    public void dispose() {
        for (Texture texture : effectTextures.values()) texture.dispose();
        for (Texture texture : groundTextures.values()) texture.dispose();
    }
}

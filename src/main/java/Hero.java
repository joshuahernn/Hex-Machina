// Each hero sprite frame is 32 pixels wide by 48 pixels tall, one frame per facing.
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.HashMap; // Pairs each direction key (like "n" for north) with its sprite frame.
import java.util.Map;

/**
 * A player character that walks around a TileMap: the knight, mage, or ranger.
 * Movement, wall collision, and mouse aiming follow Player; the three classes differ only in
 * what HeroClass holds.
 */
public class Hero {
    private static final float SPEED = 95f; // Distance the hero moves in map pixels per second.
    private static final float WALK_FPS = 14f; // Number of walk animation frames shown per second.
    private static final int FRAME_WIDTH = 32;
    private static final int FRAME_HEIGHT = 48;
    private static final float FEET_OFFSET = 4f; // Empty rows under the feet in the sprite (the shadow).
    // The part of the hero that walls stop: a box around the feet.
    private static final float HITBOX_WIDTH = 14f;
    private static final float HITBOX_HEIGHT = 10f;
    private static final float AIM_HEIGHT = 24f; // Height of the point the hero aims from, above the feet.

    // Rows follow vertical input (-1, 0, +1); columns follow horizontal input.
    private static final String[][] FACINGS = {
        {"sw", "s", "se"},
        {"w", null, "e"},
        {"nw", "n", "ne"}
    };
    // Order of the frames in the sprite strip, left to right.
    private static final String[] STRIP_ORDER = {"s", "sw", "w", "nw", "n", "ne", "e", "se"};

    private final HeroClass heroClass;
    private final Texture texture;
    private final Texture walkTexture;
    private final Cursor customCursor;
    private final Map<String, TextureRegion> facingFrames = new HashMap<>();
    private final Map<String, TextureRegion[]> walkFrames = new HashMap<>(); // the walk cycle for each facing
    private final TileMap tileMap;
    private final int upKey, downKey, leftKey, rightKey;
    private final Vector2 movement = new Vector2();
    private final Vector3 mouse = new Vector3();
    // (x, y) is the middle of the hero's feet, in map pixels.
    private float x;
    private float y;
    private String facing = "s";
    private boolean moving;
    private float animationTime;

    public Hero(HeroClass heroClass, TileMap tileMap, GridPoint2 spawnTile) {
        this(heroClass, tileMap, spawnTile, Input.Keys.W, Input.Keys.S, Input.Keys.A, Input.Keys.D);
    }

    public Hero(HeroClass heroClass, TileMap tileMap, GridPoint2 spawnTile, int upKey, int downKey, int leftKey, int rightKey) {
        this.heroClass = heroClass;
        this.tileMap = tileMap;
        this.upKey = upKey;
        this.downKey = downKey;
        this.leftKey = leftKey;
        this.rightKey = rightKey;

        texture = new Texture(Gdx.files.internal(heroClass.spriteFile));
        walkTexture = new Texture(Gdx.files.internal(heroClass.walkFile));
        int walkSteps = walkTexture.getHeight() / FRAME_HEIGHT;
        for (int i = 0; i < STRIP_ORDER.length; i++) {
            facingFrames.put(STRIP_ORDER[i], new TextureRegion(texture, i * FRAME_WIDTH, 0, FRAME_WIDTH, FRAME_HEIGHT));
            TextureRegion[] cycle = new TextureRegion[walkSteps];
            for (int step = 0; step < walkSteps; step++) {
                cycle[step] = new TextureRegion(walkTexture, i * FRAME_WIDTH, step * FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);
            }
            walkFrames.put(STRIP_ORDER[i], cycle);
        }
        x = spawnTile.x * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f;
        y = spawnTile.y * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f - HITBOX_HEIGHT / 2f;

        // crosshair
        Pixmap pixmap = new Pixmap(Gdx.files.internal("ui/crosshair.png"));
        int xHotspot = pixmap.getWidth() / 2;
        int yHotspot = pixmap.getHeight() / 2;
        customCursor = Gdx.graphics.newCursor(pixmap, xHotspot, yHotspot);
        pixmap.dispose();
    }

    private int held(int key) {
        if (Gdx.input.isKeyPressed(key)) {
            return 1;
        }
        return 0;
    }

    private boolean blockedAt(float feetX, float feetY) {
        return tileMap.overlapsSolid(feetX, feetY + HITBOX_HEIGHT / 2f, HITBOX_WIDTH, HITBOX_HEIGHT);
    }

    public void update(float deltaSeconds, OrthographicCamera camera) {
        int dx = held(rightKey) - held(leftKey);
        int dy = held(upKey) - held(downKey);
        if (dx != 0 || dy != 0) facing = FACINGS[dy + 1][dx + 1];

        // A diagonal and a straight step cover the same distance in one second.
        movement.set(dx, dy).nor();
        float distance = SPEED * deltaSeconds;

        // Each axis moves on its own, so a hero pushing diagonally into a wall slides along it.
        float startX = x;
        float startY = y;
        float nextX = x + movement.x * distance;
        if (!blockedAt(nextX, y)) x = nextX;
        float nextY = y + movement.y * distance;
        if (!blockedAt(x, nextY)) y = nextY;

        // The walk cycle plays while the hero is actually getting somewhere, and restarts when they stop.
        moving = x != startX || y != startY;
        animationTime = moving ? animationTime + deltaSeconds : 0f;

        // mouse1 aiming
        boolean aiming = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        if (aiming) {
            Gdx.graphics.setCursor(customCursor);

            // The mouse is in window pixels; the camera turns that into a spot on the map.
            camera.unproject(mouse.set(Gdx.input.getX(), Gdx.input.getY(), 0));

            float aimX = mouse.x - x;
            float aimY = mouse.y - (y + AIM_HEIGHT);

            // Same 45-degree slices as Player: east is 0, counterclockwise, 22.5 either side of each direction.
            float angle = MathUtils.atan2(aimY, aimX) * MathUtils.radiansToDegrees;

            if (angle >= -22.5f && angle < 22.5f) {
                facing = "e";
            }

            if (angle >= 22.5f && angle < 67.5f) {
                facing = "ne";
            }

            if (angle >= 67.5f && angle < 112.5f) {
                facing = "n";
            }

            if (angle >= 112.5f && angle < 157.5f) {
                facing = "nw";
            }

            if (angle >= 157.5f || angle < -157.5f) {
                facing = "w";
            }

            if (angle >= -157.5f && angle < -112.5f) {
                facing = "sw";
            }

            if (angle >= -112.5f && angle < -67.5f) {
                facing = "s";
            }

            if (angle >= -67.5f && angle < -22.5f) {
                facing = "se";
            }
        } else {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        }
    }

    public HeroClass getHeroClass() {
        return heroClass;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    /** The map row the hero's feet are in, used to draw the hero in front of or behind walls. */
    public int getRow() {
        return TileMap.rowAt(y);
    }

    public void draw(SpriteBatch batch) {
        TextureRegion sprite = facingFrames.get(facing);
        if (moving) {
            TextureRegion[] cycle = walkFrames.get(facing);
            sprite = cycle[(int) (animationTime * WALK_FPS) % cycle.length];
        }
        batch.draw(sprite, Math.round(x - FRAME_WIDTH / 2f), Math.round(y - FEET_OFFSET));
    }

    public void dispose() {
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        customCursor.dispose();
        texture.dispose();
        walkTexture.dispose();
    }
}

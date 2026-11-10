// Each player sprite frame is 24 pixels wide by 28 pixels tall. At 6x scale, it draws at 144 x 168 pixels.
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap; // Pairs each direction key (like "n" for north) with its animation frames.
import java.util.Map;

public class Player {
    private static final float IDLE_LOOP_SECONDS = 2f; // Seconds for one complete idle animation.
    private static final float RUN_FPS = 12f; // Number of run animation frames shown per second.
    private static final float SPEED = 600f; // Distance the player moves in pixels per second.
    private static final float SPRITE_SCALE = 6f;

    // Rows follow vertical input (-1, 0, +1); columns follow horizontal input.
    private static final String[][] FACINGS = {
        {"sw", "s", "se"},
        {"w", null, "e"},
        {"nw", "n", "ne"}
    };

    private final TextureAtlas atlas;
    private final Cursor customCursor;
    private final Map<String, Array<TextureAtlas.AtlasRegion>> idleFrames = new HashMap<>();
    private final Map<String, Array<TextureAtlas.AtlasRegion>> runFrames = new HashMap<>();
    private final int upKey, downKey, leftKey, rightKey;
    private final Vector2 movement = new Vector2();
    private final float drawWidth, drawHeight;
    private float x;
    private float y;
    private String facing = "s";
    private Array<TextureAtlas.AtlasRegion> frames;
    private float animationTime;
    private float loopSeconds = IDLE_LOOP_SECONDS;

    public Player() {
        this("characters/sunnyside-soldier", Input.Keys.W, Input.Keys.S, Input.Keys.A, Input.Keys.D, 0);
    }

    public Player(String spriteFolder, int upKey, int downKey, int leftKey, int rightKey, float startOffset) {
        this.upKey = upKey;
        this.downKey = downKey;
        this.leftKey = leftKey;
        this.rightKey = rightKey;

        atlas = new TextureAtlas(Gdx.files.internal(spriteFolder + "/soldier.atlas"));
        for (String[] row : FACINGS) {
            for (String direction : row) {
                if (direction == null) continue; // idle clips
                idleFrames.put(direction, atlas.findRegions("idle-" + direction));
                runFrames.put(direction, atlas.findRegions("run-" + direction));
            }
        }
        frames = idleFrames.get(facing);
        drawWidth = frames.first().getRegionWidth() * SPRITE_SCALE;
        drawHeight = frames.first().getRegionHeight() * SPRITE_SCALE;
        float maxX = Math.max(0f, Gdx.graphics.getWidth() - drawWidth);
        float maxY = Math.max(0f, Gdx.graphics.getHeight() - drawHeight);
        x = MathUtils.clamp(maxX / 2 + startOffset, 0, maxX);
        y = maxY / 2;

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

    public void update(float deltaSeconds) {
        int dx = held(rightKey) - held(leftKey);
        int dy = held(upKey) - held(downKey);
        if (dx != 0 || dy != 0) facing = FACINGS[dy + 1][dx + 1];

        // A diagonal and a straight step cover the same distance in one second.
        movement.set(dx, dy).nor();
        float distance = SPEED * deltaSeconds;
        float maxX = Math.max(0f, Gdx.graphics.getWidth() - drawWidth);
        float maxY = Math.max(0f, Gdx.graphics.getHeight() - drawHeight);

        float nextX = MathUtils.clamp(x + movement.x * distance, 0, maxX);
        float nextY = MathUtils.clamp(y + movement.y * distance, 0, maxY);

        boolean moving = nextX != x || nextY != y;
        x = nextX;
        y = nextY;

        // mouse1 aiming
        boolean aiming = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        if (aiming) {
            Gdx.graphics.setCursor(customCursor);

            float centerX = x + drawWidth / 2f;
            float centerY = y + drawHeight / 2f;

            float mouseX = Gdx.input.getX();
            float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();

            /*
            * 360 / 8 = 45 degrees per direction.
            * Each direction extends 22.5 degrees either side of its center.
            *
            * Facing | Center | Angle range
            * E      |    0   | -22.5 <= angle < 22.5
            * NE     |   45   |  22.5 <= angle < 67.5
            * N      |   90   |  67.5 <= angle < 112.5
            * NW     |  135   | 112.5 <= angle < 157.5
            * W      | +/-180 | angle >= 157.5 OR angle < -157.5
            * SW     | -135   | -157.5 <= angle < -112.5
            * S      |  -90   | -112.5 <= angle < -67.5
            * SE     |  -45   | -67.5 <= angle < -22.5
            */


            float aimX = mouseX - centerX;
            float aimY = mouseY - centerY;

            float angle = MathUtils.atan2(aimY, aimX) * MathUtils.radiansToDegrees; // we need to use an angle to take in both distances into account


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


        // Select the run or idle frames for the direction the player is facing.
        Array<TextureAtlas.AtlasRegion> nextFrames = moving ? runFrames.get(facing) : idleFrames.get(facing);
        if (frames != nextFrames) {
            frames = nextFrames;
            loopSeconds = moving ? frames.size / RUN_FPS : IDLE_LOOP_SECONDS;
            animationTime = 0f;
        } else {
            animationTime = (animationTime + deltaSeconds) % loopSeconds;
        }
    }

    public void draw(SpriteBatch batch) {
        // Convert progress through the loop into a frame index.
        int frame = (int) (animationTime / loopSeconds * frames.size);
        // Pick the image at that index to draw this frame.
        TextureAtlas.AtlasRegion sprite = frames.get(frame);
        batch.draw(sprite, Math.round(x), Math.round(y), drawWidth, drawHeight);
    }

    public void dispose() {
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        customCursor.dispose();
        atlas.dispose();
    }
}

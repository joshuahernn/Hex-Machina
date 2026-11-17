import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * The picture of something a hero holds: a weapon or a shield.
 *
 * Its sheet has the same 8 facings across as a hero strip, and two rows: the top row is the
 * part drawn behind the hero's body, the bottom row the part drawn in front of it. For any one
 * facing only one of the two has anything in it. Each frame is the size of a hero frame and is
 * drawn at the same spot, so the item sits in the hand of whichever hero holds it.
 */
public class HeldSprite {
    private static final int FRAME_WIDTH = 32;
    private static final int FRAME_HEIGHT = 48;
    private static final int SOLID_ALPHA = 200; // Fainter pixels are glow, not part of the item itself.
    private static final int HAND_ROW = 26; // Rows down from the top of a frame to the hand that holds a weapon.
    private static final float GRIP_ABOVE_END = 3f; // If nothing is at the hand, the grip is this far above the item's lower end.

    private final Texture texture;
    private final TextureRegion[] behind;
    private final TextureRegion[] inFront;
    // The item's highest point in each facing (a sword's point, a staff's head), measured from the
    // frame's bottom-left corner.
    private final float[] tipX;
    private final float[] tipY;
    // Where the hand holds the item in each facing: the point it turns about when it is swung.
    private final float[] gripX;
    private final float[] gripY;
    private final boolean[] behindBody; // whether the item is on the far side of the hero in each facing

    public HeldSprite(String spriteFile) {
        Pixmap pixmap = new Pixmap(Gdx.files.internal(spriteFile));
        texture = new Texture(pixmap);
        int facings = texture.getWidth() / FRAME_WIDTH;
        behind = new TextureRegion[facings];
        inFront = new TextureRegion[facings];
        tipX = new float[facings];
        tipY = new float[facings];
        gripX = new float[facings];
        gripY = new float[facings];
        behindBody = new boolean[facings];
        for (int i = 0; i < facings; i++) {
            behind[i] = new TextureRegion(texture, i * FRAME_WIDTH, 0, FRAME_WIDTH, FRAME_HEIGHT);
            inFront[i] = new TextureRegion(texture, i * FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);
            findTip(pixmap, i);
            behindBody[i] = hasPixels(pixmap, i * FRAME_WIDTH, 0);
        }
        pixmap.dispose();
    }

    /** Whether the frame whose top-left corner is (left, top) has anything drawn in it. */
    private boolean hasPixels(Pixmap pixmap, int left, int top) {
        for (int row = 0; row < FRAME_HEIGHT; row++) {
            for (int column = 0; column < FRAME_WIDTH; column++) {
                if ((pixmap.getPixel(left + column, top + row) & 0xff) >= SOLID_ALPHA) return true;
            }
        }
        return false;
    }

    /** The middle of the item along one row of one facing, or -1 if that row is empty. */
    private float middleOfRow(Pixmap pixmap, int facing, int row) {
        int total = 0;
        int count = 0;
        for (int column = 0; column < FRAME_WIDTH; column++) {
            for (int half = 0; half < 2; half++) {
                int pixel = pixmap.getPixel(facing * FRAME_WIDTH + column, half * FRAME_HEIGHT + row);
                if ((pixel & 0xff) >= SOLID_ALPHA) {
                    total += column;
                    count++;
                }
            }
        }
        return count == 0 ? -1f : (float) total / count + 0.5f;
    }

    /** Finds the item's top point and where it is held in one facing. */
    private void findTip(Pixmap pixmap, int facing) {
        tipX[facing] = gripX[facing] = FRAME_WIDTH / 2f;
        tipY[facing] = gripY[facing] = FRAME_HEIGHT / 2f;
        for (int row = 0; row < FRAME_HEIGHT; row++) {
            float middle = middleOfRow(pixmap, facing, row);
            if (middle >= 0f) {
                tipX[facing] = middle;
                tipY[facing] = FRAME_HEIGHT - row - 0.5f;
                break;
            }
        }
        float atHand = middleOfRow(pixmap, facing, HAND_ROW);
        if (atHand >= 0f) {
            gripX[facing] = atHand;
            gripY[facing] = FRAME_HEIGHT - HAND_ROW - 0.5f;
            return;
        }
        for (int row = FRAME_HEIGHT - 1; row >= 0; row--) {
            float middle = middleOfRow(pixmap, facing, row);
            if (middle >= 0f) {
                gripX[facing] = middle;
                gripY[facing] = FRAME_HEIGHT - row - 0.5f + GRIP_ABOVE_END;
                break;
            }
        }
    }

    /** Draw this before the hero's body. (x, y) is where the hero's frame is drawn. */
    public void drawBehind(SpriteBatch batch, int facingIndex, float x, float y) {
        batch.draw(behind[facingIndex], x, y);
    }

    /** Draw this after the hero's body. */
    public void drawInFront(SpriteBatch batch, int facingIndex, float x, float y) {
        batch.draw(inFront[facingIndex], x, y);
    }

    /** drawBehind, with the item tilted `rotation` degrees counterclockwise about the hand. */
    public void drawBehind(SpriteBatch batch, int facingIndex, float x, float y, float rotation) {
        batch.draw(behind[facingIndex], x, y, gripX[facingIndex], gripY[facingIndex], FRAME_WIDTH, FRAME_HEIGHT, 1f, 1f, rotation);
    }

    /** drawInFront, with the item tilted `rotation` degrees counterclockwise about the hand. */
    public void drawInFront(SpriteBatch batch, int facingIndex, float x, float y, float rotation) {
        batch.draw(inFront[facingIndex], x, y, gripX[facingIndex], gripY[facingIndex], FRAME_WIDTH, FRAME_HEIGHT, 1f, 1f, rotation);
    }

    /**
     * Draws the whole item turned about the hand that holds it, for a swing. The item stands
     * upright in its sheet, so `rotation` is degrees counterclockwise from upright.
     */
    public void drawTurned(SpriteBatch batch, int facingIndex, float x, float y, float rotation) {
        batch.draw(behind[facingIndex], x, y, gripX[facingIndex], gripY[facingIndex], FRAME_WIDTH, FRAME_HEIGHT, 1f, 1f, rotation);
        batch.draw(inFront[facingIndex], x, y, gripX[facingIndex], gripY[facingIndex], FRAME_WIDTH, FRAME_HEIGHT, 1f, 1f, rotation);
    }

    /**
     * Makes this sprite turn about the same point as `other`, for a sheet drawn over another
     * item's (like the glow round a sword) that has to stay on it when the item is tilted.
     */
    public void turnAbout(HeldSprite other) {
        System.arraycopy(other.gripX, 0, gripX, 0, gripX.length);
        System.arraycopy(other.gripY, 0, gripY, 0, gripY.length);
    }

    /** True when the hero's body hides this item in this facing: it is in the far hand. */
    public boolean isBehindBody(int facingIndex) {
        return behindBody[facingIndex];
    }

    public float getGripX(int facingIndex) {
        return gripX[facingIndex];
    }

    public float getGripY(int facingIndex) {
        return gripY[facingIndex];
    }

    public float getTipX(int facingIndex) {
        return tipX[facingIndex];
    }

    public float getTipY(int facingIndex) {
        return tipY[facingIndex];
    }

    public void dispose() {
        texture.dispose();
    }
}

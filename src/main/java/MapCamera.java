import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;

/**
 * The scrolling view of the map. It shows a slice of the map ZOOM times smaller than the
 * window, scaled up to fill it, and follows a point up, down, left, and right.
 */
public class MapCamera {
    public static final int ZOOM = 3;

    public final OrthographicCamera camera = new OrthographicCamera();
    private final float viewWidth;
    private final float viewHeight;

    public MapCamera() {
        viewWidth = Gdx.graphics.getWidth() / (float) ZOOM;
        viewHeight = Gdx.graphics.getHeight() / (float) ZOOM;
        camera.setToOrtho(false, viewWidth, viewHeight);
    }

    /**
     * Centers the view on (x, y), stopping at the edges of the map. A map smaller than the
     * view is centered instead. The position is snapped to whole window pixels so tiles don't
     * shimmer while scrolling.
     */
    public void follow(float x, float y, TileMap tileMap) {
        camera.position.x = center(x, viewWidth, tileMap.getWidth());
        camera.position.y = center(y, viewHeight, tileMap.getHeight());
        camera.update();
    }

    private static float center(float target, float viewSize, float mapSize) {
        float center = mapSize <= viewSize ? mapSize / 2f : MathUtils.clamp(target, viewSize / 2f, mapSize - viewSize / 2f);
        return Math.round(center * ZOOM) / (float) ZOOM;
    }
}

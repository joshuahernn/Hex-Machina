import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private TileMap tileMap;
    private MapCamera mapCamera;
    private Hero player;


    public static void main(String[] args) {
        var config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Hex Machina");
        config.setWindowedMode(1920, 1080);
        config.setResizable(false);
        new Lwjgl3Application(new Main(), config);
    }
    @Override
    public void create() {
        batch = new SpriteBatch();
        tileMap = new TileMap("maps/level1.txt");
        mapCamera = new MapCamera();
        player = new Hero(HeroClass.KNIGHT, tileMap, tileMap.getPlayerSpawns().get(0));
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.24f, 0.26f, 0.28f, 1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.app.exit();
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) tileMap.openNextGate(); // temporary, until bosses drop keys

        player.update(Gdx.graphics.getDeltaTime(), mapCamera.camera);
        mapCamera.follow(player.getX(), player.getY(), tileMap);
        batch.setProjectionMatrix(mapCamera.camera.combined);
        batch.begin();
        tileMap.draw(batch, mapCamera.camera, row -> {
            if (player.getRow() == row) player.draw(batch);
        });
        batch.end();
    }

    @Override
    public void dispose() {
        player.dispose();
        tileMap.dispose();
        batch.dispose();
    }
}

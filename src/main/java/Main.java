import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Player player;


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
        player = new Player();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.24f, 0.26f, 0.28f, 1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.app.exit();

        player.update(Gdx.graphics.getDeltaTime());
        batch.begin();
        player.draw(batch);
        batch.end();
    }

    @Override
    public void dispose() {
        player.dispose();
        batch.dispose();
    }
}

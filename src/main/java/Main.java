import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.utils.ScreenUtils;

public class Main extends ApplicationAdapter {
    public static void main(String[] args) {
        var config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Hex Machina");
        config.setDecorated(true);
        config.setResizable(false);
        config.setWindowedMode(1920, 1080);
        config.useVsync(true);
        new Lwjgl3Application(new Main(), config);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.08f, 0.08f, 0.08f, 1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.app.exit();
    }
}

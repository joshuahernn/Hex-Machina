import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;

import java.util.ArrayList;

/**
 * 2D level layout loaded from a text file, one character per tile. The map can be any size.
 *
 * The first line of the file is the top row of the map. Row 0 is the bottom row, so tile
 * (col, row) covers x col * TILE_SIZE and y row * TILE_SIZE upward.
 *
 * Walls are taller than a tile: each wall piece is WALL_PIECE_HEIGHT tall and is drawn
 * WALL_LIFT above its tile, so its stone top overlaps the row behind it and its brick face
 * hangs into the row in front. Rows are therefore drawn back to front (top of the screen
 * first), with everything standing in a row drawn right after that row's walls.
 */
public class TileMap {
    public static final int TILE_SIZE = 32;

    public static final char WALL = '#';
    public static final char TORCH_WALL = 'T';
    public static final char FLOOR = '.';
    public static final char PLAYER_SPAWN = 'P';
    public static final char BARREL = 'B';
    public static final char CRATE = 'K';
    public static final char CHEST = 'C';
    public static final char ENEMY_SPAWN = 'E';
    public static final char BOSS_SPAWN = 'M';
    public static final char GATE = 'G';
    public static final char EXIT = 'X';

    public static final int WALL_PIECE_HEIGHT = 56;
    public static final int WALL_LIFT = 8;
    private static final int WALL_SHEET_COLUMNS = 9;
    private static final int WALL_SHEET_ROWS = 4;
    private static final int CRACKED_FLOOR_ONE_IN = 8;
    private static final float TORCH_GLOW_Y = 11f; // flame height above the bottom of the torch's tile
    private static final float EDGE_EPSILON = 0.001f;

    /** Draws whatever stands in one row of the map (players, enemies). */
    public interface RowDrawer {
        void drawRow(int row);
    }

    private char[][] tiles;
    private int cols;
    private int rows;
    private final ArrayList<GridPoint2> playerSpawns = new ArrayList<>();
    private final ArrayList<GridPoint2> enemySpawns = new ArrayList<>();
    private final ArrayList<GridPoint2> bossSpawns = new ArrayList<>();
    // left tile of each two-tile-wide gate, in the order the party reaches them (bottom of the map first)
    private final ArrayList<GridPoint2> gates = new ArrayList<>();
    private boolean[][] gateOpen;

    private final Texture floorTexture;
    private final Texture crackedFloorTexture;
    private final Texture wallsTexture;
    private final Texture barrelTexture;
    private final Texture crateTexture;
    private final Texture chestTexture;
    private final Texture torchTexture;
    private final Texture torchGlowTexture;
    private final Texture gateTexture;
    private final Texture exitTexture;
    private final TextureRegion[] gateHalves = new TextureRegion[2]; // left half, right half
    private final TextureRegion[] exitHalves = new TextureRegion[2];
    private final TextureRegion torchGlow; // the glow, cut off at the top of the torch's wall face
    private final TextureRegion[][] wallPieces; // [row in the sheet][column in the sheet]

    public TileMap(String mapFile) {
        floorTexture = new Texture(Gdx.files.internal("tiles/floor.png"));
        crackedFloorTexture = new Texture(Gdx.files.internal("tiles/floor_cracked.png"));
        wallsTexture = new Texture(Gdx.files.internal("tiles/walls.png"));
        barrelTexture = new Texture(Gdx.files.internal("objects/barrel.png"));
        crateTexture = new Texture(Gdx.files.internal("objects/crate.png"));
        chestTexture = new Texture(Gdx.files.internal("objects/chest_closed.png"));
        torchTexture = new Texture(Gdx.files.internal("objects/torch.png"));
        torchGlowTexture = new Texture(Gdx.files.internal("objects/torch_glow.png"));
        // The light must not reach the top of the wall the torch hangs on, or the floor behind
        // it, so only the part of the glow below the top edge of the brick face is kept.
        int glowRadius = torchGlowTexture.getHeight() / 2;
        int hiddenRows = glowRadius - (int) (TILE_SIZE - WALL_LIFT - TORCH_GLOW_Y);
        torchGlow = new TextureRegion(torchGlowTexture, 0, hiddenRows,
                torchGlowTexture.getWidth(), torchGlowTexture.getHeight() - hiddenRows);

        gateTexture = new Texture(Gdx.files.internal("tiles/gate.png"));
        exitTexture = new Texture(Gdx.files.internal("tiles/exit.png"));
        for (int half = 0; half < 2; half++) {
            gateHalves[half] = new TextureRegion(gateTexture, half * TILE_SIZE, 0, TILE_SIZE, TILE_SIZE);
            exitHalves[half] = new TextureRegion(exitTexture, half * TILE_SIZE, 0, TILE_SIZE, TILE_SIZE);
        }

        wallPieces = new TextureRegion[WALL_SHEET_ROWS][WALL_SHEET_COLUMNS];
        for (int sheetRow = 0; sheetRow < WALL_SHEET_ROWS; sheetRow++) {
            for (int sheetColumn = 0; sheetColumn < WALL_SHEET_COLUMNS; sheetColumn++) {
                wallPieces[sheetRow][sheetColumn] = new TextureRegion(wallsTexture,
                        sheetColumn * TILE_SIZE, sheetRow * WALL_PIECE_HEIGHT, TILE_SIZE, WALL_PIECE_HEIGHT);
            }
        }
        load(mapFile);
    }

    private void load(String mapFile) {
        ArrayList<String> lines = new ArrayList<>();
        for (String line : Gdx.files.internal(mapFile).readString().split("\\r?\\n")) {
            if (!line.isEmpty()) lines.add(line);
        }
        if (lines.isEmpty()) {
            throw new IllegalArgumentException(mapFile + ": the map is empty");
        }

        rows = lines.size();
        cols = lines.get(0).length();
        tiles = new char[rows][cols];
        gateOpen = new boolean[rows][cols];
        for (int i = 0; i < rows; i++) {
            String line = lines.get(i);
            if (line.length() != cols) {
                throw new IllegalArgumentException(mapFile + ": line " + (i + 1) + " has " + line.length() + " tiles, expected " + cols);
            }
            int row = rows - 1 - i;
            for (int col = 0; col < cols; col++) {
                char c = line.charAt(col);
                switch (c) {
                    case WALL:
                    case TORCH_WALL:
                    case FLOOR:
                    case BARREL:
                    case CRATE:
                    case CHEST:
                    case GATE:
                    case EXIT:
                        tiles[row][col] = c;
                        break;
                    case ENEMY_SPAWN:
                        enemySpawns.add(new GridPoint2(col, row));
                        tiles[row][col] = FLOOR;
                        break;
                    case BOSS_SPAWN:
                        bossSpawns.add(new GridPoint2(col, row));
                        tiles[row][col] = FLOOR;
                        break;
                    case PLAYER_SPAWN:
                        playerSpawns.add(new GridPoint2(col, row));
                        tiles[row][col] = FLOOR;
                        break;
                    default:
                        throw new IllegalArgumentException(mapFile + ": unknown tile '" + c + "' on line " + (i + 1) + ", column " + (col + 1));
                }
            }
        }
        if (playerSpawns.isEmpty()) {
            throw new IllegalArgumentException(mapFile + ": missing player spawn '" + PLAYER_SPAWN + "'");
        }
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                char tile = tiles[row][col];
                if ((tile == TORCH_WALL || tile == GATE || tile == EXIT) && isWall(col, row - 1)) {
                    throw new IllegalArgumentException(mapFile + ": '" + tile + "' on line " + (rows - row)
                            + ", column " + (col + 1) + " needs floor below it, because it is drawn on a wall face");
                }
                if ((tile == GATE || tile == EXIT) && !isDoorTile(tile, col - 1, row) && !isDoorTile(tile, col + 1, row)) {
                    throw new IllegalArgumentException(mapFile + ": '" + tile + "' must be 2 tiles wide (line "
                            + (rows - row) + ", column " + (col + 1) + ")");
                }
                if (tile == GATE && !isDoorTile(GATE, col - 1, row)) gates.add(new GridPoint2(col, row));
            }
        }
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    public float getWidth() {
        return cols * TILE_SIZE;
    }

    public float getHeight() {
        return rows * TILE_SIZE;
    }

    public ArrayList<GridPoint2> getPlayerSpawns() {
        return playerSpawns;
    }

    public ArrayList<GridPoint2> getEnemySpawns() {
        return enemySpawns;
    }

    public ArrayList<GridPoint2> getBossSpawns() {
        return bossSpawns;
    }

    /** The left tile of each gate, in the order the party reaches them. */
    public ArrayList<GridPoint2> getGates() {
        return gates;
    }

    private boolean isDoorTile(char door, int col, int row) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) return false;
        return tiles[row][col] == door;
    }

    /** Opens both tiles of the gate whose left tile is given; it becomes floor. */
    public void openGate(GridPoint2 gate) {
        gateOpen[gate.y][gate.x] = true;
        if (isDoorTile(GATE, gate.x + 1, gate.y)) gateOpen[gate.y][gate.x + 1] = true;
    }

    /** Opens the first gate that is still closed. Returns false if every gate is already open. */
    public boolean openNextGate() {
        for (GridPoint2 gate : gates) {
            if (!gateOpen[gate.y][gate.x]) {
                openGate(gate);
                return true;
            }
        }
        return false;
    }

    public char getTile(int col, int row) {
        return tiles[row][col];
    }

    /** Tiles outside the map count as wall. A closed gate and the exit are walls too. */
    public boolean isWall(int col, int row) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) return true;
        char tile = tiles[row][col];
        if (tile == GATE) return !gateOpen[row][col];
        return tile == WALL || tile == TORCH_WALL || tile == EXIT;
    }

    /** Walls and props (barrels, crates, chests) block movement. */
    public boolean isSolid(int col, int row) {
        if (isWall(col, row)) return true;
        char tile = tiles[row][col];
        return tile == BARREL || tile == CRATE || tile == CHEST;
    }

    /**
     * Whether a width x height box centered on (centerX, centerY) touches any solid tile.
     * Sitting exactly flush against a tile does not count as touching it.
     */
    public boolean overlapsSolid(float centerX, float centerY, float width, float height) {
        int colMin = (int) Math.floor((centerX - width / 2f) / TILE_SIZE);
        int colMax = (int) Math.floor((centerX + width / 2f - EDGE_EPSILON) / TILE_SIZE);
        int rowMin = (int) Math.floor((centerY - height / 2f) / TILE_SIZE);
        int rowMax = (int) Math.floor((centerY + height / 2f - EDGE_EPSILON) / TILE_SIZE);
        for (int row = rowMin; row <= rowMax; row++) {
            for (int col = colMin; col <= colMax; col++) {
                if (isSolid(col, row)) return true;
            }
        }
        return false;
    }

    /**
     * Whether a straight line between two points crosses no wall tile, checked every pixel.
     */
    public boolean hasClearLine(float fromX, float fromY, float toX, float toY) {
        float distance = (float) Math.hypot(toX - fromX, toY - fromY);
        for (float traveled = 0; traveled <= distance; traveled += 1f) {
            float t = distance == 0 ? 0 : traveled / distance;
            int col = (int) Math.floor((fromX + (toX - fromX) * t) / TILE_SIZE);
            int row = (int) Math.floor((fromY + (toY - fromY) * t) / TILE_SIZE);
            if (isWall(col, row)) return false;
        }
        return true;
    }

    public static int rowAt(float y) {
        return (int) Math.floor(y / TILE_SIZE);
    }

    /**
     * 0 = floor beside the wall, 1 = wall beside it, 2 = wall beside it but floor diagonally
     * below on that side (only when the wall itself has a wall below).
     */
    private int sideState(int sideCol, int row, boolean wallBelow) {
        if (!isWall(sideCol, row)) return 0;
        return wallBelow && !isWall(sideCol, row - 1) ? 2 : 1;
    }

    private TextureRegion wallPiece(int col, int row) {
        boolean wallAbove = isWall(col, row + 1);
        boolean wallBelow = isWall(col, row - 1);
        int sheetColumn = sideState(col - 1, row, wallBelow) * 3 + sideState(col + 1, row, wallBelow);
        int sheetRow = (wallBelow ? 2 : 0) + (wallAbove ? 1 : 0);
        return wallPieces[sheetRow][sheetColumn];
    }

    private Texture propTexture(char tile) {
        switch (tile) {
            case BARREL:
                return barrelTexture;
            case CRATE:
                return crateTexture;
            case CHEST:
                return chestTexture;
            default:
                return null;
        }
    }

    private static int firstVisible(float cameraEdge) {
        return (int) Math.floor(cameraEdge / TILE_SIZE);
    }

    /**
     * Draws the part of the map the camera can see: floor first, then each row from the top of
     * the screen down (that row's walls and props, then whatever rowDrawer draws for it), then
     * the torch light.
     */
    public void draw(SpriteBatch batch, OrthographicCamera camera, RowDrawer rowDrawer) {
        float halfWidth = camera.viewportWidth * camera.zoom / 2f;
        float halfHeight = camera.viewportHeight * camera.zoom / 2f;
        int colMin = firstVisible(camera.position.x - halfWidth);
        int colMax = firstVisible(camera.position.x + halfWidth);
        // walls reach above and below their own tile, so look two rows further each way
        int rowMin = firstVisible(camera.position.y - halfHeight) - 2;
        int rowMax = firstVisible(camera.position.y + halfHeight) + 2;

        for (int row = Math.max(0, rowMin); row <= Math.min(rows - 1, rowMax); row++) {
            for (int col = Math.max(0, colMin); col <= Math.min(cols - 1, colMax); col++) {
                if (isWall(col, row)) continue;
                boolean cracked = Math.floorMod(col * 31 + row * 17, CRACKED_FLOOR_ONE_IN) == 0;
                batch.draw(cracked ? crackedFloorTexture : floorTexture, col * TILE_SIZE, row * TILE_SIZE);
            }
        }

        for (int row = rowMax; row >= rowMin; row--) {
            for (int col = colMin; col <= colMax; col++) {
                float x = col * TILE_SIZE;
                float y = row * TILE_SIZE;
                if (isWall(col, row)) {
                    batch.draw(wallPiece(col, row), x, y + TILE_SIZE + WALL_LIFT - WALL_PIECE_HEIGHT);
                    if (row >= 0 && row < rows && col >= 0 && col < cols) {
                        char tile = tiles[row][col];
                        // these are drawn over the wall piece's brick face
                        if (tile == TORCH_WALL) batch.draw(torchTexture, x, y - WALL_LIFT);
                        if (tile == GATE) batch.draw(gateHalves[isDoorTile(GATE, col - 1, row) ? 1 : 0], x, y - WALL_LIFT);
                        if (tile == EXIT) batch.draw(exitHalves[isDoorTile(EXIT, col - 1, row) ? 1 : 0], x, y - WALL_LIFT);
                    }
                } else {
                    Texture prop = propTexture(tiles[row][col]);
                    if (prop != null) batch.draw(prop, x, y);
                }
            }
            if (row >= 0 && row < rows) rowDrawer.drawRow(row);
        }

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (int row = Math.max(0, rowMin - 2); row <= Math.min(rows - 1, rowMax + 2); row++) {
            for (int col = Math.max(0, colMin - 3); col <= Math.min(cols - 1, colMax + 3); col++) {
                if (tiles[row][col] != TORCH_WALL) continue;
                batch.draw(torchGlow,
                        col * TILE_SIZE + TILE_SIZE / 2f - torchGlow.getRegionWidth() / 2f,
                        row * TILE_SIZE + TORCH_GLOW_Y - torchGlowTexture.getHeight() / 2f);
            }
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void dispose() {
        floorTexture.dispose();
        crackedFloorTexture.dispose();
        wallsTexture.dispose();
        barrelTexture.dispose();
        crateTexture.dispose();
        chestTexture.dispose();
        torchTexture.dispose();
        torchGlowTexture.dispose();
        gateTexture.dispose();
        exitTexture.dispose();
    }
}

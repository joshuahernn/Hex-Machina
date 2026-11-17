// Each hero sprite frame is 32 pixels wide by 48 pixels tall, one frame per facing.
// The hero's body is drawn without arms or anything in its hands. The arms are drawn here, from
// each shoulder to wherever that hand is, and the weapon and shield are separate sprites.
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
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
 *
 * Any hero can equip any Weapon. Using a weapon of the class's favored type does more damage,
 * and carrying a shield slows every class that is not the knight (for later, if we add shield tiers).
 *
 * Controls: the hero always faces the mouse. Tap left click for a light attack, hold it to
 * charge and release for a heavy one. Hold right click to raise a shield. Q drops the weapon,
 * E picks one up. With no weapon, attacks are punches.
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
    private static final float FAVORED_DAMAGE_BONUS = 1.25f; // Damage multiplier with the class's favored weapon type.
    private static final float SHIELD_SPEED_FACTOR = 0.85f; // Speed debuff factor for a class slowed by a shield.

    // attacking
    private static final float HEAVY_CHARGE_TIME = 0.6f; // Seconds left click must be held for a heavy attack.
    private static final float CHARGE_SHOWN_AFTER = 0.2f; // A tap shorter than this shows no charge-up.
    private static final float HEAVY_DAMAGE_FACTOR = 2f;
    private static final float LIGHT_COOLDOWN = 0.3f; // Seconds before the next attack can start.
    private static final float HEAVY_COOLDOWN = 0.6f;
    private static final float CHARGE_SPEED_FACTOR = 0.6f; // Speed multiplier while charging.
    private static final float PUNCH_DAMAGE = 3f; // Bare hands: weak, a last resort.
    private static final float PUNCH_REACH = 16f; // How far in front of the hero a punch lands.
    private static final float PUNCH_RADIUS = 10f;
    private static final float MELEE_REACH = 24f; // How far in front of the hero a swing is centered.
    private static final float MELEE_RADIUS = 22f;
    private static final float HEAVY_MELEE_RADIUS = 30f;
    private static final float ARROW_SPEED = 330f; // Map pixels per second.
    private static final float BOLT_SPEED = 210f;
    private static final float HEAVY_SPEED_FACTOR = 1.3f; // A charged shot flies faster.
    private static final float MUZZLE_DISTANCE = 12f; // How far in front of the hero a shot starts.
    private static final float LUNGE_TIME = 0.16f; // Seconds the weapon thrusts forward on an attack.
    private static final float LUNGE_DISTANCE = 4f;
    private static final float CHARGE_PULL_BACK = 2f; // How far a sword or staff is drawn back while charging.

    // swinging a melee weapon: the blade sweeps across the aim, from one side to the other
    // A swing has four parts: the wind-up to one side, the sweep across (once or more), the finish
    // held on the other side, and the recovery back to carrying the weapon upright. The blow lands
    // as the sweep starts. A heavy attack was wound up while charging, so its weight shows in a
    // longer held finish instead.
    private static final float LIGHT_WINDUP_TIME = 0.1f;
    private static final float HEAVY_WINDUP_TIME = 0.05f;
    private static final float SWING_TIME = 0.15f; // Seconds one sweep of the blade takes.
    private static final float LIGHT_HOLD_TIME = 0.1f;
    private static final float HEAVY_HOLD_TIME = 0.2f;
    private static final float LIGHT_RECOVER_TIME = 0.05f;
    private static final float HEAVY_RECOVER_TIME = 0.1f;
    private static final float LIGHT_SWING_ARC = 45f; // Degrees the blade starts and ends either side of the aim.
    private static final float HEAVY_SWING_ARC = 60f;
    private static final float SWING_LIFT = 0.4f; // How much of a second or third cut's time goes on lifting the blade back to the top.
    private static final float SWING_SNAP = 4f; // How sharply a sweep snaps across: higher puts more of the arc at its start.
    private static final float SWING_OVERSHOOT = 12f; // Degrees the blade carries past its finish before settling back.
    private static final float FINISH_REACH = 1f; // How much further the hand pushes into the target on the held finish.
    private static final float SWING_REACH = 5f; // How far the sword hand moves out from where it rests.
    private static final float HAND_FOLLOW = 0.5f; // The hand sweeps across this fraction of the blade's arc.
    private static final float FOREARM_LENGTH = 7f; // From the elbow to the hand on a bowstring.
    private static final float FORESHORTENED = 0.4f; // How long something pointing at the camera looks, against its real length.
    // charging a sword: the blade is drawn back, away from the target, ready to come across
    private static final float SWORD_BACK_TILT = 100f; // Degrees the blade ends up tipped back, at most: aiming straight left or right.
    private static final float SWORD_SIDE_TILT = 30f; // Aiming up or down the screen, "back" cannot be seen: the blade leans out over the shoulder this far instead.
    private static final float ELBOW_DONE = 0.6f; // The elbow has finished rising this far through the wind-up.
    private static final float HAND_STARTS = 0.15f; // The hand starts back this far through it,
    private static final float HAND_DONE = 0.85f; // and is there by this far.
    private static final float BLADE_STARTS = 0.4f; // The blade starts tipping back this far through it, and finishes last.
    private static final float RAISED_FOREARM = 6f; // The forearm of a raised sword arm is drawn no longer than this.
    private static final float ELBOW_FORWARD = 5f; // How far toward the target the raised elbow is, aiming straight left or right.
    // The arm raises it there, into a high guard: the hilt comes up beside the sword shoulder,
    // a little to the side away from the target, with the point up and leaning back.
    // The hand gets there round an arc: up first, then back.
    private static final float ELBOW_RISE = 2f; // The elbow of a raised sword arm is this far above the shoulder.
    private static final float ELBOW_OUT = 5.5f; // And this far out from the body.
    private static final float SWING_DESCENT = 0.8f; // How much of the first sweep the raised hand takes to come down, after the wind-up.
    private static final float SWORD_ARC_FORWARD = 2.5f; // How far the hand swings out toward the target on its way up, at most.
    private static final float SWORD_ARC = 3f; // How round the arc is: 1 is a straight diagonal, higher goes up first and back last.
    private static final float SWORD_HAND_HEIGHT = 4f; // How far above the shoulder the raised hand ends up.
    private static final float SWORD_HAND_BACK = 5f; // How far to the side away from the target, at most: aiming straight left or right.
    private static final float SWORD_HAND_IN = 0.25f; // How much of the way in toward the shoulder the hand comes.
    private static final float SWORD_BACK_TIME = 0.32f; // Seconds the whole wind-up takes once charging shows.
    private static final float SWORD_GRIP_DROP = 1f; // The hand holds a sword this far below its guard, on the grip.

    // drawing a bow: the left arm pushes the bow out ahead while the right hand holds the string back
    // by the chin, its forearm in line with the arrow and the elbow out behind.
    private static final float[] BOW_PUSH = {3f, 5f}; // How far forward the bow is held at each stage of the draw.
    // Facing the camera the bow hangs at the hero's side; to shoot it is brought this far in toward the middle of the body.
    private static final float[] BOW_INSET = {1f, 2f};
    private static final int FACING_CAMERA = 0; // The column of the facing-down frames.
    private static final float[] BOW_RAISE = {2f, 4f}; // How far it is lifted, bringing the string hand up to the chin.
    private static final float ARROW_START_HEIGHT = 24f; // How high a shot starts when it has no weapon to leave from.
    private static final float QUICK_DRAW_TIME = 0.14f; // Seconds a tapped shot takes to pull the string back and let go.
    // Facing the camera, the string hand comes back no higher than this (above the bottom of the
    // frame): the chest. Any higher and the arm would cover the face.
    private static final float STRING_HAND_LIMIT = 27f;
    private static final float[] FACING_ANGLE = {-90f, -135f, 180f, 135f, 90f, 45f, 0f, -45f}; // strip order
    private static final float SHOULDER_Y = 29f; // Both shoulders: above the bottom of the frame.
    private static final float STRING_HAND_DROP = 2f; // The hand's middle is this far below the top of its marker.
    // The sleeve part of the fist sprite gives the arm its colors: lit, shaded, and the outline.
    private static final int SLEEVE_COLUMN = 3;
    private static final int SLEEVE_OUTLINE_ROW = 1;
    private static final int SLEEVE_LIT_ROW = 2;
    private static final int SLEEVE_SHADE_ROW = 3;
    private static final float SLEEVE_THICKNESS = 2f; // An arm is this thick, with a 1 pixel outline round it.

    // casting with a staff: its head tips toward the target, and the shot leaves from the head
    private static final float STAFF_TILT = 38f; // Degrees, at most: when aiming straight left or right.
    private static final float LIGHT_CAST_TILT = 0.3f; // A light shot tips the staff only this much of that.
    private static final float STAFF_TILT_TIME = 0.15f; // Seconds the staff takes to tip over once charging shows.
    private static final float UPRIGHT = 90f; // The angle a held weapon points in its sprite sheet.
    private static final float CHARGE_GLOW = 0.6f; // How much brighter a fully charged weapon is drawn.
    // The moment a charge is full the weapon flashes white and a ring pops off it; from then on
    // its glow pulses, so there is never any doubt that the heavy attack is ready.
    private static final float READY_FLASH_TIME = 0.12f; // Seconds the flash takes to fade.
    private static final float READY_FLASH = 1f; // How much brighter the weapon is drawn at the start of the flash.
    private static final float READY_PULSE = 0.5f; // How far the glow dips, over and over, once the charge is full.
    private static final float BLADE_MIDDLE = 0.6f; // Where along a sword, hand to point, the ring is centered.
    private static final EffectType FIST_READY_EFFECT = EffectType.READY_EMBER; // The ring off a fully charged fist.
    private static final float AURA_START = 0.35f; // How strongly a sword's glow shows as the charge begins; it reaches 1 when full.
    private static final float AURA_PULSE = 0.3f; // How far the glow dips, over and over, once the charge is full.
    private static final float AURA_PULSE_RATE = 8f; // Pulses per second.
    private static final float STAFF_HEAD_DROP = 3f; // A staff gathers light this far below its very top.

    // punching
    // The fist sprite is a forearm and then a fist, pointing right. The forearm's columns are
    // stretched into a sleeve of any length, and the fist is drawn on the end of it.
    private static final int FIST_HEIGHT_PIXELS = 6;
    private static final float FIST_PIVOT_Y = 3f;
    private static final int HAND_START = 7; // The hand part of the sprite: where it starts, and how wide it is.
    private static final int HAND_WIDTH = 4;
    private static final float HAND_PIVOT_X = 1.5f;
    // An arm is an upper arm and a forearm, joined at the elbow. With the hand close in, the upper
    // arm hangs straight down from the shoulder and the forearm reaches across to the hand (seen
    // from above, a forearm pointing toward or away from the camera looks short). As the hand
    // gets further away the arm straightens out.
    private static final float UPPER_ARM = 5.25f;
    private static final float BENT_REACH = 6f; // A hand this close to the shoulder or closer: elbow fully tucked.
    private static final float STRAIGHT_REACH = 10.5f; // A hand this far or further: a straight arm.

    // the arms. One entry per facing, in strip order: s, sw, w, nw, n, ne, e, se.
    // Where each shoulder is across the hero's frame; a hand at rest hangs straight below it.
    private static final float[] RIGHT_ARM_X = {9.5f, 11f, 15f, 20.5f, 22f, 22f, 17.5f, 10f};
    private static final float[] LEFT_ARM_X = {22.5f, 22f, 14.5f, 10f, 10f, 11.5f, 17f, 21f};
    // Whether the body hides that arm: it is on the far side.
    private static final boolean[] RIGHT_ARM_BEHIND = {false, true, true, true, false, false, false, false};
    private static final boolean[] LEFT_ARM_BEHIND = {false, false, false, false, false, true, true, true};
    // Whether the arm carrying a sword at rest is drawn over the sword: side-on to the right, and facing away.
    private static final boolean[] ARM_OVER_SWORD = {false, false, false, false, true, true, true, false};
    // Whether a bow's sheet for that facing is a mirror image of another facing's: down-left and up-right.
    private static final boolean[] BOW_MIRRORED = {false, true, false, false, false, true, false, false};
    private static final float REST_HAND_Y = 18.5f; // A hanging hand, above the bottom of the frame: a straight arm below the shoulder.
    private static final float FIST_HEIGHT = 19f; // Height of the hands above the feet.
    private static final float PUNCH_TIME = 0.2f; // Seconds the fist takes to swing out and back.
    // A punch is thrown from the punching shoulder's side of the body, not from its middle, and
    // reaches no further than an arm can: about 11 pixels from the shoulder.
    private static final float FIST_REST = 3f; // How far out from that side the fist starts and ends.
    private static final float FIST_EXTEND_RAISE = 3f; // A thrown fist rises this far, toward shoulder height.
    private static final float PUNCH_EFFECT_AHEAD = 3f; // The burst plays this far beyond the fist at full reach.
    // Charging a punch: the fist is drawn smoothly back past the body and up to the chest, then held still.
    private static final float FIST_BACK = 1f; // How far behind the shoulder the fist ends up.
    private static final float COCKED_FOREARM = 6.5f; // How far behind the fist the elbow is pulled.
    private static final float FIST_RAISE = 3f; // How far it is lifted on the way.
    private static final float FIST_BACK_TIME = 0.3f; // Seconds it takes to get there once charging shows.
    private static final float LIGHT_FIST_REACH = 9f;
    private static final float HEAVY_FIST_REACH = 11.5f;
    private static final float AIMING_AWAY = 0.4f; // Aiming this far up-screen puts the fist behind the body.

    // blocking
    private static final float BLOCK_SPEED_FACTOR = 0.5f; // Speed multiplier with the shield raised.

    // picking up / dropping weapons
    private static final float PICKUP_RANGE = 22f; // How close a dropped weapon must be to pick it up.
    private static final float DROP_DISTANCE = 14f; // A dropped weapon lands this far in front of the hero.
    private static final float DROP_CLEARANCE = 8f; // Free floor a dropped weapon needs around its middle.

    // How the walk moves what the hero holds, one entry per step of the 8-step cycle.
    // These match the walk sheets: the body is lowest (1) as a foot lands and highest (-1) as the legs pass,
    // and seen from the front or back it shifts over the planted foot.
    private static final int[] WALK_BOB = {1, 0, -1, 0, 1, 0, -1, 0};
    private static final int[] WALK_SWAY = {0, 1, 1, 0, 0, -1, -1, 0};

    // Order of the frames in the sprite strip, left to right.
    private static final String[] STRIP_ORDER = {"s", "sw", "w", "nw", "n", "ne", "e", "se"};
    // The same facings going round counterclockwise from east, 45 degrees apart.
    private static final String[] FACING_BY_ANGLE = {"e", "ne", "n", "nw", "w", "sw", "s", "se"};

    private final HeroClass heroClass;
    private final Texture texture;
    private final Texture walkTexture;
    private final Texture fistTexture;
    private final Texture pixelTexture; // one white pixel, stretched and tinted to draw the arms
    private final Color sleeveLit;
    private final Color sleeveShade;
    private final Color sleeveOutline;
    private final Cursor customCursor;
    private final Map<String, TextureRegion> facingFrames = new HashMap<>();
    private final Map<String, TextureRegion[]> walkFrames = new HashMap<>(); // the walk cycle for each facing
    private final Map<String, Integer> facingIndex = new HashMap<>(); // column of each facing in the sprite sheets
    private final TileMap tileMap;
    private final GameWorld world;
    private final int upKey, downKey, leftKey, rightKey;
    private final Vector2 movement = new Vector2();
    private final Vector3 mouse = new Vector3();
    private Weapon weapon; // null = empty-handed
    private OffHand offHand; // null = nothing on the other arm
    private HeldSprite weaponSprite;
    private HeldSprite[] weaponChargeSprites = new HeldSprite[0];
    private HeldSprite[] weaponChargeHandSprites = new HeldSprite[0];
    private HeldSprite weaponGripMarker;
    private HeldSprite weaponAuraSprite;
    private HeldSprite offHandStowedSprite;
    private final Vector2 staffHead = new Vector2();
    private final Vector2 rightHand = new Vector2();
    private final Vector2 leftHand = new Vector2();
    // where a swinging weapon is drawn this frame
    private float swingX;
    private float swingY;
    private float swingRotation;
    private HeldSprite offHandSprite;
    private HeldSprite offHandBlockSprite;
    // (x, y) is the middle of the hero's feet, in map pixels.
    private float x;
    private float y;
    private String facing = "s";
    private float aimAngle = -90f; // Degrees toward the mouse: 0 = east, counterclockwise.
    private boolean moving;
    private float animationTime;
    private boolean charging;
    private float chargeTime;
    private float cooldown;
    private float lungeTimer;
    private float punchTimer;
    private float swingTime = -1f; // Seconds into the current swing; below 0 when not swinging.
    private float quickDraw = -1f; // Seconds into a tapped bow shot's pull; below 0 when there is none.
    private float swingWindup;
    private float swingHold;
    private float swingRecover;
    private int swingCount;
    private float swingArc;
    private boolean swingLanded;
    private boolean swingHeavy;
    private float swingSide = 1f; // 1 or -1: which way round the blade goes so that it comes down from the top.
    private float swingStartRotation; // How far the blade was already turned when the swing began.
    private float swingStartX; // And how far the hand had been raised, from where it carries the sword.
    private float swingStartY;
    private final Vector2 swordRaise = new Vector2();
    // How far each part of the sword arm is through a wind-up, 0 to 1 (see windUp).
    private float elbowRaised;
    private float handRaised;
    private float bladeBack;
    private boolean punchHeavy;
    private float castTilt = 1f; // How far the staff was tipped as the last shot left it, 0 to 1.
    private float punchFrom; // How far out the fist was when the punch began: drawn back, if it was charged.
    private float punchRaise; // And how far it was lifted.
    private boolean blocking;

    public Hero(HeroClass heroClass, TileMap tileMap, GameWorld world, GridPoint2 spawnTile) {
        this(heroClass, tileMap, world, spawnTile, Input.Keys.W, Input.Keys.S, Input.Keys.A, Input.Keys.D);
    }

    public Hero(HeroClass heroClass, TileMap tileMap, GameWorld world, GridPoint2 spawnTile, int upKey, int downKey, int leftKey, int rightKey) {
        this.heroClass = heroClass;
        this.tileMap = tileMap;
        this.world = world;
        this.upKey = upKey;
        this.downKey = downKey;
        this.leftKey = leftKey;
        this.rightKey = rightKey;

        texture = new Texture(Gdx.files.internal(heroClass.spriteFile));
        walkTexture = new Texture(Gdx.files.internal(heroClass.walkFile));
        Pixmap fistPixmap = new Pixmap(Gdx.files.internal(heroClass.fistFile));
        fistTexture = new Texture(fistPixmap);
        sleeveLit = new Color(fistPixmap.getPixel(SLEEVE_COLUMN, SLEEVE_LIT_ROW));
        sleeveShade = new Color(fistPixmap.getPixel(SLEEVE_COLUMN, SLEEVE_SHADE_ROW));
        sleeveOutline = new Color(fistPixmap.getPixel(SLEEVE_COLUMN, SLEEVE_OUTLINE_ROW));
        fistPixmap.dispose();
        Pixmap whitePixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        whitePixel.setColor(Color.WHITE);
        whitePixel.fill();
        pixelTexture = new Texture(whitePixel);
        whitePixel.dispose();
        int walkSteps = walkTexture.getHeight() / FRAME_HEIGHT;
        for (int i = 0; i < STRIP_ORDER.length; i++) {
            facingIndex.put(STRIP_ORDER[i], i);
            facingFrames.put(STRIP_ORDER[i], new TextureRegion(texture, i * FRAME_WIDTH, 0, FRAME_WIDTH, FRAME_HEIGHT));
            TextureRegion[] cycle = new TextureRegion[walkSteps];
            for (int step = 0; step < walkSteps; step++) {
                cycle[step] = new TextureRegion(walkTexture, i * FRAME_WIDTH, step * FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);
            }
            walkFrames.put(STRIP_ORDER[i], cycle);
        }
        x = spawnTile.x * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f;
        y = spawnTile.y * TileMap.TILE_SIZE + TileMap.TILE_SIZE / 2f - HITBOX_HEIGHT / 2f;

        equip(heroClass.startingWeapon);
        setOffHand(heroClass.startingOffHand);

        // crosshair: always shown, since the hero always aims at the mouse
        Pixmap pixmap = new Pixmap(Gdx.files.internal("ui/crosshair.png"));
        int xHotspot = pixmap.getWidth() / 2;
        int yHotspot = pixmap.getHeight() / 2;
        customCursor = Gdx.graphics.newCursor(pixmap, xHotspot, yHotspot);
        pixmap.dispose();
        Gdx.graphics.setCursor(customCursor);
    }

    private int held(int key) {
        if (Gdx.input.isKeyPressed(key)) {
            return 1;
        }
        return 0;
    }

    /** Either swaps weapon, or empties the hand. Any hero can hold any weapon. */
    public void equip(Weapon newWeapon) {
        disposeWeaponSprites();
        weapon = newWeapon;
        weaponSprite = newWeapon == null ? null : new HeldSprite(newWeapon.spriteFile);
        weaponChargeSprites = new HeldSprite[newWeapon == null ? 0 : newWeapon.chargeSpriteFiles.length];
        for (int i = 0; i < weaponChargeSprites.length; i++) {
            weaponChargeSprites[i] = new HeldSprite(newWeapon.chargeSpriteFiles[i]);
        }
        weaponChargeHandSprites = new HeldSprite[newWeapon == null ? 0 : newWeapon.chargeHandFiles.length];
        for (int i = 0; i < weaponChargeHandSprites.length; i++) {
            weaponChargeHandSprites[i] = new HeldSprite(newWeapon.chargeHandFiles[i]);
        }
        weaponGripMarker = newWeapon == null || newWeapon.gripMarkerFile == null ? null : new HeldSprite(newWeapon.gripMarkerFile);
        weaponAuraSprite = newWeapon == null || newWeapon.auraFile == null ? null : new HeldSprite(newWeapon.auraFile);
        if (weaponAuraSprite != null) weaponAuraSprite.turnAbout(weaponSprite);
        swingTime = -1f;
        quickDraw = -1f;
    }

    private void disposeWeaponSprites() {
        if (weaponSprite != null) weaponSprite.dispose();
        for (HeldSprite sprite : weaponChargeSprites) sprite.dispose();
        for (HeldSprite sprite : weaponChargeHandSprites) sprite.dispose();
        if (weaponGripMarker != null) weaponGripMarker.dispose();
        if (weaponAuraSprite != null) weaponAuraSprite.dispose();
    }

    /**
     * How far a staff is tipped over.
     * It tips most aiming left or right; aiming up or down the screen cannot be seen, so it stays upright.
     */
    private float staffTilt(float amount) {
        return -STAFF_TILT * MathUtils.cosDeg(aimAngle) * amount;
    }

    /**
     * Works out how far the elbow, the hand and the blade each are through a sword's wind-up,
     * from how long the charge has been showing. They move one after another, overlapping.
     */
    private void windUp() {
        float through = chargeTime >= CHARGE_SHOWN_AFTER ? Math.min(1f, (chargeTime - CHARGE_SHOWN_AFTER) / SWORD_BACK_TIME) : 0f;
        elbowRaised = easeInOut(MathUtils.clamp(through / ELBOW_DONE, 0f, 1f));
        handRaised = easeInOut(MathUtils.clamp((through - HAND_STARTS) / (HAND_DONE - HAND_STARTS), 0f, 1f));
        bladeBack = easeInOut(MathUtils.clamp((through - BLADE_STARTS) / (1f - BLADE_STARTS), 0f, 1f));
    }

    /**
     * How far a sword is tipped back while a heavy attack is charged
     * Aiming left or right the point goes back away from the target,
     * past level, behind the head. Aiming up or down the screen would be toward
     * or away from the camera, so the blade leans out over the sword shoulder instead.
     */
    private float swordTilt(int column, float amount) {
        float sideways = MathUtils.cosDeg(aimAngle);
        float outward = Math.signum(RIGHT_ARM_X[column] - FRAME_WIDTH / 2f);
        return (SWORD_BACK_TILT * sideways - SWORD_SIDE_TILT * outward * (1f - Math.abs(sideways))) * amount;
    }

    /**
     * How far the hand has moved from where it carries a sword
     * raising it for a heavy attack: up to beside the shoulder, and back from the target.
     */
    private Vector2 swordRaise(int column, float amount) {
        float gripX = weaponSprite.getGripX(column);
        float raisedX = MathUtils.lerp(gripX, RIGHT_ARM_X[column], SWORD_HAND_IN) - SWORD_HAND_BACK * MathUtils.cosDeg(aimAngle);
        float raisedY = SHOULDER_Y + SWORD_HAND_HEIGHT;
        // Rising runs ahead of moving back, so the hand goes up and over, not straight across.
        float risen = 1f - (float) Math.pow(1f - amount, SWORD_ARC);
        float gone = (float) Math.pow(amount, SWORD_ARC);
        // On the way the hand swings out toward the target and comes back, rounding the path into an arc.
        float forward = SWORD_ARC_FORWARD * MathUtils.cosDeg(aimAngle) * MathUtils.sin(MathUtils.PI * amount);
        return swordRaise.set((raisedX - gripX) * gone + forward, (raisedY - weaponSprite.getGripY(column)) * risen);
    }

    /** Where the head of the staff is on screen, with the staff drawn at (weaponX, weaponY) and tipped by `tilt`. */
    private Vector2 staffHeadAt(int column, float weaponX, float weaponY, float tilt) {
        float gripX = weaponSprite.getGripX(column);
        float gripY = weaponSprite.getGripY(column);
        staffHead.set(weaponSprite.getTipX(column) - gripX, weaponSprite.getTipY(column) - STAFF_HEAD_DROP - gripY);
        return staffHead.rotateDeg(tilt).add(weaponX + gripX, weaponY + gripY);
    }

    /**
     * How far a bow is lifted at one stage of the draw. While facing camera, pulling the string back
     * brings the hand up the screen, so the bow is held lower, keeping that hand
     * at the chest and off the face.
     */
    private float bowRaise(int column, int stage) {
        float raise = BOW_RAISE[Math.min(stage, BOW_RAISE.length - 1)];
        float facingY = MathUtils.sinDeg(FACING_ANGLE[column]);
        if (facingY < 0f) {
            float push = BOW_PUSH[Math.min(stage, BOW_PUSH.length - 1)];
            float over = Math.round(facingY * push) + Math.round(raise) + weaponChargeHandSprites[stage].getTipY(column)
                    - STRING_HAND_DROP - STRING_HAND_LIMIT;
            if (over > 0f) raise -= MathUtils.ceil(over);
        }
        return raise;
    }

    /** Puts something on the hero's other arm, or takes it off (null). */
    public void setOffHand(OffHand newOffHand) {
        if (offHandSprite != null) offHandSprite.dispose();
        if (offHandBlockSprite != null) offHandBlockSprite.dispose();
        if (offHandStowedSprite != null) offHandStowedSprite.dispose();
        offHand = newOffHand;
        offHandSprite = newOffHand == null ? null : new HeldSprite(newOffHand.spriteFile);
        offHandBlockSprite = newOffHand == null ? null : new HeldSprite(newOffHand.blockFile);
        offHandStowedSprite = newOffHand == null ? null : new HeldSprite(newOffHand.stowedFile);
    }

    /** The weapon in hand, or null if the hero is empty-handed. */
    public Weapon getWeapon() {
        return weapon;
    }

    public OffHand getOffHand() {
        return offHand;
    }

    /** Whether the weapon in hand is the type this hero's class is best with. */
    public boolean hasFavoredWeapon() {
        return weapon != null && weapon.type == heroClass.favoredType;
    }

    /** A light attack's damage: the weapon's, with the class bonus if it is the favored type, or a punch. */
    public float getAttackDamage() {
        if (weapon == null) return PUNCH_DAMAGE;
        return weapon.damage * (hasFavoredWeapon() ? FAVORED_DAMAGE_BONUS : 1f);
    }

    /** Movement speed in map pixels per second right now. */
    public float getSpeed() {
        float speed = SPEED;
        if (offHand != null && heroClass.slowedByShield) speed *= SHIELD_SPEED_FACTOR;
        if (blocking) speed *= BLOCK_SPEED_FACTOR;
        if (charging && chargeTime >= CHARGE_SHOWN_AFTER) speed *= CHARGE_SPEED_FACTOR;
        return speed;
    }

    /** Whether the shield is raised. Nothing attacks the hero yet, so this only changes speed and the drawing. */
    public boolean isBlocking() {
        return blocking;
    }

    /** False while a two-handed weapon is in use: the shield is slung on the back and cannot block. */
    private boolean offHandShown() {
        return offHand != null && (weapon == null || !weapon.twoHanded);
    }

    private boolean blockedAt(float feetX, float feetY) {
        return tileMap.overlapsSolid(feetX, feetY + HITBOX_HEIGHT / 2f, HITBOX_WIDTH, HITBOX_HEIGHT);
    }

    /**
     * Drops the weapon in hand on the floor just in front of the hero, pointing the way the hero
     * was aiming it. If a wall is in the way it lands at the hero's feet instead.
     */
    private void dropWeapon() {
        if (weapon == null) return;
        float feetY = y + HITBOX_HEIGHT / 2f;
        float dropX = x + MathUtils.cosDeg(aimAngle) * DROP_DISTANCE;
        float dropY = feetY + MathUtils.sinDeg(aimAngle) * DROP_DISTANCE;
        if (tileMap.overlapsSolid(dropX, dropY, DROP_CLEARANCE, DROP_CLEARANCE)) {
            dropX = x;
            dropY = feetY;
        }
        world.dropWeapon(weapon, dropX, dropY, aimAngle);
        equip(null);
    }

    /** Picks up the nearest dropped weapon. A weapon already in hand is dropped first. */
    private void pickUpWeapon() {
        Weapon found = world.takeNearestWeapon(x, y + HITBOX_HEIGHT / 2f, PICKUP_RANGE);
        if (found == null) return;
        dropWeapon();
        equip(found);
    }

    /** Seconds the current swing lasts, wind-up to recovery. */
    private float swingLength() {
        return swingWindup + SWING_TIME * swingCount + swingHold + swingRecover;
    }

    /** The moment a swing's wind-up ends and the blade comes across, the blow lands and the slash shows. */
    private void landSwing() {
        swingLanded = true;
        if (weapon == null) return;
        float damage = getAttackDamage() * (swingHeavy ? HEAVY_DAMAGE_FACTOR : 1f);
        // The slash traces the blade, which turns about the hand, so it is centered on the hand.
        int column = facingIndex.get(facing);
        float handX = Math.round(x - FRAME_WIDTH / 2f) + weaponSprite.getGripX(column);
        float handY = Math.round(y - FEET_OFFSET) + weaponSprite.getGripY(column);
        EffectType slash = swingHeavy ? weapon.heavyEffect : weapon.lightEffect;
        // A blade in the far hand, or swung at something up the screen, cuts on the far side of the hero.
        boolean farSide = weaponSprite.isBehindBody(column) || MathUtils.sinDeg(aimAngle) > AIMING_AWAY;
        // The slash is drawn cutting downward when it points right; aiming left it is mirrored to match the blade.
        world.playEffect(slash, handX, handY, slash.turnsWithAim() ? aimAngle : 0f, farSide, swingSide < 0f);
        world.addMeleeHit(x + MathUtils.cosDeg(aimAngle) * MELEE_REACH, y + MathUtils.sinDeg(aimAngle) * MELEE_REACH,
                swingHeavy ? HEAVY_MELEE_RADIUS : MELEE_RADIUS, damage);
    }

    /** Attacks toward the mouse: a punch, a swing, an arrow, or a bolt, depending on what is in hand. */
    private void attack(boolean heavy) {
        float damage = getAttackDamage() * (heavy ? HEAVY_DAMAGE_FACTOR : 1f);
        float directionX = MathUtils.cosDeg(aimAngle);
        float directionY = MathUtils.sinDeg(aimAngle);
        float chestY = y + AIM_HEIGHT;

        if (weapon == null) {
            float hitX = x + directionX * PUNCH_REACH;
            float hitY = chestY + directionY * PUNCH_REACH;
            EffectType punch = heavy ? EffectType.PUNCH_HEAVY : EffectType.PUNCH_LIGHT;
            // The burst goes where the fist ends up, just beyond it.
            fistAt((heavy ? HEAVY_FIST_REACH : LIGHT_FIST_REACH) + PUNCH_EFFECT_AHEAD, FIST_EXTEND_RAISE);
            world.playEffect(punch, rightHand.x, rightHand.y, punch.turnsWithAim() ? aimAngle : 0f);
            world.addMeleeHit(hitX, hitY - AIM_HEIGHT, PUNCH_RADIUS, damage);
            punchTimer = PUNCH_TIME;
            punchHeavy = heavy;
            float back = fistDrawnBack();
            punchFrom = MathUtils.lerp(FIST_REST, -FIST_BACK, back);
            punchRaise = FIST_RAISE * back * Math.abs(directionX);
        } else if (weapon.type == WeaponType.MELEE) {
            // The swing starts now; the blow lands when the wind-up is over (landSwing).
            swingHeavy = heavy;
            swingCount = heavy ? weapon.heavySwings : 1;
            swingArc = heavy ? HEAVY_SWING_ARC : LIGHT_SWING_ARC;
            swingWindup = heavy ? HEAVY_WINDUP_TIME : LIGHT_WINDUP_TIME;
            swingHold = heavy ? HEAVY_HOLD_TIME : LIGHT_HOLD_TIME;
            swingRecover = heavy ? HEAVY_RECOVER_TIME : LIGHT_RECOVER_TIME;
            swingTime = 0f;
            // Aiming left, "above the aim" is the other way round from aiming right.
            swingSide = directionX >= 0f ? 1f : -1f;
            // A charged swing starts from where the charge left the blade: drawn back.
            windUp();
            swingStartRotation = swordTilt(facingIndex.get(facing), bladeBack);
            Vector2 raised = swordRaise(facingIndex.get(facing), handRaised);
            swingStartX = raised.x;
            swingStartY = raised.y;
            swingLanded = false;
            lungeTimer = 0f;
            cooldown = Math.max(heavy ? HEAVY_COOLDOWN : LIGHT_COOLDOWN, swingLength());
            return;
        } else if (weapon.type == WeaponType.RANGED && !heavy && chargeTime < CHARGE_SHOWN_AFTER) {
            // A tapped shot: the string is pulled back first, quickly, and the arrow goes as it is let go.
            quickDraw = 0f;
            cooldown = LIGHT_COOLDOWN + QUICK_DRAW_TIME;
            return;
        } else {
            shoot(heavy);
        }
        lungeTimer = LUNGE_TIME;
        cooldown = heavy ? HEAVY_COOLDOWN : LIGHT_COOLDOWN;
    }

    /**
     * How far a staff is tipped toward the target by the charge so far, 0 to 1. It tips only a
     * little at first, as far as a light shot needs, and all the way once the charge is a heavy one.
     */
    private float staffCharged() {
        if (chargeTime < CHARGE_SHOWN_AFTER) return LIGHT_CAST_TILT;
        float light = Math.min(1f, (chargeTime - CHARGE_SHOWN_AFTER) / STAFF_TILT_TIME) * LIGHT_CAST_TILT;
        float heavy = MathUtils.clamp((chargeTime - CHARGE_SHOWN_AFTER - STAFF_TILT_TIME) / (HEAVY_CHARGE_TIME - CHARGE_SHOWN_AFTER - STAFF_TILT_TIME), 0f, 1f);
        return MathUtils.lerp(light, 1f, easeInOut(heavy));
    }

    /** Sends an arrow or a bolt off toward the mouse. */
    private void shoot(boolean heavy) {
        if (weapon.type == WeaponType.MAGIC) castTilt = heavy ? 1f : staffCharged();
        float damage = getAttackDamage() * (heavy ? HEAVY_DAMAGE_FACTOR : 1f);
        float speed = (weapon.type == WeaponType.RANGED ? ARROW_SPEED : BOLT_SPEED) * (heavy ? HEAVY_SPEED_FACTOR : 1f);
        float startX = x + MathUtils.cosDeg(aimAngle) * MUZZLE_DISTANCE;
        float startY = y + MathUtils.sinDeg(aimAngle) * MUZZLE_DISTANCE;
        float startHeight = ARROW_START_HEIGHT;
        if (weapon.type == WeaponType.RANGED) {
            // An arrow leaves from the middle of the bow, where the bow is held at full draw.
            int column = facingIndex.get(facing);
            int stage = weaponChargeHandSprites.length - 1;
            float push = BOW_PUSH[Math.min(stage, BOW_PUSH.length - 1)];
            startX = Math.round(x - FRAME_WIDTH / 2f) + Math.round(MathUtils.cosDeg(FACING_ANGLE[column]) * push)
                    - (column == FACING_CAMERA ? BOW_INSET[Math.min(stage, BOW_INSET.length - 1)] : 0f)
                    + weaponGripMarker.getTipX(column);
            float bowY = Math.round(y - FEET_OFFSET) + Math.round(MathUtils.sinDeg(FACING_ANGLE[column]) * push)
                    + Math.round(bowRaise(column, stage)) + weaponGripMarker.getTipY(column) - STRING_HAND_DROP;
            startHeight = bowY - startY;
        } else if (weapon.type == WeaponType.MAGIC) {
            // A bolt leaves from the head of the tipped staff, then levels out.
            Vector2 head = staffHeadAt(facingIndex.get(facing), Math.round(x - FRAME_WIDTH / 2f),
                    Math.round(y - FEET_OFFSET), staffTilt(castTilt));
            startX = head.x;
            startHeight = head.y - startY;
        }
        world.fireProjectile(heavy ? weapon.heavyEffect : weapon.lightEffect, heavy ? weapon.heavyTrail : null,
                weapon.impactEffect, damage, startX, startY, aimAngle, speed, startHeight,
                (float) Math.hypot(mouse.x - startX, mouse.y - GameWorld.FLIGHT_HEIGHT - startY));
    }

    public void update(float deltaSeconds, OrthographicCamera camera) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) dropWeapon();
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) pickUpWeapon();

        // The hero always faces the mouse. The mouse is in window pixels; the camera turns that into a spot on the map.
        camera.unproject(mouse.set(Gdx.input.getX(), Gdx.input.getY(), 0));
        float aimX = mouse.x - x;
        float aimY = mouse.y - (y + AIM_HEIGHT);
        if (aimX != 0 || aimY != 0) {
            aimAngle = MathUtils.atan2(aimY, aimX) * MathUtils.radiansToDegrees;
            // 360 / 8 = 45 degrees per facing, each reaching 22.5 degrees either side of its center.
            facing = FACING_BY_ANGLE[Math.floorMod(Math.round(aimAngle / 45f), 8)];
        }

        // right click: raise the shield
        blocking = offHandShown() && Gdx.input.isButtonPressed(Input.Buttons.RIGHT);

        // left click: tap for a light attack, hold and release for a heavy one
        cooldown = Math.max(0f, cooldown - deltaSeconds);
        lungeTimer = Math.max(0f, lungeTimer - deltaSeconds);
        punchTimer = Math.max(0f, punchTimer - deltaSeconds);
        if (swingTime >= 0f) {
            swingTime += deltaSeconds;
            if (!swingLanded && swingTime >= swingWindup) landSwing();
            if (swingTime >= swingLength()) swingTime = -1f;
        }
        if (quickDraw >= 0f) {
            quickDraw += deltaSeconds;
            if (quickDraw >= QUICK_DRAW_TIME) {
                quickDraw = -1f;
                shoot(false);
                lungeTimer = LUNGE_TIME;
            }
        }
        boolean attackHeld = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        if (blocking) {
            charging = false;
        } else if (!charging && attackHeld && cooldown == 0f) {
            charging = true;
            chargeTime = 0f;
        } else if (charging && attackHeld) {
            chargeTime += deltaSeconds;
        } else if (charging) {
            charging = false;
            attack(chargeTime >= HEAVY_CHARGE_TIME);
        }

        int dx = held(rightKey) - held(leftKey);
        int dy = held(upKey) - held(downKey);

        // A diagonal and a straight step cover the same distance in one second.
        movement.set(dx, dy).nor();
        float distance = getSpeed() * deltaSeconds;

        // Each axis moves on its own, so a hero pushing diagonally into a wall slides along it.
        float startX = x;
        float startY = y;
        float nextX = x + movement.x * distance;
        if (!blockedAt(nextX, y)) x = nextX;
        float nextY = y + movement.y * distance;
        if (!blockedAt(x, nextY)) y = nextY;

        // The walk cycle plays while the hero is actually getting somewhere, and restarts when they stop.
        moving = x != startX || y != startY;
        // The legs keep pace with the ground covered: a slowed hero takes slower steps.
        animationTime = moving ? animationTime + deltaSeconds * getSpeed() / SPEED : 0f;
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
        int column = facingIndex.get(facing);
        // While walking, a held weapon stays with the body side to side and rides up and down one step
        // behind it; a shield is strapped on, so it moves exactly with the body.
        int sway = 0;
        int weaponLift = 0;
        int bodyLift = 0;
        if (moving) {
            TextureRegion[] cycle = walkFrames.get(facing);
            int step = (int) (animationTime * WALK_FPS) % cycle.length;
            sprite = cycle[step];
            boolean sideView = facing.equals("w") || facing.equals("e");
            sway = sideView ? 0 : WALK_SWAY[step % WALK_SWAY.length];
            bodyLift = -WALK_BOB[step % WALK_BOB.length];
            weaponLift = -WALK_BOB[(step + WALK_BOB.length - 1) % WALK_BOB.length];
        }
        float drawX = Math.round(x - FRAME_WIDTH / 2f);
        float drawY = Math.round(y - FEET_OFFSET);
        float bodyX = drawX + sway;
        float bodyY = drawY + bodyLift;
        float aimDirectionX = MathUtils.cosDeg(aimAngle);
        float aimDirectionY = MathUtils.sinDeg(aimAngle);

        boolean chargeShown = charging && chargeTime >= CHARGE_SHOWN_AFTER;
        // On an attack the weapon thrusts out toward the target and comes back; while charging it is drawn back.
        boolean swinging = weapon != null && swingTime >= 0f;
        float lunge = LUNGE_DISTANCE * lungeTimer / LUNGE_TIME;
        if (swinging) {
            lunge = 0f;
        } else if (chargeShown) {
            // A bow is not drawn back as a whole: its string is, in the charge sprites.
            lunge = weapon != null && weapon.type == WeaponType.RANGED ? 0f : -CHARGE_PULL_BACK;
        }
        HeldSprite heldSprite = weaponSprite;
        HeldSprite stringHand = null;
        float bowPush = 0f;
        float bowRaise = 0f;
        float bowInset = 0f;
        boolean quickDrawing = quickDraw >= 0f;
        if ((chargeShown || quickDrawing) && weaponChargeSprites.length > 0) {
            float charged = quickDrawing ? quickDraw / QUICK_DRAW_TIME : (chargeTime - CHARGE_SHOWN_AFTER) / (HEAVY_CHARGE_TIME - CHARGE_SHOWN_AFTER);
            int stage = Math.min(weaponChargeSprites.length - 1, (int) (charged * weaponChargeSprites.length));
            heldSprite = weaponChargeSprites[stage];
            stringHand = weaponChargeHandSprites[stage];
            bowPush = BOW_PUSH[Math.min(stage, BOW_PUSH.length - 1)];
            bowRaise = bowRaise(column, stage);
            bowInset = BOW_INSET[Math.min(stage, BOW_INSET.length - 1)];
        } else if (weapon != null && weapon.type == WeaponType.RANGED && lungeTimer > 0f) {
            // After the arrow goes the bow arm stays out, pointing at the target, and comes down.
            float held = lungeTimer / LUNGE_TIME;
            bowPush = BOW_PUSH[BOW_PUSH.length - 1] * held;
            bowRaise = bowRaise(column, weaponChargeHandSprites.length - 1) * held;
            bowInset = BOW_INSET[BOW_INSET.length - 1] * held;
            lunge = 0f;
        }
        // A staff tips toward the target while a shot is charged, and for a moment as one is fired.
        boolean casting = weapon != null && weapon.type == WeaponType.MAGIC && (chargeShown || lungeTimer > 0f);
        float tilt = 0f;
        if (casting) {
            float cast = chargeShown ? staffCharged() : castTilt * lungeTimer / LUNGE_TIME;
            tilt = staffTilt(cast);
        }
        // A bow being drawn is pushed out ahead, the way the hero faces, so the string hand stays by the chin.
        float weaponX = drawX + sway + Math.round(aimDirectionX * lunge) + Math.round(MathUtils.cosDeg(FACING_ANGLE[column]) * bowPush)
                - (column == FACING_CAMERA ? Math.round(bowInset) : 0);
        float weaponY = drawY + weaponLift + Math.round(aimDirectionY * lunge) + Math.round(MathUtils.sinDeg(FACING_ANGLE[column]) * bowPush) + Math.round(bowRaise);
        boolean facingCamera = MathUtils.sinDeg(FACING_ANGLE[column]) < 0f;
        float swordRaised = 0f; // how far a sword has been raised for a heavy attack, 0 to 1
        if (weapon != null && weapon.type == WeaponType.MELEE && chargeShown && !swinging) {
            windUp();
            swordRaised = elbowRaised;
            tilt = swordTilt(column, bladeBack);
            Vector2 raised = swordRaise(column, handRaised);
            weaponX += Math.round(raised.x);
            weaponY += Math.round(raised.y);
        }
        // A weapon in the far hand stays on the far side of the body, and of the shield, even while it is swung.
        boolean weaponBehind = weaponSprite != null && weaponSprite.isBehindBody(column);
        // The weapon itself brightens as the charge builds.
        float glow = chargeShown ? CHARGE_GLOW * Math.min(1f, chargeTime / HEAVY_CHARGE_TIME) : 0f;
        if (chargeShown && chargeTime >= HEAVY_CHARGE_TIME) {
            float sinceFull = chargeTime - HEAVY_CHARGE_TIME;
            if (sinceFull < READY_FLASH_TIME) {
                glow = MathUtils.lerp(READY_FLASH, CHARGE_GLOW, sinceFull / READY_FLASH_TIME);
            } else {
                glow *= 1f - READY_PULSE * (0.5f - 0.5f * MathUtils.cos(MathUtils.PI2 * AURA_PULSE_RATE * (sinceFull - READY_FLASH_TIME)));
            }
        }

        // A sword being charged is wrapped in light, which builds with the charge and pulses once it is full.
        float aura = 0f;
        if (chargeShown && weaponAuraSprite != null) {
            aura = MathUtils.lerp(AURA_START, 1f, Math.min(1f, chargeTime / HEAVY_CHARGE_TIME));
            if (chargeTime >= HEAVY_CHARGE_TIME) {
                aura -= AURA_PULSE * (0.5f + 0.5f * MathUtils.sin(MathUtils.PI2 * AURA_PULSE_RATE * chargeTime));
            }
        }

        // A blocking hero holds the shield out between the body and whatever they are facing; with both
        // hands on a weapon it is slung on the back.
        HeldSprite shieldSprite = blocking ? offHandBlockSprite : offHandSprite;
        if (offHand != null && !offHandShown()) shieldSprite = offHandStowedSprite;

        // The right arm: on the weapon, punching, on a bowstring, or hanging.
        // The bow's sheets for facing down-left and up-right are mirror images of down-right and
        // up-left, so there the bow is on the other side of the body. The arms swap shoulders to
        // match: the bow arm comes from the shoulder beside the bow, not from across the chest.
        boolean armsSwapped = weapon != null && weapon.type == WeaponType.RANGED && BOW_MIRRORED[column];
        float rightShoulderX = bodyX + (armsSwapped ? LEFT_ARM_X : RIGHT_ARM_X)[column];
        float shoulderY = bodyY + SHOULDER_Y;
        boolean rightBehind = (armsSwapped ? LEFT_ARM_BEHIND : RIGHT_ARM_BEHIND)[column];
        boolean onString = false;
        rightHand.set(rightShoulderX, drawY + weaponLift + REST_HAND_Y);
        if (weapon == null) {
            if (punchTimer > 0f || chargeShown) {
                placeFist(chargeShown);
                // The far arm stays beyond the body, and so behind a shield on the near arm.
                rightBehind = RIGHT_ARM_BEHIND[column] || aimDirectionY > AIMING_AWAY;
            }
        } else if (weapon.type == WeaponType.RANGED) {
            if (stringHand != null) {
                rightHand.set(weaponX + stringHand.getTipX(column), weaponY + stringHand.getTipY(column) - STRING_HAND_DROP);
                // Facing away, the arm on the string is beyond the body; otherwise it reaches across in front.
                rightBehind = facing.equals("n") || facing.equals("nw") || facing.equals("ne");
                onString = true;
            }
        } else {
            if (swinging) {
                placeSwing(column, weaponX, weaponY);
                rightHand.set(swingX, swingY);
            } else {
                rightHand.set(weaponX, weaponY);
            }
            rightHand.add(weaponSprite.getGripX(column), weaponSprite.getGripY(column));
            if (weapon.type == WeaponType.MELEE && !swinging) rightHand.y -= SWORD_GRIP_DROP;
            rightBehind = weaponBehind;
        }

        // The left arm: holding the bow, carrying the shield, or hanging.
        float leftShoulderX = bodyX + (armsSwapped ? RIGHT_ARM_X : LEFT_ARM_X)[column];
        boolean leftBehind = (armsSwapped ? RIGHT_ARM_BEHIND : LEFT_ARM_BEHIND)[column];
        boolean onBow = weaponGripMarker != null;
        leftHand.set(leftShoulderX, drawY + weaponLift + REST_HAND_Y);
        if (onBow) {
            leftHand.set(weaponX + weaponGripMarker.getTipX(column), weaponY + weaponGripMarker.getTipY(column) - STRING_HAND_DROP);
            leftBehind = weaponBehind;
        } else if (offHandShown()) {
            leftHand.set(bodyX + shieldSprite.getGripX(column), bodyY + shieldSprite.getGripY(column));
            leftBehind = shieldSprite.isBehindBody(column);
        }

        // Seen from behind, a cape or cloak hangs over the arms: they go under it, showing only
        // where they reach out past its edge. An arm carrying a shield in front keeps its place.
        if (heroClass.cloaked && MathUtils.sinDeg(FACING_ANGLE[column]) > 0f) {
            // An arm holding a sword or staff out to the side has come out from under the cloak.
            if (weapon == null || weapon.type == WeaponType.RANGED) rightBehind = true;
            if (onBow || !offHandShown()) leftBehind = true;
        }

        // Everything on the far side of the body, then the body, then everything on the near side.
        for (int pass = 0; pass < 2; pass++) {
            boolean behind = pass == 0;
            if (!behind) batch.draw(sprite, drawX, drawY);

            // On the far side the shield is beyond the arm that carries it; on the near side it covers that arm.
            if (offHand != null && behind) shieldSprite.drawBehind(batch, column, bodyX, bodyY);
            // The arm holding a bow is behind the bow: the bow is drawn over it.
            if (leftBehind == behind) drawLimb(batch, leftShoulderX, shoulderY, leftHand.x, leftHand.y);
            if (offHand != null && !behind) shieldSprite.drawInFront(batch, column, bodyX, bodyY);

            // Facing the camera a sword is held up in front of the arm: the arm goes behind it, and
            // only the hand is drawn over it, closed round the grip. Where the sword arm is the
            // near one and the hero is side-on or facing away, the arm and hand of a sword carried
            // at rest are in front of it instead. Raised for a charge or mid-swing the blade
            // crosses the arm, so there it is always the blade in front.
            boolean armOverSword = ARM_OVER_SWORD[column] && !chargeShown && !swinging;
            boolean swordArm = weapon != null && weapon.type == WeaponType.MELEE && rightBehind == behind && !armOverSword;
            if (swordArm && swordRaised > 0f) {
                drawRaisedArm(batch, rightShoulderX, shoulderY, rightHand.x, rightHand.y, swordRaised, rightShoulderX - (bodyX + FRAME_WIDTH / 2f));
            } else if (swordArm) {
                drawLimb(batch, rightShoulderX, shoulderY, rightHand.x, rightHand.y);
            }
            if (weaponSprite != null && swinging) {
                if (weaponBehind == behind) weaponSprite.drawTurned(batch, column, swingX, swingY, swingRotation);
            } else if (weaponSprite != null) {
                if (aura > 0f) {
                    batch.setColor(1f, 1f, 1f, aura);
                    drawHeld(batch, weaponAuraSprite, !behind, column, weaponX, weaponY, tilt, 0f);
                    batch.setColor(Color.WHITE);
                }
                drawHeld(batch, heldSprite, !behind, column, weaponX, weaponY, tilt, glow);
            }
            // The right hand goes over what it holds.
            if (swordArm) {
                drawHand(batch, rightHand.x, rightHand.y, MathUtils.atan2(rightHand.y - shoulderY, rightHand.x - rightShoulderX) * MathUtils.radiansToDegrees);
            } else if (rightBehind == behind) {
                // Facing the camera the elbow cannot be shown out behind the arrow (it would be up over
                // the face), so that arm is drawn like any other: bent at the chest.
                if (onString && !facingCamera) {
                    drawStringArm(batch, rightShoulderX, shoulderY, rightHand.x, rightHand.y, FACING_ANGLE[column]);
                } else if (weapon == null && chargeShown) {
                    drawCockedArm(batch, rightShoulderX, shoulderY, rightHand.x, rightHand.y, fistDrawnBack());
                } else {
                    drawLimb(batch, rightShoulderX, shoulderY, rightHand.x, rightHand.y);
                }
            }
            // A fully charged fist gives the same signal as a weapon: a flash, a ring, then a pulse.
            if (weapon == null && chargeShown && rightBehind == behind && chargeTime >= HEAVY_CHARGE_TIME) {
                float sinceFull = chargeTime - HEAVY_CHARGE_TIME;
                float shine = sinceFull < READY_FLASH_TIME
                        ? MathUtils.lerp(READY_FLASH, CHARGE_GLOW, sinceFull / READY_FLASH_TIME)
                        : CHARGE_GLOW * (1f - READY_PULSE * (0.5f - 0.5f * MathUtils.cos(MathUtils.PI2 * AURA_PULSE_RATE * (sinceFull - READY_FLASH_TIME))));
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                batch.setColor(1f, 1f, 1f, shine);
                drawHand(batch, rightHand.x, rightHand.y, MathUtils.atan2(rightHand.y - shoulderY, rightHand.x - rightShoulderX) * MathUtils.radiansToDegrees);
                batch.setColor(Color.WHITE);
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                if (sinceFull < FIST_READY_EFFECT.duration()) {
                    world.drawEffect(batch, FIST_READY_EFFECT, sinceFull, rightHand.x, rightHand.y, 0f);
                }
            }
            // The charge-up sits on the weapon, so the body hides it whenever it hides the weapon.
            if (chargeShown && weapon != null && weaponBehind == behind) drawCharge(batch, column, weaponX, weaponY, tilt);
        }

    }

    /**
     * Draws the charge-up on the weapon: streaks on a bow's arrow, an orb at a staff's head,
     * light running up a sword's blade and sparks coming off it.
     */
    private void drawCharge(SpriteBatch batch, int column, float weaponX, float weaponY, float tilt) {
        // the ring that pops off the weapon the moment the charge is full
        float sinceFull = chargeTime - HEAVY_CHARGE_TIME;
        boolean ringShown = sinceFull >= 0f && sinceFull < weapon.readyEffect.duration();
        if (weapon.type == WeaponType.RANGED) {
            // a bow: on the arrow, out in front
            float arrowX = x + MathUtils.cosDeg(aimAngle) * MUZZLE_DISTANCE;
            float arrowY = y + AIM_HEIGHT + MathUtils.sinDeg(aimAngle) * MUZZLE_DISTANCE;
            world.drawEffect(batch, weapon.chargeEffect, chargeTime, arrowX, arrowY, 0f);
            if (ringShown) world.drawEffect(batch, weapon.readyEffect, sinceFull, arrowX, arrowY, 0f);
        } else if (weapon.type == WeaponType.MAGIC) {
            // a staff: at its head, wherever that has tipped to
            Vector2 head = staffHeadAt(column, weaponX, weaponY, tilt);
            world.drawEffect(batch, weapon.chargeEffect, chargeTime, head.x, head.y, 0f);
            if (ringShown) world.drawEffect(batch, weapon.readyEffect, sinceFull, head.x, head.y, 0f);
        } else {
            if (ringShown) {
                // a sword: round the middle of the blade, wherever that has tipped to
                float gripX = weaponSprite.getGripX(column);
                float gripY = weaponSprite.getGripY(column);
                staffHead.set(weaponSprite.getTipX(column) - gripX, weaponSprite.getTipY(column) - gripY)
                        .scl(BLADE_MIDDLE).rotateDeg(tilt).add(weaponX + gripX, weaponY + gripY);
                world.drawEffect(batch, weapon.readyEffect, sinceFull, staffHead.x, staffHead.y, 0f);
            }
            // a sword: along the blade, from the hand to the point
            float bladeAngle = MathUtils.atan2(weaponSprite.getTipY(column) - weaponSprite.getGripY(column),
                    weaponSprite.getTipX(column) - weaponSprite.getGripX(column)) * MathUtils.radiansToDegrees;
            world.drawEffect(batch, weapon.chargeEffect, chargeTime, weaponX + weaponSprite.getGripX(column),
                    weaponY + weaponSprite.getGripY(column), bladeAngle + tilt);
        }
    }

    /**
     * Draws the half of a held weapon that goes in front of the body, or the half that goes behind
     * it. With `glow` above 0 it is drawn a second time, added on top, so it shines.
     */
    private void drawHeld(SpriteBatch batch, HeldSprite held, boolean inFront, int column, float heldX, float heldY, float tilt, float glow) {
        for (int pass = 0; pass < (glow > 0f ? 2 : 1); pass++) {
            if (pass == 1) {
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                batch.setColor(1f, 1f, 1f, glow);
            }
            if (inFront) {
                held.drawInFront(batch, column, heldX, heldY, tilt);
            } else {
                held.drawBehind(batch, column, heldX, heldY, tilt);
            }
        }
        if (glow > 0f) {
            batch.setColor(1f, 1f, 1f, 1f);
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    /**
     * Works out where the weapon is mid-swing (swingX, swingY, swingRotation): turned about the
     * hand, sweeping from one side of the aim to the other. A heavy attack with more than one
     * swing comes back the other way each time.
     */
    private void placeSwing(int column, float weaponX, float weaponY) {
        float sweepTime = SWING_TIME * swingCount;
        // Every cut comes down from the top: the blade starts on the upper side of the aim and
        // ends on the lower side, whichever way the hero is aiming. It never cuts upward.
        float endOffset = -swingArc * swingSide;
        float offset;       // degrees the blade is from the aim
        float reach = 1f;   // how far out the hand is, 0 = carrying the weapon at rest
        float raised = 1f;  // how far the weapon has turned from upright toward the swing, 0 = upright
        float from = 0f;    // the rotation it turns from: upright, or drawn back by a charge
        // The speed is uneven on purpose: a wind-up that eases in and settles, a sweep that covers
        // nearly all of its arc in a snap, a finish that overshoots and comes back, a gentle recovery.
        if (swingTime < swingWindup) {
            from = swingStartRotation;
            raised = reach = easeInOut(swingTime / swingWindup);
            offset = swingArc * swingSide;
        } else if (swingTime < swingWindup + sweepTime) {
            float progress = (swingTime - swingWindup) / SWING_TIME;
            int sweep = Math.min(swingCount - 1, (int) progress);
            float along = progress - sweep;
            // the last sweep carries on a little past where the blade will come to rest
            float past = sweep == swingCount - 1 ? SWING_OVERSHOOT : 0f;
            if (sweep > 0 && along < SWING_LIFT) {
                // between cuts the blade is lifted back to the top, ready to come down again
                offset = MathUtils.lerp(-swingArc, swingArc, easeInOut(along / SWING_LIFT));
            } else {
                if (sweep > 0) along = (along - SWING_LIFT) / (1f - SWING_LIFT);
                along = 1f - (float) Math.pow(1f - along, SWING_SNAP); // most of the arc at once, then easing off
                offset = MathUtils.lerp(swingArc, -swingArc - past, along);
            }
            offset *= swingSide;
        } else if (swingTime < swingWindup + sweepTime + swingHold) {
            float settled = (swingTime - swingWindup - sweepTime) / swingHold;
            offset = endOffset + Math.signum(endOffset) * SWING_OVERSHOOT * (1f - settled) * (1f - settled);
            reach = 1f + FINISH_REACH / SWING_REACH;
        } else {
            float recovered = Math.min(1f, (swingTime - swingWindup - sweepTime - swingHold) / swingRecover);
            raised = reach = 1f - easeInOut(recovered);
            offset = endOffset;
        }
        // The hand carries the grip across in front of the hero, following the blade round.
        float handAngle = aimAngle + offset * HAND_FOLLOW;
        // A charged swing brings the hand down from where the charge raised it, round an arc:
        // forward over the top first, then down, as the blade comes across.
        float descent = MathUtils.clamp(swingTime / (swingWindup + SWING_TIME * SWING_DESCENT), 0f, 1f);
        float stillBack = (float) Math.pow(1f - descent, SWORD_ARC);
        float stillUp = 1f - (float) Math.pow(descent, SWORD_ARC);
        swingX = weaponX + Math.round(MathUtils.cosDeg(handAngle) * SWING_REACH * reach + swingStartX * stillBack);
        swingY = weaponY + Math.round(MathUtils.sinDeg(handAngle) * SWING_REACH * reach + swingStartY * stillUp);
        swingRotation = MathUtils.lerpAngleDeg(from, aimAngle + offset - UPRIGHT, raised);
    }

    /** 0 to 1, slow at both ends and quick through the middle. */
    private static float easeInOut(float amount) {
        return amount * amount * (3f - 2f * amount);
    }

    /** How far a charging punch has been drawn back, 0 to 1, easing in and out. 0 when no charge is showing. */
    private float fistDrawnBack() {
        float back = MathUtils.clamp((chargeTime - CHARGE_SHOWN_AFTER) / FIST_BACK_TIME, 0f, 1f);
        return back * back * (3f - 2f * back);
    }

    /**
     * Puts the right hand where the punching fist is: being drawn back while a punch is charged,
     * or driving out toward the target from wherever it was and coming back to rest.
     */
    private void placeFist(boolean cocked) {
        float distance;
        float raise;
        if (cocked) {
            float back = fistDrawnBack();
            distance = MathUtils.lerp(FIST_REST, -FIST_BACK, back);
            // Aiming up or down the screen, back is toward or away from the camera: the fist stays low, by the ribs.
            raise = FIST_RAISE * back * Math.abs(MathUtils.cosDeg(aimAngle));
        } else {
            float progress = 1f - punchTimer / PUNCH_TIME;
            float swing = MathUtils.sin(MathUtils.PI * progress);
            float reach = punchHeavy ? HEAVY_FIST_REACH : LIGHT_FIST_REACH;
            // out from where the charge left it, back to where a fist rests
            boolean goingOut = progress < 0.5f;
            distance = MathUtils.lerp(goingOut ? punchFrom : FIST_REST, reach, swing);
            raise = MathUtils.lerp(goingOut ? punchRaise : 0f, FIST_EXTEND_RAISE, swing);
        }
        fistAt(distance, raise);
    }

    /** Puts the right hand `distance` out toward the target from the punching shoulder's side of the body, `raise` above fist height. */
    private void fistAt(float distance, float raise) {
        float sideX = Math.round(x - FRAME_WIDTH / 2f) + RIGHT_ARM_X[facingIndex.get(facing)];
        rightHand.set(sideX + MathUtils.cosDeg(aimAngle) * distance, y + FIST_HEIGHT + raise + MathUtils.sinDeg(aimAngle) * distance);
    }

    /**
     * Draws one arm from the shoulder to the hand, working out where the elbow goes. The elbow
     * hangs straight below the shoulder while the hand is close in, so the arm never folds in
     * toward the body, and moves onto the line to the hand as the arm straightens.
     */
    private void drawLimb(SpriteBatch batch, float shoulderX, float shoulderY, float handX, float handY) {
        float distance = (float) Math.hypot(handX - shoulderX, handY - shoulderY);
        float straight = MathUtils.clamp((distance - BENT_REACH) / (STRAIGHT_REACH - BENT_REACH), 0f, 1f);
        float elbowX = MathUtils.lerp(shoulderX, (shoulderX + handX) / 2f, straight);
        float elbowY = MathUtils.lerp(shoulderY - UPPER_ARM, (shoulderY + handY) / 2f, straight);
        drawArm(batch, shoulderX, shoulderY, elbowX, elbowY, handX, handY);
    }

    /**
     * Draws the arm that holds a bowstring: the forearm lies in line with the arrow
     * with the elbow out behind, and the upper arm runs from the shoulder to that elbow.
     */
    private void drawStringArm(SpriteBatch batch, float shoulderX, float shoulderY, float handX, float handY, float forward) {
        float elbowX = handX - MathUtils.cosDeg(forward) * FOREARM_LENGTH;
        float elbowY = handY - MathUtils.sinDeg(forward) * FOREARM_LENGTH;
        drawArm(batch, shoulderX, shoulderY, elbowX, elbowY, handX, handY);
    }

    /**
     * Draws the arm raising a sword for a heavy attack. With the hand up by the head the elbow
     * cannot hang below the shoulder: it rises with the sword to beside the shoulder, out from
     * the body, and the forearm reaches from there to the hand. `raised` is how far
     * the sword has been raised, 0 to 1; `outward` is which side of the body the shoulder is on.
     */
    private void drawRaisedArm(SpriteBatch batch, float shoulderX, float shoulderY, float handX, float handY, float raised, float outward) {
        // the elbow an arm normally has (see drawLimb)
        float distance = (float) Math.hypot(handX - shoulderX, handY - shoulderY);
        float straight = MathUtils.clamp((distance - BENT_REACH) / (STRAIGHT_REACH - BENT_REACH), 0f, 1f);
        float hangingX = MathUtils.lerp(shoulderX, (shoulderX + handX) / 2f, straight);
        float hangingY = MathUtils.lerp(shoulderY - UPPER_ARM, (shoulderY + handY) / 2f, straight);
        // The raised elbow is a fixed place beside the shoulder and a little above it: in front,
        // toward the target, or out from the body when aiming up or down the screen. It does not
        // depend on where the hand is, so it cannot jump from one side of the arm to the other
        // as the hero walks or turns.
        float sideways = MathUtils.cosDeg(aimAngle);
        float liftedX = shoulderX + ELBOW_FORWARD * sideways + Math.signum(outward) * ELBOW_OUT * (1f - Math.abs(sideways));
        float liftedY = shoulderY + ELBOW_RISE;
        float elbowX = MathUtils.lerp(hangingX, liftedX, raised);
        float elbowY = MathUtils.lerp(hangingY, liftedY, raised);
        // A forearm is only so long: where the hand is further from the elbow than that, the elbow follows it.
        float forearm = (float) Math.hypot(handX - elbowX, handY - elbowY);
        if (forearm > RAISED_FOREARM) {
            float keep = RAISED_FOREARM / forearm;
            elbowX = handX + (elbowX - handX) * keep;
            elbowY = handY + (elbowY - handY) * keep;
        }
        drawArm(batch, shoulderX, shoulderY, elbowX, elbowY, handX, handY);
    }

    /**
     * Draws the arm of a punch being charged. Up and down the screen the forearm
     * points toward or away from the camera, so it is drawn short, and the elbow never rises
     * above the shoulder.
     */
    private void drawCockedArm(SpriteBatch batch, float shoulderX, float shoulderY, float handX, float handY, float back) {
        float pulledX = handX - MathUtils.cosDeg(aimAngle) * COCKED_FOREARM;
        float pulledY = Math.min(shoulderY - 1f, handY - MathUtils.sinDeg(aimAngle) * COCKED_FOREARM * FORESHORTENED);
        // From the elbow an arm at rest has, hanging below the shoulder, to the one pulled back.
        // Aiming up or down the screen the pulled-back elbow is hidden behind the arm itself, so
        // it stays hanging and the fist sits under it, by the ribs.
        float pulled = back * Math.abs(MathUtils.cosDeg(aimAngle));
        float elbowX = MathUtils.lerp(shoulderX, pulledX, pulled);
        float elbowY = MathUtils.lerp(shoulderY - UPPER_ARM, pulledY, pulled);
        drawArm(batch, shoulderX, shoulderY, elbowX, elbowY, handX, handY);
    }

    /**
     * Draws an arm through its three joints as one shape: the whole outline first, then the sleeve
     * on top of it, so the upper arm and forearm meet in a rounded elbow with no line across it.
     */
    private void drawArm(SpriteBatch batch, float shoulderX, float shoulderY, float elbowX, float elbowY, float handX, float handY) {
        float outlined = SLEEVE_THICKNESS + 2f;
        batch.setColor(sleeveOutline);
        drawBar(batch, shoulderX, shoulderY, elbowX, elbowY, outlined, 0f);
        drawBar(batch, elbowX, elbowY, handX, handY, outlined, 0f);
        drawJoint(batch, elbowX, elbowY, outlined);
        batch.setColor(sleeveLit);
        drawBar(batch, shoulderX, shoulderY, elbowX, elbowY, SLEEVE_THICKNESS, 0f);
        drawBar(batch, elbowX, elbowY, handX, handY, SLEEVE_THICKNESS, 0f);
        drawJoint(batch, elbowX, elbowY, SLEEVE_THICKNESS);
        // the underside of each part is in shade
        batch.setColor(sleeveShade);
        drawBar(batch, shoulderX, shoulderY, elbowX, elbowY, SLEEVE_THICKNESS / 2f, SLEEVE_THICKNESS / 4f);
        drawBar(batch, elbowX, elbowY, handX, handY, SLEEVE_THICKNESS / 2f, SLEEVE_THICKNESS / 4f);
        batch.setColor(Color.WHITE);

        boolean forearmShown = Math.hypot(handX - elbowX, handY - elbowY) >= 1f;
        float fromX = forearmShown ? elbowX : shoulderX;
        float fromY = forearmShown ? elbowY : shoulderY;
        drawHand(batch, handX, handY, MathUtils.atan2(handY - fromY, handX - fromX) * MathUtils.radiansToDegrees);
    }

    /**
     * Draws a straight bar of the current batch color from one point to another, `thickness` wide.
     * `below` moves it that far toward the lower side of the line, whichever way the line runs.
     */
    private void drawBar(SpriteBatch batch, float fromX, float fromY, float toX, float toY, float thickness, float below) {
        float angle = MathUtils.atan2(toY - fromY, toX - fromX) * MathUtils.radiansToDegrees;
        float length = (float) Math.hypot(toX - fromX, toY - fromY);
        // Turned to `angle`, the bar's own "down" is down the screen only while it runs left to right.
        float shift = MathUtils.cosDeg(angle) >= 0f ? -below : below;
        float pivotY = thickness / 2f - shift;
        batch.draw(pixelTexture, fromX, fromY - pivotY, 0f, pivotY, length, thickness, 1f, 1f, angle, 0, 0, 1, 1, false, false);
    }

    /** Draws a square of the current batch color, `size` across, centered on a joint. */
    private void drawJoint(SpriteBatch batch, float jointX, float jointY, float size) {
        batch.draw(pixelTexture, jointX - size / 2f, jointY - size / 2f, size, size);
    }

    /** Draws the hand with its middle at (handX, handY), its knuckles pointing `angle` degrees. */
    private void drawHand(SpriteBatch batch, float handX, float handY, float angle) {
        // The sprite points right. Pointing left would turn it upside down, so it is flipped back upright.
        batch.draw(fistTexture, Math.round(handX - HAND_PIVOT_X), Math.round(handY - FIST_PIVOT_Y),
                HAND_PIVOT_X, FIST_PIVOT_Y, HAND_WIDTH, FIST_HEIGHT_PIXELS, 1f, 1f, angle,
                HAND_START, 0, HAND_WIDTH, FIST_HEIGHT_PIXELS, false, MathUtils.cosDeg(angle) < 0f);
    }

    public void dispose() {
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        customCursor.dispose();
        texture.dispose();
        walkTexture.dispose();
        fistTexture.dispose();
        pixelTexture.dispose();
        disposeWeaponSprites();
        if (offHandSprite != null) offHandSprite.dispose();
        if (offHandBlockSprite != null) offHandBlockSprite.dispose();
        if (offHandStowedSprite != null) offHandStowedSprite.dispose();
    }
}

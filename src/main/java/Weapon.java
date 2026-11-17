/**
 * Every weapon in the game. A weapon is not tied to a hero: any hero can equip any of them
 * (Hero.equip), and its sprite sheet lines up with every hero's frames.
 *
 * Each weapon has its own look for all of its attacks. A melee weapon's light and heavy
 * effects are its two swings, both aimed where the hero is facing. A ranged or magic weapon's are what it fires; a heavy shot can
 * also drag a trail behind it. The impact effect plays where an attack lands, and the charge
 * effect shows on the weapon while a heavy attack is being charged. The ready effect plays once,
 * on the weapon, the moment that charge is full.
 *
 * Adding a weapon is one line here plus its sprite sheets in resources/weapons.
 */
public enum Weapon {
    IRON_SWORD("Iron Sword", WeaponType.MELEE, 1, false, 10, "weapons/sword_iron.png",
            EffectType.SLASH_LIGHT, EffectType.SLASH_CROSS, null, EffectType.SPARK_STEEL, EffectType.EDGE_STEEL, EffectType.READY_EMBER, 2),
    HEX_BLADE("Hex Blade", WeaponType.MELEE, 2, false, 16, "weapons/sword_hex.png",
            EffectType.SLASH_HEX, EffectType.SLASH_HEX_HEAVY, null, EffectType.BURST_CYAN, EffectType.EDGE_CYAN, EffectType.READY_CYAN, 2),
    MACHINA_BLADE("Machina Blade", WeaponType.MELEE, 3, false, 24, "weapons/sword_machina.png",
            EffectType.SLASH_MACHINA, EffectType.SLASH_MACHINA_HEAVY, null, EffectType.BURST_RED, EffectType.EDGE_RED, EffectType.READY_RED, 3),
    OAK_STAFF("Oak Staff", WeaponType.MAGIC, 1, false, 10, "weapons/staff_oak.png",
            EffectType.BOLT_CYAN, EffectType.DISC_CYAN, null, EffectType.BURST_CYAN, EffectType.CHARGE, EffectType.READY_CYAN, 0),
    HEX_STAFF("Hex Staff", WeaponType.MAGIC, 2, false, 16, "weapons/staff_hex.png",
            EffectType.DART_MAGENTA, EffectType.CRYSTAL_MAGENTA, null, EffectType.BURST_MAGENTA, EffectType.CHARGE_MAGENTA, EffectType.READY_MAGENTA, 0),
    MACHINA_STAFF("Machina Staff", WeaponType.MAGIC, 3, false, 24, "weapons/staff_machina.png",
            EffectType.SLUG_RED, EffectType.CORE_RED, null, EffectType.BURST_RED, EffectType.CHARGE_RED, EffectType.READY_RED, 0),
    SHORT_BOW("Short Bow", WeaponType.RANGED, 1, true, 10, "weapons/bow_short.png",
            EffectType.ARROW, EffectType.ARROW_CHARGED, EffectType.TRAIL_EMBER, EffectType.SPARK_STEEL, EffectType.DRAW_EMBER, EffectType.READY_EMBER, 0),
    HEX_BOW("Hex Bow", WeaponType.RANGED, 2, true, 16, "weapons/bow_hex.png",
            EffectType.ARROW_HEX, EffectType.ARROW_HEX_CHARGED, EffectType.TRAIL_CYAN, EffectType.BURST_CYAN, EffectType.DRAW_CYAN, EffectType.READY_CYAN, 0),
    MACHINA_BOW("Machina Bow", WeaponType.RANGED, 3, true, 24, "weapons/bow_machina.png",
            EffectType.ARROW_MACHINA, EffectType.ARROW_MACHINA_CHARGED, EffectType.TRAIL_RED, EffectType.BURST_RED, EffectType.DRAW_RED, EffectType.READY_RED, 0);

    public final String displayName;
    public final WeaponType type;
    public final int tier; // 1 = what a hero starts with; 2 (hex) and 3 (machina) come from chests
    public final boolean twoHanded; // a two-handed weapon leaves no hand for a shield
    public final float damage; // a light attack, before the hero's class bonus
    public final String spriteFile; // held: 8 facings across, behind-the-body row then in-front row
    public final String groundFile; // lying on the floor after being dropped
    // A bow being drawn while a shot is charged, least pulled first. Empty for every other weapon.
    public final String[] chargeSpriteFiles;
    public final String[] chargeHandFiles; // marks where the hand on the string is at each of those stages
    public final String gripMarkerFile; // marks where the hand holding a bow is; null for every other weapon
    public final String auraFile; // the glow round a sword while it is charged; null for every other weapon
    public final EffectType lightEffect;
    public final EffectType heavyEffect;
    public final EffectType heavyTrail; // follows a heavy shot in flight; null = none
    public final EffectType impactEffect;
    public final EffectType chargeEffect;
    public final EffectType readyEffect; // pops off the weapon the moment the charge is full
    public final int heavySwings; // melee: how many times a heavy attack swings the blade across

    Weapon(String displayName, WeaponType type, int tier, boolean twoHanded, float damage, String spriteFile,
           EffectType lightEffect, EffectType heavyEffect, EffectType heavyTrail, EffectType impactEffect,
           EffectType chargeEffect, EffectType readyEffect, int heavySwings) {
        this.displayName = displayName;
        this.type = type;
        this.tier = tier;
        this.twoHanded = twoHanded;
        this.damage = damage;
        this.spriteFile = spriteFile;
        this.groundFile = spriteFile.replace(".png", "_ground.png");
        if (type == WeaponType.RANGED) {
            chargeSpriteFiles = new String[] {spriteFile.replace(".png", "_pull1.png"), spriteFile.replace(".png", "_pull2.png")};
            chargeHandFiles = new String[] {"weapons/bow_pull1_hand.png", "weapons/bow_pull2_hand.png"};
            gripMarkerFile = "weapons/bow_grip_hand.png";
        } else {
            chargeSpriteFiles = new String[0];
            chargeHandFiles = new String[0];
            gripMarkerFile = null;
        }
        auraFile = type == WeaponType.MELEE ? spriteFile.replace(".png", "_aura.png") : null;
        this.lightEffect = lightEffect;
        this.heavyEffect = heavyEffect;
        this.heavyTrail = heavyTrail;
        this.impactEffect = impactEffect;
        this.chargeEffect = chargeEffect;
        this.readyEffect = readyEffect;
        this.heavySwings = heavySwings;
    }
}

/**
 * The playable classes. Everything that differs between them lives here, so adding a class or
 * a per-class stat does not need a new Hero class.
 */
public enum HeroClass {
    KNIGHT("players/knight.png", "players/knight_walk.png", "players/knight_fist.png", WeaponType.MELEE, Weapon.IRON_SWORD, OffHand.KITE_SHIELD, false, true),
    MAGE("players/mage.png", "players/mage_walk.png", "players/mage_fist.png", WeaponType.MAGIC, Weapon.OAK_STAFF, null, true, false),
    RANGER("players/ranger.png", "players/ranger_walk.png", "players/ranger_fist.png", WeaponType.RANGED, Weapon.SHORT_BOW, null, true, true);

    public final String spriteFile; // standing: one row of 8 frames, one per facing
    public final String walkFile;   // walking: the same 8 facings across, the steps of the cycle down
    public final String fistFile;   // the forearm and fist, pointing right: punches, and the arm on a swing or a bowstring
    public final WeaponType favoredType; // the hero does extra damage with weapons of this type
    public final Weapon startingWeapon;
    public final OffHand startingOffHand; // null = nothing on the other arm
    public final boolean slowedByShield; // false = trained with a shield, no speed penalty
    public final boolean cloaked; // wears a cape or cloak down the back, which covers the arms seen from behind

    HeroClass(String spriteFile, String walkFile, String fistFile, WeaponType favoredType, Weapon startingWeapon,
              OffHand startingOffHand, boolean slowedByShield, boolean cloaked) {
        this.spriteFile = spriteFile;
        this.walkFile = walkFile;
        this.fistFile = fistFile;
        this.favoredType = favoredType;
        this.startingWeapon = startingWeapon;
        this.startingOffHand = startingOffHand;
        this.slowedByShield = slowedByShield;
        this.cloaked = cloaked;
    }
}

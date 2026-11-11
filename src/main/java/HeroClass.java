/**
 * The playable classes. Everything that differs between them lives here, so adding a class or
 * a per-class stat does not need a new Hero class.
 */
public enum HeroClass {
    KNIGHT("players/knight.png", "players/knight_walk.png"),
    MAGE("players/mage.png", "players/mage_walk.png"),
    RANGER("players/ranger.png", "players/ranger_walk.png");

    public final String spriteFile; // standing: one row of 8 frames, one per facing
    public final String walkFile;   // walking: the same 8 facings across, the steps of the cycle down

    HeroClass(String spriteFile, String walkFile) {
        this.spriteFile = spriteFile;
        this.walkFile = walkFile;
    }
}

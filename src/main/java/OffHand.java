/**
 * What a hero can carry on the other arm. Any hero can carry a shield,
 * but only the knight class can move at full speed while they do.
 */
public enum OffHand {
    KITE_SHIELD("Kite Shield", "weapons/shield_kite.png", "weapons/shield_kite_block.png", "weapons/shield_kite_back.png");

    public final String displayName;
    public final String spriteFile;
    public final String blockFile; // held out in front of the hero while blocking
    public final String stowedFile; // slung on the hero's back while both hands are on a weapon

    OffHand(String displayName, String spriteFile, String blockFile, String stowedFile) {
        this.displayName = displayName;
        this.spriteFile = spriteFile;
        this.blockFile = blockFile;
        this.stowedFile = stowedFile;
    }
}

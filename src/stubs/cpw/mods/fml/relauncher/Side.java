package cpw.mods.fml.relauncher;

public enum Side {
    CLIENT,
    SERVER;

    public boolean isClient() {
        return this == CLIENT;
    }
}

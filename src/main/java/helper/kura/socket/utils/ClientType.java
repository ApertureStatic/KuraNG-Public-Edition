package helper.kura.socket.utils;

/**
 * @author DiaoLing
 * @since 4/5/2024
 */
public enum ClientType {
    EMPTY("Empty"),
    Artist("Artist"),
    Kura("Kura"),
    Rebirth("Rebirth"),
    NEVER("Never");
    private final String name;

    ClientType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

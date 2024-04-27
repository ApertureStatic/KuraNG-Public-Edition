package helper.kura.socket.utils;


/**
 * @author DiaoLing
 * @since 4/5/2024
 */
public enum ChannelType {
    GLOBAL("Global"),
    PRIVATE("Private");

    private final String name;

    ChannelType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
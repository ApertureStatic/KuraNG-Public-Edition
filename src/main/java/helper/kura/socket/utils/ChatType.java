package helper.kura.socket.utils;

/**
 * @author DiaoLing
 * @since 4/8/2024
 */
public enum ChatType {
    NORMAL("Normal"),
    NOTICE("Notice"),
    SERVER("Server");

    private final String name;

    ChatType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

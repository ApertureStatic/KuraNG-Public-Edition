package helper.kura.socket.utils;

/**
 * @author DiaoLing
 * @since 4/5/2024
 */
public enum Rank {
    EMPTY("Empty"),
    USER("User"),
    CONTRIBUTOR("Contributor"),
    BETA("Beta"),
    TESTER("Tester"),
    MEDIA("Media"),
    PARTNER("Partner"),
    MODERATOR("Moderator"),
    DEV("Dev"),
    ADMIN("Admin"),
    FEMBOY("Femboy"),
    GAY("Gay"),
    OWNER("Owner");

    private final String name;

    Rank(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

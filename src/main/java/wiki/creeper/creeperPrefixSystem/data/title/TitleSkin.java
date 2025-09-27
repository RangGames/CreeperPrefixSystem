package wiki.creeper.creeperPrefixSystem.data.title;

import java.util.Objects;

/**
 * Cosmetic representation for titles including chat prefixes and name tag overrides.
 */
public final class TitleSkin {
    private final String chatPrefix;
    private final String nameTag;

    public TitleSkin(String chatPrefix, String nameTag) {
        this.chatPrefix = chatPrefix;
        this.nameTag = nameTag;
    }

    public String getChatPrefix() {
        return chatPrefix;
    }

    public String getNameTag() {
        return nameTag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TitleSkin titleSkin = (TitleSkin) o;
        return Objects.equals(chatPrefix, titleSkin.chatPrefix) && Objects.equals(nameTag, titleSkin.nameTag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatPrefix, nameTag);
    }
}

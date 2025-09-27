package wiki.creeper.creeperPrefixSystem.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Helper methods to convert formatted strings into Adventure components.
 */
public final class TextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private TextUtil() {
    }

    public static Component colorize(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        try {
            return MINI_MESSAGE.deserialize(input);
        } catch (Exception ex) {
            return LEGACY.deserialize(input);
        }
    }

    public static Component legacy(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return LEGACY.deserialize(input);
    }
}

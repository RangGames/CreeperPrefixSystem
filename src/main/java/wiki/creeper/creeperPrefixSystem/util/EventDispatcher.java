package wiki.creeper.creeperPrefixSystem.util;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Ensures that custom events are always fired synchronously regardless of the calling thread.
 */
public final class EventDispatcher {

    private EventDispatcher() {
    }

    public static <T extends Event> T dispatch(Plugin plugin, T event) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(event);
            return event;
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Bukkit.getPluginManager().callEvent(event);
                future.complete(event);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        try {
            return future.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Event dispatch interrupted", ex);
        } catch (ExecutionException ex) {
            throw new RuntimeException("Failed to dispatch event", ex.getCause());
        }
    }
}

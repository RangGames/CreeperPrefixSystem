package wiki.creeper.creeperPrefixSystem.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;
import wiki.creeper.creeperPrefixSystem.data.player.PlayerTitleState;
import wiki.creeper.creeperPrefixSystem.data.set.SetDefinition;
import wiki.creeper.creeperPrefixSystem.data.set.SetRegistry;
import wiki.creeper.creeperPrefixSystem.data.title.TitleDefinition;
import wiki.creeper.creeperPrefixSystem.data.title.TitleEffect;
import wiki.creeper.creeperPrefixSystem.data.title.TitleRegistry;
import wiki.creeper.creeperPrefixSystem.event.TitleEquipEvent;
import wiki.creeper.creeperPrefixSystem.event.TitleGrantEvent;
import wiki.creeper.creeperPrefixSystem.event.TitleRevokeEvent;
import wiki.creeper.creeperPrefixSystem.event.TitleSetActivateEvent;
import wiki.creeper.creeperPrefixSystem.event.TitleSetDeactivateEvent;
import wiki.creeper.creeperPrefixSystem.event.TitleUnequipEvent;
import wiki.creeper.creeperPrefixSystem.storage.MySqlStorage;
import wiki.creeper.creeperPrefixSystem.util.EventDispatcher;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for loading player title data, applying effects, and coordinating set bonuses.
 */
public final class TitleService {

    private final Plugin plugin;
    private final TitleRegistry titleRegistry;
    private final SetRegistry setRegistry;
    private final MySqlStorage storage;
    private final StatService statService;
    private final ExecutorService executor;

    private final Map<UUID, PlayerTitleState> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> activeSets = new ConcurrentHashMap<>();

    public TitleService(Plugin plugin,
                        TitleRegistry titleRegistry,
                        SetRegistry setRegistry,
                        MySqlStorage storage,
                        StatService statService,
                        ExecutorService executor) {
        this.plugin = plugin;
        this.titleRegistry = titleRegistry;
        this.setRegistry = setRegistry;
        this.storage = storage;
        this.statService = statService;
        this.executor = executor;
    }

    public CompletableFuture<PlayerTitleState> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerTitleState state = new PlayerTitleState();
            storage.loadPlayerTitles(uuid, state);
            cache.put(uuid, state);
            return state;
        }, executor).thenApply(state -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                state.getEquippedTitle().flatMap(id -> Optional.ofNullable(titleRegistry.get(id)))
                        .ifPresent(definition -> applyEffects(uuid, definition, false));
                recalculateSets(uuid, state);
            });
            return state;
        });
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
        activeSets.remove(uuid);
    }

    public PlayerTitleState getOrLoadSync(UUID uuid) {
        return cache.computeIfAbsent(uuid, key -> {
            PlayerTitleState state = new PlayerTitleState();
            storage.loadPlayerTitles(uuid, state);
            return state;
        });
    }

    public boolean grantTitle(UUID uuid, String titleId) {
        TitleDefinition definition = titleRegistry.get(titleId);
        if (definition == null) {
            return false;
        }
        PlayerTitleState state = getOrLoadSync(uuid);
        if (state.isOwned(titleId)) {
            return false;
        }
        TitleGrantEvent event = EventDispatcher.dispatch(plugin, new TitleGrantEvent(uuid, definition, !Bukkit.isPrimaryThread()));
        if (event.isCancelled()) {
            return false;
        }
        state.addOwnedTitle(titleId);
        executor.execute(() -> storage.upsertPlayerTitle(uuid, titleId, false));
        Bukkit.getScheduler().runTask(plugin, () -> recalculateSets(uuid, state));
        sendMessage(uuid, "§a새로운 칭호를 획득했습니다: §f" + definition.getDisplay());
        return true;
    }

    public boolean equipTitle(UUID uuid, String titleId) {
        TitleDefinition definition = titleRegistry.get(titleId);
        if (definition == null) {
            return false;
        }
        PlayerTitleState state = getOrLoadSync(uuid);
        if (!state.isOwned(titleId)) {
            return false;
        }
        Optional<String> previous = state.getEquippedTitle();
        TitleEquipEvent event = EventDispatcher.dispatch(plugin,
                new TitleEquipEvent(uuid, definition, previous.orElse(null), !Bukkit.isPrimaryThread()));
        if (event.isCancelled()) {
            return false;
        }

        previous.flatMap(id -> Optional.ofNullable(titleRegistry.get(id))).ifPresent(def -> clearEffects(uuid, def, "title:" + def.getId()));

        state.setEquippedTitle(titleId);
        executor.execute(() -> {
            storage.clearEquipped(uuid);
            storage.upsertPlayerTitle(uuid, titleId, true);
        });
        Bukkit.getScheduler().runTask(plugin, () -> applyEffects(uuid, definition, true));
        sendMessage(uuid, "§e칭호를 장착했습니다: §f" + definition.getDisplay());
        return true;
    }

    public boolean unequip(UUID uuid) {
        PlayerTitleState state = getOrLoadSync(uuid);
        Optional<String> previous = state.getEquippedTitle();
        if (previous.isEmpty()) {
            return false;
        }
        TitleDefinition current = titleRegistry.get(previous.get());
        if (current != null) {
            TitleUnequipEvent event = EventDispatcher.dispatch(plugin,
                    new TitleUnequipEvent(uuid, current, !Bukkit.isPrimaryThread()));
            if (event.isCancelled()) {
                return false;
            }
            clearEffects(uuid, current, "title:" + current.getId());
        }
        state.setEquippedTitle(null);
        executor.execute(() -> storage.clearEquipped(uuid));
        sendMessage(uuid, "§c칭호를 해제했습니다.");
        return true;
    }

    public boolean revokeTitle(UUID uuid, String titleId) {
        PlayerTitleState state = getOrLoadSync(uuid);
        if (!state.removeOwnedTitle(titleId)) {
            return false;
        }
        TitleDefinition definition = titleRegistry.get(titleId);
        if (definition != null) {
            TitleRevokeEvent event = EventDispatcher.dispatch(plugin,
                    new TitleRevokeEvent(uuid, definition, !Bukkit.isPrimaryThread()));
            if (event.isCancelled()) {
                return false;
            }
        }
        if (state.getEquippedTitle().filter(titleId::equals).isPresent()) {
            if (definition != null) {
                clearEffects(uuid, definition, "title:" + definition.getId());
            }
            state.setEquippedTitle(null);
            executor.execute(() -> storage.clearEquipped(uuid));
        }
        executor.execute(() -> storage.deletePlayerTitle(uuid, titleId));
        Bukkit.getScheduler().runTask(plugin, () -> recalculateSets(uuid, state));
        sendMessage(uuid, "§c칭호가 회수되었습니다: " + titleId);
        return true;
    }

    public Optional<String> getEquippedTitle(UUID uuid) {
        PlayerTitleState state = cache.get(uuid);
        return state == null ? Optional.empty() : state.getEquippedTitle();
    }

    public Set<String> getOwnedTitles(UUID uuid) {
        PlayerTitleState state = cache.get(uuid);
        return state == null ? Collections.emptySet() : state.getOwnedTitles();
    }

    private void applyEffects(UUID uuid, TitleDefinition definition, boolean removeExisting) {
        String source = "title:" + definition.getId();
        if (removeExisting) {
            clearEffects(uuid, definition, source);
        }
        for (TitleEffect effect : definition.getEffects()) {
            switch (effect.getType()) {
                case STAT_MOD -> statService.addModifier(uuid, effect.getStatId(), effect.getValue(), source, effect.getStatOperation(), null);
                case POTION -> applyPotion(uuid, effect.getPotionEffectType(), effect.getPotionLevel());
                case ATTRIBUTE -> {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && effect.getAttribute() != null) {
                        player.getAttribute(effect.getAttribute()).setBaseValue(effect.getValue());
                    }
                }
                case COMMAND -> {
                    String cmd = effect.getCommand();
                    if (cmd != null && !cmd.isBlank()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", Objects.requireNonNullElse(Bukkit.getOfflinePlayer(uuid).getName(), uuid.toString())));
                    }
                }
            }
        }
        recalculateSets(uuid, getOrLoadSync(uuid));
    }

    private void applyPotion(UUID uuid, PotionEffectType type, int level) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || type == null) {
            return;
        }
        PotionEffect effect = new PotionEffect(type, Integer.MAX_VALUE, Math.max(0, level - 1), true, false, true);
        player.addPotionEffect(effect);
    }

    private void clearEffects(UUID uuid, TitleDefinition definition, String source) {
        for (TitleEffect effect : definition.getEffects()) {
            switch (effect.getType()) {
                case STAT_MOD -> statService.removeModifier(uuid, effect.getStatId(), source);
                case POTION -> {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && effect.getPotionEffectType() != null) {
                        player.removePotionEffect(effect.getPotionEffectType());
                    }
                }
                case ATTRIBUTE -> {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && effect.getAttribute() != null) {
                        // Reset to default base value (Paper exposes default attribute base on spawn).
                        player.getAttribute(effect.getAttribute()).setBaseValue(player.getAttribute(effect.getAttribute()).getDefaultValue());
                    }
                }
                default -> {
                }
            }
        }
    }

    private void recalculateSets(UUID uuid, PlayerTitleState state) {
        Set<String> newlyActive = new HashSet<>();
        for (SetDefinition definition : setRegistry.all()) {
            if (state.getOwnedTitles().containsAll(definition.getMembers())) {
                newlyActive.add(definition.getId());
            }
        }
        Set<String> previous = activeSets.computeIfAbsent(uuid, key -> new HashSet<>());
        Set<String> removed = new HashSet<>(previous);
        removed.removeAll(newlyActive);
        Set<String> added = new HashSet<>(newlyActive);
        added.removeAll(previous);

        for (String setId : removed) {
            SetDefinition definition = setRegistry.get(setId);
            if (definition == null) {
                continue;
            }
            clearSetEffects(uuid, definition);
            EventDispatcher.dispatch(plugin, new TitleSetDeactivateEvent(uuid, definition, !Bukkit.isPrimaryThread()));
            previous.remove(setId);
        }

        for (String setId : added) {
            SetDefinition definition = setRegistry.get(setId);
            if (definition == null) {
                continue;
            }
            TitleSetActivateEvent event = EventDispatcher.dispatch(plugin,
                    new TitleSetActivateEvent(uuid, definition, !Bukkit.isPrimaryThread()));
            if (event.isCancelled()) {
                continue;
            }
            applySetEffects(uuid, definition);
            previous.add(setId);
        }
    }

    private void applySetEffects(UUID uuid, SetDefinition definition) {
        String source = "set:" + definition.getId();
        for (TitleEffect effect : definition.getEffects()) {
            if (effect.getType() == TitleEffect.Type.STAT_MOD) {
                statService.addModifier(uuid, effect.getStatId(), effect.getValue(), source, effect.getStatOperation(), null);
            } else if (effect.getType() == TitleEffect.Type.POTION) {
                applyPotion(uuid, effect.getPotionEffectType(), effect.getPotionLevel());
            }
        }
    }

    private void clearSetEffects(UUID uuid, SetDefinition definition) {
        String source = "set:" + definition.getId();
        for (TitleEffect effect : definition.getEffects()) {
            if (effect.getType() == TitleEffect.Type.STAT_MOD) {
                statService.removeModifier(uuid, effect.getStatId(), source);
            } else if (effect.getType() == TitleEffect.Type.POTION) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && effect.getPotionEffectType() != null) {
                    player.removePotionEffect(effect.getPotionEffectType());
                }
            }
        }
    }

    private void sendMessage(UUID uuid, String message) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && message != null) {
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(message));
        }
    }
}

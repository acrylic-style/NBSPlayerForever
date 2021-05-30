package xyz.acrylicstyle.nbsPlayerForever;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import util.promise.rewrite.Promise;
import xyz.acrylicstyle.tomeito_api.nbs.BukkitNBSFile;
import xyz.acrylicstyle.tomeito_api.nbs.BukkitNBSTick;
import xyz.acrylicstyle.tomeito_api.nbs.v4.BukkitNBS4Reader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class NBSPlayerForever extends JavaPlugin implements Listener {
    private final List<BukkitNBSFile> songs = new ArrayList<>();

    @Override
    public void onEnable() {
        File songsFolder = new File("./plugins/NBSPlayerForever/songs/");
        if (!songsFolder.exists() || !songsFolder.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            songsFolder.delete();
            if (!songsFolder.mkdirs()) {
                getLogger().warning("Could not create directory... for some reason");
            }
        }
        BukkitNBS4Reader reader = new BukkitNBS4Reader();
        for (File file : Objects.requireNonNull(songsFolder.listFiles())) {
            if (file.isFile()) {
                if (!file.canRead()) {
                    getLogger().warning("Cannot read " + file.getAbsolutePath() + " (check for permissions?)");
                } else {
                    try {
                        getLogger().info("Loading " + file.getAbsolutePath());
                        songs.add(reader.read(file));
                        getLogger().info("Loaded " + file.getAbsolutePath());
                    } catch (IOException e) {
                        getLogger().warning("Failed to load " + file.getAbsolutePath());
                        e.printStackTrace();
                    }
                }
            }
        }
        if (songs.isEmpty()) {
            getLogger().info("No songs loaded.");
        } else {
            getLogger().info(songs.size() + " songs loaded.");
        }
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (songs.size() > 0) {
            playSong(e.getPlayer(), (int) Math.round(Math.random() * (songs.size() - 1)));
        }
    }

    private void playSong(Player player, int index) {
        playSong(player, songs.get(index % songs.size())).thenDo(v -> playSong(player, index + 1)).onCatch(t -> {});
    }

    private @NotNull Promise<Void> playSong(Player player, BukkitNBSFile file) {
        return new Promise<>(context -> {
            AtomicBoolean cancelled = new AtomicBoolean(false);
            file.getBukkitTicks().forEach(tick -> new BukkitRunnable() {
                @Override
                public void run() {
                    if (cancelled.get()) return;
                    if (!player.isOnline()) {
                        context.reject(new Throwable());
                        cancelled.set(true);
                        return;
                    }
                    tick.getPlayableBukkitLayers().forEach(note -> note.play(player));
                }
            }.runTaskLater(this, tick.getTick()));
            BukkitNBSTick tick = (BukkitNBSTick) file.getLastTick();
            if (tick != null) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        context.resolve();
                    }
                }.runTaskLater(this, tick.getTick() + 10);
            } else {
                context.resolve();
            }
        });
    }
}

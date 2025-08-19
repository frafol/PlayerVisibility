package it.frafol.playervisibility;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import it.frafol.playervisibility.commands.HideCommand;
import it.frafol.playervisibility.enums.Config;
import it.frafol.playervisibility.enums.Data;
import it.frafol.playervisibility.listeners.JoinListener;
import it.frafol.playervisibility.listeners.QuitListener;
import it.frafol.playervisibility.listeners.WorldChangeListener;
import it.frafol.playervisibility.objects.TextFile;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.Library;
import net.byteflux.libby.relocation.Relocation;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class PlayerVisibility extends JavaPlugin {

    @Getter
    private TextFile configTextFile;

    @Getter
    private TextFile dataTextFile;

    @Getter
    public static PlayerVisibility instance;

    private final boolean isWindows = System.getProperty("os.name").startsWith("Windows");
    private boolean updated = false;

    public boolean file = false;

    @Setter
    @Getter
    public boolean hided = false;

    @Override
    public void onEnable() {

        instance = this;

        loadDependencies();
        loadConfiguration();
        loadCommands();
        loadListeners();
        loadDatabase();
        loadMetrics();
        checkForUpdatesTask();

        getLogger().info("Enabled successfully.");
    }

    @Override
    public void onDisable() {

        showPlayers();
        instance = null;

        getLogger().info("Disabled successfully.");
    }

    private void loadDependencies() {

        getLogger().info("Loading dependencies...");

        BukkitLibraryManager bukkitLibraryManager = new BukkitLibraryManager(this);
        final Relocation yamlrelocation = new Relocation("yaml", "it{}frafol{}libs{}yaml");
        Library yaml = Library.builder()
                .groupId("me{}carleslc{}Simple-YAML")
                .artifactId("Simple-Yaml")
                .version("1.8.4")
                .relocate(yamlrelocation)
                .build();
        bukkitLibraryManager.addJitPack();

        final Relocation schedulerrelocation = new Relocation("scheduler", "it{}frafol{}libs{}scheduler");
        Library scheduler = Library.builder()
                .groupId("com{}github{}Anon8281")
                .artifactId("UniversalScheduler")
                .version("0.1.6")
                .relocate(schedulerrelocation)
                .build();

        try {
            bukkitLibraryManager.loadLibrary(yaml);
        } catch (RuntimeException ignored) {
            getLogger().severe("Failed to load Simple-YAML library. Trying to download it from GitHub...");
            yaml = Library.builder()
                    .groupId("me{}carleslc{}Simple-YAML")
                    .artifactId("Simple-Yaml")
                    .version("1.8.4")
                    .url("https://github.com/Carleslc/Simple-YAML/releases/download/1.8.4/Simple-Yaml-1.8.4.jar")
                    .build();
        }

        bukkitLibraryManager.loadLibrary(yaml);
        bukkitLibraryManager.loadLibrary(scheduler);
    }

    private void loadConfiguration() {
        getLogger().info("Loading configuration...");
        configTextFile = new TextFile(getDataFolder().toPath(), "config.yml");

        if (Config.SAVE_FILE.get(Boolean.class)) {
            dataTextFile = new TextFile(getDataFolder().toPath(), "data.yml");
            file = true;
        }

    }

    private void loadCommands() {
        getLogger().info("Loading commands...");
        getServer().getPluginManager().registerEvents(new HideCommand(this), this);
    }

    private void loadListeners() {
        getLogger().info("Loading listeners...");
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new QuitListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldChangeListener(this), this);
    }

    private void loadMetrics() {
        if (!Config.STATS.get(Boolean.class)) {
            return;
        }

        getLogger().info("Loading metrics...");
        new Metrics(this, 19040);
    }

    private void loadDatabase() {

        if (!file) {
            return;
        }

        getLogger().info("Loading database...");
        if (!Data.HIDDEN.get(Boolean.class)) {
            showPlayers();
            return;
        }

        hidePlayers();
    }

    public void hidePlayers() {
        setHided(true);
        for (Player players : getServer().getOnlinePlayers()) {
            for (Player targetPlayers : getServer().getOnlinePlayers()) {
                if (targetPlayers != players) {

                    if (targetPlayers.hasPermission(Config.BYPASS_PERMISSION.get(String.class))) {
                        continue;
                    }

                    if (players.hasPermission(Config.BYPASS_PERMISSION.get(String.class))) {
                        continue;
                    }

                    try {
                        targetPlayers.hidePlayer(this, players);
                    } catch (Exception ignored) {
                        targetPlayers.hidePlayer(players);
                    }
                }
            }
        }

        if (!file) {
            return;
        }

        getDataTextFile().getConfig().set("hidden", true);
    }

    public void showPlayers() {
        setHided(false);
        for (Player players : getServer().getOnlinePlayers()) {
            for (Player targetPlayers : getServer().getOnlinePlayers()) {
                if (targetPlayers != players) {
                    try {
                        targetPlayers.showPlayer(this, players);
                    } catch (Exception ignored) {
                        targetPlayers.showPlayer(players);
                    }
                }
            }
        }

        if (!file) {
            return;
        }

        getDataTextFile().getConfig().set("hidden", false);
    }

    public void autoUpdate() {

        if (isWindows) {
            return;
        }

        String fileUrl = "https://github.com/frafol/PlayerVisibility/releases/download/release/PlayerVisibility.jar";
        String destination = "./plugins/";

        String fileName = getFileNameFromUrl(fileUrl);
        File outputFile = new File(destination, fileName);

        downloadFile(fileUrl, outputFile);
        updated = true;
        getLogger().warning("PlayerVisibility successfully updated, a restart is required.");
    }

    private String getFileNameFromUrl(String fileUrl) {
        int slashIndex = fileUrl.lastIndexOf('/');
        if (slashIndex != -1 && slashIndex < fileUrl.length() - 1) {
            return fileUrl.substring(slashIndex + 1);
        }
        throw new IllegalArgumentException("Invalid file URL");
    }

    @SneakyThrows
    private void downloadFile(String fileUrl, File outputFile) {
        URL url = new URL(fileUrl);
        try (InputStream inputStream = url.openStream()) {
            Files.copy(inputStream, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void checkForUpdatesTask() {
        TaskScheduler scheduler = UniversalScheduler.getScheduler(this);
        scheduler.runTaskTimerAsynchronously(this::checkUpdate, 0L, 20L * 600L);
    }

    private void checkUpdate() {

        if (updated) {
            return;
        }

        if (Config.UPDATE_CHECKER.get(Boolean.class)) {
            new UpdateCheck(this).getVersion(version -> {

                if (Integer.parseInt(getDescription().getVersion().replace(".", "")) < Integer.parseInt(version.replace(".", ""))) {

                    if (Config.AUTO_UPDATE.get(Boolean.class) && !updated) {
                        autoUpdate();
                        return;
                    }

                    if (!updated) {
                        getLogger().warning("§eThere is a new update available, download it on SpigotMC!");
                    }
                }

                if (Integer.parseInt(getDescription().getVersion().replace(".", "")) > Integer.parseInt(version.replace(".", ""))) {
                    getLogger().warning("§eYou are using a development version, please report any bugs!");
                }

            });
        }
    }
}

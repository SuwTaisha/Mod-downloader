import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Tracks which Modrinth mod/version is installed in a mods folder, so a future update check has something to compare against. */
final class InstallManifest {

    private static final String FILE_NAME = "modsoft-manifest.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    List<InstalledMod> mods = new ArrayList<>();

    static Path fileFor(Path dir) {
        return dir.resolve(FILE_NAME);
    }

    static InstallManifest load(Path dir) {
        Path file = fileFor(dir);
        if (!Files.isRegularFile(file)) {
            return new InstallManifest();
        }
        try {
            InstallManifest manifest = GSON.fromJson(Files.readString(file), InstallManifest.class);
            if (manifest == null) {
                return new InstallManifest();
            }
            if (manifest.mods == null) {
                manifest.mods = new ArrayList<>();
            }
            return manifest;
        } catch (Exception e) {
            return new InstallManifest();
        }
    }

    void save(Path dir) throws IOException {
        Files.writeString(fileFor(dir), GSON.toJson(this));
    }

    String installedAtOrNow(String projectId) {
        return mods.stream()
                .filter(m -> projectId.equals(m.projectId))
                .map(m -> m.installedAt)
                .findFirst()
                .orElseGet(() -> java.time.Instant.now().toString());
    }

    void put(InstalledMod mod) {
        mods.removeIf(m -> mod.projectId.equals(m.projectId));
        mods.add(mod);
    }
}

final class InstalledMod {
    String projectId;
    String slug;
    String title;
    String versionId;
    String versionNumber;
    List<String> gameVersions;
    String fileName;
    String sha1;
    String installedAt;
}

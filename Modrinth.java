import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

final class ModrinthClient {

    private static final String BASE = "https://api.modrinth.com/v2";
    private static final String USER_AGENT = "hoangmen0715-modsoft/1.0 (contact: hoangmen0715@gmail.com)";

    private final HttpClient http = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    /** Browses/searches Fabric mods on Modrinth with sorting, pagination, and an optional Minecraft version filter. */
    BrowseResult browseFabricMods(String query, String sortIndex, int offset, int limit, String gameVersion) throws IOException, InterruptedException {
        String url = BASE + "/search?query=" + URLEncoder.encode(query == null ? "" : query, StandardCharsets.UTF_8)
                + "&facets=" + URLEncoder.encode(buildFacets(gameVersion), StandardCharsets.UTF_8)
                + "&index=" + URLEncoder.encode(sortIndex, StandardCharsets.UTF_8)
                + "&offset=" + offset
                + "&limit=" + limit;
        HttpResponse<String> response = http.send(requestBuilder(url).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " while browsing mods");
        }
        BrowseResult result = gson.fromJson(response.body(), BrowseResult.class);
        return result != null ? result : new BrowseResult();
    }

    private static String buildFacets(String gameVersion) {
        StringBuilder sb = new StringBuilder("[[\"project_type:mod\"],[\"categories:fabric\"]");
        if (gameVersion != null && !gameVersion.isEmpty()) {
            sb.append(",[\"versions:").append(gameVersion).append("\"]");
        }
        return sb.append(']').toString();
    }

    /** Returns official (release) Minecraft versions, most recent first. */
    List<GameVersionTag> getReleaseGameVersions() throws IOException, InterruptedException {
        String url = BASE + "/tag/game_version";
        HttpResponse<String> response = http.send(requestBuilder(url).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return List.of();
        }
        GameVersionTag[] tags = gson.fromJson(response.body(), GameVersionTag[].class);
        if (tags == null) {
            return List.of();
        }
        return List.of(tags).stream()
                .filter(t -> "release".equals(t.versionType))
                .sorted((a, b) -> safe(b.date).compareTo(safe(a.date)))
                .toList();
    }

    /** Returns Fabric-loader versions of a project, most recently published first. */
    List<ModrinthVersion> getFabricVersions(String projectId) throws IOException, InterruptedException {
        String loaders = URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8);
        String url = BASE + "/project/" + URLEncoder.encode(projectId, StandardCharsets.UTF_8) + "/version?loaders=" + loaders;
        HttpResponse<String> response = http.send(requestBuilder(url).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return List.of();
        }
        ModrinthVersion[] versions = gson.fromJson(response.body(), ModrinthVersion[].class);
        if (versions == null) {
            return List.of();
        }
        return List.of(versions).stream()
                .sorted((a, b) -> safe(b.datePublished).compareTo(safe(a.datePublished)))
                .toList();
    }

    /** Fetches one exact version by its ID — used to honor a dependency's pinned version rather than re-resolving "latest". */
    Optional<ModrinthVersion> getVersion(String versionId) throws IOException, InterruptedException {
        String url = BASE + "/version/" + URLEncoder.encode(versionId, StandardCharsets.UTF_8);
        HttpResponse<String> response = http.send(requestBuilder(url).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return Optional.empty();
        }
        return Optional.ofNullable(gson.fromJson(response.body(), ModrinthVersion.class));
    }

    /** Looks up which Modrinth version a local file corresponds to, by its SHA-1 hash. Empty if unknown to Modrinth. */
    Optional<ModrinthVersion> getVersionByFileHash(String sha1Hex) throws IOException, InterruptedException {
        String url = BASE + "/version_file/" + sha1Hex + "?algorithm=sha1";
        HttpResponse<String> response = http.send(requestBuilder(url).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return Optional.empty();
        }
        return Optional.ofNullable(gson.fromJson(response.body(), ModrinthVersion.class));
    }

    /** Fetches a project's title/description/icon by its ID or slug. */
    Optional<ModrinthProjectSummary> getProjectSummary(String idOrSlug) throws IOException, InterruptedException {
        String url = BASE + "/project/" + URLEncoder.encode(idOrSlug, StandardCharsets.UTF_8);
        HttpResponse<String> response = http.send(requestBuilder(url).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return Optional.empty();
        }
        return Optional.ofNullable(gson.fromJson(response.body(), ModrinthProjectSummary.class));
    }

    void downloadFile(String fileUrl, Path target) throws IOException, InterruptedException {
        HttpResponse<Path> response = http.send(requestBuilder(fileUrl).build(), HttpResponse.BodyHandlers.ofFile(target));
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " while downloading " + fileUrl);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private HttpRequest.Builder requestBuilder(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .GET();
    }
}

final class BrowseResult {
    List<ModrinthSearchHit> hits = List.of();
    @SerializedName("total_hits")
    int totalHits;
    int offset;
    int limit;
}

final class ModrinthSearchHit {
    @SerializedName("project_id")
    String id;
    String slug;
    String title;
    String description;
    @SerializedName("icon_url")
    String iconUrl;
    long downloads;
    long follows;

    @Override
    public String toString() {
        return title != null ? title : slug;
    }
}

final class ModrinthVersion {
    String id;
    @SerializedName("project_id")
    String projectId;
    @SerializedName("version_number")
    String versionNumber;
    @SerializedName("game_versions")
    List<String> gameVersions;
    @SerializedName("date_published")
    String datePublished;
    List<ModrinthFile> files;
    List<ModrinthDependency> dependencies;
}

final class ModrinthFile {
    String url;
    String filename;
    boolean primary;
}

final class ModrinthDependency {
    @SerializedName("version_id")
    String versionId;
    @SerializedName("project_id")
    String projectId;
    @SerializedName("dependency_type")
    String dependencyType;
}

final class ModrinthProjectSummary {
    String id;
    String slug;
    String title;
    String description;
    @SerializedName("icon_url")
    String iconUrl;
}

final class GameVersionTag {
    String version;
    @SerializedName("version_type")
    String versionType;
    String date;
    boolean major;
}

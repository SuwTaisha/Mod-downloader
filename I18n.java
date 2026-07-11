import java.util.HashMap;
import java.util.Map;

public final class I18n {

    public enum Lang { VI, EN }

    private static Lang current = Lang.VI;

    private static final Map<String, String> VI = new HashMap<>();
    private static final Map<String, String> EN = new HashMap<>();

    static {
        VI.put("app.title", "Modrinth Mod Downloader");
        VI.put("app.subtitle", "Tự động tải mod Minecraft từ Modrinth");
        VI.put("section.path", "THƯ MỤC CÀI ĐẶT");
        VI.put("section.browse", "TÌM MOD FABRIC");
        VI.put("section.selected", "MOD SẼ CÀI ĐẶT");
        VI.put("section.installed", "MOD ĐÃ CÀI ĐẶT");
        VI.put("button.browse", "Chọn thư mục...");
        VI.put("button.add", "Thêm vào danh sách");
        VI.put("button.remove", "Xóa mod đã chọn");
        VI.put("button.install", "Cài đặt");
        VI.put("button.prev", "Trước");
        VI.put("button.next", "Sau");
        VI.put("tab.download", "Tải mod");
        VI.put("tab.update", "Cập nhật mod");
        VI.put("button.refresh", "Quét lại");
        VI.put("button.check_updates", "Kiểm tra cập nhật");
        VI.put("button.update_selected", "Cập nhật đã chọn");
        VI.put("button.delete_installed", "Xóa mod đã chọn");
        VI.put("update.up_to_date", "Đã mới nhất");
        VI.put("status.updates_found", "Tìm thấy %d mod có bản cập nhật.");
        VI.put("search.placeholder", "Tìm mod Fabric trên Modrinth...");
        VI.put("sort.relevance", "Liên quan nhất");
        VI.put("sort.downloads", "Tải nhiều nhất");
        VI.put("sort.follows", "Theo dõi nhiều nhất");
        VI.put("sort.newest", "Mới nhất");
        VI.put("sort.updated", "Cập nhật gần đây");
        VI.put("version.all", "Tất cả phiên bản");
        VI.put("version.tooltip", "Lọc theo phiên bản Minecraft chính thức");
        VI.put("version.update_tooltip", "Đổi phiên bản Minecraft mục tiêu cho toàn bộ mod đã cài");
        VI.put("version.unresolved", "Chưa chọn phiên bản");
        VI.put("sort.tooltip", "Sắp xếp theo");
        VI.put("status.no_compatible_version_for_mod", "Mod này không có phiên bản Fabric phù hợp.");
        VI.put("dialog.pick_version_title", "Chọn phiên bản");
        VI.put("dialog.pick_version_body", "Chọn phiên bản của %s bạn muốn cài đặt:");
        VI.put("dialog.delete_mods_title", "Xóa mod đã cài");
        VI.put("dialog.delete_mods_body", "Các mod sau sẽ bị xóa khỏi thư mục. Hành động này không thể hoàn tác:");
        VI.put("dialog.delete_confirm", "Xóa");
        VI.put("status.deleted_mods", "Đã xóa %d mod, %d lỗi.");
        VI.put("update.no_version_for_target", "Không có bản cho phiên bản MC này");
        VI.put("pagination.page", "Trang %d/%d");
        VI.put("status.empty_list", "Chưa có mod nào trong danh sách.");
        VI.put("status.empty_path", "Vui lòng chọn thư mục cài đặt.");
        VI.put("status.loading", "Đang tải danh sách mod...");
        VI.put("status.no_results", "Không tìm thấy mod nào.");
        VI.put("status.network_error", "Lỗi kết nối tới Modrinth.");
        VI.put("status.cannot_create_dir", "Không thể tạo thư mục cài đặt.");
        VI.put("status.downloading", "Đang tải:");
        VI.put("status.install_done", "Hoàn tất: %d thành công, %d lỗi.");
        VI.put("status.checking", "Đang kiểm tra thư mục cài đặt...");
        VI.put("status.cancelled", "Đã hủy cài đặt.");
        VI.put("status.loaded_from_folder", "Đã liệt kê %d mod có sẵn trong thư mục.");
        VI.put("status.scanning_folder", "Đang quét thư mục tìm mod...");
        VI.put("status.folder_picker_error", "Không thể mở hộp thoại chọn thư mục.");
        VI.put("log.no_compatible_version", "Không tìm thấy phiên bản Fabric phù hợp");
        VI.put("dialog.errors_title", "Chi tiết lỗi");
        VI.put("dialog.errors_body", "Một số mod không tải được. Chi tiết đã được ghi vào file log tại: %s");
        VI.put("dialog.close", "Đóng");
        VI.put("status.choose_path_first", "Vui lòng chọn thư mục cài đặt trước khi thêm mod.");
        VI.put("path.placeholder", "Chưa chọn thư mục cài đặt...");
        VI.put("dialog.version_change_title", "Phát hiện thay đổi phiên bản");
        VI.put("dialog.version_change_body", "Các mod sau sẽ đổi phiên bản khi cài đặt (↑ nâng cấp, ↓ hạ cấp xuống bản cũ hơn):");
        VI.put("dialog.archive_checkbox", "Lưu trữ (archive) file cũ thay vì xóa hẳn");
        VI.put("dialog.dependencies_title", "Mod phụ thuộc");
        VI.put("dialog.dependencies_body", "Các mod bạn chọn cần những mod phụ thuộc sau. Chọn mod bạn muốn tải kèm:");
        VI.put("dialog.incompatible_title", "Mod không tương thích");
        VI.put("dialog.incompatible_body", "Các cặp mod sau được khai báo không tương thích với nhau — cài cùng lúc có thể khiến Minecraft không khởi động được. Chọn cách xử lý cho từng cặp:");
        VI.put("dialog.incompatible_drop", "Bỏ %s khỏi danh sách cài đặt");
        VI.put("dialog.incompatible_keep_both", "Vẫn cài cả hai (bỏ qua cảnh báo)");
        VI.put("dependency.required", "bắt buộc");
        VI.put("dependency.optional", "tùy chọn");
        VI.put("dialog.choose_folder_title", "Chọn thư mục cài đặt");
        VI.put("dialog.select_folder", "Chọn thư mục này");
        VI.put("dialog.cancel", "Hủy");
        VI.put("dialog.continue", "Tiếp tục");
        VI.put("folder.quick_access", "TRUY CẬP NHANH");
        VI.put("folder.this_pc", "MÁY NÀY");
        VI.put("folder.desktop", "Màn hình nền");
        VI.put("folder.downloads", "Tải xuống");
        VI.put("folder.documents", "Tài liệu");
        VI.put("folder.pictures", "Hình ảnh");
        VI.put("folder.music", "Âm nhạc");
        VI.put("folder.videos", "Video");
        VI.put("folder.onedrive", "OneDrive");
        VI.put("theme.light", "Sáng");
        VI.put("theme.dark", "Tối");
        VI.put("lang.vi", "Tiếng Việt");
        VI.put("lang.en", "English");

        EN.put("app.title", "Modrinth Mod Downloader");
        EN.put("app.subtitle", "Automatically download Minecraft mods from Modrinth");
        EN.put("section.path", "INSTALL DIRECTORY");
        EN.put("section.browse", "BROWSE FABRIC MODS");
        EN.put("section.selected", "MODS TO INSTALL");
        EN.put("section.installed", "INSTALLED MODS");
        EN.put("button.browse", "Browse...");
        EN.put("button.add", "Add to list");
        EN.put("button.remove", "Remove selected");
        EN.put("button.install", "Install");
        EN.put("button.prev", "Prev");
        EN.put("button.next", "Next");
        EN.put("tab.download", "Download");
        EN.put("tab.update", "Update mods");
        EN.put("button.refresh", "Rescan");
        EN.put("button.check_updates", "Check for updates");
        EN.put("button.update_selected", "Update selected");
        EN.put("button.delete_installed", "Delete selected");
        EN.put("update.up_to_date", "Up to date");
        EN.put("status.updates_found", "Found %d mod(s) with updates.");
        EN.put("search.placeholder", "Search Fabric mods on Modrinth...");
        EN.put("sort.relevance", "Most relevant");
        EN.put("sort.downloads", "Most downloads");
        EN.put("sort.follows", "Most follows");
        EN.put("sort.newest", "Newest");
        EN.put("sort.updated", "Recently updated");
        EN.put("version.all", "All versions");
        EN.put("version.tooltip", "Filter by official Minecraft version");
        EN.put("version.update_tooltip", "Change the target Minecraft version for all installed mods");
        EN.put("version.unresolved", "No version chosen");
        EN.put("sort.tooltip", "Sort by");
        EN.put("status.no_compatible_version_for_mod", "This mod has no compatible Fabric version.");
        EN.put("dialog.pick_version_title", "Choose a version");
        EN.put("dialog.pick_version_body", "Choose the version of %s you want to install:");
        EN.put("dialog.delete_mods_title", "Delete installed mods");
        EN.put("dialog.delete_mods_body", "The following mods will be removed from the folder. This cannot be undone:");
        EN.put("dialog.delete_confirm", "Delete");
        EN.put("status.deleted_mods", "Deleted %d mod(s), %d failed.");
        EN.put("update.no_version_for_target", "No version for this MC version");
        EN.put("pagination.page", "Page %d/%d");
        EN.put("status.empty_list", "The mod list is empty.");
        EN.put("status.empty_path", "Please choose an install directory.");
        EN.put("status.loading", "Loading mods...");
        EN.put("status.no_results", "No mods found.");
        EN.put("status.network_error", "Network error while contacting Modrinth.");
        EN.put("status.cannot_create_dir", "Could not create the install directory.");
        EN.put("status.downloading", "Downloading:");
        EN.put("status.install_done", "Done: %d succeeded, %d failed.");
        EN.put("status.checking", "Checking install directory...");
        EN.put("status.cancelled", "Installation cancelled.");
        EN.put("status.loaded_from_folder", "Listed %d mods already in the folder.");
        EN.put("status.scanning_folder", "Scanning folder for mods...");
        EN.put("status.folder_picker_error", "Could not open the folder picker.");
        EN.put("log.no_compatible_version", "No compatible Fabric version found");
        EN.put("dialog.errors_title", "Error details");
        EN.put("dialog.errors_body", "Some mods failed to download. Details were written to the log file at: %s");
        EN.put("dialog.close", "Close");
        EN.put("status.choose_path_first", "Please choose an install directory before adding mods.");
        EN.put("path.placeholder", "No install directory selected...");
        EN.put("dialog.version_change_title", "Version change detected");
        EN.put("dialog.version_change_body", "The following mods will change version when installed (↑ upgrade, ↓ downgrade to an older version):");
        EN.put("dialog.archive_checkbox", "Archive the old file instead of deleting it");
        EN.put("dialog.dependencies_title", "Dependencies");
        EN.put("dialog.dependencies_body", "The mods you selected require the following dependencies. Choose which ones to also download:");
        EN.put("dialog.incompatible_title", "Incompatible mods");
        EN.put("dialog.incompatible_body", "The following mod pairs are declared incompatible with each other — installing both may prevent Minecraft from launching. Choose how to resolve each pair:");
        EN.put("dialog.incompatible_drop", "Remove %s from the install list");
        EN.put("dialog.incompatible_keep_both", "Install both anyway (ignore warning)");
        EN.put("dependency.required", "required");
        EN.put("dependency.optional", "optional");
        EN.put("dialog.choose_folder_title", "Choose install folder");
        EN.put("dialog.select_folder", "Select this folder");
        EN.put("dialog.cancel", "Cancel");
        EN.put("dialog.continue", "Continue");
        EN.put("folder.quick_access", "QUICK ACCESS");
        EN.put("folder.this_pc", "THIS PC");
        EN.put("folder.desktop", "Desktop");
        EN.put("folder.downloads", "Downloads");
        EN.put("folder.documents", "Documents");
        EN.put("folder.pictures", "Pictures");
        EN.put("folder.music", "Music");
        EN.put("folder.videos", "Videos");
        EN.put("folder.onedrive", "OneDrive");
        EN.put("theme.light", "Light");
        EN.put("theme.dark", "Dark");
        EN.put("lang.vi", "Tiếng Việt");
        EN.put("lang.en", "English");
    }

    private I18n() {
    }

    public static Lang getLang() {
        return current;
    }

    public static void setLang(Lang lang) {
        current = lang;
    }

    public static String t(String key) {
        Map<String, String> table = current == Lang.VI ? VI : EN;
        return table.getOrDefault(key, key);
    }
}

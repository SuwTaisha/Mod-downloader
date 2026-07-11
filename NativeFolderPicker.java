import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Opens Windows' own folder picker (the modern common-item dialog) via a hidden PowerShell/.NET call,
 * instead of a hand-rolled Swing dialog — gives real Quick Access, drives, and %ENV% expansion for free.
 */
final class NativeFolderPicker {

    private NativeFolderPicker() {
    }

    static File pick(String initialDir) throws Exception {
        String script = buildScript(initialDir);
        ProcessBuilder builder = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                "-WindowStyle", "Hidden", "-Command", script);
        builder.redirectErrorStream(false);
        Process process = builder.start();

        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.readLine();
        }
        process.waitFor();

        if (output == null || output.isBlank()) {
            return null;
        }
        File folder = new File(output.trim());
        return folder.isDirectory() ? folder : null;
    }

    private static String buildScript(String initialDir) {
        String safeInitial = (initialDir == null ? "" : initialDir).replace("'", "''");
        return "Add-Type -AssemblyName System.Windows.Forms | Out-Null; "
                + "$dialog = New-Object System.Windows.Forms.OpenFileDialog; "
                + "$dialog.ValidateNames = $false; "
                + "$dialog.CheckFileExists = $false; "
                + "$dialog.CheckPathExists = $true; "
                + "$dialog.FileName = 'Select Folder'; "
                + "if (Test-Path -LiteralPath '" + safeInitial + "') { $dialog.InitialDirectory = '" + safeInitial + "' }; "
                + "if ($dialog.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) { "
                + "Write-Output ([System.IO.Path]::GetDirectoryName($dialog.FileName)) }";
    }
}

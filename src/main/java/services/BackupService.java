package services;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.Icon;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Servicio de copia de seguridad de la base de datos.
 *
 * Estrategia:
 * 1. Local  — Copia el .db a cualquier carpeta que elija el usuario.
 * 2. Nube   — Detecta las carpetas de sincronización de OneDrive y Google Drive
 * en la PC del cliente y copia allí. Al estar en esas carpetas,
 * el cliente de nube sincroniza automáticamente sin ninguna API key.
 * 3. Restore — Restaura un .db de backup sobreescribiendo el actual (cierra conexiones antes).
 *
 * No requiere ninguna dependencia adicional ni credenciales OAuth.
 */
public class BackupService {

    private static final String DB_FILE = System.getProperty("user.home") + File.separator + "Ivencore" + File.separator + "BaseNegocio.db";

    // ── Detección de carpetas de nube ─────────────────────────────────────────

    // 1. Cambiamos 'String icono' por 'Icon icono' para que acepte imágenes SVG
    public record CloudFolder(String nombre, Icon icono, Path ruta) {}

    /** Detecta las carpetas de sincronización de OneDrive y Google Drive disponibles en este PC. */
    public static List<CloudFolder> detectarCarpetasNube() {
        List<CloudFolder> lista = new ArrayList<>();
        String home  = System.getProperty("user.home");
        String user  = System.getProperty("user.name");
        String os    = System.getProperty("os.name").toLowerCase();

        // ── OneDrive (Windows) ────────────────────────────────────────────
        List<String> oneDrivePaths = new ArrayList<>();
        if (os.contains("win")) {
            oneDrivePaths.add(home + "\\OneDrive");
            oneDrivePaths.add(home + "\\OneDrive - ");
            String envOD = System.getenv("ONEDRIVE");
            if (envOD != null) oneDrivePaths.add(envOD);
            String envODB = System.getenv("ONEDRIVECOMMERCIAL");
            if (envODB != null) oneDrivePaths.add(envODB);
        } else if (os.contains("mac")) {
            oneDrivePaths.add(home + "/OneDrive");
            oneDrivePaths.add(home + "/OneDrive - ");
        }

        // 2. Asignamos el icono SVG de OneDrive
        Icon iconOneDrive = new FlatSVGIcon("icons/onedrive.svg", 0.6f);

        for (String p : oneDrivePaths) {
            try {
                Path parent = Paths.get(p).getParent();
                if (parent != null && Files.exists(parent)) {
                    Files.list(parent).forEach(dir -> {
                        if (dir.toString().startsWith(p) && Files.isDirectory(dir)) {
                            lista.add(new CloudFolder("OneDrive — " + dir.getFileName(), iconOneDrive, dir));
                        }
                    });
                } else if (Files.exists(Paths.get(p))) {
                    lista.add(new CloudFolder("OneDrive", iconOneDrive, Paths.get(p)));
                }
            } catch (Exception ignored) {}
        }

        // ── Google Drive (cualquier OS) ───────────────────────────────────
        List<String> gdrivePaths = new ArrayList<>();
        if (os.contains("win")) {
            gdrivePaths.add(home + "\\Google Drive");
            gdrivePaths.add(home + "\\GoogleDrive");
            gdrivePaths.add("G:\\Mi unidad");
            gdrivePaths.add("G:\\");
            for (char d = 'D'; d <= 'K'; d++) {
                String letter = d + ":\\";
                File f = new File(letter);
                if (f.exists()) {
                    File miUnidad = new File(letter + "Mi unidad");
                    if (miUnidad.exists()) gdrivePaths.add(miUnidad.getAbsolutePath());
                    else gdrivePaths.add(letter);
                }
            }
        } else if (os.contains("mac")) {
            gdrivePaths.add(home + "/Google Drive");
            gdrivePaths.add(home + "/Library/CloudStorage/GoogleDrive-" + user);
            try {
                Path cloudStorage = Paths.get(home + "/Library/CloudStorage");
                if (Files.exists(cloudStorage)) {
                    Files.list(cloudStorage).forEach(dir -> {
                        if (dir.getFileName().toString().startsWith("GoogleDrive")) {
                            lista.add(new CloudFolder("Google Drive", new FlatSVGIcon("icons/drive.svg", 1.5f), dir));
                        }
                    });
                }
            } catch (Exception ignored) {}
        } else {
            gdrivePaths.add(home + "/Google Drive");
            gdrivePaths.add(home + "/GoogleDrive");
        }

        // 3. Asignamos el icono SVG de Google Drive
        Icon iconGDrive = new FlatSVGIcon("icons/drive.svg", 0.6f);

        for (String p : gdrivePaths) {
            if (Files.exists(Paths.get(p)) && !lista.stream().anyMatch(c -> c.ruta().equals(Paths.get(p)))) {
                lista.add(new CloudFolder("Google Drive", iconGDrive, Paths.get(p)));
            }
        }

        // ── Dropbox ───────────────────────────────────────────────────────
        List<String> dropboxPaths = new ArrayList<>();
        if (os.contains("win")) {
            dropboxPaths.add(home + "\\Dropbox");
        } else {
            dropboxPaths.add(home + "/Dropbox");
        }
        try {
            String dbxConfig = home + (os.contains("win")
                    ? "\\AppData\\Roaming\\Dropbox\\info.json"
                    : "/.dropbox/info.json");
            if (Files.exists(Paths.get(dbxConfig))) {
                String json = Files.readString(Paths.get(dbxConfig));
                int idx = json.indexOf("\"path\"");
                if (idx >= 0) {
                    int start = json.indexOf("\"", idx + 7) + 1;
                    int end   = json.indexOf("\"", start);
                    if (start > 0 && end > start) dropboxPaths.add(json.substring(start, end));
                }
            }
        } catch (Exception ignored) {}

        // 4. Asignamos el icono SVG de Dropbox
        Icon iconDropbox = new FlatSVGIcon("icons/drop.svg", 0.6f);

        for (String p : dropboxPaths) {
            if (Files.exists(Paths.get(p))) {
                lista.add(new CloudFolder("Dropbox", iconDropbox, Paths.get(p)));
            }
        }

        return lista;
    }

    // ── Hacer backup ──────────────────────────────────────────────────────────

    public static Path hacerBackup(Path carpetaDestino) throws IOException {
        File dbFile = new File(DB_FILE);
        if (!dbFile.exists()) throw new FileNotFoundException("No se encontró la base de datos: " + DB_FILE);

        try (java.sql.Connection conn = dao.Conexion.getConnection();
             java.sql.Statement st = conn.createStatement()) {
            st.execute("PRAGMA wal_checkpoint(FULL)");
        } catch (Exception ignored) {}

        Path subcarpeta = carpetaDestino.resolve("ivencore_backups");
        Files.createDirectories(subcarpeta);

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        Path destino = subcarpeta.resolve("BaseNegocio_" + timestamp + ".db");

        Files.copy(dbFile.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
        return destino;
    }

    public static List<Path> listarBackups(Path carpeta) {
        List<Path> lista = new ArrayList<>();
        try {
            Path subcarpeta = carpeta.resolve("ivencore_backups");
            if (Files.exists(subcarpeta)) {
                Files.list(subcarpeta)
                        .filter(p -> p.toString().endsWith(".db"))
                        .sorted(Comparator.reverseOrder())
                        .forEach(lista::add);
            }
        } catch (Exception ignored) {}
        return lista;
    }

    // ── Restaurar backup ──────────────────────────────────────────────────────

    public static void restaurarBackup(Path archivoBackup) throws IOException {
        if (!Files.exists(archivoBackup))
            throw new FileNotFoundException("Archivo de backup no encontrado: " + archivoBackup);

        try {
            dao.Conexion.cerrarPool();
            Thread.sleep(400);
        } catch (Exception ignored) {}

        String dbPath = DB_FILE;
        for (String suffix : new String[]{"-wal", "-shm", ".db-wal", ".db-shm"}) {
            String finalPath = suffix.startsWith(".") ? dbPath + suffix : dbPath.replace(".db", "") + suffix;
            File walFile = new File(finalPath);
            try { if (walFile.exists()) walFile.delete(); } catch (Exception ignored) {}
        }

        new File(DB_FILE + "-wal").delete();
        new File(DB_FILE + "-shm").delete();

        File actual = new File(DB_FILE);
        if (actual.exists()) {
            try {
                Path emergencia = Paths.get(DB_FILE + ".pre_restore_" +
                        new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
                Files.copy(actual.toPath(), emergencia, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {}
        }

        Files.copy(archivoBackup, Paths.get(DB_FILE), StandardCopyOption.REPLACE_EXISTING);
    }

    public static String tamanioBaseDatos() {
        File f = new File(DB_FILE);
        if (!f.exists()) return "N/A";
        long bytes = f.length();
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024));
    }
}
package geq.kaboom.app.kaboot;

import android.content.Context;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class KabUtil {

    public static void toast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static String fetch(final String urlString) throws Exception {
        StringBuilder content = new StringBuilder();
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("HTTP request failed with response code: " + responseCode);
        }

        try (BufferedReader in =
                new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
        }

        connection.disconnect();
        return content.toString();
    }

    public static void deleteFile(String path) throws Exception {
        new ProcessBuilder("rm", "-rf", path).start().waitFor();
    }

    public static void makeDir(String path) {
        File file = new File(path);
        if (!isExistFile(file.getAbsolutePath())) {
            file.mkdirs();
        }
    }

    public static boolean isExistFile(String path) {
        return new File(path).exists();
    }

    private static void createNewFile(String path) throws IOException {
        int lastSep = path.lastIndexOf(File.separator);
        if (lastSep > 0) {
            String dirPath = path.substring(0, lastSep);
            makeDir(dirPath);
        }

        File file = new File(path);
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    public static void writeFile(String path, String str) throws IOException {
        createNewFile(path);
        try (FileWriter fileWriter = new FileWriter(new File(path), false)) {
            fileWriter.write(str);
            fileWriter.flush();
        }
    }

    public static String readFile(String path) throws IOException {
        createNewFile(path);
        StringBuilder sb = new StringBuilder();

        try (FileReader fr = new FileReader(new File(path))) {
            char[] buff = new char[1024];
            int length;
            while ((length = fr.read(buff)) > 0) {
                sb.append(new String(buff, 0, length));
            }
        }

        return sb.toString();
    }

    public static ArrayList<HashMap<String, String>> getProcesses() throws Exception {
        ArrayList<HashMap<String, String>> processList = new ArrayList<>();
        Process process = new ProcessBuilder("ps", "-eo", "pid,comm").start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        reader.readLine();
        while ((line = reader.readLine()) != null) {
            String[] parts = line.trim().split("\\s+", 2);
            if (parts.length == 2) {
                HashMap<String, String> processInfo = new HashMap<>();
                processInfo.put("pid", parts[0]);
                processInfo.put("name", parts[1]);
                processList.add(processInfo);
            }
        }

        process.waitFor();
        return processList;
    }

    public static void killProcess(int pid) throws Exception {
        new ProcessBuilder("kill", "-9", String.valueOf(pid)).start().waitFor();
    }

    public static int getPort() {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        } catch (Exception e) {
            port = -1;
        }
        return port;
    }

    public static void errorDialog(Context context, String content) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        TextView cont = new TextView(context);
        cont.setText(content);
        builder.setView(cont);
        builder.show();
    }

   public static long getFolderSize(File folder) {
    long totalSize = 0;

    if (folder != null && folder.exists()) {
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    totalSize += file.length();
                } else if (file.isDirectory()) {
                    totalSize += getFolderSize(file);
                }
            }
        }
    }

    return totalSize;
}
public static String formatSize(long size) {
    if (size <= 0) return "0.00B";
    final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
    int unitIndex = (int) (Math.log10(size) / Math.log10(1024));
    double sizeInUnit = size / Math.pow(1024, unitIndex);
    return String.format("%.2f%s", sizeInUnit, units[unitIndex]);
}
}

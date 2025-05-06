package geq.kaboom.app.kaboot;

import android.content.Context;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

public class KabUtil {

    public static void toast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static String fetch(String urlString) throws IOException {
        StringBuilder content = new StringBuilder();
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP request failed with code: " + responseCode);
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
        } finally {
            connection.disconnect();
        }

        return content.toString();
    }

    public static void deleteFile(String path) throws IOException, InterruptedException {
        new ProcessBuilder("rm", "-rf", path).start().waitFor();
    }

    public static void makeDir(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static boolean isExistFile(String path) {
        return new File(path).exists();
    }

    private static void createNewFile(String path) throws IOException {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        if (!file.exists()) {
            file.createNewFile();
        }
    }

    public static void writeFile(String path, String content) throws IOException {
        createNewFile(path);
        try (FileWriter writer = new FileWriter(path, false)) {
            writer.write(content);
        }
    }

    public static String readFile(String path) throws IOException {
        createNewFile(path);
        StringBuilder sb = new StringBuilder();

        try (FileReader fr = new FileReader(path);
             BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }

        return sb.toString().trim();
    }

    public static ArrayList<HashMap<String, String>> getProcesses() throws IOException, InterruptedException {
        ArrayList<HashMap<String, String>> processList = new ArrayList<>();
        Process process = new ProcessBuilder("ps", "-eo", "pid,comm").start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+", 2);
                if (parts.length == 2) {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("pid", parts[0]);
                    map.put("name", parts[1]);
                    processList.add(map);
                }
            }
        }

        process.waitFor();
        return processList;
    }

    public static void killProcess(int pid) throws IOException, InterruptedException {
        new ProcessBuilder("kill", "-9", String.valueOf(pid)).start().waitFor();
    }

    public static int getPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            return -1;
        }
    }

    public static void errorDialog(Context context, String content) {
        TextView messageView = new TextView(context);
        messageView.setText(content);
        new MaterialAlertDialogBuilder(context)
                .setTitle("Error")
                .setView(messageView)
                .setPositiveButton("OK", null)
                .show();
    }

    public static String getFolderSize(File file) {
        long size = calculateSize(file);
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int index = 0;
        double adjusted = size;

        while (adjusted >= 1024 && index < units.length - 1) {
            adjusted /= 1024;
            index++;
        }

        return String.format("%.1f%s", adjusted, units[index]);
    }

    private static long calculateSize(File file) {
        if (Files.isSymbolicLink(file.toPath())) return 0;
        if (file.isFile()) return file.length();

        long total = 0;
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                total += calculateSize(f);
            }
        }
        return total;
    }
}
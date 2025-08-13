package geq.kaboom.app.kaboot.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.system.Os;
import android.system.OsConstants;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

public class KabUtil {

  private Context context;

  public KabUtil(Context context) {
    this.context = context;
  }

  public void toast(String msg) {
    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
  }

  public String fetch(String urlString) {
    String result = null;
    try {
      StringBuilder content = new StringBuilder();
      HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
      connection.setRequestMethod("GET");
      connection.setRequestProperty("Accept", "application/json");

      int responseCode = connection.getResponseCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {
        throw new IOException("HTTP request failed with code: " + responseCode);
      }

      BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        content.append(inputLine);
      }
      in.close();
      connection.disconnect();
      result = content.toString();
    } catch (Exception e) {
      return null;
    }
    return result;
  }

  public boolean deleteFile(String path) {
    if (!isExistFile(path)) return true;
    try {
      return (new ProcessBuilder("rm", "-rf", path).start().waitFor() == 0);
    } catch (Exception e) {
      return false;
    }
  }

  public void makeDir(String path) {
    if (isExistFile(path)) return;
    new File(path).mkdirs();
  }

  public boolean isExistFile(String path) {
    return new File(path).exists();
  }

  public boolean renameFile(String path, String name) {
    if (!isExistFile(path)) return false;
    File file = new File(path);
    return file.renameTo(new File(file.getParent(), name));
  }

  private boolean createNewFile(String path) {
    try {
      File file = new File(path);
      File parent = file.getParentFile();
      if (parent != null && !parent.exists()) parent.mkdirs();
      if (!isExistFile(path)) file.createNewFile();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public void resetFolder(String path) {
    deleteFile(path);
    makeDir(path);
  }

  public void resetFile(String path) {
    deleteFile(path);
    createNewFile(path);
  }

  public String getLastPath(String path) {
    return Uri.parse(path).getLastPathSegment();
  }

  public boolean writeFile(String path, String content) {
    if (!createNewFile(path)) return false;
    try (FileWriter writer = new FileWriter(path, false)) {
      writer.write(content);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public String readFile(String path) {
    if (!isExistFile(path)) return null;
    StringBuilder sb = new StringBuilder();

    try (FileReader fr = new FileReader(path);
        BufferedReader br = new BufferedReader(fr)) {
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line).append('\n');
      }
    } catch (Exception e) {
      return null;
    }

    return sb.toString().trim();
  }

  public ArrayList<HashMap<String, String>> getProcesses()
      throws IOException, InterruptedException {
    ArrayList<HashMap<String, String>> processList = new ArrayList<>();
    java.lang.Process process = new ProcessBuilder("ps", "-eo", "pid,ppid,user,comm").start();

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      reader.readLine(); // skip header
      String line;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.trim().split("\\s+", 4);
        if (parts[1].equals(String.valueOf(Os.getpid()))) continue;
        if (parts.length == 4) {
          HashMap<String, String> map = new HashMap<>();
          map.put("pid", parts[0]);
          map.put("ppid", parts[1]);
          map.put("name", parts[3]);
          processList.add(map);
        }
      }
    }

    process.waitFor();
    return processList;
  }

  public boolean killProcess(int pid) {
    try {
      Os.kill(pid, OsConstants.SIGKILL);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public void showDialog(String title, String content) {
    new MaterialAlertDialogBuilder(context).setTitle(title).setMessage(content).show();
  }

  public String getFolderSize(File file) {
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

  public long calculateSize(File file) {
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

  public void copy(String content) {
    ClipboardManager clipboard = ContextCompat.getSystemService(context, ClipboardManager.class);
    if (clipboard == null) return;
    clipboard.setPrimaryClip(
        new ClipData(null, new String[] {"text/plain"}, new ClipData.Item(content)));
    toast("Copied to clipboard!");
  }

  public float getScale() {
    return context.getResources().getDisplayMetrics().density;
  }

  public void showPermissionDialog(String message, Intent intent) {
    new MaterialAlertDialogBuilder(context)
        .setTitle("Permission Required")
        .setMessage(message)
        .setPositiveButton(
            "Yes",
            (dialog, which) -> {
              context.startActivity(intent);
            })
        .setNegativeButton(
            "No",
            (dialog, which) -> {
              dialog.dismiss();
            })
        .show();
  }
}

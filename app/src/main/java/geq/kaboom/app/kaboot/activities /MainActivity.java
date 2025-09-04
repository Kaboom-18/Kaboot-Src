package geq.kaboom.app.kaboot.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;
import geq.kaboom.app.kaboot.utils.KabUtil;
import geq.kaboom.app.kaboot.utils.Config;
import geq.kaboom.app.kaboot.adapters.ListAdapter;
import geq.kaboom.app.kaboot.adapters.InstallAdapter;
import geq.kaboom.app.kaboot.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

  private File PATH;
  private final ArrayList<HashMap<String, Object>> data = new ArrayList<>();
  private final ArrayList<HashMap<String, Object>> indata = new ArrayList<>();
  private RecyclerView list;
  private ExtendedFloatingActionButton install;
  private SwipeRefreshLayout base;
  private MaterialToolbar toolbar;
  private ListAdapter adapter;
  private KabUtil util;
  private SharedPreferences config;
  private TextView warn;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    list = findViewById(R.id.list);
    warn = findViewById(R.id.warn);
    base = findViewById(R.id.base);
    install = findViewById(R.id.install);
    toolbar = findViewById(R.id.toolbar);
    
    config = getSharedPreferences("Configuration", MODE_PRIVATE);
    util = new KabUtil(this);
    PATH = new File(getFilesDir(), "Packages");
    adapter = new ListAdapter(data, list, warn);

    setSupportActionBar(toolbar);

    list.setLayoutManager(new LinearLayoutManager(this));
    list.setAdapter(adapter);

    install.setOnClickListener(v -> showInstallDialog());
    
    base.setOnRefreshListener(() -> refresh());
    
    if (config.getBoolean("permission", true)) {
      setupAllPermissions();
    }

    if (config.getBoolean("version", true)) {
      checkVersion();
    }
    
  }

  private void showInstallDialog() {
    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
    builder.setOnDismissListener(
        dialog -> {
          if (Config.refreshList) {
            Config.refreshList = false;
            refresh();
          }
        });

    RecyclerView installList = new RecyclerView(this);
    installList.setPadding(7, 14, 7, 14);
    installList.setLayoutManager(new LinearLayoutManager(this));
    builder.setTitle("Install a package");
    builder.setView(installList);
    AlertDialog dialog = builder.create();
    installList.setAdapter(new InstallAdapter(dialog, indata));
    dialog.show();
  }

  private void fetchPackages() {
    indata.clear();
    new Thread(
            () -> {
              String content;
              if ((content = util.fetch(config.getString("repo", Config.REPOURL))) != null) {
                try {
                  JSONArray resp = new JSONArray(content.trim());

                  for (int i = 0; i < resp.length(); i++) {
                    HashMap<String, Object> p = new HashMap<>();

                    JSONObject pkg = resp.getJSONObject(i);
                    JSONObject url = pkg.getJSONObject("url");

                    p.put("name", pkg.getString("name"));
                    p.put("desc", pkg.getString("description"));

                    for (String arc : Build.SUPPORTED_ABIS) {
                      if (url.has(arc)) {
                        p.put("url", url.getString(arc));
                        break;
                      }
                    }
                    indata.add(p);
                  }

                  Config.UI.post(() -> install.setVisibility(View.VISIBLE));
                } catch (Exception e) {
                  Config.UI.post(
                      () -> {
                        util.toast("Repository is badly formatted!");
                        install.setVisibility(View.GONE);
                      });
                }
              } else {
                Config.UI.post(
                    () -> {
                      util.toast("Couldn't connect to repository!");
                      install.setVisibility(View.GONE);
                    });
              }
            })
        .start();
  }

  private void refresh() {
    base.setRefreshing(true);
    new Thread(
            () -> {
              ArrayList<HashMap<String, Object>> tempData = new ArrayList<>();

              if (PATH.exists() && PATH.isDirectory()) {
                File[] files = PATH.listFiles();
                if (files != null) {
                  for (File file : files) {
                    if (file.isDirectory() && !file.getName().startsWith(".")) {
                      HashMap<String, Object> fileData = new HashMap<>();
                      fileData.put("path", file.getAbsolutePath());
                      if (config.getBoolean("size", false))
                        fileData.put("size", util.getFolderSize(file));
                      tempData.add(fileData);
                    }
                  }
                }
              } else {
                if (!PATH.mkdir()) {
                  Config.UI.post(
                      () -> {
                        util.toast("I/O error occurred!");
                        finish();
                      });
                  return;
                }
              }

              Config.UI.post(
                  () -> {
                    data.clear();
                    if (tempData.isEmpty()) {
                      list.setVisibility(View.GONE);
                      warn.setVisibility(View.VISIBLE);
                    } else {
                      list.setVisibility(View.VISIBLE);
                      warn.setVisibility(View.GONE);
                    }
                    data.addAll(tempData);
                    adapter.updateData(data);
                    base.setRefreshing(false);
                  });
            })
        .start();
  }

  private void showRunningProcessesDialog() {
    ArrayList<String> names = new ArrayList<>();
    ArrayList<HashMap<String, String>> processes;
    try {
      processes = util.getProcesses();
    } catch (Exception e) {
      util.toast("Couldn't fetch processes!");
      return;
    }
    processes.forEach((process) -> names.add(process.get("name")));
    if (!names.isEmpty()) {
      new MaterialAlertDialogBuilder(this)
          .setTitle("Kill a running process")
          .setItems(
              names.toArray(new String[0]),
              (dialog, which) -> {
                if (util.killProcess(Integer.parseInt(processes.get(which).get("pid")))) {
                  util.toast(
                      "Process ["
                          + processes.get(which).get("name")
                          + "] with pid ["
                          + processes.get(which).get("pid")
                          + "] killed successfully!");
                } else {
                  util.toast(
                      "Failed to kill ["
                          + processes.get(which).get("name")
                          + "] with pid ["
                          + processes.get(which).get("pid")
                          + "]");
                }
              })
          .show();
    } else {
      util.toast("No running processes found!");
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    refresh();
    fetchPackages();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    if (item.getItemId() == R.id.running_process) {
      showRunningProcessesDialog();
      return true;
    }
    if (item.getItemId() == R.id.settings) {
      startActivity(new Intent(this, SettingsActivity.class));
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void checkVersion() {
    AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setTitle("Checking Version")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create();

    dialog.show();

    new Thread(() -> {
        final String versionRaw = util.fetch(Config.VERSIONURL);
        final String downloadUrl = util.fetch(Config.DOWNLOADURL);
        Config.UI.post(() -> {
            if (versionRaw == null) {
                if (dialog.isShowing() && !isFinishing()) dialog.dismiss();
                util.toast("Failed to check version!");
                return;
            }

            try {
                String currentVersion =
                        getPackageManager().getPackageInfo(getPackageName(), 0).versionName;

                if (!currentVersion.equals(versionRaw.trim())
                        || !getPackageName().equals(Config.PACKAGE_NAME)) {

                    if (dialog.isShowing() && !isFinishing()) dialog.dismiss();
                    util.toast("Install the latest version!");
                    
                    if (downloadUrl != null) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                        startActivity(intent);
                        finishAffinity();
                    }
                } else {
                    if (dialog.isShowing() && !isFinishing()) dialog.dismiss();
                    util.toast("Version check successful!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (dialog.isShowing() && !isFinishing()) dialog.dismiss();
                util.toast("Version check failed!");
            }
        });
    }).start();
}

  public void setupAllPermissions() {
    // 1️⃣ Notification permission (Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
          != PackageManager.PERMISSION_GRANTED) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
            this, Manifest.permission.POST_NOTIFICATIONS)) {
          ActivityCompat.requestPermissions(
              this, new String[] {Manifest.permission.POST_NOTIFICATIONS}, 101);
        } else {
          Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
          intent.setData(Uri.parse("package:" + getPackageName()));
          util.showPermissionDialog(
              "This app needs notification permission to keep foreground service active. Give permission?",
              intent);
        }
      }
    }
  }
}

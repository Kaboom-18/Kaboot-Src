package geq.kaboom.app.kaboot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private File PATH;
    private final ArrayList<HashMap<String, Object>> data = new ArrayList<>();
    private final ArrayList<HashMap<String, Object>> indata = new ArrayList<>();
    private RecyclerView list;
    private FloatingActionButton install;
    private SwipeRefreshLayout base;
    private String arch;
    private MaterialToolbar toolbar;
    private ListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        list = findViewById(R.id.list);
        base = findViewById(R.id.base);
        install = findViewById(R.id.install);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        PATH = new File(getFilesDir(), "Packages");
        arch = System.getProperty("os.arch");

        adapter = new ListAdapter(data);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        install.setOnClickListener(v -> showInstallDialog());

        base.setOnRefreshListener(() -> refresh());
        
        refresh();
        fetchPackages();
    }

    private void showInstallDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setOnDismissListener(dialog -> refresh());

        RecyclerView installList = new RecyclerView(this);
        installList.setPadding(7, 14, 7, 14);
        installList.setLayoutManager(new LinearLayoutManager(this));
        builder.setTitle("Install a package..");
        builder.setView(installList);
        AlertDialog dialog = builder.create();
        installList.setAdapter(new InstallAdapter(dialog, indata));
        dialog.show();
    }

    private void fetchPackages() {
        indata.clear();
        new Thread(() -> {
            try {
                JSONArray resp = new JSONArray(KabUtil.fetch(getString(R.string.repo)));

                for (int i = 0; i < resp.length(); i++) {
                    JSONObject pkg = resp.getJSONObject(i);
                    JSONObject url = pkg.getJSONObject("url");

                    HashMap<String, Object> p = new HashMap<>();
                    p.put("name", pkg.getString("name"));
                    p.put("url", url.getString(arch));
                    indata.add(p);
                }

                runOnUiThread(() -> install.setVisibility(View.VISIBLE));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    KabUtil.toast(MainActivity.this, "Couldn't connect to repository");
                    install.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void refresh() {
        if(!base.isRefreshing())base.setRefreshing(true);
        new Thread(() -> {
            ArrayList<HashMap<String, Object>> tempData = new ArrayList<>();

            if (PATH.exists() && PATH.isDirectory()) {
                File[] files = PATH.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory() && !file.getName().startsWith(".")) {
                            HashMap<String, Object> fileData = new HashMap<>();
                            fileData.put("path", file.getAbsolutePath());
                            fileData.put("size", KabUtil.getFolderSize(file));
                            tempData.add(fileData);
                        }
                    }
                }
            } else {
                if (!PATH.mkdir()) {
                    runOnUiThread(() -> {
                        KabUtil.toast(MainActivity.this, "I/O error occurred!");
                        finish();
                    });
                    return;
                }
            }

            runOnUiThread(() -> {
                data.clear();
                data.addAll(tempData);
                adapter.updateData(data);
                base.setRefreshing(false);
            });
        }).start();
    }

    private void showRunningProcessesDialog() {
        new Thread(() -> {
            ArrayList<String> names = new ArrayList<>();
            ArrayList<HashMap<String, String>> processes;

            try {
                processes = KabUtil.getProcesses();
                for (HashMap<String, String> process : processes) {
                    names.add(process.get("name"));
                }

                runOnUiThread(() -> new MaterialAlertDialogBuilder(this)
                        .setTitle("Kill a running process")
                        .setItems(names.toArray(new String[0]), (dialog, which) -> {
                            try {
                                int pid = Integer.parseInt(processes.get(which).get("pid"));
                                String name = processes.get(which).get("name");

                                KabUtil.killProcess(pid);
                                KabUtil.toast(MainActivity.this,
                                        "Process [" + name + "] with pid [" + pid + "] killed successfully!");
                            } catch (Exception e) {
                                KabUtil.toast(MainActivity.this,
                                        "Failed to kill [" + processes.get(which).get("name")
                                                + "] with pid [" + processes.get(which).get("pid") + "]");
                            }
                        }).show());

            } catch (Exception e) {
                runOnUiThread(() -> KabUtil.toast(MainActivity.this, "Couldn't fetch processes"));
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
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
}
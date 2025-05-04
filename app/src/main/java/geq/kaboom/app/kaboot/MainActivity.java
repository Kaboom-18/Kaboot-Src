package geq.kaboom.app.kaboot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private File PATH;
    private ArrayList<HashMap<String, Object>> data;
    private ArrayList<HashMap<String, Object>> indata;
    private RecyclerView list;
    private FloatingActionButton install;
    private SwipeRefreshLayout base;
    private String arch;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        list = findViewById(R.id.list);
        base = findViewById(R.id.base);
        install = findViewById(R.id.install);
        toolbar = findViewById(R.id.toolbar);

        PATH = new File(getFilesDir(), "Packages");
        data = new ArrayList<>();
        indata = new ArrayList<>();
        arch = System.getProperty("os.arch");

        setSupportActionBar(toolbar);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new ListAdapter(data));
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this, new String[] {Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        install.setOnClickListener((v) -> showInstallDialog());

        base.setOnRefreshListener(
                () -> {
                    refresh();
                    base.setRefreshing(false);
                });

        refresh();
        fetchPackages();
    }

    private void showInstallDialog() {
        MaterialAlertDialogBuilder installd = new MaterialAlertDialogBuilder(this);
        installd.setOnDismissListener(dialog -> refresh());

        RecyclerView list = new RecyclerView(this);
        list.setPadding(8, 8, 8, 8);

        installd.setView(list);
        AlertDialog dialog = installd.create();
        dialog.setTitle("Install a package..");

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new InstallAdapter(dialog, indata));

        dialog.show();
    }

    private void fetchPackages() {
        indata.clear();
        new Thread(
                        () -> {
                            try {
                                JSONArray resp =
                                        new JSONArray(KabUtil.fetch(getString(R.string.repo)));

                                for (int i = 0; i < resp.length(); i++) {
                                    JSONObject pkg = resp.getJSONObject(i);
                                    HashMap<String, Object> p = new HashMap<>();
                                    JSONObject js = new JSONObject();
                                    JSONObject url = pkg.getJSONObject("url");
                                    p.put("name", pkg.getString("name"));
                                    p.put("url", url.getString(arch));
                                    indata.add(p);
                                }
                                runOnUiThread(() -> install.setVisibility(View.VISIBLE));
                            } catch (Exception e) {
                                runOnUiThread(
                                        () -> {
                                            KabUtil.toast(MainActivity.this, "Couldn't connect to repository");
                                            install.setVisibility(View.GONE);
                                        });
                            }
                        })
                .start();
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
        if (item.getItemId() == R.id.settings){
           Intent intent = new Intent();
            intent.setClass(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showRunningProcessesDialog() {
        ArrayList<String> names = new ArrayList<>();

        try {
            ArrayList<HashMap<String, String>> processes = KabUtil.getProcesses();
            for (HashMap<String, String> process : processes) {
                names.add(process.get("name"));
            }

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Kill a running process")
                    .setItems(
                            names.toArray(new String[0]),
                            (dialog, which) -> {
                                try {
                                    KabUtil.killProcess(
                                            Integer.parseInt(processes.get(which).get("pid")));
                                    KabUtil.toast(
                                            MainActivity.this,
                                            "Process ["
                                                    + processes.get(which).get("name")
                                                    + "] with pid ["
                                                    + processes.get(which).get("pid")
                                                    + "] killed successfully!");
                                } catch (Exception e) {
                                    KabUtil.toast(
                                            MainActivity.this,
                                            "Failed to kill ["
                                                    + processes.get(which).get("name")
                                                    + "] with pid ["
                                                    + processes.get(which).get("pid")
                                                    + "]");
                                }
                            })
                    .show();

        } catch (Exception e) {
            KabUtil.toast(MainActivity.this, "Couldn't fetch processes");
        }
    }

    private void refresh() {
        data.clear();

        if (PATH.exists() && PATH.isDirectory()) {
            File[] files = PATH.listFiles();

            if (files != null) {
                boolean dirFound = false;
                for (File file : files) {
                    if (file.isDirectory()) {
                        dirFound = true;
                        HashMap<String, Object> fileData = new HashMap<>();
                        fileData.put("path", file.getAbsolutePath());
                        fileData.put("size", KabUtil.formatSize(KabUtil.getFolderSize(file)));
                        data.add(fileData);
                    }
                }

                if (dirFound) {
                    list.getAdapter().notifyDataSetChanged();
                }
            }
        } else {
            if (!PATH.mkdir()) {
                KabUtil.toast(MainActivity.this, "I/O error occurred!");
                finish();
            }
        }
    }
}

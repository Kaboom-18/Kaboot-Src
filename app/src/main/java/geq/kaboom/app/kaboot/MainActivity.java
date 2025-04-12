package geq.kaboom.app.kaboot;

import android.Manifest;
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
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
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
    private MaterialToolbar toolbar;
    private RecyclerView list;
    private ExtendedFloatingActionButton install;
    private SwipeRefreshLayout base;
    private final android.app.Activity THIS = this;
    private String arch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        list = findViewById(R.id.list);
        base = findViewById(R.id.base);
        install = findViewById(R.id.install);

        PATH = new File(getFilesDir(), "Packages");
        data = new ArrayList<>();
        indata = new ArrayList<>();
        arch = System.getProperty("os.arch");

        list.setLayoutManager(new LinearLayoutManager(THIS));
        list.setAdapter(new ListAdapter(data));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this, new String[] {Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        install.setOnClickListener(v -> showInstallDialog());

        base.setOnRefreshListener(
                () -> {
                    refresh();
                    base.setRefreshing(false);
                });

        refresh();
        fetchPackages();
    }

    private void showInstallDialog() {
        MaterialAlertDialogBuilder installd = new MaterialAlertDialogBuilder(THIS);
        installd.setOnDismissListener(dialog -> refresh());

        RecyclerView list = new RecyclerView(THIS);
        list.setPadding(8, 8, 8, 8);

        installd.setView(list);
        AlertDialog dialog = installd.create();
        dialog.setTitle("Install a package..");

        list.setLayoutManager(new LinearLayoutManager(THIS));
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
                                            KabUtil.toast(THIS, "Couldn't connect to repository");
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
        return super.onOptionsItemSelected(item);
    }

    private void showRunningProcessesDialog() {
        ArrayList<String> names = new ArrayList<>();

        try {
            ArrayList<HashMap<String, String>> processes = KabUtil.getProcesses();
            for (HashMap<String, String> process : processes) {
                names.add(process.get("name"));
            }

            new MaterialAlertDialogBuilder(THIS)
                    .setTitle("Kill a running process")
                    .setItems(
                            names.toArray(new String[0]),
                            (dialog, which) -> {
                                try {
                                    KabUtil.killProcess(
                                            Integer.parseInt(processes.get(which).get("pid")));
                                    KabUtil.toast(
                                            THIS,
                                            "Process ["
                                                    + processes.get(which).get("name")
                                                    + "] with pid ["
                                                    + processes.get(which).get("pid")
                                                    + "] killed successfully!");
                                } catch (Exception e) {
                                    KabUtil.toast(
                                            THIS,
                                            "Failed to kill ["
                                                    + processes.get(which).get("name")
                                                    + "] with pid ["
                                                    + processes.get(which).get("pid")
                                                    + "]");
                                }
                            })
                    .show();

        } catch (Exception e) {
            KabUtil.toast(THIS, "Couldn't fetch processes");
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
                KabUtil.toast(THIS, "I/O error occurred!");
                finish();
            }
        }
    }
}

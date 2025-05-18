package geq.kaboom.app.kaboot;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {

    private RecyclerView list;
    private SharedPreferences config;
    private SharedPreferences.Editor configEditor;
    private KabUtil util;
    private MaterialToolbar toolbar;
    private LinearLayout.LayoutParams params;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        toolbar = findViewById(R.id.toolbar);
        list = findViewById(R.id.list);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        config = getSharedPreferences("Configuration", MODE_PRIVATE);
        params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        configEditor = config.edit();
        util = new KabUtil(this);

        list.setLayoutManager(new LinearLayoutManager(this));

        ArrayList<SettingItem> settings = new ArrayList<>();

        settings.add(new SettingItem(
                "Custom Packages Repository",
                "Define the URL of the custom repository from which the packages will be fetched.",
                (t, d) -> showTextInputDialog(
                        "Custom Package Repository",
                        "(Do not enter anything unless you know what you're doing!)",
                        "repo",
                        "Enter custom repo url",
                        true)));

        settings.add(new SettingItem(
                "Default Terminal Color",
                "Define the default background color of terminal.",
                (t, d) -> showTextInputDialog(
                        "Default color",
                        "Enter the default color of terminal",
                        "color",
                        "Enter default terminal color",
                        true)));

        settings.add(new SettingItem(
                "Terminal FontSize",
                "Define the default font size to be used in the terminal; it can be changed by resizing the screen.",
                (t, d) -> showNumberInputDialog(
                        "Font Size",
                        "fontSize",
                        "Enter font size")));

        settings.add(new SettingItem(
                config.getBoolean("size", false) ? "Hide package size" : "Show package size",
                "Specify whether the package should display its size. Showing size may increase refreshing time.",
                (t, d) -> {
                    boolean newState = !config.getBoolean("size", false);
                    configEditor.putBoolean("size", newState).apply();
                    setResult(Config.REFRESH_CODE);
                    util.toast(newState ? "Package Size Shown!" : "Package Size Hidden!");
                    t.setText(newState ? "Hide package size" : "Show package size");
                }));

        settings.add(new SettingItem(
                "Clear Tmp",
                "Clears application temporary files and package tmp directories.",
                (t, d) -> {
                    if (util.deleteFile(Config.getTmpDir(this))) {
                        setResult(Config.REFRESH_CODE);
                        util.toast("Cache cleared!");
                    } else {
                        util.toast("Failed to clear tmp!");
                    }
                }));

        settings.add(new SettingItem(
                "About us",
                "More about this application.",
                (t, d) -> {
                    new Thread(()->{
                   final String about = util.fetch(Config.ABOUTURL);
                   Config.UI.post(()->util.showDialog("About Us", about!=null?about.trim().replace("\\n", "\n"):"Couldn't fetch!"));
                   }).start();
                }));

        list.setAdapter(new SettingsAdapter(settings));
    }

    private void showTextInputDialog(String title, String message, String key, String hint, boolean triggerRefresh) {
        final EditText input = new EditText(this);
        input.setText(config.getString(key, ""));
        input.setHint(hint);
        input.setLayoutParams(params);
        LinearLayout container = new LinearLayout(this);
        container.setPadding(36, 8, 36, 8);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setView(container)
                .setPositiveButton("OK", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (!value.isEmpty()) {
                        configEditor.putString(key, value).apply();
                    }else{
                        configEditor.remove(key).apply();
                    }
                   if (triggerRefresh) setResult(Config.PKG_REFRESH_CODE);
                })
                .setNegativeButton("Reset", (dialog, which) -> {
                    configEditor.remove(key).apply();
                    if (triggerRefresh) setResult(Config.PKG_REFRESH_CODE);
                })
                .show();
    }

    private void showNumberInputDialog(String title, String key, String hint) {
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        int defaultValue = config.getInt(key, -1);
        input.setText(defaultValue != -1 ? String.valueOf(defaultValue) : "");
        input.setHint(hint);
        input.setLayoutParams(params);
        LinearLayout container = new LinearLayout(this);
        container.setPadding(36, 8, 36, 8);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(container)
                .setPositiveButton("OK", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (!value.isEmpty()) {
                        configEditor.putInt(key, Integer.parseInt(value)).apply();
                    }
                })
                .setNegativeButton("Reset", (dialog, which) -> configEditor.remove(key).apply())
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
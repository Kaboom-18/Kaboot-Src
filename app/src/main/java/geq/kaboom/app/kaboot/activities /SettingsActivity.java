package geq.kaboom.app.kaboot.activities;

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
import geq.kaboom.app.kaboot.utils.KabUtil;
import geq.kaboom.app.kaboot.utils.Config;
import geq.kaboom.app.kaboot.misc.SettingItem;
import geq.kaboom.app.kaboot.adapters.SettingsAdapter;
import geq.kaboom.app.kaboot.R;

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
    params =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
    configEditor = config.edit();
    util = new KabUtil(this);

    list.setLayoutManager(new LinearLayoutManager(this));

    ArrayList<SettingItem> settings = new ArrayList<>();

    settings.add(
        new SettingItem(
            "Custom Packages Repository",
            "Define the URL of the custom repository from which the packages will be fetched.",
            (t, d) ->
                showTextInputDialog(
                    "Custom Package Repository",
                    "(Do not enter anything unless you know what you're doing!)",
                    "repo",
                    "Enter custom repo url",
                    true)));

    settings.add(
        new SettingItem(
            "Default Terminal Color",
            "Define the default background color of terminal.",
            (t, d) ->
                showTextInputDialog(
                    "Default color",
                    "Enter the default color of terminal",
                    "color",
                    "Enter default terminal color",
                    true)));

    settings.add(
        new SettingItem(
            "Terminal FontSize",
            "Define the default font size to be used in the terminal; it can be changed by resizing the screen.",
            (t, d) -> showNumberInputDialog("Font Size", "fontSize", "Enter font size")));

    settings.add(
        new SettingItem(
            config.getBoolean("size", false) ? "Hide package size" : "Show package size",
            "Specify whether the package should display its size. Showing size may increase refreshing time.",
            (t, d) -> {
              boolean newState = !config.getBoolean("size", false);
              configEditor.putBoolean("size", newState).apply();
              util.toast(newState ? "Package Size Shown!" : "Package Size Hidden!");
              t.setText(newState ? "Hide package size" : "Show package size");
            }));

    settings.add(
        new SettingItem(
            config.getBoolean("permission", true)
                ? "Skip permission onStartUp"
                : "Ask permission onStartUp",
            "Specify whether the application should ask for runtime permissions on app startup.",
            (t, d) -> {
              boolean newState = !config.getBoolean("permission", true);
              configEditor.putBoolean("permission", newState).apply();
              util.toast(
                  newState ? "Ask's Permissions onStartUp!" : "Skips Permissions onStartUp!");
              t.setText(newState ? "Skip permission onStartUp" : "Ask permission onStartUp");
            }));

    settings.add(
        new SettingItem(
            config.getBoolean("version", true)
                ? "Skip versionCheck onStartUp"
                : "Perform versionCheck onStartUp",
            "Specify whether the application should search for latest version on app startup. (recommended!)",
            (t, d) -> {
              boolean newState = !config.getBoolean("version", true);
              configEditor.putBoolean("version", newState).apply();
              util.toast(
                  newState ? "Perform VersionCheck onStartUp!" : "Skips VersionCheck onStartUp!");
              t.setText(
                  newState ? "Skip versionCheck onStartUp" : "Perform versionCheck onStartUp");
            }));

    settings.add(
        new SettingItem(
            "Clear Tmp",
            "Clears application temporary files and package tmp directories.",
            (t, d) -> {
              if (util.deleteFile(Config.getTmpDir(this))) {
                util.toast("Cache cleared!");
              } else {
                util.toast("Failed to clear tmp!");
              }
            }));

    list.setAdapter(new SettingsAdapter(settings));
  }

  private void showTextInputDialog(
      String title, String message, String key, String hint, boolean triggerRefresh) {
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
        .setPositiveButton(
            "OK",
            (dialog, which) -> {
              String value = input.getText().toString().trim();
              if (!value.isEmpty()) {
                configEditor.putString(key, value).apply();
              } else {
                configEditor.remove(key).apply();
              }
            })
        .setNegativeButton(
            "Reset",
            (dialog, which) -> {
              configEditor.remove(key).apply();
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
        .setPositiveButton(
            "OK",
            (dialog, which) -> {
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

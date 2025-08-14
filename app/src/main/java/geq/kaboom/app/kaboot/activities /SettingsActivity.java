package geq.kaboom.app.kaboot.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;

import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.color.DynamicColors;
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
    configEditor = config.edit();
    util = new KabUtil(this);
    params =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

    list.setLayoutManager(new LinearLayoutManager(this));

    ArrayList<SettingItem> settings = new ArrayList<>();

    settings.add(
        new SettingItem(
            getThemeLabel(config.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)),
            "Switch between Light, Dark, and System default theme.",
            (t, d) -> {
              int newMode = cycleTheme();
              AppCompatDelegate.setDefaultNightMode(newMode);
              updateSettingText(t, newMode);
              util.toast("Theme switched to " + getThemeLabel(newMode)+"!");
            }));

    settings.add(
        new SettingItem(
            config.getBoolean("dynamic_colors", false)
                ? "Dynamic Colors: Enabled"
                : "Dynamic Colors: Disabled",
            "Toggle Dynamic Colors.",
            (t, d) -> {
              boolean enabled = !config.getBoolean("dynamic_colors", false);
              configEditor.putBoolean("dynamic_colors", enabled).apply();
              t.setText(enabled ? "Dynamic Colors: Enabled" : "Dynamic Colors: Disabled");
              util.toast(enabled ? "Dynamic Colors Enabled!" : "Dynamic Colors Disabled!");
              util.toast("Restart application for changes!");
            }));

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

    addToggleSetting(settings, "pkgSize", "Show Package Size", false);

    addToggleSetting(settings, "permission", "Ask Permissions On StartUp", true);

    addToggleSetting(settings, "version", "Check Latest Version On StartUp", true);

    settings.add(
        new SettingItem(
            "Clear Temp",
            "Clears application temporary files and package tmp directories.",
            (t, d) ->
                util.toast(
                    util.deleteFile(Config.getTmpDir(this))
                        ? "Cache cleared!"
                        : "Failed to clear tmp!")));

    settings.add(
        new SettingItem(
            "More About Us",
            "Visit our website.",
            (t, d) -> {
              new Thread(
                      () -> {
                        String url = util.fetch(Config.WEBSITE);
                        if (url == null) return;
                        Config.UI.post(
                            () -> {
                              Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                              startActivity(intent);
                            });
                      })
                  .start();
            }));

    list.setAdapter(new SettingsAdapter(settings));
  }

  private int cycleTheme() {
    int current = config.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    int next;
    switch (current) {
      case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
        next = AppCompatDelegate.MODE_NIGHT_NO;
        break;
      case AppCompatDelegate.MODE_NIGHT_NO:
        next = AppCompatDelegate.MODE_NIGHT_YES;
        break;
      case AppCompatDelegate.MODE_NIGHT_YES:
      default:
        next = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        break;
    }
    configEditor.putInt("theme", next).apply();
    return next;
  }

  private void updateSettingText(TextView textView, int mode) {
    textView.setText(getThemeLabel(mode));
  }

  private void toggleBooleanSetting(
      String key, TextView textView, String trueMsg, String falseMsg) {
    boolean newState = !config.getBoolean(key, false);
    configEditor.putBoolean(key, newState).apply();
    textView.setText(newState ? trueMsg : falseMsg);
    util.toast(newState ? trueMsg : falseMsg);
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
              if (!value.isEmpty()) configEditor.putString(key, value).apply();
              else configEditor.remove(key).apply();
            })
        .setNegativeButton("Reset", (dialog, which) -> configEditor.remove(key).apply())
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
              if (!value.isEmpty()) configEditor.putInt(key, Integer.parseInt(value)).apply();
            })
        .setNegativeButton("Reset", (dialog, which) -> configEditor.remove(key).apply())
        .show();
  }

  private String getThemeLabel(int mode) {
    switch (mode) {
      case AppCompatDelegate.MODE_NIGHT_NO:
        return "Light Theme";
      case AppCompatDelegate.MODE_NIGHT_YES:
        return "Dark Theme";
      case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
      default:
        return "System Default";
    }
  }

  private void addToggleSetting(
      ArrayList<SettingItem> settings, String key, String label, boolean defaultValue) {
    boolean enabled = config.getBoolean(key, defaultValue);
    settings.add(
        new SettingItem(
            label + ": " + (enabled ? "Enabled" : "Disabled"),
            "Toggle " + label + ".",
            (t, d) -> {
              boolean newState = !config.getBoolean(key, defaultValue);
              configEditor.putBoolean(key, newState).apply();
              t.setText(label + ": " + (newState ? "Enabled" : "Disabled"));
              util.toast(label + (newState ? " Enabled" : " Disabled"));
            }));
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

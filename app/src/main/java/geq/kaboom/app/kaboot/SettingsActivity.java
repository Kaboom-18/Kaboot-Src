package geq.kaboom.app.kaboot;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
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
        private boolean update = false;
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        toolbar = findViewById(R.id.toolbar);
        list = findViewById(R.id.list);
        setSupportActionBar(toolbar);
        
        ArrayList<SettingItem> settings = new ArrayList<>();
        config = getSharedPreferences("Configuration", MODE_PRIVATE);
        configEditor = config.edit();
        util = new KabUtil(this);
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        list.setLayoutManager(new LinearLayoutManager(this));
        
        settings.add(new SettingItem("Custom Packages Repository", "Define the URL of the custom repository from which the packages will be fetched.", (t,d) -> {
        final EditText input = new EditText(this);
        input.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ));
        input.setText(config.getString("repo", ""));
        input.setHint("Enter custom repo url");
        LinearLayout container = new LinearLayout(this);
        container.setPadding(36,8,36,8);
        container.addView(input);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Custom Package repository")
                .setMessage("(Do not enter anything unless you know what you're doing!)")
                .setView(container)
                .setPositiveButton("OK", (dialog, which) -> {
                    configEditor.putString("repo", input.getText().toString().trim()).apply();
                    setResult(Config.PKG_REFRESH_CODE);
                    })
                .setNegativeButton("Reset", (dialog, which) -> {
                    configEditor.remove("repo").apply();
                    setResult(Config.PKG_REFRESH_CODE);
                })
                .show();
        }));
        
        settings.add(new SettingItem(config.getBoolean("size", false)?"Hide package size":"Show package size", "Specify whether the package should display its size. Showing size may increase loading time.", (t,d) -> {
             boolean dat = config.getBoolean("size", false)?false:true;
             configEditor.putBoolean("size", dat).apply();
             setResult(Config.REFRESH_CODE);
             util.toast(dat?"Packaze Size Shown!":"Package Size Hidden!");
             t.setText(dat?"Hide package size":"Show package size");
        }));
        
        settings.add(new SettingItem("Terminal FontSize", "Define the default font size to be used in the terminal; it can be changed by resizing the screen.", (t,d)->{
        final EditText input = new EditText(this);
        input.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ));
        input.setText(String.valueOf(config.getInt("fontSize", -1)));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter font size");
        LinearLayout container = new LinearLayout(this);
        container.setPadding(36,8,36,8);
        container.addView(input);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Font Size")
                .setView(container)
                .setPositiveButton("OK", (dialog, which) -> configEditor.putInt("fontSize", Integer.parseInt(input.getText().toString().trim())).apply())
                .setNegativeButton("Reset", (dialog, which) -> configEditor.remove("fontSize").apply())
                .show();
        }));
        
        settings.add(new SettingItem("Clear Tmp", "Clears application temporary files and package tmp directories.", (t,d)->{
            if(util.deleteFile(Config.getTmpDir(this))){
            setResult(Config.REFRESH_CODE);
            util.toast("Cache cleared!");
            }else{
                util.toast("Failed to clear tmp!");
            }
        }));
        
        settings.add(new SettingItem("About us", "More about this application.", (t,d)-> util.showDialog("About Us", Config.ABOUTUS)));
        
        list.setAdapter(new SettingsAdapter(settings));
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

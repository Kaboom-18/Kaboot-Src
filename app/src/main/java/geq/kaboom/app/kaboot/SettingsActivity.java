package geq.kaboom.app.kaboot;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {
    
        private RecyclerView list;
        private SharedPreferences config;
        private SharedPreferences.Editor configEditor;
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        list = findViewById(R.id.list);
        
        ArrayList<SettingItem> settings = new ArrayList<>();
        config = getSharedPreferences("Configuration", MODE_PRIVATE);
        configEditor = config.edit();
        
        list.setLayoutManager(new LinearLayoutManager(this));
        
        settings.add(new SettingItem("Terminal FontSize", "Define the default font size to be used in the terminal; it can be changed by resizing the screen.", ()->{
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
                .setNegativeButton("Reset", (dialog, which) -> configEditor.putInt("fontSize", -1).apply())
                .show();
        }));
        list.setAdapter(new SettingsAdapter(settings));
    }
}

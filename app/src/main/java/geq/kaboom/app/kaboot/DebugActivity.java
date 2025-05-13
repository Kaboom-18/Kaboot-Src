package geq.kaboom.app.kaboot;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.HashMap;
import java.util.Map;

public class DebugActivity extends AppCompatActivity {

    private static final Map<String, String> exceptionMap = new HashMap<String, String>() {{
        put("StringIndexOutOfBoundsException", "Invalid string operation\n");
        put("IndexOutOfBoundsException", "Invalid list operation\n");
        put("ArithmeticException", "Invalid arithmetical operation\n");
        put("NumberFormatException", "Invalid toNumber block operation\n");
        put("ActivityNotFoundException", "Invalid intent operation\n");
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SpannableStringBuilder formattedMessage = new SpannableStringBuilder();
        Intent intent = getIntent();
        String errorMessage = "";

        if (intent != null) {
            errorMessage = intent.getStringExtra("error");
        }

        if (!errorMessage.isEmpty()) {
            String[] split = errorMessage.split("\n");

            String exceptionType = split[0];
            String message = exceptionMap.containsKey(exceptionType) ? exceptionMap.get(exceptionType) : "";

            if (!message.isEmpty()) {
                formattedMessage.append(message);
            }

            for (int i = 1; i < split.length; i++) {
                formattedMessage.append(split[i]);
                formattedMessage.append("\n");
            }
        } else {
            formattedMessage.append("No error message available.");
        }

        TextView errorView = new TextView(this);
        errorView.setText(formattedMessage);
        errorView.setTextIsSelectable(true);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("Application Crashed!");
        toolbar.setTitleTextColor(Color.RED);
        toolbar.setBackgroundColor(Color.parseColor("#222222"));
        getWindow().setStatusBarColor(Color.parseColor("#222222"));
        setSupportActionBar(toolbar);
        HorizontalScrollView hscroll = new HorizontalScrollView(this);
        ScrollView vscroll = new ScrollView(this);
        hscroll.addView(vscroll);
        vscroll.addView(errorView);
        layout.addView(toolbar);
        layout.addView(hscroll);

        setContentView(layout);
    }
}

package geq.kaboom.app.kaboot;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CrashActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().setStatusBarColor(Color.parseColor("#101010"));
    final String crashLog = getIntent().getStringExtra("log");
    new MaterialAlertDialogBuilder(this)
        .setTitle("Application Crashed!")
        .setMessage(crashLog)
        .setPositiveButton(
            "Send Report",
            (dialog, which) -> {
              Intent intent = new Intent(Intent.ACTION_SEND);
              intent.setType("message/rfc822");
              intent.putExtra(Intent.EXTRA_EMAIL, new String[] {Config.EMAIL});
              intent.putExtra(Intent.EXTRA_SUBJECT, "Kaboot Crash Report");
              intent.putExtra(Intent.EXTRA_TEXT, crashLog);
              try {
                startActivity(Intent.createChooser(intent, "Send email using..."));
              } catch (android.content.ActivityNotFoundException ex) {}
              finishAffinity();
            })
        .setNegativeButton("Close", (dialog, which) -> finishAffinity())
        .setCancelable(false)
        .show();
  }
}

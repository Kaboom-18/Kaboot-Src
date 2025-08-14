package geq.kaboom.app.kaboot;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Process;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.color.DynamicColors;
import geq.kaboom.app.kaboot.activities.CrashActivity;

public class KabootApplication extends Application{

private SharedPreferences config;

  @Override
  public void onCreate() {
    super.onCreate();
    
    config = getSharedPreferences("Configuration", MODE_PRIVATE);
    
    AppCompatDelegate.setDefaultNightMode(config.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
    
     if(config.getBoolean("dynamic_colors", false)){
        DynamicColors.applyToActivitiesIfAvailable(this);
      }
    
    Thread.setDefaultUncaughtExceptionHandler(
        (thread, log) -> {
          Intent intent = new Intent(this, CrashActivity.class);
          intent.putExtra("log", Log.getStackTraceString(log));
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
          startActivity(intent);
          Process.killProcess(Process.myPid());
          System.exit(1);
        });
  }
}

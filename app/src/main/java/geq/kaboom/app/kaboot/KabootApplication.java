package geq.kaboom.app.kaboot;

import android.app.Application;
import android.content.Intent;
import android.os.Process;
import android.util.Log;
import geq.kaboom.app.kaboot.activities.CrashActivity;

public class KabootApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
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

package geq.kaboom.app.kaboot;

import android.app.Application;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

public class KabootApplication extends Application {

    @Override
    public void onCreate() {
        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread thread, Throwable throwable) {
                        Intent intent = new Intent(getApplicationContext(), DebugActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra("error", Log.getStackTraceString(throwable));
                        startActivity(intent);
                    Process.killProcess(Process.myPid());
                        System.exit(1);
                    }
                });
        super.onCreate();
    }
}

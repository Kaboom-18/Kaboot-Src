package geq.kaboom.app.kaboot.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import geq.kaboom.app.kaboot.R;
import geq.kaboom.app.kaboot.terminal.Session;
import geq.kaboom.app.kaboot.terminal.TerminalActivity;
import geq.kaboom.app.kaboot.terminal.termlib.TerminalSession;
import geq.kaboom.app.kaboot.utils.Config;
import geq.kaboom.app.kaboot.utils.KabUtil;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.content.Context;
import java.util.ArrayList;

public class PkgService extends Service implements TerminalSession.SessionChangedCallback {

  private static final String CHANNEL_ID = "PkgService";
  private IBinder binder = new LocalBinder();
  private KabUtil util;
  private int sessionIndex = 0;
  private ArrayList<Session> sessions = new ArrayList<>();
  public static String active = null;
  public TerminalSession.SessionChangedCallback callback;

  @Override
  public void onCreate() {
    super.onCreate();
    util = new KabUtil(this);
  }

  public void addTerminalSession(Session session, String active) {
    sessions.add(session);
    this.active = active;
  }

  public ArrayList<Session> getTerminalSessions() {
    return sessions;
  }

  public int getSessionIndex() {
    return sessionIndex;
  }

  public void setSessionIndex(int a) {
    this.sessionIndex = a;
  }

  public void killSessions() {
    if (sessions == null) return;
    sessions.forEach(
        (session) -> {
          if (!killSessionProcess(session.getTerminalSession().getPid())) {
            util.toast("Session processes autoKill failed!");
          }
          session.getTerminalSession().finishIfRunning();
        });
  }

  public boolean killSessionProcess(int ppid) {
    final boolean res[] = {false};
    try {
      util.getProcesses()
          .forEach(
              (process) -> {
                if (process.get("ppid").equals(String.valueOf(ppid))) {
                  killSessionProcess(Integer.valueOf(process.get("pid")));
                  res[0] = util.killProcess(Integer.parseInt(process.get("pid")));
                }
              });
    } catch (Exception e) {
    }
    return res[0];
  }

  public void terminateService() {
    stopForeground(STOP_FOREGROUND_REMOVE);
    active = null;
    stopSelf();
    util.toast("Terminated!");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if(intent != null && "ACTION_TERMINATE".equals(intent.getAction())){
        killSessions();
        return START_NOT_STICKY;
    }
    createNotificationChannel();
    startForeground(1, createNotification(intent));
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    killSessions();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel serviceChannel =
          new NotificationChannel(
              CHANNEL_ID, "Package Service Channel", NotificationManager.IMPORTANCE_LOW);
      NotificationManager manager = getSystemService(NotificationManager.class);
      if (manager != null) {
        manager.createNotificationChannel(serviceChannel);
      }
    }
  }

  private Notification createNotification(Intent intent) {
    Intent body = new Intent(this, TerminalActivity.class);
    body.putExtra("service", true);
    body.putExtra("pkgPath", intent.getStringExtra("pkgPath"));
    body.putExtra("config", intent.getStringExtra("config"));
    body.setFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_CLEAR_TOP
            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent bodyIntent =
        PendingIntent.getActivity(
            this, 0, body, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        
        Intent terminate = new Intent(this, PkgService.class);
    terminate.setAction("ACTION_TERMINATE");
    PendingIntent terminateIntent =
        PendingIntent.getService(
            this, 1, terminate, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            
    return new NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Package Active")
        .setContentText(Config.getPkgName(intent.getStringExtra("pkgPath"))+" is running.!")
        .setSmallIcon(R.drawable.ic_terminal)
        .addAction(R.drawable.ic_terminate, "Terminate", terminateIntent)
        .setContentIntent(bodyIntent)
        .setOngoing(true)
        .setAutoCancel(false)
        .build();
  }

  private void removeAndSwitchSession(TerminalSession session) {
    int pos = -1;
    for (int i = 0; i < sessions.size(); i++) {
      if (sessions.get(i).getTerminalSession().equals(session)) {
        pos = i;
        break;
      }
    }
    sessions.remove(pos);
    if (!sessions.isEmpty()) {
      sessionIndex = Math.max(0, Math.min(pos - 1, sessions.size() - 1));
    } else {
      terminateService();
    }
  }

  @Override
  public void onSessionFinished(TerminalSession finishedSession) {
    removeAndSwitchSession(finishedSession);
    if (callback != null) {
      callback.onSessionFinished(finishedSession);
    }
  }

  @Override
  public void onTextChanged(TerminalSession changedSession) {
    if (callback != null) {
      callback.onTextChanged(changedSession);
    }
  }

  @Override
  public void onTitleChanged(TerminalSession changedSession) {
    if (callback != null) {
      callback.onTitleChanged(changedSession);
    }
  }

  @Override
  public void onClipboardText(TerminalSession session, String text) {
    if (callback != null) {
      callback.onClipboardText(session, text);
    }
  }

  @Override
  public void onBell(TerminalSession session) {
      if (callback != null) {
      callback.onBell(session);
    }
  }

  public class LocalBinder extends Binder {
    public PkgService getService() {
      return PkgService.this;
    }
  }
}

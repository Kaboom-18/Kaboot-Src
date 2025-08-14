package geq.kaboom.app.kaboot.terminal;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import geq.kaboom.app.kaboot.utils.KabUtil;
import geq.kaboom.app.kaboot.services.PkgService;
import geq.kaboom.app.kaboot.terminal.termlib.TerminalSession;
import geq.kaboom.app.kaboot.terminal.termview.TerminalView;
import geq.kaboom.app.kaboot.terminal.termview.ExtraKeysView;
import geq.kaboom.app.kaboot.R;
import geq.kaboom.app.kaboot.utils.Config;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public final class TerminalActivity extends AppCompatActivity implements ServiceConnection {

  private int currentFontSize;
  private MaterialToolbar toolbar;
  private boolean mVirtualControlKeyDown, mVirtualFnKeyDown;
  private SharedPreferences config;
  private KabUtil util;
  private PkgService pkgService;
  private ArrayList<Session> sessions;
  private Intent serviceIntent;
  private Session pkg;
  private InputDispatcher disp;

  TerminalView mTerminalView;
  ExtraKeysView mExtraKeysView;

  @Override
  public void onServiceConnected(ComponentName name, IBinder service) {
    PkgService.LocalBinder binder = (PkgService.LocalBinder) service;
    pkgService = binder.getService();
    pkgService.callback =
        new TerminalSession.SessionChangedCallback() {
          @Override
          public void onSessionFinished(TerminalSession finishedSession) {
            if (pkgService.getSessionIndex() != -1) {
              loadSession(pkgService.getSessionIndex());
            }
          }

          @Override
          public void onTextChanged(TerminalSession changedSession) {
            mTerminalView.onScreenUpdated();
          }

          @Override
          public void onTitleChanged(TerminalSession changedSession) {
            // When a terminal program changes title
          }

          @Override
          public void onClipboardText(TerminalSession session, String text) {
            util.copy(text);
          }

          @Override
          public void onBell(TerminalSession session) {
            // Notification for user from terminal commands
          }
        };
    sessions = pkgService.getTerminalSessions();
    if (sessions.isEmpty()) {
      addAndSwitchSession();
    } else {
      loadSession(pkgService.getSessionIndex());
    }
  }

  @Override
  public void onServiceDisconnected(ComponentName name) {
    if (!this.isFinishing()) {
      finishAndRemoveTask();
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    Intent intent = new Intent(this, PkgService.class);
    if (!bindService(intent, this, 0)) {
      throw new RuntimeException("bindService() failed");
    }
    mTerminalView.onScreenUpdated();
  }

  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    setContentView(R.layout.activity_terminal);

    toolbar = findViewById(R.id.toolbar);
    sessions = new ArrayList<>();
    util = new KabUtil(this);
    disp = new InputDispatcher(this);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    mTerminalView = findViewById(R.id.terminal_view);
    mExtraKeysView = findViewById(R.id.extra_keys);
    mTerminalView.setOnKeyListener(disp);
    mTerminalView.requestFocus();
    mTerminalView.setKeepScreenOn(true);

    config = getSharedPreferences("Configuration", MODE_PRIVATE);
    currentFontSize = config.getInt("fontSize", -1);
    if (currentFontSize == -1
        || currentFontSize > Config.MAX_FONT_SIZE
        || currentFontSize < Config.MIN_FONT_SIZE) {
      // dynamic fontsize
      currentFontSize = util.getDefaultFontSize();
    }
    mTerminalView.setTextSize(currentFontSize);
    try {
      mTerminalView.setTypeface(Typeface.createFromAsset(getAssets(), "terminal.ttf"));
    } catch (Exception e) {
      mTerminalView.setTypeface(Typeface.MONOSPACE);
    }

    decorateActivity();
    if (!getIntent().getBooleanExtra("service", false)) {
      startService();
    }
  }

  private void startService() {
    serviceIntent = new Intent(this, PkgService.class);
    serviceIntent.putExtra("pkgPath", getIntent().getStringExtra("pkgPath"));
    serviceIntent.putExtra("config", getIntent().getStringExtra("config"));
    ContextCompat.startForegroundService(this, serviceIntent);
  }

  private void addAndSwitchSession() {
    try {
      Session session =
          new Session(
              this,
              getIntent().getStringExtra("pkgPath"),
              getIntent().getStringExtra("config"),
              pkgService);
      pkgService.addTerminalSession(session, getIntent().getStringExtra("pkgPath"));
      loadSession(sessions.indexOf(session));
    } catch (Exception e) {
      util.toast("Couldn't create a session!");
      finish();
      return;
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
      return true;
    }

    if (item.getItemId() == R.id.sessions) {
      ArrayList<String> sessionTitles = new ArrayList<>();
      for (int i = 0; i < sessions.size(); i++) {
        sessionTitles.add(sessions.get(i).getSessionTitle() + " Session (" + (i + 1) + ")");
      }
      new MaterialAlertDialogBuilder(this)
          .setTitle("Active Sessions")
          .setItems(sessionTitles.toArray(new String[0]), (dialog, which) -> loadSession(which))
          .setPositiveButton(
              "Add",
              (dialog, which) -> {
                addAndSwitchSession();
              })
          .setNegativeButton(
              "Keyboard",
              (dialog, which) -> {
                disp.onSingleTapUp(null);
              })
          .show();
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  private void loadSession(int pos) {
    if (sessions != null && pos >= 0 && pos < sessions.size()) {
      pkgService.setSessionIndex(pos);
      mTerminalView.attachSession(sessions.get(pos).getTerminalSession());
      getSupportActionBar().setTitle(sessions.get(pos).getSessionTitle() + " (" + (pos + 1) + ")");
    }
  }

  private void decorateActivity() {
    try {
      int color = Color.parseColor(config.getString("color", Config.TERM_BG));
      mTerminalView.setBackgroundColor(color);
      mExtraKeysView.setBackgroundColor(color);
    } catch (Exception e) {
      util.toast("Invalid terminal color returing to default!");
      mTerminalView.setBackgroundColor(Color.parseColor(Config.TERM_BG));
      mExtraKeysView.setBackgroundColor(Color.parseColor(Config.TERM_BG));
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (pkgService != null) {
      pkgService.callback = null;
      pkgService = null;
      unbindService(this);
    }
  }

  public void onBack() {
    finish();
  }

  public void doPaste() {
    ClipboardManager clipboard = getSystemService(ClipboardManager.class);
    if (clipboard != null) {
      ClipData clipData = clipboard.getPrimaryClip();
      if (clipData != null && clipData.getItemCount() > 0) {
        CharSequence paste = clipData.getItemAt(0).coerceToText(this);
        if (!TextUtils.isEmpty(paste)) {
          TerminalSession currentSession = mTerminalView.getCurrentSession();
          if (currentSession != null && currentSession.getEmulator() != null) {
            currentSession.getEmulator().paste(paste.toString());
          }
        }
      }
    }
  }

  public void changeFontSize(boolean increase) {
    currentFontSize += (increase ? 1 : -1);
    currentFontSize =
        Math.max(Config.MIN_FONT_SIZE, Math.min(currentFontSize, Config.MAX_FONT_SIZE));
    mTerminalView.setTextSize(currentFontSize);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.terminal_menu, menu);
    return true;
  }
}

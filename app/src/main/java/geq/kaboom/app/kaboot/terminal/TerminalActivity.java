package geq.kaboom.app.kaboot.terminal;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import geq.kaboom.app.kaboot.KabUtil;
import geq.kaboom.app.kaboot.terminal.termlib.TerminalSession;
import geq.kaboom.app.kaboot.terminal.termview.TerminalView;
import geq.kaboom.app.kaboot.terminal.termview.ExtraKeysView;
import geq.kaboom.app.kaboot.R;
import geq.kaboom.app.kaboot.Config;
import java.util.ArrayList;

public final class TerminalActivity extends AppCompatActivity {

    private int minFontSize;
    private int currentFontSize = -1;
    private MaterialToolbar toolbar;
    private boolean mVirtualControlKeyDown, mVirtualFnKeyDown;
    private SharedPreferences config;
    private KabUtil util;
    private Package pkg;
    private ArrayList<TerminalSession> sessions;
    private InputDispatcher disp;

    TerminalView mTerminalView;
    ExtraKeysView mExtraKeysView;

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
        
        try{
        getWindow().getDecorView().setBackgroundColor(Color.parseColor(config.getString("color", Config.TERM_BG)));
        getWindow().setNavigationBarColor(Color.parseColor(config.getString("color", Config.TERM_BG)));
        }catch(Exception e){
            util.toast("Invalid terminal color returing to default!");
           getWindow().getDecorView().setBackgroundColor(Color.parseColor(Config.TERM_BG));
        getWindow().setNavigationBarColor(Color.parseColor(Config.TERM_BG));
        }
       
        pkg = new Package(this, getIntent().getStringExtra("pkgPath"), getIntent().getStringExtra("config"),
                new TerminalSession.SessionChangedCallback() {
                    @Override
                    public void onSessionFinished(TerminalSession finishedSession) {
                        removeAndSwitchSession(finishedSession);
                    }

                    @Override
                    public void onTextChanged(TerminalSession changedSession) {
                        mTerminalView.onScreenUpdated();
                    }

                    @Override
                    public void onTitleChanged(TerminalSession changedSession) {}

                    @Override
                    public void onClipboardText(TerminalSession session, String text) {
                        util.copy(text);
                    }

                    @Override
                    public void onBell(TerminalSession session) {}
                });
        setupTerminalStyle();
        addAndSwitchSession();
    }

    private void addAndSwitchSession() {
        TerminalSession session = pkg.getTerminalSession();
        if (session == null) {
            util.toast("Couldn't initialize this package!");
            finish();
            return;
        }

        sessions.add(session);
        loadSession(sessions.indexOf(session));
    }

    private void setupTerminalStyle() {
     float scale = getResources().getDisplayMetrics().density;
        int defaultFontSize = Math.round(Config.FSCALE * scale);
        if (defaultFontSize % 2 == 1) defaultFontSize--;

        if (currentFontSize == -1) {
            currentFontSize = defaultFontSize;
        }

        minFontSize = (int) ((Config.FSCALE - 7) * scale);
        currentFontSize = Math.max(minFontSize, Math.min(currentFontSize, Config.MAX_FONTSIZE));
        mTerminalView.setTextSize(currentFontSize);

        try {
            Typeface typeface = Typeface.createFromAsset(getAssets(), "terminal.ttf");
            mTerminalView.setTypeface(typeface);
        } catch (Exception e) {
            mTerminalView.setTypeface(Typeface.MONOSPACE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTerminalView.onScreenUpdated();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        killSessions();
    }
    
    public void onBack(){
        
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
        currentFontSize += (increase ? 2 : -2);
        currentFontSize = Math.max(minFontSize, Math.min(currentFontSize, Config.MAX_FONTSIZE));
        mTerminalView.setTextSize(currentFontSize);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.terminal_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            killSessions();
            util.toast("Terminated!");
            return true;
        }

        if (item.getItemId() == R.id.sessions) {
            ArrayList<String> sessionTitles = new ArrayList<>();
            for (int i = 0; i < sessions.size(); i++) {
                sessionTitles.add("Session (" + (i + 1) + ")");
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Active Sessions")
                    .setItems(sessionTitles.toArray(new String[0]), (dialog, which) -> loadSession(which))
                    .setPositiveButton("Add", (dialog, which)->{
                       addAndSwitchSession();
                    })
                    .setNegativeButton("Keyboard", (dialog, which)->{
                        disp.onSingleTapUp(null);
                    })
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void killSessions() {
        if (sessions == null) return;
            for (TerminalSession session : sessions) {
                if(!killSessionProcess(session.getPid())) util.toast("Session processes autoKill failed!");
                session.finishIfRunning();
            }
    }

   private void removeAndSwitchSession(TerminalSession session) {
        int pos = sessions.indexOf(session);
            sessions.remove(pos);
            if (!sessions.isEmpty()) {
                int newIndex = Math.max(0, Math.min(pos - 1, sessions.size() - 1));
                loadSession(newIndex);
            } else finish();
    }

    private void loadSession(int pos) {
        if (sessions != null && pos >= 0 && pos < sessions.size()) {
            mTerminalView.attachSession(sessions.get(pos));
            getSupportActionBar().setTitle(
                    Config.getPkgName(this, getIntent().getStringExtra("pkgPath")) + " (" + (pos + 1) + ")"
            );
        }
    }
    
    public boolean killSessionProcess(int ppid){
        final boolean res[] = {false};
           try{
                util.getProcesses().forEach((process)-> {
                if(process.get("ppid").equals(String.valueOf(ppid))){
                    killSessionProcess(Integer.valueOf(process.get("pid")));
                    res[0] = util.killProcess(Integer.parseInt(process.get("pid")));
                }});
                }catch(Exception e){}
                return res[0];
            }
}
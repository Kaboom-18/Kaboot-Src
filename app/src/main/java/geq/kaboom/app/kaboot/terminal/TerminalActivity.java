package geq.kaboom.app.kaboot.terminal;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import geq.kaboom.app.kaboot.KabUtil;
import geq.kaboom.app.kaboot.terminal.termlib.TerminalSession;
import geq.kaboom.app.kaboot.terminal.termview.TerminalView;
import geq.kaboom.app.kaboot.terminal.termview.ExtraKeysView;
import geq.kaboom.app.kaboot.R;
import geq.kaboom.app.kaboot.Config;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public final class TerminalActivity extends AppCompatActivity {

    private int minFontSize;
    private int currentFontSize = -1;
    private MaterialToolbar toolbar;
    private boolean mVirtualControlKeyDown, mVirtualFnKeyDown;
    private SharedPreferences config;
    private KabUtil util;
    private Package pkg;
    private TerminalSession session;

    TerminalView mTerminalView;
    ExtraKeysView mExtraKeysView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_terminal);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getIntent().getStringExtra("name"));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTerminalView = findViewById(R.id.terminal_view);
        mExtraKeysView = findViewById(R.id.extra_keys);
        mTerminalView.setOnKeyListener(new InputDispatcher(this));
        mTerminalView.setKeepScreenOn(true);
        mTerminalView.requestFocus();

        config = getSharedPreferences("Configuration", MODE_PRIVATE);
        currentFontSize = config.getInt("fontSize", -1);
        util = new KabUtil(this);
        
        setupTerminalStyle();

        pkg = new Package(this, getIntent().getStringExtra("name"), getIntent().getStringExtra("pkgPath"), getIntent().getStringExtra("config"),
            new TerminalSession.SessionChangedCallback() {
                    @Override
                    public void onSessionFinished(TerminalSession finishedSession) {
                        finish();
                    }

                    @Override
                    public void onTextChanged(TerminalSession changedSession) {
                        mTerminalView.onScreenUpdated();
                    }

                    @Override
                    public void onTitleChanged(TerminalSession changedSession) {}

                    @Override
                    public void onClipboardText(TerminalSession session, String text) {
                        ClipboardManager clipboard = getSystemService(ClipboardManager.class);
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(
                                    new ClipData(null, new String[]{"text/plain"},
                                            new ClipData.Item(text)));
                        }
                    }
                    
                    @Override
                    public void onBell(TerminalSession session) {}
                });

       try{
        session = pkg.getTerminalSession();
        }catch(Exception e){
            util.toast("Couldn't initialize this package!");
            finish();
        }
        mTerminalView.attachSession(session);
    }

    private void setupTerminalStyle() {
        float scale = getResources().getDisplayMetrics().scaledDensity;
        int defaultFontSize = Math.round(Config.FSCALE * scale);
        if (defaultFontSize % 2 == 1) defaultFontSize--;

        if (currentFontSize == -1) {
            currentFontSize = defaultFontSize;
        }

        minFontSize = (int) ((Config.FSCALE - 2) * scale);
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
        if (session != null) session.finishIfRunning();
    }

    public void onBack() {
      
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
public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
          if(session != null) session.finishIfRunning();
          util.toast("Terminated!");
        return true;
    }
    return super.onOptionsItemSelected(item);
}
}
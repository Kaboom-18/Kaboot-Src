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

    TerminalView mTerminalView;
    ExtraKeysView mExtraKeysView;
    TerminalSession mTermSession;

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

        String libdir = getApplicationInfo().nativeLibraryDir;
        setupTerminalStyle();

        ArrayList<String> cmd = new ArrayList<>();
        util.makeDir(getCacheDir().getAbsolutePath().concat("/shm"));
        util.makeDir(getCacheDir().getAbsolutePath().concat("/tmp"));
        cmd.add(libdir.concat("/libkaboot.so"));
        cmd.add("--kill-on-exit");
        cmd.add("-w");
        cmd.add("/");
        cmd.add("-b");
        cmd.add("/dev");
        cmd.add("-b");
        cmd.add("/proc");
        cmd.add("-b");
        cmd.add("/sys");
        cmd.add("-b");
        cmd.add(getCacheDir().getAbsolutePath().concat("/shm:/dev/shm"));
        cmd.add("-b");
        cmd.add(getCacheDir().getAbsolutePath().concat("/tmp:/tmp"));
        cmd.add("-r");
        cmd.add(getIntent().getStringExtra("pkgPath").concat("/rootfs"));
        try {
            JSONObject obj = new JSONObject(getIntent().getStringExtra("config"));
            if (obj.has("args")) {
                JSONArray args = obj.getJSONArray("args");
                for (int i = 0; i < args.length(); i++) {
                    cmd.add(args.getString(i));
                }
            }
            if (obj.has("variables")) {
                JSONArray variables = obj.getJSONArray("variables");
                for (int i = 0; i < variables.length(); i++) {
                    if (variables.getString(i).startsWith("HOME")) {
                        cmd.add("-w");
                        cmd.add(variables.getString(i).split("=")[1]);
                    }
                }
                cmd.add(obj.getString("env"));
                cmd.add("-i");
                for (int i = 0; i < variables.length(); i++) {
                    cmd.add(variables.getString(i));
                }
            }
            if (obj.has("cmd")) {
                JSONArray commands = obj.getJSONArray("cmd");
                for (int i = 0; i < commands.length(); i++) {
                    cmd.add(commands.getString(i));
                }
            }
        } catch (Exception e) {
            util.toast("Couldn't initialize the package");
            finish();
        }

        mTermSession = new TerminalSession(
                cmd.toArray(new String[0]),
                new String[]{
                        "LD_LIBRARY_PATH=" + libdir,
                        "PROOT_LOADER=" + libdir + "/libkabooter.so",
                        "PROOT_LOADER_32=" + libdir + "/libkabooter32.so",
                        "PROOT_TMP_DIR=" + getCacheDir().getAbsolutePath()
                },
                getCacheDir().getAbsolutePath(),
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

        mTerminalView.attachSession(mTermSession);
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
        if (mTermSession != null) mTermSession.finishIfRunning();
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
          if(mTermSession != null) mTermSession.finishIfRunning();
          util.toast("Terminated!");
        return true;
    }
    return super.onOptionsItemSelected(item);
}
}
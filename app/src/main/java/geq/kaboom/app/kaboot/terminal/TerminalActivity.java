package geq.kaboom.app.kaboot.terminal;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import geq.kaboom.app.kaboot.KabUtil;
import geq.kaboom.app.kaboot.terminal.termlib.TerminalSession;
import geq.kaboom.app.kaboot.terminal.termlib.TerminalSession.SessionChangedCallback;
import geq.kaboom.app.kaboot.terminal.termview.TerminalView;
import geq.kaboom.app.kaboot.terminal.termview.ExtraKeysView;
import geq.kaboom.app.kaboot.R;
import geq.kaboom.app.kaboot.terminal.termview.TerminalViewClient;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public final class TerminalActivity extends AppCompatActivity {

    private final int MAX_FONTSIZE = 256;
    private int MIN_FONTSIZE;
    private static int currentFontSize = -1;

    TerminalView mTerminalView;

    ExtraKeysView mExtraKeysView;

    TerminalSession mTermSession;

    private boolean mVirtualControlKeyDown, mVirtualFnKeyDown;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_terminal);

        getSupportActionBar().setTitle(getIntent().getStringExtra("name"));

        mTerminalView = findViewById(R.id.terminal_view);
        mExtraKeysView = findViewById(R.id.extra_keys);
        mTerminalView.setOnKeyListener(new InputDispatcher(this));
        mTerminalView.setKeepScreenOn(true);
        mTerminalView.requestFocus();
        setupTerminalStyle();

        String libdir = getApplicationInfo().nativeLibraryDir;
        ArrayList<String> cmd = new ArrayList<>();
        KabUtil.makeDir(getCacheDir().getAbsolutePath().concat("/shm"));
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
                for(int i=0; i<commands.length(); i++){
                cmd.add(commands.getString(i));
                }
            }
        } catch (Exception e) {
            KabUtil.toast(this, "Couldn't initialize the package");
            finish();
        }
        mTermSession =
                new TerminalSession(
                        cmd.toArray(new String[0]),
                        new String[] {
                            "LD_LIBRARY_PATH=".concat(libdir),
                            "PROOT_LOADER=".concat(libdir.concat("/libkabooter.so")),
                            "PROOT_LOADER_32=".concat(libdir.concat("/libkabooter32.so")),
                            "PROOT_TMP_DIR=".concat(getCacheDir().getAbsolutePath())
                        },
                        getCacheDir().getAbsolutePath(),
                        new TerminalSession.SessionChangedCallback() {

                            @Override
                            public void onSessionFinished(final TerminalSession finishedSession) {
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
                                ClipboardManager clipboard =
                                        (ClipboardManager)
                                                getSystemService(Context.CLIPBOARD_SERVICE);
                                if (clipboard != null) {
                                    clipboard.setPrimaryClip(
                                            new ClipData(
                                                    null,
                                                    new String[] {"text/plain"},
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
        int defaultFontSize = Math.round(9.5f * scale);
        if (defaultFontSize % 2 == 1) defaultFontSize--;

        if (TerminalActivity.currentFontSize == -1) {
            TerminalActivity.currentFontSize = defaultFontSize;
        }

        MIN_FONTSIZE = (int) (4f * scale);

        TerminalActivity.currentFontSize =
                Math.max(MIN_FONTSIZE, Math.min(TerminalActivity.currentFontSize, MAX_FONTSIZE));
        mTerminalView.setTextSize(TerminalActivity.currentFontSize);
        mTerminalView.setTypeface(Typeface.createFromAsset(getAssets(), "console_font.ttf"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mTerminalView.onScreenUpdated();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTermSession.finishIfRunning();
    }

    public void onBack() {
        mTermSession.finishIfRunning();
        finish();
    }

    public void doPaste() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    ClipboardManager clipboard = getSystemService(ClipboardManager.class);
    if (clipboard != null) {
        ClipData clipData = clipboard.getPrimaryClip();
        if (clipData != null) {
            CharSequence paste = clipData.getItemAt(0).coerceToText(this);
            if (!TextUtils.isEmpty(paste)) {
                TerminalSession currentSession = mTerminalView.getCurrentSession();

                if (currentSession != null) {
                    currentSession.getEmulator().paste(paste.toString());
                }
            }
        }
    }
} else {
    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    if (clipboard != null) {
        ClipData clipData = clipboard.getPrimaryClip();
        if (clipData != null) {
            CharSequence paste = clipData.getItemAt(0).coerceToText(this);
            if (!TextUtils.isEmpty(paste)) {
                TerminalSession currentSession = mTerminalView.getCurrentSession();

                if (currentSession != null) {
                    currentSession.getEmulator().paste(paste.toString());
                }
            }
        }
    }
}
    }

    public void changeFontSize(boolean increase) {
        TerminalActivity.currentFontSize += (increase ? 1 : -1) * 2;
        TerminalActivity.currentFontSize =
                Math.max(MIN_FONTSIZE, Math.min(TerminalActivity.currentFontSize, MAX_FONTSIZE));
        mTerminalView.setTextSize(TerminalActivity.currentFontSize);
    }
}

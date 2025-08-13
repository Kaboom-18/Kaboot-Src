package geq.kaboom.app.kaboot.terminal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;

import androidx.core.content.ContextCompat;
import java.util.Objects;

import geq.kaboom.app.kaboot.terminal.termlib.KeyHandler;
import geq.kaboom.app.kaboot.terminal.termlib.TerminalEmulator;
import geq.kaboom.app.kaboot.terminal.termlib.TerminalSession;
import geq.kaboom.app.kaboot.terminal.termview.TerminalViewClient;

@SuppressWarnings("WeakerAccess")
public final class InputDispatcher implements TerminalViewClient {

    private final TerminalActivity mActivity;

    private boolean mVirtualControlKeyDown, mVirtualFnKeyDown;

    public InputDispatcher(TerminalActivity activity) {
        this.mActivity = activity;
    }

    @Override
    public float onScale(float scale) {
        if (scale < 0.9f || scale > 1.1f) {
            boolean increase = scale > 1.f;
            mActivity.changeFontSize(increase);
            return 1.0f;
        }
        return scale;
    }

    @Override
    public void onSingleTapUp(MotionEvent e) {
        InputMethodManager mgr = (InputMethodManager) ContextCompat.getSystemService(mActivity, InputMethodManager.class);
        if (mgr != null) mgr.showSoftInput(mActivity.mTerminalView, InputMethodManager.SHOW_IMPLICIT);
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e, TerminalSession currentSession) {
        if (handleVirtualKeys(keyCode, e, true)) return true;
        if(keyCode == KeyEvent.KEYCODE_BACK){mActivity.onBack(); return true;}
         
        if (e.isCtrlPressed() && e.isAltPressed()) {
            int unicodeChar = e.getUnicodeChar(e.getMetaState());

            if (unicodeChar == 'k') {
                InputMethodManager imm =
                        (InputMethodManager) ContextCompat.getSystemService(mActivity, InputMethodManager.class);
                if (imm != null) imm.showSoftInput(mActivity.mTerminalView, InputMethodManager.SHOW_IMPLICIT);
            } else if (unicodeChar == 'm') {
                mActivity.mTerminalView.showContextMenu();
            } else if (unicodeChar == 'v') {
                mActivity.doPaste();
            } else if (unicodeChar == '+' || e.getUnicodeChar(KeyEvent.META_SHIFT_ON) == '+') {
                mActivity.changeFontSize(true);
            } else if (unicodeChar == '-') {
                mActivity.changeFontSize(false);
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent e) {
        return handleVirtualKeys(keyCode, e, false);
    }

    @Override
    public boolean readControlKey() {
        return (mActivity.mExtraKeysView != null && mActivity.mExtraKeysView.readControlButton())
                || mVirtualControlKeyDown;
    }

    @Override
    public boolean readAltKey() {
        return (mActivity.mExtraKeysView != null && mActivity.mExtraKeysView.readAltButton());
    }

    @Override
    public boolean readShiftKey() {
        return (mActivity.mExtraKeysView != null && mActivity.mExtraKeysView.readShiftButton());
    }

    @Override
    public boolean readFnKey() {
        return (mActivity.mExtraKeysView != null && mActivity.mExtraKeysView.readFnButton());
    }

    @Override
    public boolean onCodePoint(final int codePoint, boolean ctrlDown, TerminalSession session) {
        if (mVirtualFnKeyDown) {
            int resultingKeyCode = -1;
            int resultingCodePoint = -1;
            boolean altDown = false;

            int lowerCase = Character.toLowerCase(codePoint);

            switch (lowerCase) {
                case 'w':
                    resultingKeyCode = KeyEvent.KEYCODE_DPAD_UP;
                    break;
                case 'a':
                    resultingKeyCode = KeyEvent.KEYCODE_DPAD_LEFT;
                    break;
                case 's':
                    resultingKeyCode = KeyEvent.KEYCODE_DPAD_DOWN;
                    break;
                case 'd':
                    resultingKeyCode = KeyEvent.KEYCODE_DPAD_RIGHT;
                    break;
                case 'p':
                    resultingKeyCode = KeyEvent.KEYCODE_PAGE_UP;
                    break;
                case 'n':
                    resultingKeyCode = KeyEvent.KEYCODE_PAGE_DOWN;
                    break;
                case 't':
                    resultingKeyCode = KeyEvent.KEYCODE_TAB;
                    break;
                case 'i':
                    resultingKeyCode = KeyEvent.KEYCODE_INSERT;
                    break;
                case 'h':
                    resultingCodePoint = '~';
                    break;
                case 'u':
                    resultingCodePoint = '_';
                    break;
                case 'l':
                    resultingCodePoint = '|';
                    break;
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    resultingKeyCode = (codePoint - '1') + KeyEvent.KEYCODE_F1;
                    break;
                case '0':
                    resultingKeyCode = KeyEvent.KEYCODE_F10;
                    break;
                case 'e':
                    resultingCodePoint = 27;
                    break;
                case '.':
                    resultingCodePoint = 28;
                    break;

                case 'b':
                case 'f':
                case 'x':
                    resultingCodePoint = lowerCase;
                    altDown = true;
                    break;

                case 'v':
                    resultingCodePoint = -1;
                    AudioManager audio =
                            (AudioManager) ContextCompat.getSystemService(mActivity, AudioManager.class);
                    if (audio != null) audio.adjustVolume(AudioManager.ADJUST_SAME,AudioManager.USE_DEFAULT_STREAM_TYPE);
                    break;
                case 'k':
                    mVirtualFnKeyDown = false;
                    break;
            }

            if (resultingKeyCode != -1) {
                TerminalEmulator term = session.getEmulator();
                session.write(
                        Objects.requireNonNull(
                                KeyHandler.getCode(
                                        resultingKeyCode,
                                        0,
                                        term.isCursorKeysApplicationMode(),
                                        term.isKeypadApplicationMode())));
            } else if (resultingCodePoint != -1) {
                session.writeCodePoint(altDown, resultingCodePoint);
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean onLongPress(MotionEvent event) {
        return false;
    }

    private boolean handleVirtualKeys(int keyCode, KeyEvent event, boolean down) {
        InputDevice inputDevice = event.getDevice();

        if (inputDevice != null
                && inputDevice.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC) {
            return false;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            mVirtualControlKeyDown = down;
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            mVirtualFnKeyDown = down;
            return true;
        }

        return false;
    }
}
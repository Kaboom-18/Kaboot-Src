package geq.kaboom.app.kaboot.terminal.termview;

import android.view.MotionEvent;
import android.view.ViewTreeObserver;

public interface CursorController extends ViewTreeObserver.OnTouchModeChangeListener {

    void show(MotionEvent event);

    boolean hide();

    void render();

    void updatePosition(TextSelectionHandleView handle, int x, int y);

    boolean onTouchEvent(MotionEvent event);

    void onDetached();

    boolean isActive();

}
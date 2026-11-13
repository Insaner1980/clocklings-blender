package fi.clocklings.fixture;

import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;

/** Test-APK-only keyboard using the real system InputConnection. */
public final class AuditInputMethodService extends InputMethodService {
    @Override public boolean onEvaluateFullscreenMode() { return false; }

    @Override public void onStartInput(EditorInfo info, boolean restarting) {
        super.onStartInput(info, restarting);
        Log.i("ClocklingsAuditIme", "start package=" + info.packageName + " restarting=" + restarting);
    }

    @Override public void onFinishInput() {
        Log.i("ClocklingsAuditIme", "finish input");
        super.onFinishInput();
    }

    @Override public View onCreateInputView() {
        LinearLayout keyboard = new LinearLayout(this);
        keyboard.setOrientation(LinearLayout.VERTICAL);
        keyboard.setBackgroundColor(0xffe0e0e0);
        // Keep native test buttons above the IME navigation controls, including on API 26.
        WindowInsets initialInsets = getWindow().getWindow().getDecorView().getRootWindowInsets();
        if (initialInsets != null) {
            keyboard.setPadding(0, 0, 0, initialInsets.getSystemWindowInsetBottom());
        }
        keyboard.setOnApplyWindowInsetsListener((view, insets) -> {
            int bottom = insets.getSystemWindowInsetBottom();
            if (view.getPaddingBottom() != bottom) {
                view.setPadding(0, 0, 0, bottom);
            }
            return insets;
        });
        String[][] labels = {{"Compose", "Replace", "Commit"}, {"Append", "Cancel", "Done"}};
        for (String[] rowLabels : labels) {
            LinearLayout row = new LinearLayout(this);
            for (String label : rowLabels) {
                Button key = new Button(this);
                key.setText(label);
                key.setAllCaps(false);
                key.setOnClickListener(view -> edit(label));
                row.addView(key, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            }
            keyboard.addView(row);
        }
        keyboard.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            ViewParent parent = keyboard.getParent();
            Log.i("ClocklingsAuditIme", "layout height=" + keyboard.getHeight() +
                    " measured=" + keyboard.getMeasuredHeight() + " padding=" + keyboard.getPaddingBottom() +
                    " rows=" + keyboard.getChildAt(0).getHeight() + "," + keyboard.getChildAt(1).getHeight() +
                    " parent=" + (parent instanceof View ? ((View) parent).getHeight() : -1));
        });
        return keyboard;
    }

    private void edit(String label) {
        if (getCurrentInputEditorInfo() == null ||
                !"fi.clocklings".equals(getCurrentInputEditorInfo().packageName)) return;
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) return;
        Log.i("ClocklingsAuditIme", "edit=" + label + " connection=" + System.identityHashCode(connection));
        switch (label) {
            case "Compose": connection.setComposingText("Ka", 1); break;
            case "Replace": connection.setComposingText("Kati", 1); break;
            case "Commit": connection.finishComposingText(); break;
            case "Append": connection.setComposingText("x", 1); break;
            case "Cancel":
                connection.setComposingText("", 1);
                connection.finishComposingText();
                break;
            case "Done": connection.performEditorAction(EditorInfo.IME_ACTION_DONE); break;
            default: throw new IllegalArgumentException(label);
        }
    }
}

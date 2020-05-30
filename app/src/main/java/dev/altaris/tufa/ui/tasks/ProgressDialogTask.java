package dev.altaris.tufa.ui.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Process;

import dev.altaris.tufa.ui.Dialogs;

import androidx.annotation.CallSuper;

public abstract class ProgressDialogTask<Params, Result> extends AsyncTask<Params, String, Result> {
    private ProgressDialog _dialog;

    public ProgressDialogTask(Context context, String message) {
        _dialog = new ProgressDialog(context);
        _dialog.setCancelable(false);
        _dialog.setMessage(message);
        Dialogs.secureDialog(_dialog);
    }

    @CallSuper
    @Override
    protected void onPreExecute() {
        _dialog.show();
    }

    @CallSuper
    @Override
    protected void onPostExecute(Result result) {
        if (_dialog.isShowing()) {
            _dialog.dismiss();
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if (values.length == 1) {
            _dialog.setMessage(values[0]);
        }
    }

    protected void setPriority() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE);
    }

    protected final ProgressDialog getDialog() {
        return _dialog;
    }
}

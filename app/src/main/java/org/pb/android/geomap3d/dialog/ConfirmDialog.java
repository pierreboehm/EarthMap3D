package org.pb.android.geomap3d.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.pb.android.geomap3d.R;

@EViewGroup(R.layout.dialog_confirm)
public class ConfirmDialog extends LinearLayout {

    @ViewById(R.id.tvMessage)
    TextView tvMessage;

    @ViewById(R.id.btnConfirm)
    Button btnConfirm;

    @ViewById(R.id.btnCancel)
    Button btnCancel;

    private Runnable confirmAction;
    private Dialog dialog;

    public ConfirmDialog(Context context) {
        super(context);
    }

    public void show() {
        if (dialog == null) {

            if (confirmAction == null) {
                btnConfirm.setVisibility(GONE);
                btnCancel.setText("OK");
            }

            dialog = new AlertDialog.Builder(getContext()).setView(this).create();
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            final Window window = dialog.getWindow();
            if (window != null) {
                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialogInterface) {
                        // without onShowListener this line will crash on Android 6 devices
                        window.getDecorView().setBackgroundResource(android.R.color.transparent);
                    }
                });
            }

            dialog.setCanceledOnTouchOutside(false);
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    dismiss();
                }
            });
        }
        dialog.show();
    }

    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    @Click(R.id.btnConfirm)
    public void onConfirmClick() {
        if (confirmAction != null) {
            confirmAction.run();
        }
        dismiss();
    }

    @Click(R.id.btnCancel)
    public void onCancelClick() {
        dismiss();
    }

    public static class Builder {
        private ConfirmDialog dialog;

        public Builder(Context context) {
            dialog = ConfirmDialog_.build(context);
        }

        public ConfirmDialog build() {
            return dialog;
        }

        public Builder setMessage(String message) {
            dialog.tvMessage.setText(message);
            return this;
        }

        public Builder setConfirmAction(Runnable confirmAction) {
            dialog.confirmAction = confirmAction;
            return this;
        }
    }
}

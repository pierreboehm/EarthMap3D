package org.pb.android.geomap3d.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.SeekBarProgressChange;
import org.androidannotations.annotations.SeekBarTouchStop;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.pb.android.geomap3d.AppPreferences_;
import org.pb.android.geomap3d.R;
import org.pb.android.geomap3d.util.Util;

import java.util.Locale;

@EViewGroup(R.layout.dialog_settings)
public class SettingsDialog extends LinearLayout {

    @ViewById(R.id.switchCompass)
    Switch switchCompass;

    @ViewById(R.id.switchAutomaticTrack)
    Switch switchAutomaticTrack;

    @ViewById(R.id.sbTrackDistance)
    SeekBar seekBarTrackDistance;

    @ViewById(R.id.tvTrackDistanceInMeters)
    TextView tvTrackDistanceInMeters;

    @Pref
    AppPreferences_ preferences;

    private Runnable saveAction;
    private Dialog dialog;

    public SettingsDialog(Context context) {
        super(context);
    }

    @AfterViews
    public void initViews() {
        boolean useCompass = preferences.useCompass().getOr(true);
        switchCompass.setText(useCompass ? R.string.compassOnText : R.string.compassOffText);
        switchCompass.setChecked(useCompass);

        boolean trackPosition = preferences.trackPosition().getOr(true);
        switchAutomaticTrack.setText(trackPosition ? R.string.trackOnText : R.string.trackOffText);
        switchAutomaticTrack.setChecked(trackPosition);
    }

    @Click(R.id.switchCompass)
    public void onSwitchCompassClicked() {
        boolean useCompass = switchCompass.isChecked();
        preferences.useCompass().put(useCompass);

        switchCompass.setText(useCompass ? R.string.compassOnText : R.string.compassOffText);
    }

    @Click(R.id.switchAutomaticTrack)
    public void onSwitchAutomaticTrackClicked() {
        boolean isTrackOn = switchAutomaticTrack.isChecked();
        preferences.trackPosition().put(isTrackOn);

        seekBarTrackDistance.setEnabled(isTrackOn);
        switchAutomaticTrack.setText(isTrackOn ? R.string.trackOnText : R.string.trackOffText);
    }

    @SeekBarProgressChange(R.id.sbTrackDistance)
    public void onTrackDistanceChange(SeekBar seekBar, int progress) {
        int value = Util.roundUp(progress, 50);
        tvTrackDistanceInMeters.setText(String.format(Locale.getDefault(), "%d m", value));
    }

    @SeekBarTouchStop(R.id.sbTrackDistance)
    public void onTrackDistanceChanged(SeekBar seekBar) {
        int progressValue = Util.roundUp(seekBar.getProgress(), 50);
        preferences.defaultTrackDistanceInMeters().put(progressValue);
    }

    public void show() {
        if (dialog == null) {
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

    @Click(R.id.btnSave)
    public void onConfirmClick() {
        if (saveAction != null) {
            saveAction.run();
        }
        dismiss();
    }

    @Click(R.id.btnCancel)
    public void onCancelClick() {
        dismiss();
    }

    public static class Builder {
        private SettingsDialog dialog;

        public Builder(Context context) {
            dialog = SettingsDialog_.build(context);
        }

        public SettingsDialog build() {
            return dialog;
        }

        public SettingsDialog.Builder setSaveAction(Runnable confirmAction) {
            dialog.saveAction = confirmAction;
            return this;
        }
    }
}

package org.pb.android.geomap3d.audio;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.pb.android.geomap3d.R;


@EBean(scope = EBean.Scope.Singleton)
public class AudioPlayer {

    public static final String TAG = AudioPlayer.class.getSimpleName();

    public enum Track {
        OPEN(R.raw.bionic_eye_opens),
        CLOSE(R.raw.bionic_eye_closes),
        TOUCH(R.raw.button_press);

        private int resourceId;

        Track(int resourceId) {
            this.resourceId = resourceId;
        }

        public int getResourceId() {
            return resourceId;
        }
    }

    @RootContext
    Context context;

    private MediaPlayer mediaPlayer;

    public void release() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            Log.d(TAG, "media player released");
        }
    }

    public void play(Track track) {
        mediaPlayer = MediaPlayer.create(context, track.getResourceId());
        mediaPlayer.setVolume(1f, 1f);  // has no effect
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.reset();
            }
        });
        mediaPlayer.start();
    }
}

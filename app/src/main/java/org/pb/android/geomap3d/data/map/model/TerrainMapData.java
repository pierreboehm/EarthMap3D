package org.pb.android.geomap3d.data.map.model;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import okhttp3.ResponseBody;

public class TerrainMapData {

    private static final String TAG = TerrainMapData.class.getSimpleName();

    public enum LoadingState {
        LOADING_INTERRUPTED,
        LOADING_FAILED,
        LOADING_SUCCESS
    }

    private Bitmap bitmap;
    private LoadingState loadingState;

    public TerrainMapData(Activity activity, ResponseBody responseBody) {
        loadingState = saveZipAndExtractBitmap(activity, responseBody);
    }

    public TerrainMapData(LoadingState loadingState) {
        this.loadingState = loadingState;
    }

    public LoadingState getLoadingState() {
        return loadingState;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    // FIXME: !!! optimize !!!
    private LoadingState saveZipAndExtractBitmap(Activity activity, ResponseBody responseBody) {
        boolean downloadSuccess = true;
        InputStream inputStream = null;
        File zipFile = null;

        try {
            inputStream = responseBody.byteStream();

            byte[] byteBuffer = new byte[4096];
            long currentSize = 0;
            long targetSize = responseBody.contentLength();

            zipFile = new File(activity.getCacheDir(), "map.zip");
            OutputStream output = new FileOutputStream(zipFile);

            while (currentSize < targetSize) {
                int readSize = inputStream.read(byteBuffer);
                if (readSize == -1) {
                    break;
                }

                output.write(byteBuffer, 0, readSize);
                currentSize += readSize;
            }

            output.flush();
            output.close();

        } catch (IOException ignore) {
            downloadSuccess = false;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException exception) {
                    // not implemented yet
                }
            }
        }

        downloadSuccess &= readZipAndExtractBitmap(zipFile);
        return downloadSuccess ? LoadingState.LOADING_SUCCESS : LoadingState.LOADING_FAILED;
    }

    private boolean readZipAndExtractBitmap(File downloadedZipFile) {
        boolean bitmapExtracted = false;

        try {
            ZipFile zipFile = new ZipFile(downloadedZipFile);
            for (ZipEntry zipEntry : Collections.list(zipFile.entries())) {
                if (!zipEntry.isDirectory() && zipEntry.getName().toLowerCase().contains("merged")) {
                    bitmap = BitmapFactory.decodeStream(zipFile.getInputStream(zipEntry));
                    bitmapExtracted = true;
                    break;
                }
            }
        } catch (Exception exception) {
            Log.e(TAG, exception.getLocalizedMessage());
        }

        return bitmapExtracted;
    }
}

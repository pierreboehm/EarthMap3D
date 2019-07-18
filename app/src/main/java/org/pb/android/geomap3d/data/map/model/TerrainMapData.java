package org.pb.android.geomap3d.data.map.model;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okhttp3.ResponseBody;

public class TerrainMapData {

    public enum LoadingState {
        LOADING_INTERRUPTED,
        LOADING_FAILED,
        LOADING_SUCCESS
    }

    private Bitmap bitmap;
    private LoadingState loadingState;

    public TerrainMapData(Activity activity, ResponseBody responseBody) {
        loadingState = LoadingState.LOADING_SUCCESS;
        saveZipAndExtractBitmap(activity, responseBody);
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

    private void saveZipAndExtractBitmap(Activity activity, ResponseBody responseBody) {
        boolean downloadSuccess = true;
        InputStream inputStream = null;

        try {
            inputStream = responseBody.byteStream();

            byte[] byteBuffer = new byte[4096];
            long currentSize = 0;
            long targetSize = responseBody.contentLength();

            File zipFile = new File(activity.getCacheDir(), "map.zip");
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

        if (downloadSuccess) {
            Log.v("TerrainMapData", "Map zip-file successfully downloaded.");
        } else {
            Log.v("TerrainMapData", "Map zip-file download failed.");
        }
    }

    private Bitmap extractBitmapFromZip(ZipInputStream zipInputStream) throws Exception {
        ZipInputStream zipIn = new ZipInputStream(zipInputStream);
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdir();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }

        zipIn.close();

        return null;
    }
    /**
     * Extracts a zip entry (file entry)
     * @param zipIn
     * @param filePath
     * @throws IOException
     */
    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[4096];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
}

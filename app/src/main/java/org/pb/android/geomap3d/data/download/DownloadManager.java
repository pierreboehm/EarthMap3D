package org.pb.android.geomap3d.data.download;

import android.graphics.Bitmap;
import android.location.Location;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadManager {

    // http://terrain.party/api/export?name=kaufunger_wald_2&box=9.858200,51.317693,9.743203,51.245828
    private static final String BASEURL = "http://terrain.party/api/export/?";

    public Bitmap getHeightMapBitmapForLocation(Location location) {
        return null;
    }

    private void downloadZipFile(String zipUrl) {
        try {
            ReadableByteChannel readChannel = Channels.newChannel(new URL(zipUrl).openStream());
            FileOutputStream fileOutputStream = new FileOutputStream("terrainmap.zip");
            FileChannel writeChannel = fileOutputStream.getChannel();
            writeChannel.transferFrom(readChannel, 0, Long.MAX_VALUE);
            writeChannel.close();
            readChannel.close();
        } catch (Exception exception) {
            // we'll see ...
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

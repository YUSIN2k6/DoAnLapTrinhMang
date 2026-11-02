package com.vku.lanmonitor.server.util;

import lombok.extern.slf4j.Slf4j;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

@Slf4j
public class VideoEncoderUtil {

    private static final int FPS = 5;   // 25 frames / 5s = 5 fps

    /**
     * Encode folder chua frames thanh video MP4.
     * @param framesDir Folder chua frame_001.jpg, frame_002.jpg, ...
     * @param outputFile File video output (clip.mp4)
     * @return true neu thanh cong
     */
    public static boolean encodeFramesToMp4(File framesDir, File outputFile) {
        if (framesDir == null || !framesDir.exists() || !framesDir.isDirectory()) {
            log.error("[VIDEO] Frames dir khong ton tai: {}", framesDir);
            return false;
        }

        File[] frames = framesDir.listFiles(
            f -> f.isFile() && f.getName().toLowerCase().endsWith(".jpg")
        );

        if (frames == null || frames.length == 0) {
            log.error("[VIDEO] Khong co frame .jpg trong {}", framesDir);
            return false;
        }

        Arrays.sort(frames);

        SeekableByteChannel out = null;
        try {
            out = NIOUtils.writableChannel(outputFile);
            AWTSequenceEncoder encoder = new AWTSequenceEncoder(
                out, Rational.R(FPS, 1)
            );

            for (File frame : frames) {
                BufferedImage img = ImageIO.read(frame);
                if (img == null) {
                    log.warn("[VIDEO] Khong doc duoc frame: {}", frame.getName());
                    continue;
                }
                encoder.encodeImage(img);
            }

            encoder.finish();
            log.info("[VIDEO] Da tao video: {} ({} frames, {} fps)",
                outputFile.getName(), frames.length, FPS);
            return true;

        } catch (Exception e) {
            log.error("[VIDEO] Loi encode: {}", e.getMessage(), e);
            return false;
        } finally {
            if (out != null) {
                try { out.close(); } catch (Exception ignored) {}
            }
        }
    }
}

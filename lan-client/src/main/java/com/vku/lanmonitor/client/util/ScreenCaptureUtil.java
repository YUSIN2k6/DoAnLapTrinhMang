package com.vku.lanmonitor.client.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class ScreenCaptureUtil {

    public static byte[] captureCurrentScreen() {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenFullImage = robot.createScreenCapture(screenRect);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Nén sang định dạng JPG chất lượng để truyền mạng nhanh
            ImageIO.write(screenFullImage, "jpg", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("Lỗi khi chụp màn hình: " + e.getMessage());
            return new byte[0];
        }
    }

    public static BufferedImage captureScreen() {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            return robot.createScreenCapture(screenRect);
        } catch (Exception e) {
            System.err.println("Lỗi khi chụp màn hình: " + e.getMessage());
            return new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
        }
    }
}
package com.vku.lanmonitor.client.util;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

public class ActiveWindowUtil {
    /**
     * Hàm gọi xuống Windows API để lấy tên cửa sổ hiện tại (Active Window)
     */
    public static String getActiveWindowTitle() {
        HWND fgWindow = User32.INSTANCE.GetForegroundWindow();
        if (fgWindow == null) {
            return "";
        }
        int titleLength = User32.INSTANCE.GetWindowTextLength(fgWindow) + 1;
        char[] title = new char[titleLength];
        User32.INSTANCE.GetWindowText(fgWindow, title, titleLength);
        return Native.toString(title).trim();
    }
}
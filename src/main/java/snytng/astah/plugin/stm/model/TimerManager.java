package snytng.astah.plugin.stm.model;

import java.util.Timer;
import java.util.TimerTask;
import javax.swing.SwingUtilities;

public class TimerManager {
    private Timer timer;
    private TimerTask currentTask;
    private boolean isFastMode = false;

    public void setFastMode(boolean isFastMode) {
        this.isFastMode = isFastMode;
    }

    public boolean isFastMode() {
        return isFastMode;
    }

    public void schedule(long delay, Runnable action) {
        cancel(); // Cancel previous
        if (isFastMode) {
            // Execute immediately (or with very short delay) on EDT
            SwingUtilities.invokeLater(action);
        } else {
            timer = new Timer(true);
            currentTask = new TimerTask() {
                @Override
                public void run() {
                    SwingUtilities.invokeLater(action);
                }
            };
            timer.schedule(currentTask, delay);
        }
    }

    public void cancel() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public static long parseDuration(String eventName) {
        if (eventName == null) return -1;
        if (eventName.matches("tm\\(\\d+\\)")) {
            try {
                String val = eventName.substring(3, eventName.length() - 1);
                return Long.parseLong(val);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
}
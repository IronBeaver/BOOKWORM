package com.bookworm.reader;

/** One fresh-task opportunity; backgrounding invalidates even an in-flight library scan. */
public final class LaunchAttempt {
    private boolean pending, foreground;
    private int epoch;
    public LaunchAttempt(boolean freshLauncherTask) { pending = freshLauncherTask; }
    public void resume() { foreground = true; }
    public void pause() { foreground = false; pending = false; epoch++; }
    public int claim() { if (!pending) return -1; pending = false; return epoch; }
    public boolean allowed(int ticket) { return ticket >= 0 && foreground && ticket == epoch; }
}

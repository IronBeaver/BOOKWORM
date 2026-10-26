package com.bookworm.reader;

/** Input-only tap qualification, independent of viewport pagination and reading positions. */
public final class ReaderTap {
    public enum Action { NONE, PREVIOUS, TOGGLE, NEXT }
    private final float downX, downY, slop;
    public final long downTime;
    private boolean cancelled, finished, blocked;
    private float x;
    public ReaderTap(float x, float y, long time, float slop, boolean selection) {
        downX = x; downY = y; this.x = x; downTime = time; this.slop = slop; blocked = selection;
    }
    public void block() { blocked = true; }
    public void cancel() { cancelled = true; }
    public void move(float x, float y) { if (Math.abs(x - downX) > slop || Math.abs(y - downY) > slop) cancel(); }
    public boolean moved() { return cancelled; }
    public void finish(float x, float y, long time, long maximumDuration) {
        move(x, y); this.x = x; finished = true;
        if (time < downTime || time - downTime >= maximumDuration) cancel();
    }
    public boolean eligible() { return finished && !cancelled && !blocked; }
    public Action action(float width, boolean paged, boolean exploration, boolean interactive, boolean selection) {
        if (!eligible() || Float.isNaN(width) || Float.isInfinite(width) || Float.isNaN(x) || Float.isInfinite(x) || width <= 0 || x < 0 || x >= width || exploration || interactive || selection) return Action.NONE;
        if (x >= width * .30f && x < width * .70f) return Action.TOGGLE;
        if (!paged) return Action.NONE;
        return x < width * .30f ? Action.PREVIOUS : Action.NEXT;
    }
}

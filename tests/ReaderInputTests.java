package com.bookworm.reader;

public final class ReaderInputTests {
    private static int checks;
    private static void check(boolean value, String name) { checks++; if (!value) throw new AssertionError(name); }
    private static ReaderTap tap(float x) { ReaderTap t = new ReaderTap(x, 200, 100, 8, false); t.finish(x, 200, 180, 500); return t; }
    public static void main(String[] args) {
        for (int width : new int[]{320, 600, 800, 1280}) {
            float left = width * .30f, right = width * .70f;
            float[] points = {0, left - .1f, left, (left + right) / 2, right - .1f, right, width - .1f};
            ReaderTap.Action[] expected = {ReaderTap.Action.PREVIOUS,ReaderTap.Action.PREVIOUS,ReaderTap.Action.TOGGLE,ReaderTap.Action.TOGGLE,ReaderTap.Action.TOGGLE,ReaderTap.Action.NEXT,ReaderTap.Action.NEXT};
            for (int i = 0; i < points.length; i++) {
                check(tap(points[i]).action(width, true, false, false, false) == expected[i], "Page zone boundary " + width + "/" + i);
                check(tap(points[i]).action(width, false, false, false, false) == (expected[i] == ReaderTap.Action.TOGGLE ? ReaderTap.Action.TOGGLE : ReaderTap.Action.NONE), "Scroll only toggles in centre");
                check(tap(points[i]).action(width, true, false, true, false) == ReaderTap.Action.NONE, "Link takes priority in every zone");
                check(tap(points[i]).action(width, true, false, false, true) == ReaderTap.Action.NONE, "Selection takes priority in every zone");
                check(tap(points[i]).action(width, true, true, false, false) == ReaderTap.Action.NONE, "TalkBack leaves gestures to WebView");
            }
        }
        ReaderTap drag = new ReaderTap(30, 200, 100, 8, false); drag.move(30, 220); drag.finish(30, 200, 180, 500);
        check(drag.action(600, true, false, false, false) == ReaderTap.Action.NONE, "Move and return is still a drag");
        ReaderTap pinch = tap(30); pinch.cancel(); check(!pinch.eligible(), "Multi-pointer/cancelled gesture rejected");
        ReaderTap longPress = new ReaderTap(30, 200, 100, 8, false); longPress.finish(30, 200, 600, 500); check(!longPress.eligible(), "Long-press duration rejected");
        ReaderTap shortTap = new ReaderTap(30, 200, 100, 8, false); shortTap.finish(30, 200, 599, 500); check(shortTap.eligible(), "Short tap below long-press threshold");
        ReaderTap selectionDown = new ReaderTap(30, 200, 100, 8, true); selectionDown.finish(30, 200, 180, 500); check(!selectionDown.eligible(), "Selection at down remains protected after clearing");
        ReaderTap domDown = tap(30); domDown.block(); check(!domDown.eligible(), "Delayed DOM selection/link guard latches");
        for (float x : new float[]{-1,600,Float.NaN,Float.POSITIVE_INFINITY}) check(tap(x).action(600, true, false, false, false) == ReaderTap.Action.NONE, "Out-of-bounds coordinates ignored");
        check(tap(20).action(0, true, false, false, false) == ReaderTap.Action.NONE, "Unknown viewport ignored");
        ReaderChromeState chrome = new ReaderChromeState(false); check(!chrome.shown(), "Default uncluttered state");
        chrome.toggle(); check(chrome.shown(), "Centre shows controls"); chrome.toggle(); check(!chrome.shown(), "Centre dismisses controls");
        chrome.preference(false); check(!chrome.shown(), "Unchanged preference preserves transient visibility");
        chrome.preference(true); check(chrome.shown(), "Changed preferred default applied"); chrome.show(false); chrome.preference(true); check(!chrome.shown(), "Returning from settings does not force unchanged default");
        chrome.accessibility(true); check(chrome.shown(), "Touch exploration exposes native controls");
        boolean saved = chrome.shown(); ReaderChromeState restored = new ReaderChromeState(false); restored.show(saved); check(restored.shown(), "Transient overlay visibility restores separately from default");
        check(new ReaderChromeState(true).shown(), "Preferred open state survives a fresh reader session");
        System.out.println("Passed " + checks + " reader tap-zone/gesture/selection/accessibility/chrome-state checks.");
    }
}

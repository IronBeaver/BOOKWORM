package com.bookworm.reader;

/** Default preference and transient overlay visibility are deliberately separate. */
public final class ReaderChromeState {
    private boolean preferred, shown;
    public ReaderChromeState(boolean preferred) { this.preferred = preferred; shown = preferred; }
    public boolean shown() { return shown; }
    public void show(boolean value) { shown = value; }
    public void toggle() { shown = !shown; }
    public void preference(boolean value) { if (preferred != value) { preferred = value; shown = value; } }
    public void accessibility(boolean exploration) { if (exploration) shown = true; }
}

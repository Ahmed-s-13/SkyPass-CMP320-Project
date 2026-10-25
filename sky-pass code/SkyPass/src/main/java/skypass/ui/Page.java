package skypass.ui;

/** A screen in the main window. onShow() runs each time it is opened, to load fresh data. */
public interface Page {
    void onShow();
}

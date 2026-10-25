package skypass;

import javax.swing.SwingUtilities;
import skypass.ui.Theme;
import skypass.ui.anim.SplashWindow;

/**
 * SkyPass: flight booking database GUI (CMP 320, Stage 2).
 *
 * Before running: put your Oracle username and password in
 * skypass/db/Db.java and run the SQL scripts 01 to 06 in SQL Developer.
 */
public class App {

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "SkyPass");
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameAqua");
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            SplashWindow splash = new SplashWindow();
            splash.connect(() -> {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
                splash.dispose();
            });
        });
    }
}

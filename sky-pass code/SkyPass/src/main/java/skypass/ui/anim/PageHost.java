package skypass.ui.anim;

import java.awt.CardLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** Holds the pages; switching pages fades the new one in while it rises 14px. */
public class PageHost {

    private final CardLayout cards = new CardLayout();
    private final JPanel deck = new JPanel(cards);
    private final FadeLayer fade;

    public PageHost() {
        deck.setOpaque(false);
        fade = FadeLayer.wrap(deck, 14);
    }

    public void add(String name, JComponent page) {
        deck.add(page, name);
    }

    public void show(String name) {
        cards.show(deck, name);
        fade.play(320, 0);
    }

    public JComponent component() {
        return fade.component();
    }
}

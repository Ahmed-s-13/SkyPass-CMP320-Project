package skypass.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * A read-only table of domain objects. Rows are separated by a single
 * rule (no stripes), numbers are right-aligned, and an empty table says
 * so instead of showing a blank box.
 */
public class DataTable<T> extends JPanel {

    private final String[] columns;
    private final Function<T, Object[]> mapper;
    private final List<T> items = new ArrayList<>();
    private final List<Object[]> cells = new ArrayList<>();
    private final Model model = new Model();
    private final JTable table;
    private String emptyText = "Nothing here yet.";

    public DataTable(String[] columns, Function<T, Object[]> mapper) {
        super(new BorderLayout());
        this.columns = columns;
        this.mapper = mapper;
        setOpaque(false);

        table = new JTable(model) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getRowCount() == 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setFont(Theme.body());
                    g2.setColor(Theme.INK_3);
                    int w = g2.getFontMetrics().stringWidth(emptyText);
                    g2.drawString(emptyText, Math.max(12, (getWidth() - w) / 2), 40);
                    g2.dispose();
                }
            }

            @Override
            public boolean getScrollableTracksViewportHeight() {
                return getParent() != null && getParent().getHeight() > getPreferredSize().height;
            }
        };
        table.setFont(Theme.body());
        table.setForeground(Theme.INK);
        table.setBackground(Theme.SHEET);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(Theme.small());
        table.setAutoCreateRowSorter(true);
        table.setDefaultRenderer(Object.class, new CellRenderer());
        table.setDefaultRenderer(Number.class, new CellRenderer());
        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.LEADING);

        JScrollPane scroll = Ui.scroll(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        add(scroll, BorderLayout.CENTER);
    }

    public void setItems(List<T> newItems) {
        T keep = selected();
        items.clear();
        cells.clear();
        items.addAll(newItems);
        for (T item : newItems) {
            cells.add(mapper.apply(item));
        }
        model.fireTableDataChanged();
        // keep the same row selected after a refresh, when it still exists
        if (keep != null) {
            int i = items.indexOf(keep);
            if (i >= 0) {
                int view = table.convertRowIndexToView(i);
                table.getSelectionModel().setSelectionInterval(view, view);
            }
        }
    }

    public List<T> items() {
        return items;
    }

    public T selected() {
        int view = table.getSelectedRow();
        if (view < 0) {
            return null;
        }
        int index = table.convertRowIndexToModel(view);
        return index < items.size() ? items.get(index) : null;
    }

    public void select(T item) {
        int i = items.indexOf(item);
        if (i >= 0) {
            int view = table.convertRowIndexToView(i);
            table.getSelectionModel().setSelectionInterval(view, view);
            table.scrollRectToVisible(table.getCellRect(view, 0, true));
        }
    }

    public void clearSelection() {
        table.clearSelection();
    }

    /** Called with the selected item (or null) whenever the selection settles. */
    public void onSelect(Consumer<T> listener) {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                listener.accept(selected());
            }
        });
    }

    public void setEmptyText(String text) {
        emptyText = text;
        table.repaint();
    }

    public void widths(int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            if (widths[i] > 0) {
                table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            }
        }
    }

    /** Keeps a column at least this wide when the window is narrow. */
    public void minWidth(int column, int width) {
        table.getColumnModel().getColumn(column).setMinWidth(width);
    }

    public void renderer(int column, TableCellRenderer renderer) {
        table.getColumnModel().getColumn(column).setCellRenderer(renderer);
    }

    public JTable table() {
        return table;
    }

    // ---------------- Renderers ----------------

    /** Text with padding; numbers right-aligned in tabular style. */
    public static class CellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                       boolean focus, int row, int col) {
            Object shown = value;
            if (value instanceof BigDecimal b) {
                shown = Ui.money(b);
            } else if (value instanceof Number n && !(value instanceof BigDecimal)) {
                shown = Ui.whole(n.doubleValue());
            } else if (value == null) {
                shown = "–";
            }
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, shown, selected, false, row, col);
            l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            l.setToolTipText(shown.toString().isEmpty() ? null : shown.toString());   // full text when a cell is cut off
            l.setHorizontalAlignment(value instanceof Number ? SwingConstants.TRAILING : SwingConstants.LEADING);
            l.setForeground(value == null ? Theme.INK_3 : Theme.INK);
            l.setFont(Theme.body());
            return l;
        }
    }

    /** Status text with a small coloured dot: colour carries meaning, not decoration. */
    public static TableCellRenderer status() {
        return new CellRenderer() {
            private Color dot = Theme.INK_3;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focus, int row, int col) {
                String raw = value == null ? "" : value.toString();
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, Ui.nice(raw), selected, focus, row, col);
                dot = statusColor(raw);
                l.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 10));
                return l;
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(dot);
                g2.fillOval(10, getHeight() / 2 - 3, 7, 7);
                g2.dispose();
            }
        };
    }

    public static Color statusColor(String status) {
        if (status == null) {
            return Theme.INK_3;
        }
        return switch (status) {
            case "CONFIRMED", "SCHEDULED" -> Theme.OK;
            case "PENDING", "DELAYED" -> Theme.WARN;
            case "CANCELLED" -> Theme.DANGER;
            default -> Theme.INK_3;   // ARRIVED, DEPARTED
        };
    }

    /** A cell that shows one text but sorts by another value (e.g. a date). */
    public record Sorted(String text, Comparable<Object> key) implements Comparable<Sorted> {
        @SuppressWarnings("unchecked")
        public static Sorted of(String text, Comparable<?> key) {
            return new Sorted(text, (Comparable<Object>) key);
        }

        public int compareTo(Sorted other) {
            if (key == null || other.key == null) {
                return key == null ? (other.key == null ? 0 : -1) : 1;
            }
            return key.compareTo(other.key);
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private final class Model extends AbstractTableModel {
        public int getRowCount() {
            return cells.size();
        }

        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        public Object getValueAt(int row, int column) {
            return cells.get(row)[column];
        }

        @Override
        public Class<?> getColumnClass(int column) {
            for (Object[] r : cells) {
                if (r[column] != null) {
                    return r[column].getClass();
                }
            }
            return Object.class;
        }
    }
}

package skypass.dao;

/** One choice in a dropdown: a database key and the text shown for it. */
public record Item(Object key, String label) {

    public int id() {
        return ((Number) key).intValue();
    }

    public String code() {
        return key.toString();
    }

    @Override
    public String toString() {
        return label;
    }
}

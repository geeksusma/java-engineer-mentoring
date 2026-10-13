package macrodesign.clean.frameworks.database;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The outermost circle: a "database driver" that knows tables and columns,
 * nothing about players. A real one would be JDBC, JPA, a document store...
 */
public final class PlayerTable {

    public record Row(String id, String nickname, String nicknameKey, String email) {
    }

    private final Map<String, Row> rows = new LinkedHashMap<>();
    private int sequence = 0;

    public String nextId() {
        return "P-" + (++sequence);
    }

    public void insert(Row row) {
        rows.put(row.id(), row);
    }

    public Optional<Row> findByNicknameKey(String nicknameKey) {
        return rows.values().stream().filter(row -> row.nicknameKey().equals(nicknameKey)).findFirst();
    }

    public int count() {
        return rows.size();
    }
}

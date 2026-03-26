package ya.practicum.blog.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ya.practicum.blog.model.Comment;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class CommentRepository {
    private static final RowMapper<Comment> COMMENT_ROW_MAPPER = (rs, rowNum) -> new Comment(
            rs.getLong("id"),
            rs.getLong("post_id"),
            rs.getString("text")
    );

    private final JdbcTemplate jdbcTemplate;

    public CommentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Comment> findByPostId(long postId) {
        return jdbcTemplate.query(
                "SELECT id, post_id, text FROM comments WHERE post_id = ? ORDER BY id ASC",
                COMMENT_ROW_MAPPER,
                postId
        );
    }

    public Optional<Comment> findByIdAndPostId(long id, long postId) {
        List<Comment> comments = jdbcTemplate.query(
                "SELECT id, post_id, text FROM comments WHERE id = ? AND post_id = ?",
                COMMENT_ROW_MAPPER,
                id,
                postId
        );
        return comments.stream().findFirst();
    }

    public Optional<Comment> findById(long id) {
        List<Comment> comments = jdbcTemplate.query(
                "SELECT id, post_id, text FROM comments WHERE id = ?",
                COMMENT_ROW_MAPPER,
                id
        );
        return comments.stream().findFirst();
    }

    public Comment create(long postId, String text) {
        String sql = "INSERT INTO comments(post_id, text) VALUES (?, ?)";
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[] {"id"});
            statement.setLong(1, postId);
            statement.setString(2, text);
            return statement;
        }, keyHolder);
        Number generatedId = Objects.requireNonNull(keyHolder.getKey(), "Failed to create comment");
        return findByIdAndPostId(generatedId.longValue(), postId).orElseThrow();
    }

    public Comment updateById(long id, String text) {
        jdbcTemplate.update("UPDATE comments SET text = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", text, id);
        return findById(id).orElseThrow();
    }

    public int deleteByIdAndPostId(long id, long postId) {
        return jdbcTemplate.update("DELETE FROM comments WHERE id = ? AND post_id = ?", id, postId);
    }
}

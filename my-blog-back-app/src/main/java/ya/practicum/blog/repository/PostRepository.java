package ya.practicum.blog.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ya.practicum.blog.model.Post;
import ya.practicum.blog.model.PostImage;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Repository
public class PostRepository {
    private static final RowMapper<Post> POST_ROW_MAPPER = (rs, rowNum) -> new Post(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("text"),
            deserializeTags(rs.getString("tags")),
            rs.getInt("likes_count"),
            rs.getInt("comments_count")
    );

    private final JdbcTemplate jdbcTemplate;

    public PostRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Counts posts matching title substring (case-insensitive) and hashtag filters against CSV {@code tags}.
     * Tag matching uses comma boundaries so {@code java} does not match {@code javascript}.
     */
    public int countPostsWithFilters(String titleQueryLower, List<String> tagFiltersLower) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM posts p WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        appendSearchFilters(sql, args, titleQueryLower, tagFiltersLower);
        Integer n = jdbcTemplate.queryForObject(sql.toString(), Integer.class, args.toArray());
        return n != null ? n : 0;
    }

    /**
     * Paginated posts with the same filter semantics as {@link #countPostsWithFilters}, newest {@code id} first.
     */
    public List<Post> findPostsPageWithFilters(String titleQueryLower, List<String> tagFiltersLower, int limit, int offset) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.id, p.title, p.text, p.tags, p.likes_count, COUNT(c.id) AS comments_count
                FROM posts p
                LEFT JOIN comments c ON c.post_id = p.id
                WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        appendSearchFilters(sql, args, titleQueryLower, tagFiltersLower);
        sql.append("""
                GROUP BY p.id, p.title, p.text, p.tags, p.likes_count
                ORDER BY p.id DESC
                LIMIT ? OFFSET ?
                """);
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.query(sql.toString(), POST_ROW_MAPPER, args.toArray());
    }

    public Optional<Post> findById(long id) {
        String sql = """
                SELECT p.id, p.title, p.text, p.tags, p.likes_count, COUNT(c.id) AS comments_count
                FROM posts p
                LEFT JOIN comments c ON c.post_id = p.id
                WHERE p.id = ?
                GROUP BY p.id
                """;
        List<Post> posts = jdbcTemplate.query(sql, POST_ROW_MAPPER, id);
        return posts.stream().findFirst();
    }

    public Post create(Post post) {
        String sql = "INSERT INTO posts(title, text, tags, likes_count) VALUES (?, ?, ?, ?)";
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[] {"id"});
            statement.setString(1, post.title());
            statement.setString(2, post.text());
            statement.setString(3, serializeTags(post.tags()));
            statement.setInt(4, post.likesCount());
            return statement;
        }, keyHolder);
        Number generatedId = Objects.requireNonNull(keyHolder.getKey(), "Failed to create post");
        return findById(generatedId.longValue()).orElseThrow();
    }

    public Post update(long id, Post post) {
        String sql = "UPDATE posts SET title = ?, text = ?, tags = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        jdbcTemplate.update(sql, post.title(), post.text(), serializeTags(post.tags()), id);
        return findById(id).orElseThrow();
    }

    public int deleteById(long id) {
        return jdbcTemplate.update("DELETE FROM posts WHERE id = ?", id);
    }

    public int incrementLikes(long id) {
        jdbcTemplate.update("UPDATE posts SET likes_count = likes_count + 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?", id);
        return jdbcTemplate.queryForObject("SELECT likes_count FROM posts WHERE id = ?", Integer.class, id);
    }

    public int updateImage(long postId, byte[] imageData, String contentType) {
        String sql = "UPDATE posts SET image_data = ?, image_content_type = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        return jdbcTemplate.update(sql, imageData, contentType, postId);
    }

    public Optional<PostImage> findImage(long postId) {
        String sql = "SELECT image_data, image_content_type FROM posts WHERE id = ?";
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next() || rs.getBytes("image_data") == null) {
                return Optional.empty();
            }
            return Optional.of(new PostImage(rs.getBytes("image_data"), rs.getString("image_content_type")));
        }, postId);
    }

    private void appendSearchFilters(
            StringBuilder sql,
            List<Object> args,
            String titleQueryLower,
            List<String> tagFiltersLower
    ) {
        if (titleQueryLower != null && !titleQueryLower.isBlank()) {
            sql.append(" AND LOWER(p.title) LIKE ? ESCAPE '\\' ");
            args.add("%" + escapeLikePattern(titleQueryLower) + "%");
        }
        if (tagFiltersLower != null) {
            for (String tag : tagFiltersLower) {
                if (tag == null || tag.isBlank()) {
                    continue;
                }
                sql.append(" AND LOWER(CONCAT(',', p.tags, ',')) LIKE ? ESCAPE '\\' ");
                args.add("%," + escapeLikePattern(tag.toLowerCase(Locale.ROOT)) + ",%");
            }
        }
    }

    private static String escapeLikePattern(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String serializeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return String.join(",", tags);
    }

    private static List<String> deserializeTags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(rawTags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();
    }
}

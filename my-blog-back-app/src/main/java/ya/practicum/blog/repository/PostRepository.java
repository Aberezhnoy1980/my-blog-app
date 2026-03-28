package ya.practicum.blog.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ya.practicum.blog.model.Post;
import ya.practicum.blog.model.PostImage;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class PostRepository {
    private static final RowMapper<PostCore> POST_CORE_ROW_MAPPER = (rs, rowNum) -> new PostCore(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("text"),
            rs.getInt("likes_count"),
            rs.getInt("comments_count")
    );

    private final JdbcTemplate jdbcTemplate;

    public PostRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Counts posts matching title substring (case-insensitive) and hashtag filters via normalized {@code tags} table.
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
                SELECT p.id, p.title, p.text, p.likes_count, COUNT(c.id) AS comments_count
                FROM posts p
                LEFT JOIN comments c ON c.post_id = p.id
                WHERE 1=1
                """);
        List<Object> args = new ArrayList<>();
        appendSearchFilters(sql, args, titleQueryLower, tagFiltersLower);
        sql.append("""
                GROUP BY p.id, p.title, p.text, p.likes_count
                ORDER BY p.id DESC
                LIMIT ? OFFSET ?
                """);
        args.add(limit);
        args.add(offset);
        List<PostCore> cores = jdbcTemplate.query(sql.toString(), POST_CORE_ROW_MAPPER, args.toArray());
        return attachTags(cores);
    }

    public Optional<Post> findById(long id) {
        String sql = """
                SELECT p.id, p.title, p.text, p.likes_count, COUNT(c.id) AS comments_count
                FROM posts p
                LEFT JOIN comments c ON c.post_id = p.id
                WHERE p.id = ?
                GROUP BY p.id, p.title, p.text, p.likes_count
                """;
        List<PostCore> rows = jdbcTemplate.query(sql, POST_CORE_ROW_MAPPER, id);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        PostCore c = rows.get(0);
        Map<Long, List<String>> tagMap = loadTagNamesForPosts(List.of(id));
        return Optional.of(toPost(c, tagMap.getOrDefault(id, List.of())));
    }

    public Post create(Post post) {
        String sql = "INSERT INTO posts(title, text, likes_count) VALUES (?, ?, ?)";
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[] {"id"});
            statement.setString(1, post.title());
            statement.setString(2, post.text());
            statement.setInt(3, post.likesCount());
            return statement;
        }, keyHolder);
        Number generatedId = Objects.requireNonNull(keyHolder.getKey(), "Failed to create post");
        long id = generatedId.longValue();
        replacePostTags(id, post.tags());
        return findById(id).orElseThrow();
    }

    public Post update(long id, Post post) {
        String sql = "UPDATE posts SET title = ?, text = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        jdbcTemplate.update(sql, post.title(), post.text(), id);
        replacePostTags(id, post.tags());
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

    private List<Post> attachTags(List<PostCore> cores) {
        if (cores.isEmpty()) {
            return List.of();
        }
        List<Long> ids = cores.stream().map(PostCore::id).toList();
        Map<Long, List<String>> byPost = loadTagNamesForPosts(ids);
        return cores.stream()
                .map(c -> toPost(c, byPost.getOrDefault(c.id(), List.of())))
                .toList();
    }

    private Post toPost(PostCore c, List<String> tags) {
        return new Post(c.id(), c.title(), c.text(), tags, c.likesCount(), c.commentsCount());
    }

    private Map<Long, List<String>> loadTagNamesForPosts(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = postIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = """
                SELECT pt.post_id, t.name
                FROM post_tags pt
                INNER JOIN tags t ON t.id = pt.tag_id
                WHERE pt.post_id IN (%s)
                ORDER BY pt.post_id, pt.tag_position
                """.formatted(placeholders);
        Object[] args = postIds.toArray();
        return jdbcTemplate.query(sql, rs -> {
            Map<Long, List<String>> map = new LinkedHashMap<>();
            while (rs.next()) {
                long pid = rs.getLong(1);
                String name = rs.getString(2);
                map.computeIfAbsent(pid, k -> new ArrayList<>()).add(name);
            }
            return map;
        }, args);
    }

    private void replacePostTags(long postId, List<String> tags) {
        jdbcTemplate.update("DELETE FROM post_tags WHERE post_id = ?", postId);
        if (tags == null || tags.isEmpty()) {
            return;
        }
        int position = 0;
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String raw : tags) {
            if (raw == null) {
                continue;
            }
            String name = raw.trim().toLowerCase(Locale.ROOT);
            if (name.isEmpty() || !seen.add(name)) {
                continue;
            }
            long tagId = findOrCreateTagId(name);
            jdbcTemplate.update(
                    "INSERT INTO post_tags (post_id, tag_id, tag_position) VALUES (?, ?, ?)",
                    postId,
                    tagId,
                    position++
            );
        }
    }

    private long findOrCreateTagId(String normalizedName) {
        List<Long> existing = jdbcTemplate.query(
                "SELECT id FROM tags WHERE name = ?",
                (rs, row) -> rs.getLong(1),
                normalizedName
        );
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        jdbcTemplate.update("INSERT INTO tags (name) VALUES (?)", normalizedName);
        Long id = jdbcTemplate.queryForObject("SELECT id FROM tags WHERE name = ?", Long.class, normalizedName);
        return Objects.requireNonNull(id, "tag id");
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
                sql.append("""
                         AND EXISTS (
                            SELECT 1 FROM post_tags ptx
                            INNER JOIN tags tgx ON tgx.id = ptx.tag_id
                            WHERE ptx.post_id = p.id AND LOWER(tgx.name) = ?
                        )
                        """);
                args.add(tag.toLowerCase(Locale.ROOT));
            }
        }
    }

    private static String escapeLikePattern(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private record PostCore(long id, String title, String text, int likesCount, int commentsCount) {
    }
}

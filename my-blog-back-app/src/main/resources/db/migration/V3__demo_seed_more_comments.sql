-- Extend demo dataset so the newest feed post also has comments in details view.

INSERT INTO comments (post_id, text)
SELECT p.id, 'Demo comment for the latest seeded post.'
FROM posts p
WHERE p.title LIKE 'Comments and likes%'
  AND NOT EXISTS (
      SELECT 1
      FROM comments c
      WHERE c.post_id = p.id
  );

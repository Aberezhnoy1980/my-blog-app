-- Reference data for empty databases (local Docker, portfolio demos). Applied once per database by Flyway.

INSERT INTO posts (title, text, tags, likes_count) VALUES
(
    'Getting started with Spring MVC',
    'This API uses Spring Framework without Spring Boot: Java config, DispatcherServlet, and JDBC.',
    'java,spring',
    12
),
(
    'Docker Compose for local development',
    'PostgreSQL, Tomcat WAR, and Nginx start together; the Practicum frontend calls the API on port 8080.',
    'docker,nginx',
    7
),
(
    'Пагинация и поиск',
    'Пример поста на русском — удобно проверить поиск и список на главной.',
    'postgres,sql',
    4
),
(
    'Comments and likes',
    'Use the UI to add comments or hit the likes endpoint; images are stored in PostgreSQL.',
    'rest,api',
    0
);

INSERT INTO comments (post_id, text)
SELECT p.id, 'Nice structure — clear separation of layers.' FROM posts p WHERE p.title = 'Getting started with Spring MVC';

INSERT INTO comments (post_id, text)
SELECT p.id, 'docker compose up --build works on a clean machine.' FROM posts p WHERE p.title = 'Docker Compose for local development';

INSERT INTO comments (post_id, text)
SELECT p.id, 'Полезный пример для ревью.' FROM posts p WHERE p.title = 'Пагинация и поиск';

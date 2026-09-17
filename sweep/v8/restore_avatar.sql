SET NOCOUNT ON;
SELECT id=2, url_before_fix = avatar_url FROM users WHERE user_id=2;
UPDATE users SET avatar_url = 'https://res.cloudinary.com/dsuvw92hh/image/upload/v1788343118/engflow/avatars/gawimjkso3ygpfsbgei6.png' WHERE user_id=2;
SELECT id=2, avatar_url FROM users WHERE user_id=2;
SELECT id=3, avatar_url FROM users WHERE user_id=3;

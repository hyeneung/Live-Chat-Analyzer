CREATE KEYSPACE IF NOT EXISTS chat_data
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'};

USE chat_data;

CREATE TABLE IF NOT EXISTS messages (
    id UUID,
    stream_id TEXT,
    sender_id TEXT,
    sender_name TEXT,
    sender_profile_image_url TEXT,
    content TEXT,
    created_at TIMESTAMP,
    PRIMARY KEY (stream_id, created_at, id)
) WITH CLUSTERING ORDER BY (created_at DESC, id ASC);
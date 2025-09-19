CREATE KEYSPACE IF NOT EXISTS chat_data
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'};

USE chat_data;

CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY,
    -- 🛑 DataStax 커넥터의 mapping은 필드 이름 변경을 지원하므로,
    --    가독성 좋은 snake_case 컬럼명을 그대로 사용해도 됩니다.
    stream_id TEXT,
    sender_id TEXT,
    sender_name TEXT,
    sender_profile_image_url TEXT,
    content TEXT,
    created_at TIMESTAMP
);
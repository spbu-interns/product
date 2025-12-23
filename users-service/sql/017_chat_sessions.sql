-- Chat sessions for medical assistant bot
-- Stores conversation history with Gemini AI for each user

CREATE TABLE IF NOT EXISTS chat_sessions (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    messages JSONB DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for fast lookup by user_id
CREATE INDEX idx_chat_sessions_user_id ON chat_sessions(user_id);

-- Index for session lookup
CREATE INDEX idx_chat_sessions_session_id ON chat_sessions(session_id);

-- Trigger to auto-update updated_at
CREATE OR REPLACE FUNCTION update_chat_session_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER chat_sessions_updated_at
    BEFORE UPDATE ON chat_sessions
    FOR EACH ROW
    EXECUTE FUNCTION update_chat_session_timestamp();

-- Comment on table
COMMENT ON TABLE chat_sessions IS 'Stores chat conversation history between users and Gemini AI medical assistant';
COMMENT ON COLUMN chat_sessions.messages IS 'JSONB array of chat messages in format: [{"role": "user"|"model", "parts": ["text"]}]';
COMMENT ON COLUMN chat_sessions.session_id IS 'Unique session identifier for resuming conversations';

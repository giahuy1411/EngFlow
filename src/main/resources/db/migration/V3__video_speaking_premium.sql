-- Video Speaking Premium — New tables for Phase 0
-- =================================================

-- Video Speaking Prompts (admin creates)
CREATE TABLE video_speaking_prompts (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    title NVARCHAR(200) NOT NULL,
    description NVARCHAR(MAX),
    prompt NVARCHAR(MAX) NOT NULL,
    level VARCHAR(20),
    category VARCHAR(50),
    is_premium BIT DEFAULT 0,
    thumbnail_url VARCHAR(500),
    order_index INT,
    is_published BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);

-- Video Submissions (user recordings)
CREATE TABLE video_submissions (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    user_id BIGINT NOT NULL,
    prompt_id BIGINT NOT NULL,
    video_url VARCHAR(500),
    transcript NVARCHAR(MAX),
    score_pronunciation INT,
    score_grammar INT,
    score_vocabulary INT,
    score_fluency INT,
    score_total INT,
    feedback NVARCHAR(MAX),
    submitted_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT fk_video_submissions_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_video_submissions_prompt FOREIGN KEY (prompt_id) REFERENCES video_speaking_prompts(id) ON DELETE CASCADE
);

-- Payment Transactions (SePay webhook)
CREATE TABLE payment_transactions (
    id BIGINT PRIMARY KEY IDENTITY(1,1),
    transaction_id VARCHAR(100) UNIQUE,
    user_id BIGINT,
    order_code VARCHAR(50),
    amount DECIMAL(18,0),
    gateway VARCHAR(50),
    content NVARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING',
    plan_type VARCHAR(20),
    premium_expiry DATE,
    created_at DATETIME2 DEFAULT GETDATE(),
    webhook_raw NVARCHAR(MAX),
    CONSTRAINT fk_payment_transactions_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

-- Add premium columns to users table
ALTER TABLE users ADD is_premium BIT DEFAULT 0;
ALTER TABLE users ADD premium_expiry DATE;

-- Indexes
CREATE INDEX idx_video_submissions_user ON video_submissions(user_id);
CREATE INDEX idx_video_submissions_prompt ON video_submissions(prompt_id);
CREATE INDEX idx_payment_transactions_order ON payment_transactions(order_code);
CREATE INDEX idx_payment_transactions_user ON payment_transactions(user_id);

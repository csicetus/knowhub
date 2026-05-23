-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 文档分块向量表
-- 这是 RAG 的核心表，每一行是一个文档分块和它对应的向量
CREATE TABLE IF NOT EXISTS document_chunk (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(768) NOT NULL,
    chunk_index INT NOT NULL,
    created_time TIMESTAMP NOT NULL DEFAULT NOW()
    );

-- ========== Column Comments ==========
COMMENT ON COLUMN document_chunk.document_id IS '对应 MySQL document 表的 id';
COMMENT ON COLUMN document_chunk.knowledge_base_id IS '知识库ID（多租户隔离）';
COMMENT ON COLUMN document_chunk.user_id IS '租户ID';
COMMENT ON COLUMN document_chunk.content IS '文本分块内容';
COMMENT ON COLUMN document_chunk.embedding IS 'DeepSeek embedding 向量';
COMMENT ON COLUMN document_chunk.chunk_index IS '文档中的分块序号';

-- 向量检索索引，ivfflat 是近似最近邻算法
-- lists=100 是经验值，文档量大时调大
CREATE INDEX IF NOT EXISTS idx_chunk_embedding
    ON document_chunk
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- 普通索引，按知识库查询时用
CREATE INDEX IF NOT EXISTS idx_chunk_kb_id
    ON document_chunk (knowledge_base_id);

CREATE INDEX IF NOT EXISTS idx_chunk_doc_id
    ON document_chunk (document_id);
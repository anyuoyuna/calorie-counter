-- Включаем поддержку векторов (pgvector)
CREATE EXTENSION IF NOT EXISTS vector;

-- 1. Таблица пользователей
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT UNIQUE NOT NULL,
    display_name VARCHAR(255),
    timezone VARCHAR(50) DEFAULT 'Asia/Bangkok',
    daily_calorie_goal INTEGER DEFAULT 2000,
    daily_protein_goal DOUBLE PRECISION,
    daily_fat_goal DOUBLE PRECISION,
    daily_carbs_goal DOUBLE PRECISION,
    daily_fiber_goal DOUBLE PRECISION
);

-- 2. Справочник продуктов
CREATE TABLE food_items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    calories DOUBLE PRECISION DEFAULT 0,
    protein DOUBLE PRECISION DEFAULT 0,
    fat DOUBLE PRECISION DEFAULT 0,
    carbs DOUBLE PRECISION DEFAULT 0,
    fiber DOUBLE PRECISION DEFAULT 0,
    source VARCHAR(50),
    embedding vector(768) -- Размерность для nomic-embed-text
);

-- 3. Записи о приемах пищи
CREATE TABLE meal_entries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    food_item_id BIGINT REFERENCES food_items(id),
    food_name VARCHAR(255),
    grams DOUBLE PRECISION,
    eaten_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Активность (тренировки)
CREATE TABLE activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    activity_date DATE NOT NULL,
    activity_type VARCHAR(255),
    duration_minutes INTEGER,
    estimated_calories_burned INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Траты (финансы)
CREATE TABLE expenses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    date DATE NOT NULL,
    category VARCHAR(50),
    description TEXT,
    amount DECIMAL(19, 2),
    type VARCHAR(1),
    external_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Лог веса
CREATE TABLE weight_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    logged_at DATE NOT NULL,
    weight_kg DOUBLE PRECISION,
    body_fat_percent DOUBLE PRECISION,
    muscle_weight DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. Профиль пользователя (связь 1:1 с users)
CREATE TABLE user_profile (
    user_id BIGINT PRIMARY KEY REFERENCES users(id),
    gender VARCHAR(10),
    birth_date DATE,
    height_cm DOUBLE PRECISION,
    goal_type VARCHAR(50),
    activity_level VARCHAR(50),
    target_weight_kg DOUBLE PRECISION
);
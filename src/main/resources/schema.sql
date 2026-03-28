CREATE TABLE reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(100),
    cat_name VARCHAR(100) NOT NULL,
    stay_days VARCHAR(50),
    room_type VARCHAR(100),
    note_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
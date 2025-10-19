CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(20),
    password VARCHAR(255) NOT NULL,
    aadhar VARCHAR(20),
    bank VARCHAR(100),
    role VARCHAR(20)
);

CREATE TABLE accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_name VARCHAR(100),
    account_number VARCHAR(50) NOT NULL UNIQUE,
    account_type VARCHAR(50),
    atm_card_number VARCHAR(50),
    balance DOUBLE,
    pin VARCHAR(255),
    cvv VARCHAR(10),
    user_id BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(20),
    amount DOUBLE,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    account_id BIGINT,
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE TABLE banks (
    id VARCHAR(50) PRIMARY KEY,
    full_name VARCHAR(150),
    symbol_url VARCHAR(255)
);

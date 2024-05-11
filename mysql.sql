-- Users
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(150),
    name VARCHAR(150),
    phone VARCHAR(15),
    token VARCHAR(32),
    settings TEXT,
    origin INT,
    permissions INT
);

-- Tracking hits on objects (articles, users, images, etc)
CREATE TABLE pings (
    user VARCHAR(36),
    object VARCHAR(36),
    origin INT,
    type VARCHAR(6)
);

-- For system messages, through Twilio/SMS
CREATE TABLE systemsms (
    id VARCHAR(36),
    type VARCHAR(50),
    content TEXT,
    is_read boolean,
    origin INT
);

-- Parser parse tracker/history
CREATE TABLE parsed (
    user VARCHAR(36),
    uri TEXT,
    origin INT
);

-- Authentication tokens
CREATE TABLE authentications (
    user VARCHAR(36),
    token VARCHAR(32),
    refresh VARCHAR(32),
    origin INT,
    expiration INT
)

-- Receipts for payments and invoices, not for backend software
CREATE TABLE receipts (
    user VARCHAR(36),
    customer VARCHAR(36),
    invoice  VARCHAR(36),
    total INT,
    origin INT
);

-- Domains are like organizations.
CREATE TABLE domains (
    id VARCHAR(36) PRIMARY KEY,
    owner VARCHAR(36),
    name VARCHAR(150),
    alt VARCHAR(150),
    permissions INT,
    origin INT,
    FOREIGN KEY (owner) REFERENCES users (id)
)

-- Memberships to domains
CREATE TABLE memberships (
    user VARCHAR(36),
    domain VARCHAR(36),
    origin INT,
    permissions INT,
    FOREIGN KEY (user) REFERENCES users (id),
    FOREIGN KEY (domain) REFERENCES domains (id)
)

-- Collections are sets of articles in a domain //TODO implement
CREATE TABLE collections (
    id VARCHAR(36),
    domain VARCHAR(36),
    name VARCHAR(150),
    alt VARCHAR(150),
    origin INT,
    FOREIGN KEY (domain) REFERENCES domains (id)
)

-- For when user requests actions like
CREATE TABLE handles (user VARCHAR(36), connector VARCHAR(150), query TEXT, endpoint TEXT, origin INT, resolved INT, FOREIGN KEY (user) REFERENCES users(id))


-- //TODO Implement
CREATE TABLE timelines (object VARCHAR(36), action VARCHAR(10), notes TEXT, origin INT, FOREIGN KEY (user) REFERENCES users(id))
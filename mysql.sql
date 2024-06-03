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

CREATE TABLE shortcuts (
    user VARCHAR(36),
    object TEXT,
    name TEXT,
    icon TEXT,
    origin INT,
    FOREIGN KEY (user) REFERENCES users (id)
)

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

-- Domains are like organizations.
CREATE TABLE domains (
    id VARCHAR(36) PRIMARY KEY,
    owner VARCHAR(36),
    name VARCHAR(150),
    alias VARCHAR(150),
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

-- Indexes are sets of articles in a domain //TODO implement
CREATE TABLE indexes (
    id VARCHAR(36),
    domain VARCHAR(36),
    name VARCHAR(150),
    alias VARCHAR(150),
    description VARCHAR(150), --TODO
    origin INT,
    FOREIGN KEY (domain) REFERENCES domains (id)
)

-- For timelines
CREATE TABLE events (
    user VARCHAR(36),
    object VARCHAR(36),
    action VARCHAR(50),
    notes TEXT,
    origin INT,
    FOREIGN KEY (user) REFERENCES users (id)
)

--TODO
-- For social use
CREATE TABLE relationship (
    one VARCHAR(36),
    two VARCHAR(36),
    description TEXT,
    origin INT,
    FOREIGN KEY (one) REFERENCES users (id)
    FOREIGN KEY (two) REFERENCES users (id)
)

--Front end use only
CREATE TABLE themes (
    user VARCHAR(36),
    origin INT,
    json TEXT,
    FOREIGN KEY (user) REFERENCES users(id)
)
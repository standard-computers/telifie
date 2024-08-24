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

CREATE TABLE pings (
    user VARCHAR(36),
    object VARCHAR(36),
    origin INT,
    type VARCHAR(6)
);

CREATE TABLE systemsms (
    id VARCHAR(36),
    type VARCHAR(50),
    content TEXT,
    is_read boolean,
    origin INT
);

CREATE TABLE authentications (
    user VARCHAR(36),
    token VARCHAR(32),
    refresh VARCHAR(32),
    origin INT,
    expiration INT
)

CREATE TABLE domains (
    id VARCHAR(36) PRIMARY KEY,
    owner VARCHAR(36),
    name VARCHAR(150),
    alias VARCHAR(150),
    permissions INT,
    origin INT,
    FOREIGN KEY (owner) REFERENCES users (id)
)

CREATE TABLE memberships (
    user VARCHAR(36),
    domain VARCHAR(36),
    origin INT,
    permissions INT,
    FOREIGN KEY (user) REFERENCES users (id),
    FOREIGN KEY (domain) REFERENCES domains (id)
)

CREATE TABLE indexes (
    id VARCHAR(36),
    domain VARCHAR(36),
    name VARCHAR(150),
    alias VARCHAR(150),
    origin INT,
    FOREIGN KEY (domain) REFERENCES domains (id)
)

CREATE TABLE events (
    user VARCHAR(36),
    object VARCHAR(36),
    action VARCHAR(50),
    notes TEXT,
    origin INT,
    FOREIGN KEY (user) REFERENCES users (id)
)

CREATE TABLE themes (
    user VARCHAR(36),
    name VARCHAR(150),
    origin INT,
    json TEXT,
    FOREIGN KEY (user) REFERENCES users(id)
);

CREATE TABLE shortcuts (
    user VARCHAR(36),
    object TEXT,
    name TEXT,
    icon TEXT,
    origin INT,
    FOREIGN KEY (user) REFERENCES users (id)
);
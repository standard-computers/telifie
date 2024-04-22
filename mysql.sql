-- Tracking hits on objects (articles, users, images, etc)
CREATE TABLE pings (user VARCHAR(36), object VARCHAR(36), origin INT(13), type VARCHAR(6));

-- For system messages, through Twilio/SMS
CREATE TABLE systemsms (id VARCHAR(36), type VARCHAR(50), content TEXT, is_read boolean, origin INT(13));

-- Parser Queue
CREATE TABLE queue (user VARCHAR(36), uri TEXT, origin INT(20));

-- Parser parse tracker/history
CREATE TABLE parsed (user VARCHAR(36), uri TEXT, origin INT(20));

-- Authentications //TODO needs migrated
CREATE TABLE authentications (user VARCHAR(36), token VARCHAR(32), refresh VARCHAR(32), origin INT(13), expiration INT(13))

-- Receipts for payments and invoices
CREATE TABLE receipts (user VARCHAR(36), customer VARCHAR(36), invoice VARCHAR(36), total INT(10), origin INT(13));


-- For when user requests actions like
CREATE TABLE handles (user VARCHAR(36), connector VARCHAR(150), query TEXT, endpoint TEXT, origin INT(13), resolved INT(13))


-- quickresponse
CREATE TABLE history (user VARCHAR(36), session VARCHAR(36), query TEXT, response TEXT, origin INT(20));

-- Potential for users. Better security and access. MongoDB is just documents
CREATE TABLE users (id VARCHAR(36), name VARCHAR(150), email VARCHAR(150), phone VARCHAR(14), origin INT(13), access INT(2))
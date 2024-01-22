CREATE DATABASE telifie;
CREATE TABLE messages (user VARCHAR(36), sender VARCHAR(12), origin INT(20), body TEXT, reply TEXT);
CREATE TABLE pings (user VARCHAR(36), object VARCHAR(36), origin INT(20));
CREATE TABLE logs (type VARCHAR(50), content TEXT, origin INT(20), timestamp VARCHAR(20));
CREATE TABLE systemsms (id VARCHAR(36), type VARCHAR(50), content TEXT, origin INT(20));
CREATE TABLE receipts (user VARCHAR(36), object VARCHAR(36), origin INT(20));
CREATE TABLE queue (user VARCHAR(36), uri TEXT, origin INT(20));

CREATE TABLE users (id VARCHAR(36), name VARCHAR(150), email VARCHAR(150), phone VARCHAR(14), origin INT(13), access INT(2))
CREATE TABLE authentications (user VARCHAR(36), token VARCHAR(32), refresh VARCHAR(32), origin INT(13), expiration INT(13))
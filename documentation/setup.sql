CREATE DATABASE telifie;
CREATE TABLE messages (user VARCHAR(36), sender VARCHAR(12), origin INT(20), body TEXT, reply TEXT);
CREATE TABLE pings (user VARCHAR(36), object VARCHAR(36), origin INT(20));
CREATE TABLE logs (type VARCHAR(50), content TEXT, origin INT(20), timestamp VARCHAR(20));
CREATE TABLE systemsms (id VARCHAR(36), type VARCHAR(50), content TEXT, origin INT(20));
CREATE TABLE receipts (user VARCHAR(36), object VARCHAR(36), origin INT(20));
CREATE TABLE queue (user VARCHAR(36), uri TEXT, origin INT(20));

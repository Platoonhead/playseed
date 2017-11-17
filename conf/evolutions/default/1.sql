# --- !Ups
CREATE TABLE `user_profile` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `first_name`   VARCHAR(254) NOT NULL,
  `last_name`    VARCHAR(254) NOT NULL,
  `email`        VARCHAR(254) NOT NULL,
  `dob`          DATETIME     NOT NULL,
  `created`      DATETIME     NOT NULL,
  `suspended`    BOOLEAN      NOT NULL DEFAULT 0
);

CREATE TABLE `events` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `profile_id` BIGINT       NOT NULL,
  `event_type` VARCHAR(254) NOT NULL,
  `remote_ip`  VARCHAR(254),
  `invoked_at` TIMESTAMP    NOT NULL,
  `suspended`  BOOLEAN      NOT NULL DEFAULT 0
);

CREATE TABLE `p3_user_info` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `p3_user_id`   VARCHAR(254) NOT NULL,
  `email`        VARCHAR(254) NOT NULL,
  `created_date` TIMESTAMP    NOT NULL
);

CREATE TABLE `user_blocklist` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `block_email`       VARCHAR(254) NOT NULL,
  `blocked_date_time` TIMESTAMP    NOT NULL
);

CREATE TABLE `receipts` (
  `id`                    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `snap3_receipt_id`      VARCHAR(254) NOT NULL,
  `profile_id`            BIGINT       NOT NULL,
  `receipt_day`           BIGINT       NOT NULL,
  `receipt_month`         BIGINT       NOT NULL,
  `receipt_year`          BIGINT       NOT NULL,
  `email`                 VARCHAR(254) NOT NULL,
  `store_name`            VARCHAR(254),
  `p3user_id`             VARCHAR(254) NOT NULL,
  `client_id`             VARCHAR(254) NOT NULL,
  `image_name`            VARCHAR(254) NOT NULL,
  `status`                VARCHAR(254),
  `postal_code`           VARCHAR(254),
  `amount`                DOUBLE,
  `qualifying_amount`     DOUBLE,
  `request_sent_date`     DATETIME     NOT NULL,
  `callback_receive_date` BIGINT       NOT NULL,
  `date_in_receipt`       VARCHAR(254) NOT NULL,
  `products`              TEXT         NOT NULL,
  `point_sent`            BOOLEAN      NOT NULL
);

CREATE TABLE `user_support` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `name`         VARCHAR(254) NOT NULL,
  `email`        VARCHAR(254) NOT NULL,
  `message`      VARCHAR(254) NOT NULL,
  `created_date` TIMESTAMP    NOT NULL
);

CREATE TABLE `associates` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `profile_id`  BIGINT       NOT NULL,
  `p3_user_id`  VARCHAR(254) NOT NULL,
  `facebook_id` VARCHAR(254),
  `twitter_id`  VARCHAR(254),
  `email_pass`  VARCHAR(254)
);


ALTER TABLE associates ADD CONSTRAINT profile_id_fk FOREIGN KEY (profile_id) REFERENCES user_profile (id);

ALTER TABLE user_profile ADD receive_email BOOLEAN DEFAULT FALSE;

INSERT INTO user_profile (id, first_name, last_name, email, dob, created, suspended)
VALUES
  (1, 'test', 'test123', 'test@test.com', '2017-05-16 11:51:09', '2017-05-16 11:51:09', 0);

INSERT INTO receipts (id ,snap3_receipt_id, profile_id, receipt_day, receipt_month, receipt_year, email, store_name, p3user_id, client_id, image_name, status, postal_code, amount,
                      qualifying_amount, request_sent_date, callback_receive_date, date_in_receipt, products, point_sent)
VALUES
  (1, 'receiptId', 1, 12, 2, 2017, 'test@example.com', NULL, 'user123', 'client123', 'imageName', NULL, NULL, NULL, Null,
   '2017-05-16 11:51:09',
   1494915729306, '', 'Products', false);

# --- !Downs

DROP TABLE user_profile;
DROP TABLE events;
DROP TABLE p3_user_info;
DROP TABLE user_blocklist;
DROP TABLE receipts;
DROP TABLE user_support;
DROP TABLE `associates`;

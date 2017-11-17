# --- !Ups
DROP TABLE `user_profile`;
DROP TABLE `p3_user_info`;
DROP TABLE `events`;
DROP TABLE `user_blocklist`;
DROP TABLE `receipts`;
DROP TABLE `user_support`;

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

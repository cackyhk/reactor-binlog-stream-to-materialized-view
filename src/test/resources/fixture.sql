CREATE TABLE `transfer`
(
    `id`             bigint(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL AUTO_INCREMENT,
    `account_number` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    `amount`         bigint(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    `created_at`     datetime CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_bin;

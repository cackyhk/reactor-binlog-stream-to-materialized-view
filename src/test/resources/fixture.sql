CREATE TABLE `transfer`
(
    `id`             bigint(20)                                            NOT NULL AUTO_INCREMENT,
    `account_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    `amount`         bigint(20)                                            NOT NULL,
    `created_at`     datetime                                              NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_bin;


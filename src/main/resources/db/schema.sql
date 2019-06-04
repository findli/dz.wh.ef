create schema if not exists log_db collate utf8mb4_general_ci;

create table if not exists filtered
(
	id int auto_increment
		primary key,
	ip varchar(255) null,
	comment varchar(255) null,
	constraint filtered_ip_comment_uindex
		unique (ip, comment)
);

create table if not exists log
(
	id int auto_increment
		primary key,
	ip varchar(255) null,
	ts timestamp(3) null,
	constraint log_ip_ts_uindex
		unique (ip, ts)
);

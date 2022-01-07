drop table if exists test;
create table test(id SERIAL primary key,
  username varchar(255),
  my_password varchar(128),
  password varchar(128),
  last_update_time timestamp
);

ALTER TABLE test OWNER TO test_multi_db;

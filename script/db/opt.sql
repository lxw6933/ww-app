select * from table_name where id > xxxx limit 20;

select * from table_name where id > (select id from table_name limit 1 offset 100) limit 20;

select * from table_name
         where time, id > (select time, id from table_name where order by time desc, id desc limit 1 offset 100)
         order by time desc, id desc limit 20;


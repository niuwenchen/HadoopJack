## Mapreduce中的计算模型

### Reduce Sid Join
连接操作 Join，一般左连接，右连接。

Reduce Side Join  : 韦恩图， 左边的都有，右边的是只有交集

    Select * from TableA LeFt join  TableB On tableA.name = tableB.name
    
Map  Side Join: 
    
    drop table if exists bank_brand_base;
    create tablebank_brand_base
    row format delimited fields terminated by '\t'
    as 
    select * from log_t l join bank_query_t q on (l.query=q.query)
    log_t中大量的数据，bank_query_t 中少量数据，如果采用左连接，数据在Reduce阶段会倾斜。
    
    Select /*+mapjoin(bank_query_t)*/ * from log_t  l join bank_query q on (l.query = q.query)
    用mapjoin修饰的小表bank_query_t 会被读入内存，在Map阶段就会将log_t中的数据和内存中的
    bank_query_t 的数据做Join操作，减少了Map阶段的输出，即减轻了Reduce阶段的输入，提高效率。
    
    
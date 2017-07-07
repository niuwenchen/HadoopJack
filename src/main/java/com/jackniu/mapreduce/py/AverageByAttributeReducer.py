#encoding:utf-8
#@Time : 2017/7/7 10:48
#@Author : JackNiu

import sys
last_key=None
sum=0
count =0

for line in sys.stdin:
    (key,value) = line.split('\t')
    if last_key and last_key != key:
        print(last_key +"\t"+ str(sum/count))
        (sum,count)=(0.0,0)
    last_key=key
    sum += float(value)
    count +=1
print(last_key +"\t"+ str(sum/count))

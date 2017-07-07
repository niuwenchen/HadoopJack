#encoding:utf-8
#@Time : 2017/7/7 11:01
#@Author : JackNiu

import sys
index = int(sys.argv[1])

for line in sys.stdin:
    fields = line.split(",")
    print("LongValueSum:"+fields[index]+"\t"+"1")


'''
hadoop jar /opt/hadoop/share/hadoop/tools/lib/hadoop-streaming-2.6.0.jar  -input /jackniu/apat63_99.txt -output /jackniu/output -mapper 'python AttributeCount.py 1' -reducer aggregate  -file AttributeCount.py

'''
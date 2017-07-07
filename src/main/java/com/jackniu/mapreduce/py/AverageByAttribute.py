#encoding:utf-8
#@Time : 2017/7/7 10:38
#@Author : JackNiu

import sys

for line in sys.stdin:
    fields = line.split(',')
    if (fields[8] and fields[8].isdigit()):
        print(fields[4][1:-1] + "\t"+fields[8])

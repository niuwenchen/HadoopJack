#encoding:utf-8
#@Time : 2017/7/6 10:04
#@Author : JackNiu

import sys

index= int(sys.argv[1])
max=0
for line in sys.stdin:
    fileds= line.strip().split(",")
    if fileds[index].isdigit():
        val = int(fileds[index])
        if (val > max):
            max = val

print(max)

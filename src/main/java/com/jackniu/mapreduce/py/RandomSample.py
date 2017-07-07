#encoding:utf-8
#@Time : 2017/7/6 9:21
#@Author : JackNiu

import sys,random
for line in sys.stdin:
    if (random.randint(1,100) <= int(sys.argv[1])):
        print(line.strip())
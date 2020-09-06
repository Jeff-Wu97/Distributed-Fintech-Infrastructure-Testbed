# -*- coding: utf-8 -*-
"""
Created on Sun Aug 23 00:33:07 2020

@author: wuzhe
"""
from pathlib import Path

data_folder = Path(r"C:\Users\wuzhe\Desktop\TestData\pbft_norm\time")#\TestData\pbft_norm\size")#\TestData\pbft_vc\freq")#C:\Users\wuzhe\Desktop")#

file_to_open = data_folder / "1000tx_pbft_norm_1kb_1500ms_log.txt"
fp = open(file_to_open)


total_delay=0
tx_num=0
vc_num=0
piece=0
for line in fp:
    if(line.find("Test End")!=-1):
        print(line)
        break
    if(line.find("tx =")!=-1):#每个交易记录
        line=line.split(",")
        if(line[0].find("Data Size")!=-1):
            print(line)
            continue;
        tx=line[0].split(" ")
        tx_num=tx_num+1
        delay=line[1].split(" ")
        delay=delay[-1].rstrip("\n")
        total_delay=total_delay+int(delay)
        if(int(delay)>8000):
            vc_num=vc_num+1
#        total_time=total_time+float(delay)
print("ave_delay = %f"%(total_delay/tx_num))
print(vc_num)
#print("tx_num = %d, vc_num = %d, takes up %f" %(tx_num,vc_num,(float(vc_num)/float(tx_num))))
#print("total_delay = %f"%total_time)
fp.close()
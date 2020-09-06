# -*- coding: utf-8 -*-
"""
Created on Mon Aug 31 11:07:02 2020

@author: wuzhe
"""


from pylab import *

# axes = gca()
# axes.set_xlim(0,4)
# axes.set_ylim(0,3)
# axes.set_xticklabels([])
# axes.set_yticklabels([])

# n = 256
# X = np.linspace(-np.pi,np.pi,n,endpoint=True)
# Y = np.sin(2*X)

# plot (X, Y+1, color='blue', alpha=1.00)
# plot (X, Y-1, color='blue', alpha=1.00)
# show()


from matplotlib import pyplot as plt
import numpy as np 
from matplotlib import pyplot as plt 
from scipy.optimize import curve_fit
from matplotlib.legend_handler import HandlerLine2D

def func(x, a, b,c):
    return a*np.sqrt(x)*(b*np.square(x)+c)

plt.rcParams['figure.figsize'] = (6.0, 4.0)#设置图片大小
plt.rcParams['savefig.dpi'] = 300 #保存的图片像素
plt.rcParams['figure.dpi'] = 300 #绘制图片的分辨率

x_vals = np.linspace(0, 8, 400, endpoint=False)#初始化一系列x
# popt, pcov = curve_fit(func, x, y)
# a = popt[0] 
# b = popt[1]
# c = popt[2]
#yvals = func(x_vals,a,b,c)

x1 = [1.000,	1.500,	2.000,	2.500,	3.000,	3.500,	4.000,	4.500,	5.000]
x1=np.array(x1)
y1 = [2.510,	2.407,	2.449,	2.420,	2.408,	2.484,	2.409,	2.435,	2.430]
y1=np.array(y1) 
f1 = np.polyfit(x1, y1, 6)
yvals_1=np.polyval(f1, x1)

x2 =  [1.000,	1.500,	2.000,	2.500,	3.000,	3.500,	4.000,	4.500,	5.000]
x2=np.array(x2)
y2 = [1.252,	1.179,	1.176,	1.164,	1.182,	1.176,	1.180,	1.167,	1.167]
y2=np.array(y2) 
f2 = np.polyfit(x2, y2, 8)
yvals_2=np.polyval(f2, x2)#_vals)



# xlim(1.4,5.1)#设置x轴大小
# xticks(np.linspace(1.5,5,8,endpoint=True))#设置刻度
# ylim(0.5,3.5)
# yticks(np.linspace(0.5,3.5,13,endpoint=True))
# xlim(1.4,5.1)#设置x轴大小
# xticks(np.linspace(1.5,5,8,endpoint=True))#设置刻度
# ylim(0.1,0.75)
# yticks(np.linspace(0.1,0.75,14,endpoint=True))
xlim(0.9,5.1)#设置x轴大小
xticks(np.linspace(1,5,9,endpoint=True))#设置刻度
ylim(0.5,3.5)
yticks(np.linspace(0.5,3.5,7,endpoint=True))

#plt.title("") #图片名称
plt.xlabel("Time interval (s)")#x轴标签 
plt.ylabel("Latency (s)") #y轴标签
#plt.plot(x1,y1,".b") #绘制xy
#l1=plt.scatter(x1,y1,marker='o',c='',edgecolors='b')
l1,=plt.plot(x1,yvals_1,"b",linewidth=0.75,marker="x")

#l2=plt.scatter(x2,y2,marker='^',c='',edgecolors='r')
l2,=plt.plot(x2,yvals_2,"r",linewidth=0.75,marker="*")
grid(color='b', linestyle='--', linewidth=0.1)#设置网格
plt.legend(handles=[l1,l2],labels=['PBFT','Zyzzyva'],loc='best')
plt.show()
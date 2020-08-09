# -*- coding: utf-8 -*-
"""
Created on Tue Jul 14 09:43:44 2020

@author: wuzhe
"""

import telnetlib
vm_num=4;
host_ip=b"localhost"
vm_port=[5554,5556,5558,5560]
origin_listen_port=[8000,8002,8004,8006]
redir_listen_port=[8001,8003,8005,8007]
emulator_auth=b"auth 1TTxm+dW0V/1oVAV\n"

def virtual_redir(host_ip,vm_port,listen_port,redir_listen_port,emulator_auth):
    tn=telnetlib.Telnet(host_ip,vm_port)
    tn.set_debuglevel(2)
    tn.read_until(b'OK')
    tn.write(emulator_auth)
    tn.read_until(b'OK')
    add_command="redir add tcp:"+str(listen_port)+":"+str(redir_listen_port)+"\n"
    tn.write(add_command.encode('utf-8'))
    tn.write(b"\n")
    tn.close();
    

for i in range(0,len(vm_port)):
    try:
        virtual_redir(host_ip, vm_port[i], origin_listen_port[i], redir_listen_port[i], emulator_auth)
    except ConnectionRefusedError:
        print("ERROR: target compter denied : "+host_ip+" "+str(vm_port[i]))

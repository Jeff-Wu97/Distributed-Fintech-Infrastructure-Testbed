# -*- coding: utf-8 -*-
"""
Created on Tue Jul 14 09:43:44 2020

@author: wuzhe
"""

import telnetlib

#def virtual_redir(host_ip,vm_port,listen_port,redir_listen_port):
tn=telnetlib.Telnet("localhost",5554)
tn.set_debuglevel(2)
tn.read_until(b'OK')
tn.write(b"auth 1TTxm+dW0V/1oVAV\n")
tn.read_until(b'OK')
tn.write(b"redir add tcp:8000:8001\n")
tn.write(b"\n")
tn.close();

tn=telnetlib.Telnet("localhost",5556)
tn.set_debuglevel(2)
tn.read_until(b'OK')
tn.write(b"auth 1TTxm+dW0V/1oVAV\n")
tn.read_until(b'OK')
tn.write(b"redir add tcp:8002:8003\n")
tn.write(b"\n")
tn.close();

tn=telnetlib.Telnet("localhost",5558)
tn.set_debuglevel(2)
tn.read_until(b'OK')
tn.write(b"auth 1TTxm+dW0V/1oVAV\n")
tn.read_until(b'OK')
tn.write(b"redir add tcp:8004:8005\n")
tn.write(b"\n")
tn.close();

tn=telnetlib.Telnet("localhost",5560)
tn.set_debuglevel(2)
tn.read_until(b'OK')
tn.write(b"auth 1TTxm+dW0V/1oVAV\n")
tn.read_until(b'OK')
tn.write(b"redir add tcp:8006:8007\n")
tn.write(b"\n")
tn.close();
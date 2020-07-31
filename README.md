# Distributed-Fintech-Infrastructure-Testbed
## Update: View-Change Protocol
The View-Change protocol has been implemented in the dir *PBFT_v2*, which is an Andriod demo. 


To interconnecting emulator instances, a script code *tel_redir.py* has been uploaded to redirect ports, so that you needn't start telnet session manually anymore.


Thank you!

## Update: ECSchnorr Signature
The *Schnorr.java* contains a demo for ECSchnorr signature. It creates EC keypairs, generates Schnorr signature, and verify the message with signature. The encrypted string in the demo is "HelloZhe" (defined at row 273, main).  

## Mobile PBFT 
Thanks for your visit!

With the advancement of Internet financial technology, the transaction security is drawing more public attention. The consensus algorithms is applied in distributed financial systems to ensure transaction consistency. This ongoing project aims to explore implementation consensus algorithms based on mobile.

The *PBFTDemo* is an Android project of the practical Byzantine fault tolerance algorithm. To run the demo, you'd better to install the Android Studio first. Then import the "PBFTDemo" project directly. If you would like to run the demo on the emulator, you need to create the virtual machine in Android Studio by "Tools" - "AVD Manager". Then, you should redirect the ports. Modify the "port_arr" and "port_send_arr" in "SystemInfo" class of *MainActivity.java* file (*PBFTDemo/app/src/main/java/com/example/commdemo/MainActivity.java*), which contain the listening port and the port redircting to the listening port. After that, please set port redir according to the https://developer.android.com/studio/run/emulator-networking?hl=zh-cn. (A script code to simplify the step is on the way.)

If you have any questions, welcome to contact with me <Z.Wu-28@ed.ac.uk>.

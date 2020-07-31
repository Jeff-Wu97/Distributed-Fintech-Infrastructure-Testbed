package com.example.commdemo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Initialize {
    //read the doc to got the system Info
    static void initializeKey() {
        SystemInfo.privateKey.put(""+0,
                "34c9baac2f4287ab902534d546e9fa8871b7af84c7472105cf59e6c1a737768a");
        SystemInfo.privateKey.put(""+1,
                "c8a5146c3756377342ca427760aba7b43e39b77867a56ca7cae502a37daec45f");
        SystemInfo.privateKey.put(""+2,
                "5ca04cba1d76e80f708b7d51f07949dcbce967454a9e149fd2ac8d1cde96e13f");
        SystemInfo.privateKey.put(""+3,
                "2b2751a7cc33bcb36c9579d5d39accfb737b80baaa075a23d9730f43247605a6");

        SystemInfo.publicKey.put(""+0,
                "0387dea55d0dbbe189124aac011fdc7d236a5c88c6384685c9e46def0c640938b8");
        SystemInfo.publicKey.put(""+1,
                "022f403f05692ea236066f0f1c943a72b7c1d2c3cdf5693a241713577fc5f4673e");
        SystemInfo.publicKey.put(""+2,
                "02916061a21a52865526bdc00715997b3e5370dfb8906bba49f699d78f414bbb25");
        SystemInfo.publicKey.put(""+3,
                "0295b79e2b14594c5e63c46a09d17684324ee4fed8e3a63c8a5686d51705737c62");

        String selfPrivateKeytxt=SystemInfo.privateKey.get(""+SystemInfo.node_id)+",private";
        SystemInfo.key_lib.add(selfPrivateKeytxt);
        String selfPublicKeyTxt=SystemInfo.publicKey.get(""+SystemInfo.node_id)+",KEYof"+SystemInfo.node_id;
        SystemInfo.key_lib.add(selfPublicKeyTxt);
        UtilClass.multicastOther("KEY,"+selfPublicKeyTxt);

        while (true){
            if (SystemInfo.key_lib.size()>=5){
                System.out.println("----- Got all key");
                break;
            }
        }
    }

}

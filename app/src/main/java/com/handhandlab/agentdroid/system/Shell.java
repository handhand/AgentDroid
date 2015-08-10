package com.handhandlab.agentdroid.system;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class Shell {

    /**
     * run command as root
     * @param cmds
     */
	public static void runAsRoot(String[] cmds){
		try {
			Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());            
            for (String tmpCmd : cmds) {
                os.writeBytes(tmpCmd+"\n");
            }           
            os.writeBytes("exit\n");  
            os.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    /**
     * run command as root
     * @param cmds
     */
    public static void runAsRoot(List<String> cmds){
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            for (String tmpCmd : cmds) {
                os.writeBytes(tmpCmd+"\n");
            }
            os.writeBytes("exit\n");
            os.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
	
	public static void test(){
		String[] cmds = {
			//"cat /sdcard/wb.apk > /system/xbin/wb.apk",
			"rm /system/xbin/wb.apk"
		};
		runAsRoot(cmds);
	}

}

package com.allinno.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

import com.allinno.patcher.SmartPatcher;

class StreamGobbler extends Thread {
	InputStream is;
	OutputStream os;

	StreamGobbler(InputStream is, OutputStream os) {
		this.is = is;
		this.os = os;
	}

	public void run() {
		try {
			StringBuffer buf = new StringBuffer();
			PrintWriter pw = os != null ? new PrintWriter(os) : null;
			InputStreamReader isr = new InputStreamReader(is, "GBK");
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null) {
				buf.append(line + '\n');
				if (pw != null) {
					pw.println(line);
				}
				// System.out.println(line);
				// System.out.flush();
			}
			if (pw != null) {
				pw.flush();
				pw.close();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}

public class CmdUtil {
	public static Logger log = Logger.getLogger(CmdUtil.class);

	public static int executeCmd(String strCmd) {
		return executeCmd(strCmd, null);
	}

	public static int executeCmd(String strCmd, OutputStream os) {
		try {
			log.debug("Execute cmd:" + strCmd);
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(strCmd);
			// StreamGobbler gobbler = new StreamGobbler(proc.getInputStream(),
			// os);
			// gobbler.start();

			StringBuffer buf = new StringBuffer();
			PrintWriter pw = os != null ? new PrintWriter(os) : null;
			InputStream is = proc.getInputStream();
			InputStreamReader isr = new InputStreamReader(is, "GBK");
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			log.debug("Git更新信息如下:");
			log.debug("----------------------------------------------");
			while ((line = br.readLine()) != null) {
				buf.append(line + '\n');
				if (pw != null) {
					pw.println(line);
				}
				log.debug(line);
				// System.out.println(line);
				// System.out.flush();
			}
			log.debug("----------------------------------------------");
			if (pw != null) {
				pw.flush();
				pw.close();
			}

			int val = proc.waitFor();
			log.debug("Cmd result:" + val);
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		return 0;
	}

	public static void main(String[] args) throws FileNotFoundException {
		// String strCmd=(new GitLog()).getCmd();
		// System.out.println(strCmd);
		// String str = CmdUtil.executeCmd(strCmd);
		// System.out.print(str);

		FileOutputStream fos = new FileOutputStream("e:\\out.txt");
		//String strCmd = (new GitLog()).getCmd();
		//CmdUtil.executeCmd(strCmd, fos);
	}
}

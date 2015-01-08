package com.allinno.patcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.allinno.util.CmdUtil;
import com.allinno.util.GitLog;
import com.allinno.util.SFTPUtil;
import com.allinno.util.ShellUtil;

/**
 * 
 * @author json
 * @version 1.1
 * 
 */

public class SmartPatcher {
	private String filePath;
	private String proPath;
	private String serverPath;
	private String serverProPath;
	private String target;

	private String ip;
	private int port;
	private String user;
	private String password;

	private ArrayList<String> delList;
	private String isUpload;
	private String isRestart;

	public SmartPatcher() {
		Locale locale = Locale.getDefault();
		ResourceBundle localResource = ResourceBundle.getBundle("application",
				locale);
		this.filePath = localResource.getString("patcher.filePath");
		this.target = localResource.getString("patcher.target");

		this.proPath = localResource.getString("patcher.proPath");
		this.serverPath = localResource.getString("patcher.serverPath");
		this.serverProPath = localResource.getString("patcher.serverProPath");

		this.ip = localResource.getString("shell.ip");
		this.port = Integer.parseInt(localResource.getString("shell.port"));
		this.user = localResource.getString("shell.user");
		this.password = localResource.getString("shell.password");

		this.isUpload = localResource.getString("shell.isUpload");
		this.isRestart = localResource.getString("shell.isRestart");
		delList = new ArrayList<String>();
	}

	public void createPatch() throws Exception {
		File fileP = new File(this.filePath);
		if (!fileP.exists()) {
			fileP.createNewFile();
		}
		FileOutputStream fos = new FileOutputStream(this.filePath);
		String strCmd = (new GitLog()).getCmd();
		CmdUtil.executeCmd(strCmd, fos);
	}

	private void donePatch(File srcFile, File destFile, String status)
			throws IOException {
		if ("D".equals(status)) {
			System.out.println("清除文件：" + srcFile.getPath());

			// 记录服务器要删除的文件
			String del = srcFile.getPath().replace("\\", "/");
			del = del.replace(proPath, serverProPath);
			delList.add(del);

			if (!srcFile.exists()) {
				System.out.println("清除文件清除成功！！！");
				return;
			} else {
				FileUtils.deleteDirectory(srcFile);
				System.out.println("清除文件清除成功！！！");
			}
		} else {
			System.out.println("原文件：" + srcFile.getPath());
			System.out.println("目标文件：" + destFile.getPath());

			if (!srcFile.exists()) {
				System.err.println("原文件不存在！！！");
				return;
			}
			FileUtils.copyFile(srcFile, destFile);
		}
	}

	public void upload() {
		if (!"Y".equals(this.isUpload)) {
			return;
		}
		System.out.println("\r\n \r\n \r\n开始上传补丁到服务器");
		System.out
				.println("-----------------------------------------------------------");
		SFTPUtil sftp = new SFTPUtil(ip, port, user, password);
		sftp.uploadDirectory(serverProPath, target);// 更新文件
		for (String del : delList) {
			sftp.delete(del);
			System.out.println("删除服务器文件:" + del);
		}

	}

	public void doPatch(String srcName, String destName, String status)
			throws IOException {
		File baseDir = new File(this.proPath);
		File patchDir = new File(this.target);

		File srcFile = new File(baseDir, srcName);
		File destFile = new File(patchDir, destName);

		donePatch(srcFile, destFile, status);

		if (srcName.endsWith(".class")) {
			File dir = new File(srcFile.getParent());
			String name = srcFile.getName();
			String prefix = name.substring(0, name.length() - 6) + "$";
			for (File src : dir.listFiles()) {
				if (src.getName().indexOf(prefix) == 0) {
					File dest = new File(new File(destFile.getParent()),
							src.getName());
					donePatch(src, dest, status);
				}
			}
		}
	}

	public boolean validate() {
		File patchFiles = new File(this.filePath);
		if (!patchFiles.exists()) {
			System.err.println("补丁列表文件不存在！！！");
			return false;
		}
		if (!patchFiles.isFile()) {
			System.err.println("补丁列表文件不是一件文件！！！");
			return false;
		}
		File baseDir = new File(this.proPath);
		if (!baseDir.exists()) {
			System.err.println("基础目录不存在！！！");
			return false;
		}
		if (!baseDir.isDirectory()) {
			System.err.println("基础目录不是一个目录！！！");
			return false;
		}
		File patchDir = new File(this.target);
		if (patchDir.exists()) {
			System.out.println("补丁目录已存在，执行删除操作。。。");
			patchDir.delete();
		}
		System.out.println("执行创建补丁目录操作。。。");
		patchDir.mkdirs();
		return true;
	}

	public void patch() throws FileNotFoundException {
		if (!validate()) {
			return;
		}
		File patchFiles = new File(this.filePath);
		BufferedReader br = new BufferedReader(new FileReader(patchFiles));
		String line = null;
		String fileName = null;
		String status = null;
		try {
			System.out.println("\r\n开始处理补丁列表文件。。。");
			while ((line = br.readLine()) != null) {
				if (line.trim().length() == 0) {
					System.out.println("\r\n空行，跳过。。。");
					continue;
				}
				if (line.trim().startsWith("[Log]")) {
					System.out.println("\r\n" + line.trim());
					System.out
							.println("------------------------------------------------------");
					continue;
				}

				line = line.trim().replaceAll("\\\\", "/");
				System.out.println("\r\n文件：" + line);

				fileName = line.substring(2);
				status = line.substring(0, 1);

				String suffix = fileName
						.substring(fileName.lastIndexOf(".") + 1);
				System.out.println("后缀：" + suffix);
				String srcName = "";
				String destName = "";

				// src开始 正常项目
				if (fileName.startsWith("src")) {
					String append = suffix.equals("java") ? "class" : suffix;
					destName = "WEB-INF/classes/"
							+ fileName.substring(fileName.indexOf("/"),
									fileName.length() - suffix.length())
							+ append;
					srcName = "WebRoot/" + destName;
				}
				// WEB-INF开始 fqbus等项目
				else if (fileName.startsWith("WEB-INF")) {
					String append = suffix.equals("java") ? "class" : suffix;
					fileName = fileName.substring(8, fileName.length());// 过滤掉WEB-INF/
					destName = "";
					if (fileName.startsWith("src")) {
						destName = "WEB-INF/classes/"
								+ fileName.substring(fileName.indexOf("/"),
										fileName.length() - suffix.length())
								+ append;
					} else {
						destName = "WEB-INF/"
								+ fileName.substring(0, fileName.length()
										- suffix.length()) + append;
					}
					srcName = destName;
				}
				// 普通文件,直接copy
				else {
					srcName = fileName;
					destName = fileName.contains("WebRoot/") ? fileName
							.substring(8) : fileName;
				}
				doPatch(srcName, destName, status);
			}
			System.out.println("\r\n补丁列表文件处理完成。");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(br);
		}
	}

	public void restart() {// 短连接的ShellUtil
		if (!"Y".equals(this.isRestart)) {
			return;
		}
		// shutdown
		ShellUtil sl = new ShellUtil(this.ip, this.port, this.user,
				this.password);
		String[] commands = new String[3];
		commands[0] = "pid=`ps aux | grep tomcat | grep -v grep | grep -v retomcat | awk '{print $2}'`";
		commands[1] = "kill -9 $pid";
		commands[2] = "sleep 5";// 休眠5秒
		System.out.println("IsSuccess:" + sl.executeCommands(commands));
		sl.disconnect();

		// start
		sl = new ShellUtil(this.ip, this.port, this.user, this.password);
		commands = new String[2];
		commands[0] = "cd " + this.serverPath + "/bin";
		commands[1] = "./startup.sh";
		System.out.println("IsSuccess:" + sl.executeCommands(commands));
		sl.disconnect();
	}

	public static void main(String[] args) throws FileNotFoundException {
		/*
		 * for (String arg : args) { System.out.println(arg); if
		 * ("-h".equals(arg)) { System.out.println("help"); } }
		 */

		/*
		 * SmartPatcher patcher = new SmartPatcher(); patcher.createPatch(); //
		 * 执行命令，生成patch列表文件 patcher.patch(); // 生成补丁 patcher.upload();
		 * patcher.restart();
		 */

		String relativelyPath = System.getProperty("user.dir");
		System.out.println(relativelyPath);
	}
}

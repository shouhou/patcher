package com.allinno.patcher;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.allinno.util.CmdUtil;
import com.allinno.util.GitLog;
import com.allinno.util.SFTPUtil;
import com.allinno.util.ShellUtil;

/**
 * 
 * @author shouhou
 * @version 2.2.0
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

	private String basePath;
	
	private Logger log = Logger.getLogger(SmartPatcher.class);
	
	//private static final Logger log = LoggerFactory.getLogger(SmartPatcher.class);

	public SmartPatcher() {
		basePath = System.getProperty("user.dir");
		Properties props = new Properties();
		try {
			//如果同级目录存在配置文件则使用该文件，否则用默认的
			File file = new File(basePath + "\\application.properties");
			InputStream in;
			if (file.exists()) {
				in = new BufferedInputStream(new FileInputStream(file));	
			}else{
				in = getClass().getClassLoader().getResourceAsStream("application.properties");
			}
			props.load(in);
			
			//如果配置文件中执行导出更新日志的位置，则采用,否则在jar包同级目录生成
			this.filePath = props.getProperty("patcher.filePath");			
			File filePath = new File(this.filePath);
			if (!filePath.exists()) {
				this.filePath = this.basePath + "/patch_files.txt";
				file = new File(this.filePath);
				file.createNewFile();
			}
			
			//如果配置文件中执行导出更新文件的位置，则采用,否则在jar包同级目录生成
			this.target = props.getProperty("patcher.target");
			File patchDir = new File(this.target);
			if (!patchDir.exists()) {
				this.target = this.basePath + "\\patcher";
			}

			this.proPath = props.getProperty("patcher.proPath");
			this.serverPath = props.getProperty("patcher.serverPath");
			this.serverProPath = props.getProperty("patcher.serverProPath");

			this.ip = props.getProperty("shell.ip");
			this.port = Integer.parseInt(props.getProperty("shell.port"));
			this.user = props.getProperty("shell.user");
			this.password = props.getProperty("shell.password");

			this.isUpload = props.getProperty("shell.isUpload");
			this.isRestart = props.getProperty("shell.isRestart");
		} catch (Exception e) {
			e.printStackTrace();
		}

		/*
		 * Locale locale = Locale.getDefault(); ResourceBundle localResource =
		 * ResourceBundle.getBundle("application",locale); this.filePath =
		 * localResource.getString("patcher.filePath");
		 */

		delList = new ArrayList<String>();
	}

	public void createPatch() {
		try {
			FileOutputStream fos = new FileOutputStream(this.filePath);
			String strCmd = (new GitLog()).getCmd();
			CmdUtil.executeCmd(strCmd, fos);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void donePatch(File srcFile, File destFile, String status)
			throws IOException {
		if ("D".equals(status)) {
			log.debug("清除文件：" + srcFile.getPath());

			// 记录服务器要删除的文件
			String del = srcFile.getPath().replace("\\", "/");
			del = del.replace(proPath, serverProPath);
			delList.add(del);

			if (!srcFile.exists()) {
				log.debug("清除文件清除成功！！！");
				
				return;
			} else {
				FileUtils.deleteDirectory(srcFile);
				log.debug("清除文件清除成功！！！");
			}
		} else {
			log.debug("原文件：" + srcFile.getPath());
			log.debug("目标文件：" + destFile.getPath());

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
		log.debug("\r\n \r\n \r\n开始上传补丁到服务器");
		System.out
				.println("-----------------------------------------------------------");
		SFTPUtil sftp = new SFTPUtil(ip, port, user, password);
		sftp.uploadDirectory(serverProPath, target);// 更新文件
		for (String del : delList) {
			sftp.delete(del);
			log.debug("删除服务器文件:" + del);
		}
		sftp.disconnect();
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
		patchDir = new File(this.target);
		if (patchDir.exists()) {
			log.debug("补丁目录已存在，执行删除操作。。。");
			patchDir.delete();
		}
		log.debug("执行创建补丁目录操作。。。");
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
			log.debug("\r\n开始处理补丁列表文件。。。");
			while ((line = br.readLine()) != null) {
				if (line.trim().length() == 0) {
					log.debug("\r\n空行，跳过。。。");
					continue;
				}
				if (line.trim().startsWith("[Log]")) {
					log.debug("\r\n" + line.trim());
					System.out
							.println("------------------------------------------------------");
					continue;
				}

				line = line.trim().replaceAll("\\\\", "/");
				log.debug("\r\n文件：" + line);

				fileName = line.substring(2);
				status = line.substring(0, 1);

				String suffix = fileName
						.substring(fileName.lastIndexOf(".") + 1);
				log.debug("后缀：" + suffix);
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
			log.debug("\r\n补丁列表文件处理完成。");
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
		ShellUtil sl = new ShellUtil(this.ip, this.port, this.user,this.password);
		String[] commands = new String[3];
		commands[0] = "pid=`ps aux | grep tomcat | grep -v grep | grep -v retomcat | awk '{print $2}'`";
		commands[1] = "kill -9 $pid";
		commands[2] = "sleep 5";// 休眠5秒
		log.debug("IsSuccess:" + sl.executeCommands(commands));
		sl.disconnect();

		// start
		sl = new ShellUtil(this.ip, this.port, this.user, this.password);
		commands = new String[2];
		commands[0] = "cd " + this.serverPath + "/bin";
		commands[1] = "./startup.sh";
		log.debug("IsSuccess:" + sl.executeCommands(commands));
		//log.debug(sl.getResponse());
		sl.disconnect();
	}

	public static void main(String[] args) throws FileNotFoundException {
		for (String arg : args) {
			System.out.println(arg);
			if ("-h".equals(arg)) {
				System.out.println("help");
			}
		}
		SmartPatcher patcher = new SmartPatcher();
		patcher.createPatch();// 执行命令，生成patch列表文件
		patcher.patch(); // 生成补丁
		//patcher.upload();
		//patcher.restart();
	}
}

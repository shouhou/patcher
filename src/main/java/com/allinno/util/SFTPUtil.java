package com.allinno.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.allinno.common.FileProgressMonitor;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

public class SFTPUtil {
	private String ip;
	private int port;
	private String user;
	private String password;
	public Session session;
	public ChannelSftp sftp;
	
	private Logger log = Logger.getLogger(SFTPUtil.class);

	public SFTPUtil(String ip, int port, String user, String password) {
		this.ip = ip;
		this.port = port;
		this.user = user;
		this.password = password;
		sftp = getSftp();
	}

	private ChannelSftp getSftp() {
		ChannelSftp sftp = null;
		try {
			JSch jsch = new JSch();
			jsch.getSession(user, ip, port);
			session = jsch.getSession(user, ip, port);
			session.setPassword(password);
			Properties sshConfig = new Properties();
			sshConfig.put("StrictHostKeyChecking", "no");
			session.setConfig(sshConfig);
			log.debug("Start to connect" + ip + ":" + port);
			session.connect();
			Channel channel = session.openChannel("sftp");
			channel.connect();
			sftp = (ChannelSftp) channel;
			log.debug("Connected to " + ip + ":" + port);
			return sftp;
		} catch (Exception e) {
			log.debug("Connect to " + ip + ":" + port+ "Failed,Please Check Name and Password!");	
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 上传文件
	 * 
	 * @param directory
	 *            上传的目录
	 * @param uploadFile
	 *            要上传的文件
	 */
	public void upload(String directory, String uploadFile) {
		try {
			sftp.cd(directory);
			File file = new File(uploadFile);
			SftpProgressMonitor monitor=new FileProgressMonitor(file.length());
			FileInputStream in = new FileInputStream(file);
			sftp.put(in, file.getName(),monitor);		
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 上传目录
	 * 
	 * @param directory
	 *            上传的目录
	 * @param uploadDirectory
	 *            被上传的目录
	 */
	public void uploadDirectory(String directory, String uploadDirectory) {
		try {
			log.debug("Upload:" + uploadDirectory+"--->"+directory);
			File dir = new File(uploadDirectory);
			for (File file : dir.listFiles()) {
				sftp.cd(directory);
				if (file.isDirectory()) {				
					try {
						sftp.cd(file.getName());
					} catch (Exception e) {
						sftp.mkdir(file.getName());
						sftp.cd(file.getName());
					}
					//System.out.println("当前服务器目录：" + sftp.pwd() + " 本地目录：" + file.getPath());
					uploadDirectory(sftp.pwd(), file.getPath());
				} else {
					upload(sftp.pwd(), file.getPath());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.debug(e.getMessage());
		}
	}

	/**
	 * 下载文件
	 * 
	 * @param directory
	 *            下载目录
	 * @param downloadFile
	 *            下载的文件
	 * @param saveFile
	 *            存在本地的路径
	 */
	public void download(String directory, String downloadFile, String saveFile) {
		try {
			sftp.cd(directory);
			File file = new File(saveFile);
			sftp.get(downloadFile, new FileOutputStream(file));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 删除文件
	 * 
	 * @param directory
	 *            要删除文件所在目录
	 * @param deleteFile
	 *            要删除的文件
	 */
	public void delete(String file) {
		// /root/aa/c.txt
		String deleteFile = file.substring(file.lastIndexOf("/")+1);
		String directory = file.substring(0, file.lastIndexOf("/"));
		delete(directory, deleteFile);
	}

	/**
	 * 删除文件
	 * 
	 * @param directory
	 *            要删除文件所在目录
	 * @param deleteFile
	 *            要删除的文件
	 */
	public void delete(String directory, String deleteFile) {
		try {
			sftp.cd(directory);
			sftp.rm(deleteFile);
		} catch (Exception e) {
			//出现异常，一般为文件不存在,不处理
		}
	}

	/**
	 * 列出目录下的文件
	 * 
	 * @param directory
	 *            要列出的目录
	 * @return
	 * @throws SftpException
	 */
	public Vector listFiles(String directory) throws SftpException {
		return sftp.ls(directory);
	}
	
	/**
	 * 断开连接
	 */
	public void disconnect() {
		if (sftp != null) {
			sftp.disconnect();
		}
		if (session != null) {
			session.disconnect();
		}
	}

	public static void main(String[] args) throws SftpException {
		String ip = "192.168.221.128";
		int port = 2222;
		String user = "root";
		String password = "qazzaqqq";
		String directory = "/root/new";
		String uploadFile = "E:\\newstar.rar";
		String downloadFile = "out.txt";
		String saveFile = "E:\\new\\download.txt";
		String deleteFile = "out.txt";
		String delete = "/root/aa/c.txt";
		
		String uploadDirectory = "E:\\new";

		SFTPUtil util = new SFTPUtil(ip, port, user, password);
		// util.upload(directory, uploadFile);
		// util.download(directory, downloadFile, saveFile);
		// util.delete("", deleteFile);
		 util.uploadDirectory(directory, uploadDirectory);
		// util.delete(delete);
	}
}

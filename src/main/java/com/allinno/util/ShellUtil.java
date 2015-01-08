package com.allinno.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.oro.text.regex.MalformedPatternException;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import expect4j.Closure;
import expect4j.Expect4j;
import expect4j.ExpectState;
import expect4j.matches.EofMatch;
import expect4j.matches.RegExpMatch;
import expect4j.matches.TimeoutMatch;

public class ShellUtil {

	private Session session;
	private ChannelShell shell;
	private ChannelSftp sftp;
	private static Expect4j expect = null;
	private static final long defaultTimeOut = 1000;
	private StringBuffer buffer = new StringBuffer();

	public static final int COMMAND_EXECUTION_SUCCESS_OPCODE = -2;
	public static final String BACKSLASH_R = "\r";
	public static final String BACKSLASH_N = "\n";
	public static final String COLON_CHAR = ":";
	public static String ENTER_CHARACTER = BACKSLASH_R;
	public static final int SSH_PORT = 22;

	// 正则匹配，用于处理服务器返回的结果
	public static String[] linuxPromptRegEx = new String[] { "~]#", "~#", "#",
			":~#", "/$", ">" };

	public static String[] errorMsg = new String[] { "could not acquire the config lock " };

	// ssh服务器的ip地址
	private String ip;
	// ssh服务器的登入端口
	private int port;
	// ssh服务器的登入用户名
	private String user;
	// ssh服务器的登入密码
	private String password;

	public ShellUtil() {
		Locale locale = Locale.getDefault();
		ResourceBundle localResource = ResourceBundle.getBundle("application",
				locale);
		this.ip = localResource.getString("shell.ip");
		this.port = Integer.parseInt(localResource.getString("shell.port"));
		this.user = localResource.getString("shell.user");
		this.password = localResource.getString("shell.password");
		expect = getExpect();

	}

	public ShellUtil(String ip, int port, String user, String password) {
		this.ip = ip;
		this.port = port;
		this.user = user;
		this.password = password;
		expect = getExpect();
	}

	/**
	 * 关闭SSH远程连接
	 */
	public void disconnect() {
		if (shell != null) {
			shell.disconnect();
		}
		if (session != null) {
			session.disconnect();
		}
	}

	/**
	 * 获取服务器返回的信息
	 * 
	 * @return 服务端的执行结果
	 */
	public String getResponse() {
		return buffer.toString();
	}

	// 获得Expect4j对象，该对象可以往SSH发送命令请求
	private Expect4j getExpect() {
		try {
			System.out.println("Start logging to %s@%s:%s" + user + ip + port);
			JSch jsch = new JSch();
			session = jsch.getSession(user, ip, port);
			session.setPassword(password);
			Hashtable config = new Hashtable();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			localUserInfo ui = new localUserInfo();
			session.setUserInfo(ui);
			session.connect();
			shell = (ChannelShell) session.openChannel("shell");
			Expect4j expect = new Expect4j(shell.getInputStream(),
					shell.getOutputStream());
			shell.connect();
			System.out.println("Logging to %s@%s:%s successfully!" + user + ip
					+ port);
			return expect;
		} catch (Exception ex) {
			System.out.println("Connect to " + ip + ":" + port
					+ "failed,please check your username and password!");
			ex.printStackTrace();
		}
		return null;
	}

	// 获取sftp对象，
	private ChannelSftp getFtp() {
		try {
			JSch jsch = new JSch();
			jsch.getSession(user, ip, port);
			Session sshSession = jsch.getSession(user, ip, port);
			System.out.println("Session created.");
			sshSession.setPassword(password);
			Hashtable config = new Hashtable();
			config.put("StrictHostKeyChecking", "no");
			sshSession.setConfig(config);
			sshSession.connect();
			Channel channel = sshSession.openChannel("sftp");
			channel.connect();
			sftp = (ChannelSftp) channel;
			System.out.println("Connected to " + ip + ":" + port);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sftp;
	}

	/**
	 * 执行配置命令
	 * 
	 * @param commands
	 *            要执行的命令，为字符数组
	 * @return 执行是否成功
	 */
	public boolean executeCommands(String[] commands) {
		// 如果expect返回为0，说明登入没有成功
		if (expect == null) {
			return false;
		}
		for (int i = 0; i < commands.length; i++) {
			String command = commands[i];
			System.out.println(command);
		}
		Closure closure = new Closure() {
			public void run(ExpectState expectState) throws Exception {
				buffer.append(expectState.getBuffer());// buffer is string
														// buffer for appending
														// output of executed
														// command
				expectState.exp_continue();

			}
		};
		List lstPattern = new ArrayList();
		String[] regEx = linuxPromptRegEx;
		if (regEx != null && regEx.length > 0) {
			synchronized (regEx) {
				for (int i = 0; i < regEx.length; i++) {// list of regx like,
														// :>, />
					String regexElement = regEx[i];
					// etc. it is possible
					// command prompts of your
					// remote machine
					try {
						RegExpMatch mat = new RegExpMatch(regexElement, closure);
						lstPattern.add(mat);
					} catch (MalformedPatternException e) {
						return false;
					} catch (Exception e) {
						return false;
					}
				}
				lstPattern.add(new EofMatch(new Closure() { // should cause
															// entire page to be
															// collected
							public void run(ExpectState state) {
							}
						}));
				lstPattern.add(new TimeoutMatch(defaultTimeOut, new Closure() {
					public void run(ExpectState state) {
					}
				}));
			}
		}
		try {
			boolean isSuccess = true;
			for (int i = 0; i < commands.length; i++) {
				String strCmd = commands[i];
				isSuccess = isSuccess(lstPattern, strCmd);
			}
			// 防止最后一个命令执行不了
			isSuccess = !checkResult(expect.expect(lstPattern));

			// 找不到错误信息标示成功
			String response = buffer.toString().toLowerCase();
			for (int i = 0; i < errorMsg.length; i++) {
				String msg = errorMsg[i];
				if (response.indexOf(msg) > -1) {
					return false;
				}
			}

			return isSuccess;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	// 检查执行是否成功
	private boolean isSuccess(List objPattern, String strCommandPattern) {
		try {
			boolean isFailed = checkResult(expect.expect(objPattern));
			if (!isFailed) {
				expect.send(strCommandPattern);
				expect.send("\r");
				return true;
			}
			return false;
		} catch (MalformedPatternException ex) {
			return false;
		} catch (Exception ex) {
			return false;
		}
	}

	// 检查执行返回的状态
	private boolean checkResult(int intRetVal) {
		if (intRetVal == COMMAND_EXECUTION_SUCCESS_OPCODE) {
			return true;
		}
		return false;
	}

	public static class localUserInfo implements UserInfo {
		String passwd;

		public String getPassword() {
			return passwd;
		}

		public boolean promptYesNo(String str) {
			return true;
		}

		public String getPassphrase() {
			return null;
		}

		public boolean promptPassphrase(String message) {
			return true;
		}

		public boolean promptPassword(String message) {
			return true;
		}

		public void showMessage(String message) {

		}
	}

	public static void main(String[] args) {
		String host = "192.168.10.138";
		int port = 2222;
		String username = "root";
		String password = "qazzaqqq";
		ShellUtil sl = new ShellUtil(host, port, username, password);
		String[] commands = new String[2];
		commands[0] = "cd /root";
		// commands[1] = "rm -rf index.html";
		System.out.println("输出 list=" + sl.executeCommands(commands));
		sl.disconnect();
	}
}

package com.allinno.util;

import com.jcraft.jsch.SftpProgressMonitor;

public class FileProgressMonitor implements SftpProgressMonitor {
	private long fileSize;
	private long transfered;
	private int percent = 0;

	public FileProgressMonitor(long size) {
		this.fileSize = size;
	}

	@Override
	public boolean count(long count) {
		this.transfered = this.transfered + count;
		this.percent = (int) (100 * this.transfered / this.fileSize);
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < this.percent / 4; i++) {
			buf.append("*");
		}
		String name = String.format("[%-25s]", buf.toString());
		System.out.print("Percent:" + name +this.percent + "%\r");
		return true;
	}

	@Override
	public void end() {
		System.out.println("Transferring Done");
	}

	@Override
	public void init(int op, String src, String dest, long max) {
		System.out.println("Transferring Begin");
	}

	public static void main(String[] args) {
	}
}
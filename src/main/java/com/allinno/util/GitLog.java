package com.allinno.util;

import java.util.Locale;
import java.util.ResourceBundle;

import org.joda.time.DateTime;

public class GitLog {
	private String proPath = "";
	private String sinceDate = "";
	private String untilDate = "";
	private String author = "";

	public GitLog(String proPath,String sinceDate,String untilDate,String author){
		this.proPath=proPath.replace("/", "\\\\");
		this.sinceDate=sinceDate;
		this.untilDate=untilDate;
		this.author=author;
	}

	public String getCmd() {
		// cmd /c F: && cd F:\\Project\\JavaWebP\\learn && git log --pretty=format:'Log' --name-status --author=shouhou --since={2014-12-03} --until={2014-12-08}
		StringBuffer cmd = new StringBuffer();
		cmd.append("cmd /c ");
		cmd.append(this.proPath.substring(0, 1) + ":");
		cmd.append(" && cd " + this.proPath);
		cmd.append(" && git log --pretty=format:\"[Log]%an %ad\"  --name-status --date=short");
		if(!"".equals(this.author)){
			cmd.append(" --author=" + this.author);			
		}
		
		if(!"".equals(this.sinceDate)){
			//String day=DateTime.parse(this.sinceDate).minusDays(1).toString("yyyy-MM-dd");
			cmd.append(" --since={" + this.sinceDate + "}");			
		}
		if(!"".equals(this.untilDate)){
			cmd.append(" --until={" + this.untilDate + "}");			
		}
		return cmd.toString();
	}
	public static void main(String[] args) {
		//GitLog log=new GitLog();
		//System.out.println(log.getCmd());
	}
}

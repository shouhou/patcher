package com.allinno.util;

import java.io.File;

public class FileUtil {
	public static void delete(File file){
	    if (!file.exists())  
	        return;
	    if (file.isFile()) {  
	    	file.delete();  
	        return;
	    }  
	    File[] files = file.listFiles();  
	    for (int i = 0; i < files.length; i++) {  
	    	delete(files[i]);  
	    }  
	    file.delete();  
	}
}

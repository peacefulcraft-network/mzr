package net.peacefulcraft.mzr.config;

import java.io.File;
import java.io.FileFilter;

public class YAMLFileFilter implements FileFilter {

	@Override
	public boolean accept(File pathname) {
		return pathname.toString().endsWith(".yml") || pathname.toString().endsWith(".yaml");
	}

	public static String removeExtension(String name) {
		if (name.endsWith(".yml")) {
			return name.substring(0, name.length()-4);
		} else if (name.endsWith(".yaml")) {
			return name.substring(0, name.length()-5);
		}

		return name;
	}
	
}

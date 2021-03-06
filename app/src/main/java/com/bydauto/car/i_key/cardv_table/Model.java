package com.bydauto.car.i_key.cardv_table;

public class Model {
	private static final String TAG = "Model";
	private final boolean isDirectory;
	private int size;
	private String name;
	private String time;
	private String thumbURL = null;

//	{"Leauto_20170719_191757A.MP4":"68157440 bytes|2017-07-19 19:18:40"}
//	[{"2017-09-13-16-31-27.MP4":"188743680 bytes|2017-09-13 16:33:26"},{"2017-09-13-16-35-27.MP4":"188743680 bytes|2017-09-13 16:37:26"}]
	public Model(String descriptor) {
//		Log.e(TAG, "Model: 11111 descriptor=" + descriptor);
		descriptor = descriptor.replaceAll("[{}\"]", "");
		// parse the name
		int index = descriptor.indexOf(':');
		name = descriptor.substring(0, index).trim();

		// parse the thumb name
		if (name.endsWith("A.MP4")) {
			thumbURL = name.replace("A.MP4", "T.JPG");
		} else if (name.endsWith("A.JPG")) {
			thumbURL = name.replace("A.JPG", "T.JPG");
		} else if (name.endsWith(".JPG")) {    //我加入的一个判断
//			Log.e(TAG, "Model: 1111name" +  name);
			thumbURL = name;
		} else if (name.endsWith(".MP4")) {    //我加入的二个判断
//			Log.e(TAG, "Model: 1111name" +  name);
			thumbURL = name;
		}
		// figure out if this is file or directory
		isDirectory = name.endsWith("/");
		if (isDirectory) {
			name = name.substring(0, name.length() - 2);
		}

		if (descriptor.contains("|")) {
			// get the size
			descriptor = descriptor.substring(index + 1).trim();
			index = descriptor.indexOf(" ");
			size = Integer.parseInt(descriptor.substring(0, index));

			// get the time
			time = descriptor.substring(descriptor.indexOf('|') + 1).trim();
		} else if (descriptor.contains("bytes")) {
			index = descriptor.indexOf("bytes");
			size = Integer.parseInt(descriptor.substring(0, index));
			time = null;
		} else {
			size = -1;
			time = descriptor.substring(index + 1).trim();
		}
	}

	public String getName() {
		return name;
	}

	public String getThumbFileName() {
		return thumbURL;
	}

	public int getSize() {
		return size;
	}

	public String getTime() {
		return time;
	}

	public boolean isDirectory() {
		return isDirectory;
	}
}

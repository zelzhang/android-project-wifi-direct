package com.example.android.wifidirect;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class SongsManager {
	/*add*/
	ArrayList<File> list;

	private ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();
	
	// Constructor
	public SongsManager(){
		
	}
	
	/**
	 * Function to read all mp3 files from sdcard
	 * and store the details in ArrayList
	 * */
	public ArrayList<HashMap<String, String>> getPlayList(){
		list = audioReader( Environment.getExternalStorageDirectory() );

		//File home = new File("/storage/sdcard0/Music");
		//Log.d("AA", MEDIA_PATH);


		/*if (home.listFiles(new FileExtensionFilter()).length > 0) {
			for (File file : home.listFiles(new FileExtensionFilter())) {
				HashMap<String, String> song = new HashMap<String, String>();
				song.put("songTitle", file.getName().substring(0, (file.getName().length() - 4)));
				song.put("songPath", file.getPath());
				Log.d("TAG", file.getPath());
				// Adding each song to SongList
				songsList.add(song);
			}
		}*/
		// return songs list array
		return songsList;
	}
	
	/**
	 * Class to filter files which are having .mp3 extension
	 *
	class FileExtensionFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			return (name.endsWith(".mp3") || name.endsWith(".MP3"));
		}
	}
	*/

	/*add*/
	public ArrayList<File> audioReader(File root){
		ArrayList<File> a = new ArrayList<>();

		File[] files = root.listFiles();
		for(int i = 0; i < files.length; i++) {
			if(files[i].isDirectory()){
				a.addAll( audioReader(files[i]) );
			}
			else {
				if(files[i].getName().endsWith(".mp3") || files[i].getName().endsWith(".MP3")){
					//a.add(files[i]);


					HashMap<String, String> song = new HashMap<String, String>();
					song.put("songTitle", files[i].getName().substring(0, (files[i].getName().length() - 4)));
					song.put("songPath", files[i].getPath());
					Log.d("TAG", files[i].getPath());
					// Adding each song to SongList
					songsList.add(song);

				}

			}
		}
		return a;
	}
}

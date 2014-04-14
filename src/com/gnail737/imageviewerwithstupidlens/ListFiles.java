package com.gnail737.imageviewerwithstupidlens;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ListFiles extends ListActivity {
	
	private List<String> directoryEntries = new ArrayList<String>();

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Intent i = getIntent();
        File directory = new File(i.getStringExtra("directory"));

        if (directory.isDirectory()){
            File[] files = directory.listFiles();

            //Sort in descending date order
            Arrays.sort(files, new Comparator<File>(){
                public int compare(File f1, File f2) {
                    
                	return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                
                }
            });

            //Fill list with files
            this.directoryEntries.clear();
            for (File file : files){
                this.directoryEntries.add(file.getPath());
            }

            ArrayAdapter<String> directoryList = new ArrayAdapter<String>(
                    this,
                    R.layout.file_row, this.directoryEntries);

            //Alphabetize entries
            //directoryList.sort(null);
            this.setListAdapter(directoryList);
        }
    }
	
	@SuppressWarnings("unchecked")
	@Override
    protected void onListItemClick(ListView l, View v,
                                       int position, long id) {
        File clickedFile = new File(this.directoryEntries.get(position));
        Intent i = getIntent();
        i.putExtra("clickedFile", clickedFile.toString());
        ((ArrayAdapter<String>)this.getListAdapter()).notifyDataSetChanged();
        setResult(RESULT_OK, i);
        finish();
    }
}

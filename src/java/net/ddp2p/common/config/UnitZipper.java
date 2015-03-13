/*
 * Adapted after code published by
By Z. Steve Jin and John D. Mitchell

	Adaptation by: Marius Silaghi: msilaghi@fit.edu
	Florida Tech, Human Decision Support Systems Laboratory
   
     This program is free software; you can redistribute it and/or modify
     it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */
package net.ddp2p.common.config;

import java.net.URL;
import java.io.*;
import java.net.*;
import java.util.zip.*;
import java.util.*;

public class UnitZipper{
    private static final boolean DEBUG = false;
	private String myClassName;
    static String MANIFEST = "META-INF/MANIFEST.MF";

    public static void main(String[] args){
    	UnitZipper zse = new UnitZipper();
        String jarFileName = zse.getJarFileName();
        try {
			jarFileName = URLDecoder.decode(jarFileName, "utf-8");
		} catch (UnsupportedEncodingException e) {
			if(DEBUG)e.printStackTrace();
		}
        zse.extract(jarFileName);
        System.exit(0);
    }
    private String getJarFileName()
    {
        myClassName = this.getClass().getName() + ".class";
        URL urlJar = /*this.getClass().getClassLoader().*/ ClassLoader.getSystemResource(myClassName);
        String urlStr = urlJar.toString();
        int from = "jar:file:".length();
        if(DEBUG) System.out.println("UnitZipper:getJarFile: urlJar="+urlStr);
        int to = urlStr.indexOf("!/");
        return urlStr.substring(from, to);
    }

/*
        JFileChooser fc = new JFileChooser();

        fc.setCurrentDirectory(new File("."));
        fc.setDialogType(JFileChooser.OPEN_DIALOG);
        fc.setDialogTitle("Select destination directory for extracting " +
                currentArchive.getName());
        fc.setMultiSelectionEnabled(false);
       
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if (fc.showDialog(UnitZipper.this, "Select")
            != JFileChooser.APPROVE_OPTION) 
        {
            return;  //only when user select valid dir, it can return approve_option
        }
       File outputDir = fc.getSelectedFile()
        ProgressMonitor pm = null;
        SimpleDateFormat formatter = new SimpleDateFormat ("MM/dd/yyyy hh:mma",Locale.getDefault());

                pm = new ProgressMonitor(getParent(), "Extracting files...", "starting", 0, size-4);
                pm.setMillisToDecideToPopup(0);
                pm.setMillisToPopup(0);
*/
    public void extract(String zipfile){
    	if(DEBUG)System.out.println("UnitZipper:getJarFile: Extracting from: "+zipfile);
        File currentArchive = new File(zipfile);
        File outputDir = new File(".");
        byte[] buf = new byte[1024];
        boolean overwrite = false;

        ZipFile zf = null;
        FileOutputStream out = null;
        InputStream in = null;
        try {
                zf = new ZipFile(currentArchive);
                int size = zf.size();
                int extracted = 0;
                Enumeration entries = zf.entries();

                for (int i=0; i<size; i++) 
                {
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    if(entry.isDirectory())
                        continue;

                    String pathname = entry.getName();
                    if(myClassName.equals(pathname) || MANIFEST.equals(pathname.toUpperCase()))
                        continue;

                    extracted ++;
                    
                    /*
                    pm.setProgress(i);
                    pm.setNote(pathname);
                    if(pm.isCanceled())
                        return;
*/
                    in = zf.getInputStream(entry);

                    File outFile = new File(outputDir, pathname);
                    Date archiveTime = new Date(entry.getTime());

                    if(overwrite==false)
                    {
                        if(outFile.exists())
                        {
                            Object[] options = {"Yes", "Yes To All", "No"};
                            Date existTime = new Date(outFile.lastModified());
                            Long archiveLen = new Long(entry.getSize());
 /*
                           
                            String msg = "File name conflict: "
                                + "There is already a file with "
                                + "that name on the disk!\n"
                                + "\nFile name: " + outFile.getName()
                                + "\nExisting file: "
                                + formatter.format(existTime) + ",  "
                                + outFile.length() + "Bytes"
                                + "\nFile in archive:"
                                + formatter.format(archiveTime) + ",  " 
                                + archiveLen + "Bytes"
                                +"\n\nWould you like to overwrite the file?";
                            int result = JOptionPane.showOptionDialog(ZipSelfExtractor.this,
                                msg, "Warning", JOptionPane.DEFAULT_OPTION,
                                JOptionPane.WARNING_MESSAGE, null, options,options[0]); 
                            */
                            int result = 2;
                            if(result == 2) // No
                            {
                                continue;
                            }
                            else if( result == 1) //YesToAll

                            	
                            {
                                overwrite = true;
                            }
                        }
                    }

                    File parent = new File(outFile.getParent());
                    if (parent != null && !parent.exists())
                    {
                        parent.mkdirs();
                    }

                    out = new FileOutputStream(outFile);                

                    while (true) 
                    {
                        int nRead = in.read(buf, 0, buf.length);
                        if (nRead <= 0)
                            break;
                        out.write(buf, 0, nRead);
                    }
                    
                    out.close();
                    outFile.setLastModified(archiveTime.getTime());
                }
             
                //pm.close();
                zf.close();
                //getToolkit().beep();
                /*
                JOptionPane.showMessageDialog
                    (UnitZipper.this,
                     "Extracted " + extracted +
                     " file" + ((extracted > 1) ? "s": "") +
                     " from the\n" +
                     zipfile + "\narchive into the\n" +
                     outputDir.getPath() +
                     "\ndirectory.",
                     "Zip Self Extractor",
                     JOptionPane.INFORMATION_MESSAGE);
*/
            }
            catch (Exception e)
            {
                System.out.println(e);
                if(DEBUG) e.printStackTrace();
                if(zf!=null) { try { zf.close(); } catch(IOException ioe) {;} }
                if(out!=null) { try {out.close();} catch(IOException ioe) {;} }
                if(in!=null) { try { in.close(); } catch(IOException ioe) {;} }
            }
    }
}
                            	
                            	
                            	
                            	
        
        
        
        
/*

public class UnitZipper {
	private String myClassName;

	private String getJarFileName ()
    {
      myClassName = this.getClass().getName() + ".class";
      URL urlJar =
          this.getClass().getClassLoader().getSystemResource(myClassName);
      String urlStr = urlJar.toString();
      int from = "jar:file:".length();
      int to = urlStr.indexOf("!/");
      return urlStr.substring(from, to);
    }
}
*/
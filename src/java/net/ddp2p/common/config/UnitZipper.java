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
        URL urlJar =  ClassLoader.getSystemResource(myClassName);
        String urlStr = urlJar.toString();
        int from = "jar:file:".length();
        if(DEBUG) System.out.println("UnitZipper:getJarFile: urlJar="+urlStr);
        int to = urlStr.indexOf("!/");
        return urlStr.substring(from, to);
    }
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
                            int result = 2;
                            if(result == 2) 
                            {
                                continue;
                            }
                            else if( result == 1) 
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
                zf.close();
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

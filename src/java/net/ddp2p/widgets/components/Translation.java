/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2011 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
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
 package net.ddp2p.widgets.components;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import net.ddp2p.common.config.Language;
import net.ddp2p.common.data.DDTranslation;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.DDIcons;


public
class Translation implements IconedItem {
	public static String DEFAULT_TEXT_LANG = "en";
	String original;
	String text;
	String lang;
	String flavor;
	public Translation(String text, String lang, String flavor, String original){
		if((lang == null)||("null".equals(lang))){
			//Util.printCallPath("No lang for elem: "+this);
			lang = Translation.DEFAULT_TEXT_LANG;
		}
		this.original = original;
		this.text = text;
		this.lang = lang;
		this.flavor = flavor;
	}
	public Translation(String text, String lang, String flavor){
		if((lang == null)||("null".equals(lang))){
			//Util.printCallPath("No lang for elem: "+this);
			lang = Translation.DEFAULT_TEXT_LANG;
		}
		this.text = text;
		this.lang = lang;
		this.flavor = flavor;
	}
	public Translation(String text, String lang){
		if((lang == null)||("null".equals(lang))){
			//Util.printCallPath("No lang for elem: "+this);
			lang = Translation.DEFAULT_TEXT_LANG;
		}
		this.text = text;
		this.lang = lang;
		this.flavor = null;
	}
	public Translation(String text){
		this.text = text;
		this.lang = Translation.DEFAULT_TEXT_LANG;
		this.flavor = null;
	}
	public static Translation translated(String source, Language from) {
		ArrayList<ArrayList<Object>> sel =
			DDTranslation.translates(source, from);
		if (sel == null) return new Translation(source,null,null,source);
		return new Translation(Util.sval(sel.get(0).get(0),""),
				Util.sval(sel.get(0).get(1),""),
				Util.sval(sel.get(0).get(2),""),
				source);
	}
	public String getOriginal(){return (original==null)?text:original;}
	public static String getTip(String ietf){
		String icon_name=Translation.getIconName(ietf);
		URL u = DDIcons.getResourceURL(icon_name);/*
		Object ur=null;
		try {
			ur = u.getContent();
		} catch (IOException e) {
			//e.printStackTrace();
			ur=null;
		}
		System.out.println("Translation:getTip: got content:"+ur);
		*/
		//System.out.println("Translation:getTip: got url:"+u);
		if(u!=null)return "<html><img src='"+u+"' title='language'> ("+ietf+")</html>";
		else return ietf;
		/*
		File f=new File(icon_name);
		if(f.exists())
			return "<html><img src='file:"+icon_name+"' title='language'> ("+ietf+")</html>";
		else return ietf;
		*/
	}
	public String getTip(){
		String tip;
		if(flavor!=null)
			tip = lang+"_"+flavor;
		else
			tip = lang;
		String result = getTip(tip);
		/*
		String icon_name=this.getIconName(tip);
		File f=new File(icon_name);
		if(f.exists())
			return "<html><img src='file:"+icon_name+"' title='language'> ("+tip+")</html>";
		else return tip;
		*/
		//System.out.println("Translation:getTip: got "+result);
		return result;
	}
	public String toString(){
		return text;
	}
	public static String getIconName(String ietf){
		return "steag/"+ietf+".png";
	}
	public URL getIconURL() throws MalformedURLException{
		if(lang == null){
			Util.printCallPath("No lang for elem: "+this);
			return null;
		}
		if("null".equals(lang)){
			Util.printCallPath("No \"lang\" for elem: "+this);
			return null;
		}
		if(flavor!=null) {
			URL url = DDIcons.getResourceURLOrNull("steag/"+lang+"_"+flavor+".png");
			if(url!=null) return url;
		}
		URL url = DDIcons.getResourceURL("steag/"+lang+".png");
		return url;
	}
	public String getIconName(){
		if(lang == null){
			Util.printCallPath("No lang for elem: "+this);
			return null;
		}
		if("null".equals(lang)){
			Util.printCallPath("No \"lang\" for elem: "+this);
			return null;
		}
		if(flavor!=null) {
		    //return "flags/"+lang+"_"+flavor+".gif";
			return "steag/"+lang+"_"+flavor+".png";
		}
		//return "flags/"+lang+".gif";
		return "steag/"+lang+".png";
	}
	@Override
	public ImageIcon getImageIcon() {
		return DDIcons.getImageIconFromResource(getIconName(), null);
	}
}

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
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.*;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static net.ddp2p.common.util.Util.__;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import net.ddp2p.common.util.Util;
import util.*;

public
class DDCountrySelector extends JComboBox  implements ActionListener{
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	static String[] c=new String[]{"AD","AE","AF","AG","AI","AL","AM","AN","AO","AQ","AR","AS","AT","AU","AW","AX","AZ","BA","BB","BD","BE","BF","BG","BH","BI","BJ","BM","BN","BO","BR","BS","BT","BV","BW","BY","BZ","CA","CC","CD","CF","CG","CH","CI","CK","CL","CM","CN","CO","CR","CU","CV","CX","CY","CZ","DE","DJ","DK","DM","DO","DZ","EC","EE","EG","EH","ER","ES","ET","EU","FI","FJ","FK","FM","FO","FR","GA","GB","GD","GE","GF","GG","GH","GI","GL","GM","GN","GP","GQ","GR","GS","GT","GU","GW","GY","HK","HM","HN","HR","HT","HU","ID","IE","IL","IM","IN","IO","IQ","IR","IS","IT","JE","JM","JO","JP","KE","KG","KH","KI","KM","KN","KP","KR","KW","KY","KZ","LA","LB","LC","LI","LK","LR","LS","LT","LU","LV","LY","MA","MC","MD","ME","MG","MH","MK","ML","MM","MN","MO","MP","MQ","MR","MS","MT","MU","MV","MW","MX","MY","MZ","NA","NC","NE","NF","NG","NI","NL","NO","NP","NR","NU","NZ","OM","PA","PE","PF","PG","PH","PK","PL","PM","PN","PR","PS","PT","PW","PY","QA","RE","RO","RS","RU","RW","SA","SB","SC","SD","SE","SG","SH","SI","SJ","SK","SL","SM","SN","SO","SR","ST","SV","SY","SZ","TC","TD","TF","TG","TH","TJ","TK","TL","TM","TN","TO","TR","TT","TV","TW","TZ","UA","UG","UM","US","UY","UZ","VA","VC","VE","VG","VI","VN","VU","WF","WS","YE","YT","ZA","ZM","ZW"};
	//static String[] cy=new String[]{"AD","AD","AE","AE","AF","Afganistan","AG","AG","AI","AI","AL","AL","AM","AM","AN","AN","AO","AO","AQ","AQ","AR","AR","AS","AS","AT","AT","AU","AU","AW","AW","AX","AX","AZ","AZ","BA","BA","BB","BB","BD","BD","BE","BE","BF","BF","BG","BG","BH","BH","BI","BI","BJ","BJ","BM","BM","BN","BN","BO","BO","BR","BR","BS","BS","BT","BT","BV","BV","BW","BW","BY","BY","BZ","BZ","CA","CA","CC","CC","CD","CD","CF","CF","CG","CG","CH","China","CI","CI","CK","CK","CL","CL","CM","CM","CN","CN","CO","CO","CR","CR","CU","CU","CV","CV","CX","CX","CY","CY","CZ","CZ","DE","DE","DJ","DJ","DK","DK","DM","DM","DO","DO","DZ","DZ","EC","EC","EE","EE","EG","EG","EH","EH","ER","ER","ES","ES","ET","ET","EU","EU","FI","FI","FJ","FJ","FK","FK","FM","FM","FO","FO","FR","FR","GA","GA","GB","GB","GD","GD","GE","GE","GF","GF","GG","GG","GH","GH","GI","GI","GL","GL","GM","GM","GN","GN","GP","GP","GQ","GQ","GR","GR","GS","GS","GT","GT","GU","GU","GW","GW","GY","GY","HK","HK","HM","HM","HN","HN","HR","HR","HT","HT","HU","HU","ID","ID","IE","IE","IL","IL","IM","IM","IN","IN","IO","IO","IQ","IQ","IR","IR","IS","IS","IT","IT","JE","JE","JM","JM","JO","JO","JP","JP","KE","KE","KG","KG","KH","KH","KI","KI","KM","KM","KN","KN","KP","KP","KR","KR","KW","KW","KY","KY","KZ","KZ","LA","LA","LB","LB","LC","LC","LI","LI","LK","LK","LR","LR","LS","LS","LT","LT","LU","LU","LV","LV","LY","LY","MA","MA","MC","MC","MD","MD","ME","ME","MG","MG","MH","MH","MK","MK","ML","ML","MM","MM","MN","MN","MO","MO","MP","MP","MQ","MQ","MR","MR","MS","MS","MT","MT","MU","MU","MV","MV","MW","MW","MX","MX","MY","MY","MZ","MZ","NA","NA","NC","NC","NE","NE","NF","NF","NG","NG","NI","NI","NL","NL","NO","NO","NP","NP","NR","NR","NU","NU","NZ","NZ","OM","OM","PA","PA","PE","PE","PF","PF","PG","PG","PH","PH","PK","PK","PL","PL","PM","PM","PN","PN","PR","PR","PS","PS","PT","PT","PW","PW","PY","PY","QA","QA","RE","RE","RO","Romania","RS","RS","RU","RU","RW","RW","SA","SA","SB","SB","SC","SC","SD","SD","SE","SE","SG","SG","SH","SH","SI","SI","SJ","SJ","SK","SK","SL","SL","SM","SM","SN","SN","SO","SO","SR","SR","ST","ST","SV","SV","SY","SY","SZ","SZ","TC","TC","TD","TD","TF","TF","TG","TG","TH","TH","TJ","TJ","TK","TK","TL","TL","TM","TM","TN","TN","TO","TO","TR","TR","TT","TT","TV","TV","TW","TW","TZ","TZ","UA","UA","UG","UG","UM","UM","US","United States","UY","UY","UZ","UZ","VC","VC","VE","VE","VG","VG","VI","VI","VN","VN","VU","VU","WF","WF","WS","WS","YE","YE","YT","YT","ZA","ZA","ZM","ZM","ZW","ZW"};
    static String[] cy=new String[]{"AF","AFGHANISTAN","AL","ALBANIA","DZ","ALGERIA","AS","AMERICAN SAMOA","AD","ANDORRA","AO","ANGOLA","AI","ANGUILLA","AQ","ANTARCTICA","AG","ANTIGUA AND BARBUDA","AR","ARGENTINA","AM","ARMENIA","AW","ARUBA","AU","AUSTRALIA","AT","AUSTRIA","AZ","AZERBAIJAN","BS","BAHAMAS","BH","BAHRAIN","BD","BANGLADESH","BB","BARBADOS","BY","BELARUS","BE","BELGIUM","BZ","BELIZE","BJ","BENIN","BM","BERMUDA","BT","BHUTAN","BO","BOLIVIA","BA","BOSNIA AND HERZEGOWINA","BW","BOTSWANA","BV","BOUVET ISLAND","BR","BRAZIL","IO","BRITISH INDIAN OCEAN TERRITORY","BN","BRUNEI DARUSSALAM","BG","BULGARIA","BF","BURKINA FASO","BI","BURUNDI","KH","CAMBODIA","CM","CAMEROON","CA","CANADA","CV","CAPE VERDE","KY","CAYMAN ISLANDS","CF","CENTRAL AFRICAN REPUBLIC","TD","CHAD","CL","CHILE","CN","CHINA","CX","CHRISTMAS ISLAND","CC","COCOS (KEELING) ISLANDS","CO","COLOMBIA","KM","COMOROS","CG","CONGO","CD","CONGO"," THE DRC","CK","COOK ISLANDS","CR","COSTA RICA","CI","COTE D'IVOIRE","HR","CROATIA (local name: Hrvatska)","CU","CUBA","CY","CYPRUS","CZ","CZECH REPUBLIC","DK","DENMARK","DJ","DJIBOUTI","DM","DOMINICA","DO","DOMINICAN REPUBLIC","TP","EAST TIMOR","EC","ECUADOR","EG","EGYPT","SV","EL SALVADOR","GQ","EQUATORIAL GUINEA","ER","ERITREA","EE","ESTONIA","ET","ETHIOPIA","FK","FALKLAND ISLANDS (MALVINAS)","FO","FAROE ISLANDS","FJ","FIJI","FI","FINLAND","FR","FRANCE","FX","FRANCE"," METROPOLITAN","GF","FRENCH GUIANA","PF","FRENCH POLYNESIA","TF","FRENCH SOUTHERN TERRITORIES","GA","GABON","GM","GAMBIA","GE","GEORGIA","DE","GERMANY","GH","GHANA","GI","GIBRALTAR","GR","GREECE","GL","GREENLAND","GD","GRENADA","GP","GUADELOUPE","GU","GUAM","GT","GUATEMALA","GN","GUINEA","GW","GUINEA-BISSAU","GY","GUYANA","HT","HAITI","HM","HEARD AND MC DONALD ISLANDS","VA","HOLY SEE (VATICAN CITY STATE)","HN","HONDURAS","HK","HONG KONG","HU","HUNGARY","IS","ICELAND","IN","INDIA","ID","INDONESIA","IR","IRAN (ISLAMIC REPUBLIC OF)","IQ","IRAQ","IE","IRELAND","IL","ISRAEL","IT","ITALY","JM","JAMAICA","JP","JAPAN","JO","JORDAN","KZ","KAZAKHSTAN","KE","KENYA","KI","KIRIBATI","KP","KOREA"," D.P.R.O.","KR","KOREA"," REPUBLIC OF","KW","KUWAIT","KG","KYRGYZSTAN","LA","LAOS","LV","LATVIA","LB","LEBANON","LS","LESOTHO","LR","LIBERIA","LY","LIBYAN ARAB JAMAHIRIYA","LI","LIECHTENSTEIN","LT","LITHUANIA","LU","LUXEMBOURG","MO","MACAU","MK","MACEDONIA","MG","MADAGASCAR","MW","MALAWI","MY","MALAYSIA","MV","MALDIVES","ML","MALI","MT","MALTA","MH","MARSHALL ISLANDS","MQ","MARTINIQUE","MR","MAURITANIA","MU","MAURITIUS","YT","MAYOTTE","MX","MEXICO","FM","MICRONESIA"," FEDERATED STATES OF","MD","MOLDOVA"," REPUBLIC OF","MC","MONACO","MN","MONGOLIA","MS","MONTSERRAT","MA","MOROCCO","MZ","MOZAMBIQUE","MM","MYANMAR (Burma)","NA","NAMIBIA","NR","NAURU","NP","NEPAL","NL","NETHERLANDS","AN","NETHERLANDS ANTILLES","NC","NEW CALEDONIA","NZ","NEW ZEALAND","NI","NICARAGUA","NE","NIGER","NG","NIGERIA","NU","NIUE","NF","NORFOLK ISLAND","MP","NORTHERN MARIANA ISLANDS","NO","NORWAY","OM","OMAN","PK","PAKISTAN","PW","PALAU","PA","PANAMA","PG","PAPUA NEW GUINEA","PY","PARAGUAY","PE","PERU","PH","PHILIPPINES","PN","PITCAIRN","PL","POLAND","PT","PORTUGAL","PR","PUERTO RICO","QA","QATAR","RE","REUNION","RO","ROMANIA","RU","RUSSIAN FEDERATION","RW","RWANDA","KN","SAINT KITTS AND NEVIS","LC","SAINT LUCIA","VC","SAINT VINCENT AND THE GRENADINES","WS","SAMOA","SM","SAN MARINO","ST","SAO TOME AND PRINCIPE","SA","SAUDI ARABIA","SN","SENEGAL","SC","SEYCHELLES","SL","SIERRA LEONE","SG","SINGAPORE","SK","SLOVAKIA (Slovak Republic)","SI","SLOVENIA","SB","SOLOMON ISLANDS","SO","SOMALIA","ZA","SOUTH AFRICA","GS","SOUTH GEORGIA AND SOUTH S.S.","ES","SPAIN","LK","SRI LANKA","SH","ST. HELENA","PM","ST. PIERRE AND MIQUELON","SD","SUDAN","SR","SURINAME","SJ","SVALBARD AND JAN MAYEN ISLANDS","SZ","SWAZILAND","SE","SWEDEN","CH","SWITZERLAND","SY","SYRIAN ARAB REPUBLIC","TW","TAIWAN"," PROVINCE OF CHINA","TJ","TAJIKISTAN","TZ","TANZANIA"," UNITED REPUBLIC OF","TH","THAILAND","TG","TOGO","TK","TOKELAU","TO","TONGA","TT","TRINIDAD AND TOBAGO","TN","TUNISIA","TR","TURKEY","TM","TURKMENISTAN","TC","TURKS AND CAICOS ISLANDS","TV","TUVALU","UG","UGANDA","UA","UKRAINE","AE","UNITED ARAB EMIRATES","GB","UNITED KINGDOM","US","UNITED STATES","UM","U.S. MINOR ISLANDS","UY","URUGUAY","UZ","UZBEKISTAN","VA","VATICAN","VU","VANUATU","VE","VENEZUELA","VN","VIET NAM","VG","VIRGIN ISLANDS (BRITISH)","VI","VIRGIN ISLANDS (U.S.)","WF","WALLIS AND FUTUNA ISLANDS","EH","WESTERN SAHARA","YE","YEMEN","YU","Yugoslavia (Serbia and Montenegro)","ZM","ZAMBIA","ZW","ZIMBABWE"};
	JTextFieldIconed editor;
	static Hashtable<String, String> c_hash=null;// = countries_map();
	@SuppressWarnings("unchecked")
	public DDCountrySelector(){
		if(c_hash==null) c_hash = countries_map();
		//super(c);
		loadCountries();
		editor = new JTextFieldIconed();
		//editor.addMouseListener(this);
		setEditor(editor);
		setEditable(true);
		setRenderer(new LabelRenderer());
		setChoice(DDTranslation.authorship_lang.flavor);
		addActionListener(this);
		//addMouseListener(this); /// listening on editor		
	}
	static Hashtable<String, String> countries_map() {
		if(c_hash!=null) return c_hash;
		Hashtable<String, String> result = new Hashtable<String, String>();
		for(int i = 0 ; i<cy.length; i+=2)
			result.put(cy[i], cy[i+1]);
		return result;
	}
	static String country(String c){
		Object result=c_hash.get(c);
		if(result == null){
			//System.err.println("Unknown country: "+c);
			return (String)result;
		}
		return c;
		//return DDTranslation.translate((String)result,new Language("en","US"));
	}
	public void setChoice(String c) {
		if(c==null) return;
		for(int k=0;k<this.getItemCount(); k++){
			Country item=(Country)this.getItemAt(k);
			if(item.code.equalsIgnoreCase(c)) {
				this.setSelectedItem(item);
				return;
			}
		}
		editor.setItem(new Country(c,country(c)));
	}
	@SuppressWarnings("unchecked")
	void loadCountries(){
		for(int i=0; i<c.length; i++) {
			this.addItem(new Country(c[i],country(c[i])));
		}
	}
	public void actionPerformed(ActionEvent evt) {
		super.actionPerformed(evt);
		onEdited("comboBoxEdited".equals(evt.getActionCommand()),
				"comboBoxEdited".equals(evt.getActionCommand()));
	}
	void onEdited(boolean edited, boolean changed){
        int idx = getSelectedIndex();
		if(DEBUG)System.err.println("DDCountrySelector:onEdited:Index="+idx);
        if(idx != -1)  {
        	Country crt = (Country)getSelectedItem();
        	setToolTipText(crt.getTip());
        }else{
        	//setToolTipText(Country.getTip(DDTranslation.authorship_lang.toString()));
        	if (edited) {
        		//setTranslation();
        	} 	
        }
	}	
}

//package com.jeeva.chatclient;
package chatApp;	

import java.awt.*;
import java.util.Vector;

public class BorderPanel extends Panel
{

    public BorderPanel(TapPanel tappanel, ChatPeer app , CardLayout cardlayout, Panel panel, int i, int j)
    {
        xofs = 0;
        curTab = 0;
        Cframe = tappanel;
        cardLayout = cardlayout;
        cardPanel = panel;
        tabNames = new Vector(10, 5);
        tabPos = new Vector(10, 5);
        cardNames = new Vector(10, 5);
        textFont = new Font("Helvetica", 1, 11);
        setBackground(Color.white);
        GridBagLayout gridbaglayout = new GridBagLayout();
        GridBagConstraints gridbagconstraints = new GridBagConstraints();
        setLayout(gridbaglayout);
        gridbagconstraints.weightx = 1.0D;
        gridbagconstraints.fill = 1;
        gridbagconstraints.gridwidth = 0;
		chatPeer = app;
        Tabs = new Border(Cframe, chatPeer , this, 1, i);
        gridbaglayout.setConstraints(Tabs, gridbagconstraints);
        add(Tabs);
        gridbagconstraints.weightx = 0.0D;
        gridbagconstraints.weighty = 1.0D;
        gridbagconstraints.gridwidth = 1;
        Border border = new Border(Cframe, chatPeer , this, 2, j);
        gridbaglayout.setConstraints(border, gridbagconstraints);
        add(border);
        Panel panel1 = new Panel();
        GridBagLayout gridbaglayout1 = new GridBagLayout();
        panel1.setLayout(gridbaglayout1);
        gridbagconstraints.weightx = 1.0D;
        gridbagconstraints.gridwidth = 0;
        gridbaglayout1.setConstraints(panel, gridbagconstraints);
        panel1.add(panel);
        gridbagconstraints.gridwidth = -1;
        gridbaglayout.setConstraints(panel1, gridbagconstraints);
        add(panel1);
        gridbagconstraints.weightx = 0.0D;
        gridbagconstraints.gridwidth = 0;
        Border border1 = new Border(Cframe,chatPeer, this, 3, j);
        gridbaglayout.setConstraints(border1, gridbagconstraints);
        add(border1);
        gridbagconstraints.weightx = 1.0D;
        gridbagconstraints.weighty = 0.0D;
        Border border2 = new Border(Cframe,chatPeer, this, 4, i);
        gridbaglayout.setConstraints(border2, gridbagconstraints);
        add(border2);
        validate();
    }

    public void setTab(int i)
    {
        Tabs.setTab(i);
    }

    public int addTab(String s, String s1)
    {
        tabNames.addElement(s);
        cardNames.addElement(s1);
        return tabNames.size() - 1;
    }

    public Vector tabNames;
    public Vector tabPos;
    public Vector cardNames;
    public Panel cardPanel;
    public CardLayout cardLayout;
    public int xofs;
    public Font textFont;
    public int curTab;
    Dimension dim;
    Border Tabs;
    TapPanel Cframe;
	ChatPeer chatPeer;
}

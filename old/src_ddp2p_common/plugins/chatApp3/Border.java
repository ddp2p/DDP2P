
//package com.jeeva.chatclient;
package dd_p2p.plugin;

import java.awt.*;
import java.awt.Graphics;
import java.awt.Dimension;
import java.util.Vector;
public class Border extends Canvas implements CommonSettings
{

    public void DrawBottom(Graphics g)
    {
        int i = size().width;
        int j = size().height;
        g.setColor(chatPeer.ColorMap[4]);
        g.fillRect(0, 0, i, j);
        g.setColor(Color.white);
        g.drawLine(1, 0, 1, j - 2);
        int k = i - (bsize + 4);
        g.drawLine(bsize + 2, 1, k, 1);
        g.fillRect(k, 0, 1, 1);
        g.setColor(Color.gray);
        g.drawLine(bsize + 2, 0, bsize + 2, 0);
        g.drawLine(1, bsize + 2, i - 2, bsize + 2);
        g.drawLine(i - 2, 0, i - 2, j - 2);
        g.setColor(chatPeer.ColorMap[4]);
        g.drawLine(1, bsize + 3, i - 1, bsize + 3);
        g.drawLine(i - 1, 0, i - 1, j - 1);
    }

    Border(TapPanel tappanel, ChatPeer app , BorderPanel borderpanel, int i, int j)
    {
        Cframe = tappanel;
        mode = i;
        size = j;
		chatPeer = app;
        parent = borderpanel;
        bsize = 4;
        tabHeight = 22;
        if(i == 1)
        {
            height = 38;
            width = j;
        }
        if(i == 4)
        {
            height = bsize + 4;
            width = j;
        }
        if(i == 2 || i == 3)
        {
            width = bsize + 4;
            height = j;
        }
        if(i == 5)
        {
            width = j;
            height = 8;
        }
        dim = new Dimension(width, height);
        resize(dim);
        validate();
    }

    public void paint(Graphics g)
    {
        if(mode == 1)
        {
            DrawTabs(g);
            return;
        }
        if(mode == 2)
        {
            DrawVertical(g);
            return;
        }
        if(mode == 3)
        {
            DrawVertical(g);
            return;
        }
        if(mode == 4)
        {
            DrawBottom(g);
            return;
        } else
        {
            DrawHorizontal(g);
            return;
        }
    }

    public void DrawTab(Graphics g, int i, int j, int k, int l, boolean flag, String s)
    {
        g.setColor(chatPeer.ColorMap[4]);
        g.fillRect(i, j, k, l);
        g.setColor(chatPeer.ColorMap[4]);
        g.drawLine(i, j, (i + k) - 2, j);
        g.drawLine(i, j, i, (j + l) - 1);
        g.setColor(Color.gray);
        g.drawLine((i + k) - 2, j, (i + k) - 2, (j + l) - 1);
        g.setColor(chatPeer.ColorMap[0]);
        g.drawLine((i + k) - 1, j + 1, (i + k) - 1, (j + l) - 1);
        g.setColor(chatPeer.ColorMap[7]);
        if(flag)
        {
            g.drawString(s, i + (((TAPPANEL_WIDTH / TAP_COUNT) -  fontmetrics.stringWidth(s)) / 2), j + 16);
            return;
        } else
        {
            g.drawString(s, i + (((TAPPANEL_WIDTH / TAP_COUNT) -  fontmetrics.stringWidth(s)) / 2), j + 16);
            return;
        }
    }

    public void setTab(int i)
    {
        parent.curTab = i;
        String s = (String)parent.cardNames.elementAt(i);       
        parent.cardLayout.show(parent.cardPanel, s);
        repaint();
    }

    public void DrawTop(Graphics g)
    {
        int i = size().width;
        int j = size().height;
        g.setColor(chatPeer.ColorMap[0]);
        g.fillRect(0, 0, size().width, size().height);
        g.setColor(chatPeer.ColorMap[0]);
        g.drawLine(1, 1, 1, j + 1);
        g.drawLine(1, 1, i - 3, 1);
        g.setColor(chatPeer.ColorMap[0]);
        g.drawLine(0, 0, i, 0);
        g.drawLine(0, 1, 0, j + 1);
        g.fillRect(2, 2, bsize, j);
        g.fillRect(bsize + 2, 2, i - bsize, bsize);
        g.setColor(Color.gray);
        g.drawLine(bsize + 2, bsize + 3, i - (bsize + 2), bsize + 3);
        g.drawLine(i - 2, 1, i - 2, j + 1);
        g.setColor(chatPeer.ColorMap[0]);
        g.drawLine(bsize + 3, bsize + 4, i - (bsize + 3), bsize + 4);
        g.drawLine(i - 1, 1, i - 1, j + 1);
    }

    public Dimension minimumSize()
    {
        return dim;
    }

    public void update(Graphics g)
    {
        paint(g);
    }

    public void DrawVertical(Graphics g)
    {
        int i = size().height;
        g.setColor(chatPeer.ColorMap[4]);
        g.drawLine(0, 0, 0, i);
        g.fillRect(2, 0, bsize, i);
        g.setColor(chatPeer.ColorMap[4]);
        g.drawLine(1, 0, 1, i);
        g.setColor(Color.gray);
        g.drawLine(bsize + 2, 0, bsize + 2, i);
        g.setColor(chatPeer.ColorMap[4]);
        g.drawLine(bsize + 3, 0, bsize + 3, i);
    }
	
	public boolean 	mouseEnter(Event event, int i, int j)
	{
		setCursor(new Cursor(Cursor.HAND_CURSOR));
		return true;
	}

	public boolean 	mouseExit(Event event, int i, int j)
	{
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR)); 
		return true;
	}
	
    public boolean mouseDown(Event event, int i, int j)
    {
        if(mode != 1)
            return true;
        for(int k = 0; k < parent.tabPos.size(); k++)
        {
            Rectangle rectangle = (Rectangle)parent.tabPos.elementAt(k);
            if(rectangle.inside(i, j))
                setTab(k);
        }

        return true;
    }

    public Dimension preferredSize()
    {
        return minimumSize();
    }

    public void DrawTabs(Graphics g)
    {
        int i = size().width;
        int k = size().height;
        int l = k - (bsize + 4);
        /*****Top Background*********/
        g.setColor(chatPeer.ColorMap[0]);
        g.fillRect(0, 0, i, k);
        g.setFont(parent.textFont);
        fontmetrics = g.getFontMetrics();
        fontmetrics.getHeight();
        int i1 = parent.xofs + 1;
        byte byte0 = 7;
        g.setColor(chatPeer.ColorMap[4]);
        g.fillRect(0, l, i, k);
        g.drawLine(0, l, 0, k + 1);
        g.fillRect(2, l + 1, bsize, k);
        g.fillRect(bsize + 2, l + 1, i - bsize, bsize);
        g.setColor(chatPeer.ColorMap[4]);
        g.drawLine(1, l, 1, k + 1);
        g.drawLine(1, l, i - 3, l);
        g.setColor(Color.gray);
        g.drawLine(bsize + 2, l + bsize + 2, i - (bsize + 2), l + bsize + 2);
        g.drawLine(i - 2, l, i - 2, k + 1);
        g.setColor(chatPeer.ColorMap[4]);
        g.drawLine(bsize + 3, l + bsize + 3, i - (bsize + 3), l + bsize + 3);
        g.drawLine(i - 1, l, i - 1, k + 1);
        parent.tabPos.removeAllElements();
        for(int j1 = 0; j1 < parent.tabNames.size(); j1++)
        {
            String s = (String)parent.tabNames.elementAt(j1);
            Rectangle rectangle1 = new Rectangle();
            int j = fontmetrics.stringWidth(s);
            rectangle1.x = i1;
            rectangle1.y = byte0 + 1;
            rectangle1.width = TAPPANEL_WIDTH / TAP_COUNT;
            rectangle1.height = tabHeight;
            parent.tabPos.addElement(rectangle1);
            DrawTab(g, rectangle1.x, rectangle1.y, rectangle1.width, rectangle1.height, false, s);
            i1 += rectangle1.width;
        }

        Rectangle rectangle = (Rectangle)parent.tabPos.elementAt(parent.curTab);
        DrawTab(g, rectangle.x, rectangle.y - 4, rectangle.width + 2, rectangle.height + 5, true, (String)parent.tabNames.elementAt(parent.curTab));
    }

    public void DrawHorizontal(Graphics g)
    {
        g.setColor(chatPeer.ColorMap[4]);
        g.fillRect(0, 0, width, height);
        g.setColor(chatPeer.ColorMap[4]);
        int i = width - 8;
        g.drawLine(0, 1, i, 1);
        g.fillRect(i, 0, 1, 1);
        g.setColor(chatPeer.ColorMap[4]);
        g.drawLine(6, 0, 6, 0);
        g.drawLine(1, 6, width - 2, 6);
        g.drawLine(width - 2, 0, width - 2, height - 2);
        g.setColor(chatPeer.ColorMap[4]);
        g.drawLine(1, 7, width - 1, 7);
        g.drawLine(width - 1, 0, width - 1, height - 1);
    }

    int mode;
    int width;
    int height;
    int size;
    int bsize;
    Dimension dim;
    BorderPanel parent;
    TapPanel Cframe;
    Color c1;
    int tabHeight;
	ChatPeer chatPeer;
	FontMetrics fontmetrics;
}

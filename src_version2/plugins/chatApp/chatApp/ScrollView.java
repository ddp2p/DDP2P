//package com.jeeva.chatclient;
package chatApp;

import java.awt.*;

public class ScrollView extends Panel
{

    public void reset()
    {
        if(HorScroll != null)
            HorScroll.setViewArea(0, 0);
        if(VerScroll != null)
            VerScroll.setViewArea(0, 0);
    }

    public ScrollView(Component component, boolean flag, boolean flag1, int i, int j, int k)
    {
        Item = component;
        SbSize = k;
        HorScroll = VerScroll = null;
        if(flag)
        {
            if(flag1)
                HorScroll = new ScrollBar(1, this, i - SbSize, SbSize);
            else
                HorScroll = new ScrollBar(1, this, i, SbSize);
            HorScroll.setScrollValue(0, 10, 2);
        }
        if(flag1)
        {
            if(flag)
                VerScroll = new ScrollBar(2, this, SbSize, j - SbSize);
            else
                VerScroll = new ScrollBar(2, this, SbSize, j);
            VerScroll.setScrollValue(0, 10, 2);
        }
        setBackground(Color.lightGray);
        GridBagLayout gridbaglayout = new GridBagLayout();
        GridBagConstraints gridbagconstraints = new GridBagConstraints();
        setLayout(gridbaglayout);
        gridbagconstraints.fill = 1;
        if(!flag1)
            gridbagconstraints.gridwidth = 0;
        gridbagconstraints.weightx = 1.0D;
        gridbagconstraints.weighty = 1.0D;
        gridbaglayout.setConstraints(Item, gridbagconstraints);
        add(Item);
        if(flag1)
        {
            gridbagconstraints.fill = 3;
            gridbagconstraints.weightx = 0.0D;
            gridbagconstraints.weighty = 1.0D;
            gridbagconstraints.gridwidth = 0;
            gridbaglayout.setConstraints(VerScroll, gridbagconstraints);
            add(VerScroll);
        }
        if(flag)
        {
            gridbagconstraints.fill = 2;
            gridbagconstraints.weightx = 1.0D;
            gridbagconstraints.weighty = 0.0D;
            gridbagconstraints.gridwidth = 1;
            gridbaglayout.setConstraints(HorScroll, gridbagconstraints);
            add(HorScroll);
        }
        Dim = new Dimension(i, j);
        resize(Dim);
        validate();
    }

    public void setValues(int i, int j)
    {
        ContentWidth = i;
        ContentHeight = j;
        if(HorScroll != null)
            HorScroll.setViewArea(ContentWidth, Item.size().width - 5);
        if(VerScroll != null)
            VerScroll.setViewArea(ContentHeight, Item.size().height - 5);
    }

    public Dimension minimumSize()
    {
        return Dim;
    }

    public void setScrollSteps(int i, int j, int k)
    {
        if(i == 1 && HorScroll != null)
        {
            HorScroll.setScrollValue(HorScroll.ScrollValue, j, k);
            return;
        }
        if(i == 2 && VerScroll != null)
            VerScroll.setScrollValue(VerScroll.ScrollValue, j, k);
    }

    public void setScrollPos(int i, int j)
    {
        if(i == 1 && HorScroll != null)
        {
            HorScroll.setScrollPos(j);
            return;
        }
        if(i == 2 && VerScroll != null)
            VerScroll.setScrollPos(j);
    }

    public Dimension preferredSize()
    {
        return size();
    }

    public boolean handleEvent(Event event)
    {
        if((event.target == HorScroll || event.target == VerScroll) && event.id == 1001)
        {
            byte byte0 = 1;
            if(event.target == VerScroll)
                byte0 = 2;
            Event event1 = new Event(event.target, 0L, event.id, 0, 0, event.key, byte0, this);
            Item.deliverEvent(event1);
            return true;
        } else
        {
            return super.handleEvent(event);
        }
    }

    public void layout()
    {
        super.layout();
        setValues(ContentWidth, ContentHeight);
    }

    Component Item;
    public ScrollBar HorScroll;
    public ScrollBar VerScroll;
    boolean HsbDisabled;
    boolean VsbDisabled;
    int SbSize;
    int ContentWidth;
    int ContentHeight;
    public Dimension Dim;
}

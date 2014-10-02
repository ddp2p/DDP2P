//package com.jeeva.chatclient;
package dd_p2p.plugin;

import java.awt.Panel;
import java.awt.Dimension;

class CustomPanel extends Panel
{

    public CustomPanel(int i, int j)
    {
        dimension = new Dimension(i, j);
        resize(dimension);
        validate();
    }

    public Dimension minimumSize()
    {
        return dimension;
    }

    public Dimension preferredSize()
    {
        return size();
    }

	/*************Global Variable Declarations**********/
    public Dimension dimension;
}

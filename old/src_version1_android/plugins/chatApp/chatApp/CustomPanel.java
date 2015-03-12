//package com.jeeva.chatclient;
package chatApp;

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

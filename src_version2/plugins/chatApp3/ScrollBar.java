//package com.jeeva.chatclient;
package dd_p2p.plugin;

import java.awt.*;

public class ScrollBar extends Canvas
    implements Runnable
{

    public boolean mouseMove(Event event, int i, int j)
    {
        if(Disabled)
        {
            return true;
        } else
        {
            LastX = i;
            LastY = j;
            return true;
        }
    }

    public void drawDownScroller(Graphics g, int i, int j, int k, int l)
    {
        g.setColor(Colors[4]);
        g.fillRect(i, j, k, l);
        g.setColor(Colors[3]);
        g.drawRect(i, j, k - 1, l - 1);
    }

    public boolean mouseEnter(Event event, int i, int j)
    {
        return true;
    }

    public void setViewArea(int i, int j)
    {
        TotalArea = i;
        ViewArea = j;
        if(i <= j)
        {
            Disabled = true;
            setScrollPos(1);
            Event event = new Event(this, 0L, 1001, 0, 0, 0, 5, null);
            Parent.deliverEvent(event);
            return;
        } else
        {
            Disabled = false;
            calcValues();
            repaint();
            return;
        }
    }

    public boolean mouseExit(Event event, int i, int j)
    {
        return true;
    }

    public void GetVerValue()
    {
        int i = size().height - (Scroller.height + BsizeY * 2);
        double d = (double)(Scroller.y - BsizeY) / (double)i;
        int j = TotalArea - ViewArea;
        double d1 = (double)j * d;
        ScrollValue = (int)d1;
    }

    ScrollBar(int i, Component component, int j, int k)
    {
        Graph = null;
        Img = null;
        sleeptime = 100;
        Type = i;
        Parent = component;
        Width = j;
        Height = k;
        Scroller = new Rectangle(0, 0, 0, 0);
        Colors = new Color[10];
        Colors[0] = new Color(223, 223, 223);
        Colors[1] = Color.black;
        Colors[2] = Color.white;
        Colors[3] = Color.gray;
        Colors[4] = Color.lightGray;
        Xpnt = new int[3];
        Ypnt = new int[3];
        if(i == 1)
        {
            BsizeY = k;
            BsizeX = BsizeY;
            Scroller.x = BsizeX;
        } else
        {
            BsizeX = j;
            BsizeY = BsizeX;
            Scroller.y = BsizeY;
        }
        Down = Down1 = Down2 = false;
        Disabled = true;
        Dim = new Dimension(j, k);
        resize(Dim);
        validate();
    }

    public void paint(Graphics g)
    {
        int i = size().width;
        int j = size().height;
        if(i <= 0 || j <= 0)
            return;
        if(Graph == null || i != Dwidth || j != Dheight)
        {
            Dwidth = i;
            Dheight = j;
            Img = createImage(i, j);
            if(Img != null)
            {
                if(Graph != null)
                    Graph.dispose();
                Graph = Img.getGraphics();
            }
        }
        if(Graph != null && Img != null)
        {
            drawScrollBar(Graph, 0, 0, i, j);
            g.drawImage(Img, 0, 0, this);
        }
    }

    void doLineUp()
    {
        if(ScrollValue <= 0)
            return;
        Down = true;
        Down1 = true;
        ScrollValue -= LineValue;
        int i = 0;
        if(ScrollValue < i)
            ScrollValue = i;
        if(Type == 1)
        {
            int j = size().width - 2 * BsizeX;
            int l = (LineValue * j) / TotalArea;
            if(l <= 0)
                l = 1;
            int j1 = BsizeX;
            Scroller.x -= l;
            if(Scroller.x < j1)
                Scroller.x = j1;
            GetHorValue();
        } else
        {
            int k = size().height - 2 * BsizeY;
            int i1 = (LineValue * k) / TotalArea;
            if(i1 <= 0)
                i1 = 1;
            int k1 = BsizeY;
            Scroller.y -= i1;
            if(Scroller.y < k1)
                Scroller.y = k1;
            GetVerValue();
        }
        repaint();
        if(ScrollValue != LastValue)
        {
            Event event = new Event(this, 0L, 1001, 0, 0, ScrollValue, 1, null);
            Parent.deliverEvent(event);
            LastValue = ScrollValue;
        }
    }

    public boolean mouseUp(Event event, int i, int j)
    {
        if(runner != null && running)
            runner.stop();
        runner = null;
        running = false;
        if(Disabled)
            return true;
        Down = false;
        if(Down1 || Down2)
            Down1 = Down2 = false;
        repaint();
        return true;
    }

    void calcValues()
    {
        int j = TotalArea - ViewArea;
        int i;
        if(Type == 1)
            i = size().width - 2 * BsizeX;
        else
            i = size().height - 2 * BsizeY;
        double d = 1.0D - (double)j / (double)TotalArea;
        double d1 = d * (double)i;
        if(Type == 1)
        {
            Scroller.width = (int)d1;
            if(Scroller.width < 8)
                Scroller.width = 8;
            Scroller.height = size().height;
            return;
        }
        Scroller.width = size().width;
        Scroller.height = (int)d1;
        if(Scroller.height < 8)
            Scroller.height = 8;
    }

    public Dimension minimumSize()
    {
        return Dim;
    }

    public void drawArrow(Graphics g, int i, int j, int k, int l, int i1)
    {
        int j1 = (int)Math.round((double)l / 2D);
        int k1 = (int)Math.round((double)i1 / 2D);
        switch(i)
        {
        case 1: // '\001'
            Xpnt[0] = (j + l) - 1;
            Ypnt[0] = k;
            Xpnt[1] = (j + l) - 1;
            Ypnt[1] = (k + i1) - 1;
            Xpnt[2] = j;
            Ypnt[2] = (k + k1) - 1;
            break;

        case 2: // '\002'
            Xpnt[0] = j;
            Ypnt[0] = k;
            Xpnt[1] = j;
            Ypnt[1] = (k + i1) - 1;
            Xpnt[2] = (j + l) - 1;
            Ypnt[2] = (k + k1) - 1;
            break;

        case 3: // '\003'
            Xpnt[0] = j;
            Ypnt[0] = (k + i1) - 1;
            Xpnt[1] = (j + l) - 1;
            Ypnt[1] = (k + i1) - 1;
            Xpnt[2] = (j + j1) - 1;
            Ypnt[2] = k;
            break;

        case 4: // '\004'
            Xpnt[0] = j;
            Ypnt[0] = k;
            Xpnt[1] = (j + l) - 1;
            Ypnt[1] = k;
            Xpnt[2] = (j + j1) - 1;
            Ypnt[2] = (k + i1) - 1;
            break;
        }
        if(Disabled)
            g.setColor(Colors[0]);
        else
            g.setColor(Colors[1]);
        g.fillPolygon(Xpnt, Ypnt, 3);
    }

    public void update(Graphics g)
    {
        paint(g);
    }

    public void GetHorValue()
    {
        int i = size().width - (Scroller.width + BsizeX * 2);
        double d = (double)(Scroller.x - BsizeX) / (double)i;
        int j = TotalArea - ViewArea;
        double d1 = (double)j * d;
        ScrollValue = (int)d1;
    }

    void doPageUp()
    {
        Down = true;
        Graphics g = getGraphics();
        g.setColor(Colors[3]);
        ScrollValue -= PageValue;
        int i = 0;
        if(ScrollValue < i)
            ScrollValue = i;
        if(Type == 1)
        {
            int j = size().width - 2 * BsizeX;
            int l = (PageValue * j) / TotalArea;
            int j1 = BsizeX;
            int l1 = j1;
            g.fillRect(l1, 0, Scroller.x - BsizeX, size().height - 1);
            Scroller.x -= l;
            if(Scroller.x < j1)
                Scroller.x = j1;
        } else
        {
            int k = size().height - 2 * BsizeY;
            int i1 = (PageValue * k) / TotalArea;
            int k1 = BsizeY;
            int i2 = k1;
            g.fillRect(0, i2, size().width - 1, Scroller.y - BsizeY);
            Scroller.y -= i1;
            if(Scroller.y < k1)
                Scroller.y = k1;
        }
        Event event = new Event(this, 0L, 1001, 0, 0, ScrollValue, 3, null);
        Parent.deliverEvent(event);
        g.dispose();
    }

    public void setScrollPos(int i)
    {
        if(i == 1)
        {
            int j = 0;
            ScrollValue = j;
            if(Type == 1)
            {
                int l = BsizeX;
                Scroller.x = l;
            } else
            {
                int i1 = BsizeY;
                Scroller.y = i1;
            }
        } else
        {
            int k = TotalArea - ViewArea;
            ScrollValue = k;
            if(Type == 1)
            {
                int j1 = size().width - (Scroller.width + BsizeX);
                Scroller.x = j1;
            } else
            {
                int k1 = size().height - (Scroller.height + BsizeY);
                Scroller.y = k1;
            }
        }
        repaint();
    }

    void start(Event event)
    {
        lastMouseDown = event;
        if(!running && runner == null)
        {
            runner = new Thread(this);
            running = true;
            runner.start();
        }
    }

    public void enable()
    {
        super.enable();
        calcValues();
        Disabled = false;
        if(Graph != null)
        {
            Graph.dispose();
            Graph = null;
        }
        setScrollPos(1);
        doLineDown();
        doLineUp();
        repaint();
    }

    public void drawScrollBar(Graphics g, int i, int j, int k, int l)
    {
        g.setColor(Colors[0]);
        g.fillRect(i, j, k, l);
        if(Type == 1)
        {
            int k1 = BsizeX;
            int i1 = size().width - (Scroller.width + BsizeX);
            if(Scroller.x < k1)
                Scroller.x = k1;
            if(Scroller.x > i1)
                Scroller.x = i1;
        } else
        {
            int l1 = BsizeY;
            int j1 = size().height - (Scroller.height + BsizeY);
            if(Scroller.y < l1)
                Scroller.y = l1;
            if(Scroller.y > j1)
                Scroller.y = j1;
        }
        if(Type == 1)
        {
            if(Down1)
                drawDownScroller(g, i, j, BsizeX, BsizeY);
            else
                drawScroller(g, i, j, BsizeX, BsizeY);
            drawArrow(g, 1, i + 3, j + 3, BsizeX - 6, BsizeY - 6);
            int i2 = (i + k) - BsizeX;
            if(Down2)
                drawDownScroller(g, i2, j, BsizeX, BsizeY);
            else
                drawScroller(g, i2, j, BsizeX, BsizeY);
            drawArrow(g, 2, i2 + 3, j + 3, BsizeX - 6, BsizeY - 6);
        } else
        {
            if(Down1)
                drawDownScroller(g, i, j, BsizeX, BsizeY);
            else
                drawScroller(g, i, j, BsizeX, BsizeY);
            drawArrow(g, 3, i + 3, j + 3, BsizeX - 6, BsizeY - 6);
            int j2 = (j + l) - BsizeY;
            if(Down2)
                drawDownScroller(g, i, j2, BsizeX, BsizeY);
            else
                drawScroller(g, i, j2, BsizeX, BsizeY);
            drawArrow(g, 4, i + 4, j2 + 4, BsizeX - 7, BsizeY - 7);
        }
        Down1 = Down2 = false;
        if(!Disabled)
            drawScroller(g, Scroller.x, Scroller.y, Scroller.width, Scroller.height);
    }

    void doLineDown()
    {
        int i = TotalArea - ViewArea;
        if(ScrollValue >= i)
            return;
        Down = true;
        Down2 = true;
        ScrollValue += LineValue;
        if(ScrollValue > i)
            ScrollValue = i;
        if(Type == 1)
        {
            int j = size().width - 2 * BsizeX;
            int l = (LineValue * j) / TotalArea;
            if(l <= 0)
                l = 1;
            int j1 = size().width - (Scroller.width + BsizeX);
            Scroller.x += l;
            if(Scroller.x > j1)
                Scroller.x = j1;
            GetHorValue();
        } else
        {
            int k = size().height - 2 * BsizeY;
            int i1 = (LineValue * k) / TotalArea;
            if(i1 <= 0)
                i1 = 1;
            int k1 = size().height - (Scroller.height + BsizeY);
            Scroller.y += i1;
            if(Scroller.y > k1)
                Scroller.y = k1;
            GetVerValue();
        }
        repaint();
        if(ScrollValue != LastValue)
        {
            Event event = new Event(this, 0L, 1001, 0, 0, ScrollValue, 2, null);
            Parent.deliverEvent(event);
            LastValue = ScrollValue;
        }
    }

    public Dimension preferredSize()
    {
        return minimumSize();
    }

    public void drawScroller(Graphics g, int i, int j, int k, int l)
    {
        g.setColor(Colors[4]);
        g.fillRect(i, j, k, l);
        g.setColor(Colors[0]);
        g.drawLine(i, j, (i + k) - 1, j);
        g.drawLine(i, j, i, (j + l) - 1);
        g.setColor(Colors[2]);
        g.drawLine(i + 1, j + 1, (i + k) - 4, j + 1);
        g.drawLine(i + 1, j + 1, i + 1, (j + l) - 4);
        g.setColor(Colors[3]);
        g.drawLine((i + k) - 2, j + 1, (i + k) - 2, (j + l) - 2);
        g.drawLine(i + 1, (j + l) - 2, (i + k) - 2, (j + l) - 2);
        g.setColor(Colors[1]);
        g.drawLine((i + k) - 1, j, (i + k) - 1, (j + l) - 1);
        g.drawLine(i, (j + l) - 1, (i + k) - 1, (j + l) - 1);
    }

    public boolean mouseDown(Event event, int i, int j)
    {
        if(Disabled)
            return true;
        if(Type == 1)
        {
            if(i >= 0 && i <= BsizeX)
            {
                doLineUp();
                start(event);
            } else
            if(i >= size().width - BsizeX && i <= size().width)
            {
                doLineDown();
                start(event);
            } else
            if(i < Scroller.x)
            {
                doPageUp();
                start(event);
            } else
            if(i > Scroller.x + Scroller.width)
            {
                doPageDown();
                start(event);
            }
        } else
        if(j >= 0 && j <= BsizeY)
        {
            doLineUp();
            start(event);
        } else
        if(j >= size().height - BsizeY && j <= size().height)
        {
            doLineDown();
            start(event);
        } else
        if(j < Scroller.y)
        {
            doPageUp();
            start(event);
        } else
        if(j > Scroller.y + Scroller.height)
        {
            doPageDown();
            start(event);
        }
        return true;
    }

    public void run()
    {
        do
        {
            try
            {
                Thread.sleep(sleeptime);
            }
            catch(Exception _ex) { }
            postEvent(lastMouseDown);
            repaint();
        } while(true);
    }

    public void setScrollValue(int i, int j, int k)
    {
        int l = TotalArea - ViewArea;
        if(i < 0 || i > l)
            ScrollValue = 0;
        else
            ScrollValue = i;
        PageValue = j;
        LineValue = k;
        repaint();
    }

    public boolean mouseDrag(Event event, int i, int j)
    {
        if(Disabled || Down)
            return true;
        if(Type == 1)
        {
            Scroller.x += i - LastX;
            int k = size().width - (Scroller.width + BsizeX);
            int i1 = BsizeX;
            if(Scroller.x + Scroller.width > size().width - BsizeX)
                Scroller.x = k;
            if(Scroller.x < i1)
                Scroller.x = i1;
            GetHorValue();
        } else
        {
            Scroller.y += j - LastY;
            int l = size().height - (Scroller.height + BsizeY);
            int j1 = BsizeY;
            if(Scroller.y + Scroller.height > size().height - BsizeY)
                Scroller.y = l;
            if(Scroller.y < j1)
                Scroller.y = j1;
            GetVerValue();
        }
        LastX = i;
        LastY = j;
        repaint();
        if(ScrollValue != LastValue)
        {
            Event event1 = new Event(this, 0L, 1001, 0, 0, ScrollValue, 5, null);
            Parent.deliverEvent(event1);
            LastValue = ScrollValue;
        }
        return true;
    }

    void doPageDown()
    {
        Down = true;
        Graphics g = getGraphics();
        g.setColor(Colors[3]);
        ScrollValue += PageValue;
        int i = TotalArea - ViewArea;
        if(ScrollValue > i)
            ScrollValue = i;
        if(Type == 1)
        {
            int j = size().width - 2 * BsizeX;
            int l = (PageValue * j) / TotalArea;
            int j1 = Scroller.x + Scroller.width;
            g.fillRect(j1, 0, size().width - j1 - BsizeX, size().height - 1);
            Scroller.x += l;
            int l1 = size().width - (Scroller.width + BsizeX);
            if(Scroller.x > l1)
                Scroller.x = l1;
        } else
        {
            int k = size().height - 2 * BsizeY;
            int i1 = (PageValue * k) / TotalArea;
            int k1 = Scroller.y + Scroller.height;
            g.fillRect(0, k1, size().width - 1, size().height - k1 - BsizeY);
            Scroller.y += i1;
            int i2 = size().height - (Scroller.height + BsizeY);
            if(Scroller.y > i2)
                Scroller.y = i2;
        }
        Event event = new Event(this, 0L, 1001, 0, 0, ScrollValue, 4, null);
        Parent.deliverEvent(event);
        g.dispose();
    }

    public final int HORIZONTAL = 1;
    public final int VERTICAL = 2;
    public final int MINPOS = 1;
    public final int MAXPOS = 2;
    Color Colors[];
    Component Parent;
    int Xpnt[];
    int Ypnt[];
    Rectangle Scroller;
    Dimension Dim;
    Graphics Graph;
    Image Img;
    boolean Down;
    boolean Down1;
    boolean Down2;
    int Dwidth;
    int Dheight;
    int LastValue;
    int LastX;
    int LastY;
    int BsizeX;
    int BsizeY;
    int Width;
    int Height;
    int Type;
    public boolean Disabled;
    public int ScrollValue;
    public int TotalArea;
    public int ViewArea;
    public int PageValue;
    public int LineValue;
    boolean running;
    Thread runner;
    Event lastMouseDown;
    int sleeptime;
}

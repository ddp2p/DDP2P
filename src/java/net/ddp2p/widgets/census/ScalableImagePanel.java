package net.ddp2p.widgets.census;
import java.awt.Graphics;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
public class ScalableImagePanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
    private Image image;
    private Image scaledImage;
    private int imageWidth = 0;
    private int imageHeight = 0;
    public ScalableImagePanel() {
        super();
    }
    public void loadImage(String file) throws IOException {
		image = net.ddp2p.widgets.app.DDIcons.getImageFromResource(file,null);
		if (image != null) {
	        imageWidth = image.getWidth(this);
	        imageHeight = image.getHeight(this);
	        setScaledImage();
		}
    }
    public void scaleImage() {
        setScaledImage();
    }
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if ( scaledImage != null ) {
            g.drawImage(scaledImage, 0, 0, this);
        }
    }
    private void setScaledImage() {
        if ( image != null ) {
            float iw = imageWidth;
            float ih = imageHeight;
            float pw = this.getWidth();   
            float ph = this.getHeight();  
            if ( pw < iw || ph < ih ) {
                if ( (pw / ph) > (iw / ih) ) {
                    iw = -1;
                    ih = ph;
                } else {
                    iw = pw;
                    ih = -1;
                }
                if (iw == 0) {
                    iw = -1;
                }
                if (ih == 0) {
                    ih = -1;
                }
                scaledImage = image.getScaledInstance(
                            new Float(iw).intValue(), new Float(ih).intValue(), Image.SCALE_DEFAULT);
            } else {
                scaledImage = image;
            }
        }
    }
}

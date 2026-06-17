import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GeneratePlayStoreIcon {
    public static void main(String[] args) throws Exception {
        File output = args.length > 0
            ? new File(args[0])
            : new File("store-assets/play-store-icon.png");
        output.getParentFile().mkdirs();

        BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g.setColor(new Color(0x0F766E));
        g.fillRect(0, 0, 512, 512);

        g.setComposite(AlphaComposite.SrcOver.derive(0.72f));
        g.setColor(new Color(0x14B8A6));
        Path2D accent = new Path2D.Double();
        accent.moveTo(0, 356);
        accent.lineTo(512, 184);
        accent.lineTo(512, 512);
        accent.lineTo(0, 512);
        accent.closePath();
        g.fill(accent);

        g.setComposite(AlphaComposite.SrcOver.derive(0.92f));
        g.setColor(new Color(0xFBBF24));
        g.fill(new Ellipse2D.Double(348, 50, 96, 96));

        g.setComposite(AlphaComposite.SrcOver.derive(0.14f));
        g.setColor(Color.WHITE);
        g.fillRect(84, 84, 344, 18);

        g.setComposite(AlphaComposite.SrcOver.derive(0.10f));
        Path2D stripe = new Path2D.Double();
        stripe.moveTo(54, 424);
        stripe.lineTo(360, 318);
        stripe.lineTo(368, 336);
        stripe.lineTo(62, 442);
        stripe.closePath();
        g.fill(stripe);

        g.setComposite(AlphaComposite.SrcOver.derive(0.18f));
        g.setColor(Color.BLACK);
        g.fill(new RoundRectangle2D.Double(142, 122, 228, 280, 30, 30));

        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(132, 112, 228, 280, 30, 30));

        g.setColor(new Color(0xECFEFF));
        g.fill(new RoundRectangle2D.Double(162, 174, 168, 172, 18, 18));

        g.setColor(new Color(0xFBBF24));
        g.fill(new RoundRectangle2D.Double(188, 74, 116, 76, 26, 26));

        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(216, 106, 60, 18, 9, 9));

        g.setColor(new Color(0x0F766E));
        g.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(198, 226, 298, 226);
        g.drawLine(198, 276, 284, 276);

        g.setColor(new Color(0xF97316));
        g.setStroke(new BasicStroke(24f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D check = new Path2D.Double();
        check.moveTo(198, 324);
        check.lineTo(238, 360);
        check.lineTo(314, 268);
        g.draw(check);

        g.dispose();
        ImageIO.write(image, "png", output);
    }
}

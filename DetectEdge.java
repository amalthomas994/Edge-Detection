import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;

// Main class
public class DetectEdge extends Frame implements ActionListener {
	BufferedImage input;
	int width, height;
	int lowT=20, highT=100;
	CanvasImage source, target;
	CheckboxGroup metrics = new CheckboxGroup();
	// Constructor
	public DetectEdge(String name) {
		super("Edge Detection - Amal Thomas - 1202009");
		// load image
		try {
			input = ImageIO.read(new File(name));
		}
		catch ( Exception ex ) {
			ex.printStackTrace();
		}
		width = input.getWidth();
		height = input.getHeight();
		// prepare the panel for image canvas.
		Panel main = new Panel();
		source = new CanvasImage(input);
		target = new CanvasImage(width, height);
		main.setLayout(new GridLayout(1, 2, 10, 10));
		main.add(source);
		main.add(target);
		// prepare the panel for buttons.
		Panel controls = new Panel();
		Button button = new Button("DoG_x");
		button.addActionListener(this);
		controls.add(button);
		button = new Button("DoG_y");
		button.addActionListener(this);
		controls.add(button);
		button = new Button("Grad Mag");
		button.addActionListener(this);
		controls.add(button);
		button = new Button("Grad Dir");
		button.addActionListener(this);
		controls.add(button);
		button = new Button("Non-max Suppression");
		button.addActionListener(this);
		controls.add(button);

		JLabel label1 = new JLabel("lowT=" + lowT);
		label1.setPreferredSize(new Dimension(60, 20));
		controls.add(label1);
		JSlider slider1 = new JSlider(1, 128, lowT);
		slider1.setPreferredSize(new Dimension(75, 20));
		controls.add(slider1);
		slider1.addChangeListener(changeEvent -> {
			lowT = slider1.getValue();
			label1.setText("lowT=" + lowT);
		});
		JLabel label2 = new JLabel("highT=" + highT);
		label2.setPreferredSize(new Dimension(60, 20));
		controls.add(label2);
		JSlider slider2 = new JSlider(1, 128, highT);
		slider2.setPreferredSize(new Dimension(75, 20));
		controls.add(slider2);
		slider2.addChangeListener(changeEvent -> {
			highT = slider2.getValue();
			label2.setText("highT=" + highT);
		});

		button = new Button("Thresholding");
		button.addActionListener(this);
		controls.add(button);
		button = new Button("Hysteresis Tracking");
		button.addActionListener(this);
		controls.add(button);
		// add two panels
		add("Center", main);
		add("South", controls);
		addWindowListener(new ExitListener());
		pack();
		setVisible(true);
	}
	class ExitListener extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			System.exit(0);
		}
	}
	// Action listener for button click events
	public void actionPerformed(ActionEvent e) {
		// generate Moravec corner detection result
		if ( ((Button)e.getSource()).getLabel().equals("DoG_x") )
			target.resetImage(derivatives_x());
		if ( ((Button)e.getSource()).getLabel().equals("DoG_y") )
			target.resetImage(derivatives_y());
		if ( ((Button)e.getSource()).getLabel().equals("Grad Mag") )
			target.resetImage(grad_mag(derivatives_x(), derivatives_y()));
		if ( ((Button)e.getSource()).getLabel().equals("Grad Dir") )
			target.resetImage(grad_dir(derivatives_x(), derivatives_y()));
		if ( ((Button)e.getSource()).getLabel().equals("Non-max Suppression") )
			target.resetImage(non_max_suppression(grad_mag(derivatives_x(), derivatives_y()), grad_dir(derivatives_x(), derivatives_y())));
	}

	public BufferedImage derivatives_x() {
		int l, r, dr, dg, db;
		Color clr1, clr2;
		BufferedImage t = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				l = p==0 ? p : p-1;
				r = p==width-1 ? p : p+1;
				clr1 = new Color(source.image.getRGB(l,q));
				clr2 = new Color(source.image.getRGB(r,q));
				dr = clr2.getRed() - clr1.getRed();
				dg = clr2.getGreen() - clr1.getGreen();
				db = clr2.getBlue() - clr1.getBlue();
				dr = Math.max(0, Math.min(dr+128, 255));
				dg = Math.max(0, Math.min(dg+128, 255));
				db = Math.max(0, Math.min(db+128, 255));
				t.setRGB(p, q, new Color(dr, dg, db).getRGB());
			}
		}
		return t;
	}
	
	public BufferedImage derivatives_y() {
		int l, r, dr, dg, db;
		Color clr1, clr2;
		BufferedImage t = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		for ( int p=0 ; p<width ; p++ ) {
			for ( int q=0 ; q<height ; q++ ) {
				l = q==0 ? q : q-1;
				r = q==height-1 ? q : q+1;
				// System.out.println("q: " + q + " p: " + p + " l: " + l + " r: " + r);

				clr1 = new Color(source.image.getRGB(p,l));
				clr2 = new Color(source.image.getRGB(p,r));
				dr = clr2.getRed() - clr1.getRed();
				dg = clr2.getGreen() - clr1.getGreen();
				db = clr2.getBlue() - clr1.getBlue();
				dr = Math.max(0, Math.min(dr+128, 255));
				dg = Math.max(0, Math.min(dg+128, 255));
				db = Math.max(0, Math.min(db+128, 255));
				t.setRGB(p, q, new Color(dr, dg, db).getRGB());
			}
		}
		return t;
	}

	public BufferedImage grad_mag(BufferedImage GoD_x, BufferedImage GoD_y){
		int l, r, dr, dg, db;
		Color GoD_x_color, GoD_y_color;
		BufferedImage t = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				l = p==0 ? p : p-1;
				r = p==width-1 ? p : p+1;
				GoD_x_color = new Color(GoD_x.getRGB(p,q));
				GoD_y_color = new Color(GoD_y.getRGB(p,q));
				double magnitude_r = Math.sqrt(Math.pow(GoD_x_color.getRed(), 2) + Math.pow(GoD_y_color.getRed(), 2));
				double magnitude_g = Math.sqrt(Math.pow(GoD_x_color.getGreen(), 2) + Math.pow(GoD_y_color.getGreen(), 2));
				double magnitude_b = Math.sqrt(Math.pow(GoD_x_color.getBlue(), 2) + Math.pow(GoD_y_color.getBlue(), 2));
				int rr = (int) Math.min(magnitude_r, 255);
				int gg = (int) Math.min(magnitude_g, 255);
				int bb = (int) Math.min(magnitude_b, 255);
				// System.out.println("Something Happening");
				t.setRGB(p, q, new Color(rr, gg, bb).getRGB());

			}
		}
		return t;
	}

	public BufferedImage grad_dir(BufferedImage GoD_x, BufferedImage GoD_y){
		int l, r, dr, dg, db;
		Color GoD_x_color, GoD_y_color;
		BufferedImage t = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				l = p==0 ? p : p-1;
				r = p==width-1 ? p : p+1;
				GoD_x_color = new Color(GoD_x.getRGB(p,q));
				GoD_y_color = new Color(GoD_y.getRGB(p,q));
				double angle_r = Math.atan((double) GoD_y_color.getRed()/(double) GoD_x_color.getRed());
				double angle_g = Math.atan((double) GoD_y_color.getGreen()/(double) GoD_x_color.getGreen());
				double angle_b = Math.atan((double) GoD_y_color.getBlue()/(double) GoD_x_color.getBlue());
				// System.out.println("R: " + angle_r + " G: " + angle_g + " B: " + angle_b);
				// int rr = (int) Math.min(magnitude_r, 255);
				// int gg = (int) Math.min(magnitude_g, 255);
				// int bb = (int) Math.min(magnitude_b, 255);
				// System.out.println("Something Happening");
				float r_ = (float) Math.min(angle_r, 1);
				float g_ = (float) Math.min(angle_g, 1);
				float b_ = (float) Math.min(angle_b, 1);
				// System.out.println("R: " + magnitude_r + " G: " + magnitude_g + " B: " + magnitude_b);
				
				t.setRGB(p, q, new Color(r_, g_, b_).getRGB());

			}
		}
		return t;
	}

	public BufferedImage non_max_suppression(BufferedImage grad_mag, BufferedImage grad_dir){
		int l, r, dr, dg, db;
		Color grad_mag_color, grad_mag_color_1, grad_mag_color_2, grad_dir_color, grad_dir_color_1, grad_dit_color_2;
		BufferedImage t = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				l = p==0 ? p : p-1;
				r = p==width-1 ? p : p+1;
				grad_mag_color_1 = new Color(grad_mag.getRGB(p,q));
				grad_mag_color = new Color(grad_mag.getRGB(p,q));
				grad_mag_color_2 = new Color(grad_mag.getRGB(p,q));

				grad_dir_color = new Color(grad_dir.getRGB(p,q));

				double grad_dir_angle_r = Math.toDegrees(grad_dir_color.getRed()/255);
				double grad_dir_angle_g = Math.toDegrees(grad_dir_color.getGreen()/255);
				double grad_dir_angle_b = Math.toDegrees(grad_dir_color.getBlue()/255);
				System.out.println("R: " + grad_dir_angle_r + " G: " + grad_dir_angle_g + " B: " + grad_dir_angle_b);


				double magnitude_r = Math.atan2((double) grad_mag_color.getRed(), (double) grad_mag_color.getRed());
				double magnitude_g = Math.atan2((double) grad_mag_color.getGreen(),(double) grad_mag_color.getGreen());
				double magnitude_b = Math.atan2((double) grad_mag_color.getBlue(), (double) grad_mag_color.getBlue());
				// System.out.println("R: " + magnitude_r + " G: " + magnitude_g + " B: " + magnitude_b);
				// int rr = (int) Math.min(magnitude_r, 255);
				// int gg = (int) Math.min(magnitude_g, 255);
				// int bb = (int) Math.min(magnitude_b, 255);
				// System.out.println("Something Happening");
				float r_ = (float) Math.min(magnitude_r, 1);
				float g_ = (float) Math.min(magnitude_g, 1);
				float b_ = (float) Math.min(magnitude_b, 1);
				// System.out.println("R: " + magnitude_r + " G: " + magnitude_g + " B: " + magnitude_b);
				
				t.setRGB(p, q, new Color(r_, g_, b_).getRGB());

			}
		}
		return t;
	}
	
	public static void main(String[] args) {
		new DetectEdge(args.length==1 ? args[0] : "Johnston_Hall.png");
	}
}

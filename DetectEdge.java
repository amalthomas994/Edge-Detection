import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;
import java.util.stream.IntStream;
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

		final BufferedImage blurredImage = approximationFilter(source.image);
		BufferedImage blurredImages = grayscale(blurredImage);

		JLabel label1 = new JLabel("lowT=" + lowT);
		label1.setPreferredSize(new Dimension(60, 20));
		controls.add(label1);
		JSlider slider1 = new JSlider(1, 128, lowT);
		slider1.setPreferredSize(new Dimension(75, 20));
		controls.add(slider1);
		slider1.addChangeListener(changeEvent -> {
			lowT = slider1.getValue();
			label1.setText("lowT=" + lowT);
			target.resetImage(non_max_suppression(grad_mag(derivatives_x(blurredImages), derivatives_y(blurredImages)), grad_dir(derivatives_x(blurredImages), derivatives_y(blurredImages))));

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
		BufferedImage blurredImage = approximationFilter(source.image);
		blurredImage = grayscale(blurredImage);

		if ( ((Button)e.getSource()).getLabel().equals("DoG_x") )
			//Paint the result of the intensity in the x direction
			target.resetImage(derivatives_x(blurredImage));
		if ( ((Button)e.getSource()).getLabel().equals("DoG_y") )
			//Paint the result of the intensity in the y direction
			target.resetImage(derivatives_y(blurredImage));
		if ( ((Button)e.getSource()).getLabel().equals("Grad Mag") )
			//Calculate the gradient magnitude by taking the intensity in the x and y direction and square rooting the squared sum of the intensities
			target.resetImage(grad_mag(derivatives_x(blurredImage), derivatives_y(blurredImage)));
		if ( ((Button)e.getSource()).getLabel().equals("Grad Dir") )
			target.resetImage(colorWheel(grad_dir(derivatives_x(blurredImage), derivatives_y(blurredImage))));
			// target.resetImage(grad_dir(derivatives_x(blurredImage), derivatives_y(blurredImage)));
			// source.resetImage(colorWheel(grad_dir(derivatives_x(blurredImage), derivatives_y(blurredImage))));
		if ( ((Button)e.getSource()).getLabel().equals("Non-max Suppression") )
			target.resetImage(non_max_suppression(grad_mag(derivatives_x(blurredImage), derivatives_y(blurredImage)), grad_dir(derivatives_x(blurredImage), derivatives_y(blurredImage))));
	}

	/*Function to get the intensity of the image in the x direction
		Input (BufferedImage): Original Image
		Output (BufferedImage): Intensity of input in the x direction. RGB values are offset by 128
	*/
	public BufferedImage derivatives_x(BufferedImage image) {
		int l, r, dr, dg, db;
		Color clr1, clr2;
		BufferedImage t = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				l = p==0 ? p : p-1;
				r = p==width-1 ? p : p+1;
				clr1 = new Color(image.getRGB(l,q));
				clr2 = new Color(image.getRGB(r,q));
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
	
	/*Function to get the intensity of the image in the y direction
		Input (BufferedImage): Original Image
		Output (BufferedImage): Intensity of input in the y direction. RGB values are offset by 128
	*/
	public BufferedImage derivatives_y(BufferedImage image) {
		int l, r, dr, dg, db;
		Color clr1, clr2;
		BufferedImage t = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		for ( int p=0 ; p<width ; p++ ) {
			for ( int q=0 ; q<height ; q++ ) {
				l = q==0 ? q : q-1;
				r = q==height-1 ? q : q+1;
				clr1 = new Color(image.getRGB(p,l));
				clr2 = new Color(image.getRGB(p,r));
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

	/*Function to get the gradient magnitude using the intensities in the x and y direction
		Input (BufferedImage, BufferedImage): Image intensity in the x direction, Image intensity in the y direction
		Output (BufferedImage): Gradient Magnitude of the image
		Gradient Magnitude Function: |G| = sqrt((G_x)^2 + (G_y)^2)
	*/
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
				double magnitude_r = Math.sqrt(Math.pow(GoD_x_color.getRed()-128, 2) + Math.pow(GoD_y_color.getRed()-128, 2));
				double magnitude_g = Math.sqrt(Math.pow(GoD_x_color.getGreen()-128, 2) + Math.pow(GoD_y_color.getGreen()-128, 2));
				double magnitude_b = Math.sqrt(Math.pow(GoD_x_color.getBlue()-128, 2) + Math.pow(GoD_y_color.getBlue()-128, 2));
				int rr = (int) Math.min(Math.max(0, magnitude_r), 255);
				int gg = (int) Math.min(Math.max(0, magnitude_g), 255);
				int bb = (int) Math.min(Math.max(0, magnitude_b), 255);
				// System.out.println("R: " + rr + " G: " + gg + " B: " + bb);
				t.setRGB(p, q, new Color(rr, gg, bb).getRGB());

			}
		}
		return t;
	}

	/*Function to get the gradient direction using the intensities in the x and y direction
		Input (BufferedImage, BufferedImage): Image intensity in the x direction, Image intensity in the y direction
		Output (BufferedImage): Gradient Direction of the image
		Gradient Direction Function: <G = arctan(G_y/G_x)
	*/
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
				
				//360 Degrees
				double circle = Math.PI * 2;

				//Get RGB values of intensity map, subtract 128 to remove offset from GoD_x and GoD_y functions
				//Divide by 255 to normalize values between 0 and 1
				//Multiply by 2PI to map values to circle
				double godx_r = (GoD_x_color.getRed()-128/255d) * circle;
				double godx_g = (GoD_x_color.getGreen()-128/255d) * circle;
				double godx_b = (GoD_x_color.getBlue()-128/255d) * circle;
				double gody_r = (GoD_y_color.getRed()-128/255d) * circle;
				double gody_g = (GoD_y_color.getGreen()-128/255d) * circle;
				double gody_b = (GoD_y_color.getBlue()-128/255d) * circle;
				
				//Gradient Direction: arc tan of GoD_Y/GoD_X
				double angle_r = Math.atan2(gody_r, godx_r);
				double angle_g = Math.atan2(gody_g, godx_g);
				double angle_b = Math.atan2(gody_b, godx_b);
				// System.out.println("RR: " + angle_r + " GG: " + angle_g + " BB: " + angle_b);
				float r_ = (float) Math.min(Math.max(0, angle_r), 1);
				float g_ = (float) Math.min(Math.max(0, angle_g), 1);
				float b_ = (float) Math.min(Math.max(0, angle_b), 1);
				
				t.setRGB(p, q, new Color(r_, g_, b_).getRGB());

			}
		}
		return t;
	}

	/*Function to perform non max suppression using the Gradient Magnitude and Gradient Direction
		Input (BufferedImage, BufferedImage): Gradient Magnitude, Gradient Direction
		Output (BufferedImage): Non-Max Suppressed Image
		Non-max Suppression Algorithm: 
			- Iterate through each pixel in the gradient direction image and gradient magnitude image,
			- If the gradient direction falls between 0 and 22.5, and the magnitude of the intensity of the current pixel is larger than the one left or right to it, keep pixel, otherwise set to 0
			- If the gradient direction falls between 22.5 and 67.5, and the magnitude of the intensity of the current pixel is larger than the one up right or down left to it, keep pixel, otherwise set to 0
			- If the gradient direction falls between 67.5 and 112.5, and the magnitude of the intensity of the current pixel is larger than the one up right or down left to it, keep pixel, otherwise set to 0
			 
	*/
	public BufferedImage non_max_suppression(BufferedImage grad_mag, BufferedImage grad_dir){
		int left, middle, right, up, down, up_left, up_right, down_left, down_right, red = 0, green = 0, blue = 0;
		Color grad_mag_color, grad_mag_color_1, grad_mag_color_2, grad_dir_color, grad_dir_color_1, grad_dit_color_2;
		BufferedImage t = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				left = p==0 ? p : p-1;
				middle = p;
				right = p==width-1 ? p : p+1;
				up = q==0 ? q : q-1;
				down = q==height-1 ? q : q+1;

				float grad_mag_val = grad_mag.getRaster().getSample(p, q, 0);
				float grad_dir_val = grad_dir.getRaster().getSample(p, q, 0);

				float grad_mag_left = grad_mag.getRaster().getSample(left, q, 0);
				float grad_mag_middle = grad_mag.getRaster().getSample(middle, q, 0);
				float grad_mag_right = grad_mag.getRaster().getSample(right, q, 0);
				float grad_mag_up = grad_mag.getRaster().getSample(p, up, 0);
				float grad_mag_down = grad_mag.getRaster().getSample(p, down, 0);
				float grad_mag_up_left = grad_mag.getRaster().getSample(left, up, 0);
				float grad_mag_up_right = grad_mag.getRaster().getSample(right, up, 0);
				float grad_mag_down_left = grad_mag.getRaster().getSample(left, down, 0);
				float grad_mag_down_right = grad_mag.getRaster().getSample(right, down, 0);

				double grad_dir_angle = (grad_dir_val/255d) * Math.PI * 2;

				// double grad_dir_angle = Math.toDegrees((double) grad_dir_val/255d);
				
				Color grad_mag_left_color = new Color(grad_mag.getRGB(left, q));
				Color grad_mag_middle_color = new Color(grad_mag.getRGB(middle, q));
				Color grad_mag_right_color = new Color(grad_mag.getRGB(right, q));
				Color grad_mag_up_color = new Color(grad_mag.getRGB(p, up));
				Color grad_mag_down_color = new Color(grad_mag.getRGB(p, down));
				Color grad_mag_up_left_color = new Color(grad_mag.getRGB(left, up));
				Color grad_mag_up_right_color = new Color(grad_mag.getRGB(right, up));
				Color grad_mag_down_left_color = new Color(grad_mag.getRGB(left, down));
				Color grad_mag_down_right_color = new Color(grad_mag.getRGB(right, down));
				
				//Grad Dir Angle = 0
				if (grad_dir_angle >= 0 && grad_dir_angle <= 22.5d){
					if (grad_mag_middle>grad_mag_left && grad_mag_middle>grad_mag_right){
						red = grad_mag_middle_color.getRed();
						green = grad_mag_middle_color.getGreen();
						blue = grad_mag_middle_color.getBlue();
					}else{
						red = 0;
						green = 0;
						blue = 0;
					}
				}else if (grad_dir_angle > 22.5 && grad_dir_angle <= 67.5d){
					if (grad_mag_middle>grad_mag_up_right && grad_mag_middle>grad_mag_down_left){
						red = grad_mag_middle_color.getRed();
						green = grad_mag_middle_color.getGreen();
						blue = grad_mag_middle_color.getBlue();
					}else{
						red = 0;
						green = 0;
						blue = 0;
					}
				}else if (grad_dir_angle > 67.5 && grad_dir_angle <= 112.5){
					if (grad_mag_middle>grad_mag_up && grad_mag_middle>grad_mag_down){
						red = grad_mag_middle_color.getRed();
						green = grad_mag_middle_color.getGreen();
						blue = grad_mag_middle_color.getBlue();
					}else{
						red = 0;
						green = 0;
						blue = 0;
					}
				}else if (grad_dir_angle > 112.5 && grad_dir_angle <= 157.5){
					if (grad_mag_middle>grad_mag_up_left && grad_mag_middle>grad_mag_down_right){
						red = grad_mag_middle_color.getRed();
						green = grad_mag_middle_color.getGreen();
						blue = grad_mag_middle_color.getBlue();
					}else{
						red = 0;
						green = 0;
						blue = 0;
					}
				}else {
					System.out.println("This angle not covered: " + grad_dir_angle);
				}
				

				int grayScaled = (int)(red+green+blue)/3;
				t.setRGB(p, q, new Color(grayScaled, grayScaled, grayScaled).getRGB());

			}
		}
		return t;
	}
	
	// public BufferedImage thresholding(BufferedImage img, int highT, int lowT){
	// 	int l, r, dr, dg, db;
	// 	Color img_color;
	// 	BufferedImage t = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

	// 	for ( int q=0 ; q<height ; q++ ) {
	// 		for ( int p=0 ; p<width ; p++ ) {
	// 			l = p==0 ? p : p-1;
	// 			r = p==width-1 ? p : p+1;
				
	// 			float val = img.getRaster().getSample(p, q, 0);
	// 			if (val > ){

	// 			}

	// 			// System.out.println("R: " + rr + " G: " + gg + " B: " + bb);
	// 			t.setRGB(p, q, new Color(rr, gg, bb).getRGB());

	// 		}
	// 	}
	// 	return t;
	// }

	public BufferedImage grayscale(BufferedImage img){
		BufferedImage grayscaleImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		for (int i = 0; i < grayscaleImage.getHeight(); i++) {
			for (int j = 0; j < grayscaleImage.getWidth(); j++) {
				Color c = new Color(img.getRGB(j, i));
				int red = (int) (c.getRed() * 0.299f);
				int green = (int) (c.getGreen() * 0.587f);
				int blue = (int) (c.getBlue() * 0.114f);
				Color newColor = new Color(
						red + green + blue,
						red + green + blue,
						red + green + blue);
				grayscaleImage.setRGB(j, i, newColor.getRGB());
			}
		}
		return grayscaleImage;
	}

	public BufferedImage approximationFilter(BufferedImage img){
		// BufferedImage approximated_img = averageBlur(img);
		// BufferedImage approximated_img = gaussianBlur(img);
		int[] filter = {1, 2, 1, 2, 4, 2, 1, 2, 1};
		int filterWidth = 3;
		BufferedImage approximated_img = blur(img, filter, filterWidth);

		return approximated_img;
	}

	public BufferedImage averageBlur(BufferedImage image){
		
		//Average Kernel
		Kernel kernel = new Kernel(3, 3, new float[] {
			1f/10f, 1f/10f, 1f/10f, 
			1f/10f, 1f/10f, 1f/10f, 
			1f/10f, 1f/10f, 1f/10f
		});
		
		BufferedImageOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
		//Run convolution filter with given image.
		BufferedImage blurredImage = op.filter(image, null);

		return blurredImage;
	}

	public BufferedImage gaussianBlur(BufferedImage img) {
        int radius = 5;
		boolean horizontal = true;
		if (radius < 1) {
            throw new IllegalArgumentException("Radius must be >= 1");
        }
        int size = radius * 2 + 1;
        float[] data = new float[size];
        float sigma = radius / 3.0f;
        float twoSigmaSquare = 2.0f * sigma * sigma;
        float sigmaRoot = (float) Math.sqrt(twoSigmaSquare * Math.PI);
        float total = 0.0f;
        for (int i = -radius; i <= radius; i++) {
            float distance = i * i;
            int index = i + radius;
            data[index] = (float) Math.exp(-distance / twoSigmaSquare)
                    / sigmaRoot;
            total += data[index];
        }
        for (int i = 0; i < data.length; i++) {
            data[i] /= total;
        }
        Kernel kernel;
        if (horizontal) {
            kernel = new Kernel(size, 1, data);
        } else {
            kernel = new Kernel(1, size, data);
        }
        BufferedImageOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
		BufferedImage blurredImage = op.filter(img, null);
		return blurredImage;
    }

	public static BufferedImage blur(BufferedImage image, int[] filter, int filterWidth) {
		if (filter.length % filterWidth != 0) {
			throw new IllegalArgumentException("filter contains a incomplete row");
		}
	
		final int width = image.getWidth();
		final int height = image.getHeight();
		final int sum = IntStream.of(filter).sum();
	
		int[] input = image.getRGB(0, 0, width, height, null, 0, width);
	
		int[] output = new int[input.length];
	
		final int pixelIndexOffset = width - filterWidth;
		final int centerOffsetX = filterWidth / 2;
		final int centerOffsetY = filter.length / filterWidth / 2;
	
		// apply filter
		for (int h = height - filter.length / filterWidth + 1, w = width - filterWidth + 1, y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int r = 0;
				int g = 0;
				int b = 0;
				for (int filterIndex = 0, pixelIndex = y * width + x;
						filterIndex < filter.length;
						pixelIndex += pixelIndexOffset) {
					for (int fx = 0; fx < filterWidth; fx++, pixelIndex++, filterIndex++) {
						int col = input[pixelIndex];
						int factor = filter[filterIndex];
	
						// sum up color channels seperately
						r += ((col >>> 16) & 0xFF) * factor;
						g += ((col >>> 8) & 0xFF) * factor;
						b += (col & 0xFF) * factor;
					}
				}
				r /= sum;
				g /= sum;
				b /= sum;
				// combine channels with full opacity
				output[x + centerOffsetX + (y + centerOffsetY) * width] = (r << 16) | (g << 8) | b | 0xFF000000;
			}
		}
	
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		result.setRGB(0, 0, width, height, output, 0, width);
		return result;
	}
	public BufferedImage colorWheel(BufferedImage img){
		int r=0,g=0,b=0;
		BufferedImage colorWheelImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for ( int q=0 ; q<height ; q++ ) {
			for ( int p=0 ; p<width ; p++ ) {
				Color img_color = new Color(img.getRGB(p,q));
				// float gray = (img_color.getRed() + img_color.getGreen() + img_color.getBlue())/3f;
				float gray = (float) img.getRaster().getSample(p, q, 0);
				// System.out.println("Gray: " + gray + " 360: " + gray/360f);
				int hsv = Color.HSBtoRGB((gray/255f), 1f, 1f);
				colorWheelImage.setRGB(p, q, hsv);


			}
		}
		return colorWheelImage;
	}
	public static void main(String[] args) {
		new DetectEdge(args.length==1 ? args[0] : "Johnston_Hall.png");
	}
}

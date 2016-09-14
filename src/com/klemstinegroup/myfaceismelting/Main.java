package com.klemstinegroup.myfaceismelting;

import com.jhlabs.image.AbstractBufferedImageOp;
import com.jhlabs.image.SwimFilter;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import static java.awt.Color.gray;
import static org.bytedeco.javacpp.helper.opencv_core.AbstractIplImage.create;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvZero;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BILATERAL;
import static org.bytedeco.javacpp.opencv_imgproc.cvSmooth;

/**
 * Created by Paul on 9/13/2016.
 */


public class Main {
    ;
    SwimFilter sf = new SwimFilter();
    SwimFilter sf1 = new SwimFilter();
    int EDGES_THRESHOLD = 70;
    int LAPLACIAN_FILTER_SIZE = 5; // 5
    int MEDIAN_BLUR_FILTER_SIZE = 7; // 7
    int repetitions = 7; // Repetitions for strong cartoon effect.
    int ksize = 1; // Filter size. Has a large effect on speed.
    double sigmaColor = 9; // Filter color strength.
    double sigmaSpace = 7; // Spatial strength. Affects speed.
    int NUM_COLORS = 16;
    int gg = (256 / NUM_COLORS);
    private float t1;
    private float t2;

    private CanvasFrame cf = new CanvasFrame("test", 1f);
    private boolean isRunning = true;

    public Main(String file, int width, int height) throws Exception {
        sf.setAmount(10f);
        sf.setTurbulence(1f);
        sf.setScale(100);
        sf.setEdgeAction(sf.CLAMP);
        sf1.setEdgeAction(sf1.CLAMP);
        sf1.setAmount(20f);
        sf1.setTurbulence(1f);
        sf1.setScale(600);
        sf1.setStretch(80);

        System.out.println("file=" + file);

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file);
        grabber.start();
        if (file.endsWith(".mp4")) file = file.substring(0, file.length() - 4);
        int frameLength = grabber.getLengthInFrames();

        int cnt = 0;
        while (isRunning) {
            System.out.println("Frame:" + cnt + "/" + frameLength);
            Frame grab = grabber.grabImage();
            if (grab == null) break;
            IplImage image = new OpenCVFrameConverter.ToIplImage().convert(grab);

            IplImage gray = create(image.cvSize(), IPL_DEPTH_8U, 1);
            //IplImage edges = create(gray.cvSize(), gray.depth(), gray.nChannels());
            IplImage temp12 = create(image.cvSize(), image.depth(), image.nChannels());
            IplImage pp8 = render(copy(image), sf);
            IplImage copy11 = render(copy(pp8), sf1);
            cvCvtColor(copy11, gray, CV_BGR2GRAY);
            cvSmooth(gray, gray, CV_MEDIAN, MEDIAN_BLUR_FILTER_SIZE, 0, 0, 0);
           // cvLaplace(gray, edges, LAPLACIAN_FILTER_SIZE);
           // cvThreshold(edges, edges, 80, 255, CV_THRESH_BINARY_INV);
            temp12 = create(copy11.cvSize(), copy11.depth(), copy11.nChannels());
            for (int i = 0; i < repetitions; i++) {
                cvSmooth(copy11, temp12, CV_BILATERAL, ksize, 0, sigmaColor, sigmaSpace);
                cvSmooth(temp12, copy11, CV_BILATERAL, ksize, 0, sigmaColor, sigmaSpace);
            }

            temp12 = create(copy11.cvSize(), copy11.depth(), copy11.nChannels());
            cvZero(temp12);
//
            cvCopy(copy11, temp12,null);
//            cvCopy(copy11, temp12,edges);
            sf.setTime(t1 += .07f);
            sf1.setTime(t2 += .05f);

            BufferedImage bi = new Java2DFrameConverter().convert(new OpenCVFrameConverter.ToIplImage().convert(copy(temp12)));
            BufferedImage biScaled = resizeImage(bi, width, height);
            String imageFile = file + "." + cnt + ".jpg";
            ImageIO.write(resizeImage(bi, width, height), "jpg", new File(imageFile));
            cf.showImage(new Java2DFrameConverter().convert(biScaled));
/*            try {
                Process proc = new ProcessBuilder("cat " + imageFile + " | docker run -i deepdream-cli > " + outImageFile).start();
                BufferedReader stdInput = new BufferedReader(new
                        InputStreamReader(proc.getInputStream()));
                BufferedReader stdError = new BufferedReader(new
                        InputStreamReader(proc.getErrorStream()));

// read the output from the command
                System.out.println("Here is the standard output of the command:\n");
                String s = null;
                while ((s = stdInput.readLine()) != null) {
                    System.out.println(s);
                }

// read any errors from the attempted command
                System.out.println("Here is the standard error of the command (if any):\n");
                while ((s = stdError.readLine()) != null) {
                    System.out.println(s);
                }
                proc.waitFor();
                System.out.println("I dreamed a dream: " + outImageFile);

            }
            catch (Exception e){e.printStackTrace();}*/
//            copy11.release();
//            gray.release();
//            edges.release();
//            temp12.release();
//            copy11.release();
//            image.release();
            cnt++;
        }
        System.exit(0);
    }

    public static IplImage copy(IplImage image) {
        IplImage copy = null;
        if (image.roi() != null)
            copy = create(image.roi().width(), image.roi().height(), image.depth(), image.nChannels());
        else
            copy = create(image.cvSize(), image.depth(), image.nChannels());
        cvCopy(image, copy);
        return copy;
    }

    public static IplImage render(IplImage image, AbstractBufferedImageOp rf) {
        BufferedImage bi = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_INT_ARGB);
        BufferedImage bi2 = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_INT_ARGB);
        //crashes here
        bi.getGraphics().drawImage(new Java2DFrameConverter().convert(new OpenCVFrameConverter.ToIplImage().convert(image)), 0, 0, null);
        rf.filter(bi, bi2);
        BufferedImage bi1 = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_3BYTE_BGR);
        bi1.getGraphics().drawImage(bi2, 0, 0, null);
        return new OpenCVFrameConverter.ToIplImage().convert(new Java2DFrameConverter().convert(bi1));
    }

    static public BufferedImage resizeImage(BufferedImage image, int width, int height) {
        int type = 0;
        type = image.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : image.getType();
        BufferedImage resizedImage = new BufferedImage(width, height, type);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(image, 0, 0, width, height, null);
        return resizedImage;
    }

    public static void main(String[] args) throws Exception {
        if (args.length==0){
            new Main("cuteanimals.mp4",960,520);
        }
        if (args.length == 3) {
                new Main(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        }
    }
}

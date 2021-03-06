/*
 * Copyright (C) 2016 Riccardo Leschiutta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.prgpascal.bluerand;

import java.io.*;
import java.awt.image.BufferedImage;
import java.awt.*;
import javax.imageio.ImageIO;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.nio.charset.Charset;
import java.lang.Math;
 
public class BlueRand {
    
    // Input / Output
    private File inputImage1;
    private File inputImage2;
    
    // Output File
    private boolean createOutputFile = false;
    private File outputFile;
    private boolean overwriteOutputFile = true;
    
    // Output Image
    private boolean createOutputImage = false;
    private File outputImage;
    
    // Delete Input files
    private boolean deleteInputFiles = false;
    
    // Random Bytes
    private ArrayList<Byte> bytes = new ArrayList<Byte>();
    private byte buffer;
    private int bufferIndex = 7;
    
    // Other params
    private boolean considerTwoLSB = false;
    
    // Delay
    private int delay = 0;
    private int delayMin;
    private int delayMax;
    private SecureRandom random;
    
    /** 
     * Empty constructor.
     */
    public BlueRand(){} 
    
    /** 
     * Constructor. 
     * 
     * @param inputImage1 first input image.
     * @param inputImage2 second input image.
     */
    public BlueRand(File inputImage1, File inputImage2){
        this.inputImage1 = inputImage1;
        this.inputImage2 = inputImage2;        
    }
    
    /** 
     * Constructor. 
     * 
     * @param inputImage1 first input image.
     * @param inputImage2 second input image.
     */
    public BlueRand(String inputImage1, String inputImage2){
    	this(new File(inputImage1), new File(inputImage2));
    }
    
    /**
     * Set the input images.
     * 
     * @param inputImage1 first input image.
     * @param inputImage2 second input image.
     */
    public void setInputImages(String inputImage1, String inputImage2){
        this.inputImage1 = new File(inputImage1);
        this.inputImage2 = new File(inputImage2);
    }   
    
    /**
     * Consider two least significant bits (LSB).
     * Default = FALSE.
     *
     * @param consider set it as TRUE if two least significant bits must be considered.
     */
    public void considerTwoLSB(boolean consider){
        this.considerTwoLSB = consider;
    }
    
    /**
     * Set the output resource File.
     * If not set, no output File will be created.
     *
     * @param path the path of the output File.
     */
    public void setOutputFile(String path){
        outputFile = new File(path);
        createOutputFile = true;
    }
    
    /**
     * Set the output image resource File.
     * If not set, no output image will be created.
     *
     * @param path the path to the output image File.
     */
    public void setOutputImage(String path){
        outputImage = new File(path);
        createOutputImage = true;
    }
     
    /**
     * Overwrite the output resource File with the generated random bytes.
     * Default = TRUE.
     *
     * @param path TRUE if the output File must be overwritten.
     */
    public void overwriteOutputFile(boolean overwrite){
        this.overwriteOutputFile = overwrite;
    }
    
    /**
     * Delete or not the input file after the generation.
     * Default = FALSE.
     *
     * @param delete TRUE if the input files must be deleted after the generation.
     */
    public void deleteInputFiles(boolean delete){
        this.deleteInputFiles = delete;
    } 
    
    /**
     * Start the generator.
     *
     * @return bytes ArrayList containing the generated Bytes.
     */
    public ArrayList<Byte> generateRandom() throws BlueRandException {
        
        // New Instances
        random = new SecureRandom();
        bytes = new ArrayList<Byte>();   
            
        // Buffered Image
        BufferedImage img1 = null;
        BufferedImage img2 = null;

        try {
            // Buffer the image
            img1 = ImageIO.read(inputImage1);
            img2 = ImageIO.read(inputImage2);
            
        } catch (IOException e){
        	// Error reading input files
        	throw new BlueRandException("Input Error");
        }
            
        // Check if img1 and img2 have the same number of pixels
        if ((img1.getWidth() != img2.getWidth()) ||
            (img1.getHeight() != img2.getHeight())){
        	
        	// Input images have different resolutions!
        	throw new BlueRandException("Input images have different resolutions!");
        }
        
        // Params
        int width = img1.getWidth();
        int heigth = img1.getHeight();
            
        // Set the max/min delay
        delayMax = (int) Math.log(width*heigth);
        delayMin = delayMax/2;
                    
        for (int y=0; y<heigth; y++){
            for (int x=0; x<width; x++){
                
                if (delay == 0){
                    
                    // Get the blue color of the (x,y) pixel in image1
                    int rgbValue = img1.getRGB(x,y);         
                    Color color = new Color(rgbValue);
                    int c1 = color.getBlue();
                    
                    // Get the blue color of the (x,y) pixel in image2
                    rgbValue = img2.getRGB(x,y);         
                    color = new Color(rgbValue);
                    int c2 = color.getBlue();
         
                    // Add the colors to output bytes
                    addToGroup(c1, c2);
                    
                } else {
                    delay--;
                }
            }
      	}

        // Write to Output file
        try {    
	        if (createOutputFile){
	            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(outputFile, !overwriteOutputFile), Charset.forName("ISO-8859-1"));
	            for (Byte s : bytes){
	                String output = encodeISO88591(new byte[]{s.byteValue()});
	                out.write(output, 0, output.length());
	                out.flush();
	            }
	            out.close();
	        }
        } catch(IOException e){
        	throw new BlueRandException("Output error");
        }   
             
        // Write to Output image
        if (createOutputImage){
            if (!overwriteOutputFile){
                // Append to an existing image
                new CreateOutputImage(outputImage, outputFile).create();
            } else {
                // Create new output image
                new CreateOutputImage(outputImage, bytes).create();
            }
        }
        
        // Delete input files
        if (deleteInputFiles){
	        if ((!inputImage1.delete()) || (!inputImage2.delete())){
	        	throw new BlueRandException("Input files not deleted");
	        }
        }
        	
        // Finished!
        return bytes;
    } 
    
    /**
     * Add a single bit to output bytes.
     *
     * @param c1 blue color of the pixel in image1.
     * @param c2 blue color of the pixel in image2.
     */
    private void addToGroup(int c1, int c2){
        
        if (considerTwoLSB){
            // For each color, get 2 LSB and XOR them together.
            /* http://stackoverflow.com/questions/2529756/assigning-int-to-byte-in-java */
            
            // C1
            byte tempByte = (byte)c1;
            int p7 = (tempByte >> 1) & 1;
            int p8 = (tempByte >> 0) & 1;
            int xored = p7^p8;
            c1 = xored;
            
            // C2
            tempByte = (byte)c2;
            p7 = (tempByte >> 1) & 1;
            p8 = (tempByte >> 0) & 1;
            xored = p7^p8;
            c2 = xored;
        }
        
        
        // New bit value (simple XOR operation)
        int newValue = (c1%2)^(c2%2);
        
        // Calculate new delay (how many future pixel must be ignored)
        do {
            delay = random.nextInt(delayMax);
        } while(delay<delayMin);
               
        
        /*
         * Add the resulting bit to the buffer byte
         * http://stackoverflow.com/questions/4844342/change-bits-value-in-byte
         */
        if (newValue == 0){
            buffer &= ~(1 << bufferIndex); // set a bit to 0
        } else {
            buffer |= (1 << bufferIndex);  // set a bit to 1
        }
        
        
        if (bufferIndex == 0){
            // Buffer full, flush it to the output ArrayList
            bytes.add(new Byte(buffer));
            bufferIndex = 7;
            
        } else {
            bufferIndex--;
        }
    }
    
    /**
     * Encode a byte array into a ISO-8859-1 String.
     *
     * @param bytes array of bytes.
     * @return ISO-8859-1 String representation.
     */
    private String encodeISO88591(byte[] bytes){
        try {
            // byte[] to ISO-8859-1 String
            String encoded = new String(bytes, "ISO-8859-1");

            return encoded;

        } catch (UnsupportedEncodingException e){
            e.printStackTrace();
        }

        return null;
    }
}



package com.cw.litenote.util.image;

import java.util.ArrayList;
import java.util.List;

public class SlideshowInfo
{
   private List<String> imageList; // this slideshow's images
   private List<String> textList; // this slideshow's texts

   // constructor 
   public SlideshowInfo()
   {
      imageList = new ArrayList<>();
      textList = new ArrayList<>();
   }

   // add a new image path
   public void addImage(String path)
   {
	  System.out.println("path = " + path); 
      imageList.add(path);
   }

   // add a new text
   public void addText(String text)
   {
      System.out.println("text = " + text);
      textList.add(text);
   }
   
   // return String at position index
   public String getImageAt(int index)
   {
      if (index >= 0 && index < imageList.size())
         return imageList.get(index);
      else
         return null;
   }

   // return text at position index
   public String getTextAt(int index)
   {
      if (index >= 0 && index < imageList.size())
         return textList.get(index);
      else
         return null;
   }
   
   // return number of images/videos in the slideshow
   public int imageSize()
   {
      return imageList.size();
   }
}
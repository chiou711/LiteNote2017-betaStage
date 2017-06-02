package com.cw.litenote.util.image;

import java.util.ArrayList;
import java.util.List;

public class SlideshowInfo
{
   private List<ViewHolder> showList;

   public class ViewHolder {
       String imagePath;
       String text;
       Integer position;
   }

   // constructor 
   public SlideshowInfo()
   {
       showList = new ArrayList<>();
   }

   public void addShowItem(String path,String text,Integer position)
   {
       ViewHolder holder = new ViewHolder();
       holder.imagePath = path;
       holder.text = text;
       holder.position = position;
       showList.add(holder);
   }

   public ViewHolder getShowItem(Integer index)
   {
       System.out.println("SlideshowInfo / _getShowItem / index = " + index);
       if ((index >= 0) && (index < showList.size()))
           return showList.get(index);
       else
           return null;
   }

   public int showItemsSize()
   {
       return showList.size();
   }
}
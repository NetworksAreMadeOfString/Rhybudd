/*
 * Copyright (C) 2013 - Gareth Llewellyn
 *
 * This file is part of Rhybudd - http://blog.NetworksAreMadeOfString.co.uk/Rhybudd/
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package net.networksaremadeofstring.rhybudd;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.Random;

public class ZenossGroupsGridAdapter  extends BaseAdapter
{
        private Context mContext;
        private int imgWidth = 0;
        private int imgHeight = 0;
        private String[] groups = null;

        public ZenossGroupsGridAdapter(Context c, String[] newGroups)
        {
            mContext = c;
            groups = newGroups;
        }

        public int getCount()
        {
            return groups.length;
        }

        public Object getItem(int position)
        {
            return groups[position];
        }

        public long getItemId(int position)
        {
            return position;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent)
        {
            if(imgWidth == 0)
            {
                imgWidth = (parent.getMeasuredWidth() / mContext.getResources().getInteger(R.integer.GridColumns));
                imgHeight = (300 * (imgWidth / 500));
            }

            //Log.e("Width", Integer.toString(imgWidth) + " / " + Integer.toString(imgHeight));
            /*ImageView imageView;
            if (convertView == null)
            {
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(imgWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                //imageView.setPadding(8, 8, 8, 8);
            }
            else
            {
                imageView = (ImageView) convertView;
            }

            imageView.setImageResource(mThumbIds[position]);
            return imageView;*/

            if (convertView == null)
            {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.groups_grid_item, null);
            }

            //((RelativeLayout) convertView.findViewById(R.id.gridItemContainer)).set
            ImageView groupImg = ((ImageView) convertView.findViewById(R.id.groupImage));
            //groupImg.setLayoutParams(new ViewGroup.LayoutParams(imgWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            groupImg.setScaleType(ImageView.ScaleType.CENTER_CROP);

            Random r = new Random();
            groupImg.setImageResource(mThumbIds[r.nextInt(mThumbIds.length)]);

            ((TextView) convertView.findViewById(R.id.groupTitle)).setText(groups[position]);
            ((TextView) convertView.findViewById(R.id.groupTitle)).setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

            return convertView;
        }

        // references to our images
        private Integer[] mThumbIds = {
                R.drawable.groups_a, R.drawable.groups_b,
                R.drawable.groups_c, R.drawable.groups_d,
                R.drawable.groups_e, R.drawable.groups_f,
                R.drawable.groups_g, R.drawable.groups_h,
                R.drawable.groups_i, R.drawable.groups_j,
                R.drawable.groups_k, R.drawable.groups_l
        };
}


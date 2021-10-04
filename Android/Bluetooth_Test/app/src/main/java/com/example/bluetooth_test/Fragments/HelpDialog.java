package com.example.bluetooth_test.Fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.example.bluetooth_test.R;

public class HelpDialog {
    Dialog dialog;
    Context context;
    public HelpDialog(Context context){
        this.context = context;
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.help_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        dialog = builder.setView(view)
                .setNegativeButton("Finish", (d, i)->d.dismiss()).create();
        ViewPager pager = view.findViewById(R.id.help_dialog_view_pager);
                pager.setAdapter(new HelpPagerAdaptor());
        view.findViewById(R.id.buttonNext).setOnClickListener(v->
                pager.setCurrentItem(
                        pager.getCurrentItem()+1==pager.getAdapter().getCount()?
                                pager.getCurrentItem():
                                pager.getCurrentItem()+1)
        );
        view.findViewById(R.id.buttonPrev).setOnClickListener(v->
                pager.setCurrentItem(
                        pager.getCurrentItem()==0?
                                pager.getCurrentItem():
                                pager.getCurrentItem()-1)
        );
    }

    public void show(){
        dialog.show();
    }



    class HelpPagerAdaptor extends PagerAdapter{

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            ImageView view = new ImageView(context);
            view.setImageDrawable(context.getDrawable(getImageAtPos(position)));
            container.addView(view);
            return view;
        }

        private int getImageAtPos(int position) {
            switch (position){
                case 1:
                    return R.drawable.app_help_2;
                case 2:
                    return R.drawable.app_help_3;
                default:
                    return R.drawable.app_help_1;
            }
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
            return view==o;
        }
    }


}

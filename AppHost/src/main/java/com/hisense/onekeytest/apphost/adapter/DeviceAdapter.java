package com.hisense.onekeytest.apphost.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hisense.onekeytest.apphost.R;
import com.hisense.onekeytest.apphost.bean.DeviceBean;

import java.util.List;

/**
 * Created by Administrator on 2017/3/2.
 * ListView适配器
 */

public class DeviceAdapter extends BaseAdapter {

    private List<DeviceBean> mData;
    private Context mContext;

    public DeviceAdapter(List<DeviceBean> mData, Context mContext) {
        this.mData = mData;
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return mData==null?0:mData.size();
    }

    @Override
    public Object getItem(int i) {
        return mData.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder vh;
        if (view==null){
            view = LayoutInflater.from(mContext).inflate(R.layout.item_list_view_main_device_ip,null);
            vh = new ViewHolder(view);
            view.setTag(vh);
        }else {
            vh = (ViewHolder) view.getTag();
        }
        vh.mIPTextView.setText(mData.get(i).getIp());
        if (mData.get(i).getIsConnect()){
            vh.mTextView.setText(mContext.getResources().getString(R.string.str_item_disconnect));
        }else {
            vh.mTextView.setText(mContext.getResources().getString(R.string.str_item_connect));
        }
        return view;
    }

    class ViewHolder {

        public TextView mIPTextView,mTextView;

        public ViewHolder(View view) {
            this.mIPTextView = (TextView) view.findViewById(R.id.tv_item_ip);
            this.mTextView = (TextView) view.findViewById(R.id.tv_item_text);
        }
    }
}

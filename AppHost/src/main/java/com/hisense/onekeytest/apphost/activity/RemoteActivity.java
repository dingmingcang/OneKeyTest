package com.hisense.onekeytest.apphost.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.hisense.onekeytest.apphost.R;
import com.hisense.onekeytest.apphost.search.SearcherConst;
import com.hisense.onekeytest.apphost.socket.HostSocket;
import com.hisense.onekeytest.apphost.util.Const;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 遥控器界面
 */

public class RemoteActivity extends AppCompatActivity {

    private int mKeyCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_0:
                mKeyCode = 7;
                break;
            case R.id.btn_1:
                mKeyCode = 8;
                break;
            case R.id.btn_2:
                mKeyCode = 9;
                break;
            case R.id.btn_3:
                mKeyCode = 10;
                break;
            case R.id.btn_4:
                mKeyCode = 11;
                break;
            case R.id.btn_5:
                mKeyCode = 12;
                break;
            case R.id.btn_6:
                mKeyCode = 13;
                break;
            case R.id.btn_7:
                mKeyCode = 14;
                break;
            case R.id.btn_8:
                mKeyCode = 15;
                break;
            case R.id.btn_9:
                mKeyCode = 16;
                break;
            case R.id.btn_setting:
                mKeyCode = 176;
                break;
            case R.id.btn_home:
                mKeyCode = 142;
                break;
            case R.id.btn_back:
                mKeyCode = 4;
                break;
            case R.id.btn_meiti:
                mKeyCode = 170;
                break;
            case R.id.btn_vod:
                mKeyCode = 131;
                break;
            case R.id.btn_app:
                mKeyCode = 132;
                break;
            case R.id.btn_game:
                mKeyCode = 209;
                break;
            case R.id.btn_up:
                mKeyCode = 19;
                break;
            case R.id.btn_down:
                mKeyCode = 20;
                break;
            case R.id.btn_left:
                mKeyCode = 21;
                break;
            case R.id.btn_right:
                mKeyCode = 22;
                break;
            case R.id.btn_ok:
                mKeyCode = 23;
                break;
            case R.id.btn_app_back:
                finish();
                break;
            case R.id.btn_big:
                mKeyCode = 24;
                break;
            case R.id.btn_small:
                mKeyCode = 25;
                break;
            case R.id.btn_cast:
                mKeyCode = 82;
                break;
            default:
                break;
        }
        Log.e(Const.CONST_LOG_TAG, "按下："+mKeyCode );
        HostSocket.getInstance().sendJsonData(packJson());
    }


    /**
     * 打包Json数据
     * @return json字符串
     */
    private String packJson() {
        JSONObject jo = new JSONObject();
        try {
            jo.put(Const.CONST_PACK_TYPE,SearcherConst.SOCKET_TYPE_KEY_CODE);
            jo.put(Const.CONST_KEY_TYPE,SearcherConst.SOCKET_TYPE_KEY_CODE_TYPE_DONE);
            jo.put(Const.CONST_KEY_CODE,mKeyCode);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(Const.CONST_LOG_TAG, "打包json数据出错");
        }
        return jo.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
    }
}

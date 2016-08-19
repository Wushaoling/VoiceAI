package com.example.tvoice;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.baidu.apistore.sdk.ApiCallBack;
import com.baidu.apistore.sdk.ApiStoreSDK;
import com.baidu.apistore.sdk.network.Parameters;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.sunflower.FlowerCollector;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 语音识别及AI回复
 * 
 * @author WSL
 *
 */
@SuppressWarnings("all")
public class IatDemo extends Activity implements OnClickListener {
	
	private static String TAG = IatDemo.class.getSimpleName();
	
	// 语音听写UI
	private RecognizerDialog mIatDialog;
	private EditText et = null;
	private TextView tv = null;
	private Toast mToast;
	
	// 用HashMap存储听写结果
	private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
		
	// 语音合成对象
	private SpeechSynthesizer mTts;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		SpeechUtility.createUtility(IatDemo.this, "appid=your xfyunappid");
		initLayout();
		// 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
		// 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
		mIatDialog = new RecognizerDialog(IatDemo.this, mInitListener);
		//初始化语音合成
		mTts = SpeechSynthesizer.createSynthesizer(IatDemo.this, mInitListener);
		//初始化听写及合成的参数
		initParam();
	}

	/**
	 * 初始化布局
	 */
	private void initLayout() {
		findViewById(R.id.button_voice).setOnClickListener(IatDemo.this);
		findViewById(R.id.button_ok).setOnClickListener(this);
		findViewById(R.id.button_clear).setOnClickListener(this);
		et = (EditText) findViewById(R.id.editText_input);
		tv = (TextView) findViewById(R.id.textView_output);
	}
	
	/**
	 * 初始化监听器
	 */
	private InitListener mInitListener = new InitListener() {
		@Override
		public void onInit(int code) {
			if (code != ErrorCode.SUCCESS) {
				showTip("初始化失败，错误码：" + code);
			}
		}
	};
	
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.button_voice:
			mTts.stopSpeaking();
			// 移动数据分析，收集开始听写事件
			FlowerCollector.onEvent(IatDemo.this, "iat_recognize");
			mIatResults.clear();
			// 显示听写对话框
			mIatDialog.show();
			break;
		case R.id.button_ok:
			mTts.stopSpeaking();
			String str = et.getText().toString();
			if (0 != str.length()) {
				tv.append("我：" + str + "\n");
				et.setText("");
				getResultFromAI(str);
			}
			break;
		case R.id.button_clear:
			mTts.stopSpeaking();
			tv.setText("");
			break;
		default:
			break;
		}
	}

	/**
	 * 打印语音识别结果
	 * 
	 * @param results
	 */
	private void printResult(RecognizerResult results) {
		String text = JsonParser.parseIatResult(results.getResultString());
		String sn = null;
		try {
			JSONObject resultJson = new JSONObject(results.getResultString());
			sn = resultJson.optString("sn");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		mIatResults.put(sn, text);
		StringBuffer resultBuffer = new StringBuffer();
		for (String key : mIatResults.keySet()) {
			resultBuffer.append(mIatResults.get(key));
		}
		String result = resultBuffer.toString();
		tv.append("我：" + result + "\n");
		et.setText("");
		getResultFromAI(result);
	}

	/**
	 * 听写UI监听器
	 */
	private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
		public void onResult(RecognizerResult results, boolean isLast) {
			if (!isLast) {
				printResult(results);
			}
		}

		// 识别回调错误.
		public void onError(SpeechError error) {
			showTip(error.getPlainDescription(true));
		}
	};
	
	/**
	 * 合成回调监听。
	 */
	private SynthesizerListener mTtsListener = new SynthesizerListener() {

		@Override
		public void onSpeakBegin() {
		}

		@Override
		public void onSpeakPaused() {
		}

		@Override
		public void onSpeakResumed() {
		}

		@Override
		public void onCompleted(SpeechError error) {
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
		}

		@Override
		public void onBufferProgress(int arg0, int arg1, int arg2, String arg3) {
		}

		@Override
		public void onSpeakProgress(int arg0, int arg1, int arg2) {
		}
	};

	/**
	 * 获得AI结果
	 * 
	 * @param content
	 * @return
	 */
	public void getResultFromAI(String content) {
		try {
			content = URLEncoder.encode(content, "UTF-8");
			Parameters para = new Parameters();
			para.put("key", "your turingkey");
			para.put("info", content);
			para.put("userid", "eb2edb736");
			ApiStoreSDK.execute("http://apis.baidu.com/turing/turing/turing", ApiStoreSDK.GET, para, new ApiCallBack() {
				@Override
				public void onSuccess(int status, String responseString) {
					try {
						String result = new JSONObject(responseString).getString("text");
						tv.append("AI：" + result + "\n");
						FlowerCollector.onEvent(IatDemo.this, "tts_play");
						mTts.startSpeaking(result, mTtsListener);
					} catch (JSONException e) {
						tv.append("error\n");
					}
				}

				@Override
				public void onComplete() {
				}

				@Override
				public void onError(int status, String responseString, Exception e) {
					tv.append(getStackTrace(e) + "\n");
				}
			});
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	/**
	 * 参数设置
	 * 
	 * @param param
	 * @return
	 */
	private void initParam() {
		mIatDialog.setParameter(SpeechConstant.ASR_PTT, "0");
		mIatDialog.setListener(mRecognizerDialogListener);
		// 清空参数
		mTts.setParameter(SpeechConstant.PARAMS, null);
		// 根据合成引擎设置相应参数
		mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
		// 设置在线合成发音人
		mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
		// 设置合成语速
		mTts.setParameter(SpeechConstant.SPEED, "50");
		// 设置合成音调
		mTts.setParameter(SpeechConstant.PITCH, "50");
		// 设置合成音量
		mTts.setParameter(SpeechConstant.VOLUME, "50");
		// 设置播放器音频流类型
		mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
		// 设置播放合成音频打断音乐播放，默认为true
		mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

		// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
		// 注：AUDIO_FORMAT参数语记需要更新版本才能生效
		mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
		mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/tts.wav");
	}
	
	public String getStackTrace(Throwable e) {
		if (e == null) {
			return "";
		}
		StringBuilder str = new StringBuilder();
		str.append(e.getMessage()).append("\n");
		for (int i = 0; i < e.getStackTrace().length; i++) {
			str.append(e.getStackTrace()[i]).append("\n");
		}
		return str.toString();
	}

	/**
	 * showTip
	 * 
	 * @param str
	 */
	private void showTip(final String str) {
		Toast.makeText(this, str, Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mTts.stopSpeaking();
		mTts.destroy();
	}

	@Override
	protected void onResume() {
		// 开放统计 移动数据统计分析
		FlowerCollector.onResume(IatDemo.this);
		FlowerCollector.onPageStart(TAG);
		super.onResume();
	}

	@Override
	protected void onPause() {
		// 开放统计 移动数据统计分析
		FlowerCollector.onPageEnd(TAG);
		FlowerCollector.onPause(IatDemo.this);
		super.onPause();
	}
}

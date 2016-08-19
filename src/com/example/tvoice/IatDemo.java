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
 * ����ʶ��AI�ظ�
 * 
 * @author WSL
 *
 */
@SuppressWarnings("all")
public class IatDemo extends Activity implements OnClickListener {
	
	private static String TAG = IatDemo.class.getSimpleName();
	
	// ������дUI
	private RecognizerDialog mIatDialog;
	private EditText et = null;
	private TextView tv = null;
	private Toast mToast;
	
	// ��HashMap�洢��д���
	private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
		
	// �����ϳɶ���
	private SpeechSynthesizer mTts;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		SpeechUtility.createUtility(IatDemo.this, "appid=your xfyunappid");
		initLayout();
		// ��ʼ����дDialog�����ֻʹ����UI��д���ܣ����贴��SpeechRecognizer
		// ʹ��UI��д���ܣ������sdk�ļ�Ŀ¼�µ�notice.txt,���ò����ļ���ͼƬ��Դ
		mIatDialog = new RecognizerDialog(IatDemo.this, mInitListener);
		//��ʼ�������ϳ�
		mTts = SpeechSynthesizer.createSynthesizer(IatDemo.this, mInitListener);
		//��ʼ����д���ϳɵĲ���
		initParam();
	}

	/**
	 * ��ʼ������
	 */
	private void initLayout() {
		findViewById(R.id.button_voice).setOnClickListener(IatDemo.this);
		findViewById(R.id.button_ok).setOnClickListener(this);
		findViewById(R.id.button_clear).setOnClickListener(this);
		et = (EditText) findViewById(R.id.editText_input);
		tv = (TextView) findViewById(R.id.textView_output);
	}
	
	/**
	 * ��ʼ��������
	 */
	private InitListener mInitListener = new InitListener() {
		@Override
		public void onInit(int code) {
			if (code != ErrorCode.SUCCESS) {
				showTip("��ʼ��ʧ�ܣ������룺" + code);
			}
		}
	};
	
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.button_voice:
			mTts.stopSpeaking();
			// �ƶ����ݷ������ռ���ʼ��д�¼�
			FlowerCollector.onEvent(IatDemo.this, "iat_recognize");
			mIatResults.clear();
			// ��ʾ��д�Ի���
			mIatDialog.show();
			break;
		case R.id.button_ok:
			mTts.stopSpeaking();
			String str = et.getText().toString();
			if (0 != str.length()) {
				tv.append("�ң�" + str + "\n");
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
	 * ��ӡ����ʶ����
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
		tv.append("�ң�" + result + "\n");
		et.setText("");
		getResultFromAI(result);
	}

	/**
	 * ��дUI������
	 */
	private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
		public void onResult(RecognizerResult results, boolean isLast) {
			if (!isLast) {
				printResult(results);
			}
		}

		// ʶ��ص�����.
		public void onError(SpeechError error) {
			showTip(error.getPlainDescription(true));
		}
	};
	
	/**
	 * �ϳɻص�������
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
	 * ���AI���
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
						tv.append("AI��" + result + "\n");
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
	 * ��������
	 * 
	 * @param param
	 * @return
	 */
	private void initParam() {
		mIatDialog.setParameter(SpeechConstant.ASR_PTT, "0");
		mIatDialog.setListener(mRecognizerDialogListener);
		// ��ղ���
		mTts.setParameter(SpeechConstant.PARAMS, null);
		// ���ݺϳ�����������Ӧ����
		mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
		// �������ߺϳɷ�����
		mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
		// ���úϳ�����
		mTts.setParameter(SpeechConstant.SPEED, "50");
		// ���úϳ�����
		mTts.setParameter(SpeechConstant.PITCH, "50");
		// ���úϳ�����
		mTts.setParameter(SpeechConstant.VOLUME, "50");
		// ���ò�������Ƶ������
		mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
		// ���ò��źϳ���Ƶ������ֲ��ţ�Ĭ��Ϊtrue
		mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

		// ������Ƶ����·����������Ƶ��ʽ֧��pcm��wav������·��Ϊsd����ע��WRITE_EXTERNAL_STORAGEȨ��
		// ע��AUDIO_FORMAT���������Ҫ���°汾������Ч
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
		mToast.setText(str);
		mToast.show();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mTts.stopSpeaking();
		mTts.destroy();
	}

	@Override
	protected void onResume() {
		// ����ͳ�� �ƶ�����ͳ�Ʒ���
		FlowerCollector.onResume(IatDemo.this);
		FlowerCollector.onPageStart(TAG);
		super.onResume();
	}

	@Override
	protected void onPause() {
		// ����ͳ�� �ƶ�����ͳ�Ʒ���
		FlowerCollector.onPageEnd(TAG);
		FlowerCollector.onPause(IatDemo.this);
		super.onPause();
	}
}

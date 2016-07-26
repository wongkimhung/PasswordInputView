package com.rtfsc.library;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wongkimhung on 2016/7/26.
 */
public class PasswordView extends View {
	private InputMethodManager mInputMethodManager;

	private int mCount = 6;
	private int mSize;
	private int mWidth;
	private int mHeight;
	private float mRoundRadius;
	private int mDpi;

	private RectF mRoundRect;
	private Paint mBorderPaint;
	private Paint mDotPaint;

	private int mBorderColor;
	private int mDotColor;

	private List<Integer> mResult = new ArrayList<>();

	public PasswordView(Context context) {
		this(context, null);
	}

	public PasswordView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PasswordView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// 获取焦点
		this.setFocusable(true);
		this.setFocusableInTouchMode(true);

		mDpi = context.getResources().getDisplayMetrics().densityDpi;
		mSize = mDpi * 20;
		mRoundRect = new RectF();
		mRoundRadius = mDpi * 5;

		mBorderColor = Color.LTGRAY;
		mDotColor = Color.GRAY;

		mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mBorderPaint.setStrokeWidth(2);
		mBorderPaint.setStyle(Paint.Style.STROKE);
		mBorderPaint.setColor(mBorderColor);

		mDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mDotPaint.setStyle(Paint.Style.FILL);
		mDotPaint.setColor(mDotColor);

		mInputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		setOnKeyListener(new InputNumderKeyListener());
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = measureSize(widthMeasureSpec);
		int height = measureSize(heightMeasureSpec);

		if (width == -1) {
			if (height != -1) {
				width = height * mCount;
				mSize = height;
			} else {
				width = mSize * mCount;
				height = mSize;
			}
		} else {
			if (height == -1) {
				height = width / 6;
				mSize = height;
			}
		}

		int wSize = MeasureSpec.getSize(widthMeasureSpec);
		int hSize = MeasureSpec.getSize(heightMeasureSpec);

		setMeasuredDimension(Math.min(width, wSize), Math.min(height, hSize));
	}


	private int measureSize(int spec) {
		int mode = MeasureSpec.getMode(spec);
		int size = MeasureSpec.getSize(spec);
		if (mode == MeasureSpec.AT_MOST) {//wrap_content
			return -1;
		}
		return size;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mWidth = w - 2;
		mHeight = h - 2;
		mRoundRect.set(0, 0, mWidth, mHeight);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		//	圆角矩形
		canvas.drawRoundRect(mRoundRect, mRoundRadius, mRoundRadius, mBorderPaint);
		//	分割线
		for (int i = 1; i < mCount; i++) {
			int x = i * mSize;
			canvas.drawLine(x, 0, x, mHeight, mBorderPaint);
		}

		int dotRaduis = mSize / 6;
		int size = mResult.size();
		for (int i = 0; i < size; i++) {
			float x = (i + .5f) * mSize;
			float y = .5f * mSize;
			canvas.drawCircle(x, y, dotRaduis, mDotPaint);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			//	当视图被按下时，获取焦点，显示软键盘
			requestFocus();
			mInputMethodManager.showSoftInput(this, InputMethodManager.SHOW_FORCED);
			return true;
		}
		return super.onTouchEvent(event);
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);
		if (!hasWindowFocus) {
			//	当视图失去焦点时，隐藏软键盘
			mInputMethodManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
		}
	}

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		outAttrs.inputType = InputType.TYPE_CLASS_NUMBER;
		outAttrs.imeOptions = EditorInfo.TYPE_CLASS_PHONE;
		return super.onCreateInputConnection(outAttrs);
	}

	@Override
	public boolean onCheckIsTextEditor() {
		//	告诉系统我是个可编辑文本框
		return true;
	}

	static class PrivateInputConnection extends BaseInputConnection {
		public PrivateInputConnection(View targetView, boolean fullEditor) {
			super(targetView, fullEditor);
		}

		@Override
		public boolean commitText(CharSequence text, int newCursorPosition) {
			//这里是接受输入法的文
			return true;
		}

		@Override
		public boolean deleteSurroundingText(int beforeLength, int afterLength) {
			//软键盘的删除键 DEL 无法直接监听，自己发送del事件
			if (beforeLength == 1 && afterLength == 0) {
				return super.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
						&& super.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
			}
			return super.deleteSurroundingText(beforeLength, afterLength);
		}
	}

	class InputNumderKeyListener implements OnKeyListener {

		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
					//	输入数字
					if (mResult.size() < mCount) {
						mResult.add(keyCode - 7);
						invalidate();
					}
					if (mResult.size() == mCount) {
						ensureFinishInput();
					}
					return true;
				}


				if (keyCode == KeyEvent.KEYCODE_DEL) {
					//	用户点击删除
					if (!mResult.isEmpty()) {
						mResult.remove(mResult.size() - 1);
						invalidate();
					}
					return true;
				}

				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					//	用户点击确认
					ensureFinishInput();
					return true;
				}
			}
			return false;
		}
	}

	private void ensureFinishInput() {
		if (mResult.size() == mCount) {
			StringBuffer stringBuffer = new StringBuffer();
			for (int i : mResult) {
				stringBuffer.append(i);
			}

			if (BuildConfig.DEBUG) {
				System.out.println(stringBuffer.toString());
			}

		}
	}
}

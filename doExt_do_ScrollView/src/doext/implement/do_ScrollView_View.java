package doext.implement;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import core.DoServiceContainer;
import core.helper.DoIOHelper;
import core.helper.DoJsonHelper;
import core.helper.DoTextHelper;
import core.helper.DoUIModuleHelper;
import core.helper.DoUIModuleHelper.LayoutParamsType;
import core.interfaces.DoIPage;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoIUIModuleView;
import core.object.DoInvokeResult;
import core.object.DoProperty;
import core.object.DoSourceFile;
import core.object.DoUIContainer;
import core.object.DoUIModule;
import doext.define.do_ScrollView_IMethod;
import doext.define.do_ScrollView_MAbstract;
import doext.scrollview.pullToRefresh.DoPullToRefreshView;

/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,Do_ScrollView_IMethod接口；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
public class do_ScrollView_View extends DoPullToRefreshView implements DoIUIModuleView, do_ScrollView_IMethod {
	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private do_ScrollView_MAbstract model;
	private String defaultDirection = "vertical";
	private DoIScrollView doIScrollView;

	public do_ScrollView_View(Context context) {
		super(context);
		this.setOrientation(VERTICAL);
	}

	/**
	 * 初始化加载view准备,_doUIModule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(DoUIModule _doUIModule) throws Exception {
		this.model = (do_ScrollView_MAbstract) _doUIModule;
		TYPEID = this.model.getTypeID();
		String direction = this.model.getDirection();
		if (direction == null || "".equals(direction.trim())) {
			direction = defaultDirection;
		}
		initView(direction);
	}

	/**
	 * 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行onPropertiesChanged，否则不进行赋值；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public boolean onPropertiesChanging(Map<String, String> _changedValues) {
		return true;
	}

	/**
	 * 属性赋值成功后被调用，可以根据组件定义相关属性值修改UIView可视化操作；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public void onPropertiesChanged(Map<String, String> _changedValues) {
		DoUIModuleHelper.handleBasicViewProperChanged(this.model, _changedValues);
		if (_changedValues.containsKey("isShowbar")) {
			boolean verticalScrollBarEnabled = DoTextHelper.strToBool(_changedValues.get("isShowbar"), true);
			doIScrollView.isShowbar(verticalScrollBarEnabled);
		}
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("scrollTo".equals(_methodName)) {
			scrollTo(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("toBegin".equals(_methodName)) {
			toBegin(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("toEnd".equals(_methodName)) {
			toEnd(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("rebound".equals(_methodName)) {
			rebound(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		return false;
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @throws Exception
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.model.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		if ("screenShot".equals(_methodName)) {
			screenShot(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		return false;
	}

	/**
	 * 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
	 */
	@Override
	public void onDispose() {
		// ...do something
	}

	/**
	 * 重绘组件，构造组件时由系统框架自动调用；
	 * 或者由前端JS脚本调用组件onRedraw方法时被调用（注：通常是需要动态改变组件（X、Y、Width、Height）属性时手动调用）
	 */
	@Override
	public void onRedraw() throws Exception {
		if (this.model.getLayoutParamsType() != null) {
			this.setLayoutParams(DoUIModuleHelper.getLayoutParams(this.model));
		}
		int childSize = model.getChildUIModules().size();
		if (childSize == 0) {
			return;
		}
		DoUIModule _childUI = this.model.getChildUIModules().get(0);
		DoIUIModuleView _currentView = _childUI.getCurrentUIModuleView();
		_currentView.onRedraw();
//		View view = (View) _currentView;
//		view.setFocusable(true);
//		view.setFocusableInTouchMode(true);
		this.setFocusable(true);
		this.setFocusableInTouchMode(true);
		this.setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS);
	}

	private void initView(final String direction) throws Exception {
		Context context = (Context) model.getCurrentPage().getPageView();
		if ("horizontal".equals(direction)) {
			this.doIScrollView = new HScrollView(context);
		} else if ("vertical".equals(direction)) {
			this.doIScrollView = new VScrollView(context);
		}
		if (this.model.getChildUIModules().size() > 0) {
			DoUIModule _childUI = this.model.getChildUIModules().get(0);
			View childView = (View) _childUI.getCurrentUIModuleView();
			_childUI.setLayoutParamsType(LayoutParamsType.Alayout.toString());
			doIScrollView.addFirstView(childView);
		}
		doIScrollView.setOnScrollChangedListener(new OnScrollChangedListener() {
			@Override
			public void scrollChanged(int l, int t, int oldl, int oldt) {
				DoInvokeResult _invokeResult = new DoInvokeResult(model.getUniqueKey());
				JSONObject _obj = new JSONObject();
				try {
					if ("horizontal".equals(direction)) {
						_obj.put("left", (l / model.getXZoom()));
						_obj.put("oldLeft", (oldl / model.getXZoom()));
					} else if ("vertical".equals(direction)) {
						_obj.put("top", (t / model.getYZoom()));
						_obj.put("oldTop", (oldt / model.getYZoom()));
					}
				} catch (Exception e) {

				}
				_invokeResult.setResultNode(_obj);
				model.getEventCenter().fireEvent("scroll", _invokeResult);
			}
		});
		String _headerViewPath = this.model.getHeaderView();
		String _footerViewPath = this.model.getFooterView();
		setHeaderView(createView(_headerViewPath));
		this.addView((View) doIScrollView, new LinearLayout.LayoutParams(-1, -1));
		setFooterView(createFooterView(_footerViewPath));
		if ("horizontal".equals(direction)) {
			this.setSupportHeaderRefresh(false);
			this.setSupportFooterRefresh(false);
		} else if ("vertical".equals(direction)) {
			this.setSupportHeaderRefresh(isHeaderVisible());
			this.setSupportFooterRefresh(isFooterVisible());
		}
		this.onFinishInflate();
	}

	private View createView(String _uiPath) throws Exception {
		View _newView = null;
		if (_uiPath != null && !"".equals(_uiPath.trim())) {
			this.headerUIPath = _uiPath;
			DoIPage _doPage = this.model.getCurrentPage();
			DoSourceFile _uiFile = _doPage.getCurrentApp().getSourceFS().getSourceByFileName(_uiPath);
			if (_uiFile != null) {
				headerRootUIContainer = new DoUIContainer(_doPage);
				headerRootUIContainer.loadFromFile(_uiFile, null, null);
				if (null != _doPage.getScriptEngine()) {
					headerRootUIContainer.loadDefalutScriptFile(_uiPath);
				}
				DoUIModule _model = headerRootUIContainer.getRootView();
				_newView = (View) _model.getCurrentUIModuleView();
				// 设置headerView 的 宽高
				_newView.setLayoutParams(new LayoutParams((int) _model.getRealWidth(), (int) _model.getRealHeight()));
			} else {
				DoServiceContainer.getLogEngine().writeDebug("试图打开一个无效的页面文件:" + _uiPath);
			}
		}
		return _newView;
	}

	private View createFooterView(String _uiPath) throws Exception {
		View _newView = null;
		if (_uiPath != null && !"".equals(_uiPath.trim())) {
			this.footerUIPath = _uiPath;
			DoIPage _doPage = this.model.getCurrentPage();
			DoSourceFile _uiFile = _doPage.getCurrentApp().getSourceFS().getSourceByFileName(_uiPath);
			if (_uiFile != null) {
				footerRootUIContainer = new DoUIContainer(_doPage);
				footerRootUIContainer.loadFromFile(_uiFile, null, null);
				if (null != _doPage.getScriptEngine()) {
					footerRootUIContainer.loadDefalutScriptFile(_uiPath);
				}
				DoUIModule _model = footerRootUIContainer.getRootView();
				_newView = (View) _model.getCurrentUIModuleView();
				// 设置headerView 的 宽高
				_newView.setLayoutParams(new LayoutParams((int) _model.getRealWidth(), (int) _model.getRealHeight()));
			} else {
				DoServiceContainer.getLogEngine().writeDebug("试图打开一个无效的页面文件:" + _uiPath);
			}
		}
		return _newView;
	}

	private DoUIContainer headerRootUIContainer;
	private String headerUIPath;

	private DoUIContainer footerRootUIContainer;
	private String footerUIPath;

	public void loadDefalutScriptFile() throws Exception {
		if (headerRootUIContainer != null && headerUIPath != null) {
			headerRootUIContainer.loadDefalutScriptFile(headerUIPath);
		}
		if (footerRootUIContainer != null && footerUIPath != null) {
			footerRootUIContainer.loadDefalutScriptFile(footerUIPath);
		}
	}

	private boolean isHeaderVisible() throws Exception {
		DoProperty _property = this.model.getProperty("isHeaderVisible");
		if (_property == null) {
			return false;
		}
		return DoTextHelper.strToBool(_property.getValue(), false);
	}

	private boolean isFooterVisible() throws Exception {
		DoProperty _property = this.model.getProperty("isFooterVisible");
		if (_property == null) {
			return false;
		}
		return DoTextHelper.strToBool(_property.getValue(), false);
	}

	/**
	 * 获取当前model实例
	 */
	@Override
	public DoUIModule getModel() {
		return model;
	}

	interface DoIScrollView {
		// view.post方法里等待队列处理，view获得当前线程（即UI线程）的Handler;
		void scrollTo(int offset);

		void toBegin();

		int getScrollY();

		void toEnd();

		void isShowbar(boolean verticalScrollBarEnabled);

		void addFirstView(View childView);

		void setOnScrollChangedListener(OnScrollChangedListener listener);

		void screenShot(String fillPath, String fileName);

		int getDirection();
	}

	private OnScrollChangedListener scrollChangedListener;

	public interface OnScrollChangedListener {
		void scrollChanged(int l, int t, int oldl, int oldt);
	}

	class HScrollView extends HorizontalScrollView implements DoIScrollView {

		int mLastMotionX;
		int mLastMotionY;

		public HScrollView(Context context) {
			super(context);
			this.setFillViewport(true);
			this.setBackgroundColor(Color.TRANSPARENT);
			this.setHorizontalScrollBarEnabled(false);
		}

		@Override
		public void toBegin() {
			this.post(new Runnable() {
				@Override
				public void run() {
					fullScroll(ScrollView.FOCUS_LEFT);
				}
			});
		}

		@Override
		public void toEnd() {
			this.post(new Runnable() {
				@Override
				public void run() {
					fullScroll(ScrollView.FOCUS_RIGHT);
				}
			});
		}

		@Override
		public void isShowbar(boolean horizontalScrollBarEnabled) {
			this.setHorizontalScrollBarEnabled(horizontalScrollBarEnabled);
		}

		@Override
		public void addFirstView(View childView) {
			if (null != childView) {
				this.addView(childView);
			}
		}

		@Override
		protected void onScrollChanged(int l, int t, int oldl, int oldt) {
			super.onScrollChanged(l, t, oldl, oldt);
			if (null != scrollChangedListener) {
				scrollChangedListener.scrollChanged(l, t, oldl, oldt);
			}
		}

		@Override
		public void setOnScrollChangedListener(OnScrollChangedListener _listener) {
			scrollChangedListener = _listener;
		}

		@Override
		public boolean onInterceptTouchEvent(MotionEvent ev) {
			switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mLastMotionX = (int) ev.getRawX();
				mLastMotionY = (int) ev.getRawY();
				break;
			default:
				break;
			}
			return super.onInterceptTouchEvent(ev);
		}

		@SuppressLint("ClickableViewAccessibility")
		@Override
		public boolean onTouchEvent(MotionEvent ev) {
			int deltaX = (int) ev.getRawX() - mLastMotionX;
			int deltaY = (int) ev.getRawY() - mLastMotionY;
			double atan2 = Math.atan2(Math.abs(deltaY), Math.abs(deltaX));
			double slideAngle = (180 * atan2) / Math.PI;
			if (slideAngle > 35) {
				getParent().requestDisallowInterceptTouchEvent(false);
				return false;
			}
			switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				getParent().requestDisallowInterceptTouchEvent(true);
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				getParent().requestDisallowInterceptTouchEvent(false);
				break;
			default:
				break;
			}
			return super.onTouchEvent(ev);
		}

		@Override
		public void scrollTo(final int offset) {
			// 为解决 页面还未加载完成就执行scrollTo方法无效的问题
			this.post(new Runnable() {
				@Override
				public void run() {
					scrollTo((int) (offset * model.getXZoom()), 0);
				}
			});
		}

		@Override
		public void screenShot(String fillPath, String fileName) {
			int bgColor = 0;
			try {
				bgColor = DoUIModuleHelper.getColorFromString(model.getPropertyValue("bgColor"), 0);
			} catch (Exception e) {
				DoServiceContainer.getLogEngine().writeError("do_ScrollView_View", e);
			}
			getBitmapByViewHScrollView(this, fillPath, bgColor);
		}

		@Override
		public int getDirection() {
			return 0;
		}
	}

	class VScrollView extends ScrollView implements DoIScrollView {

		public VScrollView(Context context) {
			super(context);
			this.setFillViewport(true);

			this.setBackgroundColor(Color.TRANSPARENT);
			this.setVerticalScrollBarEnabled(false);
		}

		@Override
		public void toBegin() {
			this.post(new Runnable() {
				@Override
				public void run() {
					fullScroll(ScrollView.FOCUS_UP);
				}
			});
		}

		@Override
		public void toEnd() {
			this.post(new Runnable() {
				@Override
				public void run() {
					fullScroll(ScrollView.FOCUS_DOWN);
				}
			});
		}

		@Override
		public void isShowbar(boolean verticalScrollBarEnabled) {
			this.setVerticalScrollBarEnabled(verticalScrollBarEnabled);
		}

		@Override
		public void addFirstView(View childView) {
			if (null != childView) {
				this.addView(childView);
			}
		}

		@Override
		protected void onScrollChanged(int l, int t, int oldl, int oldt) {
			super.onScrollChanged(l, t, oldl, oldt);
			if (null != scrollChangedListener) {
				scrollChangedListener.scrollChanged(l, t, oldl, oldt);
			}
		}

		@Override
		public void setOnScrollChangedListener(OnScrollChangedListener _listener) {
			scrollChangedListener = _listener;
		}

		@Override
		public void scrollTo(final int offset) {
			// 为解决 页面还未加载完成就执行scrollTo方法无效的问题
			this.post(new Runnable() {
				@Override
				public void run() {
					scrollTo(0, (int) (offset * model.getYZoom()));
				}
			});
		}

		@Override
		public void screenShot(String fillPath, String fileName) {

			int bgColor = 0;
			try {
				bgColor = DoUIModuleHelper.getColorFromString(model.getPropertyValue("bgColor"), 0);
			} catch (Exception e) {
				DoServiceContainer.getLogEngine().writeError("do_ScrollView_View", e);
			}
			getBitmapByViewVScrollView(this, fillPath, bgColor);
		}

		@Override
		public int getDirection() {
			return 1;
		}
	}

	@Override
	public void toBegin(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		doIScrollView.toBegin();

	}

	@Override
	public void toEnd(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		doIScrollView.toEnd();

	}

	@Override
	public void scrollTo(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		int _offset = DoJsonHelper.getInt(_dictParas, "offset", 0);
		doIScrollView.scrollTo(_offset);
	}

	@Override
	public void rebound(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if (mPullState == PULL_DOWN_STATE) {
			savaTime(System.currentTimeMillis());
			onHeaderRefreshComplete();
		} else if (mPullState == PULL_UP_STATE) {
			onFooterRefreshComplete();
		}
	}

	@Override
	protected void fireEvent(int mState, int newTopMargin, String eventName) {
		int _height = mHeaderView.getHeight();
		int _offset = _height + newTopMargin;
		if (mState != PULL_TO_REFRESH) {
			if (mPullState == PULL_UP_STATE) {
				_offset = mFooterView.getHeight();
			} else {
				_offset = _height;
			}
		}
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		try {
			JSONObject _node = new JSONObject();
			_node.put("state", mState);
			_node.put("offset", (Math.abs(_offset) / this.model.getYZoom()) + "");
			_invokeResult.setResultNode(_node);
			this.model.getEventCenter().fireEvent(eventName, _invokeResult);
		} catch (Exception _err) {
			DoServiceContainer.getLogEngine().writeError("DoScrollView " + eventName + " \n", _err);
		}
	}

	@Override
	public void onFinishInflate() {
		super.onFinishInflate();
		int _direction = doIScrollView.getDirection();
		if (_direction == 0) {
			mHScrollView = (HorizontalScrollView) doIScrollView;
		} else {
			mVScrollView = (ScrollView) doIScrollView;
		}
	}

	@Override
	public void screenShot(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {

		String _path = _scriptEngine.getCurrentApp().getDataFS().getRootPath() + "/temp/do_ScrollView/";
		String _fileName = DoTextHelper.getTimestampStr() + ".jpg";

		String _fillPath = _path + _fileName;
		if (!DoIOHelper.existFile(_fillPath)) {
			DoIOHelper.createFile(_fillPath);
		}

		doIScrollView.screenShot(_fillPath, _fileName);
		DoInvokeResult _invokeResult = new DoInvokeResult(model.getUniqueKey());
		String _resultText = "data://temp/do_ScrollView/" + _fileName;
		_invokeResult.setResultText(_resultText);
		_scriptEngine.callback(_callbackFuncName, _invokeResult);
	}

	private String getBitmapByViewVScrollView(ScrollView scrollView, String fillPath, int color) {
		int w = 0;
		int h = 0;
		Bitmap bitmap = null;
		for (int i = 0; i < scrollView.getChildCount(); i++) {
			w += scrollView.getChildAt(i).getWidth();
			h += scrollView.getChildAt(i).getHeight();
		}
		if (color == 0) {
			bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		} else {
			bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		}

		final Canvas canvas = new Canvas(bitmap);
		try {
			canvas.drawColor(color);
		} catch (NumberFormatException e1) {
			e1.printStackTrace();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		scrollView.draw(canvas);

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(fillPath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			if (null != out) {
				if (color == 0) {
					bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
				} else {
					bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
				}
				out.flush();
				out.close();
			}
		} catch (IOException e) {
		}
		return fillPath;
	}

	private String getBitmapByViewHScrollView(HorizontalScrollView scrollView, String fillPath, int color) {
		int w = 0;
		int h = 0;
		Bitmap bitmap = null;
		for (int i = 0; i < scrollView.getChildCount(); i++) {
			w += scrollView.getChildAt(i).getWidth();
			h += scrollView.getChildAt(i).getHeight();
		}
		if (color == 0) {
			bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		} else {
			bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		}

		final Canvas canvas = new Canvas(bitmap);
		try {
			canvas.drawColor(color);

		} catch (NumberFormatException e1) {
			e1.printStackTrace();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		scrollView.draw(canvas);

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(fillPath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			if (null != out) {
				if (color == 0) {
					bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
				} else {
					bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
				}
				out.flush();
				out.close();
			}
		} catch (IOException e) {
		}
		return fillPath;
	}
}

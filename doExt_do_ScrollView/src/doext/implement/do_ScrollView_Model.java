package doext.implement;

import doext.define.do_ScrollView_MAbstract;

/**
 * 自定义扩展组件Model实现，继承Do_ScrollView_MAbstract抽象类；
 *
 */
public class do_ScrollView_Model extends do_ScrollView_MAbstract {

	public do_ScrollView_Model() throws Exception {
		super();
	}
	
	@Override
	public void didLoadView() throws Exception {
		super.didLoadView();
		((do_ScrollView_View)this.getCurrentUIModuleView()).loadDefalutScriptFile();
	}
}

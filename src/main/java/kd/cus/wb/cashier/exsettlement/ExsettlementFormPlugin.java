package kd.cus.wb.cashier.exsettlement;

import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.util.Date;
import java.util.EventObject;
import java.util.stream.Collectors;

import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.parameter.SystemParamServiceHelper;
import kd.cus.exchangerate.helpword.JMBaseDataHelper;
import org.apache.commons.lang3.StringUtils;
import org.stringtemplate.v4.compiler.STParser.namedArg_return;
import org.tmatesoft.sqljet.core.internal.lang.SqlParser.keyword_column_def_return;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.control.Toolbar;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.plugin.AbstractFormPlugin;

/**
 * 结算申请单表单插件
 * 
 * @author suyp
 *
 */
public class ExsettlementFormPlugin extends AbstractFormPlugin {

	@Override
	public void registerListener(EventObject e) {
		// TODO Auto-generated method stub
		super.registerListener(e);
		Toolbar repairDataBtnBar = this.getControl("tbmain");
		repairDataBtnBar.addItemClickListener(this);
	}

	/*@Override
	public void itemClick(ItemClickEvent evt) {
		// TODO Auto-generated method stub
		super.itemClick(evt);
		String key = evt.getItemKey();
		String opkey = evt.getOperationKey();
		if ("bar_save".equals(key)) {
			this.getModel().setValue("bfgy_exstatus", "1");
		}
	}*/

	@Override
	public void propertyChanged(PropertyChangedArgs e) {
		// TODO Auto-generated method stub
		super.propertyChanged(e);
		String name = e.getProperty().getName();
		ChangeData[] change = e.getChangeSet();
		if ("bfgy_outcurrency".equals(name)) {
//			this.getModel().setValue("bfgy_pretax", getRate());
		}

		if("bfgy_iscompanytax".equalsIgnoreCase(name) && (Boolean) change[0].getNewValue()){
			BigDecimal rate = getRate();
//			this.getModel().setValue("bfgy_pretax", rate);
		}

		String reason = "";

		//账户

		if ("bfgy_account".equalsIgnoreCase(name) || "bfgy_outcurrency".equalsIgnoreCase(name) || "bfgy_outamount".equalsIgnoreCase(name)){
			String reason_account = "";
			String reason_currency = "";
			String reason_amount = "";

			DynamicObjectCollection accounts = (DynamicObjectCollection)this.getModel().getValue("bfgy_account");
//			DynamicObjectCollection accounts = (DynamicObjectCollection)change[0].getNewValue();
			if (accounts != null && accounts.size() >0){
				String accountstr = accounts.stream().map(i->i.getDynamicObject("fbasedataid").getString("name")).collect(Collectors.joining(";"));
				if (StringUtils.isNotBlank(accountstr)){
					reason_account = "卖出账户：" + accountstr;
				}
			}

			DynamicObject currency = (DynamicObject)this.getModel().getValue("bfgy_outcurrency");
//			DynamicObject currency = (DynamicObject)change[0].getNewValue();
			if (currency != null){
				reason_currency = "，卖出币别：" + currency.getString("name");
			}

			BigDecimal outamount = (BigDecimal)this.getModel().getValue("bfgy_outamount");
//			BigDecimal outamount = (BigDecimal) change[0].getNewValue();
			if (outamount.compareTo(BigDecimal.ZERO) > 0){
				reason_amount = "，卖出币别金额：" + outamount.toString();
			}

			this.getModel().setValue("bfgy_reason",reason_account + "，结汇" + reason_currency + reason_amount);
		}
	}

	@Override
	public void afterBindData(EventObject e) {
		// TODO Auto-generated method stub
		super.afterBindData(e);
		getCreatorOrg();

		BigDecimal rate = getRate();
//		this.getModel().setValue("bfgy_pretax", rate);

		String orgbillno = (String)SystemParamServiceHelper.loadBillParameterObjectFromCache(this.getModel().getDataEntityType().getName()).get("bfgy_orgbillno");
		DynamicObject org_dy = BusinessDataServiceHelper.loadSingle("bos_org", "id", new QFilter[]{new QFilter("number", QCP.equals, orgbillno)});
		this.getModel().setValue("org",org_dy);
	}





	/**
	 * 根据申请人获得申请部门
	 */
	private void getCreatorOrg() {
		DynamicObject creator = (DynamicObject) this.getModel().getValue("creator");
		DynamicObjectCollection dpt = creator.getDynamicObjectCollection("entryentity");
		for (DynamicObject dynamicObject : dpt) {
			if (!dynamicObject.getBoolean("ispartjob")) {
				DynamicObject org = dynamicObject.getDynamicObject("dpt");
				this.getModel().setValue("bfgy_creatororg", org.getString("name"));
			}
		}
	}

	/**
	 * 修改为公司汇率
	 * 
	 * @return
	 */

	private BigDecimal getRate() {
		DynamicObject currency = (DynamicObject) this.getModel().getValue("bfgy_outcurrency");
		if (null != currency) {
			// 组织ID
			DynamicObject org = (DynamicObject) this.getModel().getValue("org");
			Long orgId = org.getLong("id");
			// 币别ID
			Long tarCurrencyId = (Long) currency.get("id");
			// 日期
			Date date = (Date) this.getModel().getValue("bfgy_taxdate");
			BigDecimal excRate = JMBaseDataHelper.getExcRate(orgId, tarCurrencyId, date);
			return excRate;
		}
		return new BigDecimal(0);
	}
}

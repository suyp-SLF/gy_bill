package kd.cus.wb.fi.revcm;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.bill.BillShowParameter;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityProperty;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.*;
import kd.bos.form.control.Toolbar;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.MessageBoxClosedEvent;
import kd.bos.list.LinkQueryPkId;
import kd.bos.list.LinkQueryPkIdCollection;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.servicehelper.org.OrgUnitServiceHelper;
import kd.bos.servicehelper.org.OrgViewType;
import kd.cus.exchangerate.helpword.JMBaseDataHelper;
import kd.cus.wb.report.AgeListDataPlugin;
import kd.fi.cas.helper.OrgHelper;

import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.EventObject;
import java.util.Map;

public class RevcmFormPlugin extends AbstractBillPlugIn {
	
	private static final String Key = "bfgy_baritemap";
	
	@Override
	public void registerListener(EventObject e) {
		super.registerListener(e);
		// 侦听主菜单按钮点击事件
		this.addItemClickListeners(Key);

		Toolbar repairDataBtnBar2 = this.getControl("bfgy_advcontoolbarap2");
		Toolbar repairDataBtnBar1 = this.getControl("bfgy_advcontoolbarap1");
		Toolbar	tbmain = this.getControl("tbmain");
		repairDataBtnBar2.addItemClickListener(this);
		repairDataBtnBar1.addItemClickListener(this);
		tbmain.addItemClickListener(this);
	}

	@Override
	public void beforeItemClick(BeforeItemClickEvent evt) {
		String itemkey = evt.getItemKey();
		if ("bar_submit".equalsIgnoreCase(itemkey)){
			BigDecimal amount = (BigDecimal)this.getModel().getValue("bfgy_amountfield5");
			if (amount.compareTo(new BigDecimal("0")) > 0){
				BigDecimal sumamount = new BigDecimal("0");
				DynamicObjectCollection nmsf_col = this.getModel().getEntryEntity("bfgy_entryentity");
				if (nmsf_col.size() > 0) {
					for (DynamicObject nmsf_col_item : nmsf_col)
						sumamount = sumamount.add(nmsf_col_item.getBigDecimal("bfgy_amountfield9"));
				}
					DynamicObjectCollection btfz_col = this.getModel().getEntryEntity("bfgy_entryentity1");
				if (btfz_col.size() > 0) {
					for (DynamicObject btfz_col_item : btfz_col)
						sumamount = sumamount.add(btfz_col_item.getBigDecimal("bfgy_amountfield10"));
				}
				ConfirmCallBackListener confirmCallBacks = new ConfirmCallBackListener("contentChange", this);
				sumamount = sumamount.setScale(2,BigDecimal.ROUND_DOWN);
				this.getView().showConfirm(String.format("本次确认税额为%s",sumamount.toString()), MessageBoxOptions.OKCancel, ConfirmTypes.Default, confirmCallBacks);
			}else {
				this.getView().invokeOperation("submit");
			}
		}else if("bfgy_baritemap1".equalsIgnoreCase(itemkey)){
			
			Boolean isPeriod = (Boolean) this.getModel().getValue("bfgy_isperiod");//wangqzh add 20210824 起初数据，不校验是否已经生成凭证
			if(isPeriod)
				return;
			
			DynamicObject this_dy = this.getModel().getDataEntity();
			QFilter[] qFilters = {new QFilter("id", QCP.equals,this_dy.getPkValue())};
			DynamicObject new_dy = BusinessDataServiceHelper.loadSingle("ar_revcfmbill", "isvoucher", qFilters);
			if (!new_dy.getBoolean("isvoucher")){
				this.getView().showTipNotification("未生成凭据，无法下推财务应付单！");
				evt.setCancel(true);
			}
		}
	}

	@Override
	public void confirmCallBack(MessageBoxClosedEvent messageBoxClosedEvent) {
		super.confirmCallBack(messageBoxClosedEvent);

		if (StringUtils.equals("contentChange", messageBoxClosedEvent.getCallBackId())
				&& messageBoxClosedEvent.getResult() == MessageBoxResult.Yes){
			this.getView().invokeOperation("submit");
		}
	}

	@Override
	public void itemClick(ItemClickEvent evt) {
		super.itemClick(evt);
//		if (StringUtils.equals(Key, evt.getItemKey())){
//			BigDecimal rate = countRate();
//			this.getModel().setValue("exchangerate", rate);
//		}

		if ("deleteentry02".equals(evt.getOperationKey())) {
			DynamicObjectCollection entry = this.getModel().getEntryEntity("bfgy_entryentity1");
			BigDecimal lotAmount = new BigDecimal(0);
			if (null != entry) {
				for (DynamicObject dy : entry) {
					if (null != dy.getBigDecimal("bfgy_amountfield10")) {
						lotAmount = lotAmount.add(dy.getBigDecimal("bfgy_amountfield10"));
					}
				}
			}
			this.getModel().setValue("bfgy_amountfield8", lotAmount);
		}

		if ("wb-deleteentry".equals(evt.getOperationKey())) {
			DynamicObjectCollection entry = this.getModel().getEntryEntity("bfgy_entryentity");
			BigDecimal lotAmount = new BigDecimal(0);
			if (null != entry) {
				for (DynamicObject dy : entry) {
					if (null != dy.getBigDecimal("bfgy_amountfield9")) {
						lotAmount = lotAmount.add(dy.getBigDecimal("bfgy_amountfield9"));
					}
				}
			}
			this.getModel().setValue("bfgy_amountfield8", lotAmount);
		}

		if ("bfgy_outretview".equalsIgnoreCase(evt.getItemKey())){
			String outrecnum = (String)this.getModel().getValue("bfgy_outrecnum");
			if (org.apache.commons.lang3.StringUtils.isNotBlank(outrecnum)){
				DynamicObject out_dy = BusinessDataServiceHelper.loadSingle("bfgy_wb_exportinvoice","id",new QFilter[]{new QFilter("bfgy_invoiceno",QCP.equals,outrecnum)});
				if (out_dy != null){
					BillShowParameter parameter = new BillShowParameter();
					parameter.setFormId("bfgy_wb_exportinvoice");
					parameter.setCaption("出口发票");
					parameter.getOpenStyle().setTargetKey("tabap");
					parameter.getOpenStyle().setShowType(ShowType.MainNewTabPage);
					parameter.setPkId(out_dy.getPkValue());
					this.getView().showForm(parameter);
				}else {
					this.getView().showTipNotification("未找到发票号为："+ outrecnum + "的出口发票！");
				}
			}else {
				this.getView().showTipNotification("出口发票号为空！，请选择出口发票进行查看");
			}
		}
	}
	
	@Override
	public void afterBindData(EventObject e) {
		// TODO Auto-generated method stub
		super.afterBindData(e);
		DynamicObject org = (DynamicObject) this.getModel().getValue("org");
		DynamicObject currency = (DynamicObject) this.getModel().getValue("currency");
		Date date = (Date) this.getModel().getValue("exratedate");
		if(null != org && null !=currency && null != date){
			BigDecimal rate = countRate();
			this.getModel().setValue("exchangerate", rate);
		}
	}
	
	
//	@Override
//	public void itemClick(ItemClickEvent evt) {
//		BigDecimal rate = countRate();
//		this.getModel().setValue("exchangerate", rate);
//	}


	@Override
	public void propertyChanged(PropertyChangedArgs e) {
		// TODO Auto-generated method stub
		super.propertyChanged(e);
		ChangeData[] change = e.getChangeSet();
		IDataEntityProperty obj = e.getProperty();

		Boolean ifbusitax = (Boolean) this.getModel().getValue("bfgy_ifbusitax");

		if(ifbusitax) {
			BigDecimal rate = countRate();
			this.getModel().setValue("exchangerate", rate);
		}
		// 修改汇率
//		if (change != null && obj != null && change.length > 0 && change[0].getNewValue().equals(Boolean.TRUE)
//				&& "bfgy_ifbusitax".equals(obj.getName())) {
//			BigDecimal rate = countRate();
//			this.getModel().setValue("exchangerate", rate);
//		}

		// 修改单据体微调税额更新单据头税额
		if (obj != null && "bfgy_amountfield9".equals(obj.getName())) {
			DynamicObjectCollection entry = this.getModel().getEntryEntity("bfgy_entryentity");
			BigDecimal lotAmount = new BigDecimal(0);
			if (null != entry) {
				for (DynamicObject dy : entry) {
					if (null != dy.getBigDecimal("bfgy_amountfield9")) {
						lotAmount = lotAmount.add(dy.getBigDecimal("bfgy_amountfield9"));
					}
				}
			}
			this.getModel().setValue("bfgy_amountfield8", lotAmount);
		}

		
		if (obj != null && "bfgy_amountfield10".equals(obj.getName())) {
			DynamicObjectCollection entry = this.getModel().getEntryEntity("bfgy_entryentity1");
			BigDecimal lotAmount = new BigDecimal(0);
			if (null != entry) {
				for (DynamicObject dy : entry) {
					if (null != dy.getBigDecimal("bfgy_amountfield10")) {
						lotAmount = lotAmount.add(dy.getBigDecimal("bfgy_amountfield10"));
					}
				}
			}
			this.getModel().setValue("bfgy_amountfield8", lotAmount);
		}
	}

	private BigDecimal countRate() {
		// 组织ID
		DynamicObject org = (DynamicObject) this.getModel().getValue("org");
		// 币别ID
		DynamicObject currency = (DynamicObject) this.getModel().getValue("currency");

		Date date = (Date) this.getModel().getValue("exratedate");
		if (null != org && null != currency && null != date) {
			Long orgId = org.getLong("id");
//			BigDecimal rate = countRate();
//			this.getModel().setValue("exchangerate", rate);

			Long tarCurrencyId = (Long) currency.get("id");
			// 日期

			BigDecimal excRate = JMBaseDataHelper.getExcRate(orgId, tarCurrencyId, date);
			return excRate;
		}
		return null;
	}

	@Override
	public void afterCreateNewData(EventObject e) {
		long org = RequestContext.get().getOrgId();
//		this.getModel().setValue("applyorg", businessId);//申请部门
		
		Map<String, Object> companyfromOrg = OrgUnitServiceHelper.getCompanyfromOrg(org);
		if (!companyfromOrg.isEmpty()) {
			Long companyId = (Long) companyfromOrg.get("id");
			DynamicObject company = OrgHelper.getOrgById(companyId); //获取当前业务单元
			this.getModel().setValue("org",company);
		}
		
		/* DynamicObject bos_org =  BusinessDataServiceHelper.loadSingle("bos_org",
				 "number",new QFilter[] {new QFilter("id", QCP.equals, org)});	
		
		  String  orgNumber=bos_org.getString("number");
		  if("8018".equals(orgNumber))
			  return;
		//bfgy_bos_org_wb_exts
		Map<Long, Long> superorg = OrgUnitServiceHelper.getDirectSuperiorOrg(OrgViewType.OrgUnit, Collections.singletonList(org));
		if (superorg.size()>0){
			DynamicObject superorgdy = BusinessDataServiceHelper.loadSingleFromCache(superorg.get(org),"bos_org");
			this.getModel().setValue("org",superorgdy);
		}*/
	}
}

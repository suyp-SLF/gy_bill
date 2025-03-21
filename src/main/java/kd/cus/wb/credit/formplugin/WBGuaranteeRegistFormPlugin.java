package kd.cus.wb.credit.formplugin;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.bill.BillShowParameter;
import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.dynamicobject.DynamicObjectType;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.*;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.events.MessageBoxClosedEvent;
import kd.bos.form.field.TextEdit;
import kd.bos.form.operate.FormOperate;
import kd.bos.id.ID;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.util.StringUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EventObject;
import java.util.Map;

/**
 * 保函登记单插件
 *
 * @author SHX
 * @date 2021-02-01.
 */
public class WBGuaranteeRegistFormPlugin extends AbstractBillPlugIn {

    // 复制
    @Override
    public void afterCopyData(EventObject e) {
        super.afterCopyData(e);
        // 版本号+1 所以要把V去掉
        String bbh = (String) this.getModel().getValue("bbh");
        String replace = bbh.replace("V", "");
        int i = Integer.parseInt(replace.trim());
        String V = "V";
        this.getModel().setValue("bbh", V + (i + 1));
        this.getModel().setValue("intbbh", i + 1);
        // 改函次数 = 版本号 - 1 改函日期保存
        this.getModel().setValue("ghcs", i);
        this.getModel().setValue("bfgy_change_date", new Date());

        IPageCache service = this.getView().getMainView().getService(IPageCache.class);
        String billNo = service.get("billlistNo");
        // 变更单回写登记单
        QFilter qFilter = new QFilter("registrationnumber", QCP.equals, billNo);
        QFilter qFilter1 = new QFilter("billstatus", QCP.equals, "C");
        DynamicObject[] load = BusinessDataServiceHelper.load("bfgy_guaranteechange_wb",
                "bghbhje,bfgy_change_end_date, bfgy_sfbgqtxx, bfgy_bgxxlx, bfgy_syrmc, bfgy_syrdz, qtsm,bfgy_sfbgsyrmc,bfgy_sfbgsyrdz,qtbgnr,sfbgbhdqr",
                qFilter.and(qFilter1).toArray(), "auditdate DESC");

        if (null != load && load.length > 0) {
            BigDecimal bghbhje = load[0].getBigDecimal("bghbhje");
            if (bghbhje.compareTo(new BigDecimal(0)) != 0) {
                this.getModel().setValue("bhamount", bghbhje);
            }
//<<<<<<< .mine
            //变更申请：是否变更保函有效期到期日
            Boolean sfbgbhdqr = load[0].getBoolean("sfbgbhdqr");
            if (sfbgbhdqr == true) {
				
            	//变更后保函有效期到期日
            	String bghbhdgr = load[0].getString("bfgy_change_end_date");
            	if (!"".equals(bghbhdgr) ||  !bghbhdgr.isEmpty() || bghbhdgr != null) {
            		this.getModel().setValue("bfgy_end_date", bghbhdgr);
            		
            		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            		Date enddate = null;
            		try {
            			enddate = sdf.parse(bghbhdgr);
            		} catch (ParseException ea) {
            			// TODO Auto-generated catch block
            			ea.printStackTrace();
            		}
            		Calendar cal = Calendar.getInstance();
            		cal.setTime(enddate);// 设置起时间:当前月的一号
            		cal.add(Calendar.MONTH, -1);//减少一个月
            		String bf = sdf.format(cal.getTime());
            		//到期前一个月
            		this.getModel().setValue("bfgy_datefield", bf);
            		
            		Calendar ovcal = Calendar.getInstance();
            		ovcal.setTime(enddate);// 设置起时间:当前月的一号
            		ovcal.add(Calendar.DATE, 1);//加一天
            		String ov = sdf.format(ovcal.getTime());
            		//超过效期
            		this.getModel().setValue("bfgy_datefield1", ov);
            		
            		Calendar ovcalthr = Calendar.getInstance();
            		ovcalthr.setTime(enddate);// 设置起时间:当前月的一号
            		ovcalthr.add(Calendar.MONTH, 3);//加三个月
            		String ovtr = sdf.format(ovcalthr.getTime());
            		//超过效期一个季度
            		this.getModel().setValue("bfgy_datefield2", ovtr);
            	}
			}
//||||||| .r2441
//            String bghbhdgr = load[0].getString("bfgy_change_end_date");
//            if (!"".equals(bghbhdgr)) {
//                    this.getModel().setValue("bfgy_end_date", bghbhdgr);
//            }
//=======
//            String bghbhdgr = load[0].getString("bfgy_change_end_date");
//            if (!"".equals(bghbhdgr)) {
//                    this.getModel().setValue("bfgy_end_date", bghbhdgr);
//                    
//                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//            		Date enddate = null;
//            		try {
//            			enddate = sdf.parse(bghbhdgr);
//            		} catch (ParseException e1) {
//            			// TODO Auto-generated catch block
//            			e1.printStackTrace();
//            		}
//            		Calendar cal = Calendar.getInstance();
//            		cal.setTime(enddate);// 设置起时间:当前月的一号
//            		cal.add(Calendar.MONTH, -1);//减少一个月
//            		String bf = sdf.format(cal.getTime());
//                    //到期前一个月
//                    this.getModel().setValue("bfgy_datefield", bf);
//                    
//                    Calendar ovcal = Calendar.getInstance();
//                    ovcal.setTime(enddate);// 设置起时间:当前月的一号
//                    ovcal.add(Calendar.DATE, 1);//加一天
//            		String ov = sdf.format(ovcal.getTime());
//                    //超过效期
//                    this.getModel().setValue("bfgy_datefield1", ov);
//                    
//                    Calendar ovcalthr = Calendar.getInstance();
//                    ovcalthr.setTime(enddate);// 设置起时间:当前月的一号
//                    ovcalthr.add(Calendar.MONTH, 3);//加三个月
//            		String ovtr = sdf.format(ovcalthr.getTime());
//                    //超过效期一个季度
//                    this.getModel().setValue("bfgy_datefield2", ovtr);
//            }
//>>>>>>> .r2730
//            if (load[0].get("bfgy_sfbgqtxx") != null) {
//                // 是否变更其他信息
//                boolean sfbgqtxx = load[0].getBoolean("bfgy_sfbgqtxx");
//                // 变更信息类型
//                String bgxxlx = load[0].getString("bfgy_bgxxlx");
//                if (sfbgqtxx) {
//                    if ("A".equals(bgxxlx)) {
//                        this.getModel().setValue("syrmc", load[0].getString("bfgy_syrmc"));
//                    }
//                    if ("B".equals(bgxxlx)) {
//                        this.getModel().setValue("bfgy_syrdz", load[0].getString("bfgy_syrdz"));
//                    }
//                }
//            }
            if (load[0].get("bfgy_sfbgsyrmc") != null) {
            	// 是否变更受益人名称
            	boolean bfgy_sfbgsyrmc = load[0].getBoolean("bfgy_sfbgsyrmc");
            	if (bfgy_sfbgsyrmc) {          		
            			this.getModel().setValue("syrmc", load[0].getString("bfgy_syrmc"));
            	}
            }
            if (load[0].get("bfgy_sfbgsyrdz") != null) {
            	// 是否变更受益人地址
            	boolean bfgy_sfbgsyrdz = load[0].getBoolean("bfgy_sfbgsyrdz");
            	if (bfgy_sfbgsyrdz) {
            			this.getModel().setValue("bfgy_syrdz", load[0].getString("bfgy_syrdz"));
            	}
            }
            

            // 其他说明
            String qtsm = load[0].getString("qtsm");
            if (!"".equals(qtsm)) {
                this.getModel().setValue("bfgy_qtsm", qtsm);
            }
            // 其他变更内容
            String qtbgnr = load[0].getString("qtbgnr");
            if (!"".equals(qtbgnr)) {
            	this.getModel().setValue("bfgy_qtbgnr", qtbgnr);
            }

        }
    }

    @Override
    public void afterLoadData(EventObject e) {
        super.afterLoadData(e);

        // 查询意见征询记录
        QFilter opFilter1 = new QFilter("bfgy_bh_number", QCP.equals, this.getModel().getValue("bhnumber"));
        QFilter opFilter2 = new QFilter("billstatus", QCP.equals, "C");
        Map<Object, DynamicObject> map = BusinessDataServiceHelper.loadFromCache("bfgy_consult_wb",
                "bfgy_consult_content, bfgy_reply_content, creator, createtime, auditor, auditdate, bfgy_attachment",
                opFilter1.and(opFilter2).toArray());
        DynamicObjectType type = EntityMetadataCache.getDataEntityType("bd_attachment");
        for (Object key : map.keySet()) {
            DynamicObject dyn = map.get(key);
            int index = this.getModel().createNewEntryRow("bfgy_consult_entryentity");
            this.getModel().setValue("bfgy_consultation", dyn.get("bfgy_consult_content"), index);
            this.getModel().setValue("bfgy_reply", dyn.get("bfgy_reply_content"), index);
            this.getModel().setValue("bfgy_consulter", dyn.get("creator"), index);
            this.getModel().setValue("bfgy_consult_date", dyn.get("createtime"), index);
            this.getModel().setValue("bfgy_project_manager", dyn.get("auditor"), index);
            this.getModel().setValue("bfgy_reply_date", dyn.get("auditdate"), index);
            DynamicObjectCollection attachmentCollection = dyn.getDynamicObjectCollection("bfgy_attachment");
            DynamicObjectCollection dynamicObjectCollection = (DynamicObjectCollection) this.getModel().getValue("bfgy_attachment", index);
            StringBuilder filename = new StringBuilder();
            for (DynamicObject attachment : attachmentCollection) {
                DynamicObject temp = BusinessDataServiceHelper.loadSingleFromCache(attachment.getDynamicObject("fbasedataId").get("id"), type);
                filename.append(filename.length() > 0 ? ", " + temp.getString("name") : temp.getString("name"));
                dynamicObjectCollection.addNew().set("fbasedataId", temp);
            }
            if (filename.length() > 0) {
                this.getModel().setValue("bfgy_filename", filename.toString(), index);
            }


        }

        // 查询保函费
        QFilter costFilter1 = new QFilter("expenseentryentity.bfgy_wb_guaranteeno", QCP.equals, this.getModel().getValue("bhnumber"));
        QFilter costFilter2 = new QFilter("billstatus", QCP.equals, "C");
        DynamicObjectCollection dynamicObjectCollection = QueryServiceHelper.query("er_publicreimbursebill",
                "billno, expenseentryentity.bfgy_wb_guaranteeno guaranteeno, expenseentryentity.bfgy_wb_startdate startdate, " +
                        "expenseentryentity.bfgy_wb_enddate enddate, expenseentryentity.bfgy_wb_guaramount guaramount",
                costFilter1.and(costFilter2).toArray());
        for (DynamicObject dynamicObject : dynamicObjectCollection) {
            int index = this.getModel().createNewEntryRow("bfgy_cost_entryentity");
            this.getModel().setValue("bfgy_billno", dynamicObject.get("billno"), index);
            this.getModel().setValue("bfgy_guaranteeno", dynamicObject.get("guaranteeno"), index);
            this.getModel().setValue("bfgy_charge_startdate", dynamicObject.get("startdate"), index);
            this.getModel().setValue("bfgy_charge_enddate", dynamicObject.get("enddate"), index);
            this.getModel().setValue("bfgy_guaranteeamount", dynamicObject.get("guaramount"), index);
        }
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String key = e.getProperty().getName();
        ChangeData[] changeData = e.getChangeSet();
        Object newValue = changeData[0].getNewValue();
        Object oldValue = changeData[0].getOldValue();
        if (newValue != oldValue) {
            switch (key) {
                case "bhamount":
                    this.CompareAmount();
                    break;
                default:
                    break;
            }
        }
    }

    private void CompareAmount() {
        BigDecimal htamount = (BigDecimal) this.getModel().getValue("htamount");
        BigDecimal bhamount = (BigDecimal) this.getModel().getValue("bhamount");
        if (htamount.compareTo(bhamount) == -1) {
            this.getModel().beginInit();
            this.getModel().setValue("bhamount", null);
            this.getModel().endInit();
        }
    }

    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);
        // 点击按钮
        FormOperate operate = (FormOperate) args.getSource();
        String operateKey = operate.getOperateKey();
        super.beforeDoOperation(args);
        // 提交
        if ("closesubmit".equals(operateKey)) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String format = simpleDateFormat.format(new Date());
            try {
                Date date = simpleDateFormat.parse(format);
                this.getModel().setValue("bjrq", date);
                this.getModel().setValue("thdate", date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (this.getModel().getValue("bfgy_start_date") == null) {
                this.getModel().setValue("bfgy_start_date", this.getModel().getValue("bhkcr"));
            }
            this.getView().setEnable(false, "bhsxlx", "thdate", "bjrq", "closeremark");
            this.getModel().setValue("bhstatus", "CX");
            this.getView().invokeOperation("save");
            this.getView().showSuccessNotification("保函提交成功");
            this.getModel().setDataChanged(false);

        }
        if ("submit".equals(operateKey)) {
            if (this.getModel().getValue("bfgy_start_date") == null) {
                this.getModel().setValue("bfgy_start_date", this.getModel().getValue("bhkcr"));
            }
        }
        // 撤销
        if ("unclosesubmit".equals(operateKey)) {
            this.getModel().setValue("bhstatus", "KL");
            this.getView().invokeOperation("save");
            this.getView().showSuccessNotification("保函撤销成功");
            this.getModel().setDataChanged(false);
            this.getView().setEnable(true, "bhsxlx", "thdate", "bjrq", "closeremark");
        }
        // 当反审核操作时做标志字段判断
        if ("push".equals(operateKey)) {
            Boolean flag = (Boolean) this.getModel().getValue("isdown");
            // 标志值为true时不允许反审核
            if (flag) {
                args.setCancel(true);
                this.getView().showErrorNotification("已存在相关联的保函变更单");
            }
        }
        // 退出操作
        if ("close".equals(operateKey)) {
            BillShowParameter parameter = (BillShowParameter) this.getView().getFormShowParameter();
            String formName = parameter.getFormName();
            if ("保函撤销".equals(formName)) {
                this.getView().getParentView().invokeOperation("refresh");
            }
        }
    }

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addItemClickListeners("tbmain");
        TextEdit textEdit = this.getView().getControl("htno");
        textEdit.addClickListener(this);
    }

    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        String itemKey = evt.getItemKey();
        if ("bar_submit".equals(itemKey)) {
            String bbh = (String) this.getModel().getValue("bbh");
            if ("V1".equals(bbh)) {
                ConfirmCallBackListener confirmCallBacks = new ConfirmCallBackListener("submit", this);
                String bhnumber = (String) this.getModel().getValue("bhnumber");
                this.getView().showConfirm("请确认保函编号:" + bhnumber + "是否输入正确", MessageBoxOptions.YesNo,
                        ConfirmTypes.Default, confirmCallBacks);
                evt.setCancel(true);
            }
        }
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        if ("bfgy_addpro".equals(evt.getItemKey())) {
            if (this.getModel().getValue("bfgy_project_no") == null || this.getModel().getValue("xmmc") == null ||
                    this.getModel().getValue("bfgy_project_no") == "" || this.getModel().getValue("xmmc") == "") {
                String htno = (String) this.getModel().getValue("htno");
                QFilter filter = new QFilter("billno", QCP.equals, htno);
                DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load("bfgy_wbexportcontract", "bfgy_projno, bfgy_projname", filter.toArray());
                if (dynamicObjects != null && dynamicObjects.length > 0) {
                    DynamicObject dynamicObject = dynamicObjects[0];
                    this.getModel().setValue("bfgy_project_no", dynamicObject.get("bfgy_projno"));
                    this.getModel().setValue("xmmc", dynamicObject.get("bfgy_projname"));
                    this.getView().invokeOperation("save");
                }
            }
        }
    }

    @Override
    public void click(EventObject evt) {
        super.click(evt);
        // source点击资源
        TextEdit source = (TextEdit) evt.getSource();
        // 点击标识
        String key = source.getKey();
        if ("htno".equals(key)) {
            this.getView().invokeOperation("drawcontract");
        }
    }

    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);

        this.SetControlVisible();

        String caption = this.getView().getParentView().getFormShowParameter().getCaption();
        if ("保函开立申请单".equals(caption)) {
            this.getView().getParentView().invokeOperation("close");
            this.getView().sendFormAction(this.getView().getParentView());
        }
//		this.getView().setEnable(false, "dlrinfo");
//		this.getView().setEnable(false, "ddbrankname");
        // 审核状态
        String billstatus = (String) this.getModel().getValue("billstatus");
        if ("C".equals(billstatus)) {
            this.getView().setEnable(false, "htno", "bfgy_zxf");
        }
    }

    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);

        this.getModel().setValue("bhklsqdh", ID.genLongId());

        // 新增的时候没有提交
        this.getView().setVisible(false, "bjrq", "closeremark", "thdate", "bhsxlx", "bar_sumbitm");
    }

    /**
     * 新增或者撤销时按钮显示
     */
    private void SetControlVisible() {
        FormShowParameter parameter = this.getView().getFormShowParameter();
        // 是否时撤销
        String cxFlag = parameter.getCustomParam("isclose");
        if (StringUtils.isNotEmpty(cxFlag) && "yes".equals(cxFlag)) {
            // 撤销时，需要设置按钮可见性

            // 隐藏所有分录
            this.getView().setVisible(false, "attachementadv1", "bfgy_guarantee_wb", "bfgy_consult_adv", "attachementadv");
            // 隐藏面板
            this.getView().setVisible(false, "fieldsetpanelap", "flexpanelap1111", "flexpanelap");
            // 隐藏按钮
            this.getView().setVisible(false, "bar_new", "bar_print", "bar_audit", "bar_unaudit", "barpush", "viewflowchart");
            // 隐藏基本信息字段
            this.getView().setVisible(false, "billno", "creator", "createtime", "htcurrency", "bfgy_bhsqr", "syrmc"
                    , "bfgy_syrmc_fbs", "bfgy_syrdz", "billstatus", "djywcombo", "tbno");
            // 隐藏开具信息字段
            this.getView().setVisible(false, "bfgy_xkdk", "kjfs", "kjfstext", "domesticbank", "ddbrankname", "dlrinfo"
                    , "country", "bfgy_final_date", "bfgy_change_date", "ghcs", "bbh");


//            this.getView().setVisible(false, "bar_new", "fieldsetpanelap", "flexpanelap", "attachementadv",
//                    "bar_print", "attachementadv1", "flexpanelap1111", "billno", "bhnumber", "xmmc", "contracttype",
//                    "htno", "syrmc", "htcurrency", "htamount", "djorg", "jbuser", "djdate", "billstatus", "bbh1",
//                    "intbbh1", "djywcombo", "tbno", "bhklsqdh", "bhlxcombo", "bhamount", "bhcurrency", "bhyxqqsr",
//                    "kjfs", "domesticbank", "ddbrankname", "dlrinfo", "bhkcr", "bhkllx", "country", "ghcs",
//                    "bhyxqdqr", "bar_unaudit", "fdbbhjzr", "toebs", "bar_audit", "ispush", "isdown", "barpush",
//                    "intbbh", "bbh", "loginorg", "creator", "createtime", "viewflowchart", "operator",
//                    "banknoticeno");
            this.getView().setVisible(true, "bar_sumbitm");
        } else {
            this.getView().setVisible(false, "bar_sumbitm", "bjrq", "closeremark", "thdate", "bhsxlx");
            this.getView().setEnable(false, "bjrq", "closeremark", "thdate", "bhsxlx");
        }

        boolean status = OperationStatus.ADDNEW.equals(getView().getFormShowParameter().getStatus());
        if (!status) {
            // 当从查看历史版本进入
            Boolean isnew = (Boolean) this.getModel().getValue("bfgy_isnew");
            if (!isnew) {
                this.getView().setVisible(false, "bar_new", "bar_save", "bar_submit", "bar_audit", "bar_unaudit",
                        "viewflowchart", "tblsqchange", "bar_sumbitm", "toebs");
            }
        }
    }

    @Override
    public void confirmCallBack(MessageBoxClosedEvent messageBoxClosedEvent) {
        super.confirmCallBack(messageBoxClosedEvent);
        String callBackId = messageBoxClosedEvent.getCallBackId();

        if ("close".equals(callBackId)) {
            this.getModel().setValue("bhstatus", "CLOSED");
            this.getView().showSuccessNotification("保函关闭成功");
            this.getView().invokeOperation("save");
        }
        if ("unclose".equals(callBackId)) {
            this.getModel().setValue("bhstatus", "VERIFIED");
            this.getView().showSuccessNotification("保函打开成功");
            this.getView().invokeOperation("save");
        }
        if ("submit".equals(callBackId)) {
            this.getView().invokeOperation("submit");
        }
    }

    // 保函开立申请单——保函登记单 1V1
    @Override
    public void afterDoOperation(AfterDoOperationEventArgs afterDoOperationEventArgs) {
        super.afterDoOperation(afterDoOperationEventArgs);
        String bhklsqdh = (String) this.getModel().getValue("bhklsqdh");
        String billno = (String) this.getModel().getValue("billno");
        QFilter qFilter = new QFilter("bhklsqdh", QCP.equals, bhklsqdh);
        ArrayList<QFilter> filters = new ArrayList<QFilter>();
        filters.add(qFilter);
        Integer maxbbh = 0;

        String operateKey = afterDoOperationEventArgs.getOperateKey();
        if ("save".equals(operateKey) || "submitandnew".equals(operateKey) || "submit".equals(operateKey)) {
            // 当点击保存 提交的时候
            this.getModel().setValue("ispush", true);
            DynamicObject[] objs = BusinessDataServiceHelper.load("bfgy_bhdjd_wb", "billno,intbbh,bfgy_isnew",
                    filters.toArray(new QFilter[0]));
            for (DynamicObject obj : objs) {
                Integer bbh = obj.getInt("intbbh");
                if (bbh.compareTo(maxbbh) > 0)
                    maxbbh = bbh;
            }
            for (DynamicObject obj : objs) {
                Integer bbh = obj.getInt("intbbh");
                if (bbh.compareTo(maxbbh) == 0) {
                    obj.set("bfgy_isnew", true);
                    obj.set("bfgy_isnew", true);
                } else {
                    obj.set("bfgy_isnew", false);
                }
            }
            SaveServiceHelper.save(objs);

            // 查询下游单据的数据是否已执行变更
            QFilter source = new QFilter("registrationnumber", QCP.equals, billno);
            DynamicObject[] load = BusinessDataServiceHelper.load("bfgy_guaranteechange_wb", "iszxchenge",
                    new QFilter[]{source});
            if (null != load && load.length > 0) {
                // 如果有的话
                load[0].set("iszxchenge", true);
                SaveServiceHelper.save(load);
            }
        }
        if ("delete".equals(operateKey)) {
            // 当点击删除的时候
            this.getModel().setValue("ispush", false);
            this.getModel().setDataChanged(false);
            QFilter qFilter1 = new QFilter("billno", QCP.not_equals, billno);
            filters.add(qFilter1);
            DynamicObject[] objs = BusinessDataServiceHelper.load("bfgy_bhdjd_wb", "billno,intbbh,bfgy_isnew",
                    filters.toArray(new QFilter[0]));
            for (DynamicObject obj : objs) {
                Integer bbh = obj.getInt("intbbh");
                if (bbh.compareTo(maxbbh) > 0)
                    maxbbh = bbh;
            }
            for (DynamicObject obj : objs) {
                Integer bbh = obj.getInt("intbbh");
                if (bbh.compareTo(maxbbh) == 0) {
                    obj.set("bfgy_isnew", 1);
                } else {
                    obj.set("bfgy_isnew", 0);
                }
            }
            SaveServiceHelper.save(objs);

            // 查询下游单据的数据是否已执行变更
            QFilter source = new QFilter("registrationnumber", QCP.equals, billno);
            DynamicObject[] load = BusinessDataServiceHelper.load("bfgy_guaranteechange_wb", "iszxchenge",
                    new QFilter[]{source});
            if (null != load && load.length > 0) {
                // 如果有的话
                load[0].set("iszxchenge", false);
                SaveServiceHelper.save(load);
            }
        }
    }
}


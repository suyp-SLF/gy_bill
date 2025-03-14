package kd.cus.wb.newreport;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kd.bd.master.util.FormShowParameterUtil;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityProperty;
import kd.bos.entity.datamodel.events.BizDataEventArgs;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.ext.form.control.CustomControl;
import kd.bos.form.IClientViewProxy;
import kd.bos.form.control.Control;
import kd.bos.form.control.Html;
import kd.bos.form.control.events.BeforeClickEvent;
import kd.bos.form.events.CustomEventArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.ssc.task.helper.FormShowParameterHelper;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * 高质量发展奖|报表表单插件
 * @author suyp
 *
 */
public class HighawardFormPlugin extends AbstractBillPlugIn {

    private static final String PROJECT_TABLE = "bfgy_proj_wb_pmb";//项目表单
    private static final String ECON_TABLE = "bfgy_wbexportcontract";//出口合同
    private static final String PCON_TABLE = "bfgy_pm_wb_proconstract";//采购合同
    private static final String CAS_TABLE = "cas_recbill";//收款单

    private static final String VOUCHER_TABLE = "gl_voucher";

    private static final String BASE_PROJECT_TABLE = "bd_project";

    /*合同收入，直接成本*/
    private String ECONS = "";//出口合同,金额明细
    private String PCONS = "";//采购合同,金额明细

    private String ECONS_SUM = "";//出口合同合计
    private String SUM_CURR_ECONS = "";//分币种合计表
    private String PCONS_SUM = "";//采购合同合计

    private static DynamicObject[] vouchers = null;
    /*项目基本情况*/
    private String PROJECT_NAME = "未找到";
    private String PROJECT_CODE = "未找到";

    private BigDecimal CONTRACT_INCOME = BigDecimal.ZERO;//合同收入
    private BigDecimal CONTRACT_INTHEED = BigDecimal.ZERO;//合同已收汇金额
    private BigDecimal ACCOUNT_INTHEED = BigDecimal.ZERO;//核算期已收汇金额
    private BigDecimal CONTRACT_DIRECT_COST = BigDecimal.ZERO;//合同直接成本
    private BigDecimal CONTRACT_INDIRECT_COST = BigDecimal.ZERO;//合同间接成本
    private BigDecimal PROJECT_FUNDS = BigDecimal.ZERO;//现场项目经费
    private BigDecimal PROFITS = BigDecimal.ZERO;//预测/实际利润
    private BigDecimal PROFITS_MARGIN = null;//预测实际利润率


    private String JJ_xybx, JJ_ysbx, JJ_yyf, JJ_nlyzf, JJ_bzf, JJ_ccbgf, JJ_jpglf, JJ_cgf, JJ_jtf, JJ_lpf, JJ_ywzdf, JJ_hyf, JJ_clf, JJ_bhf, JJ_pxf, JJ_yj, JJ_yhsxf, JJ_hdsy, JJ_ygcgbx, JJ_ypf, JJ_pxf1, JJ_lwf, JJ_hsscjrjzzf, JJ_qt;
    private BigDecimal JJ_tsje = BigDecimal.ZERO;
    private BigDecimal JJ_SUM = BigDecimal.ZERO;
    private BigDecimal JJ_xcxmjf = BigDecimal.ZERO;
    private Map<String, BigDecimal> INDIRECT_COST_MAP;

    private BigDecimal YG_zjcb = BigDecimal.ZERO;
    private BigDecimal YG_yj = BigDecimal.ZERO;
    private BigDecimal YG_xcf = BigDecimal.ZERO;
    private BigDecimal YG_qtfy = BigDecimal.ZERO;
    private BigDecimal YG_hyf = BigDecimal.ZERO;
    private BigDecimal YG_ckbxf = BigDecimal.ZERO;

    private String HT_HTYSH = "";
    private BigDecimal HT_HTYSH_BL = null;
    private String HT_HSQYSH = "";
    private BigDecimal HT_HSQYSH_BL = BigDecimal.ZERO;

    private String LOD_kjyfzc = "";
    private String LOD_gzcg = "";
    private String LOD_hlkzxcfyqk = "";
    private String LOD_cktsgzwcqk = "";
    private String LOD_heshfx = "";
    private String LOD_dfjdcgcb = "";
    private String LOD_projectnum = "";
    private String LOD_rate = "";

    private JSONArray LOD_values = new JSONArray();

    @Override
    public void beforeBindData(EventObject e) {
        super.beforeBindData(e);
        // 动态设置HTML内容
    }

    @Override
    public void customEvent(CustomEventArgs e) {
        String params = e.getEventArgs();
        JSONObject jsonobject = JSON.parseObject(params);

        String projectnum = jsonobject.getString("projectnum");//项目编码

        String kjyfzc = jsonobject.getString("kjyfzc");//科技研发支出
        String gzcg = jsonobject.getString("gzcg");//工作成果
        String hlkzxcfyqk = jsonobject.getString("hlkzxcfyqk");//合理控制现场费用情况
        String cktsgzwcqk = jsonobject.getString("cktsgzwcqk");//出口退税工作完成情况
        String heshfx = jsonobject.getString("heshfx");//合同收汇风险
        String dfjdcgcb = jsonobject.getString("dfjdcgcb");//大幅降低采购成本
        String rate = jsonobject.getString("rate");//
        JSONArray values = jsonobject.getJSONArray("values");//合同收入

        QFilter[] qFilters = {new QFilter("bfgy_projectnum",QCP.equals,projectnum)};
        DynamicObject this_dy = BusinessDataServiceHelper.loadSingle("bfgy_highawardpaper", "bfgy_kjyfzc,bfgy_gzcg,bfgy_hlkzxcfyqk,bfgy_cktsgzwcqk,bfgy_heshfx,bfgy_dfjdcgcb,bfgy_projectnum,bfgy_values.bfgy_idname,bfgy_values.bfgy_value,bfgy_rate", qFilters);
        if (this_dy == null){
            this_dy = BusinessDataServiceHelper.newDynamicObject("bfgy_highawardpaper");
        }
        this_dy.set("bfgy_kjyfzc",kjyfzc);
        this_dy.set("bfgy_gzcg",gzcg);
        this_dy.set("bfgy_hlkzxcfyqk",hlkzxcfyqk);
        this_dy.set("bfgy_cktsgzwcqk",cktsgzwcqk);
        this_dy.set("bfgy_heshfx",heshfx);
        this_dy.set("bfgy_dfjdcgcb",dfjdcgcb);
        this_dy.set("bfgy_projectnum",projectnum);
        this_dy.set("bfgy_rate",rate);
        DynamicObjectCollection this_dy_col = this_dy.getDynamicObjectCollection("bfgy_values");
        this_dy_col.clear();
        for (int i = 0; i < values.size(); i++) {
            DynamicObject col_dy = new DynamicObject(this_dy_col.getDynamicObjectType());
            JSONObject item = values.getJSONObject(i);
            String id_col = item.getString("id");
            String value_col = item.getString("value");

            col_dy.set("bfgy_idname",id_col);
            col_dy.set("bfgy_value",value_col);
            this_dy_col.add(col_dy);
        }
        SaveServiceHelper.save(new DynamicObject[]{this_dy});
    }

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        Html html = this.getView().getControl("bfgy_htmlap");
        IClientViewProxy proxy = (IClientViewProxy) this.getView().getService(IClientViewProxy.class);
        this.getView().getControl("html");
    }

    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);

    }

    @Override
    public void afterCreateNewData(EventObject e) {

        Date enddate = HighawardReportFormPlugin.enddate;
        Date startdate = HighawardReportFormPlugin.startdate;

        QFilter[] qFilters_v = new QFilter[3];
        qFilters_v[0] = new QFilter("ispost", QCP.equals, true);
        if (null != enddate){
            qFilters_v[1] = new QFilter("bookeddate", QCP.less_than, enddate);
        }
        if (null != startdate){
            qFilters_v[2] = new QFilter("bookeddate", QCP.large_equals, startdate);
        }
        
        QFilter[] qFilters_IN = new QFilter[3];
        if (null != enddate){
        	qFilters_IN[0] = new QFilter("bfgy_noticedate", QCP.less_than, enddate);
        }
        if (null != startdate){
        	qFilters_IN[1] = new QFilter("bfgy_noticedate", QCP.large_equals, startdate);
        }

        vouchers = BusinessDataServiceHelper.load(VOUCHER_TABLE, "id,entries.account,entries.assgrp,entries.debitlocal,entries.debitori", qFilters_v);

        String pkid = this.getView().getFormShowParameter().getCustomParam("cus_id_value");

        DynamicObject project_dy = BusinessDataServiceHelper.loadSingle(pkid, PROJECT_TABLE);

        DynamicObject base_project = BusinessDataServiceHelper.loadSingle(BASE_PROJECT_TABLE, "id", new QFilter[]{new QFilter("number", QCP.equals, project_dy.getString("bfgy_projno"))});

//        DynamicObjectCollection econ_cols = project_dy.getDynamicObjectCollection("bfgy_econstractentity");//出口合同
//        DynamicObjectCollection pcon_cols = project_dy.getDynamicObjectCollection("bfgy_pconstractentity");//采购合同


        QFilter[] qFilters_cas = new QFilter[]{new QFilter("bfgy_projbd.id",QCP.equals,pkid)};
        DynamicObjectCollection cas_cols = QueryServiceHelper.query(CAS_TABLE, "bfgy_conamountlocal,bfgy_amountfield", qFilters_cas);

//        cas_cols.forEach(m->{
//            HT_HTYSH = HT_HTYSH.add(m.getBigDecimal("bfgy_conamountlocal"));
//            HT_HSQYSH = HT_HSQYSH.add(m.getBigDecimal("bfgy_amountfield"));
//        });
        
        

        DynamicObjectCollection econ_cols = QueryServiceHelper.query(ECON_TABLE, "billno,bfgy_projno,bfgy_amountfield_wb,bfgy_rate,bfgy_currency.name", new QFilter[]{new QFilter("bfgy_projno", QCP.equals, project_dy.getString("bfgy_projno"))});
        DynamicObjectCollection pcon_cols = QueryServiceHelper.query(PCON_TABLE, "billno,bfgy_sleprojno,bfgy_rmbamount", new QFilter[]{new QFilter("bfgy_sleprojno", QCP.equals, project_dy.getString("bfgy_projno"))});
        List<String[]> econ_list = new ArrayList<>();
        List<String[]> pcon_list = new ArrayList<>();
        Map<String, BigDecimal> currencyMap = new HashMap<>();
        BigDecimal econ_rmb_sumamount = BigDecimal.ZERO;
        BigDecimal pcon_rmb_sumamount = BigDecimal.ZERO;
        for (DynamicObject econ_col : econ_cols) {
            if (null == currencyMap.get(econ_col.getString("bfgy_currency.name"))) {
                currencyMap.put(econ_col.getString("bfgy_currency.name"), econ_col.getBigDecimal("bfgy_amountfield_wb"));
            } else {
                currencyMap.put(econ_col.getString("bfgy_currency.name"), currencyMap.get(econ_col.getString("bfgy_currency.name")).add(econ_col.getBigDecimal("bfgy_amountfield_wb")));
            }
            BigDecimal econ_rmb_amount = econ_col.getBigDecimal("bfgy_amountfield_wb");
            BigDecimal econ_loc_amount = econ_rmb_amount.multiply(econ_col.getBigDecimal("bfgy_rate"));
            econ_rmb_sumamount = econ_rmb_sumamount.add(econ_loc_amount);
            econ_list.add(new String[]{econ_col.getString("billno"), econ_rmb_amount.setScale(2,2).toString(), econ_col.getString("bfgy_currency.name"), econ_col.getString("bfgy_rate"), "<input id=\"HTSR_" + econ_col.getString("billno") + "\" class=\"yjbl\" type=\"text\" style=\"width: 90%; height: 100%; border: 0px; text-align-last: right;\" /><label>%</label>"});
        }
        for (DynamicObject pcon_col : pcon_cols) {
            BigDecimal pcon_rmb_amount = pcon_col.getBigDecimal("bfgy_rmbamount");
            pcon_rmb_sumamount = pcon_rmb_sumamount.add(pcon_rmb_amount);
            pcon_list.add(new String[]{pcon_col.getString("billno"), pcon_rmb_amount.setScale(2,2).toString()});
        }

        List<String[]> currenctList = new ArrayList<>();
        currencyMap.entrySet().forEach(m -> {
            currenctList.add(new String[]{m.getKey(), m.getValue().setScale(2,2).toString()});
        });

        //制表
        ECONS = formatTable(econ_list, null);

        SUM_CURR_ECONS = formatTable(currenctList, new String[]{"2", "3"});

        PCONS = formatTable(pcon_list, null);

        ECONS_SUM = econ_rmb_sumamount.setScale(2,2).toString();
        PCONS_SUM = pcon_rmb_sumamount.setScale(2,2).toString();

        /*项目基本情况*/
        PROJECT_NAME = project_dy.getString("bfgy_projname");
        PROJECT_CODE = project_dy.getString("bfgy_projno");
        
        DynamicObjectCollection income_SH_dys = QueryServiceHelper.query("cas_recbill", "entry.e_receivablelocamt,bfgy_biztypes.number,entry.bfgy_conproj,entry.project,entry.bfgy_proj", new QFilter[] {new QFilter("entry.bfgy_proj.number", QCP.equals, PROJECT_CODE), new QFilter("bfgy_biztypes.number", QCP.equals, "WBSR04")});//
        
       
        
        BigDecimal incomein_1 = income_SH_dys.stream().map(i->i.getBigDecimal("entry.e_receivablelocamt")).reduce(BigDecimal.ZERO,BigDecimal::add);
        
        BigDecimal incomein_2 = project_dy.getBigDecimal("bfgy_rmbamount");
        
        JJ_tsje = incomein_1;
        
        CONTRACT_INCOME = incomein_1.add(incomein_2);//合同收入
        CONTRACT_INTHEED = BigDecimal.ZERO;//合同已收汇金额
        ACCOUNT_INTHEED = BigDecimal.ZERO;//核算期已收汇金额
        CONTRACT_DIRECT_COST = pcon_rmb_sumamount;//合同直接成本
        CONTRACT_INDIRECT_COST = YG_yj.add(YG_qtfy).add(YG_hyf).add(YG_ckbxf);//合同间接成本
        PROJECT_FUNDS = JJ_xcxmjf;//现场项目经费
        
        DynamicObjectCollection INTHEED_dys = null;
        DynamicObjectCollection INTHEED_dys_f = null;
        
        if(StringUtils.isNotBlank(project_dy.getString("bfgy_projno"))) {
        	INTHEED_dys = QueryServiceHelper.query("bfgy_wb_receiptnotice", "bfgy_shjezbb,bfgy_cybzje,bfgy_currencywb.number,bfgy_currencywb.name", new QFilter[] {new QFilter("bfgy_projno.number", QCP.equals, project_dy.getString("bfgy_projno"))});
        }
        if(StringUtils.isNotBlank(project_dy.getString("bfgy_projno"))) {
        	qFilters_IN[2] = new QFilter("bfgy_projno.number", QCP.equals, project_dy.getString("bfgy_projno"));
        	INTHEED_dys_f = QueryServiceHelper.query("bfgy_wb_receiptnotice", "bfgy_shjezbb,bfgy_cybzje,bfgy_currencywb,bfgy_currencywb.number,bfgy_currencywb.name", qFilters_IN);
        }
        
        Map<String, BigDecimal> INTHEED_dys_map = new HashMap<>();
        List<String[]> INTHEED_dys_table = new ArrayList<>();
        if(INTHEED_dys != null && INTHEED_dys.size() > 0) {
        	CONTRACT_INTHEED = INTHEED_dys.stream().map(i->i.getBigDecimal("bfgy_shjezbb")).reduce(BigDecimal.ZERO,BigDecimal::add);
        	Map<String, List<DynamicObject>> INTHEED_dys_group = INTHEED_dys.stream().filter(i->StringUtils.isNotBlank(i.getString("bfgy_currencywb.name"))).collect(Collectors.groupingBy(i->i.getString("bfgy_currencywb.name")));
        	
        	INTHEED_dys_group.forEach((key,value)->{
        	 	BigDecimal sumvalue = value.stream().map(i->i.getBigDecimal("bfgy_cybzje")).reduce(BigDecimal.ZERO,BigDecimal::add);
        		INTHEED_dys_table.add(new String[] {key, sumvalue.setScale(2,2).toString()});
        	});
        	
        	HT_HTYSH = formatTable(INTHEED_dys_table, null);
        }
        
        Map<String, BigDecimal> INTHEED_BL_dys_map = new HashMap<>();
        List<String[]> INTHEED_BL_dys_table = new ArrayList<>();
        if(INTHEED_dys_f != null && INTHEED_dys_f.size() > 0) {
        	ACCOUNT_INTHEED = INTHEED_dys_f.stream().map(i->i.getBigDecimal("bfgy_shjezbb")).reduce(BigDecimal.ZERO,BigDecimal::add);
        
        	Map<String, List<DynamicObject>> INTHEED_BL_dys_group = INTHEED_dys_f.stream().filter(i->StringUtils.isNotBlank(i.getString("bfgy_currencywb.name"))).collect(Collectors.groupingBy(i->i.getString("bfgy_currencywb.name")));
        	
        	INTHEED_BL_dys_group.forEach((key,value)->{
        	 	BigDecimal sumvalue = value.stream().map(i->i.getBigDecimal("bfgy_cybzje")).reduce(BigDecimal.ZERO,BigDecimal::add);
        	 	INTHEED_BL_dys_table.add(new String[] {key, sumvalue.setScale(2,2).toString()});
        	});
        	
        	HT_HSQYSH = formatTable(INTHEED_BL_dys_table, null);
        }
        
        PROFITS = econ_rmb_sumamount.subtract(CONTRACT_INTHEED.add(ACCOUNT_INTHEED).add(CONTRACT_DIRECT_COST).add(CONTRACT_INDIRECT_COST).add(PROJECT_FUNDS));//预测/实际利润
        if (econ_rmb_sumamount.compareTo(BigDecimal.ZERO)!=0) {
            PROFITS_MARGIN = PROFITS.divide(econ_rmb_sumamount, 4).multiply(new BigDecimal(100));//预测实际利润率
//            HT_HTYSH_BL = HT_HTYSH.divide(econ_rmb_sumamount, 4).multiply(new BigDecimal(100));
//            HT_HSQYSH_BL = HT_HSQYSH.divide(econ_rmb_sumamount, 4).multiply(new BigDecimal(100));
        }

        /*项目预估成本*/
        DynamicObjectCollection costestimate = project_dy.getDynamicObjectCollection("bfgy_costestimateentity");
        costestimate.forEach(m->{
            String name = m.getString("bfgy_cscoststructure.name");
            String number = m.getString("bfgy_cscoststructure.number");
            BigDecimal amount = m.getBigDecimal("bfgy_csacirmb");
            if ("C01".equalsIgnoreCase(number)){
                //直接成本
                YG_zjcb = amount;
            }else if ("C02".equalsIgnoreCase(number)){
                //佣金
                YG_yj = amount;
            }else if("C04".equalsIgnoreCase(number)){
                //现场费i
                YG_xcf = amount;
            }else if ("C09".equalsIgnoreCase(number)){
                //其他费用
                YG_qtfy = amount;
            }else if ("C03".equalsIgnoreCase(number)){
                //海运费
                YG_hyf = amount;
            } else if("C05".equalsIgnoreCase(number)){
                //出口保信费
                YG_ckbxf = amount;
            }
        });
        
        CONTRACT_INDIRECT_COST = YG_yj.add(YG_qtfy).add(YG_hyf).add(YG_ckbxf);//合同间接成本

        if (base_project != null) {
            voucherlist(base_project.getPkValue().toString());
        } else {
            this.getView().showTipNotification("未找到该单据对应的项目，请检查标准项目下是否有相同的项目号的项目！");
        }
        QFilter[] qFilters = {new QFilter("bfgy_projectnum",QCP.equals,project_dy.getString("bfgy_projno"))};
        DynamicObject this_dy = BusinessDataServiceHelper.loadSingle("bfgy_highawardpaper", "bfgy_kjyfzc,bfgy_gzcg,bfgy_hlkzxcfyqk,bfgy_cktsgzwcqk,bfgy_heshfx,bfgy_dfjdcgcb,bfgy_projectnum,bfgy_values.bfgy_idname,bfgy_values.bfgy_value,bfgy_rate", qFilters);
        if (this_dy != null){
            LOD_kjyfzc = this_dy.getString("bfgy_kjyfzc");
            LOD_gzcg = this_dy.getString("bfgy_gzcg");
            LOD_hlkzxcfyqk = this_dy.getString("bfgy_hlkzxcfyqk");
            LOD_cktsgzwcqk = this_dy.getString("bfgy_cktsgzwcqk");
            LOD_heshfx = this_dy.getString("bfgy_heshfx");
            LOD_dfjdcgcb = this_dy.getString("bfgy_dfjdcgcb");
            LOD_projectnum = this_dy.getString("bfgy_projectnum");
            LOD_rate = this_dy.getString("bfgy_rate");
            LOD_values = new JSONArray();
            DynamicObjectCollection this_dy_cols = this_dy.getDynamicObjectCollection("bfgy_values");
            for(DynamicObject this_dy_col:this_dy_cols){
                JSONObject json = new JSONObject();
                json.put("id",this_dy_col.getString("bfgy_idname"));
                json.put("value",this_dy_col.getString("bfgy_value"));
                LOD_values.add(json);
            }
        }
        updateTable();
    }

    @Override
    public void beforeClick(BeforeClickEvent evt) {
        super.beforeClick(evt);
    }

    private void updateTable() {
        CustomControl customcontrol = this.getView().getControl("bfgy_customcontrolap");

        Map<String, String> data = new HashMap<>();

        data.put("LOD_kjyfzc",LOD_kjyfzc);
        data.put("LOD_gzcg",LOD_gzcg);
        data.put("LOD_hlkzxcfyqk",LOD_hlkzxcfyqk);
        data.put("LOD_cktsgzwcqk",LOD_cktsgzwcqk);
        data.put("LOD_heshfx",LOD_heshfx);
        data.put("LOD_dfjdcgcb",LOD_dfjdcgcb);
        data.put("LOD_projectnum",LOD_projectnum);
        data.put("LOD_rate", LOD_rate);
        data.put("LOD_values",LOD_values.toJSONString());


        data.put("ECONS", ECONS);
        data.put("PCONS", PCONS);

        data.put("ECONS_SUM", ECONS_SUM);
        data.put("PCONS_SUM", PCONS_SUM);
        data.put("SUM_CURR_ECONS", SUM_CURR_ECONS);

        data.put("PROJECT_NAME", PROJECT_NAME);
        data.put("PROJECT_CODE", PROJECT_CODE);

        data.put("CONTRACT_INCOME", CONTRACT_INCOME.setScale(2,2).toString());
        data.put("CONTRACT_INTHEED", CONTRACT_INTHEED.setScale(2,2).toString());
        data.put("ACCOUNT_INTHEED", ACCOUNT_INTHEED.setScale(2,2).toString());
        data.put("CONTRACT_DIRECT_COST", CONTRACT_DIRECT_COST.setScale(2,2).toString());
        data.put("CONTRACT_INDIRECT_COST", CONTRACT_INDIRECT_COST.setScale(2,2).toString());
        data.put("PROJECT_FUNDS", /*PROJECT_FUNDS*/YG_xcf.setScale(2,2).toString());
        data.put("PROFITS", PROFITS.setScale(2,2).toString());
        data.put("PROFITS_MARGIN", PROFITS_MARGIN == null ? "--%" :PROFITS_MARGIN.setScale(2,2).toString() + "%");

        data.put("HT_HTYSH",HT_HTYSH);
        data.put("HT_HTYSH_BL",HT_HTYSH_BL == null ? "--%" :HT_HTYSH_BL.setScale(2,2).toString() + "%");
        data.put("HT_HSQYSH",HT_HSQYSH);
        data.put("HT_HSQYSH_BL",HT_HSQYSH_BL == null ? "--%" :HT_HSQYSH_BL.setScale(2,2).toString() + "%");
//        data.put("",);

        // 通过setData给自定义控件传输数据，前端就能通过props.data获取数据
        data.put("JJ_xybx", JJ_xybx);
        data.put("JJ_ysbx", JJ_ysbx);
        data.put("JJ_yyf", JJ_yyf);
        data.put("JJ_nlyzf", JJ_nlyzf);
        data.put("JJ_bzf", JJ_bzf);
        data.put("JJ_ccbgf", JJ_ccbgf);
        data.put("JJ_jpglf", JJ_jpglf);
        data.put("JJ_cgf", JJ_cgf);
        data.put("JJ_jtf", JJ_jtf);
        data.put("JJ_lpf", JJ_lpf);
        data.put("JJ_ywzdf", JJ_ywzdf);
        data.put("JJ_hyf", JJ_hyf);
        data.put("JJ_clf", JJ_clf);
        data.put("JJ_bhf", JJ_bhf);
        data.put("JJ_pxf", JJ_pxf);
        data.put("JJ_yj", JJ_yj);
        data.put("JJ_yhsxf", JJ_yhsxf);
        data.put("JJ_hdsy", JJ_hdsy);
        data.put("JJ_ygcgbx", JJ_ygcgbx);
        data.put("JJ_ypf", JJ_ypf);
        data.put("JJ_pxf1", JJ_pxf1);
        data.put("JJ_lwf", JJ_lwf);
        data.put("JJ_hsscjrjzzf", JJ_hsscjrjzzf);
        data.put("JJ_qt", JJ_qt);
        data.put("JJ_xcxmjf", JJ_xcxmjf.setScale(2,2).toString());
        data.put("JJ_SUM", JJ_SUM.setScale(2,2).toString());
        data.put("JJ_tsje", JJ_tsje.setScale(2,2).toString());

        data.put("YG_zjcb",YG_zjcb.setScale(2,2).toString());
        data.put("YG_yj",YG_yj.setScale(2,2).toString());
        data.put("YG_xcf",YG_xcf.setScale(2,2).toString());
        data.put("YG_qtfy",YG_qtfy.setScale(2,2).toString());
        data.put("YG_hyf",YG_hyf.setScale(2,2).toString());
        data.put("YG_ckbxf",YG_ckbxf.setScale(2,2).toString());



        customcontrol.setData(data);
    }

    /**
     * 间接成本列表计算
     */
    private void voucher() {


    }

    /**
     * 间接成本
     *
     * @return
     */
    private BigDecimal voucherlist(String projectPkid) {

        INDIRECT_COST_MAP = new HashMap<String, BigDecimal>() {{
            put("1472.04.02.17", BigDecimal.ZERO);//"信用保险",
            put("1472.04.02.03", BigDecimal.ZERO);//"运输保险",
            put("1472.04.02.02", BigDecimal.ZERO);//"外运费",
            put("1472.04.02.22", BigDecimal.ZERO);//"内陆运杂费",
            put("1472.04.02.23", BigDecimal.ZERO);//"包装费",
            put("1472.04.02.04", BigDecimal.ZERO);//"仓储保管费",
            put("1472.04.02.07", BigDecimal.ZERO);//"军品管理费",
            put("1472.04.02.09", BigDecimal.ZERO);//"出国费",
            put("1472.04.02.11", BigDecimal.ZERO);//"接团费",
            put("1472.04.02.12", BigDecimal.ZERO);//"礼品费",
            put("1472.04.02.14", BigDecimal.ZERO);//"业务招待费",
            put("1472.04.02.13", BigDecimal.ZERO);//"会议费",
            put("1472.04.02.10", BigDecimal.ZERO);//"差旅费",
            put("1472.04.02.19", BigDecimal.ZERO);//"保函费",
            put("1472.04.02.25", BigDecimal.ZERO);//"培训费",
            put("1472.04.02.01", BigDecimal.ZERO);//"佣金",
            put("1472.04.02.20", BigDecimal.ZERO);//"银行手续费",
            put("1472.04.02.21", BigDecimal.ZERO);//"汇兑损益",
            put("1472.04.02.27", BigDecimal.ZERO);//"员工出国保险",
            put("1472.04.02.06", BigDecimal.ZERO);//"样品费",
            put("1472.04.02.251", BigDecimal.ZERO);//"培训费1",
            put("1472.04.02.08", BigDecimal.ZERO);//"劳务费",
            put("1472.04.02.24", BigDecimal.ZERO);//"技术手册及软件制作费",
            put("1472.04.02.28", BigDecimal.ZERO);//"其他",
            put("1472.04.03.02", BigDecimal.ZERO);//"现场项目经费"
            put("1221.07", BigDecimal.ZERO);//退税金额
        }};

        List<String> keylist = INDIRECT_COST_MAP.entrySet().stream().map(i -> i.getKey()).collect(Collectors.toList());
//        DynamicObjectCollection vouchers = QueryServiceHelper.query(VOUCHER_TABLE, "id,entries.account,entries.assgrp",
//                /*new QFilter[]{new QFilter("ispost", QCP.equals, true)}*/null);


        for (DynamicObject voucher : vouchers) {
            voucher.get("entries.assgrp");
            DynamicObjectCollection entries = voucher.getDynamicObjectCollection("entries");
            for (DynamicObject entry : entries) {
                String assgrp = entry.getString("assgrp");
                BigDecimal amount = entry.getBigDecimal("debitlocal");
                if (null != entry && null != entry.getDynamicObject("assgrp") && null != entry.getDynamicObject("assgrp").getString("value")) {
                    String json = entry.getDynamicObject("assgrp").getString("value");
                    JSONObject jsonObject = JSONObject.parseObject(json);
                    if (StringUtils.isNotBlank(jsonObject.getString("f000006")) && jsonObject.getString("f000006").equalsIgnoreCase(projectPkid)) {
                        String keynumber = entry.getDynamicObject("account").getString("number");
                        if (keynumber != null && keylist.contains(keynumber)) {
                            INDIRECT_COST_MAP.put(keynumber, INDIRECT_COST_MAP.get(keynumber).add(amount));
                        }
                    }
                }
            }
        }

//        vouchers.stream().filter();
        JJ_xybx = INDIRECT_COST_MAP.get("1472.04.02.17").setScale(2,2).toString();
        JJ_ysbx = INDIRECT_COST_MAP.get("1472.04.02.03").setScale(2,2).toString();//"运输保险",
        JJ_yyf = INDIRECT_COST_MAP.get("1472.04.02.02").setScale(2,2).toString();//"外运费",
        JJ_nlyzf = INDIRECT_COST_MAP.get("1472.04.02.22").setScale(2,2).toString();//"内陆运杂费",
        JJ_bzf = INDIRECT_COST_MAP.get("1472.04.02.23").setScale(2,2).toString();//"包装费",
        JJ_ccbgf = INDIRECT_COST_MAP.get("1472.04.02.04").setScale(2,2).toString();//"仓储保管费",
        JJ_jpglf = INDIRECT_COST_MAP.get("1472.04.02.07").setScale(2,2).toString();//"军品管理费",
        JJ_cgf = INDIRECT_COST_MAP.get("1472.04.02.09").setScale(2,2).toString();//"出国费",
        JJ_jtf = INDIRECT_COST_MAP.get("1472.04.02.11").setScale(2,2).toString();//"接团费",
        JJ_lpf = INDIRECT_COST_MAP.get("1472.04.02.12").setScale(2,2).toString();//"礼品费",
        JJ_ywzdf = INDIRECT_COST_MAP.get("1472.04.02.14").setScale(2,2).toString();//"业务招待费",
        JJ_hyf = INDIRECT_COST_MAP.get("1472.04.02.13").setScale(2,2).toString();//"会议费",
        JJ_clf = INDIRECT_COST_MAP.get("1472.04.02.10").setScale(2,2).toString();//"差旅费",
        JJ_bhf = INDIRECT_COST_MAP.get("1472.04.02.19").setScale(2,2).toString();//"保函费",
        JJ_pxf = INDIRECT_COST_MAP.get("1472.04.02.25").setScale(2,2).toString();//"培训费",
        JJ_yj = INDIRECT_COST_MAP.get("1472.04.02.01").setScale(2,2).toString();//"佣金",
        JJ_yhsxf = INDIRECT_COST_MAP.get("1472.04.02.20").setScale(2,2).toString();//"银行手续费",
        JJ_hdsy = INDIRECT_COST_MAP.get("1472.04.02.21").setScale(2,2).toString();//"汇兑损益",
        JJ_ygcgbx = INDIRECT_COST_MAP.get("1472.04.02.27").setScale(2,2).toString();//"员工出国保险",
        JJ_ypf = INDIRECT_COST_MAP.get("1472.04.02.06").setScale(2,2).toString();//"样品费",
        JJ_pxf1 = INDIRECT_COST_MAP.get("1472.04.02.251").setScale(2,2).toString();//"培训费1",
        JJ_lwf = INDIRECT_COST_MAP.get("1472.04.02.08").setScale(2,2).toString();//"劳务费",
        JJ_hsscjrjzzf = INDIRECT_COST_MAP.get("1472.04.02.24").setScale(2,2).toString();//"技术手册及软件制作费",
        JJ_qt = INDIRECT_COST_MAP.get("1472.04.02.28").setScale(2,2).toString();//"其他",
        JJ_xcxmjf = INDIRECT_COST_MAP.get("1472.04.03.02");//"现场项目经费",

//        JJ_tsje = INDIRECT_COST_MAP.get("1221.07");//"退税金额",

        JJ_SUM = INDIRECT_COST_MAP.entrySet().stream().map(i -> i.getValue()).reduce(BigDecimal.ZERO, BigDecimal::add);
        return null;
    }



    /**
     * 制表
     *
     * @param tablepre
     * @return
     */
    private String formatTable(List<String[]> tablepre, String[] colpan) {
        String str = "";
        for (String[] row : tablepre) {
            str += "<tr>";
            for (int i = 0; i < row.length; i++) {
                if (StringUtils.isNotBlank(row[i])) {
                    if (null != colpan && i > colpan.length && null == colpan[i]) {
                        str += "<td colspan=" + colpan[i] + ">";
                    } else {
                        str += "<td>";
                    }
                    str += row[i];
                    str += "</td>";
                } else {
                    str += "<td>&nbsp;</td>";
                }
            }
            str += "</tr>";
        }
        return str;
    }
}

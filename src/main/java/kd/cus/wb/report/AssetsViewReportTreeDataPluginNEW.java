﻿package kd.cus.wb.report;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kd.bos.algo.Algo;
import kd.bos.algo.DataSet;
import kd.bos.algo.DataType;
import kd.bos.algo.RowMeta;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.report.*;
import kd.bos.orm.ORM;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class AssetsViewReportTreeDataPluginNEW extends AbstractReportListDataPlugin {
    private static final String PROJECT_BILL_LOGO = "bfgy_proj_wb_pmb";
    private static final String CONTRACT_BILL_LOGO = "bfgy_wbexportcontract";
    private static final String EXPORTINVOICE_BILL_LOGO = "bfgy_wb_exportinvoice";
    private static final String REVCFMBILL_BILL_LOGO = "ar_revcfmbill";
    private static final String FINARBILL_BILL_LOGO = "ar_finarbill";

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object o) throws Throwable {
    	
    	Map<String, Object[]> value_map = new HashMap<String, Object[]>(); 
    	
        FilterInfo qfilter = reportQueryParam.getFilter();
        DynamicObjectCollection projects = qfilter.getDynamicObjectCollection("bfgy_projectfield");//项目
        DynamicObjectCollection contracts = qfilter.getDynamicObjectCollection("bfgy_contractfield");//合同
        String exportinvoice = qfilter.getString("bfgy_exportinvoicefield");//出口发票
        String revcfmbill = qfilter.getString("bfgy_revcfmbillfield");//收入确认
        String finarbill = qfilter.getString("bfgy_finarbillfield");//应收确认
        
        QFilter[] finarbill_qFilters = new QFilter[2];
        QFilter[] revcfmbill_qFilters = new QFilter[2];
        QFilter[] exportinvoice_qFilters = new QFilter[2];
        QFilter[] contract_qFilters = new QFilter[2];
        QFilter[] projects_qFilters = new QFilter[2];
        
        //应收确认过滤
        if (StringUtils.isNotBlank(finarbill)){
        	finarbill_qFilters[0] = new QFilter("billno", QCP.like, "%" + finarbill + "%");
        }
        
        //收入确认过滤
        if (StringUtils.isNotBlank(revcfmbill)){
            revcfmbill_qFilters[0] = new QFilter("billno", QCP.like, "%" + revcfmbill + "%");
        }
        
        //出口发票过滤
        if (StringUtils.isNotBlank(exportinvoice)){
            exportinvoice_qFilters[0] = new QFilter("bfgy_invoiceno", QCP.like, "%" + exportinvoice + "%");
        }
        
        //合同过滤
        if (contracts != null){
        	List<String> contractsnums = contracts.stream().map(i->i.getString("number")).collect(Collectors.toList());
            contract_qFilters[0] = new QFilter("billno", QCP.in, contractsnums);
        }

        //项目过滤
        if (projects != null){
        	List<String> projectsnums = projects.stream().map(i->i.getString("number")).collect(Collectors.toList());
            projects_qFilters[0] = new QFilter("bfgy_projno", QCP.in, projectsnums);
        }

        //报表数据
        List<Object[]> result_dataSetList = new ArrayList<>();

        Map<String,String> currencymap = new HashMap<>();

        Set<String> dist_finarbill = new HashSet<>();
        DynamicObjectCollection finarbill_cols = QueryServiceHelper.query(FINARBILL_BILL_LOGO,"id,billno,bfgy_reccomfbillno,recamount,bfgy_amountfield1",finarbill_qFilters);
        List<String> finarbill_numbers_exist = finarbill_cols.stream().map(i->i.getString("bfgy_reccomfbillno")).distinct().collect(Collectors.toList());
        
        revcfmbill_qFilters[1] = new QFilter("billno", QCP.in,finarbill_numbers_exist );
        
        Set<String> dist_revcfmbill = new HashSet<>();
        DynamicObjectCollection revcfmbill_cols = QueryServiceHelper.query(REVCFMBILL_BILL_LOGO,"id,billno,bfgy_outrecnum,confirmamt,bfgy_amountfield,bfgy_amountfield3",revcfmbill_qFilters);
        List<String> revcfmbill_numbers_exist = revcfmbill_cols.stream().map(i->i.getString("bfgy_outrecnum")).distinct().collect(Collectors.toList());
        
        exportinvoice_qFilters[1] = new QFilter("bfgy_invoiceno", QCP.in, revcfmbill_numbers_exist);
        
        Set<String> dist_exportinvoice = new HashSet<>();
        DynamicObjectCollection exportinvoice_cols = QueryServiceHelper.query(EXPORTINVOICE_BILL_LOGO,"bfgy_invoiceno,bfgy_excontractno,bfgy_currency,bfgy_totalamount,bfgy_unconfirmmoney",exportinvoice_qFilters);
        List<String> contract_numbers_exist = exportinvoice_cols.stream().map(i->i.getString("bfgy_excontractno")).distinct().collect(Collectors.toList());

        contract_qFilters[1] = new QFilter("billno", QCP.in, contract_numbers_exist);
        
        Set<String> dist_contract = new HashSet<>();
        DynamicObjectCollection contract_cols = QueryServiceHelper.query(CONTRACT_BILL_LOGO,"billno,bfgy_projno,bfgy_exportname",contract_qFilters);
        List<String> project_numbers_exist = contract_cols.stream().map(i -> i.getString("bfgy_projno")).distinct().collect(Collectors.toList());
        
        projects_qFilters[1] = new QFilter("bfgy_projno", QCP.in, project_numbers_exist);
        
        Set<String> dist_project = new HashSet<>();
        DynamicObjectCollection project_cols = QueryServiceHelper.query(PROJECT_BILL_LOGO, "bfgy_projno,bfgy_projname", projects_qFilters);
        List<String> project_numbers = project_cols.stream().map(i -> i.getString("bfgy_projno")).distinct().collect(Collectors.toList());

        //过滤
        project_cols.stream().filter(i->distinct(dist_project,i,"bfgy_projno")).forEach(item->{
            String COL_1_PROJECTNO = item.getString("bfgy_projno");
            String COL_1_PROJECTNAME = item.getString("bfgy_projname");
            
//            result_dataSetList.add(new Object[]{
////                    "P_" + COL_1_PROJECTNO,
////                    "0",
////                    "P_" + COL_1_PROJECTNO,
////                    project_numbers_exist.contains(COL_1_PROJECTNO),
////                    "项目：" + COL_1_PROJECTNO,
//                    COL_1_PROJECTNAME,
//                    COL_1_PROJECTNO,
//                    null,
//                    null,
//                    null,
//                    null,
//                    null,
//                    null,
//                    null,
//                    null,
//            });
        });

        contract_cols.stream().filter(i->distinct(dist_contract,i,"billno")).forEach(item->{
            String COL_2_CONTRACTNO = item.getString("billno");
            String COL_2_CONTRACTPROJECTNO = item.getString("bfgy_projno");
            String COL_2_CONTRACTNAME = item.getString("bfgy_exportname");
            
            Object[] value = {
            		COL_2_CONTRACTPROJECTNO,
            		COL_2_CONTRACTNAME
            };
            
            value_map.put("C_" + COL_2_CONTRACTNO, value);

//            result_dataSetList.add(new Object[]{
////                    "C_" + COL_2_CONTRACTNO,
////                    "P_" + COL_2_CONTRACTPROJECTNO,
////                    "C_" + COL_2_CONTRACTNO,
////                    contract_numbers_exist.contains(COL_2_CONTRACTNO),
////                    "出口合同：" + COL_2_CONTRACTNO,
//                    null,
//                    null,
//                    COL_2_CONTRACTNO,
//                    null,
//                    null,
//                    null,
//                    null,
//                    null,
//                    null,
//                    null,
//            });
        });
        
        Map<String, BigDecimal> revcfmbill_amount_map = new HashMap<>();
        Map<String, Set<String>> revcfmbill_ids_map = new HashMap<>();
        Map<String, String> link_map = new HashMap<>();
        
        revcfmbill_cols.stream().filter(i->distinct(dist_revcfmbill,i,"billno")).forEach(item->{
        	String id = item.getString("id");
            String COL_4_REVEFMBILLNO = item.getString("billno");
            String COL_4_REVEFMBILLEXPORTINVOICENO = item.getString("bfgy_outrecnum");
            BigDecimal COL_4_REVEFMBILLAMOUNT = item.getBigDecimal("confirmamt");
            BigDecimal COL_4_REVEFMBILLCONAMOUNT = item.getBigDecimal("bfgy_amountfield");
            BigDecimal COL_4_REVEFMBILLCONAMOUNTED = item.getBigDecimal("bfgy_amountfield3");
            
            link_map.put(COL_4_REVEFMBILLNO, COL_4_REVEFMBILLEXPORTINVOICENO);
            
            if(revcfmbill_amount_map.get(COL_4_REVEFMBILLEXPORTINVOICENO) == null) {
            	revcfmbill_amount_map.put(COL_4_REVEFMBILLEXPORTINVOICENO, COL_4_REVEFMBILLAMOUNT);
            	Set<String> ids = new HashSet<>();
            	ids.add(id);
            	revcfmbill_ids_map.put(COL_4_REVEFMBILLEXPORTINVOICENO, ids);
            }else {
            	revcfmbill_amount_map.put(COL_4_REVEFMBILLEXPORTINVOICENO, revcfmbill_amount_map.get(COL_4_REVEFMBILLEXPORTINVOICENO).add(COL_4_REVEFMBILLAMOUNT));
            	Set<String> ids = revcfmbill_ids_map.get(COL_4_REVEFMBILLEXPORTINVOICENO);
            	ids.add(id);
            	revcfmbill_ids_map.put(COL_4_REVEFMBILLEXPORTINVOICENO, ids);
            }
        });

        Map<String, BigDecimal> finarbill_amount_map = new HashMap<>();
        Map<String, Set<String>> finarbill_ids_map = new HashMap<>();
        finarbill_cols.stream().filter(i->distinct(dist_finarbill,i,"billno")).forEach(item->{
        	String id = item.getString("id");
        	
            String COL_5_FINARBILLNO = item.getString("billno");
            String COL_5_FINARBILLREVEFMBILLNO = item.getString("bfgy_reccomfbillno");
            
            String key = link_map.get(COL_5_FINARBILLREVEFMBILLNO);
            
            BigDecimal COL_5_FINARBILLAMOUNT = item.getBigDecimal("bfgy_amountfield1");
            
            if(finarbill_amount_map.get(key) == null) {
            	finarbill_amount_map.put(key, COL_5_FINARBILLAMOUNT);
            	Set<String> ids = new HashSet<>();
            	ids.add(id);
            	finarbill_ids_map.put(key, ids);
            }else {
            	finarbill_amount_map.put(key, finarbill_amount_map.get(key).add(COL_5_FINARBILLAMOUNT));
            	Set<String> ids = finarbill_ids_map.get(key);
            	ids.add(id);
            	finarbill_ids_map.put(key, ids);
            }
        });

        exportinvoice_cols.stream().filter(i->distinct(dist_exportinvoice,i,"bfgy_invoiceno")).forEach(item->{
            String COL_3_EXPORTINVOICENO = item.getString("bfgy_invoiceno");
            String COL_3_EXPORTINVOICECONTRACTNO = item.getString("bfgy_excontractno");
            String COL_3_EXPORTINVOICECURRENCY = item.getString("bfgy_currency");
            BigDecimal COL_3_EXPORTINVOICETOTALAMOUNT = item.getBigDecimal("bfgy_totalamount");
            BigDecimal COL_3_EXPORTINVOICETOTUNCONFIRM = item.getBigDecimal("bfgy_unconfirmmoney"); 
            
            String COL_4_REVEFMBILLEXPORTINVOICENO = null;
            BigDecimal COL_4_REVEFMBILLAMOUNT = null;
            BigDecimal COL_4_REVEFMBILLCONAMOUNT = null;
            BigDecimal COL_4_REVEFMBILLCONAMOUNTED = null;
            
            String COL_2_CONTRACTPROJECTNO = null;
            String COL_2_CONTRACTNAME = null;
            
            currencymap.put("I_" + COL_3_EXPORTINVOICENO,COL_3_EXPORTINVOICECURRENCY);
            
            Object[] contract_values = value_map.get("C_" + COL_3_EXPORTINVOICECONTRACTNO);
            if(contract_values != null) {
	            COL_2_CONTRACTPROJECTNO = (String) contract_values[0];
	            COL_2_CONTRACTNAME = (String) contract_values[1];
            }
            BigDecimal amountsub1 = revcfmbill_amount_map.get(COL_3_EXPORTINVOICENO);
            BigDecimal amountsub2 = COL_3_EXPORTINVOICETOTALAMOUNT.subtract(amountsub1);
            BigDecimal amountsub3 = finarbill_amount_map.get(COL_3_EXPORTINVOICENO);
            BigDecimal amountsub4 = amountsub1.subtract(amountsub3);
            result_dataSetList.add(new Object[]{
//                    "F_" + COL_5_FINARBILLNO,
//                    "R_" + COL_5_FINARBILLREVEFMBILLNO,
//                    "F_" + COL_5_FINARBILLNO,
//                    false,
//                    "应收确认：" + COL_5_FINARBILLNO,
            		COL_2_CONTRACTNAME,
            		COL_2_CONTRACTPROJECTNO,
            		COL_3_EXPORTINVOICECONTRACTNO,
            		COL_3_EXPORTINVOICENO,
            		COL_3_EXPORTINVOICECURRENCY,
            		COL_3_EXPORTINVOICETOTALAMOUNT,
            		amountsub1,
            		amountsub2,
            		amountsub3,
            		amountsub4,
            		StringUtils.join(revcfmbill_ids_map.get(COL_3_EXPORTINVOICENO), ","),
            		StringUtils.join(finarbill_ids_map.get(COL_3_EXPORTINVOICENO), ",")
            });
        });
        String[] cols = {
//                "id",
//                "pid",
//                "rowid",
//                "isgroupnode",
//                "col_1_01",
                "col_1_02",
                "col_1_03",
                "col_1_04",
                "col_1_05",
                "col_1_06",
                "col_1_07",
                "col_1_08",
                "col_1_09",
                "col_1_10",
                "col_1_11",
                "col_1_08_D",
                "col_1_10_D",
        };


        DataType[] datatypes = {
//                DataType.StringType,//0
//                DataType.StringType,//0
//                DataType.StringType,//0
//                DataType.BooleanType,//0
//                DataType.StringType,//1
                DataType.StringType,//2
                DataType.StringType,//3
                DataType.StringType,//4
                DataType.StringType,//5
                DataType.StringType,//6
                DataType.StringType,//7
                DataType.StringType,//8
                DataType.StringType,//9
                DataType.StringType,//10
                DataType.StringType,//11
                DataType.StringType,//10
                DataType.StringType,//11
        };

        RowMeta rowMeta = new RowMeta(cols, datatypes);
        Algo algo = Algo.create(this.getClass().getName());
        DataSet result_dataSet = algo.createDataSet(result_dataSetList, rowMeta);

        return result_dataSet;
    }

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        String json = "[" +

//                "{\"caption\":\"ID\"," +
//                "\"key\":\"id\"," +
//                "\"link\":\"false\"," +
//                "\"type\":\"" + ReportColumn.TYPE_TEXT + "\"}," +
//
//                "{\"caption\":\"父ID\"," +
//                "\"key\":\"pid\"," +
//                "\"link\":\"false\"," +
//                "\"type\":\"" + ReportColumn.TYPE_TEXT + "\"}," +
//
//                "{\"caption\":\"rowID\"," +
//                "\"key\":\"rowid\"," +
//                "\"link\":\"false\"," +
//                "\"type\":\"" + ReportColumn.TYPE_TEXT + "\"}," +
//
//                "{\"caption\":\"叶子节点\"," +
//                "\"key\":\"isgroupnode\"," +
//                "\"link\":\"false\"," +
//                "\"type\":\"" + ReportColumn.TYPE_BOOLEAN + "\"}," +
//                "{\"caption\":\"单据\"," +
//                "\"key\":\"col_1_01\"," +
//                "\"link\":\"false\"," +
//                "\"type\":\"" + ReportColumn.TYPE_TEXT + "\"}," +

                "{\"caption\":\"合同号\"," +
                "\"key\":\"col_1_02\"," +
                "\"link\":\"false\"," +
                "\"type\":\"" + ReportColumn.TYPE_TEXT + "\"}," +

                "{\"caption\":\"项目编码\"," +
                "\"key\":\"col_1_03\"," +
                "\"link\":\"false\"," +
                "\"type\":\"" + ReportColumn.TYPE_TEXT + "\"}," +

                "{\"caption\":\"合同号\"," +
                "\"key\":\"col_1_04\"," +
                "\"link\":\"false\"," +
                "\"type\":\"" + ReportColumn.TYPE_TEXT + "\"}," +

                "{\"caption\":\"发票编号\"," +
                "\"key\":\"col_1_05\"," +
                "\"link\":\"false\"," +
                "\"type\":\"" + ReportColumn.TYPE_TEXT + "\"}," +

                "{\"caption\":\"币种\"," +
                "\"key\":\"col_1_06\"," +
                "\"link\":\"false\"," +
                "\"dyname\":\"bd_currency\"," +
                "\"type\":\"" + ReportColumn.TYPE_BASEDATA + "\"}," +

                "{\"caption\":\"发票金额\"," +
                "\"key\":\"col_1_07\"," +
                "\"link\":\"false\"," +
                "\"currency\":\"col_1_06\"," +
                "\"type\":\"" + ReportColumn.TYPE_AMOUNT + "\"}," +

                "{\"caption\":\"已确认合同资产金额\"," +
                "\"key\":\"col_1_08\"," +
                "\"link\":\"true\"," +
                "\"currency\":\"col_1_06\"," +
                "\"type\":\"" + ReportColumn.TYPE_AMOUNT + "\"}," +

                "{\"caption\":\"未确认合同资产金额\"," +
                "\"key\":\"col_1_09\"," +
                "\"link\":\"false\"," +
                "\"currency\":\"col_1_06\"," +
                "\"type\":\"" + ReportColumn.TYPE_AMOUNT + "\"}," +

                "{\"caption\":\"已确认应收账款金额\"," +
                "\"key\":\"col_1_10\"," +
                "\"link\":\"true\"," +
                "\"currency\":\"col_1_06\"," +
                "\"type\":\"" + ReportColumn.TYPE_AMOUNT + "\"}," +

                "{\"caption\":\"未确认应收账款金额\"," +
                "\"key\":\"col_1_11\"," +
                "\"link\":\"false\"," +
                "\"currency\":\"col_1_06\"," +
                "\"type\":\"" + ReportColumn.TYPE_AMOUNT + "\"}," +
                
				"{\"caption\":\"ids\"," +
				"\"key\":\"col_1_08_D\"," +
				"\"link\":\"false\"," +
				"\"type\":\"" + ReportColumn.TYPE_TEXT + "\"}," +
				
				"{\"caption\":\"ids\"," +
				"\"key\":\"col_1_10_D\"," +
				"\"link\":\"false\"," +
				"\"type\":\"" + ReportColumn.TYPE_TEXT + "\"}," +

                "]";
        JSONArray titleJSON = JSONArray.parseArray(json);
        columns.addAll(makeTitles(titleJSON));
        return columns;
    }

    //构造报表头
    private AbstractReportColumn makeColumn(String caption, String ReportColumnType, String fieldKey, Boolean
            isLink, String dyname, String currency) {
//        DecimalReportColumn column = new DecimalReportColumn();

        ReportColumn column = new ReportColumn();
//        column.setSummary(1);
        if (StringUtils.isNotBlank(currency)) {
            column.setCurrencyField(currency);
        }

        if (StringUtils.isNotBlank(dyname)) {
            column.setRefBasedataProp(dyname);
            column = ReportColumn.createCurrencyColumn(fieldKey);
        }

        LocaleString localeString = new LocaleString();
        localeString.setLocaleValue(caption);

        column.setCaption(localeString);
        column.setDisplayProp(fieldKey);
        column.setScale(-1);
        //
        column.setFieldKey(fieldKey);
        ColumnStyle defstyle = new ColumnStyle();
        defstyle.setFontSize(12);
        defstyle.setTextAlign("default");
        column.setStyle(defstyle);

        column.setHyperlink(isLink == null ? false : isLink);
        column.setFieldType(ReportColumnType);
        return column;
    }

    //集合报表头
    private List<AbstractReportColumn> makeTitles(JSONArray titleJSON) {
        List<AbstractReportColumn> titles = new ArrayList<>();
        for (int i = 0; i < titleJSON.size(); i++) {
            JSONObject item = titleJSON.getJSONObject(i);
            titles.add(makeColumn(item.getString("caption"),
                    item.getString("type"),
                    item.getString("key"),
                    item.getBoolean("link"),
                    item.getString("dyname"),
                    item.getString("currency")));
        }
        return titles;
    }

    private Boolean distinct(Set<String> dist, DynamicObject dy,String field){
        int size = dist.size();
        dist.add(dy.getString(field));
        if (dist.size() > size){
            return true;
        }else {
            return false;
        }
    }
}

package kd.cus.wb.ap.busbill;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.BillEntityType;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.ExtendedDataEntitySet;
import kd.bos.entity.botp.ConvertOpType;
import kd.bos.entity.botp.ConvertRuleElement;
import kd.bos.entity.botp.plugin.AbstractConvertPlugIn;
import kd.bos.entity.botp.plugin.args.AfterConvertEventArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.cus.exchangerate.helpword.JMBaseDataHelper;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 暂估应付单，转换插件|供方发票，接收单-》暂估 ，
 * author suyp
 */

public class BusbillReturnConvertPlugIn extends AbstractConvertPlugIn {

    private void getContext(){
        // 源单主实体
        BillEntityType srcMainType = this.getSrcMainType();
        // 目标单主实体
        BillEntityType tgtMainType = this.getTgtMainType();
        // 转换规则
        ConvertRuleElement rule = this.getRule();
        // 转换方式：下推、选单
        ConvertOpType opType = this.getOpType();
    }


//    @Override
//    public void afterBuildQueryParemeter(AfterBuildQueryParemeterEventArgs e) {
//        this.printEventInfo("afterBuildQueryParemeter", "");
//    }
//
//    private void printEventInfo(String eventName, String argString){
//        String msg = String.format("%s : %s", eventName, argString);
//        System.out.println(msg);
//    }

    /**
     * 下推单据
     * @param e
     */
    @Override
    public void afterConvert(AfterConvertEventArgs e) {

        super.afterConvert(e);
        ExtendedDataEntitySet entitySet = e.getTargetExtDataEntitySet();
        ExtendedDataEntity[] entities = entitySet.FindByEntityKey("ap_busbill");
        for (int i = 0; i < entities.length; i++) {
            ExtendedDataEntity entity = entities[i];
            DynamicObject dataEntity = entity.getDataEntity();

            DynamicObjectCollection entry = dataEntity.getDynamicObjectCollection("entry");
            BigDecimal amountzbb = dataEntity.getBigDecimal("pricetaxtotalbase");//合同
            BigDecimal sumamount = BigDecimal.ZERO;
            for(DynamicObject item:entry){
                sumamount = sumamount.add(item.getBigDecimal("e_uninvoicedamt"));
                item.set("e_pricetaxtotal",BigDecimal.ZERO.subtract(item.getBigDecimal("e_uninvoicedamt")));
                item.set("e_pricetaxtotalbase",BigDecimal.ZERO.subtract(item.getBigDecimal("e_uninvoicedamt").multiply(getRate(dataEntity))));
            }

            DynamicObjectCollection amountzbbcol = dataEntity.getDynamicObjectCollection("planentity");
            amountzbbcol.clear();
            DynamicObject new_dy = new DynamicObject(amountzbbcol.getDynamicObjectType());
            new_dy.set("planpricetaxloc",amountzbb);//
            new_dy.set("p_uninvoicedlocamt",amountzbb);
            amountzbbcol.add(new_dy);

            dataEntity.set("uninvoicedamt",BigDecimal.ZERO.subtract(sumamount));

            String contract = dataEntity.getString("bfgy_contract");//合同
            String project = dataEntity.getString("bfgy_project");//项目
            String custom = dataEntity.getString("bfgy_custom");
            BigDecimal pricetaxtotal = BigDecimal.ZERO.subtract(dataEntity.getBigDecimal("pricetaxtotal"));
//        ((ExtendedDataEntitySet)e.getTargetExtDataEntitySet()).extDataEntityMap.get("ap_busbill").get(0).getDataEntity().getString("bfgy_contract");
//        this.printEventInfo("afterConvert", "");
            if (StringUtils.isNotBlank(contract)){
                QFilter[] qFilters_con = {new QFilter("name", QCP.equals,contract)};
                DynamicObject dy_pro = BusinessDataServiceHelper.loadSingleFromCache("bfgy_saleorderbd", qFilters_con);
                dataEntity.set("bfgy_contract_base",dy_pro);
            }

            if (StringUtils.isNotBlank(project)){
                QFilter[] qFilters_pro = {new QFilter("name",QCP.equals,project)};
                DynamicObject dy_con = BusinessDataServiceHelper.loadSingleFromCache("bd_project", qFilters_pro);
                dataEntity.set("bfgy_project_base",dy_con);
            }

            if (StringUtils.isNotBlank(custom)){
                QFilter[] qFilters_pro = {new QFilter("name",QCP.equals,custom)};
                DynamicObject dy_con = BusinessDataServiceHelper.loadSingleFromCache("bd_supplier", qFilters_pro);
                dataEntity.set("asstact",dy_con);
            }

            dataEntity.set("exchangerate", getRate(dataEntity));

            if (null != pricetaxtotal && null != getRate(dataEntity)){
                dataEntity.set("pricetaxtotalbase",BigDecimal.ZERO.subtract(pricetaxtotal.multiply(getRate(dataEntity))));
            }

            if(null != getRate(dataEntity) && null != sumamount){
                dataEntity.set("uninvoicedlocamt",BigDecimal.ZERO.subtract(sumamount.multiply(getRate(dataEntity))));
            }
        }
    }

    /**
     * 获得公司汇率
     * @return
     */

    private BigDecimal getRate(DynamicObject dataEntity) {
        DynamicObject currency = (DynamicObject) dataEntity.get("currency");
        if (null != currency) {
            // 组织ID
            DynamicObject org = (DynamicObject) dataEntity.get("org");
            Long orgId = org.getLong("id");
            // 币别ID
            Long tarCurrencyId = (Long) currency.get("id");
            // 日期
            Date date = (Date) dataEntity.get("exratedate");
            BigDecimal excRate = JMBaseDataHelper.getExcRate(orgId, tarCurrencyId, date);
            return excRate;
        }
        return new BigDecimal(0);
    }
}

package kd.cus.wb.ap.busbill;

import kd.bos.bill.IBillView;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.botp.runtime.BFRow;
import kd.bos.entity.operate.result.IOperateInfo;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.operation.DeleteServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * 暂估应付单|操作插件，操作之后删除选择单据
 *
 * @author suyp
 */

public class BusbillOpPlugin extends AbstractOperationServicePlugIn {

    /**
     * 操作之后删除
     *
     * @param e
     */
    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        String key = e.getOperationKey();
        
        DynamicObject[] dys = e.getDataEntities();
        if ("unauditanddelete".equalsIgnoreCase(key)) {
            /*获得所选单据*/
            List<Object> ids = Arrays.stream(dys).map(i -> i.getPkValue()).collect(Collectors.toList());
            OperationServiceHelper.executeOperate("delete", this.billEntityType.getName(), dys, OperateOption.create());
            QFilter[] qFilters = {new QFilter("id", QCP.in, ids)};
            DeleteServiceHelper helper = new DeleteServiceHelper();
            OperationResult res = helper.deleteOperate(this.billEntityType.getName(), ids.toArray(), OperateOption.create());
            List<IOperateInfo> errorInfo = res.getAllErrorOrValidateInfo();
            String msg = errorInfo.stream().map(m -> m.getMessage()).collect(Collectors.joining("\r\n"));
            this.setOperationResult(res);
        } else if ("pushandsave".equalsIgnoreCase(key)) {

        }
    }

    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        String key = e.getOperationKey();
        DynamicObject[] dys = e.getDataEntities();
        if ("unauditanddelete".equalsIgnoreCase(key)) {
            /*获得所选单据*/
            List<Object> ids = Arrays.stream(dys).map(i -> i.getPkValue()).collect(Collectors.toList());
            Map<String, HashSet<Long>> source = BFTrackerServiceHelper.findSourceBills(this.billEntityType.getName(), ids.toArray(new Long[ids.size()]));
            HashSet<Long> source0 = source.get("ap_busbill");
            HashSet<Long> source1 = source.get("ecma_materialinbill");
            HashSet<Long> source2 = source.get("bfgy_pm_wb_supinvoice");
            if (source0 != null){
                QFilter[] qFilters0 = {new QFilter("id", QCP.in, source0.stream().collect(Collectors.toList()))};
                DynamicObject[] sourcedys = BusinessDataServiceHelper.load("ap_busbill", "id,bfgy_checkboxfield", qFilters0);
                for (DynamicObject sourcedy : sourcedys) {
                    sourcedy.set("bfgy_checkboxfield", false);
                }
                SaveServiceHelper.save(sourcedys);
            }
//            if (source1 != null){
//                QFilter[] qFilters1 = {new QFilter("id", QCP.in, source1.stream().collect(Collectors.toList()))};
//                DynamicObject[] sourcedys1 = BusinessDataServiceHelper.load("ecma_materialinbill", "id,bfgy_isgetpay", qFilters1);
//                for (DynamicObject sourcedy : sourcedys1) {
//                    sourcedy.set("bfgy_isgetpay", false);
//                }
//                SaveServiceHelper.save(sourcedys1);
//            }
            
            for(DynamicObject entry :dys) {
            	Boolean isadjust = entry.getBoolean("isadjust");//冲回单
            	if(!isadjust) {
            		Long[] sids=new Long[1];
            		sids[0] = (Long) entry.get("id");
            		Map<String, HashSet<Long>> entrySource=BFTrackerServiceHelper.findSourceBills(this.billEntityType.getName(), sids);
                    HashSet<Long> ecma = source.get("ecma_materialinbill");
                    if (ecma != null){
                    	for(Long id:ecma) {
                    		DynamicObject loadSingle = BusinessDataServiceHelper.loadSingle(id, "ecma_materialinbill");
                    		loadSingle.set("bfgy_isgetpay", false);
                    		SaveServiceHelper.save(new DynamicObject[] {loadSingle});                    	}
                    	
                    }
            	}
            }
            
            
            if (source2 != null){
                QFilter[] qFilters2 = {new QFilter("id", QCP.in, source2.stream().collect(Collectors.toList()))};
                DynamicObject[] sourcedys2 = BusinessDataServiceHelper.load("bfgy_pm_wb_supinvoice", "id,bfgy_checkboxfield", qFilters2);
                for (DynamicObject sourcedy : sourcedys2) {
                    sourcedy.set("bfgy_checkboxfield", false);
                }
                SaveServiceHelper.save(sourcedys2);
            }
        }
    }
}

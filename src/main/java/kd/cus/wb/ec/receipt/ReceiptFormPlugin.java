package kd.cus.wb.ec.receipt;

import kd.bos.bill.BillShowParameter;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.AppMetadataCache;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.botp.runtime.BFRow;
import kd.bos.entity.botp.runtime.TableDefine;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.operate.PushAndSave;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.operate.botp.Push;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.botp.ConvertMetaServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.fi.iep.util.BussinessAndOperProvider;

import java.util.EventObject;
import java.util.List;
import java.util.Map;

/**
 * 接收单/供方发票下推判断是否下推
 */
public class ReceiptFormPlugin extends AbstractFormPlugin {

    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);

        if ((args.getSource() instanceof PushAndSave) && "pushandsave".equalsIgnoreCase(((PushAndSave) args.getSource()).getOperateKey())) {
            Long pkid = (Long) this.getModel().getDataEntity().getPkValue();
            String billName = this.getModel().getDataEntity().getDataEntityType().getName();
            String status = this.getModel().getDataEntity().getString("billstatus");
            Map<Long, List<BFRow>> dest = BFTrackerServiceHelper.findDirtTargetBills(billName/*"bfgy_pm_wb_supinvoice"*/, new Long[]{pkid});
            List<BFRow> destbills = dest.get(pkid);
            if(null != destbills) {
                for (BFRow item : destbills) {
                    if (item.getId() != null && item.getId().getMainTableId() != null) {
                        TableDefine tabledf = ConvertMetaServiceHelper.loadTableDefine(item.getId().getMainTableId());
                        if ("ap_busbill".equalsIgnoreCase(tabledf.getEntityNumber())) {
                            this.getView().showErrorNotification("单据已经下推,无法再次下推!");
                            args.setCancelMessage("单据已经下推,无法再次下推!");
                            args.setCancel(true);
                        }
                    }
                }
            }
            if (!"C".equalsIgnoreCase(status)) {
                this.getView().showErrorNotification("单据未审核!");
                args.setCancelMessage("单据未审核!");
                args.setCancel(true);
            }
        }
    }

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs afterDoOperationEventArgs) {
        super.afterDoOperation(afterDoOperationEventArgs);
        if ("pushandsave".equalsIgnoreCase(afterDoOperationEventArgs.getOperateKey())) {
            DynamicObject this_dy = this.getModel().getDataEntity();
            Long[] id = new Long[]{Long.parseLong(this_dy.getPkValue().toString())};
            Map<Long, List<BFRow>> dest = BFTrackerServiceHelper.findDirtTargetBills("ecma_materialinbill", id);
            if (dest.size() > 0) {
                List<BFRow> destId = dest.get(this_dy.getPkValue());
                if (destId.size() > 0) {
                    if (destId.size() > 1) {
                        this.getView().showErrorNotification("发现多张下推单，展示第一张");
                    }
                    String appId = AppMetadataCache.getAppInfo("bfgy_ecma_ext").getAppId();
                    
                    for(BFRow bfRow : destId) {
                    	Long firstdestId = bfRow.getId().getBillId();
                    	QFilter qFilter = new QFilter("id", QCP.equals, firstdestId);
                    	DynamicObject[] loads = BusinessDataServiceHelper.load("ap_busbill", "id", qFilter.toArray());
                    	if(loads.length>0) {
                    		 BillShowParameter parameter = new BillShowParameter();
                             parameter.setFormId("ap_busbill");
                             parameter.setCaption("暂估应付单");
                             parameter.getOpenStyle().setTargetKey("tabap");
                             parameter.getOpenStyle().setShowType(ShowType.MainNewTabPage);
                             parameter.setPkId(firstdestId);
                             this.getView().showForm(parameter);
                    	}
                    }
                    
//                    BFRow bfRow = destId.get(0);
//                    Long firstdestId = bfRow.getId().getBillId();
//                    BillShowParameter parameter = new BillShowParameter();
//                    parameter.setFormId("ap_busbill");
//                    parameter.setCaption("暂估应付单");
//                    parameter.getOpenStyle().setTargetKey("tabap");
//                    parameter.getOpenStyle().setShowType(ShowType.MainNewTabPage);
//                    parameter.setPkId(firstdestId);
//                    this.getView().showForm(parameter);
                    
                    

                }
            } else {
//                this.getView().showErrorNotification("未找到生成的下推单！");
            }
            System.out.println(dest);
        }
    }
}

package kd.cus.wb.cas.offerfile;


import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.events.*;
import kd.bos.form.operate.listop.ExportList;
import org.apache.commons.lang.StringUtils;
import org.tmatesoft.sqljet.core.internal.lang.SqlParser.operation_conflict_clause_return;


import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.monitor.log.KDException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

/**
 * 报盘文件|列表插件
 *
 * @author suyp
 */
public class OfferfileListPlugin extends AbstractListPlugin {

    private static final String Key = "bfgy_baritemap";
    private static final String BILL_LOGO = "bfgy_offerfile_inh";


//	@Override
//	public void addItemClickListeners(String... arg0) {
//		// TODO Auto-generated method stub
//		super.addItemClickListeners(arg0);
//		this.addItemClickListeners(Key);
//	}

    /**
     * 过滤审核状态，导出状态为未导出的单据
     *
     * @param e
     */
    @Override
    public void beforeQueryOfExport(BeforeQueryOfExportEvent e) {
        // TODO Auto-generated method stub
        super.beforeQueryOfExport(e);
        QFilter[] filters = e.getFilters();
        filters[0].and(new QFilter("billstatus", "=", "C"));
        filters[1].and(new QFilter("bfgy_datefield", "=", null));
    }

    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        if ("bfgy_reback".equalsIgnoreCase(evt.getItemKey())){
            ListSelectedRowCollection selects = this.getSelectedRows();
            List<Object> ids = selects.stream().map(i->i.getPrimaryKeyValue()).collect(Collectors.toList());
            QFilter[] qFilters = {new QFilter("id", QCP.in, ids)};
            DynamicObject[] dys = BusinessDataServiceHelper.load(BILL_LOGO, "billno,bfgy_datefield,billstatus,bfgy_billtype", qFilters);
            String showmsg = "";
            for (DynamicObject dy:dys){
                Date outdate = dy.getDate("bfgy_datefield");
                String status = dy.getString("billstatus");
                String billno = dy.getString("billno");
                DynamicObject type = dy.getDynamicObject("bfgy_billtype");
                if (outdate != null && "C".equalsIgnoreCase(status) && "WB_offerfile_BT_01".equalsIgnoreCase(type.getString("number"))){
                    showmsg += billno + " ";
                }
            }

            if (StringUtils.isNotBlank(showmsg)){
                evt.setCancel(true);
                this.getView().showErrorNotification("存在已导出的单据，未审核,非资流单的单据：" + showmsg);
            }

            for (DynamicObject dy:dys){
                dy.set("bfgy_datefield", new Date());
            }
            SaveServiceHelper.save(dys);
        }
    }

    /**
     * 操作之前，对数据进行校验
     *
     * @param args
     */
    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        // TODO Auto-generated method stub
        super.beforeDoOperation(args);
        args.getSource();
        if (((args.getSource() instanceof ExportList) && "exportlistbyselectfields".equalsIgnoreCase(((ExportList) args.getSource()).getOperateKey()))) {
            ListSelectedRowCollection selects = this.getSelectedRows();
            List<Object> distinctSelect = selects.stream().map(i -> i.getPrimaryKeyValue()).distinct().collect(Collectors.toList());

            if (distinctSelect.size() == 1) {
//				List<String> lists = selects.stream().map(i -> i.getBillNo()).collect(Collectors.toList());
                QFilter[] qFilters = {new QFilter("id", QCP.equals, distinctSelect.get(0))};
                DynamicObject dys = BusinessDataServiceHelper.loadSingle(BILL_LOGO, "id,billno,billstatus,bfgy_datefield", qFilters);
                if (null != dys && StringUtils.isNotBlank(dys.getString("billstatus")) && "C".equalsIgnoreCase(dys.getString("billstatus")) && null == dys.getDate("bfgy_datefield")) {
//			args.setCancel(true);
                    dys.set("bfgy_datefield", new Date());
                    this.getView().showTipNotification("数据符合条件，正在进行导出...");
                    SaveServiceHelper.save(new DynamicObject[]{dys});
                } else {
                    this.getView().showErrorNotification("不符合条件（已审核、已导出）");
                    args.setCancel(true);
                }
            } else if (distinctSelect.size() > 1) {
                this.getView().showErrorNotification("仅支持一张导出！");
                args.setCancelMessage("仅支持一张导出！");
                args.setCancel(true);
            } else {
                this.getView().showErrorNotification("请选择一张导出！");
                args.setCancelMessage("请选择一张导出！");
                args.setCancel(true);
            }
        }
    }

//    @Override
//    public void afterDoOperation(AfterDoOperationEventArgs afterDoOperationEventArgs) {
//        super.afterDoOperation(afterDoOperationEventArgs);
//        if ("exportlistbyselectfields".equalsIgnoreCase(afterDoOperationEventArgs.getOperateKey())) {
//            List<Object> ids = this.getSelectedRows().stream().map(i -> i.getPrimaryKeyValue()).collect(Collectors.toList());
//            QFilter[] qFilters = new QFilter[]{new QFilter("id",QCP.in,ids)};
//            DynamicObject[] list = BusinessDataServiceHelper.load("bfgy_offerfile_inh", "id,bfgy_datefield",qFilters);
//            for (DynamicObject dynamicObject:list){
//                dynamicObject.set("bfgy_datefield", new Date());
//            }
//            SaveServiceHelper.save(list);
//        }
//    }

    /**
     * 导出之后，将导出状态置为已导出
     * @param e
     */
    @Override
    public void afterQueryOfExport(AfterQueryOfExportEvent e) {
        DynamicObject[] list = e.getQueryValues();
//		List<DynamicObject> new_list = Arrays.stream(list).filter(m -> "C".equalsIgnoreCase(m.getString("billstatus")) && "0".equalsIgnoreCase(m.getString("bfgy_isexport"))).collect(Collectors.toList());
//		e.setQueryValues(new_list.toArray(new DynamicObject[list.length]));
        for (DynamicObject dynamicObject : list) {
            dynamicObject.set("bfgy_datefield", new Date());
            dynamicObject.set("bfgy_exportstatus", true);
        }
        SaveServiceHelper.save(list);
    }


    /**
     * 下拉按钮，根据选择的新增按钮的不同将不同的值置入页面缓存
     *
     * @param evt
     */
    @Override
    public void itemClick(ItemClickEvent evt) {
        String key = evt.getItemKey();
        if ("wbzld".equals(key)) { //万宝其他应付
            this.getPageCache().put("billtype", "WB_offerfile_BT_01");
            this.getView().invokeOperation("new");
        } else if ("wbdfd".equals(key)) { //万宝采购应付
            this.getPageCache().put("billtype", "WB_offerfile_BT_02");
            this.getView().invokeOperation("new");
        } else if ("wbdhd".equalsIgnoreCase(key)){
            this.getPageCache().put("billtype", "WB_offerfile_BT_03");
            this.getView().invokeOperation("new");
        }
    }
}
